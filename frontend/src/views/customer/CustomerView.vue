<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import { customerApi } from '../../api/customer'
import { fileUrl } from '../../api/client'
import { classifyToSlots, loadClassifier } from '../../lib/classifier'
import { compressImage } from '../../lib/resize'
import PhotoUploadCard from '../../components/PhotoUploadCard.vue'
import Icon from '../../components/Icon.vue'

const phone = ref('')
const status = ref(null) // lookup 결과
const loading = ref(false)
const submitting = ref(false)
const uploadStatus = ref('') // 전송 진행 문구 (압축/업로드 %)
const error = ref('')
const submitHint = ref('')
const lightbox = ref('') // 확대 이미지 URL

// 전송 전까지 로컬에 보관하는 선택 파일 (itemId -> File)
const selectedFiles = reactive({})
// AI가 자동 배정한 항목 (itemId -> true). 직접 선택/이동하면 해제
const autoAssigned = reactive({})

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
  Object.keys(autoAssigned).forEach((k) => delete autoAssigned[k])
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

// --- 사진 한번에 올리기 (온디바이스 자동 분류) ---
const bulkInput = ref(null)
const classifying = ref(false)
const classifyStatus = ref('') // 진행 문구
const autoMsg = ref('') // 완료 배너

// 내부 확인용 분류 상세 (고객에게 표시하지 않음 — 로고 5번 탭으로만 열림)
const aiDebug = ref([]) // [{slot, file, score, margin, second}]
const showAiDebug = ref(false)
let brandTaps = 0
let brandTapTimer = null
function onBrandTap() {
  brandTaps += 1
  clearTimeout(brandTapTimer)
  brandTapTimer = setTimeout(() => (brandTaps = 0), 2000)
  if (brandTaps >= 5) {
    brandTaps = 0
    showAiDebug.value = true
  }
}

function pickBulk() {
  bulkInput.value?.click()
}

async function onBulkFiles(e) {
  const files = [...(e.target.files || [])].filter((f) => f.type.startsWith('image/'))
  e.target.value = ''
  if (!files.length) return

  // 아직 선택 안 된 슬롯에만 배정 (직접 고른 사진은 유지)
  const slots = items.value
    .filter((i) => !selectedFiles[i.itemId])
    .map((i) => ({
      itemId: i.itemId,
      label: i.classificationHint || i.name,
      exampleUrl: i.exampleImageUrl ? fileUrl(i.exampleImageUrl) : null
    }))
  if (!slots.length) return

  classifying.value = true
  autoMsg.value = ''
  error.value = ''
  try {
    classifyStatus.value = 'AI 분류 준비 중…'
    await loadClassifier((pct) => {
      classifyStatus.value = pct != null ? `AI 모델 내려받는 중 ${pct}%` : 'AI 분류 준비 중…'
    })
    const assigned = await classifyToSlots(files, slots, (done, total) => {
      classifyStatus.value = `사진 분류 중 (${done}/${total})`
    })
    assigned.forEach(({ itemId, file }) => {
      selectedFiles[itemId] = file
      autoAssigned[itemId] = true
    })
    // 내부 확인용 기록 (화면에는 노출하지 않음)
    const nameOf = (id) => items.value.find((i) => i.itemId === id)?.name || '-'
    aiDebug.value = assigned.map((a) => ({
      slot: nameOf(a.itemId),
      file: a.file.name,
      score: Math.round(a.score * 100),
      margin: Math.round(a.margin * 100),
      second: a.secondItemId ? nameOf(a.secondItemId) : '-'
    }))
    submitHint.value = ''
    autoMsg.value = `사진 ${assigned.length}장을 자동으로 분류했어요. 각 항목이 맞는지 확인해주세요.`
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (err) {
    error.value = '자동 분류를 사용할 수 없어요. 항목별로 직접 선택해주세요.'
    console.error(err)
  } finally {
    classifying.value = false
  }
}

/** AI 자동 배정 전체 취소 (직접 고른 사진은 유지) */
function undoAuto() {
  Object.keys(autoAssigned).forEach((k) => {
    delete selectedFiles[k]
    delete autoAssigned[k]
  })
  autoMsg.value = ''
}

// --- 위치 바꾸기 (이동/교환) ---
const swapSource = ref(null) // 이동할 사진이 있는 itemId
const swapRows = ref([]) // [{itemId, name, hasFile, thumb}]

function openSwap(itemId) {
  swapSource.value = itemId
  swapRows.value = items.value
    .filter((i) => i.itemId !== itemId)
    .map((i) => {
      const f = selectedFiles[i.itemId]
      return {
        itemId: i.itemId,
        name: i.name,
        hasFile: !!f,
        thumb: f ? URL.createObjectURL(f) : null
      }
    })
}

function closeSwap() {
  swapRows.value.forEach((r) => r.thumb && URL.revokeObjectURL(r.thumb))
  swapRows.value = []
  swapSource.value = null
}

/** 목적지에 사진이 있으면 교환, 없으면 이동 */
function doSwap(targetId) {
  const src = swapSource.value
  const a = selectedFiles[src] || null
  const b = selectedFiles[targetId] || null
  if (b) selectedFiles[src] = b
  else delete selectedFiles[src]
  if (a) selectedFiles[targetId] = a
  else delete selectedFiles[targetId]
  // 사용자가 직접 배치했으므로 두 항목 모두 AI 표시 해제
  delete autoAssigned[src]
  delete autoAssigned[targetId]
  submitHint.value = ''
  closeSwap()
}

function onSelect({ itemId, file }) {
  selectedFiles[itemId] = file
  delete autoAssigned[itemId] // 직접 선택 → AI 표시 해제
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
    // 1) 업로드 전 압축 (긴 변 1920px, JPEG 85% — 전송량 대폭 절감)
    const prepared = {}
    let done = 0
    for (const i of items.value) {
      uploadStatus.value = `사진 최적화 중 (${++done}/${items.value.length})`
      prepared[i.itemId] = await compressImage(selectedFiles[i.itemId])
    }

    // 2) 병렬 업로드 + 전체 진행률
    const totals = {}
    const loadeds = {}
    const refresh = () => {
      const t = Object.values(totals).reduce((a, b) => a + b, 0)
      const l = Object.values(loadeds).reduce((a, b) => a + b, 0)
      uploadStatus.value = t > 0 ? `전송 중 ${Math.min(99, Math.round((l / t) * 100))}%` : '전송 중…'
    }
    refresh()
    await Promise.all(
      items.value.map((i) =>
        customerApi.uploadPhoto(i.itemId, prepared[i.itemId], (e) => {
          totals[i.itemId] = e.total || 0
          loadeds[i.itemId] = e.loaded || 0
          refresh()
        })
      )
    )

    // 3) 제출 확정
    uploadStatus.value = '제출 처리 중…'
    status.value = await customerApi.submit(status.value.companyId)
    clearSelected()
    window.scrollTo({ top: 0 })
  } catch (err) {
    error.value = err.message
  } finally {
    submitting.value = false
    uploadStatus.value = ''
  }
}
</script>

