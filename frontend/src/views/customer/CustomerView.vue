<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { customerApi } from '../../api/customer'
import { fileUrl } from '../../api/client'
import { classifyToSlots, loadClassifier } from '../../lib/classifier'
import { compressImage } from '../../lib/resize'
import { ANY_ACCEPT, kindOf } from '../../lib/filekind'
import PhotoUploadCard from '../../components/PhotoUploadCard.vue'
import DocPreview from '../../components/DocPreview.vue'
import Icon from '../../components/Icon.vue'
import { CircleAlert, Inbox, Link2, TimerOff } from 'lucide-vue-next'

// 관리자가 보낸 제출 링크(/s/:token)로만 들어온다
const props = defineProps({ token: { type: String, default: '' } })

const status = ref(null) // 링크를 열었을 때의 제출 상태
const loading = ref(false)
const linkError = ref('') // '' | 'missing'(토큰 없음/없는 링크) | 'expired' | 'empty'(요청 자료 없음) | 'failed'
const linkErrorMessage = ref('')
const submitting = ref(false)
const uploadStatus = ref('') // 전송 진행 문구 (압축/업로드 %)
const error = ref('')
const submitHint = ref('')
const lightbox = ref('') // 확대 이미지 URL

// 항목별 선택 상태 (itemId -> { kind: 'photo', file } | { kind: 'doc', staged })
// 사진은 전송 시 업로드하고, 문서는 분류를 위해 이미 서버에 올라가 있다.
const selection = reactive({})
// AI가 자동 배정한 항목 (itemId -> true). 직접 선택/이동하면 해제
const autoAssigned = reactive({})
// 어느 항목인지 판단하지 못한 서류 (사용자가 직접 배치)
const unassignedDocs = ref([])

const items = computed(() => status.value?.items ?? [])
const selectedCount = computed(() => items.value.filter((i) => selection[i.itemId]).length)
const allSelected = computed(
  () => items.value.length > 0 && items.value.every((i) => selection[i.itemId])
)
const progressPct = computed(() =>
  items.value.length ? Math.round((selectedCount.value / items.value.length) * 100) : 0
)

// 항목 구성에 맞춰 문구를 바꾼다 (사진만 / 서류만 / 섞임)
const unit = computed(() => {
  const types = new Set(items.value.map((i) => i.fileType || 'PHOTO'))
  // obj: 수량 뒤 목적격 조사 (5건을 / 5개를 / 5장을)
  if (types.size === 1 && types.has('DOCUMENT')) return { noun: '서류', counter: '건', obj: '을', verb: '올려' }
  if (types.has('DOCUMENT')) return { noun: '자료', counter: '개', obj: '를', verb: '올려' }
  return { noun: '사진', counter: '장', obj: '을', verb: '촬영해' }
})
const showUploadUI = computed(
  () => status.value && ['INITIAL', 'RETURNED'].includes(status.value.state)
)

function clearSelected() {
  Object.keys(selection).forEach((k) => delete selection[k])
  Object.keys(autoAssigned).forEach((k) => delete autoAssigned[k])
  unassignedDocs.value = []
}

function scrollToItem(itemId, block = 'center') {
  const el = document.querySelector(`[data-item-id="${itemId}"]`)
  el?.scrollIntoView({ behavior: 'smooth', block })
}

async function openLink(token) {
  status.value = null
  error.value = ''
  linkError.value = ''
  clearSelected()
  if (!token) {
    linkError.value = 'missing'
    return
  }
  loading.value = true
  try {
    status.value = await customerApi.open(token)
  } catch (err) {
    if (err.status === 410) linkError.value = 'expired'
    else if (err.status === 404 && /자료/.test(err.message)) linkError.value = 'empty'
    else if (err.status === 404) linkError.value = 'missing'
    else {
      linkError.value = 'failed'
      linkErrorMessage.value = err.message
    }
  } finally {
    loading.value = false
  }
}
watch(() => props.token, openLink, { immediate: true })

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

