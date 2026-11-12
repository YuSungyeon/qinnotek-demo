<script setup>
import { computed, nextTick, reactive, ref } from 'vue'

// 시안: 전화번호 조회 대신 "관리자가 만든 링크"로만 들어와 제출한다.
// design.md(모노크롬 · 스타디움 필 · 그림자 없는 틴트 계단)를 적용한 데모이며 서버와 연결되지 않는다.
import {
  Link2,
  Copy,
  Check,
  MessageSquare,
  RefreshCw,
  Link2Off,
  Camera,
  FileText,
  Upload,
  Clock,
  CircleCheck,
  TimerOff,
  Phone,
  ArrowRight,
  Eye,
  CornerDownRight
} from 'lucide-vue-next'

const tab = ref('admin') // 'admin' | 'customer'

// ---------------- 데모 데이터 ----------------
const COMPANY = '한빛정밀(주)'
const ITEMS = [
  { id: 1, name: '사업자등록증', type: 'doc', hint: 'PDF·사진 모두 가능' },
  { id: 2, name: '공장 외관', type: 'photo', hint: '정문이 보이게 촬영' },
  { id: 3, name: '생산 라인', type: 'photo', hint: '라인 전체가 나오게' },
  { id: 4, name: '대표 장비 명판', type: 'photo', hint: '글자가 읽히도록 가까이' }
]
const RETURNED = { 4: '명판 글자가 흐려서 읽을 수 없어요. 가까이서 다시 찍어주세요.' }

// ---------------- 관리자: 링크 발급 ----------------
const EXPIRY = [
  { key: 7, label: '7일' },
  { key: 30, label: '30일' },
  { key: 0, label: '제출 완료까지' }
]
const expiry = ref(30)
const link = ref(null) // { token, createdAt, expiresAt, lastOpenedAt, active }
const copied = ref(false)
const confirmReissue = ref(false)
const smsOpen = ref(false)
const smsTo = ref('')
const smsSent = ref('')

const origin = typeof window !== 'undefined' ? window.location.origin : 'https://example.com'
const linkUrl = computed(() => (link.value ? `${origin}/s/${link.value.token}` : ''))

