<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import { customerApi } from '../../api/customer'
import PhotoUploadCard from '../../components/PhotoUploadCard.vue'
import Icon from '../../components/Icon.vue'

const phone = ref('')
const status = ref(null) // lookup 결과
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const submitHint = ref('')
const lightbox = ref('') // 확대 이미지 URL

// 전송 전까지 로컬에 보관하는 선택 파일 (itemId -> File)
const selectedFiles = reactive({})

const items = computed(() => status.value?.items ?? [])
const selectedCount = computed(() => items.value.filter((i) => selectedFiles[i.itemId]).length)
const allSelected = computed(
  () => items.value.length > 0 && items.value.every((i) => selectedFiles[i.itemId])
)
const progressPct = computed(() =>
  items.value.length ? Math.round((selectedCount.value / items.value.length) * 100) : 0
)
const showUploadUI = computed(
  () => status.value && ['INITIAL', 'RETURNED'].includes(status.value.state)
)

/** 입력 중 자동 하이픈 (010-1234-5678) */
function onPhoneInput(e) {
  const d = e.target.value.replace(/\D/g, '').slice(0, 11)
  let out = d
  if (d.length > 3 && d.length <= 7) out = `${d.slice(0, 3)}-${d.slice(3)}`
  else if (d.length > 7) out = `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7)}`
  phone.value = out
}

function clearSelected() {
  Object.keys(selectedFiles).forEach((k) => delete selectedFiles[k])
}

function scrollToItem(itemId, block = 'center') {
  const el = document.querySelector(`[data-item-id="${itemId}"]`)
  el?.scrollIntoView({ behavior: 'smooth', block })
}

