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
 * 확신이 낮아도 전부 배정한다(고객에게 별도 표시 없음).
 * 확신 지표(score/margin)는 내부 확인용으로만 함께 반환 — 모델 정확도가
 * 충분히 오르면 반환하지 않는 방향으로 정리 예정.
 *
 * @param files File[]
 * @param slots [{ itemId, label }]  label = 영문 분류 힌트(없으면 항목명)
 * @param onProgress (done, total) => void
 * @returns [{ itemId, file, score, margin, secondItemId }]
 *          margin = 배정 라벨 점수 - 그 파일의 차순위 라벨 점수 (낮을수록 헷갈린 것)
 */
export async function classifyToSlots(files, slots, onProgress) {
  const classifier = await loadClassifier()
  const labels = slots.map((s) => s.label)

  // 파일별 라벨 점수 (내림차순)
  const perFile = [] // fileIdx -> [{slotIdx, score}]
  for (let f = 0; f < files.length; f++) {
    const url = URL.createObjectURL(files[f])
    try {
      const out = await classifier(url, labels) // [{score, label}]
      const rows = out
        .map(({ score, label }) => ({ slotIdx: labels.indexOf(label), score }))
        .filter((r) => r.slotIdx !== -1)
        .sort((a, b) => b.score - a.score)
      perFile.push(rows)
    } finally {
      URL.revokeObjectURL(url)
      onProgress?.(f + 1, files.length)
    }
  }

  // 그리디 1:1 배정 - 점수 높은 쌍부터 확정
  const flat = []
  perFile.forEach((rows, fileIdx) => rows.forEach((r) => flat.push({ fileIdx, ...r })))
  flat.sort((a, b) => b.score - a.score)

  const usedFile = new Set()
  const usedSlot = new Set()
  const result = []
  for (const { fileIdx, slotIdx, score } of flat) {
    if (usedFile.has(fileIdx) || usedSlot.has(slotIdx)) continue
    usedFile.add(fileIdx)
    usedSlot.add(slotIdx)
    const others = perFile[fileIdx].filter((r) => r.slotIdx !== slotIdx)
    result.push({
      itemId: slots[slotIdx].itemId,
      file: files[fileIdx],
      score,
      margin: score - (others[0]?.score ?? 0),
      secondItemId: others[0] ? slots[others[0].slotIdx].itemId : null
    })
  }
  return result
}