<template>
  <div class="page">
    <!-- 앱바 -->
    <header class="appbar">
      <div class="appbar-inner">
        <span class="brand-mark" @click="onBrandTap"><Icon name="camera" :size="20" /></span>
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

          <!-- 사진 한번에 올리기 (AI 자동 분류) -->
          <button v-if="!allSelected" class="bulk-btn" :disabled="classifying" @click="pickBulk">
            <span class="bulk-title"><Icon name="image" :size="24" /> 사진 한번에 올리기</span>
            <span class="bulk-sub">여러 장을 고르면 AI가 항목에 맞게 자동 분류해요</span>
          </button>
          <input ref="bulkInput" type="file" accept="image/*" multiple hidden @change="onBulkFiles" />

          <div v-if="autoMsg" class="alert alert-info auto-banner">
            <span>{{ autoMsg }}</span>
            <button class="undo-link" @click="undoAuto">모두 지우기</button>
          </div>

          <div class="items">
            <PhotoUploadCard
              v-for="(item, idx) in items"
              :key="item.itemId"
              :item="item"
              :index="idx + 1"
              :file="selectedFiles[item.itemId] || null"
              :auto="!!autoAssigned[item.itemId]"
              :swappable="items.length > 1"
              @select="onSelect"
              @swap="openSwap"
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

        <!-- 위치 바꾸기 바텀시트 -->
        <div v-if="swapSource" class="sheet-mask" @click.self="closeSwap">
          <div class="sheet">
            <p class="sheet-title">이 사진을 어디로 옮길까요?</p>
            <div class="sheet-list">
              <button v-for="row in swapRows" :key="row.itemId" class="sheet-row" @click="doSwap(row.itemId)">
                <span class="sheet-thumb">
                  <img v-if="row.thumb" :src="row.thumb" alt="" />
                  <Icon v-else name="image" :size="24" />
                </span>
                <span class="sheet-info">
                  <b>{{ row.name }}</b>
                  <small>{{ row.hasFile ? '사진 있음 → 서로 교환' : '비어 있음 → 이동' }}</small>
                </span>
                <Icon :name="row.hasFile ? 'swap' : 'arrowRight'" :size="22" class="sheet-arrow" />
              </button>
            </div>
            <button class="btn btn-ghost btn-block sheet-cancel" @click="closeSwap">취소</button>
          </div>
        </div>

        <!-- 자동 분류 진행 오버레이 -->
        <div v-if="classifying" class="lightbox classify-overlay">
          <span class="spinner big"></span>
          <p class="lightbox-hint">{{ classifyStatus }}</p>
          <p class="lightbox-hint dim">사진은 기기 안에서만 처리되고 아직 전송되지 않아요</p>
        </div>

        <!-- 전송 진행 오버레이 -->
        <div v-if="submitting" class="lightbox classify-overlay">
          <span class="spinner big"></span>
          <p class="lightbox-hint">{{ uploadStatus || '전송 준비 중…' }}</p>
          <p class="lightbox-hint dim">화면을 닫지 말고 잠시만 기다려주세요</p>
        </div>

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

    <!-- 내부 확인용: AI 분류 상세 (로고 5번 탭으로만 열림, 고객 대상 아님) -->
    <div v-if="showAiDebug" class="lightbox" @click.self="showAiDebug = false">
      <div class="ai-debug">
        <p class="ai-debug-title">AI 분류 상세 <small>내부 확인용</small></p>
        <template v-if="aiDebug.length">
          <div v-for="(r, i) in aiDebug" :key="i" class="ai-debug-row">
            <b>{{ r.slot }}</b> ← {{ r.file }}
            <span class="ai-debug-meta">
              확신 {{ r.score }}% · 2위({{ r.second }})와 차이 {{ r.margin >= 0 ? '+' : '' }}{{ r.margin }}%p
            </span>
          </div>
        </template>
        <p v-else class="ai-debug-empty">이 세션에서 자동 분류 기록이 없습니다.</p>
        <button class="btn btn-ghost btn-block" style="margin-top: 12px" @click="showAiDebug = false">
          닫기
        </button>
      </div>
    </div>
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

