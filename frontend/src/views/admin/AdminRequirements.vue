<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../../api/admin'
import { fileUrl } from '../../api/client'

const list = ref([])
const loading = ref(true)
const error = ref('')

// 등록/수정 폼
const editing = ref(null) // null=새로등록, 객체=수정
const form = ref({ name: '', description: '' })
const file = ref(null)
const fileInput = ref(null)
const saving = ref(false)

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

function resetForm() {
  editing.value = null
  form.value = { name: '', description: '' }
  file.value = null
  if (fileInput.value) fileInput.value.value = ''
}

function startEdit(r) {
  editing.value = r
  form.value = { name: r.name, description: r.description || '' }
  file.value = null
  if (fileInput.value) fileInput.value.value = ''
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function onFile(e) {
  file.value = e.target.files?.[0] || null
}

async function save() {
  if (!form.value.name.trim()) return
  saving.value = true
  error.value = ''
  try {
    const payload = {
      name: form.value.name.trim(),
      description: form.value.description.trim(),
      exampleImage: file.value
    }
    if (editing.value) {
      await adminApi.updateRequirement(editing.value.id, payload)
    } else {
      await adminApi.createRequirement(payload)
    }
    resetForm()
    await load()
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function remove(r) {
  if (!confirm(`'${r.name}' 요구 사진을 삭제할까요?`)) return
  error.value = ''
  try {
    await adminApi.deleteRequirement(r.id)
    if (editing.value?.id === r.id) resetForm()
    await load()
  } catch (err) {
    error.value = err.message
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head"><h1>요구 사진 관리</h1></div>
    <p class="muted" style="margin-top: -8px; margin-bottom: 18px">
      기업에 지정할 수 있는 사진 항목을 등록합니다. 사진 명칭은 중복될 수 없습니다.
    </p>

    <div v-if="error" class="alert alert-error" style="margin-bottom: 16px">{{ error }}</div>

    <!-- 등록/수정 폼 -->
    <section class="card form-card">
      <h2 class="ctitle">{{ editing ? '요구 사진 수정' : '요구 사진 등록' }}</h2>
      <div class="fields">
        <div>
          <label class="label">사진 명칭 *</label>
          <input v-model="form.name" class="input" placeholder="예: 건물 정면" />
        </div>
        <div>
          <label class="label">설명</label>
          <input v-model="form.description" class="input" placeholder="촬영 안내 문구" />
        </div>
        <div>
          <label class="label">예시 이미지 {{ editing ? '(변경 시에만 선택)' : '' }}</label>
          <input ref="fileInput" type="file" accept="image/*" @change="onFile" />
        </div>
      </div>
      <div class="form-actions">
        <button v-if="editing" class="btn btn-ghost" @click="resetForm">취소</button>
        <button class="btn" :disabled="saving" @click="save">{{ editing ? '수정 저장' : '등록' }}</button>
      </div>
    </section>

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
          <button class="btn btn-ghost btn-sm" @click="startEdit(r)">수정</button>
          <button class="btn btn-danger btn-sm" @click="remove(r)">삭제</button>
        </div>
      </div>
      <div v-if="list.length === 0" class="muted">등록된 요구 사진이 없습니다.</div>
    </div>
  </div>
</template>

<style scoped>
.page-head h1 {
  font-size: 25px;
  font-weight: 800;
  margin-bottom: 6px;
}
.ctitle {
  font-size: 16px;
  margin-bottom: 14px;
}
.form-card {
  margin-bottom: 24px;
}
.fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.fields > div:last-child {
  grid-column: 1 / -1;
}
@media (max-width: 640px) {
  .fields {
    grid-template-columns: 1fr;
  }
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
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
</style>
