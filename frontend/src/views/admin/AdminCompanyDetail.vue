<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Camera,
  Check,
  ChevronLeft,
  Copy,
  CornerDownRight,
  Download,
  Eye,
  FileText,
  Info,
  Link2,
  Link2Off,
  MessageSquare,
  Pencil,
  RefreshCw,
  Trash2,
  Users
} from 'lucide-vue-next'
import { adminApi } from '../../api/admin'
import { absoluteUrl } from '../../api/client'
import { companyChip, itemChip } from '../../lib/status'
import { dateTime as fmtDate, phone as formatPhone } from '../../lib/format'

const props = defineProps({ id: { type: String, required: true } })
const router = useRouter()

const company = ref(null)
const requirements = ref([])
const managers = ref([])
const submission = ref(null)
const loading = ref(true)
const error = ref('')

// 담당자 지정 모달
const showManagerModal = ref(false)
const selectedManagerIds = ref([])
const managerKeyword = ref('')
const savingMgr = ref(false)
const filteredManagers = computed(() => {
  const k = managerKeyword.value.trim().toLowerCase()
  if (!k) return managers.value
  return managers.value.filter((m) =>
    [m.name, m.position, m.phoneNumber].some((v) => (v || '').toLowerCase().includes(k))
  )
})
const assignedManagerNames = computed(() =>
  managers.value.filter((m) => selectedManagerIds.value.includes(m.id)).map((m) => m.name)
)

// 제출 링크
const LINK_EXPIRY = [
  { days: 7, label: '7일' },
  { days: 30, label: '30일' },
  { days: null, label: '기한 없음' }
]
const linkDays = ref(30)
const linkBusy = ref(false)
const linkCopied = ref(false)
const confirmReissue = ref(false)
const smsOpen = ref(false)
const smsTo = ref('')
const smsSending = ref(false)
const smsMsg = ref('')
const link = computed(() => company.value?.link || null)
const linkUrl = computed(() => (link.value ? `${window.location.origin}/s/${link.value.token}` : ''))

// 요청 자료 지정
const selectedReqIds = ref([])
const savingReq = ref(false)
const reqMsg = ref('')

// 반환 모달
const returnTarget = ref(null)
const returnReason = ref('')
const returnConfirmed = ref(false)
const processing = ref(false)

// 기업명 수정 모달
const showNameModal = ref(false)
const nameInput = ref('')
const savingName = ref(false)
const nameError = ref('')

// 기업 삭제 모달
const showDeleteModal = ref(false)
const deleting = ref(false)
const deleteError = ref('')

// 요청 자료 자세히 모달
const reqDetail = ref(null)

// 항목별 추가 설명 모달
const noteTarget = ref(null)
const noteText = ref('')
const savingNote = ref(false)

// 이미지 라이트박스
const lightbox = ref('')

const companyId = computed(() => Number(props.id))
const approvedCount = computed(
  () => (submission.value?.items || []).filter((i) => i.status === 'APPROVED' && i.fileUrl).length
)
const zipUrl = computed(() =>
  absoluteUrl(`/api/admin/companies/${companyId.value}/submissions/approved-zip`)
)
// 워드/한글 원본 없이 PDF 사본만 받고 싶을 때
const zipPdfUrl = computed(() => zipUrl.value + '?scope=pdf')
const hasDocument = computed(() =>
  (submission.value?.items || []).some((i) => i.contentKind === 'DOCUMENT')
)

