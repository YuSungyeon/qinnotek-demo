<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../../api/admin'
import { fileUrl } from '../../api/client'

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

function openCreate() {
  editing.value = null
  form.value = { name: '', description: '', classificationHint: '' }
  file.value = null
  modalError.value = ''
  showModal.value = true
}

function openEdit(r) {
  editing.value = r
  form.value = {
    name: r.name,
    description: r.description || '',
    classificationHint: r.classificationHint || ''
  }
  file.value = null
  modalError.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  if (fileInput.value) fileInput.value.value = ''
}

function onFile(e) {
  file.value = e.target.files?.[0] || null
}

async function save() {
  if (!form.value.name.trim()) {
    modalError.value = '사진 명칭을 입력해주세요.'
    return
  }
  saving.value = true
  modalError.value = ''
  try {
    const payload = {
      name: form.value.name.trim(),
      description: form.value.description.trim(),
      classificationHint: form.value.classificationHint.trim(),
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
  if (!confirm(`'${r.name}' 요구 사진을 삭제할까요?`)) return
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
        <h1>요구 사진 관리</h1>
        <p class="page-sub muted">기업에 지정할 수 있는 사진 항목을 등록합니다. 사진 명칭은 중복될 수 없습니다.</p>
      </div>
      <button class="btn" @click="openCreate">+ 요구 사진 등록</button>
    </div>

    <div v-if="error" class="alert alert-error" style="margin-bottom: 16px">{{ error }}</div>

    <!-- 목록 -->
    <div v-if="loading" class="muted">불러오는 중…</div>
    <div v-else class="req-grid">
      <div v-for="r in list" :key="r.id" class="req-card">
        <div class="thumb">
          <img v-if="r.exampleImageUrl" :src="fileUrl(r.exampleImageUrl)" alt="예시" />
          <div v-else class="no-img">예시 없음</div>
        </div>
        <div class="req-body">
          <div class="req-name">{{ r.name }}</div>
          <div class="req-desc muted">{{ r.description || '설명 없음' }}</div>
        </div>
        <div class="req-actions">
          <button class="btn btn-ghost btn-sm" @click="openEdit(r)">수정</button>
          <button class="btn btn-danger btn-sm" @click="remove(r)">삭제</button>
        </div>
      </div>
      <div v-if="list.length === 0" class="muted">등록된 요구 사진이 없습니다.</div>
    </div>

    <!-- 등록/수정 모달 -->
    <div v-if="showModal" class="modal-mask" @click.self="closeModal">
      <div class="modal">
        <h3>{{ editing ? '요구 사진 수정' : '요구 사진 등록' }}</h3>

        <label class="label">사진 명칭 *</label>
        <input v-model="form.name" class="input" placeholder="예: 건물 정면" />

        <label class="label" style="margin-top: 14px">설명</label>
        <textarea
          v-model="form.description"
          class="input"
          rows="3"
          placeholder="고객에게 보여줄 촬영 안내 문구"
        ></textarea>

        <label class="label" style="margin-top: 14px">AI 분류 힌트 (영문)</label>
        <input
          v-model="form.classificationHint"
          class="input"
          placeholder="예: the exterior facade of a building"
        />
        <p class="muted hint-help">
          고객이 '사진 한번에 올리기'를 쓰면 이 힌트로 자동 분류합니다. 사진을 영어로 묘사해주세요.
        </p>

        <label class="label" style="margin-top: 14px">
          예시 이미지 {{ editing ? '(변경 시에만 선택)' : '' }}
        </label>
        <div v-if="editing?.exampleImageUrl && !file" class="cur-example">
          <img :src="fileUrl(editing.exampleImageUrl)" alt="현재 예시" />
          <span class="muted" style="font-size: 13px">현재 등록된 예시</span>
        </div>
        <input ref="fileInput" type="file" accept="image/*" @change="onFile" />

        <div v-if="modalError" class="alert alert-error" style="margin-top: 14px">{{ modalError }}</div>

        <div class="modal-actions">
          <button class="btn btn-ghost" @click="closeModal">취소</button>
          <button class="btn" :disabled="saving" @click="save">
            {{ editing ? '수정 저장' : '등록' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.page-head h1 {
  font-size: 25px;
  font-weight: 800;
}
.page-sub {
  margin: 4px 0 0;
  font-size: 14px;
}
.req-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}
.req-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.15s, transform 0.15s;
}
.req-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.thumb {
  aspect-ratio: 16/10;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.no-img {
  color: var(--text-muted);
  font-size: 14px;
}
.req-body {
  padding: 12px 14px;
  flex: 1;
}
.req-name {
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 4px;
}
.req-desc {
  font-size: 13px;
}
.req-actions {
  display: flex;
  gap: 8px;
  padding: 0 14px 14px;
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
  max-width: 460px;
  max-height: 88vh;
  overflow-y: auto;
}
.modal h3 {
  font-size: 18px;
  margin-bottom: 16px;
}
.modal textarea {
  resize: vertical;
}
.hint-help {
  margin: 6px 0 0;
  font-size: 12px;
}
.cur-example {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.cur-example img {
  width: 72px;
  height: 54px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--border);
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
}
</style>
