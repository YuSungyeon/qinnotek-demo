<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Camera, FileText, Image as ImageIcon, Pencil, Plus, Trash2, Upload } from 'lucide-vue-next'
import { adminApi } from '../../api/admin'
import { absoluteUrl } from '../../api/client'

const list = ref([])
const loading = ref(true)
const error = ref('')

// 등록/수정 모달
const showModal = ref(false)
const editing = ref(null) // null=새로등록, 객체=수정
const form = ref({ name: '', description: '' })
const file = ref(null)
const fileInput = ref(null)
const saving = ref(false)
const modalError = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    list.value = await adminApi.listRequirements()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const FILE_TYPES = [
  { value: 'PHOTO', label: '사진' },
  { value: 'DOCUMENT', label: '문서' },
  { value: 'ANY', label: '사진·문서' }
]
// 카드 위 유형 표시 ('무관'보다 무엇을 받는지가 바로 보이게)
const TYPE_LABEL = { PHOTO: '사진', DOCUMENT: '문서', ANY: '사진·문서' }

// 유형 거르기
const FILTERS = [
  { key: 'ALL', label: '전체' },
  { key: 'PHOTO', label: '사진' },
  { key: 'ANY', label: '사진·문서' },
  { key: 'DOCUMENT', label: '문서' }
]
const filter = ref('ALL')
const visible = computed(() =>
  filter.value === 'ALL' ? list.value : list.value.filter((r) => r.fileType === filter.value)
)

// 새로 고른 예시 이미지 미리보기 (창을 닫거나 바꾸면 URL 해제)
const previewUrl = ref('')
watch(file, (f) => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = f ? URL.createObjectURL(f) : ''
})
onBeforeUnmount(() => previewUrl.value && URL.revokeObjectURL(previewUrl.value))

function openCreate() {
  editing.value = null
  form.value = {
    name: '',
    description: '',
    classificationHint: '',
    fileType: 'PHOTO',
    documentKeywords: ''
  }
  file.value = null
  modalError.value = ''
  showModal.value = true
}

function openEdit(r) {
  editing.value = r
  form.value = {
    name: r.name,
    description: r.description || '',
    classificationHint: r.classificationHint || '',
    fileType: r.fileType || 'PHOTO',
    documentKeywords: r.documentKeywords || ''
  }
  file.value = null
  modalError.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  file.value = null
  if (fileInput.value) fileInput.value.value = ''
}

function onFile(e) {
  file.value = e.target.files?.[0] || null
}

async function save() {
  if (!form.value.name.trim()) {
    modalError.value = '자료 명칭을 입력해주세요.'
    return
  }
  saving.value = true
  modalError.value = ''
  try {
    const payload = {
      name: form.value.name.trim(),
      description: form.value.description.trim(),
      classificationHint: form.value.classificationHint.trim(),
      fileType: form.value.fileType,
      documentKeywords: form.value.documentKeywords.trim(),
      exampleImage: file.value
    }
    if (editing.value) {
      await adminApi.updateRequirement(editing.value.id, payload)
    } else {
      await adminApi.createRequirement(payload)
    }
    closeModal()
    await load()
  } catch (err) {
    modalError.value = err.message
  } finally {
    saving.value = false
  }
}