async function loadAll() {
  loading.value = true
  error.value = ''
  try {
    const [detail, reqs, mgrs, subs] = await Promise.all([
      adminApi.getCompany(companyId.value),
      adminApi.listRequirements(),
      adminApi.listManagers(),
      adminApi.getSubmissions(companyId.value)
    ])
    company.value = detail
    requirements.value = reqs
    managers.value = mgrs
    submission.value = subs
    smsTo.value = formatPhone(detail.phoneNumber || '')
    selectedReqIds.value = [...detail.assignedRequirementIds]
    selectedManagerIds.value = [...(detail.assignedManagerIds || [])]
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

/** 발급/재발급 - 재발급하면 이전 링크는 서버에서 즉시 무효 */
async function issueLink() {
  linkBusy.value = true
  error.value = ''
  try {
    company.value = await adminApi.issueLink(companyId.value, linkDays.value)
    confirmReissue.value = false
    smsMsg.value = ''
  } catch (err) {
    error.value = err.message
  } finally {
    linkBusy.value = false
  }
}
async function revokeLink() {
  linkBusy.value = true
  error.value = ''
  try {
    company.value = await adminApi.revokeLink(companyId.value)
    smsOpen.value = false
    smsMsg.value = ''
  } catch (err) {
    error.value = err.message
  } finally {
    linkBusy.value = false
  }
}
async function copyLink() {
  try {
    await navigator.clipboard.writeText(linkUrl.value)
  } catch {
    // 클립보드 권한이 없는 환경 - 직접 선택해 복사하도록 안내
    window.prompt('아래 링크를 복사하세요', linkUrl.value)
    return
  }
  linkCopied.value = true
  setTimeout(() => (linkCopied.value = false), 1600)
}
async function sendLinkSms() {
  const digits = smsTo.value.replace(/\D/g, '')
  if (digits.length < 10) {
    smsMsg.value = '받는 번호를 확인해주세요.'
    return
  }
  smsSending.value = true
  smsMsg.value = ''
  try {
    await adminApi.sendLinkSms(companyId.value, digits, linkUrl.value)
    smsMsg.value = `${formatPhone(digits)}로 보냈습니다.`
    smsOpen.value = false
  } catch (err) {
    smsMsg.value = err.message
  } finally {
    smsSending.value = false
  }
}

function toggleReq(reqId) {
  const i = selectedReqIds.value.indexOf(reqId)
  if (i === -1) selectedReqIds.value.push(reqId)
  else selectedReqIds.value.splice(i, 1)
}

async function saveRequirements() {
  savingReq.value = true
  reqMsg.value = ''
  error.value = ''
  try {
    company.value = await adminApi.assignRequirements(companyId.value, selectedReqIds.value)
    submission.value = await adminApi.getSubmissions(companyId.value)
    reqMsg.value = '저장되었습니다.'
    setTimeout(() => (reqMsg.value = ''), 2000)
  } catch (err) {
    error.value = err.message
  } finally {
    savingReq.value = false
  }
}

function openManagerModal() {
  selectedManagerIds.value = [...(company.value.assignedManagerIds || [])]
  managerKeyword.value = ''
  showManagerModal.value = true
}
function toggleManager(id) {
  const i = selectedManagerIds.value.indexOf(id)
  if (i === -1) selectedManagerIds.value.push(id)
  else selectedManagerIds.value.splice(i, 1)
}
async function saveManagers() {
  savingMgr.value = true
  error.value = ''
  try {
    company.value = await adminApi.assignManagers(companyId.value, selectedManagerIds.value)
    showManagerModal.value = false
  } catch (err) {
    error.value = err.message
  } finally {
    savingMgr.value = false
  }
}

async function approve(item) {
  processing.value = true
  try {
    await adminApi.approve(item.itemId)
    await refreshSubmissions()
  } catch (err) {
    error.value = err.message
  } finally {
    processing.value = false
  }
}

function openReturn(item) {
  returnTarget.value = item
  returnReason.value = ''
  returnConfirmed.value = false
}
function closeReturn() {
  returnTarget.value = null
}
async function confirmReturn() {
  if (!returnReason.value.trim()) return
  processing.value = true
  try {
    await adminApi.markReturned(returnTarget.value.itemId, returnReason.value.trim())
    closeReturn()
    await refreshSubmissions()
  } catch (err) {
    error.value = err.message
  } finally {
    processing.value = false
  }
}

async function refreshSubmissions() {
  submission.value = await adminApi.getSubmissions(companyId.value)
  company.value = await adminApi.getCompany(companyId.value)
}

// --- 기업명 수정 ---
function openNameEdit() {
  nameInput.value = company.value.name
  nameError.value = ''
  showNameModal.value = true
}
async function saveName() {
  if (!nameInput.value.trim()) return
  savingName.value = true
  nameError.value = ''
  try {
    company.value = await adminApi.updateName(companyId.value, nameInput.value.trim())
    showNameModal.value = false
  } catch (err) {
    nameError.value = err.message
  } finally {
    savingName.value = false
  }
}

// --- 기업 삭제 ---
async function confirmDelete() {
  deleting.value = true
  deleteError.value = ''
  try {
    await adminApi.deleteCompany(companyId.value)
    router.push('/admin/companies')
  } catch (err) {
    deleteError.value = err.message
    deleting.value = false
  }
}

// --- 항목별 추가 설명 ---
function openNote(item) {
  noteTarget.value = item
  noteText.value = item.adminNote || ''
}
async function saveNote() {
  savingNote.value = true
  error.value = ''
  try {
    await adminApi.updateItemNote(noteTarget.value.itemId, noteText.value.trim())
    noteTarget.value = null
    await refreshSubmissions()
  } catch (err) {
    error.value = err.message
  } finally {
    savingNote.value = false
  }
}

onMounted(loadAll)
</script>

<template>
  <div v-if="loading" class="muted">불러오는 중…</div>
  <div v-else-if="company">
    <button class="btn btn-soft btn-sm back" @click="router.push('/admin/companies')">
      <ChevronLeft :size="16" /> 기업 목록
    </button>

    <!-- 머리: 기업명 + 상태 + 관리 동작 -->
    <div class="page-head">
      <div class="title">
        <span class="squircle title-mark">{{ company.name.slice(0, 1) }}</span>
        <div>
          <h1>{{ company.name }}</h1>
          <p class="title-meta">
            <span class="chip" :class="companyChip(company.status)">{{ company.statusLabel }}</span>
            <span class="muted">
              요청 자료 {{ company.assignedRequirementIds.length }}건 · 담당자
              {{ (company.assignedManagerIds || []).length }}명
            </span>
          </p>
        </div>
      </div>
      <div class="head-actions">
        <button class="btn btn-ghost btn-sm" @click="openManagerModal">
          <Users :size="15" /> 알림 담당자 {{ (company.assignedManagerIds || []).length }}
        </button>
        <button class="btn btn-ghost btn-sm" @click="openNameEdit"><Pencil :size="15" /> 이름 수정</button>
        <!-- 위험 동작은 색 대신 확인 창으로 구분한다 -->
        <button class="btn btn-soft btn-sm" @click="deleteError = ''; showDeleteModal = true">
          <Trash2 :size="15" /> 삭제
        </button>
      </div>
    </div>

    <div v-if="error" class="alert" style="margin-bottom: 16px">{{ error }}</div>

    <div class="grid">
      <!-- 제출 링크 (강조 카드: 연회색 바탕) -->
      <section class="card card-soft">
        <div class="card-head">
          <h2 class="ctitle">제출 링크</h2>
          <span v-if="link" class="chip link-chip" :class="link.expired ? 'chip-none' : ''">
            <span class="dot"></span>{{ link.expired ? '만료됨' : '사용 중' }}
          </span>
        </div>

        <!-- 아직 없음 / 재발급 확인 -->
        <template v-if="!link || confirmReissue">
          <p class="muted small">
            <template v-if="confirmReissue">
              새로 만들면 <b>지금 링크는 바로 쓸 수 없게 됩니다.</b> 고객에게 다시 보내야 해요.
            </template>
            <template v-else>기업마다 링크는 1개만 유효합니다.</template>
          </p>
          <div class="seg seg-full on-soft">
            <button
              v-for="e in LINK_EXPIRY"
              :key="e.label"
              :class="{ on: linkDays === e.days }"
              @click="linkDays = e.days"
            >
              {{ e.label }}
            </button>
          </div>
          <div class="link-actions">
            <button class="btn" :disabled="linkBusy" @click="issueLink">
              <Link2 :size="16" /> {{ confirmReissue ? '새로 만들기' : '링크 만들기' }}
            </button>
            <button v-if="confirmReissue" class="btn btn-ghost" @click="confirmReissue = false">취소</button>
          </div>
        </template>

        <!-- 발급됨 -->
        <template v-else>
          <div class="link-url" :class="{ off: link.expired }">{{ linkUrl }}</div>
          <div class="link-actions">
            <button class="btn" :disabled="link.expired" @click="copyLink">
              <component :is="linkCopied ? Check : Copy" :size="16" /> {{ linkCopied ? '복사됨' : '링크 복사' }}
            </button>
            <button class="btn btn-ghost" :disabled="link.expired" @click="smsOpen = !smsOpen">
              <MessageSquare :size="16" /> 문자로 보내기
            </button>
          </div>
          <div v-if="smsOpen" class="row">
            <input
              :value="smsTo"
              class="input on-soft-input"
              type="tel"
              inputmode="numeric"
              placeholder="받는 번호"
              @input="smsTo = formatPhone($event.target.value)"
              @keyup.enter="sendLinkSms"
            />
            <button class="btn" :disabled="smsSending" @click="sendLinkSms">보내기</button>
          </div>
          <p v-if="smsMsg" class="small link-msg">{{ smsMsg }}</p>

          <dl class="link-meta">
            <div><dt>만든 날</dt><dd>{{ fmtDate(link.issuedAt) }}</dd></div>
            <div><dt>만료</dt><dd>{{ link.expiresAt ? fmtDate(link.expiresAt) : '기한 없음' }}</dd></div>
            <div><dt>마지막 접속</dt><dd>{{ link.openedAt ? fmtDate(link.openedAt) : '아직 열지 않음' }}</dd></div>
          </dl>
          <div class="link-sub">
            <button class="btn-text" :disabled="linkBusy" @click="confirmReissue = true">
              <RefreshCw :size="15" /> 재발급
            </button>
            <button class="btn-text" :disabled="linkBusy" @click="revokeLink"><Link2Off :size="15" /> 링크 끄기</button>
            <a class="btn-text" :href="linkUrl" target="_blank" rel="noopener"><Eye :size="15" /> 고객 화면</a>
          </div>
        </template>
      </section>

      <!-- 요청 자료 지정: 누르면 켜지고 꺼지는 알약. i 버튼으로 설명·예시 보기 -->
      <section class="card">
        <div class="card-head">
          <h2 class="ctitle">요청 자료</h2>
          <span class="muted small">{{ selectedReqIds.length }}건 선택</span>
        </div>
        <p v-if="requirements.length === 0" class="muted small">
          등록된 요청 자료가 없습니다.
          <RouterLink to="/admin/requirements" class="text-link">요청 자료 관리 →</RouterLink>
        </p>
        <div v-else class="toggles">
          <span v-for="r in requirements" :key="r.id" class="toggle" :class="{ on: selectedReqIds.includes(r.id) }">
            <button class="toggle-main" :aria-pressed="selectedReqIds.includes(r.id)" @click="toggleReq(r.id)">
              <Check v-if="selectedReqIds.includes(r.id)" :size="14" />
              {{ r.name }}
            </button>
            <button class="toggle-info" :aria-label="`${r.name} 자세히`" @click="reqDetail = r">
              <Info :size="14" />
            </button>
          </span>
        </div>
        <div class="card-foot">
          <span v-if="reqMsg" class="small">{{ reqMsg }}</span>
          <button class="btn btn-sm" :disabled="savingReq" @click="saveRequirements">저장</button>
        </div>
      </section>
    </div>

    <!-- 제출 자료 검수 -->
    <section class="review-section">
      <div class="section-head">
        <h2 class="section-title">제출 자료 검수</h2>
        <div class="head-actions">
          <span v-if="submission?.latestSubmittedAt" class="muted small">
            최근 제출 {{ fmtDate(submission.latestSubmittedAt) }}
          </span>
          <a v-if="approvedCount > 0" class="btn btn-ghost btn-sm" :href="zipUrl" download>
            <Download :size="15" /> 완료본 ZIP ({{ approvedCount }})
          </a>
          <a v-if="approvedCount > 0 && hasDocument" class="btn btn-soft btn-sm" :href="zipPdfUrl" download>
            PDF만 받기
          </a>
        </div>
      </div>

      <p v-if="!submission || submission.items.length === 0" class="muted">지정된 제출 항목이 없습니다.</p>
      <div v-else class="reviews">
        <!-- 검수 대기 카드만 파란 테두리 - 지금 결정할 것 -->
        <article
          v-for="item in submission.items"
          :key="item.itemId"
          class="review"
          :class="{ waiting: item.status === 'SUBMITTED' }"
        >
          <!-- 문서: PDF 사본을 바로 본다 / 사진: 누르면 확대 -->
          <div v-if="item.contentKind === 'DOCUMENT'" class="media doc">
            <iframe
              v-if="item.pdfUrl"
              :src="absoluteUrl(item.pdfUrl) + '#toolbar=1&view=FitH'"
              class="pdf-frame"
              :title="item.displayName"
            ></iframe>
            <div v-else class="media-empty">
              <FileText :size="28" />
              <small>PDF 변환 실패 · 원본만 확인 가능</small>
            </div>
            <span class="overlay">문서</span>
          </div>
          <div
            v-else
            class="media"
            :class="{ zoomable: item.fileUrl }"
            @click="item.fileUrl && (lightbox = absoluteUrl(item.fileUrl))"
          >
            <img v-if="item.fileUrl" :src="absoluteUrl(item.fileUrl)" :alt="item.requirementName" />
            <div v-else class="media-empty">
              <component :is="item.fileType === 'DOCUMENT' ? FileText : Camera" :size="28" />
              <small>미제출</small>
            </div>
            <span v-if="item.fileUrl" class="overlay">사진</span>
          </div>

          <div class="review-body">
            <div class="review-title">
              <b>{{ item.requirementName }}</b>
              <span class="chip" :class="itemChip(item.status)">{{ item.statusLabel }}</span>
            </div>

            <p v-if="item.status === 'RETURNED' && item.rejectReason" class="note-box">
              <CornerDownRight :size="14" /> <span><b>반환 사유</b> {{ item.rejectReason }}</span>
            </p>
            <p v-if="item.adminNote" class="note-box">
              <Info :size="14" /> <span><b>추가 설명</b> {{ item.adminNote }}</span>
            </p>

            <div v-if="item.contentKind === 'DOCUMENT'" class="doc-links">
              <a class="btn-text" :href="absoluteUrl(item.originalDownloadUrl)" download>
                <Download :size="14" /> 원본
              </a>
              <a v-if="item.pdfDownloadUrl" class="btn-text" :href="absoluteUrl(item.pdfDownloadUrl)" download>
                <Download :size="14" /> PDF
              </a>
              <span class="doc-name faint small">{{ item.originalFileName }}</span>
            </div>

            <div v-if="item.status === 'SUBMITTED'" class="review-actions">
              <button class="btn btn-sm" :disabled="processing" @click="approve(item)">
                <Check :size="15" /> 통과
              </button>
              <button class="btn btn-ghost btn-sm" :disabled="processing" @click="openReturn(item)">반환</button>
            </div>
            <p v-else class="review-state muted small">
              {{ item.status === 'APPROVED' ? '승인 완료' : item.status === 'RETURNED' ? '고객 재제출 대기' : '고객 제출 대기' }}
            </p>
            <button class="btn-text note-btn" @click="openNote(item)">
              <Pencil :size="13" /> {{ item.adminNote ? '추가 설명 수정' : '추가 설명 작성' }}
            </button>
          </div>
        </article>
      </div>
    </section>

    <!-- 반환 확인 창: 사유 필수 + 확인 체크 -->
    <div v-if="returnTarget" class="modal-mask" @click.self="closeReturn">
      <div class="modal">
        <h3>'{{ returnTarget.requirementName }}'을(를) 반환할까요?</h3>
        <p class="muted small">고객에게 사유가 보이고, 올린 파일은 지워집니다. 고객은 이 항목만 다시 올립니다.</p>
        <label class="label" for="return-reason">반환 사유 (필수)</label>
        <textarea
          id="return-reason"
          v-model="returnReason"
          class="input"
          rows="3"
          placeholder="예: 사진이 너무 어둡습니다."
        ></textarea>
        <label class="confirm-check">
          <input v-model="returnConfirmed" type="checkbox" />
          <span>위 내용을 확인했습니다.</span>
        </label>
        <div class="modal-actions">
          <button class="btn btn-soft" @click="closeReturn">취소</button>
          <button
            class="btn"
            :disabled="!returnReason.trim() || !returnConfirmed || processing"
            @click="confirmReturn"
          >
            반환하기
          </button>
        </div>
      </div>
    </div>

    <!-- 알림 담당자 지정 -->
    <div v-if="showManagerModal" class="modal-mask" @click.self="showManagerModal = false">
      <div class="modal">
        <h3>알림 담당자</h3>
        <p class="muted small">{{ company.name }}에서 제출하면 문자를 받을 사람을 고르세요.</p>

        <p v-if="managers.length === 0" class="muted small">
          등록된 담당자가 없습니다.
          <RouterLink to="/admin/managers" class="text-link">담당자 관리 →</RouterLink>
        </p>
        <template v-else>
          <input v-model="managerKeyword" class="input" placeholder="이름·직책·전화번호 검색" />
          <p v-if="assignedManagerNames.length" class="muted small picked">
            선택됨: {{ assignedManagerNames.join(', ') }}
          </p>
          <div class="mgr-list">
            <label v-for="m in filteredManagers" :key="m.id" class="mgr-opt" :class="{ on: selectedManagerIds.includes(m.id) }">
              <input type="checkbox" :checked="selectedManagerIds.includes(m.id)" @change="toggleManager(m.id)" />
              <span class="avatar">{{ m.name.slice(0, 1) }}</span>
              <span class="mgr-info">
                <b>{{ m.name }}</b>
                <small class="muted">{{ m.position ? m.position + ' · ' : '' }}{{ formatPhone(m.phoneNumber) }}</small>
              </span>
            </label>
            <p v-if="filteredManagers.length === 0" class="muted small empty">검색 결과가 없습니다.</p>
          </div>
        </template>

        <div class="modal-actions">
          <button class="btn btn-soft" @click="showManagerModal = false">취소</button>
          <button class="btn" :disabled="savingMgr || managers.length === 0" @click="saveManagers">저장</button>
        </div>
      </div>
    </div>

    <!-- 기업명 수정 -->
    <div v-if="showNameModal" class="modal-mask" @click.self="showNameModal = false">
      <div class="modal">
        <h3>기업명 수정</h3>
        <label class="label" for="company-name">기업명 (중복 불가)</label>
        <input id="company-name" v-model="nameInput" class="input" @keyup.enter="saveName" />
        <div v-if="nameError" class="alert" style="margin-top: 12px">{{ nameError }}</div>
        <div class="modal-actions">
          <button class="btn btn-soft" @click="showNameModal = false">취소</button>
          <button class="btn" :disabled="savingName || !nameInput.trim()" @click="saveName">저장</button>
        </div>
      </div>
    </div>

    <!-- 기업 삭제 확인 -->
    <div v-if="showDeleteModal" class="modal-mask" @click.self="showDeleteModal = false">
      <div class="modal">
        <h3>'{{ company.name }}'을(를) 삭제할까요?</h3>
        <p class="muted small">
          이 기업의 제출 항목과 올린 파일, 제출 링크가 모두 지워지며 <b>되돌릴 수 없습니다.</b>
        </p>
        <div v-if="deleteError" class="alert" style="margin-top: 12px">{{ deleteError }}</div>
        <div class="modal-actions">
          <button class="btn btn-soft" @click="showDeleteModal = false">취소</button>
          <button class="btn" :disabled="deleting" @click="confirmDelete">삭제하기</button>
        </div>
      </div>
    </div>

    <!-- 요청 자료 자세히 -->
    <div v-if="reqDetail" class="modal-mask" @click.self="reqDetail = null">
      <div class="modal">
        <h3>{{ reqDetail.name }}</h3>
        <div class="req-detail-img">
          <img v-if="reqDetail.exampleImageUrl" :src="absoluteUrl(reqDetail.exampleImageUrl)" alt="예시 이미지" />
          <span v-else class="muted small">예시 이미지 없음</span>
        </div>
        <p class="req-detail-desc">{{ reqDetail.description || '설명 없음' }}</p>
        <div class="modal-actions">
          <button class="btn btn-soft" @click="reqDetail = null">닫기</button>
        </div>
      </div>
    </div>

    <!-- 항목별 추가 설명 -->
    <div v-if="noteTarget" class="modal-mask" @click.self="noteTarget = null">
      <div class="modal">
        <h3>추가 설명</h3>
        <p class="muted small">'{{ noteTarget.requirementName }}' - 이 기업에만 보이는 안내입니다. 비우면 지워집니다.</p>
        <textarea
          v-model="noteText"
          class="input"
          rows="3"
          placeholder="예: 오후 2시 이전, 간판 조명 켜진 상태로 촬영해주세요."
        ></textarea>
        <p class="muted small" style="margin: 10px 0 0">고객 화면에서 기본 설명과 함께 표시됩니다.</p>
        <div class="modal-actions">
          <button class="btn btn-soft" @click="noteTarget = null">취소</button>
          <button class="btn" :disabled="savingNote" @click="saveNote">저장</button>
        </div>
      </div>
    </div>

    <!-- 사진 확대 -->
    <div v-if="lightbox" class="modal-mask lightbox" @click="lightbox = ''">
      <img :src="lightbox" class="lightbox-img" alt="확대 이미지" />
    </div>
  </div>
</template>

<style scoped>
.back {
  margin-bottom: 20px;
}
.title {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}
.title-mark {
  width: 64px;
  height: 64px;
  font-size: 24px;
  background: var(--ink);
  color: var(--on-ink);
}
.title-meta {
  margin: 10px 0 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 15px;
}
.head-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 16px;
  align-items: start;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}
.ctitle {
  font-size: 20px;
  line-height: 1.3;
}
.card-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}
.text-link {
  color: var(--ink);
  font-weight: 600;
  text-decoration: underline;
}