function randomToken(n = 22) {
  const abc = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'
  const buf = new Uint32Array(n)
  crypto.getRandomValues(buf)
  return [...buf].map((v) => abc[v % abc.length]).join('')
}
function fmt(d) {
  if (!d) return '-'
  const p = (x) => String(x).padStart(2, '0')
  return `${d.getFullYear()}.${p(d.getMonth() + 1)}.${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
function issue() {
  const now = new Date()
  link.value = {
    token: randomToken(),
    createdAt: now,
    expiresAt: expiry.value ? new Date(now.getTime() + expiry.value * 86400000) : null,
    lastOpenedAt: null,
    active: true
  }
  copied.value = false
  confirmReissue.value = false
  smsSent.value = ''
}
async function copy() {
  try {
    await navigator.clipboard.writeText(linkUrl.value)
  } catch {
    /* 미리보기 환경에서 클립보드가 막혀 있어도 표시는 진행 */
  }
  copied.value = true
  setTimeout(() => (copied.value = false), 1600)
}
function onSmsInput(e) {
  const d = e.target.value.replace(/\D/g, '').slice(0, 11)
  let out = d
  if (d.length > 3 && d.length <= 7) out = `${d.slice(0, 3)}-${d.slice(3)}`
  else if (d.length > 7) out = `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7)}`
  smsTo.value = out
}
function sendSms() {
  const d = smsTo.value.replace(/\D/g, '')
  if (d.length < 10) return
  smsSent.value = `${d.slice(0, 3)}-****-${d.slice(-4)}로 링크를 보냈어요.`
  smsOpen.value = false
  smsTo.value = ''
}
function turnOff() {
  if (link.value) link.value.active = false
}
function simulateOpen() {
  if (link.value) link.value.lastOpenedAt = new Date()
  customerState.value = link.value?.active ? 'open' : 'expired'
  tab.value = 'customer'
  nextTick(() => customerSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
}
const customerSection = ref(null)

// ---------------- 고객: 링크로 접속 ----------------
const CUSTOMER_STATES = [
  { key: 'open', label: '처음 열었을 때' },
  { key: 'returned', label: '반환됐을 때' },
  { key: 'done', label: '제출 후' },
  { key: 'expired', label: '링크 만료' }
]
const customerState = ref('open')
const picked = reactive({})
const visibleItems = computed(() =>
  customerState.value === 'returned' ? ITEMS.filter((i) => RETURNED[i.id]) : ITEMS
)
const pickedCount = computed(() => visibleItems.value.filter((i) => picked[i.id]).length)
const allPicked = computed(() => pickedCount.value === visibleItems.value.length)

function setCustomerState(k) {
  customerState.value = k
  Object.keys(picked).forEach((id) => delete picked[id])
}
function togglePick(id) {
  if (picked[id]) delete picked[id]
  else picked[id] = true
}
function pickAll() {
  visibleItems.value.forEach((i) => (picked[i.id] = true))
}
function submitDemo() {
  if (!allPicked.value) return
  setCustomerState('done')
}

const FLOW = [
  { no: 1, title: '관리자가 링크를 만들어요.', body: '기업 상세에서 버튼 한 번. 기업마다 추측할 수 없는 고유 주소가 생깁니다.' },
  { no: 2, title: '문자나 메신저로 전달해요.', body: '복사해서 카카오톡에 붙이거나, 시스템에서 바로 문자를 보냅니다.' },
  { no: 3, title: '고객은 누르면 끝이에요.', body: '전화번호 입력 없이 기업명과 제출할 항목이 바로 열립니다.' }
]

const CHANGES = [
  {
    area: '데이터',
    before: 'company.phone_number (조회 키, unique)',
    after: 'company.access_token · token_expires_at · token_opened_at 추가. 전화번호는 문자 발송용으로만 남김.'
  },
  {
    area: '관리자 API',
    before: 'PUT /admin/companies/{id}/phone',
    after: 'POST /admin/companies/{id}/link (발급·재발급) · DELETE …/link (끄기)'
  },
  {
    area: '고객 API',
    before: 'POST /customer/lookup { phoneNumber }',
    after: 'GET /customer/links/{token} — 기업·항목·상태를 한 번에 반환'
  },
  {
    area: '업로드·제출',
    before: '/customer/items/{itemId}/photo — itemId만 알면 누구나 호출',
    after: '/customer/links/{token}/items/{itemId}/… — 토큰의 기업 소유 항목인지 서버가 확인'
  },
  {
    area: '고객 화면',
    before: '/ 에서 전화번호 입력 → 조회',
    after: '/s/{token} 진입 즉시 제출 화면. / 는 “받은 링크로 접속해주세요” 안내만.'
  }
]
</script>

<template>
  <div class="lk">
    <!-- 플로팅 내비 필 -->
    <header class="lk-navwrap">
      <nav class="lk-nav">
        <span class="lk-logo"><Link2 :size="18" :stroke-width="2.4" /></span>
        <span class="lk-wordmark">자료 제출 센터</span>
        <div class="lk-seg lk-nav-seg" role="tablist">
          <button :class="{ on: tab === 'admin' }" role="tab" @click="tab = 'admin'">관리자</button>
          <button :class="{ on: tab === 'customer' }" role="tab" @click="tab = 'customer'">고객</button>
        </div>
      </nav>
    </header>

    <main class="lk-main">
      <!-- 히어로 -->
      <section class="lk-hero">
        <span class="lk-eyebrow">시안 · 링크 접속 방식</span>
        <h1 class="lk-h1">링크 하나로 제출받으세요.</h1>
        <p class="lk-lead">
          고객은 전화번호를 입력하지 않습니다. 관리자가 보낸 링크를 누르면 그 기업의 제출 화면이 바로 열립니다.
        </p>
      </section>

      <!-- 흐름 -->
      <section class="lk-flow">
        <article v-for="f in FLOW" :key="f.no" class="lk-flow-card">
          <span class="lk-flow-no">{{ f.no }}</span>
          <h3 class="lk-h4">{{ f.title }}</h3>
          <p class="lk-body-sm muted">{{ f.body }}</p>
        </article>
      </section>

      <!-- ================= 관리자 ================= -->
      <section v-if="tab === 'admin'" class="lk-section">
        <div class="lk-section-head">
          <h2 class="lk-h2">관리자 · 기업 상세.</h2>
          <p class="lk-body muted">기존 ‘전화번호’ 카드가 ‘제출 링크’ 카드로 바뀝니다.</p>
        </div>

        <div class="lk-admin">
          <!-- 기업 헤더 -->
          <div class="lk-company">
            <span class="lk-squircle">한</span>
            <div class="lk-company-text">
              <div class="lk-h3">{{ COMPANY }}</div>
              <div class="lk-body-sm muted">요구 자료 4건 · 담당자 2명</div>
            </div>
            <span class="lk-chip">제출 대기</span>
          </div>

          <div class="lk-admin-grid">
            <!-- 제출 링크 카드 -->
            <article class="lk-card">
              <div class="lk-card-head">
                <h3 class="lk-h4">제출 링크</h3>
                <span v-if="link" class="lk-chip" :class="{ off: !link.active }">
                  <span class="dot"></span>{{ link.active ? '사용 중' : '꺼짐' }}
                </span>
              </div>

              <!-- 발급 전 -->
              <template v-if="!link">
                <p class="lk-body muted">
                  기업마다 링크는 1개만 유효합니다.
                </p>
                <div class="lk-field-label">유효 기간</div>
                <div class="lk-seg lk-seg-full">
                  <button v-for="e in EXPIRY" :key="e.key" :class="{ on: expiry === e.key }" @click="expiry = e.key">
                    {{ e.label }}
                  </button>
                </div>
                <button class="lk-btn lk-btn-primary lk-btn-block" @click="issue">
                  <Link2 :size="18" /> 링크 만들기
                </button>
              </template>

              <!-- 발급 후 -->
              <template v-else>
                <div class="lk-url" :class="{ off: !link.active }">
                  <span class="lk-url-text">{{ linkUrl }}</span>
                </div>

                <div class="lk-actions">
                  <button class="lk-btn lk-btn-primary" :disabled="!link.active" @click="copy">
                    <component :is="copied ? Check : Copy" :size="17" />
                    {{ copied ? '복사됨' : '링크 복사' }}
                  </button>
                  <button class="lk-btn lk-btn-outline" :disabled="!link.active" @click="smsOpen = !smsOpen">
                    <MessageSquare :size="17" /> 문자로 보내기
                  </button>
                </div>

                <div v-if="smsOpen" class="lk-sms">
                  <input
                    :value="smsTo"
                    class="lk-input"
                    type="tel"
                    inputmode="numeric"
                    placeholder="받는 사람 번호"
                    @input="onSmsInput"
                    @keyup.enter="sendSms"
                  />
                  <button class="lk-btn lk-btn-primary" @click="sendSms">보내기</button>
                </div>
                <p v-if="smsSent" class="lk-ok"><Check :size="16" /> {{ smsSent }}</p>

                <dl class="lk-meta">
                  <div><dt>만든 날</dt><dd>{{ fmt(link.createdAt) }}</dd></div>
                  <div><dt>만료</dt><dd>{{ link.expiresAt ? fmt(link.expiresAt) : '제출 완료 시' }}</dd></div>
                  <div><dt>마지막 접속</dt><dd>{{ link.lastOpenedAt ? fmt(link.lastOpenedAt) : '아직 열지 않음' }}</dd></div>
                </dl>

                <div v-if="confirmReissue" class="lk-confirm">
                  <p class="lk-body-sm">새 링크를 만들면 <b>지금 링크는 바로 쓸 수 없게 됩니다.</b> 고객에게 다시 보내야 해요.</p>
                  <div class="lk-actions">
                    <button class="lk-btn lk-btn-primary" @click="issue">새로 만들기</button>
                    <button class="lk-btn lk-btn-soft" @click="confirmReissue = false">취소</button>
                  </div>
                </div>

                <div v-else class="lk-actions lk-actions-soft">
                  <button class="lk-btn lk-btn-soft" @click="confirmReissue = true">
                    <RefreshCw :size="16" /> 재발급
                  </button>
                  <button v-if="link.active" class="lk-btn lk-btn-soft" @click="turnOff">
                    <Link2Off :size="16" /> 링크 끄기
                  </button>
                  <button class="lk-btn lk-btn-soft" @click="simulateOpen">
                    <Eye :size="16" /> 고객 화면 보기
                  </button>
                </div>
              </template>
            </article>

            <!-- 지정된 요구 자료 (참고) -->
            <article class="lk-card lk-card-soft">
              <div class="lk-card-head">
                <h3 class="lk-h4">지정된 자료</h3>
                <span class="lk-caption muted">링크로 열리는 항목</span>
              </div>
              <ul class="lk-req">
                <li v-for="i in ITEMS" :key="i.id">
                  <span class="lk-squircle sm">
                    <component :is="i.type === 'doc' ? FileText : Camera" :size="16" />
                  </span>
                  <span class="lk-req-name">{{ i.name }}</span>
                  <span class="lk-caption muted">{{ i.type === 'doc' ? '서류' : '사진' }}</span>
                </li>
              </ul>
            </article>
          </div>
        </div>
      </section>

      <!-- ================= 고객 ================= -->
      <section v-else ref="customerSection" class="lk-section lk-anchor">
        <div class="lk-section-head">
          <h2 class="lk-h2">고객 · 링크로 접속.</h2>
          <p class="lk-body muted">링크를 누르면 시작 화면 없이 바로 제출 화면입니다. 상태를 바꿔 보세요.</p>
          <div class="lk-seg lk-seg-wrap">
            <button
              v-for="s in CUSTOMER_STATES"
              :key="s.key"
              :class="{ on: customerState === s.key }"
              @click="setCustomerState(s.key)"
            >
              {{ s.label }}
            </button>
          </div>
        </div>

        <div class="lk-phone">
          <div class="lk-phone-bar">
            <span class="lk-phone-url">{{ origin.replace(/^https?:\/\//, '') }}/s/{{ link?.token || 'k7Qm2xVb9Rz4…' }}</span>
          </div>

          <div class="lk-screen">
            <div class="lk-appbar">
              <span class="lk-logo sm"><Link2 :size="15" :stroke-width="2.4" /></span>
              <span class="lk-appbar-name">자료 제출 센터</span>
            </div>

            <!-- 열었을 때 / 반환 -->
            <template v-if="customerState === 'open' || customerState === 'returned'">
              <div class="lk-c-head">
                <span class="lk-c-company">{{ COMPANY }}</span>
                <h3 class="lk-c-title">
                  {{ customerState === 'returned' ? '1건만 다시 올려주세요.' : `자료 ${ITEMS.length}건을 올려주세요.` }}
                </h3>
                <p v-if="customerState === 'returned'" class="lk-c-lead">나머지는 확인을 마쳤어요.</p>
              </div>

              <div class="lk-progress">
                <div class="lk-progress-track">
                  <div class="lk-progress-fill" :style="{ width: (pickedCount / visibleItems.length) * 100 + '%' }"></div>
                </div>
                <span class="lk-c-small muted">{{ visibleItems.length }}건 중 <b>{{ pickedCount }}건</b> 선택</span>
              </div>

              <button v-if="!allPicked" class="lk-bulk" @click="pickAll">
                <span class="lk-bulk-icon"><Upload :size="22" /></span>
                <span>
                  <b>한번에 올리기</b>
                  <small>사진과 서류를 함께 골라도 항목에 맞게 나눠 드려요</small>
                </span>
              </button>

              <ul class="lk-items">
                <li v-for="(i, idx) in visibleItems" :key="i.id" class="lk-item" :class="{ done: picked[i.id] }">
                  <div class="lk-item-row">
                    <span class="lk-item-thumb">
                      <component :is="picked[i.id] ? Check : i.type === 'doc' ? FileText : Camera" :size="24" />
                    </span>
                    <span class="lk-item-text">
                      <span class="lk-c-small muted">{{ idx + 1 }} · {{ i.type === 'doc' ? '서류' : '사진' }}</span>
                      <b>{{ i.name }}</b>
                      <small class="muted">{{ i.hint }}</small>
                    </span>
                    <button class="lk-btn lk-btn-sm" :class="picked[i.id] ? 'lk-btn-soft' : 'lk-btn-outline'" @click="togglePick(i.id)">
                      {{ picked[i.id] ? '바꾸기' : i.type === 'doc' ? '올리기' : '촬영' }}
                    </button>
                  </div>
                  <p v-if="customerState === 'returned' && RETURNED[i.id]" class="lk-return">
                    <CornerDownRight :size="16" /> {{ RETURNED[i.id] }}
                  </p>
                </li>
              </ul>

              <div class="lk-submitbar">
                <button class="lk-btn lk-btn-primary lk-btn-block lk-btn-lg" :disabled="!allPicked" @click="submitDemo">
                  {{ allPicked ? '전송하기' : `전송 (${pickedCount}/${visibleItems.length})` }}
                </button>
              </div>
            </template>

            <!-- 제출 후 -->
            <div v-else-if="customerState === 'done'" class="lk-state">
              <span class="lk-state-icon"><Clock :size="34" /></span>
              <span class="lk-c-company">{{ COMPANY }}</span>
              <h3 class="lk-c-title">제출이 완료되었어요.</h3>
              <p class="lk-c-lead">
                검수가 끝나면 문자로 알려드려요. 같은 링크로 다시 들어오면 진행 상황을 볼 수 있어요.
              </p>
              <ol class="lk-timeline">
                <li class="on"><CircleCheck :size="18" /> 제출 완료</li>
                <li class="now"><Clock :size="18" /> 검수 중</li>
                <li><CircleCheck :size="18" /> 통과</li>
              </ol>
            </div>

            <!-- 만료 -->
            <div v-else class="lk-state">
              <span class="lk-state-icon"><TimerOff :size="34" /></span>
              <h3 class="lk-c-title">이 링크는 더 이상 쓸 수 없어요.</h3>
              <p class="lk-c-lead">기간이 지났거나 새 링크로 바뀌었어요. 담당자에게 새 링크를 요청해주세요.</p>
              <a class="lk-btn lk-btn-outline lk-btn-block lk-btn-lg" href="tel:0212345678">
                <Phone :size="20" /> 담당자에게 전화하기
              </a>
            </div>
          </div>
        </div>
      </section>

      <!-- ================= 변경 사항 ================= -->
      <section class="lk-section">
        <div class="lk-section-head">
          <h2 class="lk-h2">무엇이 바뀌나요.</h2>
          <p class="lk-body muted">전화번호가 ‘조회 키’에서 ‘문자 받을 곳’으로 내려가고, 링크 토큰이 그 자리를 대신합니다.</p>
        </div>
        <div class="lk-table">
          <div class="lk-tr lk-th">
            <span>영역</span><span>지금</span><span>바뀐 뒤</span>
          </div>
          <div v-for="c in CHANGES" :key="c.area" class="lk-tr">
            <span class="lk-td-area">{{ c.area }}</span>
            <span class="lk-td-before muted">{{ c.before }}</span>
            <span class="lk-td-after"><ArrowRight :size="15" class="lk-td-arrow" />{{ c.after }}</span>
          </div>
        </div>

        <div class="lk-notes">
          <article class="lk-note">
            <h4 class="lk-title">링크가 곧 열쇠입니다.</h4>
            <p class="lk-body-sm muted">
              22자 무작위 토큰(약 128비트)이라 추측은 불가능하지만, 링크를 받은 사람은 누구나 제출할 수 있어요.
              그래서 유효 기간 · 재발급 · 끄기를 관리자에게 줍니다.
            </p>
          </article>
          <article class="lk-note">
            <h4 class="lk-title">기존 데이터는 그대로.</h4>
            <p class="lk-body-sm muted">
              등록된 전화번호는 지우지 않고 ‘문자로 보내기’의 기본 수신 번호로 씁니다. 배포 때 기존 기업마다 링크를 한 번에 만들 수 있어요.
            </p>
          </article>
        </div>
      </section>
    </main>

    <footer class="lk-footer">
      <div class="lk-footer-inner">
        <div class="lk-footer-mark">자료 제출 센터</div>
        <p class="lk-footer-tag">링크를 받은 기업만, 필요한 자료만.</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* ---- design.md 토큰 ---- */
.lk {
  --ink: #141414;
  --ink-soft: #262626;
  --canvas: #ffffff;
  --canvas-soft: #f3f3f3;
  --field: #f0f0f0;
  --hairline-soft: #f0f0f0;
  --hairline: #e0e0e0;
  --muted: #707070;
  --faint: #adadad;
  --on-primary: #ffffff;
  --r-sm: 16px;
  --r-md: 24px;
  --r-full: 9999px;
  --font: 'Pretendard Variable', Pretendard, -apple-system, 'Helvetica Neue', Arial, sans-serif;

  min-height: 100vh;
  background: var(--canvas);
  color: var(--ink);
  font-family: var(--font);
  font-weight: 456;
  letter-spacing: 0;
  word-break: keep-all; /* 한글이 글자 단위로 끊기지 않게 */
  overflow-wrap: break-word;
  display: flex;
  flex-direction: column;
}
.lk *,
.lk *::before,
.lk *::after {
  letter-spacing: 0;
}
.muted {
  color: var(--muted);
}

/* ---- 타이포 ---- */
.lk-h1 {
  font-size: 56px;
  font-weight: 652;
  line-height: 1.1;
  margin: 0;
}
.lk-h2 {
  font-size: 44px;
  font-weight: 652;
  line-height: 1.13;
  margin: 0;
}
.lk-h3 {
  font-size: 32px;
  font-weight: 652;
  line-height: 1.13;
}
.lk-h4 {
  font-size: 24px;
  font-weight: 652;
  line-height: 1.25;
  margin: 0;
}
.lk-title {
  font-size: 20px;
  font-weight: 600;
  line-height: 1.3;
  margin: 0 0 8px;
}
.lk-lead {
  font-size: 20px;
  font-weight: 300;
  line-height: 1.5;
  margin: 0;
  color: var(--ink);
}
.lk-body {
  font-size: 16px;
  line-height: 1.5;
  margin: 0;
}
.lk-body-sm {
  font-size: 14px;
  line-height: 1.5;
  margin: 0;
}
.lk-caption {
  font-size: 12px;
  line-height: 1.33;
}

/* ---- 내비 필 ---- */
.lk-navwrap {
  position: sticky;
  top: 0;
  z-index: 20;
  padding: 16px 16px 0;
  display: flex;
  justify-content: center;
  pointer-events: none;
}
.lk-nav {
  pointer-events: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  max-width: 720px;
  padding: 8px 8px 8px 10px;
  background: var(--canvas-soft);
  border-radius: var(--r-full);
}
.lk-logo {
  width: 34px;
  height: 34px;
  border-radius: 30%;
  background: var(--ink);
  color: var(--on-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.lk-logo.sm {
  width: 28px;
  height: 28px;
}
.lk-wordmark {
  font-size: 16px;
  font-weight: 652;
  white-space: nowrap;
}
.lk-nav-seg {
  margin-left: auto;
  background: var(--canvas);
}
.lk-nav .lk-nav-seg button.on {
  background: var(--ink);
  color: var(--on-primary);
  box-shadow: none;
}

/* ---- 세그먼트 ---- */
.lk-seg {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  background: var(--canvas-soft);
  border-radius: var(--r-full);
}
.lk-seg button {
  border: 0;
  background: transparent;
  color: var(--muted);
  font: inherit;
  font-size: 15px;
  font-weight: 600;
  min-height: 36px;
  padding: 0 16px;
  border-radius: var(--r-full);
  white-space: nowrap;
}
.lk-seg button.on {
  background: var(--canvas);
  color: var(--ink);
  box-shadow: 0 1px 3px rgba(20, 20, 20, 0.08); /* design.md 유일한 그림자 예외 */
}
.lk-seg-full {
  display: flex;
  margin-bottom: 20px;
}
.lk-seg-full button {
  flex: 1;
}
.lk-seg-wrap {
  margin-top: 20px;
  flex-wrap: wrap;
  border-radius: 24px;
}

/* ---- 버튼 ---- */
.lk-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 48px;
  padding: 0 20px;
  border-radius: var(--r-full);
  border: 1px solid transparent;
  font: inherit;
  font-size: 16px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  text-decoration: none;
  transition: background 0.15s, opacity 0.15s;
}
.lk-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.lk-btn-primary {
  background: var(--ink);
  color: var(--on-primary);
}
.lk-btn-primary:not(:disabled):hover {
  background: var(--ink-soft);
}
.lk-btn-outline {
  background: var(--canvas);
  color: var(--ink);
  border-color: var(--hairline);
}
.lk-btn-outline:not(:disabled):hover {
  background: var(--canvas-soft);
}
.lk-btn-soft {
  background: var(--canvas-soft);
  color: var(--ink);
}
.lk-btn-soft:hover {
  background: #e9e9e9;
}
.lk-btn-block {
  width: 100%;
}
.lk-btn-sm {
  min-height: 40px;
  padding: 0 16px;
  font-size: 15px;
}
.lk-btn-lg {
  min-height: 56px;
  font-size: 18px;
}

/* ---- 레이아웃 ---- */
.lk-main {
  width: 100%;
  max-width: 1040px;
  margin: 0 auto;
  padding: 0 16px 120px;
  flex: 1;
}
.lk-hero {
  text-align: center;
  padding: 96px 0 56px;
  max-width: 760px;
  margin: 0 auto;
}
.lk-eyebrow {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: var(--r-full);
  background: var(--canvas-soft);
  margin-bottom: 24px;
}
.lk-hero .lk-lead {
  margin-top: 20px;
}

.lk-flow {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.lk-flow-card {
  border: 1px solid var(--hairline-soft);
  border-radius: var(--r-md);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.lk-flow-no {
  width: 32px;
  height: 32px;
  border-radius: 30%;
  background: var(--ink);
  color: var(--on-primary);
  font-size: 14px;
  font-weight: 652;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6px;
}
.lk-flow-card .lk-h4 {
  font-size: 20px;
}

.lk-section {
  padding-top: 120px;
}
.lk-anchor {
  scroll-margin-top: -40px; /* 내비 필 아래로 섹션 제목이 오도록 */
}
.lk-section-head {
  text-align: center;
  max-width: 680px;
  margin: 0 auto 40px;
}
.lk-section-head .lk-body {
  margin-top: 12px;
}

/* ---- 관리자 ---- */
.lk-admin {
  max-width: 880px;
  margin: 0 auto;
}
.lk-company {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.lk-company-text {
  flex: 1;
  min-width: 0;
}
.lk-squircle {
  width: 56px;
  height: 56px;
  border-radius: 30%;
  background: var(--ink);
  color: var(--on-primary);
  font-size: 22px;
  font-weight: 652;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.lk-squircle.sm {
  width: 32px;
  height: 32px;
  background: var(--canvas);
  color: var(--ink);
}
.lk-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  padding: 7px 12px;
  border-radius: var(--r-full);
  background: var(--canvas-soft);
  color: var(--ink);
  white-space: nowrap;
}
.lk-chip .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ink);
}
.lk-chip.off {
  color: var(--muted);
}
.lk-chip.off .dot {
  background: var(--faint);
}

.lk-admin-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
  align-items: start;
}
.lk-card {
  border: 1px solid var(--hairline-soft);
  border-radius: var(--r-md);
  padding: 24px;
  background: var(--canvas);
}
.lk-card-soft {
  background: var(--canvas-soft);
  border-color: transparent;
}
.lk-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.lk-card > .lk-body {
  margin-bottom: 24px;
}
.lk-field-label {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.lk-url {
  background: var(--field);
  border-radius: var(--r-sm);
  padding: 14px 16px;
  font-size: 15px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  word-break: break-all;
  line-height: 1.45;
}
.lk-url.off .lk-url-text {
  color: var(--faint);
  text-decoration: line-through;
}
.lk-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.lk-actions > .lk-btn-primary,
.lk-actions > .lk-btn-outline {
  flex: 1 1 160px;
}
.lk-actions-soft {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--canvas-soft);
}
.lk-actions-soft .lk-btn {
  min-height: 40px;
  font-size: 14px;
  padding: 0 14px;
}

.lk-sms {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.lk-input {
  flex: 1;
  min-width: 0;
  background: var(--field);
  border: 0;
  border-radius: var(--r-sm);
  padding: 12px 16px;
  font: inherit;
  font-size: 16px;
  color: var(--ink);
  outline: none;
}
.lk-input::placeholder {
  color: var(--faint);
}
.lk-input:focus {
  box-shadow: 0 0 0 2px var(--ink);
}
.lk-sms-note {
  margin: 10px 4px 0;
}
.lk-ok {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  margin: 12px 0 0;
}

.lk-meta {
  margin: 20px 0 0;
  display: grid;
  gap: 8px;
}
.lk-meta div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
}
.lk-meta dt {
  color: var(--muted);
}
.lk-meta dd {
  margin: 0;
  font-variant-numeric: tabular-nums;
  text-align: right;
}
.lk-confirm {
  margin-top: 20px;
  padding: 16px;
  background: var(--canvas-soft);
  border-radius: var(--r-sm);
}

.lk-req {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}
.lk-req li {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
}
.lk-req-name {
  flex: 1;
  font-weight: 600;
}

/* ---- 고객 휴대폰 ---- */
.lk-phone {
  max-width: 400px;
  margin: 0 auto;
  border: 1px solid var(--hairline);
  border-radius: 44px;
  padding: 10px;
  background: var(--canvas);
}
.lk-phone-bar {
  background: var(--canvas-soft);
  border-radius: var(--r-full);
  padding: 10px 16px;
  text-align: center;
  margin: 4px 4px 0;
}
.lk-phone-url {
  font-size: 12px;
  color: var(--muted);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.lk-screen {
  min-height: 640px;
  padding: 8px 14px 14px;
  display: flex;
  flex-direction: column;
}
.lk-appbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 2px 20px;
}
.lk-appbar-name {
  font-size: 15px;
  font-weight: 652;
}

/* 고객 화면은 기존처럼 큰 글씨를 유지 (어르신 사용자 고려) */
.lk-c-company {
  display: inline-block;
  font-size: 14px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: var(--r-full);
  background: var(--canvas-soft);
}
.lk-c-title {
  font-size: 30px;
  font-weight: 652;
  line-height: 1.2;
  margin: 14px 0 8px;
}
.lk-c-lead {
  font-size: 18px;
  font-weight: 300;
  line-height: 1.5;
  margin: 0;
}
.lk-c-small {
  font-size: 13px;
}
.lk-c-head {
  margin-bottom: 20px;
}

.lk-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.lk-progress-track {
  flex: 1;
  height: 6px;
  border-radius: var(--r-full);
  background: var(--canvas-soft);
  overflow: hidden;
}
.lk-progress-fill {
  height: 100%;
  background: var(--ink);
  border-radius: var(--r-full);
  transition: width 0.25s;
}

.lk-bulk {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 14px;
  text-align: left;
  border: 0;
  background: var(--canvas-soft);
  border-radius: var(--r-md);
  padding: 18px;
  margin-bottom: 12px;
  font: inherit;
  color: var(--ink);
  cursor: pointer;
}
.lk-bulk b {
  display: block;
  font-size: 19px;
  font-weight: 652;
}
.lk-bulk small {
  display: block;
  font-size: 14px;
  color: var(--muted);
  margin-top: 4px;
  line-height: 1.4;
}
.lk-bulk-icon {
  width: 48px;
  height: 48px;
  border-radius: 30%;
  background: var(--ink);
  color: var(--on-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.lk-items {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}
.lk-item {
  border: 1px solid var(--hairline-soft);
  border-radius: var(--r-sm);
  padding: 14px;
}
.lk-item.done {
  background: var(--canvas-soft);
  border-color: transparent;
}
.lk-item-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.lk-item-thumb {
  width: 52px;
  height: 52px;
  border-radius: 30%;
  background: var(--field);
  color: var(--muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.lk-item.done .lk-item-thumb {
  background: var(--ink);
  color: var(--on-primary);
}
.lk-item-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.lk-item-text b {
  font-size: 19px;
  font-weight: 652;
  line-height: 1.25;
}
.lk-item-text small {
  font-size: 14px;
}
.lk-return {
  display: flex;
  gap: 6px;
  align-items: flex-start;
  margin: 12px 0 0;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--canvas);
  font-size: 15px;
  line-height: 1.45;
}
.lk-item:not(.done) .lk-return {
  background: var(--canvas-soft);
}
.lk-return svg {
  flex: 0 0 auto;
  margin-top: 2px;
}
.lk-submitbar {
  margin-top: auto;
  padding-top: 20px;
}

.lk-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 48px 8px 8px;
}
.lk-state-icon {
  width: 80px;
  height: 80px;
  border-radius: 30%;
  background: var(--canvas-soft);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}
.lk-state .lk-c-title {
  margin-top: 16px;
}
.lk-state .lk-btn {
  margin-top: auto;
}
.lk-timeline {
  list-style: none;
  padding: 0;
  margin: 32px 0 0;
  width: 100%;
  display: grid;
  gap: 8px;
  text-align: left;
}
.lk-timeline li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: var(--r-sm);
  font-size: 17px;
  color: var(--faint);
  border: 1px solid var(--hairline-soft);
}
.lk-timeline li.on {
  color: var(--ink);
}
.lk-timeline li.now {
  color: var(--ink);
  background: var(--canvas-soft);
  border-color: transparent;
  font-weight: 652;
}

/* ---- 변경 표 ---- */
.lk-table {
  border: 1px solid var(--hairline-soft);
  border-radius: var(--r-md);
  overflow: hidden;
}
.lk-tr {
  display: grid;
  grid-template-columns: 120px 1fr 1.4fr;
  gap: 16px;
  padding: 18px 24px;
  font-size: 14px;
  line-height: 1.5;
  border-top: 1px solid var(--canvas-soft);
}
.lk-th {
  border-top: 0;
  background: var(--canvas-soft);
  font-size: 12px;
  font-weight: 600;
  color: var(--muted);
  padding-top: 14px;
  padding-bottom: 14px;
}
.lk-td-area {
  font-weight: 652;
}
.lk-td-before {
  word-break: break-word;
}
.lk-td-after {
  word-break: break-word;
}
.lk-td-arrow {
  vertical-align: -2px;
  margin-right: 6px;
  color: var(--muted);
}
.lk-notes {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 16px;
}
.lk-note {
  background: var(--canvas-soft);
  border-radius: var(--r-md);
  padding: 24px;
}

/* ---- 푸터 (극성 반전) ---- */
.lk-footer {
  background: var(--ink);
  color: var(--on-primary);
  border-radius: var(--r-md) var(--r-md) 0 0;
}
.lk-footer-inner {
  max-width: 1040px;
  margin: 0 auto;
  padding: 64px 24px 80px;
}
.lk-footer-mark {
  font-size: 56px;
  font-weight: 652;
  line-height: 1.1;
}
.lk-footer-tag {
  color: var(--faint);
  font-size: 16px;
  margin: 12px 0 0;
}

/* ---- 반응형 ---- */
@media (max-width: 840px) {
  .lk-flow,
  .lk-admin-grid,
  .lk-notes {
    grid-template-columns: 1fr;
  }
  .lk-tr {
    grid-template-columns: 1fr;
    gap: 4px;
  }
  .lk-th {
    display: none;
  }
}
@media (max-width: 640px) {
  .lk-hero {
    padding: 64px 0 40px;
  }
  .lk-h1 {
    font-size: 40px;
  }
  .lk-h2 {
    font-size: 32px;
  }
  .lk-h3 {
    font-size: 24px;
  }
  .lk-lead {
    font-size: 18px;
  }
  .lk-section {
    padding-top: 80px;
  }
  .lk-wordmark {
    display: none;
  }
  .lk-seg-wrap {
    display: grid;
    grid-template-columns: 1fr 1fr;
    width: 100%;
  }
  .lk-card,
  .lk-flow-card,
  .lk-note {
    padding: 20px;
  }
  .lk-tr {
    padding: 16px 20px;
  }
  .lk-footer-mark {
    font-size: 36px;
  }
  .lk-phone {
    border: 0;
    padding: 0;
    border-radius: 0;
  }
  .lk-screen {
    padding: 8px 0 0;
  }
}
</style>
