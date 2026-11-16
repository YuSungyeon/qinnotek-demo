// 처리 이력 표시용: 서버 이벤트(최신 먼저) → 화면 줄.
// 한 번에 함께 낸 제출은 한 줄로 묶고, 날짜별로 나눈다.
import { phone } from './format'

const pad = (n) => String(n).padStart(2, '0')
const DAYS = ['일', '월', '화', '수', '목', '금', '토']

export const HISTORY_FILTERS = [
  { key: 'all', label: '전체' },
  { key: 'review', label: '검수' },
  { key: 'notice', label: '알림' },
  { key: 'link', label: '링크' }
]
const CATEGORY = {
  SUBMITTED: 'review',
  APPROVED: 'review',
  RETURNED: 'review',
  NOTICE_SENT: 'notice',
  NOTICE_FAILED: 'notice',
  LINK_SMS_SENT: 'notice',
  LINK_ISSUED: 'link',
  LINK_REVOKED: 'link'
}

/** 이벤트 하나 → { chip, chipClass, title, sub, thumb, gray, fileUrl } */
export function describe(e) {
  switch (e.type) {
    case 'SUBMITTED':
      return {
        chip: e.round > 1 ? '재제출' : '제출',
        chipClass: 'ev-soft',
        title: e.itemName,
        sub: e.round > 1 ? `고객이 다시 올림 · ${e.round}차` : '고객 제출',
        ...file(e, false)
      }
    case 'APPROVED':
      return { chip: '통과', chipClass: 'chip-done', title: e.itemName }
    case 'RETURNED':
      return { chip: '반환', chipClass: 'chip-returned', title: e.itemName, sub: `사유: ${e.reason}`, ...file(e, true) }
    case 'NOTICE_SENT':
      return { chip: '알림', chipClass: 'ev-line', title: '반환 안내 문자', sub: `→ ${phone(e.recipient)} · ${e.detail}` }
    case 'NOTICE_FAILED':
      return { chip: '알림 실패', chipClass: 'chip-returned', title: '반환 안내를 보내지 못함', sub: e.detail }
    case 'LINK_SMS_SENT':
      return { chip: '알림', chipClass: 'ev-line', title: '제출 링크 문자', sub: `→ ${phone(e.recipient)}` }
    case 'LINK_ISSUED':
      return { chip: '링크', chipClass: 'ev-line', title: '제출 링크 발급', sub: e.detail }
    case 'LINK_REVOKED':
      return { chip: '링크', chipClass: 'ev-line', title: '제출 링크 끔' }
    default:
      return { chip: e.type, chipClass: 'ev-line', title: e.itemName || '' }
  }
}

// 사진은 썸네일(반환본은 회색), 문서는 열어 볼 링크
function file(e, gray) {
  if (!e.fileUrl) return {}
  return e.contentKind === 'PHOTO' ? { thumb: e.fileUrl, gray } : { fileUrl: e.pdfUrl || e.fileUrl }
}

/** 이력 → [{ label: '오늘 · 11월 16일 (월)', rows: [...] }] */
export function groupHistory(events, filter = 'all') {
  const rows = []
  for (const e of events) {
    if (filter !== 'all' && CATEGORY[e.type] !== filter) continue
    const prev = rows[rows.length - 1]
    // 한 번에 낸 제출은 서버가 같은 시각으로 남긴다
    if (e.type === 'SUBMITTED' && prev?.batch && prev.createdAt === e.createdAt) {
      prev.batch.push(e)
      continue
    }
    rows.push({ ...describe(e), id: e.id, type: e.type, createdAt: e.createdAt, batch: e.type === 'SUBMITTED' ? [e] : null })
  }
  for (const r of rows) {
    if (r.batch?.length > 1) {
      const again = r.batch.every((b) => b.round > 1)
      Object.assign(r, {
        chip: again ? '재제출' : '제출',
        title: `고객이 ${r.batch.length}건 ${again ? '다시 ' : ''}제출`,
        sub: r.batch.map((b) => b.itemName).reverse().join(' · '),
        thumb: null,
        fileUrl: null
      })
    }
  }
  const groups = []
  for (const r of rows) {
    const label = dayLabel(r.createdAt)
    if (groups[groups.length - 1]?.label !== label) groups.push({ label, rows: [] })
    groups[groups.length - 1].rows.push(r)
  }
  return groups
}

/** 09:15 */
export function timeOf(value) {
  const d = new Date(value)
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 11/16 09:15 */
export function shortDateTime(value) {
  const d = new Date(value)
  return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${timeOf(value)}`
}

function dayLabel(value) {
  const d = new Date(value)
  const today = new Date()
  const diff = Math.round(
    (new Date(today.getFullYear(), today.getMonth(), today.getDate()) -
      new Date(d.getFullYear(), d.getMonth(), d.getDate())) /
      86400000
  )
  const base = `${d.getMonth() + 1}월 ${d.getDate()}일 (${DAYS[d.getDay()]})`
  if (diff === 0) return `오늘 · ${base}`
  if (diff === 1) return `어제 · ${base}`
  return d.getFullYear() === today.getFullYear() ? base : `${d.getFullYear()}년 ${base}`
}
