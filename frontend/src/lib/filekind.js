// 파일 종류 판별 - 확장자 기준이 핵심이다.
// .hwp 는 브라우저가 빈 MIME 또는 application/octet-stream 을 주는 경우가 흔해
// file.type 만 믿으면 한글 파일을 놓친다.

export const IMAGE_EXT = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'heic', 'heif', 'bmp']
export const DOC_EXT = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'hwp', 'hwpx']

/** <input accept> 값 */
export const IMAGE_ACCEPT = 'image/*'
export const DOC_ACCEPT = DOC_EXT.map((e) => '.' + e).join(',')
export const ANY_ACCEPT = IMAGE_ACCEPT + ',' + DOC_ACCEPT

export function extOf(file) {
  const name = file?.name || ''
  const dot = name.lastIndexOf('.')
  return dot > 0 ? name.slice(dot + 1).toLowerCase() : ''
}

/** 'photo' | 'doc' | 'unknown' */
export function kindOf(file) {
  if (!file) return 'unknown'
  const ext = extOf(file)
  if (file.type?.startsWith('image/') || IMAGE_EXT.includes(ext)) return 'photo'
  if (DOC_EXT.includes(ext)) return 'doc'
  return 'unknown'
}

/** 항목이 받는 유형(PHOTO/DOCUMENT/ANY)에 맞는 파일인지 */
export function acceptsKind(fileType, kind) {
  if (kind === 'photo') return fileType !== 'DOCUMENT'
  if (kind === 'doc') return fileType === 'DOCUMENT' || fileType === 'ANY'
  return false
}

/** 항목 유형별 accept 속성 */
export function acceptFor(fileType) {
  if (fileType === 'DOCUMENT') return DOC_ACCEPT
  if (fileType === 'ANY') return ANY_ACCEPT
  return IMAGE_ACCEPT
}

export function formatBytes(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return `${bytes}B`
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)}KB`
  return `${(bytes / 1024 / 1024).toFixed(1)}MB`
}
