// 업로드 전 클라이언트 측 이미지 리사이즈/압축
// 검수 용도에는 긴 변 1920px + JPEG 85%면 충분 → 전송량 10~20배 절감

const MAX_EDGE = 1920
const QUALITY = 0.85
const SKIP_BYTES = 400 * 1024 // 이미 충분히 작은 파일은 그대로 전송

function toJpgName(name) {
  return name.replace(/\.[^.]+$/, '') + '.jpg'
}

/**
 * 이미지를 축소·압축한 File을 반환. 실패하거나 이득이 없으면 원본 그대로 반환.
 */
export async function compressImage(file) {
  if (!file.type.startsWith('image/')) return file
  try {
    // EXIF 회전을 반영해 디코드 (세로로 찍은 사진이 눕지 않도록)
    const bmp = await createImageBitmap(file, { imageOrientation: 'from-image' })
    const scale = Math.min(1, MAX_EDGE / Math.max(bmp.width, bmp.height))

    // 축소할 것도 없고 용량도 작으면 그대로
    if (scale === 1 && file.size <= SKIP_BYTES) {
      bmp.close()
      return file
    }

    const w = Math.max(1, Math.round(bmp.width * scale))
    const h = Math.max(1, Math.round(bmp.height * scale))
    const canvas = document.createElement('canvas')
    canvas.width = w
    canvas.height = h
    canvas.getContext('2d').drawImage(bmp, 0, 0, w, h)
    bmp.close()

    const blob = await new Promise((res) => canvas.toBlob(res, 'image/jpeg', QUALITY))
    if (!blob || blob.size >= file.size) return file // 압축 이득이 없으면 원본
    return new File([blob], toJpgName(file.name), { type: 'image/jpeg' })
  } catch (e) {
    // 디코드 불가(특수 포맷 등) → 원본 업로드로 폴백
    return file
  }
}
