<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api/admin'
import { absoluteUrl } from '../../api/client'

// 시안: 관리자 화면을 고객 화면과 같은 design.md 스타일(모노크롬 · 스타디움 필 · 그림자 없는 틴트)로.
// 실제 관리자 화면(/admin)은 건드리지 않는다. 데이터는 데모용 고정값이고,
// 요청 자료 예시 이미지만 로컬 서버가 켜져 있으면 읽어 와 보여준다(없으면 아이콘으로 대신).
import {
  Building2,
  Layers,
  Users,
  Settings2,
  Search,
  Plus,
  ChevronRight,
  ChevronLeft,
  Link2,
  Copy,
  MessageSquare,
  RefreshCw,
  Link2Off,
  Eye,
  Bell,
  Download,
  FileText,
  Camera,
  Check,
  CornerDownRight,
  X,
  Pencil,
  Trash2
} from 'lucide-vue-next'

const screen = ref('companies') // companies | detail | requirements | managers | settings
const NAV = [
  { key: 'companies', label: '기업', icon: Building2 },
  { key: 'requirements', label: '요청 자료', icon: Layers },
  { key: 'managers', label: '담당자', icon: Users },
  { key: 'settings', label: '설정', icon: Settings2 }
]
function go(key) {
  screen.value = key
  window.scrollTo({ top: 0 })
}

// ---------------- 상태 표기 (흑백 + '결정이 필요한 것'만 파랑) ----------------
// design.md: 파랑(accent)은 "결정을 요구할 때"만. 관리자에게 결정이 필요한 상태 = 검수 대기.
const STATUS = {
  NONE: { label: '요청 없음', cls: 'st-none' },
  WAITING: { label: '제출 대기', cls: 'st-wait' },
  REVIEW: { label: '검수 대기', cls: 'st-review' },
  RETURNED: { label: '재제출 대기', cls: 'st-returned' },
  DONE: { label: '완료', cls: 'st-done' }
}

// ---------------- 데모 데이터 ----------------
const COMPANIES = [
  { id: 1, name: '한빛정밀(주)', phone: '010-1234-5678', status: 'REVIEW', done: 7, total: 7, opened: '오늘 09:12', submitted: '오늘 09:40' },
  { id: 2, name: '대성금속(주)', phone: '010-5555-6666', status: 'RETURNED', done: 4, total: 5, opened: '어제 17:05', submitted: '11.11 16:20' },
  { id: 3, name: '세진테크', phone: '010-2222-8080', status: 'DONE', done: 4, total: 4, opened: '11.08 10:31', submitted: '11.08 11:02' },
  { id: 4, name: '동아산업', phone: '010-7070-1212', status: 'WAITING', done: 0, total: 6, opened: null, submitted: null },
  { id: 5, name: '미래플라스틱', phone: null, status: 'NONE', done: 0, total: 0, opened: null, submitted: null }
]
const FILTERS = [
  { key: 'ALL', label: '전체' },
  { key: 'REVIEW', label: '검수 대기' },
  { key: 'RETURNED', label: '재제출 대기' },
  { key: 'WAITING', label: '제출 대기' },
  { key: 'DONE', label: '완료' }
]
const filter = ref('ALL')
const keyword = ref('')
const visibleCompanies = computed(() =>
  COMPANIES.filter(
    (c) =>
      (filter.value === 'ALL' || c.status === filter.value) &&
      (!keyword.value || c.name.includes(keyword.value) || (c.phone || '').includes(keyword.value))
  )
)
const count = (s) => COMPANIES.filter((c) => c.status === s).length
const reviewCount = count('REVIEW')

const REQUIREMENTS = [
  { name: '공장 외관', type: 'PHOTO', desc: '공장 건물 전체와 출입구가 함께 나오도록 밖에서 촬영해주세요.', used: 2 },
  { name: '생산 라인', type: 'PHOTO', desc: '가동 중인 생산 라인 전체가 한 화면에 들어오도록 촬영해주세요.', used: 2 },
  { name: '설비 명판', type: 'PHOTO', desc: '주요 설비의 명판(모델명·제조번호)이 읽히도록 가까이서 촬영해주세요.', used: 1 },
  { name: '자재 창고', type: 'PHOTO', desc: '원자재가 보관된 창고의 랙과 적재 상태가 보이도록 촬영해주세요.', used: 1 },
  { name: '완제품 적재', type: 'PHOTO', desc: '출하 대기 중인 완제품의 포장·라벨 상태가 보이도록 촬영해주세요.', used: 1 },
  { name: '품질 검사 장비', type: 'PHOTO', desc: '검사실 또는 검사대의 측정 장비가 보이도록 촬영해주세요.', used: 1 },
  { name: '소방 설비', type: 'PHOTO', desc: '소화기와 소화전 위치가 함께 보이도록 촬영해주세요.', used: 1 },
  { name: '안전 표지 게시', type: 'PHOTO', desc: '작업장 안전 수칙·경고 표지가 게시된 모습을 촬영해주세요.', used: 0 },
  { name: '사업자등록증', type: 'ANY', desc: '사업자등록증 사본 파일 또는 전체가 나오도록 촬영한 사진을 올려주세요.', used: 2 },
  { name: '공장등록증명서', type: 'ANY', desc: '공장등록증명서(최근 발급본) 파일 또는 촬영 사진을 올려주세요.', used: 1 },
  { name: 'ISO 인증서', type: 'ANY', desc: 'ISO 9001 등 보유한 품질·환경 인증서를 올려주세요.', used: 1 },
  { name: '설비 목록', type: 'DOCUMENT', desc: '보유 설비 목록을 올려주세요. (엑셀/한글/PDF 가능)', used: 1 },
  { name: '공정 흐름도', type: 'DOCUMENT', desc: '입고부터 출하까지의 공정 흐름도를 올려주세요.', used: 1 },
  { name: '최근 3개월 생산 실적', type: 'DOCUMENT', desc: '최근 3개월 월별 생산 실적 자료를 올려주세요. (엑셀 권장)', used: 0 }
]
const TYPE_LABEL = { PHOTO: '사진', DOCUMENT: '문서', ANY: '사진·문서' }
const reqFilter = ref('ALL')
const visibleReqs = computed(() =>
  REQUIREMENTS.filter((r) => reqFilter.value === 'ALL' || r.type === reqFilter.value)
)