const nameOf = (id) => items.value.find((i) => i.itemId === id)?.name || '-'
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function onBulkFiles(e) {
  const files = [...(e.target.files || [])]
  e.target.value = ''
  const photos = files.filter((f) => kindOf(f) === 'photo')
  const docs = files.filter((f) => kindOf(f) === 'doc')
  if (!photos.length && !docs.length) return

  classifying.value = true
  autoMsg.value = ''
  error.value = ''
  const done = []
  try {
    // 서류는 서버가 읽어야 해서 먼저 착수시키고, 사진 분류는 그동안 기기에서 돌린다
    const docTask = docs.length ? stageAndPoll(docs) : Promise.resolve(0)
    if (photos.length) {
      const n = await classifyPhotos(photos)
      if (n) done.push(`사진 ${n}장`)
    }
    const docCount = await docTask
    if (docCount) done.push(`서류 ${docCount}건`)

    submitHint.value = ''
    if (done.length) {
      autoMsg.value = `${done.join('과 ')}을 자동으로 분류했어요. 각 항목이 맞는지 확인해주세요.`
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  } catch (err) {
    error.value = err.message || '자동 분류를 사용할 수 없어요. 항목별로 직접 선택해주세요.'
    console.error(err)
  } finally {
    classifying.value = false
    classifyStatus.value = ''
  }
}

/** 사진: 기기 안에서 CLIP 으로 분류 (사진을 받는 항목에만 배정) */
async function classifyPhotos(files) {
  const slots = items.value
    .filter((i) => !selection[i.itemId] && i.fileType !== 'DOCUMENT')
    .map((i) => ({
      itemId: i.itemId,
      label: i.classificationHint || i.name,
      exampleUrl: i.exampleImageUrl ? fileUrl(i.exampleImageUrl) : null
    }))
  if (!slots.length) return 0

  classifyStatus.value = 'AI 분류 준비 중…'
  await loadClassifier((pct) => {
    classifyStatus.value = pct != null ? `AI 모델 내려받는 중 ${pct}%` : 'AI 분류 준비 중…'
  })
  const assigned = await classifyToSlots(files, slots, (n, total) => {
    classifyStatus.value = `사진 분류 중 (${n}/${total})`
  })
  assigned.forEach(({ itemId, file }) => {
    selection[itemId] = { kind: 'photo', file }
    autoAssigned[itemId] = true
  })
  // 내부 확인용 기록 (화면에는 노출하지 않음)
  aiDebug.value = assigned.map((a) => ({
    slot: nameOf(a.itemId),
    file: a.file.name,
    score: Math.round(a.score * 100),
    margin: Math.round(a.margin * 100),
    second: a.secondItemId ? nameOf(a.secondItemId) : '-'
  }))
  return assigned.length
}

/** 서류: 서버에 올려 PDF 변환·텍스트 추출이 끝날 때까지 기다린 뒤 배정 결과를 받는다 */
async function stageAndPoll(files) {
  let uploaded = 0
  classifyStatus.value = `서류 올리는 중 (0/${files.length})`
  await Promise.all(
    files.map((f) =>
      customerApi.stageDocument(props.token, f).then(() => {
        classifyStatus.value = `서류 올리는 중 (${++uploaded}/${files.length})`
      })
    )
  )

  // 변환은 비동기라 상태가 확정될 때까지 폴링한다 (오래 걸려도 90초에서 멈춤)
  const deadline = Date.now() + 90000
  let list = []
  for (;;) {
    list = await customerApi.listStagedDocuments(props.token)
    const pending = list.filter((d) => d.conversionStatus === 'PENDING').length
    if (!pending || Date.now() > deadline) break
    classifyStatus.value = `서류 분석 중 (${list.length - pending}/${list.length})`
    await sleep(1500)
  }
  return applyStaged(list)
}

function applyStaged(list) {
  const assignedIds = new Set()
  list.forEach((doc) => {
    if (doc.suggestedItemId && !selection[doc.suggestedItemId]) {
      selection[doc.suggestedItemId] = { kind: 'doc', staged: doc }
      autoAssigned[doc.suggestedItemId] = true
      assignedIds.add(doc.stagedId)
    }
  })
  // 배정하지 못한 서류는 사용자가 직접 항목을 고르게 남긴다
  unassignedDocs.value = list.filter((d) => !assignedIds.has(d.stagedId))
  aiDebug.value = [
    ...aiDebug.value,
    ...list.map((d) => ({
      slot: d.suggestedItemId ? nameOf(d.suggestedItemId) : '(배정 없음)',
      file: d.originalFileName,
      score: Math.round((d.score || 0) * 100),
      margin: 0,
      second: d.confidence || '-'
    }))
  ]
  return assignedIds.size
}

/** 미배정 서류를 특정 항목에 직접 배치 */
function placeDoc(doc, itemId) {
  selection[itemId] = { kind: 'doc', staged: doc }
  delete autoAssigned[itemId]
  unassignedDocs.value = unassignedDocs.value.filter((d) => d.stagedId !== doc.stagedId)
  docPicker.value = null
  submitHint.value = ''
  scrollToItem(itemId, 'center')
}

/** 서류 올리기 취소 (서버에서도 삭제) */
async function removeDoc(doc) {
  unassignedDocs.value = unassignedDocs.value.filter((d) => d.stagedId !== doc.stagedId)
  try {
    await customerApi.deleteStagedDocument(props.token, doc.stagedId)
  } catch (err) {
    console.error(err)
  }
}

/** AI 자동 배정 전체 취소 (직접 고른 것은 유지) */
function undoAuto() {
  Object.keys(autoAssigned).forEach((k) => {
    const picked = selection[k]
    if (picked?.kind === 'doc') {
      // 서버에는 남겨두고 미배정 목록으로 되돌린다
      unassignedDocs.value = [...unassignedDocs.value, picked.staged]
    }
    delete selection[k]
    delete autoAssigned[k]
  })
  autoMsg.value = ''
}

// --- 서류를 직접 배치할 항목 고르기 ---
const docPicker = ref(null) // 배치할 서류
const docUploading = ref(null) // 서류 업로드 중인 itemId
const docTargets = computed(() =>
  items.value
    .filter((i) => i.fileType !== 'PHOTO')
    .map((i) => ({ itemId: i.itemId, name: i.name, hasFile: !!selection[i.itemId] }))
)

// --- 위치 바꾸기 (이동/교환) ---
const swapSource = ref(null) // 이동할 항목의 itemId
const swapRows = ref([]) // [{itemId, name, hasFile, isDoc, thumb}]

function openSwap(itemId) {
  const moving = selection[itemId]
  swapSource.value = itemId
  swapRows.value = items.value
    .filter((i) => i.itemId !== itemId)
    // 사진은 사진 슬롯으로만, 서류는 서류 슬롯으로만 옮길 수 있다
    .filter((i) => (moving?.kind === 'doc' ? i.fileType !== 'PHOTO' : i.fileType !== 'DOCUMENT'))
    .map((i) => {
      const picked = selection[i.itemId]
      return {
        itemId: i.itemId,
        name: i.name,
        hasFile: !!picked,
        isDoc: picked?.kind === 'doc',
        thumb: picked?.kind === 'photo' ? URL.createObjectURL(picked.file) : null
      }
    })
}

function closeSwap() {
  swapRows.value.forEach((r) => r.thumb && URL.revokeObjectURL(r.thumb))
  swapRows.value = []
  swapSource.value = null
}

/** 목적지에 이미 있으면 교환, 없으면 이동 */
function doSwap(targetId) {
  const src = swapSource.value
  const a = selection[src] || null
  const b = selection[targetId] || null
  if (b) selection[src] = b
  else delete selection[src]
  if (a) selection[targetId] = a
  else delete selection[targetId]
  // 사용자가 직접 배치했으므로 두 항목 모두 AI 표시 해제
  delete autoAssigned[src]
  delete autoAssigned[targetId]
  submitHint.value = ''
  closeSwap()
}

function onSelect({ itemId, file }) {
  selection[itemId] = { kind: 'photo', file }
  delete autoAssigned[itemId] // 직접 선택 → AI 표시 해제
  submitHint.value = ''
  goToNext()
}

/** 항목 카드에서 서류를 직접 고른 경우 - 즉시 서버로 올린다 */
async function onSelectDoc({ itemId, file }) {
  error.value = ''
  docUploading.value = itemId
  try {
    const staged = await customerApi.stageDocument(props.token, file)
    selection[itemId] = { kind: 'doc', staged }
    delete autoAssigned[itemId]
    submitHint.value = ''
    // 변환이 끝나면 카드의 상태 칩이 갱신되도록 한 번 더 조회한다
    refreshStagedStatus(staged.stagedId, itemId)
    goToNext()
  } catch (err) {
    error.value = err.message
  } finally {
    docUploading.value = null
  }
}

/** 특정 서류의 변환 상태가 확정될 때까지 짧게 폴링해 카드 표시를 갱신 */
async function refreshStagedStatus(stagedId, itemId) {
  const deadline = Date.now() + 90000
  while (Date.now() < deadline) {
    await sleep(1500)
    let list
    try {
      list = await customerApi.listStagedDocuments(props.token)
    } catch {
      return
    }
    const found = list.find((d) => d.stagedId === stagedId)
    if (!found) return
    if (selection[itemId]?.staged?.stagedId === stagedId) {
      selection[itemId] = { kind: 'doc', staged: found }
    }
    if (found.conversionStatus !== 'PENDING') return
  }
}

function goToNext() {
  // 다음 미선택 항목으로 부드럽게 이동 (없으면 전송 바로)
  nextTick(() => {
    const next = items.value.find((i) => !selection[i.itemId])
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
    const missing = items.value.find((i) => !selection[i.itemId])
    if (missing) {
      submitHint.value = `'${missing.name}' 항목을 선택해주세요.`
      scrollToItem(missing.itemId, 'center')
    }
    return
  }
  submitting.value = true
  error.value = ''
  try {
    const photoItems = items.value.filter((i) => selection[i.itemId]?.kind === 'photo')
    const docItems = items.value.filter((i) => selection[i.itemId]?.kind === 'doc')

    // 1) 사진만 압축 (긴 변 1920px, JPEG 85% — 전송량 대폭 절감)
    const prepared = {}
    let done = 0
    for (const i of photoItems) {
      uploadStatus.value = `사진 최적화 중 (${++done}/${photoItems.length})`
      prepared[i.itemId] = await compressImage(selection[i.itemId].file)
    }

    // 2) 사진 병렬 업로드 + 전체 진행률
    const totals = {}
    const loadeds = {}
    const refresh = () => {
      const t = Object.values(totals).reduce((a, b) => a + b, 0)
      const l = Object.values(loadeds).reduce((a, b) => a + b, 0)
      uploadStatus.value = t > 0 ? `전송 중 ${Math.min(99, Math.round((l / t) * 100))}%` : '전송 중…'
    }
    refresh()
    await Promise.all(
      photoItems.map((i) =>
        customerApi.uploadPhoto(props.token, i.itemId, prepared[i.itemId], (e) => {
          totals[i.itemId] = e.total || 0
          loadeds[i.itemId] = e.loaded || 0
          refresh()
        })
      )
    )

    // 3) 서류 확정 (이미 서버에 올라가 있어 빠르다)
    if (docItems.length) {
      uploadStatus.value = '서류 확정 중…'
      await Promise.all(
        docItems.map((i) => customerApi.attachDocument(props.token, i.itemId, selection[i.itemId].staged.stagedId))
      )
    }

    // 4) 제출 확정
    uploadStatus.value = '제출 처리 중…'
    status.value = await customerApi.submit(props.token)
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
        <span class="brand-mark" @click="onBrandTap"><Link2 :size="18" :stroke-width="2.4" /></span>
        <span class="brand-name">자료 제출 센터</span>
      </div>
    </header>

    <main class="wrap">
      <!-- 링크 여는 중 -->
      <section v-if="loading" class="state">
        <span class="spinner dark"></span>
      </section>

      <!-- 링크를 쓸 수 없음 -->
      <section v-else-if="linkError" class="state">
        <span class="state-icon">
          <TimerOff v-if="linkError === 'expired'" :size="36" />
          <Inbox v-else-if="linkError === 'empty'" :size="36" />
          <CircleAlert v-else :size="36" />
        </span>
        <h1 class="state-title">
          {{ {
            expired: '기간이 지난 링크예요.',
            empty: '아직 요청된 자료가 없어요.',
            missing: '링크를 확인해주세요.',
            failed: '링크를 열지 못했어요.'
          }[linkError] }}
        </h1>
        <p class="state-sub">
          {{ {
            expired: '담당자에게 새 링크를 요청해주세요.',
            empty: '요청이 등록되면 같은 링크로 다시 들어와 주세요.',
            missing: '담당자에게 받은 링크로 다시 접속해주세요.',
            failed: linkErrorMessage
          }[linkError] }}
        </p>
        <button v-if="linkError === 'failed'" class="btn btn-lg btn-block state-btn" @click="openLink(token)">
          다시 시도
        </button>
      </section>

      <template v-else-if="status">
        <!-- 검수 중 / 완료 -->
        <template v-if="!showUploadUI">
          <section class="state">
            <span class="state-icon">
              <Icon :name="status.state === 'COMPLETED' ? 'check' : 'clock'" :size="36" />
            </span>
            <span class="company-chip">{{ status.companyName }}</span>
            <h1 class="state-title">
              {{ status.state === 'COMPLETED' ? '모두 통과되었어요.' : '제출이 완료되었어요.' }}
            </h1>
            <p class="state-sub">
              {{ status.state === 'COMPLETED' ? '제출해 주셔서 감사합니다.' : '검수가 끝나면 안내해 드릴게요.' }}
            </p>
            <ol class="timeline">
              <li class="on"><Icon name="check" :size="18" /> 제출 완료</li>
              <li :class="status.state === 'COMPLETED' ? 'on' : 'now'"><Icon name="clock" :size="18" /> 검수</li>
              <li :class="{ now: status.state === 'COMPLETED' }"><Icon name="check" :size="18" /> 통과</li>
            </ol>
          </section>
        </template>

        <!-- 업로드 (최초 제출 / 반환) -->
        <template v-else>
          <div class="progress-head">
            <span class="company-chip">{{ status.companyName }}</span>
            <h1 class="ph-title">
              {{ status.state === 'RETURNED'
                ? `${items.length}${unit.counter}만 다시 ${unit.verb} 주세요.`
                : `${unit.noun} ${items.length}${unit.counter}${unit.obj} ${unit.verb} 주세요.` }}
            </h1>
            <div class="ph-row">
              <div class="ph-bar"><div class="ph-fill" :style="{ width: progressPct + '%' }"></div></div>
              <span class="ph-count">{{ items.length }}{{ unit.counter }} 중 <b>{{ selectedCount }}{{ unit.counter }}</b></span>
            </div>
          </div>


          <!-- 한번에 올리기 (AI 자동 분류) -->
          <button v-if="!allSelected" class="bulk-btn" :disabled="classifying" @click="pickBulk">
            <span class="bulk-title"><Icon name="upload" :size="24" /> 한번에 올리기</span>
            <span class="bulk-sub">사진과 서류를 함께 골라도 AI가 항목에 맞게 자동 분류해요</span>
          </button>
          <input ref="bulkInput" type="file" :accept="ANY_ACCEPT" multiple hidden @change="onBulkFiles" />

          <div v-if="autoMsg" class="alert alert-info auto-banner">
            <span>{{ autoMsg }}</span>
            <button class="undo-link" @click="undoAuto">모두 지우기</button>
          </div>

          <!-- 어느 항목인지 판단하지 못한 서류 -->
          <section v-if="unassignedDocs.length" class="card pending-docs">
            <p class="pd-title">이 서류는 어느 항목인지 확인이 필요해요</p>
            <div v-for="doc in unassignedDocs" :key="doc.stagedId" class="pd-row">
              <DocPreview :doc="doc" />
              <div class="pd-actions">
                <button class="btn btn-sm" @click="docPicker = doc">항목 고르기</button>
                <button class="btn btn-ghost btn-sm" @click="removeDoc(doc)">빼기</button>
              </div>
            </div>
          </section>

          <div class="items">
            <PhotoUploadCard
              v-for="(item, idx) in items"
              :key="item.itemId"
              :item="item"
              :index="idx + 1"
              :selection="selection[item.itemId] || null"
              :auto="!!autoAssigned[item.itemId]"
              :swappable="items.length > 1"
              @select="onSelect"
              @select-doc="onSelectDoc"
              @swap="openSwap"
              @zoom="lightbox = $event"
            />
          </div>

          <div v-if="submitHint" class="alert alert-error" style="margin: 14px 0 0">{{ submitHint }}</div>
          <div v-if="error" class="alert alert-error" style="margin: 14px 0 0">{{ error }}</div>

          <div class="submit-bar">
            <p v-if="!allSelected && !submitting" class="bar-hint">
              {{ unit.noun }} {{ items.length - selectedCount }}{{ unit.counter }}이 더 필요해요
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
                  <Icon v-else :name="row.isDoc ? 'fileText' : 'image'" :size="24" />
                </span>
                <span class="sheet-info">
                  <b>{{ row.name }}</b>
                  <small>{{ row.hasFile ? '이미 있음 → 서로 교환' : '비어 있음 → 이동' }}</small>
                </span>
                <Icon :name="row.hasFile ? 'swap' : 'arrowRight'" :size="22" class="sheet-arrow" />
              </button>
            </div>
            <button class="btn btn-ghost btn-block sheet-cancel" @click="closeSwap">취소</button>
          </div>
        </div>

        <!-- 미배정 서류를 항목에 배치 -->
        <div v-if="docPicker" class="sheet-mask" @click.self="docPicker = null">
          <div class="sheet">
            <p class="sheet-title">'{{ docPicker.originalFileName }}'을(를) 어느 항목에 넣을까요?</p>
            <div class="sheet-list">
              <button
                v-for="row in docTargets"
                :key="row.itemId"
                class="sheet-row"
                @click="placeDoc(docPicker, row.itemId)"
              >
                <span class="sheet-thumb"><Icon name="fileText" :size="24" /></span>
                <span class="sheet-info">
                  <b>{{ row.name }}</b>
                  <small>{{ row.hasFile ? '이미 있음 → 교체' : '비어 있음' }}</small>
                </span>
                <Icon name="arrowRight" :size="22" class="sheet-arrow" />
              </button>
            </div>
            <button class="btn btn-ghost btn-block sheet-cancel" @click="docPicker = null">취소</button>
          </div>
        </div>

        <!-- 자동 분류 진행 오버레이 -->
        <div v-if="classifying || docUploading" class="lightbox classify-overlay">
          <span class="spinner big"></span>
          <p class="lightbox-hint">{{ classifyStatus || '서류 올리는 중…' }}</p>
          <p class="lightbox-hint dim">사진은 기기 안에서 분류되고, 서류는 내용을 읽기 위해 먼저 올라가요</p>
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

    <footer class="foot">
      <span class="foot-mark">자료 제출 센터</span>
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
/* ---- design.md 토큰: 모노크롬 · 스타디움 필 · 그림자 없는 틴트 계단 ----
   관리자 테마 설정과 무관하게 고객 화면은 이 토큰을 쓴다.
   공통 컴포넌트(.btn/.card/.input/.alert, PhotoUploadCard)도 변수를 상속받아 같이 바뀐다. */
.page {
  --ink: #141414;
  --ink-soft: #262626;
  --canvas: #ffffff;
  --canvas-soft: #f3f3f3;
  --field: #f0f0f0;
  --hairline-soft: #f0f0f0;
  --hairline: #e0e0e0;
  --faint: #adadad;

  --primary: var(--ink);
  --primary-dark: var(--ink-soft);
  --primary-soft: var(--canvas-soft);
  --success: var(--ink);
  --danger: var(--ink);
  --danger-bg: var(--canvas-soft);
  --text: var(--ink);
  --text-muted: #707070;
  --border: var(--hairline-soft);
  --border-strong: var(--hairline);
  --bg: var(--canvas);
  --card: var(--canvas);
  --radius: 24px;
  --radius-sm: 16px;
  --btn-radius: 9999px;
  --shadow-sm: none;
  --shadow-md: none;
  --ring: 0 0 0 2px var(--ink);
  --font: 'Pretendard Variable', Pretendard, -apple-system, 'Helvetica Neue', Arial, sans-serif;

  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--canvas);
  color: var(--ink);
  font-family: var(--font);
  font-weight: 456;
  word-break: keep-all;
  overflow-wrap: break-word;
}
.page :deep(*) {
  letter-spacing: 0;
}
.page :deep(.btn) {
  font-weight: 600;
  box-shadow: none;
}
.page :deep(.btn-ghost) {
  background: var(--canvas);
  border-color: var(--hairline);
}
.page :deep(.btn-ghost:hover) {
  background: var(--canvas-soft);
}
.page :deep(.alert) {
  border: 0;
  border-radius: 16px;
  background: var(--canvas-soft);
  color: var(--ink);
  font-weight: 600;
}

/* 앱바 */
.appbar {
  padding: 12px 16px 0;
}
.appbar-inner {
  max-width: 560px;
  margin: 0 auto;
  padding: 8px 16px 8px 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--canvas-soft);
  border-radius: 9999px;
}
.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 30%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: var(--ink);
  flex: 0 0 auto;
}
.brand-name {
  font-size: 17px;
  font-weight: 652;
}

.wrap {
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
  padding: 28px 16px 48px;
  font-size: 22px; /* 고객 화면은 큰 글씨 유지 */
  flex: 1;
}
.wrap .input {
  font-size: 22px;
  padding: 14px 16px;
}
.wrap .btn-lg {
  font-size: 22px;
  min-height: 60px;
  padding: 0 24px;
}
.wrap .btn-sm {
  font-size: 18px;
}

.company-chip {
  display: inline-block;
  font-size: 16px;
  font-weight: 600;
  padding: 7px 14px;
  border-radius: 9999px;
  background: var(--canvas-soft);
}

/* 상태 화면 (여는 중 / 만료 / 검수 중 / 완료) */
.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 56px 4px 8px;
}
.state-icon {
  width: 88px;
  height: 88px;
  border-radius: 30%;
  background: var(--canvas-soft);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}
