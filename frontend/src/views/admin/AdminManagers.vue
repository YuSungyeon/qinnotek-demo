<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../../api/admin'

const list = ref([])
const loading = ref(true)
const error = ref('')

const showModal = ref(false)
const editing = ref(null)
const form = ref({ name: '', position: '', phoneNumber: '' })
const saving = ref(false)
const modalError = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    list.value = await adminApi.listManagers()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.value = { name: '', position: '', phoneNumber: '' }
  modalError.value = ''
  showModal.value = true
}
function openEdit(m) {
  editing.value = m
  form.value = { name: m.name, position: m.position || '', phoneNumber: m.phoneNumber || '' }
  modalError.value = ''
  showModal.value = true
}

async function save() {
  if (!form.value.name.trim() || !form.value.phoneNumber.trim()) {
    modalError.value = '이름과 전화번호는 필수입니다.'
    return
  }
  saving.value = true
  modalError.value = ''
  try {
    const payload = {
      name: form.value.name.trim(),
      position: form.value.position.trim(),
      phoneNumber: form.value.phoneNumber.trim()
    }
    if (editing.value) await adminApi.updateManager(editing.value.id, payload)
    else await adminApi.createManager(payload)
    showModal.value = false
    await load()
  } catch (err) {
    modalError.value = err.message
  } finally {
    saving.value = false
  }
}

async function remove(m) {
  if (!confirm(`'${m.name}' 담당자를 삭제할까요? 기업 지정에서도 제거됩니다.`)) return
  error.value = ''
  try {
    await adminApi.deleteManager(m.id)
    await load()
  } catch (err) {
    error.value = err.message
  }
}

const fmtPhone = (p) =>
  p && p.length === 11 ? `${p.slice(0, 3)}-${p.slice(3, 7)}-${p.slice(7)}` : p

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1>담당자 관리</h1>
        <p class="page-sub muted">제출 알림 문자를 받을 담당자를 등록합니다. 기업별 지정은 기업 상세에서 합니다.</p>
      </div>
      <button class="btn" @click="openCreate">+ 담당자 등록</button>
    </div>

    <div v-if="error" class="alert alert-error" style="margin-bottom: 16px">{{ error }}</div>
    <div v-if="loading" class="muted">불러오는 중…</div>

    <table v-else class="tbl">
      <thead>
        <tr><th>이름</th><th>직책</th><th>전화번호</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-for="m in list" :key="m.id">
          <td class="strong">{{ m.name }}</td>
          <td>{{ m.position || '—' }}</td>
          <td>{{ fmtPhone(m.phoneNumber) }}</td>
          <td class="right">
            <button class="btn btn-ghost btn-sm" @click="openEdit(m)">수정</button>
            <button class="btn btn-danger btn-sm" @click="remove(m)">삭제</button>
          </td>
        </tr>
        <tr v-if="list.length === 0">
          <td colspan="4" class="muted center">등록된 담당자가 없습니다.</td>
        </tr>
      </tbody>
    </table>

    <!-- 등록/수정 모달 -->
    <div v-if="showModal" class="modal-mask" @click.self="showModal = false">
      <div class="modal">
        <h3>{{ editing ? '담당자 수정' : '담당자 등록' }}</h3>

        <label class="label">이름 *</label>
        <input v-model="form.name" class="input" placeholder="예: 김점장" />

        <label class="label" style="margin-top: 14px">직책</label>
        <input v-model="form.position" class="input" placeholder="예: 점장" />

        <label class="label" style="margin-top: 14px">전화번호 *</label>
        <input v-model="form.phoneNumber" class="input" placeholder="예: 01012345678" @keyup.enter="save" />

        <div v-if="modalError" class="alert alert-error" style="margin-top: 14px">{{ modalError }}</div>

        <div class="modal-actions">
          <button class="btn btn-ghost" @click="showModal = false">취소</button>
          <button class="btn" :disabled="saving" @click="save">{{ editing ? '수정 저장' : '등록' }}</button>
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
.tbl {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  background: var(--card);
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
}
.tbl th,
.tbl td {
  text-align: left;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border);
  font-size: 15px;
}
.tbl th {
  background: #f8fafc;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.tbl tbody tr:last-child td {
  border-bottom: none;
}
.strong {
  font-weight: 700;
}
.center {
  text-align: center;
}
.right {
  text-align: right;
  white-space: nowrap;
}
.right .btn {
  margin-left: 8px;
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
  margin-bottom: 16px;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
}
</style>