/* 제출 링크 카드 (연회색 바탕 위라 안쪽 요소는 흰색) */
.link-chip {
  background: var(--canvas);
  color: var(--ink);
}
.seg-full {
  display: flex;
  margin: 12px 0 0;
}
.seg-full button {
  flex: 1;
}
.on-soft {
  background: var(--canvas);
}
.on-soft-input {
  background: var(--canvas);
}
.link-url {
  padding: 14px 16px;
  border-radius: var(--r-sm);
  background: var(--canvas);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  line-height: 1.45;
  word-break: break-all;
}
.link-url.off {
  color: var(--faint);
  text-decoration: line-through;
}
.link-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.link-actions .btn {
  flex: 1 1 140px;
}
.row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.row .input {
  flex: 1;
  min-width: 0;
}
.link-msg {
  margin: 8px 2px 0;
}
.link-meta {
  margin: 18px 0 0;
  display: grid;
  gap: 6px;
  font-size: 14px;
}
.link-meta div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.link-meta dt {
  color: var(--muted);
}
.link-meta dd {
  margin: 0;
  font-variant-numeric: tabular-nums;
}
.link-sub {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #e6e6e6;
}

/* 요청 자료 토글 */
.toggles {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.toggle {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--hairline);
  border-radius: var(--r-full);
  background: var(--canvas);
  color: var(--muted);
}
.toggle.on {
  background: var(--ink);
  border-color: var(--ink);
  color: var(--on-ink);
}
.toggle-main {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 0;
  background: transparent;
  color: inherit;
  min-height: 36px;
  padding: 0 4px 0 14px;
  font-size: 14px;
  font-weight: 600;
}
.toggle-info {
  width: 30px;
  height: 30px;
  margin-right: 3px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: inherit;
  opacity: 0.6;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.toggle-info:hover {
  opacity: 1;
}

/* 검수 */
.review-section {
  margin-top: 64px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.section-title {
  font-size: 28px;
  line-height: 1.2;
}
.reviews {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.review {
  display: flex;
  flex-direction: column;
  padding: 12px;
  border: 1px solid var(--hairline-soft);
  border-radius: var(--r-md);
}
.review.waiting {
  border-color: transparent;
  box-shadow: inset 0 0 0 1.5px var(--accent);
}
.media {
  position: relative;
  aspect-ratio: 4 / 3;
  border-radius: var(--r-sm);
  overflow: hidden;
  background: var(--canvas-soft);
}
.media.doc {
  aspect-ratio: 3 / 4;
}
.media.zoomable {
  cursor: zoom-in;
}
.media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.pdf-frame {
  width: 100%;
  height: 100%;
  border: 0;
  background: var(--canvas);
}
.media-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  text-align: center;
  color: var(--muted);
}
/* design.md badge-overlay: 사진 위 반투명 회색 알약 */
.overlay {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 5px 10px;
  border-radius: var(--r-full);
  background: rgba(115, 115, 115, 0.56);
  color: var(--on-ink);
  font-size: 12px;
  font-weight: 600;
}
.review-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 6px 2px;
}
.review-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.review-title b {
  font-size: 17px;
  font-weight: var(--w-heading);
}
.note-box {
  margin: 0;
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--canvas-soft);
  font-size: 13px;
  line-height: 1.45;
}
.note-box svg {
  flex: 0 0 auto;
  margin-top: 2px;
}
.note-box b {
  margin-right: 4px;
}
.doc-links {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.doc-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.review-actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
}
.review-actions .btn {
  flex: 1;
}
.review-state {
  margin: auto 0 0;
}
.note-btn {
  align-self: flex-start;
}

