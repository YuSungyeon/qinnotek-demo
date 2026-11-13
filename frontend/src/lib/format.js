// 화면 표시용 날짜·번호 형식. 관리자 화면들이 같은 모양으로 보여주도록 여기 모은다.

const pad = (n) => String(n).padStart(2, '0')

/** 2026.11.12 23:19 */
export function dateTime(value) {
  if (!value) return ''
  const d = new Date(value)
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 목록용 짧은 시각: 오늘 09:12 / 어제 17:05 / 11.08 10:31 / 2025.12.30 */
export function shortTime(value) {
  if (!value) return ''
  const d = new Date(value)
  const now = new Date()
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  const dayDiff = Math.round(
    (new Date(now.getFullYear(), now.getMonth(), now.getDate()) -
      new Date(d.getFullYear(), d.getMonth(), d.getDate())) /
      86400000
  )
  if (dayDiff === 0) return `오늘 ${time}`
  if (dayDiff === 1) return `어제 ${time}`
  if (d.getFullYear() === now.getFullYear()) return `${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${time}`
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`
}

/** 010-1234-5678 (숫자만 남기고 자릿수에 맞춰 하이픈) */
export function phone(value) {
  const d = (value || '').replace(/\D/g, '').slice(0, 11)
  if (d.length > 7) return `${d.slice(0, 3)}-${d.slice(3, d.length - 4)}-${d.slice(-4)}`
  if (d.length > 3) return `${d.slice(0, 3)}-${d.slice(3)}`
  return d
}
