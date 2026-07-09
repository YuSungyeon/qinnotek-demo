// 온디바이스 사진 자동 분류 (Transformers.js + CLIP zero-shot)
// - 모델은 최초 사용 시 1회 다운로드 후 브라우저 캐시
// - 추론은 전부 사용자 기기에서 실행 (서버 비용 0)

const MODEL_ID = 'Xenova/clip-vit-base-patch32'

let pipePromise = null

/** 분류 파이프라인 lazy 로드. onProgress(percent|null, statusText) */
export function loadClassifier(onProgress) {
  if (!pipePromise) {
    pipePromise = import('@huggingface/transformers').then(({ pipeline }) =>
      pipeline('zero-shot-image-classification', MODEL_ID, {
        dtype: 'q8',
        progress_callback: (p) => {
          if (!onProgress) return
          if (p.status === 'progress' && p.total) {
            onProgress(Math.round((p.loaded / p.total) * 100), '모델 내려받는 중')
          } else if (p.status === 'ready') {
            onProgress(null, '모델 준비 완료')
          }
        }
      })
    )
    // 실패 시 다음 시도에서 재로드 가능하도록
    pipePromise.catch(() => {
      pipePromise = null
    })
  }
  return pipePromise
}

/**
 * 여러 파일을 슬롯(요구 사진)에 자동 배정한다.
 * @param files File[]
 * @param slots [{ itemId, label }]  label = 영문 분류 힌트(없으면 항목명)
 * @param onProgress (done, total) => void
 * @returns [{ itemId, file, score }]
 */
export async function classifyToSlots(files, slots, onProgress) {
  const classifier = await loadClassifier()
  const labels = slots.map((s) => s.label)

  // 각 파일 × 각 라벨 점수 계산
  const scored = [] // { fileIdx, slotIdx, score }
  for (let f = 0; f < files.length; f++) {
    const url = URL.createObjectURL(files[f])
    try {
      const out = await classifier(url, labels) // [{score, label}]
      for (const { score, label } of out) {
        const s = labels.indexOf(label)
        if (s !== -1) scored.push({ fileIdx: f, slotIdx: s, score })
      }
    } finally {
      URL.revokeObjectURL(url)
      onProgress?.(f + 1, files.length)
    }
  }

  // 그리디 1:1 배정 - 점수 높은 쌍부터 확정
  scored.sort((a, b) => b.score - a.score)
  const usedFile = new Set()
  const usedSlot = new Set()
  const result = []
  for (const { fileIdx, slotIdx, score } of scored) {
    if (usedFile.has(fileIdx) || usedSlot.has(slotIdx)) continue
    usedFile.add(fileIdx)
    usedSlot.add(slotIdx)
    result.push({ itemId: slots[slotIdx].itemId, file: files[fileIdx], score })
  }
  return result
}