// 로컬 서버가 켜져 있으면 실제 예시 이미지를 이름으로 매칭 (읽기만 함)
const images = reactive({})
onMounted(async () => {
  try {
    const list = await adminApi.listRequirements()
    list.forEach((r) => r.exampleImageUrl && (images[r.name] = absoluteUrl(r.exampleImageUrl)))
  } catch {
    /* 서버가 없으면 아이콘 타일로 표시 */
  }
})

// 기업 상세 - 한빛정밀
const assigned = reactive(
  Object.fromEntries(
    REQUIREMENTS.map((r) => [
      r.name,
      ['공장 외관', '생산 라인', '설비 명판', '자재 창고', '사업자등록증', '공장등록증명서', '설비 목록'].includes(r.name)
    ])
  )
)
const assignedCount = computed(() => Object.values(assigned).filter(Boolean).length)
const SUBMISSIONS = reactive([
  { name: '공장 외관', kind: 'PHOTO', status: 'REVIEW' },
  { name: '생산 라인', kind: 'PHOTO', status: 'REVIEW' },
  { name: '설비 명판', kind: 'PHOTO', status: 'DONE' },
  { name: '자재 창고', kind: 'PHOTO', status: 'REVIEW' },
  { name: '사업자등록증', kind: 'DOCUMENT', status: 'DONE', file: '사업자등록증_한빛정밀.pdf' },
  { name: '공장등록증명서', kind: 'DOCUMENT', status: 'REVIEW', file: '공장등록증명서.hwp' },
  {
    name: '설비 목록',
    kind: 'DOCUMENT',
    status: 'RETURNED',
    file: '설비현황_2026.xlsx',
    reason: '도입연도 열이 비어 있어요. 채워서 다시 올려주세요.'
  }
])
const pendingReview = computed(() => SUBMISSIONS.filter((s) => s.status === 'REVIEW').length)
const approve = (s) => (s.status = 'DONE')
const returnTarget = ref(null)
const returnReason = ref('')
function openReturn(s) {
  returnTarget.value = s
  returnReason.value = ''
}
function confirmReturn() {
  if (!returnReason.value.trim()) return
  returnTarget.value.status = 'RETURNED'
  returnTarget.value.reason = returnReason.value.trim()
  returnTarget.value = null
}

const MANAGERS = [
  { name: '김현우', position: '구매팀장', phone: '010-1111-2222', companies: ['한빛정밀(주)'] },
  { name: '이서연', position: '품질팀장', phone: '010-3333-4444', companies: ['한빛정밀(주)', '대성금속(주)'] },
  { name: '박준호', position: '생산관리 대리', phone: '010-5555-7777', companies: [] }
]
const smsOn = ref(true)

const NOTES = [
  {
    title: '사이드바 대신 떠 있는 상단 바.',
    body: '고객 화면과 같은 알약형 내비게이션. 메뉴가 4개뿐이라 상단으로 충분하고, 본문 폭을 넓게 쓴다.'
  },
  {
    title: '상태는 흑백으로, 결정할 것만 파랑.',
    body: '완료는 검정, 재제출 대기는 검정 테두리, 제출 대기는 회색. 관리자가 지금 판단해야 하는 "검수 대기"만 파랑으로 띄운다.'
  },
  {
    title: '빨강 대신 확인 단계.',
    body: '삭제·반환 같은 되돌리기 어려운 행동은 색 대신 확인 창과 문구로 구분한다. 반환은 사유를 써야 버튼이 켜진다.'
  },
  {
    title: '디자인은 모노크롬 하나로 고정.',
    body: '설정의 색상 프리셋·디자인 팩 선택을 없앤다. 적용 시 theme.js · api/theme.js · main.js 의 테마 적용과 /api/theme, admin_setting 의 테마 컬럼을 정리한다.'
  },
  {
    title: '그림자 없이 틴트와 얇은 선.',
    body: '카드는 흰 바탕 + 1px 연한 선, 강조 카드는 연회색 바탕. 사진 썸네일이 화면의 유일한 색이 된다.'
  }
]
</script>

