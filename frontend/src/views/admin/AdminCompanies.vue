<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronRight, Plus, Search } from 'lucide-vue-next'
import { adminApi } from '../../api/admin'
import { companyChip } from '../../lib/status'
import { phone, shortTime } from '../../lib/format'

const router = useRouter()
const route = useRoute()
const companies = ref([])
const keyword = ref('')
const loading = ref(false)
const error = ref('')

// 상태 거르기는 화면에서 한다 (검색어는 서버, 상태는 받은 목록 안에서).
// 상단 바의 '검수 대기' 알약은 ?status=UNDER_REVIEW 로 이 화면을 연다.
const FILTERS = [
  { key: 'ALL', label: '전체' },
  { key: 'UNDER_REVIEW', label: '검수 대기' },
  { key: 'PARTIALLY_RETURNED', label: '재제출 대기' },
  { key: 'SUBMIT_WAITING', label: '제출 대기' },
  { key: 'COMPLETED', label: '완료' }
]
const filter = ref(route.query.status || 'ALL')
watch(
  () => route.query.status,
  (s) => (filter.value = s || 'ALL')
)
function setFilter(key) {
  filter.value = key
  router.replace({ query: key === 'ALL' ? {} : { status: key } })
}

const count = (s) => companies.value.filter((c) => c.status === s).length
// 요약 타일 = 자주 보는 필터 바로가기. 검수 대기 숫자만 파랑 (지금 결정할 것)
const stats = computed(() => [
  { key: 'ALL', label: '전체 기업', value: companies.value.length },
  { key: 'UNDER_REVIEW', label: '검수 대기', value: count('UNDER_REVIEW'), accent: true },
  { key: 'PARTIALLY_RETURNED', label: '재제출 대기', value: count('PARTIALLY_RETURNED') },
  { key: 'COMPLETED', label: '완료', value: count('COMPLETED') }
])
const visible = computed(() =>
  filter.value === 'ALL' ? companies.value : companies.value.filter((c) => c.status === filter.value)
)

const showCreate = ref(false)
const newName = ref('')
const creating = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    companies.value = await adminApi.searchCompanies(keyword.value)
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function createCompany() {
  if (!newName.value.trim()) return
  creating.value = true
  try {
    const created = await adminApi.createCompany(newName.value.trim())
    newName.value = ''
    showCreate.value = false
    router.push(`/admin/companies/${created.id}`)
  } catch (err) {
    error.value = err.message
  } finally {
    creating.value = false
  }
}

