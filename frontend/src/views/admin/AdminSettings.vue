<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../../api/admin'

const phone = ref('')
const smsConfigured = ref(false)
const loading = ref(true)
const saving = ref(false)
const msg = ref('')
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const s = await adminApi.getSettings()
    phone.value = s.adminPhoneNumber || ''
    smsConfigured.value = s.smsConfigured
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  msg.value = ''
  error.value = ''
  try {
    const s = await adminApi.updateAdminPhone(phone.value.trim())
    phone.value = s.adminPhoneNumber || ''
    smsConfigured.value = s.smsConfigured
    msg.value = '저장되었습니다.'
    setTimeout(() => (msg.value = ''), 2000)
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head"><h1>관리자 설정</h1></div>
    <p class="muted" style="margin-top: -8px; margin-bottom: 18px">
      고객이 사진을 제출하면 이 번호로 알림 문자가 발송됩니다.
    </p>

    <div v-if="error" class="alert alert-error" style="margin-bottom: 16px">{{ error }}</div>
    <div v-if="loading" class="muted">불러오는 중…</div>

    <section v-else class="card" style="max-width: 480px">
      <h2 class="ctitle">알림 문자 수신 번호</h2>
      <label class="label">관리자 전화번호</label>
      <div class="row">
        <input v-model="phone" class="input" placeholder="예: 01012345678" @keyup.enter="save" />
        <button class="btn" :disabled="saving" @click="save">저장</button>
      </div>
      <span v-if="msg" class="ok-msg">{{ msg }}</span>

      <div class="sms-status" :class="smsConfigured ? 'ok' : 'off'">
        <strong>SMS 연동:</strong>
        <span v-if="smsConfigured">활성화됨 (발송 가능)</span>
        <span v-else>비활성화 — 서버에 Solapi 키(SOLAPI_API_KEY/SECRET)와 SMS_ENABLED=true 설정 필요</span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page-head h1 {
  font-size: 22px;
  margin-bottom: 6px;
}
.ctitle {
  font-size: 16px;
  margin-bottom: 14px;
}
.row {
  display: flex;
  gap: 8px;
}
.ok-msg {
  display: inline-block;
  margin-top: 10px;
  color: var(--success);
  font-size: 14px;
  font-weight: 600;
}
.sms-status {
  margin-top: 18px;
  padding: 12px 14px;
  border-radius: 8px;
  font-size: 14px;
}
.sms-status.ok {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #15803d;
}
.sms-status.off {
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: var(--warning);
}
</style>