<template>
  <div class="ad">
    <!-- 상단 플로팅 내비 필 -->
    <header class="ad-navwrap">
      <nav class="ad-nav">
        <span class="ad-logo"><FileText :size="18" :stroke-width="2.3" /></span>
        <span class="ad-wordmark">자료 검수</span>
        <div class="ad-menu">
          <button
            v-for="n in NAV"
            :key="n.key"
            class="ad-menu-item"
            :class="{ on: screen === n.key || (n.key === 'companies' && screen === 'detail') }"
            @click="go(n.key)"
          >
            <component :is="n.icon" :size="16" />
            <span>{{ n.label }}</span>
          </button>
        </div>
        <button class="ad-review-pill" @click="go('companies'); filter = 'REVIEW'">
          <Bell :size="15" />
          <span><span class="ad-review-text">검수 대기 </span>{{ reviewCount }}<span class="ad-review-text">곳</span></span>
        </button>
      </nav>
    </header>

    <main class="ad-main">
      <!-- ================= 기업 관리 ================= -->
      <template v-if="screen === 'companies'">
        <section class="ad-head">
          <div>
            <h1 class="ad-h1">기업 관리</h1>
            <p class="ad-lead">요청을 보낸 기업과 제출 진행 상황입니다.</p>
          </div>
          <button class="ad-btn ad-btn-primary"><Plus :size="17" /> 기업 등록</button>
        </section>

        <section class="ad-stats">
          <button class="ad-stat" :class="{ on: filter === 'ALL' }" @click="filter = 'ALL'">
            <span class="ad-stat-num">{{ COMPANIES.length }}</span><span class="ad-stat-label">전체 기업</span>
          </button>
          <button class="ad-stat ad-stat-accent" :class="{ on: filter === 'REVIEW' }" @click="filter = 'REVIEW'">
            <span class="ad-stat-num">{{ count('REVIEW') }}</span><span class="ad-stat-label">검수 대기</span>
          </button>
          <button class="ad-stat" :class="{ on: filter === 'RETURNED' }" @click="filter = 'RETURNED'">
            <span class="ad-stat-num">{{ count('RETURNED') }}</span><span class="ad-stat-label">재제출 대기</span>
          </button>
          <button class="ad-stat" :class="{ on: filter === 'DONE' }" @click="filter = 'DONE'">
            <span class="ad-stat-num">{{ count('DONE') }}</span><span class="ad-stat-label">완료</span>
          </button>
        </section>

        <section class="ad-toolbar">
          <label class="ad-search">
            <Search :size="18" />
            <input v-model="keyword" placeholder="기업명 또는 전화번호" />
          </label>
          <div class="ad-seg">
            <button v-for="f in FILTERS" :key="f.key" :class="{ on: filter === f.key }" @click="filter = f.key">
              {{ f.label }}
            </button>
          </div>
        </section>

        <section class="ad-list">
          <div class="ad-list-head">
            <span>기업</span><span>제출 진행</span><span>제출 링크</span><span>상태</span><span></span>
          </div>
          <button v-for="c in visibleCompanies" :key="c.id" class="ad-row" @click="go('detail')">
            <span class="ad-row-name">
              <span class="ad-sq">{{ c.name.slice(0, 1) }}</span>
              <span>
                <b>{{ c.name }}</b>
                <small>{{ c.phone || '번호 없음' }}</small>
              </span>
            </span>
            <span class="ad-row-progress">
              <template v-if="c.total">
                <span class="ad-bar"><span :style="{ width: (c.done / c.total) * 100 + '%' }"></span></span>
                <small>{{ c.done }}/{{ c.total }}</small>
              </template>
              <small v-else class="faint">요청 전</small>
            </span>
            <span class="ad-row-link">
              <small v-if="c.opened">열람 {{ c.opened }}</small>
              <small v-else-if="c.total" class="faint">아직 열지 않음</small>
              <small v-else class="faint">링크 없음</small>
            </span>
            <span><span class="ad-chip" :class="STATUS[c.status].cls">{{ STATUS[c.status].label }}</span></span>
            <ChevronRight :size="18" class="ad-row-arrow" />
          </button>
          <p v-if="!visibleCompanies.length" class="ad-empty">조건에 맞는 기업이 없습니다.</p>
        </section>
      </template>

      <!-- ================= 기업 상세 ================= -->
      <template v-else-if="screen === 'detail'">
        <button class="ad-btn ad-btn-soft ad-btn-sm ad-back" @click="go('companies')">
          <ChevronLeft :size="16" /> 기업 목록
        </button>
        <section class="ad-head">
          <div class="ad-company">
            <span class="ad-sq lg">한</span>
            <div>
              <h1 class="ad-h1">한빛정밀(주)</h1>
              <p class="ad-lead">
                <span class="ad-chip st-review">검수 대기 {{ pendingReview }}</span>
                <span class="muted">요청 자료 {{ assignedCount }}건 · 담당자 2명</span>
              </p>
            </div>
          </div>
          <div class="ad-actions">
            <button class="ad-btn ad-btn-outline ad-btn-sm"><Users :size="15" /> 알림 담당자 2</button>
            <button class="ad-btn ad-btn-outline ad-btn-sm"><Pencil :size="15" /> 이름 수정</button>
            <button class="ad-btn ad-btn-soft ad-btn-sm"><Trash2 :size="15" /> 삭제</button>
          </div>
        </section>

        <section class="ad-grid2">
          <!-- 제출 링크 (강조 카드: 연회색 바탕) -->
          <article class="ad-card ad-card-soft">
            <div class="ad-card-head">
              <h2 class="ad-h3">제출 링크</h2>
              <span class="ad-chip st-done-outline"><span class="dot"></span>사용 중</span>
            </div>
            <div class="ad-url">qinnotek.minisub.store/s/Ae7n7Z2zjKrbRMBFMyPxG4</div>
            <div class="ad-row-actions">
              <button class="ad-btn ad-btn-primary"><Copy :size="16" /> 링크 복사</button>
              <button class="ad-btn ad-btn-outline"><MessageSquare :size="16" /> 문자로 보내기</button>
            </div>
            <dl class="ad-meta">
              <div><dt>만든 날</dt><dd>2026.11.12 23:19</dd></div>
              <div><dt>만료</dt><dd>기한 없음</dd></div>
              <div><dt>마지막 접속</dt><dd>오늘 09:12</dd></div>
            </dl>
            <div class="ad-sub-actions">
              <button class="ad-link-btn"><RefreshCw :size="15" /> 재발급</button>
              <button class="ad-link-btn"><Link2Off :size="15" /> 링크 끄기</button>
              <button class="ad-link-btn"><Eye :size="15" /> 고객 화면</button>
            </div>
          </article>

          <!-- 요청 자료 지정: 체크박스 대신 토글 칩 -->
          <article class="ad-card">
            <div class="ad-card-head">
              <h2 class="ad-h3">요청 자료</h2>
              <span class="muted small">{{ assignedCount }}건 선택</span>
            </div>
            <div class="ad-toggles">
              <button
                v-for="r in REQUIREMENTS"
                :key="r.name"
                class="ad-toggle"
                :class="{ on: assigned[r.name] }"
                @click="assigned[r.name] = !assigned[r.name]"
              >
                <Check v-if="assigned[r.name]" :size="14" />
                {{ r.name }}
              </button>
            </div>
            <div class="ad-card-foot">
              <button class="ad-btn ad-btn-primary ad-btn-sm">저장</button>
            </div>
          </article>
        </section>

        <section class="ad-section">
          <div class="ad-section-head">
            <h2 class="ad-h2">제출 자료 검수</h2>
            <div class="ad-actions">
              <span class="muted small">최근 제출 오늘 09:40</span>
              <button class="ad-btn ad-btn-outline ad-btn-sm"><Download :size="15" /> 완료본 ZIP</button>
            </div>
          </div>

          <div class="ad-reviews">
            <article
              v-for="s in SUBMISSIONS"
              :key="s.name"
              class="ad-review"
              :class="{ waiting: s.status === 'REVIEW' }"
            >
              <div class="ad-media">
                <img v-if="s.kind === 'PHOTO' && images[s.name]" :src="images[s.name]" :alt="s.name" />
                <div v-else class="ad-media-ph">
                  <component :is="s.kind === 'DOCUMENT' ? FileText : Camera" :size="30" />
                  <small v-if="s.file">{{ s.file }}</small>
                </div>
                <span class="ad-overlay">{{ s.kind === 'DOCUMENT' ? '문서' : '사진' }}</span>
              </div>
              <div class="ad-review-body">
                <div class="ad-review-title">
                  <b>{{ s.name }}</b>
                  <span class="ad-chip" :class="STATUS[s.status].cls">{{ STATUS[s.status].label }}</span>
                </div>
                <p v-if="s.status === 'RETURNED' && s.reason" class="ad-reason">
                  <CornerDownRight :size="14" /> {{ s.reason }}
                </p>
                <div v-if="s.kind === 'DOCUMENT'" class="ad-doc-links">
                  <button class="ad-link-btn"><Download :size="14" /> 원본</button>
                  <button class="ad-link-btn"><Download :size="14" /> PDF</button>
                </div>
                <div v-if="s.status === 'REVIEW'" class="ad-review-actions">
                  <button class="ad-btn ad-btn-primary ad-btn-sm" @click="approve(s)"><Check :size="15" /> 통과</button>
                  <button class="ad-btn ad-btn-outline ad-btn-sm" @click="openReturn(s)">반환</button>
                </div>
                <p v-else class="ad-review-state muted small">
                  {{ s.status === 'DONE' ? '승인 완료' : '고객 재제출 대기' }}
                </p>
              </div>
            </article>
          </div>
        </section>
      </template>

      <!-- ================= 요청 자료 관리 ================= -->
      <template v-else-if="screen === 'requirements'">
        <section class="ad-head">
          <div>
            <h1 class="ad-h1">요청 자료</h1>
            <p class="ad-lead">기업에 지정할 수 있는 자료입니다. 사진·문서·기타 파일을 받을 수 있습니다.</p>
          </div>
          <button class="ad-btn ad-btn-primary"><Plus :size="17" /> 요청 자료 등록</button>
        </section>
        <section class="ad-toolbar">
          <div class="ad-seg">
            <button :class="{ on: reqFilter === 'ALL' }" @click="reqFilter = 'ALL'">전체 {{ REQUIREMENTS.length }}</button>
            <button :class="{ on: reqFilter === 'PHOTO' }" @click="reqFilter = 'PHOTO'">사진</button>
            <button :class="{ on: reqFilter === 'ANY' }" @click="reqFilter = 'ANY'">사진·문서</button>
            <button :class="{ on: reqFilter === 'DOCUMENT' }" @click="reqFilter = 'DOCUMENT'">문서</button>
          </div>
        </section>
        <section class="ad-req-grid">
          <article v-for="r in visibleReqs" :key="r.name" class="ad-req">
            <div class="ad-media sm">
              <img v-if="images[r.name]" :src="images[r.name]" :alt="r.name" />
              <div v-else class="ad-media-ph">
                <component :is="r.type === 'PHOTO' ? Camera : FileText" :size="28" />
                <small>{{ r.type === 'PHOTO' ? '예시 없음' : '파일 항목' }}</small>
              </div>
              <span class="ad-overlay">{{ TYPE_LABEL[r.type] }}</span>
            </div>
            <div class="ad-req-body">
              <b>{{ r.name }}</b>
              <p>{{ r.desc }}</p>
            </div>
            <div class="ad-req-foot">
              <small class="muted">{{ r.used ? `기업 ${r.used}곳에 지정` : '지정 안 됨' }}</small>
              <button class="ad-btn ad-btn-soft ad-btn-xs"><Pencil :size="13" /> 수정</button>
            </div>
          </article>
        </section>
      </template>

      <!-- ================= 담당자 ================= -->
      <template v-else-if="screen === 'managers'">
        <section class="ad-head">
          <div>
            <h1 class="ad-h1">담당자</h1>
            <p class="ad-lead">제출 알림 문자를 받을 사람입니다. 기업별 지정은 기업 상세에서 합니다.</p>
          </div>
          <button class="ad-btn ad-btn-primary"><Plus :size="17" /> 담당자 등록</button>
        </section>
        <section class="ad-list">
          <div class="ad-list-head mgr">
            <span>이름</span><span>전화번호</span><span>지정된 기업</span><span></span>
          </div>
          <div v-for="m in MANAGERS" :key="m.name" class="ad-row mgr static">
            <span class="ad-row-name">
              <span class="ad-avatar">{{ m.name.slice(0, 1) }}</span>
              <span><b>{{ m.name }}</b><small>{{ m.position }}</small></span>
            </span>
            <span class="ad-mono">{{ m.phone }}</span>
            <span class="ad-tags">
              <span v-for="c in m.companies" :key="c" class="ad-tag">{{ c }}</span>
              <small v-if="!m.companies.length" class="faint">없음</small>
            </span>
            <span class="ad-row-tools">
              <button class="ad-btn ad-btn-soft ad-btn-xs">수정</button>
              <button class="ad-icon-btn" aria-label="삭제"><X :size="16" /></button>
            </span>
          </div>
        </section>
      </template>

      <!-- ================= 설정 ================= -->
      <template v-else>
        <section class="ad-head">
          <div>
            <h1 class="ad-h1">설정</h1>
            <p class="ad-lead">알림 문자 발송을 관리합니다.</p>
          </div>
        </section>
        <section class="ad-settings">
          <article class="ad-card">
            <div class="ad-setting-row">
              <div>
                <b>제출 알림 문자</b>
                <p class="muted small">고객이 제출하면 지정된 담당자에게 문자를 보냅니다.</p>
              </div>
              <div class="ad-seg">
                <button :class="{ on: smsOn }" @click="smsOn = true">켜짐</button>
                <button :class="{ on: !smsOn }" @click="smsOn = false">꺼짐</button>
              </div>
            </div>
            <div class="ad-setting-row">
              <div>
                <b>발신번호</b>
                <p class="muted small">서버 환경변수 SOLAPI_SENDER 로 관리됩니다.</p>
              </div>
              <span class="ad-mono">010-2936-****</span>
            </div>
          </article>
        </section>
      </template>

      <!-- 시안 메모 -->
      <section class="ad-notes">
        <h2 class="ad-h2">시안 메모.</h2>
        <div class="ad-notes-grid">
          <article v-for="n in NOTES" :key="n.title" class="ad-note">
            <b>{{ n.title }}</b>
            <p>{{ n.body }}</p>
          </article>
        </div>
      </section>
    </main>

    <!-- 반환 확인 창 -->
    <div v-if="returnTarget" class="ad-mask" @click.self="returnTarget = null">
      <div class="ad-modal">
        <h3 class="ad-h3">'{{ returnTarget.name }}'을(를) 반환할까요?</h3>
        <p class="muted small">고객에게 사유가 보이고, 올린 파일은 지워집니다. 고객은 이 항목만 다시 올립니다.</p>
        <textarea v-model="returnReason" class="ad-textarea" rows="3" placeholder="반환 사유 (필수)"></textarea>
        <div class="ad-modal-actions">
          <button class="ad-btn ad-btn-soft" @click="returnTarget = null">취소</button>
          <button class="ad-btn ad-btn-primary" :disabled="!returnReason.trim()" @click="confirmReturn">반환하기</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ---- design.md 토큰 (고객 화면 CustomerView 와 같은 값) ---- */
