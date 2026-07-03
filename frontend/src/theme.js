// 테마 프리셋 + 런타임 CSS 변수 적용

export const THEMES = {
  blue: { name: '트러스트 블루', primary: '#2563eb', dark: '#1d4ed8', soft: '#eff4ff' },
  green: { name: '베리파이 그린', primary: '#0f9d6e', dark: '#0b7a57', soft: '#eaf7f1' },
  warm: { name: '웜 뉴트럴', primary: '#d9772f', dark: '#b4531f', soft: '#fbf1e7' },
  indigo: { name: '프리미엄 인디고', primary: '#4f46e5', dark: '#4338ca', soft: '#eef0fe' }
}

const clamp = (n) => Math.max(0, Math.min(255, n))
const parse = (hex) => {
  const h = hex.replace('#', '')
  return [0, 2, 4].map((i) => parseInt(h.slice(i, i + 2), 16))
}
const toHex = (rgb) => '#' + rgb.map((v) => clamp(Math.round(v)).toString(16).padStart(2, '0')).join('')
const mix = (hex, target, amt) => {
  const a = parse(hex)
  return toHex(a.map((v, i) => v + (target[i] - v) * amt))
}
export const darken = (hex, amt = 0.16) => mix(hex, [0, 0, 0], amt)
export const tint = (hex, amt = 0.9) => mix(hex, [255, 255, 255], amt)
export const rgba = (hex, a) => {
  const [r, g, b] = parse(hex)
  return `rgba(${r}, ${g}, ${b}, ${a})`
}

/** 커스텀 색이 있으면 우선, 없으면 프리셋 사용 → 실제 적용될 색 3종 계산 */
export function resolveColors({ themeId, primaryColor } = {}) {
  if (primaryColor) {
    return { primary: primaryColor, dark: darken(primaryColor, 0.16), soft: tint(primaryColor, 0.9) }
  }
  const t = THEMES[themeId] || THEMES.blue
  return { primary: t.primary, dark: t.dark, soft: t.soft }
}

export function applyTheme(cfg = {}) {
  const { primary, dark, soft } = resolveColors(cfg)
  const s = document.documentElement.style
  s.setProperty('--primary', primary)
  s.setProperty('--primary-dark', dark)
  s.setProperty('--primary-soft', soft)
  s.setProperty('--ring', `0 0 0 3px ${rgba(primary, 0.18)}`)
}

const LS_KEY = 'photo-theme'
export function saveThemeLocal(cfg) {
  try {
    localStorage.setItem(LS_KEY, JSON.stringify(cfg))
  } catch (e) {
    /* ignore */
  }
}
export function loadThemeLocal() {
  try {
    return JSON.parse(localStorage.getItem(LS_KEY) || 'null')
  } catch (e) {
    return null
  }
}
