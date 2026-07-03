<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '../../api/admin'
import { themeApi } from '../../api/theme'
import { THEMES, DESIGN_META, applyTheme, saveThemeLocal, resolveColors } from '../../theme'

const phone = ref('')
const smsConfigured = ref(false)
const loading = ref(true)
const saving = ref(false)
const msg = ref('')
const error = ref('')

// 디자인 / 테마
const designs = DESIGN_META
const presets = Object.entries(THEMES).map(([id, t]) => ({ id, ...t }))
const designId = ref('base')
const themeId = ref('blue')
const primaryColor = ref('') // 커스텀(있으면 우선)
const themeMsg = ref('')
const isBase = computed(() => designId.value === 'base')
const currentPrimary = computed(
  () => resolveColors({ themeId: themeId.value, primaryColor: primaryColor.value || null }).primary
)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const s = await adminApi.getSettings()
    phone.value = s.adminPhoneNumber || ''
    smsConfigured.value = s.smsConfigured
    designId.value = s.designId || 'base'
    themeId.value = s.themeId || 'blue'
    primaryColor.value = s.primaryColor || ''
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function persistTheme() {
  const cfg = {
    designId: designId.value,
    themeId: themeId.value,
    primaryColor: primaryColor.value || null
  }
  applyTheme(cfg) // 즉시 반영
  saveThemeLocal(cfg)
  themeMsg.value = '적용됨'
  try {
    await themeApi.update(cfg)
    setTimeout(() => (themeMsg.value = ''), 1500)
  } catch (err) {
    error.value = err.message
  }
}

function selectDesign(id) {
  designId.value = id
  persistTheme()
}

function selectPreset(id) {
  themeId.value = id
  primaryColor.value = '' // 프리셋 선택 시 커스텀 해제
  persistTheme()
}

function onCustomColor(e) {
  primaryColor.value = e.target.value
  persistTheme()
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

    <template v-else>
      <!-- 기업 디자인 -->
      <section class="card theme-card">
        <div class="theme-head">
          <h2 class="ctitle">기업 디자인</h2>
          <span v-if="themeMsg" class="ok-msg" style="margin: 0">{{ themeMsg }}</span>
        </div>
        <p class="muted" style="font-size: 13px; margin: 0 0 14px">
          기업을 선택하면 해당 디자인(색·폰트·모양)이 전 화면에 즉시 반영·저장됩니다.
        </p>
        <div class="theme-grid">
          <button
            v-for="d in designs"
            :key="d.id"
            class="theme-item"
            :class="{ active: designId === d.id }"
            @click="selectDesign(d.id)"
          >
            <span class="swatches">
              <span v-for="(c, i) in d.colors" :key="i" :style="{ background: c }"></span>
            </span>
            <span class="theme-name">{{ d.name }}</span>
          </button>
        </div>
      </section>

      <!-- 색상 (기본 디자인일 때만) -->
      <section v-if="isBase" class="card theme-card">
        <div class="theme-head">
          <h2 class="ctitle">강조 색상</h2>
        </div>
        <p class="muted" style="font-size: 13px; margin: 0 0 14px">
          기본 디자인의 강조색을 선택합니다.
        </p>

        <div class="theme-grid">
          <button
            v-for="p in presets"
            :key="p.id"
            class="theme-item"
            :class="{ active: !primaryColor && themeId === p.id }"
            @click="selectPreset(p.id)"
          >
            <span class="swatches">
              <span :style="{ background: p.primary }"></span>
              <span :style="{ background: p.dark }"></span>
              <span :style="{ background: p.soft }"></span>
            </span>
            <span class="theme-name">{{ p.name }}</span>
          </button>
        </div>

        <div class="custom-row">
          <label class="label" style="margin: 0">커스텀 색상</label>
          <input
            type="color"
            class="color-input"
            :value="currentPrimary"
            @input="onCustomColor"
          />
          <span class="muted" style="font-size: 13px">{{ primaryColor || '프리셋 사용 중' }}</span>
        </div>
      </section>

      <section class="card" style="max-width: 480px">
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
    </template>
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
.row {
  display: flex;
  gap: 8px;
}
.row > .input {
  flex: 1;
  min-width: 0;
}
.theme-card {
  max-width: 620px;
  margin-bottom: 16px;
}
.theme-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}
.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}
.theme-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1.5px solid var(--border);
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.theme-item:hover {
  border-color: var(--border-strong);
}
.theme-item.active {
  border-color: var(--primary);
  box-shadow: var(--ring);
}
.swatches {
  display: flex;
  gap: 5px;
}
.swatches span {
  flex: 1;
  height: 26px;
  border-radius: 6px;
  border: 0.5px solid rgba(0, 0, 0, 0.08);
}
.theme-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}
.custom-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.color-input {
  width: 46px;
  height: 34px;
  padding: 2px;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
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