.ad {
  --ink: #141414;
  --ink-soft: #262626;
  --canvas: #ffffff;
  --canvas-soft: #f3f3f3;
  --field: #f0f0f0;
  --hairline-soft: #f0f0f0;
  --hairline: #e0e0e0;
  --muted: #707070;
  --faint: #adadad;
  --accent: #0066ff;
  --r-sm: 16px;
  --r-md: 24px;
  --r-full: 9999px;

  min-height: 100vh;
  background: var(--canvas);
  color: var(--ink);
  font-family: 'Pretendard Variable', Pretendard, -apple-system, 'Helvetica Neue', Arial, sans-serif;
  font-weight: 456;
  word-break: keep-all;
  overflow-wrap: break-word;
}
.ad *,
.ad *::before,
.ad *::after {
  letter-spacing: 0;
  box-sizing: border-box;
}
.muted {
  color: var(--muted);
}
.faint {
  color: var(--faint);
}
.small {
  font-size: 13px;
}

/* ---- 타이포 ---- */
.ad-h1 {
  font-size: 40px;
  font-weight: 652;
  line-height: 1.13;
  margin: 0;
}
.ad-h2 {
  font-size: 28px;
  font-weight: 652;
  line-height: 1.2;
  margin: 0;
}
.ad-h3 {
  font-size: 20px;
  font-weight: 652;
  line-height: 1.3;
  margin: 0;
}
.ad-lead {
  margin: 10px 0 0;
  font-size: 17px;
  font-weight: 300;
  line-height: 1.45;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

/* ---- 버튼 ---- */
.ad-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 44px;
  padding: 0 18px;
  border-radius: var(--r-full);
  border: 1px solid transparent;
  font: inherit;
  font-size: 15px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  color: var(--ink);
  background: var(--canvas);
}
.ad-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.ad-btn-primary {
  background: var(--ink);
  color: #fff;
}
.ad-btn-primary:not(:disabled):hover {
  background: var(--ink-soft);
}
.ad-btn-outline {
  border-color: var(--hairline);
}
.ad-btn-outline:hover {
  background: var(--canvas-soft);
}
.ad-btn-soft {
  background: var(--canvas-soft);
}
.ad-btn-soft:hover {
  background: #e9e9e9;
}
.ad-btn-sm {
  min-height: 38px;
  padding: 0 14px;
  font-size: 14px;
}
.ad-btn-xs {
  min-height: 30px;
  padding: 0 12px;
  font-size: 13px;
}
.ad-link-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 0;
  background: none;
  padding: 6px 4px;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
  cursor: pointer;
}
.ad-link-btn:hover {
  color: var(--ink);
}
.ad-icon-btn {
  width: 30px;
  height: 30px;
  border-radius: var(--r-full);
  border: 0;
  background: transparent;
  color: var(--muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.ad-icon-btn:hover {
  background: var(--canvas-soft);
  color: var(--ink);
}

/* ---- 상태 칩: 흑백 + 검수 대기만 파랑 ---- */
.ad-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  padding: 7px 11px;
  border-radius: var(--r-full);
  white-space: nowrap;
}
.st-none {
  color: var(--faint);
  box-shadow: inset 0 0 0 1px var(--hairline);
}
.st-wait {
  color: var(--muted);
  background: var(--canvas-soft);
}
.st-review {
  color: #fff;
  background: var(--accent);
}
.st-returned {
  color: var(--ink);
  box-shadow: inset 0 0 0 1.5px var(--ink);
}
.st-done {
  color: #fff;
  background: var(--ink);
}
.st-done-outline {
  color: var(--ink);
  background: var(--canvas);
}
.ad-chip .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

/* ---- 내비 ---- */
.ad-navwrap {
  position: sticky;
  top: 0;
  z-index: 20;
  padding: 16px 16px 0;
  display: flex;
  justify-content: center;
  pointer-events: none;
}
.ad-nav {
  pointer-events: auto;
  width: 100%;
  max-width: 1120px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  background: var(--canvas-soft);
  border-radius: var(--r-full);
}
.ad-logo {
  width: 36px;
  height: 36px;
  border-radius: 30%;
  background: var(--ink);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.ad-wordmark {
  font-weight: 652;
  font-size: 16px;
  margin-right: 8px;
  white-space: nowrap;
}
.ad-menu {
  display: flex;
  gap: 2px;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}
.ad-menu-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 0;
  background: transparent;
  padding: 0 14px;
  min-height: 38px;
  border-radius: var(--r-full);
  font: inherit;
  font-size: 15px;
  font-weight: 600;
  color: var(--muted);
  cursor: pointer;
  white-space: nowrap;
}
.ad-menu-item:hover {
  color: var(--ink);
}
.ad-menu-item.on {
  background: var(--canvas);
  color: var(--ink);
}
.ad-review-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 0;
  min-height: 38px;
  padding: 0 14px;
  border-radius: var(--r-full);
  background: var(--accent);
  color: #fff;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

/* ---- 레이아웃 ---- */
.ad-main {
  max-width: 1120px;
  margin: 0 auto;
  padding: 48px 16px 96px;
}
.ad-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 32px;
}
.ad-back {
  margin-bottom: 20px;
}
.ad-company {
  display: flex;
  align-items: center;
  gap: 16px;
}
.ad-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.ad-sq {
  width: 40px;
  height: 40px;
  border-radius: 30%;
  background: var(--canvas-soft);
  color: var(--ink);
  font-weight: 652;
  font-size: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.ad-sq.lg {
  width: 64px;
  height: 64px;
  font-size: 24px;
  background: var(--ink);
  color: #fff;
}
.ad-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--canvas-soft);
  font-weight: 652;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