/** 제출 링크 열의 문구: 언제 열었는지 / 아직 안 열었는지 / 링크가 없는지 */
function linkText(c) {
  if (!c.hasLink) return { text: '링크 없음', faint: true }
  if (!c.linkOpenedAt) return { text: '아직 열지 않음', faint: true }
  return { text: `열람 ${shortTime(c.linkOpenedAt)}`, faint: false }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1>기업 관리</h1>
        <p class="page-sub">요청을 보낸 기업과 제출 진행 상황입니다.</p>
      </div>
      <button class="btn" @click="showCreate = !showCreate"><Plus :size="17" /> 기업 등록</button>
    </div>

    <div v-if="showCreate" class="card card-soft create-box">
      <label class="label" for="new-company">기업명</label>
      <div class="row">
        <input
          id="new-company"
          v-model="newName"
          class="input create-input"
          placeholder="기업명 입력"
          @keyup.enter="createCompany"
        />
        <button class="btn" :disabled="creating" @click="createCompany">등록</button>
      </div>
    </div>

    <section class="stats">
      <button
        v-for="s in stats"
        :key="s.key"
        class="stat"
        :class="{ on: filter === s.key, accent: s.accent }"
        @click="setFilter(s.key)"
      >
        <span class="stat-num">{{ s.value }}</span>
        <span class="stat-label">{{ s.label }}</span>
      </button>
    </section>

    <section class="toolbar">
      <label class="search">
        <Search :size="18" />
        <input v-model="keyword" placeholder="기업명 또는 전화번호" @keyup.enter="load" @search="load" type="search" />
      </label>
      <div class="seg">
        <button v-for="f in FILTERS" :key="f.key" :class="{ on: filter === f.key }" @click="setFilter(f.key)">
          {{ f.label }}
        </button>
      </div>
    </section>

    <div v-if="error" class="alert" style="margin-bottom: 16px">{{ error }}</div>
    <div v-if="loading" class="muted">불러오는 중…</div>

    <section v-else class="list">
      <div class="list-head">
        <span>기업</span><span>제출 진행</span><span>제출 링크</span><span>상태</span><span></span>
      </div>
      <button v-for="c in visible" :key="c.id" class="row-item" @click="router.push(`/admin/companies/${c.id}`)">
        <span class="name">
          <span class="squircle">{{ c.name.slice(0, 1) }}</span>
          <span>
            <b>{{ c.name }}</b>
            <small>{{ c.phoneNumber ? phone(c.phoneNumber) : '번호 없음' }}</small>
          </span>
        </span>
        <span class="progress">
          <template v-if="c.totalItems">
            <span class="bar"><span :style="{ width: (c.submittedItems / c.totalItems) * 100 + '%' }"></span></span>
            <small>{{ c.submittedItems }}/{{ c.totalItems }}</small>
          </template>
          <small v-else class="faint">요청 전</small>
        </span>
        <span class="link">
          <small :class="{ faint: linkText(c).faint }">{{ linkText(c).text }}</small>
        </span>
        <span><span class="chip" :class="companyChip(c.status)">{{ c.statusLabel }}</span></span>
        <ChevronRight :size="18" class="arrow" />
      </button>
      <p v-if="visible.length === 0" class="empty">
        {{ companies.length ? '조건에 맞는 기업이 없습니다.' : '등록된 기업이 없습니다.' }}
      </p>
    </section>
  </div>
</template>

<style scoped>
.create-box {
  margin-bottom: 20px;
}
.row {
  display: flex;
  gap: 8px;
}
.create-input {
  flex: 1;
  min-width: 0;
  background: var(--canvas);
}

/* 요약 타일 - 누르면 그 상태로 거른다 */
.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
.stat {
  text-align: left;
  border: 0;
  border-radius: var(--r-md);
  background: var(--canvas-soft);
  padding: 20px 22px;
  color: var(--ink);
  display: flex;
  flex-direction: column;
  gap: 6px;
  box-shadow: inset 0 0 0 1.5px transparent;
}
.stat.on {
  box-shadow: inset 0 0 0 1.5px var(--ink);
}
.stat-num {
  font-size: 40px;
  font-weight: var(--w-heading);
  line-height: 1;
}
.stat.accent .stat-num {
  color: var(--accent);
}
.stat-label {
  font-size: 14px;
  color: var(--muted);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.search {
  flex: 1 1 280px;
  max-width: 420px;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 0 16px;
  border-radius: var(--r-sm);
  background: var(--field);
  color: var(--muted);
}
.search:focus-within {
  box-shadow: var(--ring);
}
.search input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: none;
  background: transparent;
  font-size: 15px;
  color: var(--ink);
}
.search input::placeholder {
  color: var(--faint);
}

/* 목록 - 흰 카드 안에 얇은 구분선 */
.list {
  border: 1px solid var(--hairline-soft);
  border-radius: var(--r-md);
  overflow: hidden;
}
.list-head,
.row-item {
  display: grid;
  grid-template-columns: 2.2fr 1.4fr 1.3fr 1fr 24px;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
}
.list-head {
  min-height: 44px;
  background: var(--canvas-soft);
  font-size: 12px;
  font-weight: 600;
  color: var(--muted);
}
.row-item {
  width: 100%;
  min-height: 76px;
  border: 0;
  border-top: 1px solid var(--canvas-soft);
  background: var(--canvas);
  color: var(--ink);
  text-align: left;
}
.list-head + .row-item {
  border-top: 0;
}
.row-item:hover {
  background: #fafafa;
}
.name {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.name > span:last-child {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.name b {
  font-size: 16px;
  font-weight: var(--w-heading);
}
.row-item small {
  font-size: 13px;
  color: var(--muted);
}
.row-item small.faint {
  color: var(--faint);
}
.progress {
  display: flex;
  align-items: center;
  gap: 10px;
}
.bar {
  flex: 1;
  max-width: 120px;
  height: 6px;
  border-radius: var(--r-full);
  background: var(--canvas-soft);
  overflow: hidden;
}
.bar span {
  display: block;
  height: 100%;
  border-radius: var(--r-full);
  background: var(--ink);
}
.arrow {
  color: var(--faint);
}
.empty {
  margin: 0;
  padding: 32px;
  text-align: center;
  color: var(--muted);
}

/* 좁은 화면: 표 머리는 숨기고 한 기업을 카드처럼 쌓는다 */
@media (max-width: 840px) {
  .stats {
    grid-template-columns: repeat(2, 1fr);
  }
  .list-head {
    display: none;
  }
  .row-item {
    grid-template-columns: 1fr auto;
    gap: 8px 12px;
    padding: 16px 20px;
  }
  .progress,
  .link {
    grid-column: 1 / -1;
  }
  /* 상태 칩은 기업명 오른쪽 첫 줄에 */
  .row-item > span:nth-child(4) {
    grid-row: 1;
    grid-column: 2;
  }
  .arrow {
    display: none;
  }
}
@media (max-width: 640px) {
  .stat-num {
    font-size: 32px;
  }
}
</style>
