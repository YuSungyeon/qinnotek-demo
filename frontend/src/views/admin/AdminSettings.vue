<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../../api/admin'

// 관리자 설정 - 지금은 제출 알림 문자 on/off 하나.
// (화면 디자인은 design.md 모노크롬으로 고정해 색상·디자인 선택 기능은 없앴다 - style.css)
const smsEnabled = ref(true)
const smsKeyReady = ref(false)
const smsToggling = ref(false)
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const s = await adminApi.getSettings()
    smsEnabled.value = s.smsEnabled
    smsKeyReady.value = s.smsKeyReady
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function setSms(next) {
  if (next === smsEnabled.value || smsToggling.value) return
  smsEnabled.value = next // 바로 반영하고, 실패하면 되돌린다
  smsToggling.value = true
  error.value = ''
  try {
    const s = await adminApi.updateSmsEnabled(next)
    smsEnabled.value = s.smsEnabled
    smsKeyReady.value = s.smsKeyReady
  } catch (err) {
    smsEnabled.value = !next
    error.value = err.message
  } finally {
    smsToggling.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1>설정</h1>
        <p class="page-sub">알림 문자 발송을 관리합니다.</p>
      </div>
    </div>

    <div v-if="error" class="alert" style="margin-bottom: 16px">{{ error }}</div>
    <div v-if="loading" class="muted">불러오는 중…</div>

    <section v-else class="settings">
      <article class="card">
        <div class="setting-row">
          <div>
            <b>제출 알림 문자</b>
            <p class="muted small">고객이 제출하면 기업에 지정된 담당자에게 문자를 보냅니다.</p>
          </div>
          <div class="seg" role="radiogroup" aria-label="제출 알림 문자">
            <button role="radio" :aria-checked="smsEnabled" :class="{ on: smsEnabled }" :disabled="smsToggling" @click="setSms(true)">
              켜짐
            </button>
            <button role="radio" :aria-checked="!smsEnabled" :class="{ on: !smsEnabled }" :disabled="smsToggling" @click="setSms(false)">
              꺼짐
            </button>
          </div>
        </div>

        <!-- 서버 키 상태: 켜 두었는데 실제로는 못 보내는 경우를 알려준다 -->
        <p v-if="smsEnabled && !smsKeyReady" class="state-note">
          켜져 있지만 서버에 Solapi 키(SOLAPI_API_KEY / SOLAPI_API_SECRET)가 없어 실제로는 보내지 않습니다.
        </p>
        <p v-else-if="smsEnabled" class="state-note ready">발송 준비 완료 - 지정된 담당자에게 문자가 전송됩니다.</p>
        <p v-else class="state-note">꺼져 있어 제출 알림 문자를 보내지 않습니다. 제출 링크 문자는 따로 보낼 수 있습니다.</p>
      </article>
    </section>
  </div>
</template>

<style scoped>
.settings {
  display: grid;
  gap: 16px;
  max-width: 760px;
}
.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.setting-row b {
  font-size: 16px;
  font-weight: var(--w-heading);
}
.setting-row p {
  margin: 4px 0 0;
}
.state-note {
  margin: 18px 0 0;
  padding: 12px 16px;
  border-radius: var(--r-sm);
  background: var(--canvas-soft);
  font-size: 14px;
  line-height: 1.5;
}
.state-note.ready {
  font-weight: 600;
}
</style>
