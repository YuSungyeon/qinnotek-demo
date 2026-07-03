<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import { customerApi } from '../../api/customer'
import PhotoUploadCard from '../../components/PhotoUploadCard.vue'
import Icon from '../../components/Icon.vue'

const phone = ref('')
const status = ref(null) // lookup 결과
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const submitHint = ref('')
const lightbox = ref('') // 확대 이미지 URL

// 전송 전까지 로컬에 보관하는 선택 파일 (itemId -> File)
const selectedFiles = reactive({})

const items = computed(() => status.value?.items ?? [])
const selectedCount = computed(() => items.value.filter((i) => selectedFiles[i.itemId]).length)
const allSelected = computed(
  () => items.value.length > 0 && items.value.every((i) => selectedFiles[i.itemId])
)
const progressPct = computed(() =>
  items.value.length ? Math.round((selectedCount.value / items.value.length) * 100) : 0
)
const showUploadUI = computed(
  () => status.value && ['INITIAL', 'RETURNED'].includes(status.value.state)
)

function clearSelected() {
  Object.keys(selectedFiles).forEach((k) => delete selectedFiles[k])
}

function scrollToItem(itemId, block = 'center') {
  const el = document.querySelector(`[data-item-id="${itemId}"]`)
  el?.scrollIntoView({ behavior: 'smooth', block })
}