async function lookup() {
  const digits = phone.value.replace(/\D/g, '')
  if (!digits) return
  error.value = ''
  loading.value = true
  status.value = null
  clearSelected()
  try {
    status.value = await customerApi.lookup(digits)
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

function reset() {
  status.value = null
  error.value = ''
  submitHint.value = ''
  clearSelected()
}

function onSelect({ itemId, file }) {
  selectedFiles[itemId] = file
  submitHint.value = ''
  // 다음 미선택 항목으로 부드럽게 이동 (없으면 전송 바로)
  nextTick(() => {
    const next = items.value.find((i) => !selectedFiles[i.itemId])
    if (next) {
      scrollToItem(next.itemId, 'start')
    } else {
      document.querySelector('.submit-bar')?.scrollIntoView({ behavior: 'smooth', block: 'end' })
    }
  })
}

async function submit() {
  submitHint.value = ''
  if (!allSelected.value) {
    // 미선택 항목으로 스크롤 + 안내
    const missing = items.value.find((i) => !selectedFiles[i.itemId])
    if (missing) {
      submitHint.value = `'${missing.name}' 사진을 선택해주세요.`
      scrollToItem(missing.itemId, 'center')
    }
    return
  }
  submitting.value = true
  error.value = ''
  try {
    // 전송 시 한 번에 업로드 → 제출
    await Promise.all(
      items.value.map((i) => customerApi.uploadPhoto(i.itemId, selectedFiles[i.itemId]))
    )
    status.value = await customerApi.submit(status.value.companyId)
    clearSelected()
    window.scrollTo({ top: 0 })
  } catch (err) {
    error.value = err.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <!-- 앱바 -->
    <header class="appbar">
      <div class="appbar-inner">
        <span class="brand-mark"><Icon name="camera" :size="20" /></span>
        <span class="brand-name">사진 제출 센터</span>
      </div>
    </header>

    <main class="wrap">
      <!-- ① 시작(전화번호 확인) -->
      <template v-if="!status">
        <section class="hero">
          <h1>안녕하세요!<br />사진 제출을 도와드릴게요</h1>
          <p class="hero-sub">기업에 등록된 전화번호를 입력하면<br />제출할 사진 목록을 확인할 수 있어요.</p>
        </section>

        <section class="card lookup-card">
          <label class="lk-label" for="phone">전화번호</label>
          <input
            id="phone"
            :value="phone"
            class="input lk-input"
            type="tel"
            inputmode="numeric"
            autocomplete="tel"
            placeholder="010-0000-0000"
            @input="onPhoneInput"
            @keyup.enter="lookup"
          />
          <div v-if="error" class="alert alert-error" style="margin-top: 12px">{{ error }}</div>
          <button class="btn btn-lg btn-block" style="margin-top: 14px" :disabled="loading" @click="lookup">
            <span v-if="loading" class="spinner"></span>
            <span v-else>시작하기</span>
          </button>
          <p class="privacy">입력하신 전화번호는 본인 확인 용도로만 사용됩니다.</p>
        </section>

        <ol class="steps">
          <li><span class="step-no">1</span><span>전화번호<br />확인</span></li>
          <li class="step-line"></li>
          <li><span class="step-no">2</span><span>사진<br />촬영</span></li>
          <li class="step-line"></li>
          <li><span class="step-no">3</span><span>제출<br />완료</span></li>
        </ol>
      </template>

      <!-- ② 조회 결과 -->
      <template v-else>
        <!-- 검수 중 / 완료 -->
        <template v-if="!showUploadUI">
          <section class="company-head">
            <div>
              <div class="company-name">{{ status.companyName }}</div>
              <div class="muted" style="font-size: 19px">사진 제출</div>
            </div>
            <button class="btn btn-ghost btn-sm" @click="reset">다시 조회</button>
          </section>

          <section v-if="status.state === 'UNDER_REVIEW'" class="card notice">
            <div class="notice-icon review"><Icon name="clock" :size="44" /></div>
            <p class="notice-msg">제출이 완료되었어요</p>
            <p class="notice-sub muted">{{ status.message }}<br />검수가 끝나면 안내해 드릴게요.</p>
          </section>

          <section v-else-if="status.state === 'COMPLETED'" class="card notice">
            <div class="notice-icon done"><Icon name="check" :size="44" /></div>
            <p class="notice-msg">모든 사진이 통과되었어요</p>
            <p class="notice-sub muted">제출해 주셔서 감사합니다.</p>
          </section>
        </template>

        <!-- 업로드 (최초 제출 / 반환) -->
        <template v-else>
          <div class="progress-head">
            <div class="ph-top">
              <div>
                <div class="company-name">{{ status.companyName }}</div>
                <div class="ph-sub muted">
                  {{ status.state === 'RETURNED' ? '반환된 사진을 다시 촬영해 주세요' : '필요한 사진을 촬영해 주세요' }}
                </div>
              </div>
              <button class="btn btn-ghost btn-sm" @click="reset">다시 조회</button>
            </div>
            <div class="ph-bar"><div class="ph-fill" :style="{ width: progressPct + '%' }"></div></div>
            <div class="ph-count">
              사진 {{ items.length }}장 중 <b>{{ selectedCount }}장</b> 선택됨
            </div>
          </div>

          <div v-if="status.state === 'RETURNED'" class="alert alert-error banner">
            {{ status.message }}
          </div>

          <div class="items">
            <PhotoUploadCard
              v-for="(item, idx) in items"
              :key="item.itemId"
              :item="item"
              :index="idx + 1"
              @select="onSelect"
              @zoom="lightbox = $event"
            />
          </div>

          <div v-if="submitHint" class="alert alert-error" style="margin: 14px 0 0">{{ submitHint }}</div>
          <div v-if="error" class="alert alert-error" style="margin: 14px 0 0">{{ error }}</div>

          <div class="submit-bar">
            <p v-if="!allSelected && !submitting" class="bar-hint">
              사진 {{ items.length - selectedCount }}장이 더 필요해요
            </p>
            <button
              class="btn btn-lg btn-block"
              :class="{ 'btn-success': allSelected }"
              :disabled="submitting"
              @click="submit"
            >
              <span v-if="submitting" class="spinner"></span>
              <span v-else-if="allSelected">전송하기</span>
              <span v-else>전송 ({{ selectedCount }}/{{ items.length }})</span>
            </button>
          </div>
        </template>

        <!-- 이미지 확대 -->
        <div v-if="lightbox" class="lightbox" @click="lightbox = ''">
          <img :src="lightbox" alt="확대 이미지" />
          <p class="lightbox-hint">화면을 누르면 닫혀요</p>
        </div>
      </template>
    </main>

    <footer class="foot muted">
      기업 요청 사진을 안전하게 제출하는 페이지입니다.
    </footer>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 앱바 */
.appbar {
  background: var(--card);
  border-bottom: 1px solid var(--border);
}
.appbar-inner {
  max-width: 560px;
  margin: 0 auto;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: var(--primary);
  flex: 0 0 auto;
}
.brand-name {
  font-size: 19px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.wrap {
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
  padding: 20px 16px 40px;
  font-size: 24px; /* 고객 기본 글자 1.5배 */
  flex: 1;
}
.wrap .input {
  font-size: 24px;
  padding: 14px 16px;
}
.wrap .btn-lg {
  font-size: 25px;
  padding: 16px 20px;
}
.wrap .btn-sm {
  font-size: 19px;
}

/* 시작 화면 */
.hero {
  padding: 26px 4px 22px;
}
.hero h1 {
  font-size: 32px;
  font-weight: 800;
  line-height: 1.3;
  letter-spacing: -0.02em;
  margin: 0 0 12px;
}
.hero-sub {
  font-size: 21px;
  line-height: 1.55;
  color: var(--text-muted);
  margin: 0;
}
.lookup-card {
  padding: 22px 20px;
}
.lk-label {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-muted);
  margin-bottom: 8px;
}
.lk-input {
  text-align: center;
  letter-spacing: 0.04em;
  font-weight: 700;
}
.privacy {
  margin: 14px 0 0;
  font-size: 16px;
  color: var(--text-muted);
  text-align: center;
}

/* 3단계 절차 */
.steps {
  list-style: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin: 30px 0 0;
  padding: 0;
}
.steps li:not(.step-line) {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 600;
  color: var(--text-muted);
  text-align: center;
  line-height: 1.3;
}
.step-no {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 19px;
  font-weight: 800;
  color: var(--primary);
  background: var(--primary-soft);
}
.step-line {
  flex: 0 0 28px;
  height: 2px;
  background: var(--border-strong);
  margin-bottom: 30px;
}

/* 결과 공통 */
.company-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.company-name {
  font-size: 29px;
  font-weight: 800;
  letter-spacing: -0.02em;
}
.notice {
  text-align: center;
  padding: 48px 24px;
}
.notice-icon {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
}
.notice-icon.review {
  color: var(--primary);
  background: var(--primary-soft);
}
.notice-icon.done {
  color: var(--success);
  background: #f0fdf4;
}
.notice-msg {
  font-size: 27px;
  font-weight: 800;
  letter-spacing: -0.02em;
  margin: 0 0 10px;
}
.notice-sub {
  font-size: 20px;
  line-height: 1.55;
  margin: 0;
}
.banner {
  margin: 12px 0 0;
  font-weight: 600;
  font-size: 21px;
}
.items {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 14px;
}

/* 상단 진행 헤더 (고정하지 않고 콘텐츠와 함께 스크롤 — 화면을 넓게 사용) */
.progress-head {
  padding: 8px 0 12px;
  margin-bottom: 2px;
}
.ph-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}
.ph-sub {
  font-size: 19px;
  margin-top: 2px;
}
.ph-bar {
  height: 12px;
  border-radius: 999px;
  background: #e5e7eb;
  overflow: hidden;
}
.ph-fill {
  height: 100%;
  border-radius: 999px;
  background: var(--success);
  transition: width 0.3s ease;
}
.ph-count {
  margin-top: 8px;
  font-size: 19px;
  color: var(--text-muted);
}
.ph-count b {
  color: var(--text);
  font-weight: 800;
}

/* 하단 고정 전송 바 */
.submit-bar {
  position: sticky;
  bottom: 0;
  margin: 18px -16px 0;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  background: rgba(245, 246, 248, 0.92);
  backdrop-filter: blur(8px);
  border-top: 1px solid var(--border);
}
.bar-hint {
  text-align: center;
  margin: 0 0 6px;
  font-size: 17px;
  color: var(--text-muted);
}

/* 이미지 확대 */
.lightbox {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(0, 0, 0, 0.88);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  padding: 20px;
}
.lightbox img {
  max-width: 96vw;
  max-height: 82vh;
  border-radius: 10px;
}
.lightbox-hint {
  color: rgba(255, 255, 255, 0.8);
  font-size: 19px;
  margin: 0;
}

/* 푸터 */
.foot {
  text-align: center;
  font-size: 15px;
  padding: 18px 16px calc(18px + env(safe-area-inset-bottom));
}
</style>