/* ---- 요약 타일 ---- */
.ad-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
.ad-stat {
  text-align: left;
  border: 0;
  border-radius: var(--r-md);
  background: var(--canvas-soft);
  padding: 20px 22px;
  font: inherit;
  color: var(--ink);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 6px;
  box-shadow: inset 0 0 0 1.5px transparent;
}
.ad-stat.on {
  box-shadow: inset 0 0 0 1.5px var(--ink);
}
.ad-stat-num {
  font-size: 40px;
  font-weight: 652;
  line-height: 1;
}
.ad-stat-label {
  font-size: 14px;
  color: var(--muted);
}
.ad-stat-accent .ad-stat-num {
  color: var(--accent);
}

/* ---- 검색 + 필터 ---- */
.ad-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.ad-search {
  flex: 1 1 280px;
  max-width: 420px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  min-height: 46px;
  background: var(--field);
  border-radius: var(--r-sm);
  color: var(--muted);
}
.ad-search:focus-within {
  box-shadow: 0 0 0 2px var(--ink);
}
.ad-search input {
  flex: 1;
  min-width: 0;
  border: 0;
  background: transparent;
  outline: none;
  font: inherit;
  font-size: 15px;
  color: var(--ink);
}
.ad-search input::placeholder {
  color: var(--faint);
}
.ad-seg {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 4px;
  background: var(--canvas-soft);
  border-radius: 22px;
}
.ad-seg button {
  border: 0;
  background: transparent;
  color: var(--muted);
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  min-height: 34px;
  padding: 0 14px;
  border-radius: var(--r-full);
  cursor: pointer;
  white-space: nowrap;
}
.ad-seg button.on {
  background: var(--canvas);
  color: var(--ink);
  box-shadow: 0 1px 3px rgba(20, 20, 20, 0.08); /* design.md 의 유일한 그림자 예외 */
}