async function lookup() {
  if (!phone.value.trim()) return
  error.value = ''
  loading.value = true
  status.value = null
  clearSelected()
  try {
    status.value = await customerApi.lookup(phone.value.trim())
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

function reset() {
  status.value = null
  error.value = ''
  submitHint.value = ''
  clearSelected()
}

function onSelect({ itemId, file }) {
  selectedFiles[itemId] = file
  submitHint.value = ''
  // 다음 미선택 항목으로 부드럽게 이동 (없으면 전송 바로)
  nextTick(() => {
    const next = items.value.find((i) => !selectedFiles[i.itemId])
    if (next) {
      scrollToItem(next.itemId, 'start')
    } else {
      document.querySelector('.submit-bar')?.scrollIntoView({ behavior: 'smooth', block: 'end' })
    }
  })
}

async function submit() {
  submitHint.value = ''
  if (!allSelected.value) {
    // 미선택 항목으로 스크롤 + 안내
    const missing = items.value.find((i) => !selectedFiles[i.itemId])
    if (missing) {
      submitHint.value = `'${missing.name}' 사진을 선택해주세요.`
      scrollToItem(missing.itemId, 'center')
    }
    return
  }
  submitting.value = true
  error.value = ''
  try {
    // 전송 시 한 번에 업로드 → 제출
    await Promise.all(
      items.value.map((i) => customerApi.uploadPhoto(i.itemId, selectedFiles[i.itemId]))
    )
    status.value = await customerApi.submit(status.value.companyId)
    clearSelected()
  } catch (err) {
    error.value = err.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="wrap">
    <header class="topbar">
      <h1>사진 제출</h1>
    </header>

    <!-- 전화번호 조회 -->
    <section v-if="!status" class="card lookup">
      <h2 class="title">전화번호 확인</h2>
      <p class="muted sub">기업에 등록된 전화번호를 입력해주세요.</p>
      <input
        v-model="phone"
        class="input"
        type="tel"
        inputmode="numeric"
        placeholder="예: 01012345678"
        @keyup.enter="lookup"
      />
      <div v-if="error" class="alert alert-error" style="margin-top: 12px">{{ error }}</div>
      <button class="btn btn-lg btn-block" style="margin-top: 14px" :disabled="loading" @click="lookup">
        <span v-if="loading" class="spinner"></span>
        <span v-else>조회</span>
      </button>
    </section>

    <!-- 조회 결과 -->
    <template v-else>
      <!-- 검수 중 / 완료: 간단 헤더 + 안내 -->
      <template v-if="!showUploadUI">
        <section class="company-head">
          <div>
            <div class="company-name">{{ status.companyName }}</div>
            <div class="muted" style="font-size: 20px">사진 제출</div>
          </div>
          <button class="btn btn-ghost btn-sm" @click="reset">다시 조회</button>
        </section>

        <section v-if="status.state === 'UNDER_REVIEW'" class="card notice">
          <div class="notice-icon review"><Icon name="clock" :size="52" /></div>
          <p class="notice-msg">{{ status.message }}</p>
          <p class="muted">감사합니다. 직원이 확인 중입니다.</p>
        </section>

        <section v-else-if="status.state === 'COMPLETED'" class="card notice">
          <div class="notice-icon done"><Icon name="check" :size="52" /></div>
          <p class="notice-msg">{{ status.message }}</p>
        </section>
      </template>

      <!-- 업로드 UI (최초 제출 / 반환) -->
      <template v-else>
        <div class="progress-head">
          <div class="ph-top">
            <div>
              <div class="company-name">{{ status.companyName }}</div>
              <div class="ph-sub muted">
                {{ status.state === 'RETURNED' ? '반환된 사진을 다시 촬영해 주세요' : '필요한 사진을 촬영해 주세요' }}
              </div>
            </div>
            <button class="btn btn-ghost btn-sm" @click="reset">다시 조회</button>
          </div>
          <div class="ph-bar"><div class="ph-fill" :style="{ width: progressPct + '%' }"></div></div>
          <div class="ph-count">
            사진 {{ items.length }}장 중 <b>{{ selectedCount }}장</b> 선택됨
          </div>
        </div>

        <div v-if="status.state === 'RETURNED'" class="alert alert-error banner">
          {{ status.message }}
        </div>

        <div class="items">
          <PhotoUploadCard
            v-for="item in items"
            :key="item.itemId"
            :item="item"
            @select="onSelect"
            @zoom="lightbox = $event"
          />
        </div>

        <div v-if="submitHint" class="alert alert-error" style="margin: 14px 0 0">{{ submitHint }}</div>
        <div v-if="error" class="alert alert-error" style="margin: 14px 0 0">{{ error }}</div>

        <div class="submit-bar">
          <p v-if="!allSelected && !submitting" class="bar-hint">
            사진 {{ items.length - selectedCount }}장이 더 필요해요
          </p>
          <button
            class="btn btn-lg btn-block"
            :class="{ 'btn-success': allSelected }"
            :disabled="submitting"
            @click="submit"
          >
            <span v-if="submitting" class="spinner"></span>
            <span v-else-if="allSelected">전송하기</span>
            <span v-else>전송 ({{ selectedCount }}/{{ items.length }})</span>
          </button>
        </div>
      </template>

      <!-- 이미지 확대 -->
      <div v-if="lightbox" class="lightbox" @click="lightbox = ''">
        <img :src="lightbox" alt="확대 이미지" />
        <p class="lightbox-hint">화면을 누르면 닫혀요</p>
      </div>
    </template>
  </div>
</template>

<style scoped>
.wrap {
  max-width: 560px;
  margin: 0 auto;
  padding: 16px 16px 60px;
  font-size: 24px; /* 고객 기본 글자 1.5배 (16 → 24) */
}
/* 입력/버튼도 1.5배로 */
.wrap .input {
  font-size: 24px;
  padding: 14px 16px;
}
.wrap .btn-lg {
  font-size: 26px;
}
.wrap .btn-sm {
  font-size: 20px;
}
.topbar {
  text-align: center;
  padding: 8px 0 16px;
}
.topbar h1 {
  font-size: 30px;
}
.title {
  font-size: 27px;
  margin-bottom: 4px;
}
.sub {
  margin: 0 0 14px;
  font-size: 21px;
}
.lookup {
  margin-top: 10px;
}
.company-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.company-name {
  font-size: 30px;
  font-weight: 800;
}
.notice {
  text-align: center;
  padding: 40px 20px;
}
.notice-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 14px;
}
.notice-icon.review {
  color: var(--primary);
}
.notice-icon.done {
  color: var(--success);
}
.notice-msg {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 6px;
}
.banner {
  margin: 12px 0 0;
  font-weight: 600;
  font-size: 22px;
}
.items {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 14px;
}

/* 상단 진행 헤더 (스크롤 시 상단 고정) */
.progress-head {
  position: sticky;
  top: 0;
  z-index: 5;
  background: var(--bg);
  padding: 8px 0 12px;
  margin-bottom: 2px;
}
.ph-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}
.ph-sub {
  font-size: 19px;
  margin-top: 2px;
}
.ph-bar {
  height: 12px;
  border-radius: 999px;
  background: #e5e7eb;
  overflow: hidden;
}
.ph-fill {
  height: 100%;
  border-radius: 999px;
  background: var(--success);
  transition: width 0.3s ease;
}
.ph-count {
  margin-top: 8px;
  font-size: 20px;
  color: var(--text-muted);
}
.ph-count b {
  color: var(--text);
  font-weight: 800;
}

/* 하단 고정 전송 바 */
.submit-bar {
  position: sticky;
  bottom: 0;
  margin: 18px -16px 0;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: rgba(245, 246, 248, 0.92);
  backdrop-filter: blur(8px);
  border-top: 1px solid var(--border);
}
.bar-hint {
  text-align: center;
  margin: 0 0 8px;
  font-size: 19px;
  color: var(--text-muted);
}

/* 이미지 확대 */
.lightbox {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(0, 0, 0, 0.88);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  padding: 20px;
}
.lightbox img {
  max-width: 96vw;
  max-height: 82vh;
  border-radius: 10px;
}
.lightbox-hint {
  color: rgba(255, 255, 255, 0.8);
  font-size: 19px;
  margin: 0;
}
</style>