.state .company-chip {
  margin-bottom: 4px;
}
.state-title {
  font-size: 32px;
  font-weight: 652;
  line-height: 1.2;
  margin: 12px 0 10px;
}
.state-sub {
  font-size: 20px;
  font-weight: 300;
  line-height: 1.5;
  margin: 0;
}
.state-btn {
  margin-top: 32px;
}
.spinner.dark {
  width: 32px;
  height: 32px;
  border-color: var(--canvas-soft);
  border-top-color: var(--ink);
  margin-top: 40px;
}
.timeline {
  list-style: none;
  padding: 0;
  margin: 36px 0 0;
  width: 100%;
  display: grid;
  gap: 8px;
  text-align: left;
}
.timeline li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
  border-radius: 16px;
  font-size: 19px;
  color: var(--faint);
  border: 1px solid var(--hairline-soft);
}
.timeline li.on {
  color: var(--ink);
}
.timeline li.now {
  color: var(--ink);
  background: var(--canvas-soft);
  border-color: transparent;
  font-weight: 652;
}

/* 진행 헤더 */
.progress-head {
  padding: 4px 0 8px;
}
.ph-title {
  font-size: 32px;
  font-weight: 652;
  line-height: 1.2;
  margin: 14px 0 18px;
}
.ph-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ph-bar {
  flex: 1;
  height: 8px;
  border-radius: 9999px;
  background: var(--canvas-soft);
  overflow: hidden;
}
.ph-fill {
  height: 100%;
  border-radius: 9999px;
  background: var(--ink);
  transition: width 0.3s ease;
}
.ph-count {
  font-size: 17px;
  color: var(--text-muted);
  white-space: nowrap;
}
.ph-count b {
  color: var(--ink);
  font-weight: 652;
}
.items {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

/* 한번에 올리기 */
.bulk-btn {
  width: 100%;
  margin-top: 16px;
  padding: 22px 20px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  text-align: left;
  background: var(--canvas-soft);
  border: 0;
  border-radius: 24px;
  cursor: pointer;
  font-family: inherit;
  color: var(--ink);
}
.bulk-btn:disabled {
  opacity: 0.5;
}
.bulk-title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 652;
}
.bulk-sub {
  font-size: 17px;
  line-height: 1.45;
  color: var(--text-muted);
}
.classify-overlay .spinner.big {
  width: 44px;
  height: 44px;
  border-width: 5px;
}