/* 확인 창 안쪽 */
.modal .input + .confirm-check,
.modal textarea {
  margin-top: 4px;
}
.confirm-check {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  font-size: 14px;
}
.confirm-check input,
.mgr-opt input {
  accent-color: var(--ink);
  width: 16px;
  height: 16px;
}
.picked {
  margin: 10px 0 0;
}
.mgr-list {
  margin-top: 12px;
  max-height: 320px;
  overflow-y: auto;
  display: grid;
  gap: 6px;
}
.mgr-opt {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--r-sm);
  cursor: pointer;
}
.mgr-opt:hover,
.mgr-opt.on {
  background: var(--canvas-soft);
}
.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--canvas-soft);
  font-weight: var(--w-heading);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.mgr-opt.on .avatar {
  background: var(--canvas);
}
.mgr-info {
  display: flex;
  flex-direction: column;
}
.empty {
  padding: 12px;
  text-align: center;
}
.req-detail-img {
  margin-top: 8px;
  aspect-ratio: 4 / 3;
  border-radius: var(--r-sm);
  background: var(--canvas-soft);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.req-detail-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.req-detail-desc {
  margin: 14px 0 0;
  font-size: 15px;
  line-height: 1.5;
}
.lightbox {
  background: rgba(20, 20, 20, 0.9);
  cursor: zoom-out;
}
.lightbox-img {
  max-width: 96vw;
  max-height: 88vh;
  border-radius: var(--r-sm);
}

@media (max-width: 1024px) {
  .reviews {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 840px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 640px) {
  .reviews {
    grid-template-columns: 1fr;
  }
  .title-mark {
    width: 52px;
    height: 52px;
    font-size: 20px;
  }
  .section-title {
    font-size: 24px;
  }
}
</style>