/* ---- 목록 (기업·담당자) ---- */
.ad-list {
  border: 1px solid var(--hairline-soft);
  border-radius: var(--r-md);
  overflow: hidden;
}
.ad-list-head,
.ad-row {
  display: grid;
  grid-template-columns: 2.2fr 1.4fr 1.3fr 1fr 24px;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
}
.ad-list-head {
  min-height: 44px;
  background: var(--canvas-soft);
  font-size: 12px;
  font-weight: 600;
  color: var(--muted);
}
.ad-row {
  width: 100%;
  min-height: 76px;
  border: 0;
  border-top: 1px solid var(--canvas-soft);
  background: var(--canvas);
  font: inherit;
  color: var(--ink);
  text-align: left;
  cursor: pointer;
}
.ad-row:first-of-type {
  border-top: 0;
}
.ad-row:not(.static):hover {
  background: #fafafa;
}
.ad-row.static {
  cursor: default;
}
.ad-list-head.mgr,
.ad-row.mgr {
  grid-template-columns: 1.6fr 1.2fr 2fr auto;
}
.ad-row-name {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.ad-row-name > span:last-child {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.ad-row-name b {
  font-size: 16px;
  font-weight: 652;
}
.ad-row-name small,
.ad-row small {
  font-size: 13px;
  color: var(--muted);
}
.ad-row small.faint {
  color: var(--faint);
}
.ad-row-progress {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ad-bar {
  flex: 1;
  max-width: 120px;
  height: 6px;
  border-radius: var(--r-full);
  background: var(--canvas-soft);
  overflow: hidden;
}
.ad-bar span {
  display: block;
  height: 100%;
  border-radius: var(--r-full);
  background: var(--ink);
}
.ad-row-arrow {
  color: var(--faint);
}
.ad-empty {
  margin: 0;
  padding: 32px;
  text-align: center;
  color: var(--muted);
}
.ad-mono {
  font-variant-numeric: tabular-nums;
  font-size: 14px;
}
.ad-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.ad-tag {
  font-size: 12px;
  font-weight: 600;
  padding: 6px 10px;
  border-radius: var(--r-full);
  background: var(--canvas-soft);
}
.ad-row-tools {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ---- 카드 ---- */
.ad-grid2 {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 16px;
  align-items: start;
}
.ad-card {
  border: 1px solid var(--hairline-soft);
  border-radius: var(--r-md);
  padding: 24px;
  background: var(--canvas);
}
.ad-card-soft {
  background: var(--canvas-soft);
  border-color: transparent;
}
.ad-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.ad-card-foot {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
.ad-url {
  background: var(--canvas);
  border-radius: var(--r-sm);
  padding: 14px 16px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  word-break: break-all;
}
.ad-row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.ad-row-actions .ad-btn {
  flex: 1 1 150px;
}
.ad-meta {
  margin: 18px 0 0;
  display: grid;
  gap: 6px;
  font-size: 14px;
}
.ad-meta div {
  display: flex;
  justify-content: space-between;
}
.ad-meta dt {
  color: var(--muted);
}
.ad-meta dd {
  margin: 0;
  font-variant-numeric: tabular-nums;
}
.ad-sub-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #e8e8e8;
}
.ad-toggles {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.ad-toggle {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid var(--hairline);
  background: var(--canvas);
  color: var(--muted);
  min-height: 36px;
  padding: 0 14px;
  border-radius: var(--r-full);
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.ad-toggle.on {
  background: var(--ink);
  border-color: var(--ink);
  color: #fff;
}

/* ---- 검수 ---- */
.ad-section {
  margin-top: 64px;
}
.ad-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.ad-reviews {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.ad-review {
  border: 1px solid var(--hairline-soft);
  border-radius: var(--r-md);
  padding: 12px;
  display: flex;
  flex-direction: column;
}
.ad-review.waiting {
  box-shadow: inset 0 0 0 1.5px var(--accent);
  border-color: transparent;
}
.ad-media {
  position: relative;
  aspect-ratio: 4 / 3;
  border-radius: var(--r-sm);
  overflow: hidden;
  background: var(--canvas-soft);
}
.ad-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.ad-media-ph {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--muted);
  padding: 12px;
  text-align: center;
}
.ad-media-ph small {
  font-size: 12px;
  word-break: break-all;
}
.ad-overlay {
  position: absolute;
  top: 10px;
  left: 10px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: rgba(115, 115, 115, 0.56); /* design.md badge-overlay */
  padding: 5px 10px;
  border-radius: var(--r-full);
}
.ad-review-body {
  padding: 14px 6px 4px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}
.ad-review-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.ad-review-title b {
  font-size: 17px;
  font-weight: 652;
}
.ad-reason {
  margin: 0;
  display: flex;
  gap: 6px;
  align-items: flex-start;
  font-size: 13px;
  line-height: 1.45;
  background: var(--canvas-soft);
  border-radius: 12px;
  padding: 10px 12px;
}
.ad-reason svg {
  flex: 0 0 auto;
  margin-top: 2px;
}
.ad-doc-links {
  display: flex;
  gap: 6px;
}
.ad-review-actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
}
.ad-review-actions .ad-btn {
  flex: 1;
}
.ad-review-state {
  margin: auto 0 0;
}

/* ---- 요청 자료 ---- */
.ad-req-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.ad-req {
  border: 1px solid var(--hairline-soft);
  border-radius: var(--r-md);
  padding: 10px;
  display: flex;
  flex-direction: column;
}
.ad-req-body {
  padding: 12px 6px 0;
  flex: 1;
}
.ad-req-body b {
  font-size: 16px;
  font-weight: 652;
}
.ad-req-body p {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.45;
  color: var(--muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.ad-req-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 6px 4px;
}

/* ---- 설정 ---- */
.ad-settings {
  display: grid;
  gap: 16px;
  max-width: 760px;
}
.ad-setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 6px 0;
}
.ad-setting-row + .ad-setting-row {
  margin-top: 14px;
  padding-top: 20px;
  border-top: 1px solid var(--canvas-soft);
}
.ad-setting-row p {
  margin: 4px 0 0;
}

/* ---- 시안 메모 ---- */
.ad-notes {
  margin-top: 96px;
  padding-top: 40px;
  border-top: 1px solid var(--hairline-soft);
}
.ad-notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-top: 20px;
}
.ad-note {
  background: var(--canvas-soft);
  border-radius: var(--r-md);
  padding: 20px;
}
.ad-note b {
  font-size: 15px;
  font-weight: 652;
}
.ad-note p {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--muted);
}

/* ---- 확인 창 ---- */
.ad-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(20, 20, 20, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.ad-modal {
  width: 100%;
  max-width: 460px;
  background: var(--canvas);
  border-radius: var(--r-md);
  padding: 28px;
}
.ad-modal .muted {
  margin: 8px 0 16px;
}
.ad-textarea {
  width: 100%;
  border: 0;
  background: var(--field);
  border-radius: var(--r-sm);
  padding: 14px 16px;
  font: inherit;
  font-size: 15px;
  resize: vertical;
  outline: none;
}
.ad-textarea:focus {
  box-shadow: 0 0 0 2px var(--ink);
}
.ad-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
}

/* ---- 반응형 ---- */
@media (max-width: 1024px) {
  .ad-req-grid,
  .ad-notes-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .ad-reviews {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 840px) {
  .ad-grid2 {
    grid-template-columns: 1fr;
  }
  .ad-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  .ad-wordmark {
    display: none;
  }
  .ad-list-head {
    display: none;
  }
  .ad-row,
  .ad-row.mgr {
    grid-template-columns: 1fr auto;
    gap: 8px 12px;
    padding: 16px 20px;
  }
  .ad-row-progress,
  .ad-row-link,
  .ad-row .ad-tags,
  .ad-row .ad-mono {
    grid-column: 1 / -1;
  }
  .ad-row-arrow {
    display: none;
  }
}
@media (max-width: 640px) {
  .ad-main {
    padding-top: 32px;
  }
  .ad-h1 {
    font-size: 30px;
  }
  .ad-h2 {
    font-size: 24px;
  }
  .ad-menu-item span {
    display: none;
  }
  .ad-review-pill {
    padding: 0 12px;
  }
  .ad-review-text {
    display: none;
  }
  .ad-reviews,
  .ad-req-grid,
  .ad-notes-grid {
    grid-template-columns: 1fr;
  }
  .ad-stat-num {
    font-size: 32px;
  }
}
</style>
