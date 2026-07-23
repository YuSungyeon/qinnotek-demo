import { ref, onMounted, onBeforeUnmount } from 'vue'
import { adminApi } from '../api/admin'

// 폴링 간격(ms). 하루 제출 소수 규모 → 60초로 충분. 필요 시 이 값만 조정.
export const POLL_INTERVAL_MS = 60_000

/**
 * 검수 대기 건수 폴링.
 * - 탭이 활성일 때만 폴링 (백그라운드에서는 중지)
 * - 탭 포커스/가시성 복귀 시 즉시 1회 조회
 * - reviewCompanies: 검수 대기 기업 수(배지 숫자), bumped: 직전보다 늘었는지(강조용)
 */
export function useReviewNotifications() {
  const reviewCompanies = ref(0)
  const pendingItems = ref(0)
  const bumped = ref(false)
  let timer = null
  let lastCount = null
  let bumpTimer = null

  async function poll() {
    try {
      const n = await adminApi.getNotifications()
      reviewCompanies.value = n.reviewCompanies
      pendingItems.value = n.pendingItems
      if (lastCount !== null && n.reviewCompanies > lastCount) {
        bumped.value = true
        clearTimeout(bumpTimer)
        bumpTimer = setTimeout(() => (bumped.value = false), 4000)
      }
      lastCount = n.reviewCompanies
    } catch {
      /* 네트워크 오류는 조용히 무시 (다음 주기에 재시도) */
    }
  }

  function start() {
    stop()
    poll() // 즉시 1회
    timer = setInterval(() => {
      if (!document.hidden) poll()
    }, POLL_INTERVAL_MS)
  }
  function stop() {
    if (timer) clearInterval(timer)
    timer = null
  }

  // 탭이 다시 보이면 즉시 최신화
  function onVisible() {
    if (!document.hidden) poll()
  }

  onMounted(() => {
    start()
    document.addEventListener('visibilitychange', onVisible)
    window.addEventListener('focus', onVisible)
  })
  onBeforeUnmount(() => {
    stop()
    clearTimeout(bumpTimer)
    document.removeEventListener('visibilitychange', onVisible)
    window.removeEventListener('focus', onVisible)
  })

  return { reviewCompanies, pendingItems, bumped, refresh: poll }
}