/* 사진 한번에 올리기 */
.bulk-btn {
  width: 100%;
  margin-top: 12px;
  padding: 18px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  background: var(--primary-soft);
  border: 2px dashed var(--primary);
  border-radius: var(--radius);
  cursor: pointer;
  font-family: inherit;
}
.bulk-btn:disabled {
  opacity: 0.6;
}
.bulk-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 23px;
  font-weight: 800;
  color: var(--primary-dark);
}
.bulk-sub {
  font-size: 17px;
  color: var(--text-muted);
}
.classify-overlay .spinner.big {
  width: 44px;
  height: 44px;
  border-width: 5px;
}

/* 자동 분류 배너 + 실행취소 */
.auto-banner {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 19px;
}
.undo-link {
  border: none;
  background: none;
  padding: 4px;
  font-size: 18px;
  font-weight: 700;
  color: var(--primary-dark);
  text-decoration: underline;
  cursor: pointer;
  white-space: nowrap;
  flex: 0 0 auto;
}

/* 위치 바꾸기 바텀시트 */
.sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.sheet {
  width: 100%;
  max-width: 560px;
  max-height: 80vh; /* 항목이 많아도 화면을 넘지 않음 */
  display: flex;
  flex-direction: column;
  background: var(--card);
  border-radius: 20px 20px 0 0;
  padding: 20px 16px calc(16px + env(safe-area-inset-bottom));
}
.sheet-title {
  font-size: 22px;
  font-weight: 800;
  margin: 0 0 14px;
  flex: 0 0 auto;
}
/* 목록만 스크롤 (제목·취소 버튼은 고정) */
.sheet-list {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
}
.sheet-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 10px;
  background: var(--card);
  border: 1px solid var(--border-strong);
  border-radius: 14px;
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.sheet-row:active {
  background: var(--primary-soft);
}
.sheet-thumb {
  width: 56px;
  height: 56px;
  flex: 0 0 auto;
  border-radius: 10px;
  background: #f1f5f9;
  border: 1px solid var(--border);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  overflow: hidden;
}
.sheet-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.sheet-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.sheet-info b {
  font-size: 21px;
  font-weight: 700;
}
.sheet-info small {
  font-size: 16px;
  color: var(--text-muted);
}
.sheet-arrow {
  color: var(--primary);
  flex: 0 0 auto;
}
.sheet-cancel {
  margin-top: 4px;
  font-size: 20px;
  flex: 0 0 auto;
}
.lightbox-hint.dim {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.55);
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

/* 내부 확인용 AI 분류 상세 */
.ai-debug {
  width: 100%;
  max-width: 480px;
  background: var(--card);
  border-radius: var(--radius);
  padding: 18px;
  max-height: 80vh;
  overflow-y: auto;
}
.ai-debug-title {
  font-size: 18px;
  font-weight: 800;
  margin: 0 0 12px;
}
.ai-debug-title small {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  margin-left: 6px;
}
.ai-debug-row {
  font-size: 15px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
  word-break: break-all;
}
.ai-debug-row:last-of-type {
  border-bottom: none;
}
.ai-debug-meta {
  display: block;
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 2px;
}
.ai-debug-empty {
  font-size: 15px;
  color: var(--text-muted);
  margin: 0;
}
</style>
