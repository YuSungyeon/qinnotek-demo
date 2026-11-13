<script setup>
import { onMounted, ref } from 'vue'
import { Pencil, Plus, Trash2 } from 'lucide-vue-next'
import { adminApi } from '../../api/admin'
import { phone } from '../../lib/format'

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

const fmtPhone = phone

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1>담당자</h1>
        <p class="page-sub">제출 알림 문자를 받을 사람입니다. 기업별 지정은 기업 상세에서 합니다.</p>
      </div>
      <button class="btn" @click="openCreate"><Plus :size="17" /> 담당자 등록</button>
    </div>

    <div v-if="error" class="alert" style="margin-bottom: 16px">{{ error }}</div>
    <div v-if="loading" class="muted">불러오는 중…</div>

    <section v-else class="list">
      <div class="list-head"><span>이름</span><span>전화번호</span><span>지정된 기업</span><span></span></div>
      <div v-for="m in list" :key="m.id" class="row-item">
        <span class="name">
          <span class="avatar">{{ m.name.slice(0, 1) }}</span>
          <span>
            <b>{{ m.name }}</b>
            <small>{{ m.position || '직책 없음' }}</small>
          </span>
        </span>
        <span class="tel">{{ fmtPhone(m.phoneNumber) }}</span>
        <span class="tags">
          <span v-for="c in m.companies || []" :key="c" class="tag">{{ c }}</span>
          <small v-if="!m.companies || !m.companies.length" class="faint">지정된 기업 없음</small>
        </span>
        <span class="tools">
          <button class="btn btn-soft btn-xs" @click="openEdit(m)"><Pencil :size="13" /> 수정</button>
          <button class="icon-btn" :aria-label="`${m.name} 삭제`" title="삭제" @click="remove(m)">
            <Trash2 :size="15" />
          </button>
        </span>
      </div>
      <p v-if="list.length === 0" class="empty">등록된 담당자가 없습니다.</p>
    </section>

    <!-- 등록/수정 -->
    <div v-if="showModal" class="modal-mask" @click.self="showModal = false">
      <div class="modal">
        <h3>{{ editing ? '담당자 수정' : '담당자 등록' }}</h3>

        <label class="label" for="mgr-name">이름 *</label>
        <input id="mgr-name" v-model="form.name" class="input" placeholder="예: 김현우" />

        <label class="label" for="mgr-position">직책</label>
        <input id="mgr-position" v-model="form.position" class="input" placeholder="예: 구매팀장" />

        <label class="label" for="mgr-phone">전화번호 *</label>
        <input
          id="mgr-phone"
          v-model="form.phoneNumber"
          class="input"
          type="tel"
          inputmode="numeric"
          placeholder="예: 01012345678"
          @keyup.enter="save"
        />

        <div v-if="modalError" class="alert" style="margin-top: 14px">{{ modalError }}</div>

        <div class="modal-actions">
          <button class="btn btn-soft" @click="showModal = false">취소</button>
          <button class="btn" :disabled="saving" @click="save">{{ editing ? '저장' : '등록' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.list {
  border: 1px solid var(--hairline-soft);
  border-radius: var(--r-md);
  overflow: hidden;
}
.list-head,
.row-item {
  display: grid;
  grid-template-columns: 1.6fr 1.2fr 2fr auto;
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
  min-height: 76px;
  border-top: 1px solid var(--canvas-soft);
}
.list-head + .row-item {
  border-top: 0;
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
}
.name b {
  font-size: 16px;
  font-weight: var(--w-heading);
}
.name small {
  font-size: 13px;
  color: var(--muted);
}
/* 사람은 동그라미 (design.md: 아바타는 원, 앱·기업은 둥근 사각) */
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--canvas-soft);
  font-weight: var(--w-heading);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.tel {
  font-size: 14px;
  font-variant-numeric: tabular-nums;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.tag {
  padding: 6px 10px;
  border-radius: var(--r-full);
  background: var(--canvas-soft);
  font-size: 12px;
  font-weight: 600;
}
.tags small {
  font-size: 13px;
}
.tools {
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
.icon-btn:hover {
  background: var(--canvas-soft);
  color: var(--ink);
}
.empty {
  margin: 0;
  padding: 32px;
  text-align: center;
  color: var(--muted);
}
.modal .label {
  margin-top: 16px;
}
.modal h3 + .label {
  margin-top: 12px;
}

@media (max-width: 840px) {
  .list-head {
    display: none;
  }
  .row-item {
    grid-template-columns: 1fr auto;
    gap: 8px 12px;
    padding: 16px 20px;
  }
  .tel,
  .tags {
    grid-column: 1 / -1;
  }
  /* 수정·삭제는 이름 오른쪽 첫 줄에 */
  .tools {
    grid-row: 1;
    grid-column: 2;
  }
}
</style>