async function remove(r) {
  if (!confirm(`'${r.name}' 요청 자료를 삭제할까요?`)) return
  error.value = ''
  try {
    await adminApi.deleteRequirement(r.id)
    await load()
  } catch (err) {
    error.value = err.message
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1>요청 자료</h1>
        <p class="page-sub">기업에 지정할 수 있는 자료입니다. 사진·문서·기타 파일을 받을 수 있습니다.</p>
      </div>
      <button class="btn" @click="openCreate"><Plus :size="17" /> 요청 자료 등록</button>
    </div>

    <div class="toolbar">
      <div class="seg">
        <button v-for="f in FILTERS" :key="f.key" :class="{ on: filter === f.key }" @click="filter = f.key">
          {{ f.label }}<template v-if="f.key === 'ALL'"> {{ list.length }}</template>
        </button>
      </div>
    </div>

    <div v-if="error" class="alert" style="margin-bottom: 16px">{{ error }}</div>
    <div v-if="loading" class="muted">불러오는 중…</div>

    <div v-else class="grid">
      <article v-for="r in visible" :key="r.id" class="req">
        <div class="media">
          <img v-if="r.exampleImageUrl" :src="absoluteUrl(r.exampleImageUrl)" :alt="r.name" />
          <div v-else class="media-empty">
            <component :is="r.fileType === 'PHOTO' ? Camera : FileText" :size="28" />
            <small>{{ r.fileType === 'PHOTO' ? '예시 없음' : '파일 항목' }}</small>
          </div>
          <span class="overlay">{{ TYPE_LABEL[r.fileType] || r.fileTypeLabel }}</span>
        </div>
        <div class="req-body">
          <b>{{ r.name }}</b>
          <p>{{ r.description || '설명 없음' }}</p>
        </div>
        <div class="req-foot">
          <small class="muted">{{ r.assignedCount ? `기업 ${r.assignedCount}곳에 지정` : '지정 안 됨' }}</small>
          <span class="req-tools">
            <button class="btn btn-soft btn-xs" @click="openEdit(r)"><Pencil :size="13" /> 수정</button>
            <!-- 기업에 지정된 자료는 서버가 삭제를 막는다 → 미리 버튼을 끈다 -->
            <button
              class="icon-btn"
              :disabled="r.assignedCount > 0"
              :title="r.assignedCount > 0 ? '기업에 지정된 자료는 삭제할 수 없습니다' : '삭제'"
              :aria-label="`${r.name} 삭제`"
              @click="remove(r)"
            >
              <Trash2 :size="15" />
            </button>
          </span>
        </div>
      </article>
      <p v-if="visible.length === 0" class="muted">
        {{ list.length ? '조건에 맞는 요청 자료가 없습니다.' : '등록된 요청 자료가 없습니다.' }}
      </p>
    </div>

    <!-- 등록/수정 -->
    <div v-if="showModal" class="modal-mask" @click.self="closeModal">
      <div class="modal wide">
        <h3>{{ editing ? '요청 자료 수정' : '요청 자료 등록' }}</h3>

        <label class="label" for="req-name">자료 명칭 *</label>
        <input id="req-name" v-model="form.name" class="input" placeholder="예: 공장 외관" />

        <label class="label" for="req-desc">설명</label>
        <textarea
          id="req-desc"
          v-model="form.description"
          class="input"
          rows="3"
          placeholder="고객에게 보여줄 안내 문구"
        ></textarea>

        <span class="label">받는 파일 유형</span>
        <div class="seg seg-full">
          <button
            v-for="t in FILE_TYPES"
            :key="t.value"
            type="button"
            :class="{ on: form.fileType === t.value }"
            @click="form.fileType = t.value"
          >
            {{ t.label }}
          </button>
        </div>
        <p class="hint">
          '문서'로 두면 고객 화면에 카메라 대신 파일 선택기가 나오고, 워드·한글은 PDF 사본이 함께 저장됩니다.
        </p>

        <!-- 사진 분류 설정 (사진을 받을 수 있을 때) -->
        <template v-if="form.fileType !== 'DOCUMENT'">
          <label class="label" for="req-hint">AI 분류 힌트 (영문)</label>
          <input
            id="req-hint"
            v-model="form.classificationHint"
            class="input"
            placeholder="예: the outside of a factory building with a large loading gate"
          />
          <p class="hint">고객이 '한번에 올리기'를 쓰면 이 힌트로 사진을 자동 분류합니다. 사진을 영어로 묘사해주세요.</p>

          <span class="label">예시 이미지 {{ editing ? '(바꿀 때만 선택)' : '' }}</span>
          <div class="example-row">
            <div class="example-thumb">
              <img v-if="previewUrl" :src="previewUrl" alt="선택한 예시" />
              <img v-else-if="editing?.exampleImageUrl" :src="absoluteUrl(editing.exampleImageUrl)" alt="현재 예시" />
              <ImageIcon v-else :size="22" />
            </div>
            <label class="btn btn-soft btn-sm file-btn">
              <Upload :size="15" /> {{ file ? file.name : '이미지 선택' }}
              <input ref="fileInput" type="file" accept="image/*" hidden @change="onFile" />
            </label>
          </div>
        </template>

        <!-- 문서 분류 설정 (문서를 받을 수 있을 때) -->
        <template v-if="form.fileType !== 'PHOTO'">
          <label class="label" for="req-keywords">문서 분류 키워드</label>
          <textarea
            id="req-keywords"
            v-model="form.documentKeywords"
            class="input"
            rows="2"
            placeholder="사업자등록증, 등록번호, 대표자, 개업연월일"
          ></textarea>
          <p class="hint">
            서류에서 이 단어들이 보이면 이 항목으로 자동 분류합니다. 쉼표로 구분하세요.
            (문서 제목에 들어가는 말을 넣으면 가장 정확합니다)
          </p>
        </template>

        <div v-if="modalError" class="alert" style="margin-top: 14px">{{ modalError }}</div>

        <div class="modal-actions">
          <button class="btn btn-soft" @click="closeModal">취소</button>
          <button class="btn" :disabled="saving" @click="save">{{ editing ? '저장' : '등록' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.req {
  display: flex;
  flex-direction: column;
  padding: 10px;
  border: 1px solid var(--hairline-soft);
  border-radius: var(--r-md);
}
.media {
  position: relative;
  aspect-ratio: 4 / 3;
  border-radius: var(--r-sm);
  overflow: hidden;
  background: var(--canvas-soft);
}
.media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.media-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--muted);
}
/* design.md badge-overlay */
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
.req-body {
  flex: 1;
  padding: 12px 6px 0;
}
.req-body b {
  font-size: 16px;
  font-weight: var(--w-heading);
}
.req-body p {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.45;
  color: var(--muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.req-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 6px 4px;
}
.req-tools {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.btn-xs {
  min-height: 30px;
  padding: 0 12px;
  font-size: 13px;
}
.icon-btn {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.icon-btn:hover:not(:disabled) {
  background: var(--canvas-soft);
  color: var(--ink);
}
.icon-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

/* 등록/수정 창 */
.modal.wide {
  max-width: 560px;
}
.modal .label {
  margin-top: 16px;
}
.modal h3 + .label {
  margin-top: 12px;
}
.seg-full {
  display: flex;
}
.seg-full button {
  flex: 1;
}
.hint {
  margin: 8px 2px 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--muted);
}
.example-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.example-thumb {
  width: 72px;
  height: 54px;
  border-radius: 12px;
  background: var(--canvas-soft);
  color: var(--muted);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.example-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.file-btn {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

@media (max-width: 1024px) {
  .grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 840px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 480px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
