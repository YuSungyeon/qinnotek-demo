<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '../../api/admin'
import { fileUrl } from '../../api/client'
import Icon from '../../components/Icon.vue'

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

// 전화번호 편집
const phoneInput = ref('')
const savingPhone = ref(false)

// 요구사진 지정
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

// 요구사진 자세히 모달
const reqDetail = ref(null)

// 항목별 추가 설명 모달
const noteTarget = ref(null)
const noteText = ref('')
const savingNote = ref(false)

// 이미지 라이트박스
const lightbox = ref('')

const companyId = computed(() => Number(props.id))
const approvedCount = computed(
  () => (submission.value?.items || []).filter((i) => i.status === 'APPROVED' && i.photoUrl).length
)
const zipUrl = computed(() =>
  fileUrl(`/api/admin/companies/${companyId.value}/submissions/approved-zip`)
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
    phoneInput.value = detail.phoneNumber || ''
    selectedReqIds.value = [...detail.assignedRequirementIds]
    selectedManagerIds.value = [...(detail.assignedManagerIds || [])]
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function savePhone() {
  savingPhone.value = true
  error.value = ''
  try {
    // 고객 조회는 숫자만 비교하므로 하이픈 등 기호 제거 후 저장
    company.value = await adminApi.updatePhone(companyId.value, phoneInput.value.replace(/\D/g, ''))
  } catch (err) {
    error.value = err.message
  } finally {
    savingPhone.value = false
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

const statusOf = (s) => ({ PENDING: '미제출', SUBMITTED: '검수 대기', APPROVED: '통과', RETURNED: '반환' }[s] || s)
const statusColor = (s) =>
  ({ PENDING: '#6b7280', SUBMITTED: '#2563eb', APPROVED: '#16a34a', RETURNED: '#dc2626' }[s] || '#6b7280')

onMounted(loadAll)
</script>

<template>
  <div v-if="loading" class="muted">불러오는 중…</div>
  <div v-else-if="company">
    <button class="btn btn-ghost btn-sm back" @click="router.push('/admin/companies')">← 목록</button>

    <div class="page-head">
      <h1>{{ company.name }}</h1>
      <span class="badge" :style="{ background: company.statusColor }">{{ company.statusLabel }}</span>
      <div class="head-actions">
        <button class="btn btn-ghost btn-sm" @click="openManagerModal">
          <Icon name="user" :size="15" /> 알림 담당자
          <span v-if="(company.assignedManagerIds || []).length" class="count-badge">
            {{ company.assignedManagerIds.length }}
          </span>
        </button>
        <button class="btn btn-ghost btn-sm" @click="openNameEdit">이름 수정</button>
        <button class="btn btn-danger btn-sm" @click="deleteError = ''; showDeleteModal = true">기업 삭제</button>
      </div>
    </div>

    <div v-if="error" class="alert alert-error" style="margin-bottom: 16px">{{ error }}</div>

    <div class="grid">
      <!-- 전화번호 -->
      <section class="card">
        <h2 class="ctitle">전화번호</h2>
        <p class="muted small">기업당 1개만 등록 가능합니다.</p>
        <div class="row">
          <input v-model="phoneInput" class="input" placeholder="전화번호 입력" />
          <button class="btn" :disabled="savingPhone" @click="savePhone">저장</button>
        </div>
      </section>

      <!-- 요구사진 지정 -->
      <section class="card">
        <h2 class="ctitle">필요 사진 지정</h2>
        <p class="muted small">이 기업이 제출할 사진 항목을 선택하세요.</p>
        <div v-if="requirements.length === 0" class="muted">
          등록된 요구 사진이 없습니다.
          <RouterLink to="/admin/requirements" class="link">요구 사진 관리 →</RouterLink>
        </div>
        <div v-else class="req-list">
          <div v-for="r in requirements" :key="r.id" class="req-item">
            <label class="req-check">
              <input
                type="checkbox"
                :checked="selectedReqIds.includes(r.id)"
                @change="toggleReq(r.id)"
              />
              <span>{{ r.name }}</span>
            </label>
            <button class="detail-link" @click="reqDetail = r">자세히</button>
          </div>
        </div>
        <div class="save-row">
          <button class="btn" :disabled="savingReq" @click="saveRequirements">지정 저장</button>
          <span v-if="reqMsg" class="ok-msg">{{ reqMsg }}</span>
        </div>
      </section>

    </div>

    <!-- 제출 사진 검수 -->
    <section class="card submissions">
      <div class="sub-head">
        <h2 class="ctitle">제출 사진 검수</h2>
        <div class="sub-head-right">
          <a v-if="approvedCount > 0" class="btn btn-sm zip-btn" :href="zipUrl" download>
            <Icon name="download" :size="16" /> 완료 사진 ZIP ({{ approvedCount }})
          </a>
          <span v-if="submission?.latestSubmittedAt" class="muted small">
            최근 제출: {{ new Date(submission.latestSubmittedAt).toLocaleString('ko-KR') }}
          </span>
        </div>
      </div>

      <div v-if="!submission || submission.items.length === 0" class="muted">
        지정된 제출 항목이 없습니다.
      </div>

      <div v-else class="item-grid">
        <div v-for="item in submission.items" :key="item.itemId" class="sub-item">
          <div class="sub-item-head">
            <span class="disp">{{ item.displayName }}</span>
            <span class="badge" :style="{ background: statusColor(item.status) }">{{ statusOf(item.status) }}</span>
          </div>

          <div class="photo-box" @click="item.photoUrl && (lightbox = fileUrl(item.photoUrl))">
            <img v-if="item.photoUrl" :src="fileUrl(item.photoUrl)" alt="제출 사진" />
            <div v-else class="no-photo">사진 없음</div>
          </div>

          <div v-if="item.status === 'RETURNED' && item.rejectReason" class="reject-reason">
            반환 사유: {{ item.rejectReason }}
          </div>

          <div v-if="item.adminNote" class="admin-note">
            <strong>추가 설명</strong> {{ item.adminNote }}
          </div>

          <div class="sub-actions" v-if="item.status === 'SUBMITTED'">
            <button class="btn btn-success btn-sm" :disabled="processing" @click="approve(item)">통과</button>
            <button class="btn btn-danger btn-sm" :disabled="processing" @click="openReturn(item)">반환</button>
          </div>
          <div v-else class="sub-actions muted small">
            {{ item.status === 'APPROVED' ? '승인 완료' : item.status === 'RETURNED' ? '고객 재제출 대기' : '고객 제출 대기' }}
          </div>

          <button class="btn btn-ghost btn-sm note-btn" @click="openNote(item)">
            {{ item.adminNote ? '추가 설명 수정' : '추가 설명 작성' }}
          </button>
        </div>
      </div>
    </section>

    <!-- 반환 모달 -->
    <div v-if="returnTarget" class="modal-mask" @click.self="closeReturn">
      <div class="modal">
        <h3>사진 반환</h3>
        <p class="disp-name">{{ returnTarget.displayName }}</p>
        <label class="label">반환 사유 (필수)</label>
        <textarea
          v-model="returnReason"
          class="input"
          rows="3"
          placeholder="예: 사진이 너무 어둡습니다."
        ></textarea>
        <div class="warn">⚠ 반환 시 기존 사진은 삭제되며 고객은 다시 촬영해야 합니다.</div>
        <label class="confirm-check">
          <input v-model="returnConfirmed" type="checkbox" />
          <span>위 내용을 확인했습니다.</span>
        </label>
        <div class="modal-actions">
          <button class="btn btn-ghost" @click="closeReturn">취소</button>
          <button
            class="btn btn-danger"
            :disabled="!returnReason.trim() || !returnConfirmed || processing"
            @click="confirmReturn"
          >
            반환하기
          </button>
        </div>
      </div>
    </div>

    <!-- 알림 담당자 지정 모달 -->
    <div v-if="showManagerModal" class="modal-mask" @click.self="showManagerModal = false">
      <div class="modal mgr-modal">
        <h3>알림 담당자 지정</h3>
        <p class="disp-name">{{ company.name }} · 제출 시 문자를 받을 담당자</p>

        <template v-if="managers.length === 0">
          <p class="muted" style="margin: 8px 0 0">
            등록된 담당자가 없습니다.
            <RouterLink to="/admin/managers" class="link">담당자 관리 →</RouterLink>
          </p>
        </template>
        <template v-else>
          <input
            v-model="managerKeyword"
            class="input"
            placeholder="이름·직책·전화번호 검색"
            style="margin-bottom: 10px"
          />
          <div v-if="assignedManagerNames.length" class="picked muted">
            선택됨: {{ assignedManagerNames.join(', ') }}
          </div>
          <div class="mgr-list">
            <label v-for="m in filteredManagers" :key="m.id" class="mgr-opt">
              <input
                type="checkbox"
                :checked="selectedManagerIds.includes(m.id)"
                @change="toggleManager(m.id)"
              />
              <span class="mgr-info">
                <b>{{ m.name }}</b>
                <small class="muted">{{ m.position ? m.position + ' · ' : '' }}{{ m.phoneNumber }}</small>
              </span>
            </label>
            <p v-if="filteredManagers.length === 0" class="muted center" style="padding: 12px">
              검색 결과가 없습니다.
            </p>
          </div>
        </template>

        <div class="modal-actions">
          <button class="btn btn-ghost" @click="showManagerModal = false">취소</button>
          <button class="btn" :disabled="savingMgr || managers.length === 0" @click="saveManagers">
            지정 저장
          </button>
        </div>
      </div>
    </div>

    <!-- 기업명 수정 모달 -->
    <div v-if="showNameModal" class="modal-mask" @click.self="showNameModal = false">
      <div class="modal">
        <h3>기업명 수정</h3>
        <label class="label">기업명 (중복 불가)</label>
        <input v-model="nameInput" class="input" @keyup.enter="saveName" />
        <div v-if="nameError" class="alert alert-error" style="margin-top: 12px">{{ nameError }}</div>
        <div class="modal-actions">
          <button class="btn btn-ghost" @click="showNameModal = false">취소</button>
          <button class="btn" :disabled="savingName || !nameInput.trim()" @click="saveName">저장</button>
        </div>
      </div>
    </div>

    <!-- 기업 삭제 모달 -->
    <div v-if="showDeleteModal" class="modal-mask" @click.self="showDeleteModal = false">
      <div class="modal">
        <h3>기업 삭제</h3>
        <p class="disp-name">{{ company.name }}</p>
        <div class="warn">
          ⚠ 삭제하면 이 기업의 제출 항목과 업로드된 사진이 모두 삭제되며 되돌릴 수 없습니다.
        </div>
        <div v-if="deleteError" class="alert alert-error" style="margin-top: 12px">{{ deleteError }}</div>
        <div class="modal-actions">
          <button class="btn btn-ghost" @click="showDeleteModal = false">취소</button>
          <button class="btn btn-danger" :disabled="deleting" @click="confirmDelete">삭제하기</button>
        </div>
      </div>
    </div>

    <!-- 요구사진 자세히 모달 -->
    <div v-if="reqDetail" class="modal-mask" @click.self="reqDetail = null">
      <div class="modal">
        <h3>{{ reqDetail.name }}</h3>
        <div class="req-detail-img" :class="{ empty: !reqDetail.exampleImageUrl }">
          <img v-if="reqDetail.exampleImageUrl" :src="fileUrl(reqDetail.exampleImageUrl)" alt="예시 이미지" />
          <span v-else class="muted">예시 이미지 없음</span>
        </div>
        <label class="label" style="margin-top: 14px">설명</label>
        <p class="req-detail-desc">{{ reqDetail.description || '설명 없음' }}</p>
        <div class="modal-actions">
          <button class="btn btn-ghost" @click="reqDetail = null">닫기</button>
        </div>
      </div>
    </div>

    <!-- 항목별 추가 설명 모달 -->
    <div v-if="noteTarget" class="modal-mask" @click.self="noteTarget = null">
      <div class="modal">
        <h3>추가 설명</h3>
        <p class="disp-name">{{ noteTarget.displayName }}</p>
        <label class="label">이 기업에만 표시할 추가 안내 (비우면 삭제)</label>
        <textarea
          v-model="noteText"
          class="input"
          rows="3"
          placeholder="예: 오후 2시 이전, 간판 조명 켜진 상태로 촬영해주세요."
        ></textarea>
        <p class="muted small" style="margin: 10px 0 0">고객 화면에서 기본 설명과 함께 표시됩니다.</p>
        <div class="modal-actions">
          <button class="btn btn-ghost" @click="noteTarget = null">취소</button>
          <button class="btn" :disabled="savingNote" @click="saveNote">저장</button>
        </div>
      </div>
    </div>

    <!-- 이미지 라이트박스 -->
    <div v-if="lightbox" class="modal-mask" @click="lightbox = ''">
      <img :src="lightbox" class="lightbox-img" alt="확대 이미지" />
    </div>
  </div>
</template>

<style scoped>
.back {
  margin-bottom: 12px;
}
.page-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.page-head h1 {
  font-size: 25px;
  font-weight: 800;
}
.head-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
@media (max-width: 720px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
.ctitle {
  font-size: 16px;
  margin-bottom: 4px;
}
.small {
  font-size: 13px;
}
.muted.small {
  margin: 0 0 12px;
}
.row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.row > .input {
  flex: 1;
  min-width: 0;
}
.req-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 8px 0 14px;
}
.req-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.req-check {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  cursor: pointer;
  flex: 1;
  min-width: 0;
}
.req-check input {
  width: 18px;
  height: 18px;
}
.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  margin-left: 2px;
  border-radius: 999px;
  background: var(--primary);
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}
.mgr-modal {
  max-width: 460px;
}
.picked {
  font-size: 13px;
  margin-bottom: 8px;
}
.mgr-list {
  max-height: 320px;
  overflow-y: auto;
  border: 1px solid var(--border);
  border-radius: 10px;
}
.mgr-opt {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 12px;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
}
.mgr-opt:last-child {
  border-bottom: none;
}
.mgr-opt input {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
}
.mgr-info {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
.mgr-info small {
  font-size: 13px;
}
.detail-link {
  border: none;
  background: none;
  padding: 4px 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--primary);
  cursor: pointer;
  white-space: nowrap;
}
.detail-link:hover {
  text-decoration: underline;
}
.save-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ok-msg {
  color: var(--success);
  font-size: 14px;
  font-weight: 600;
}
.link {
  color: var(--primary);
  font-weight: 600;
}
.submissions {
  margin-top: 4px;
}
.sub-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.sub-head-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.zip-btn {
  text-decoration: none;
}
.item-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
.sub-item {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
  background: var(--card);
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.15s, transform 0.15s;
}
.sub-item:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.sub-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}
.disp {
  font-size: 14px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.photo-box {
  width: 100%;
  aspect-ratio: 1;
  background: #f3f4f6;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.photo-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.no-photo {
  color: var(--text-muted);
  font-size: 14px;
  cursor: default;
}
.reject-reason {
  margin-top: 8px;
  font-size: 13px;
  color: var(--danger);
  background: var(--danger-bg);
  padding: 6px 8px;
  border-radius: 6px;
}
.admin-note {
  margin-top: 8px;
  font-size: 13px;
  color: var(--primary-dark);
  background: var(--primary-soft);
  padding: 6px 8px;
  border-radius: 6px;
  word-break: break-all;
}
.admin-note strong {
  font-size: 12px;
  margin-right: 4px;
}
.note-btn {
  width: 100%;
  margin-top: 10px;
}
.req-detail-img {
  width: 100%;
  aspect-ratio: 16/10;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #f1f5f9;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.req-detail-img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.req-detail-desc {
  margin: 0;
  font-size: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
}
.sub-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 50;
}
.modal {
  background: #fff;
  border-radius: var(--radius);
  padding: 22px;
  width: 100%;
  max-width: 420px;
}
.modal h3 {
  font-size: 18px;
  margin-bottom: 4px;
}
.disp-name {
  color: var(--text-muted);
  font-size: 14px;
  margin: 0 0 14px;
}
.modal textarea {
  resize: vertical;
}
.warn {
  margin: 12px 0;
  padding: 10px 12px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 8px;
  font-size: 13px;
  color: var(--warning);
}
.confirm-check {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  margin-bottom: 16px;
  cursor: pointer;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.lightbox-img {
  max-width: 90vw;
  max-height: 90vh;
  border-radius: 8px;
}
</style>