/* 미배정 서류 */
.pending-docs {
  margin-top: 12px;
  padding: 18px;
  border: 0;
  background: var(--canvas-soft);
  border-radius: 24px;
}
.pd-title {
  margin: 0 0 12px;
  font-size: 20px;
  font-weight: 652;
}
.pd-row + .pd-row {
  margin-top: 8px;
}
.pd-actions {
  display: flex;
  gap: 8px;
  margin: -6px 0 4px;
}
.pd-actions .btn {
  flex: 1;
  font-size: 18px;
}

/* 자동 분류 배너 + 실행취소 */
.auto-banner {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 18px;
  line-height: 1.45;
  padding: 16px 18px;
}
.undo-link {
  border: none;
  background: none;
  padding: 4px;
  font-size: 17px;
  font-weight: 600;
  color: var(--text-muted);
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
  background: rgba(20, 20, 20, 0.55);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.sheet {
  width: 100%;
  max-width: 560px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  background: var(--canvas);
  border-radius: 24px 24px 0 0;
  padding: 24px 16px calc(16px + env(safe-area-inset-bottom));
}
.sheet-title {
  font-size: 22px;
  font-weight: 652;
  margin: 0 0 16px;
  flex: 0 0 auto;
}
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
  margin-bottom: 8px;
  background: var(--canvas);
  border: 1px solid var(--hairline-soft);
  border-radius: 16px;
  cursor: pointer;
  font-family: inherit;
  color: var(--ink);
  text-align: left;
}
.sheet-row:active {
  background: var(--canvas-soft);
}
.sheet-thumb {
  width: 56px;
  height: 56px;
  flex: 0 0 auto;
  border-radius: 30%;
  background: var(--field);
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
  font-size: 20px;
  font-weight: 652;
}
.sheet-info small {
  font-size: 16px;
  color: var(--text-muted);
}
.sheet-arrow {
  color: var(--ink);
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

/* 하단 고정 전송 바 */
.submit-bar {
  position: sticky;
  bottom: 0;
  margin: 20px -16px 0;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
}
.bar-hint {
  text-align: center;
  margin: 0 0 8px;
  font-size: 17px;
  color: var(--text-muted);
}

/* 이미지 확대 */
.lightbox {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(20, 20, 20, 0.9);
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
  border-radius: 16px;
}
.lightbox-hint {
  color: rgba(255, 255, 255, 0.8);
  font-size: 19px;
  margin: 0;
}

/* 푸터 — 잉크 밴드, 위쪽만 둥글게 */
.foot {
  background: var(--ink);
  color: #fff;
  border-radius: 24px 24px 0 0;
  padding: 36px 24px calc(40px + env(safe-area-inset-bottom));
}
.foot-mark {
  display: block;
  max-width: 528px;
  margin: 0 auto;
  font-size: 28px;
  font-weight: 652;
}

/* 내부 확인용 AI 분류 상세 */
.ai-debug {
  width: 100%;
  max-width: 480px;
  background: var(--canvas);
  border-radius: 24px;
  padding: 20px;
  max-height: 80vh;
  overflow-y: auto;
}
.ai-debug-title {
  font-size: 18px;
  font-weight: 652;
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
  border-bottom: 1px solid var(--canvas-soft);
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
