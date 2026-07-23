// 온디바이스 사진 자동 분류 (Transformers.js + CLIP)
// - 텍스트 힌트 임베딩 + 예시 이미지 임베딩(프로토타입)을 함께 사용
// - 추론은 전부 사용자 기기에서 실행 (서버 비용 0), 모델은 최초 1회 다운로드 후 캐시
// - 두 모달리티는 코사인 스케일이 달라(텍스트~이미지 낮음, 이미지~이미지 높음)
//   슬롯 축으로 각각 z-정규화한 뒤 가중합해 섞는다.

import { orientedImageUrl } from './resize'

const MODEL_ID = 'Xenova/clip-vit-base-patch32'
const ALPHA_TEXT = 0.5 // 혼합 비중: 0=예시이미지만, 1=텍스트만
// 텍스트 힌트를 감싸는 프롬프트 앙상블(문구 흔들림 완화)
const TEXT_TEMPLATES = ['a photo of {}.', 'a clear photo of {}.']

let modelPromise = null

function reportProgress(onProgress) {
  return (p) => {
    if (!onProgress) return
    if (p.status === 'progress' && p.total) {
      onProgress(Math.round((p.loaded / p.total) * 100), '모델 내려받는 중')
    } else if (p.status === 'ready') {
      onProgress(null, '모델 준비 완료')
    }
  }
}

/** CLIP 텍스트/비전 모델 + 토크나이저/프로세서 lazy 로드 (캐시) */
export function getModel(onProgress) {
  if (!modelPromise) {
    modelPromise = import('@huggingface/transformers').then(async (T) => {
      const opts = { dtype: 'q8', progress_callback: reportProgress(onProgress) }
      const [tokenizer, textModel, processor, visionModel] = await Promise.all([
        T.AutoTokenizer.from_pretrained(MODEL_ID),
        T.CLIPTextModelWithProjection.from_pretrained(MODEL_ID, opts),
        T.AutoProcessor.from_pretrained(MODEL_ID),
        T.CLIPVisionModelWithProjection.from_pretrained(MODEL_ID, opts)
      ])
      return { T, tokenizer, textModel, processor, visionModel }
    })
    modelPromise.catch(() => {
      modelPromise = null
    })
  }
  return modelPromise
}

/** CustomerView 호환용 별칭 (다운로드 진행 표시) */
export function loadClassifier(onProgress) {
  return getModel(onProgress)
}

function l2normalize(vec) {
  let s = 0
  for (const v of vec) s += v * v
  s = Math.sqrt(s) || 1
  return vec.map((v) => v / s)
}
function dot(a, b) {
  let s = 0
  for (let i = 0; i < a.length; i++) s += a[i] * b[i]
  return s
}
/** 슬롯 축 z-정규화(평균 0, 표준편차 1) — 모달리티 간 스케일 차이 제거 */
function zstandardize(arr) {
  const n = arr.length
  if (n === 0) return arr
  const mean = arr.reduce((a, b) => a + b, 0) / n
  const varr = arr.reduce((a, b) => a + (b - mean) ** 2, 0) / n
  const std = Math.sqrt(varr) || 1
  return arr.map((v) => (v - mean) / std)
}

/** 텍스트 배열 → 정규화된 임베딩 배열 */
export async function embedTexts(texts) {
  const { tokenizer, textModel } = await getModel()
  const inputs = tokenizer(texts, { padding: true, truncation: true })
  const { text_embeds } = await textModel(inputs)
  return text_embeds.tolist().map(l2normalize)
}

/** 힌트 하나 → 프롬프트 앙상블 평균 임베딩(정규화) */
async function embedHint(hint) {
  const embs = await embedTexts(TEXT_TEMPLATES.map((t) => t.replace('{}', hint)))
  const dim = embs[0].length
  const avg = new Array(dim).fill(0)
  for (const e of embs) for (let i = 0; i < dim; i++) avg[i] += e[i] / embs.length
  return l2normalize(avg)
}

/** 이미지 URL(또는 파일 경로) → 정규화된 임베딩 */
export async function embedImageUrl(url) {
  const { T, processor, visionModel } = await getModel()
  const image = await T.RawImage.read(url)
  const inputs = await processor(image)
  const { image_embeds } = await visionModel(inputs)
  return l2normalize(image_embeds.tolist()[0])
}

/**
 * 여러 파일을 슬롯(요구 사진)에 자동 배정한다. 확신이 낮아도 전부 배정.
 * 점수 = α·(사진↔힌트텍스트) + (1−α)·(사진↔예시이미지), 모달리티별 z-정규화 후 혼합.
 *
 * @param files File[]
 * @param slots [{ itemId, label, exampleUrl? }]
 * @param onProgress (done, total) => void
 * @returns [{ itemId, file, score, margin, secondItemId }]
 */
export async function classifyToSlots(files, slots, onProgress) {
  await getModel()

  // 슬롯별 텍스트/예시이미지 임베딩 준비 (예시 없으면 null)
  const slotText = []
  const slotImg = []
  for (const s of slots) {
    slotText.push(await embedHint(s.label))
    slotImg.push(s.exampleUrl ? await embedImageUrl(s.exampleUrl).catch(() => null) : null)
  }
  const anyExample = slotImg.some(Boolean)

  // 각 사진 임베딩 → 슬롯별 혼합 점수 행렬
  const scoreMatrix = [] // [fileIdx][slotIdx]
  for (let f = 0; f < files.length; f++) {
    const url = await orientedImageUrl(files[f])
    let photo
    try {
      photo = await embedImageUrl(url)
    } finally {
      URL.revokeObjectURL(url)
    }
    const tRaw = slotText.map((t) => dot(photo, t))
    const tz = zstandardize(tRaw)
    let combined
    if (anyExample) {
      // 예시 없는 슬롯은 이미지 점수를 텍스트 점수로 대체(불이익 방지)
      const iRaw = slotImg.map((im, i) => (im ? dot(photo, im) : tRaw[i]))
      const iz = zstandardize(iRaw)
      combined = tz.map((v, i) => ALPHA_TEXT * v + (1 - ALPHA_TEXT) * iz[i])
    } else {
      combined = tz
    }
    scoreMatrix.push(combined)
    onProgress?.(f + 1, files.length)
  }

  // 그리디 1:1 배정 - 점수 높은 쌍부터 확정
  const flat = []
  scoreMatrix.forEach((row, fileIdx) =>
    row.forEach((score, slotIdx) => flat.push({ fileIdx, slotIdx, score }))
  )
  flat.sort((a, b) => b.score - a.score)

  const usedFile = new Set()
  const usedSlot = new Set()
  const result = []
  for (const { fileIdx, slotIdx, score } of flat) {
    if (usedFile.has(fileIdx) || usedSlot.has(slotIdx)) continue
    usedFile.add(fileIdx)
    usedSlot.add(slotIdx)
    const others = scoreMatrix[fileIdx]
      .map((sc, i) => ({ i, sc }))
      .filter((o) => o.i !== slotIdx)
      .sort((a, b) => b.sc - a.sc)
    result.push({
      itemId: slots[slotIdx].itemId,
      file: files[fileIdx],
      score,
      margin: score - (others[0]?.sc ?? 0),
      secondItemId: others[0] ? slots[others[0].i].itemId : null
    })
  }
  return result
}
