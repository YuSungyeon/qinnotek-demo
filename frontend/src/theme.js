// 색상 프리셋(기본 디자인) + 기업 디자인 팩(전체 토큰 교체)

export const THEMES = {
  blue: { name: '트러스트 블루', primary: '#2563eb', dark: '#1d4ed8', soft: '#eff4ff' },
  green: { name: '베리파이 그린', primary: '#0f9d6e', dark: '#0b7a57', soft: '#eaf7f1' },
  warm: { name: '웜 뉴트럴', primary: '#d9772f', dark: '#b4531f', soft: '#fbf1e7' },
  indigo: { name: '프리미엄 인디고', primary: '#4f46e5', dark: '#4338ca', soft: '#eef0fe' }
}

const KR = "'Apple SD Gothic Neo', 'Malgun Gothic'"

// design/*.md 에서 추출한 기업 디자인 토큰
export const DESIGNS = {
  apple: {
    name: 'Apple',
    primary: '#0066cc', dark: '#004a99', soft: '#e6f0fb',
    text: '#1d1d1f', muted: '#6e6e73',
    border: '#e0e0e0', borderStrong: '#d2d2d7',
    bg: '#f5f5f7', card: '#ffffff',
    radius: '18px', radiusSm: '12px', btnRadius: '9999px',
    shadowSm: 'none', shadowMd: '0 8px 30px rgba(0,0,0,0.12)',
    font: `-apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', system-ui, ${KR}, sans-serif`,
    side: { bg: '#1d1d1f', text: '#f5f5f7', activeBg: '#0066cc', activeText: '#fff', hover: 'rgba(255,255,255,0.08)', border: 'rgba(255,255,255,0.06)', brandBg: '#0066cc' }
  },
  figma: {
    name: 'Figma',
    primary: '#000000', dark: '#000000', soft: '#f2f2f0',
    text: '#000000', muted: '#5a5a5a',
    border: '#e6e6e6', borderStrong: '#d8d8d8',
    bg: '#ffffff', card: '#ffffff',
    radius: '24px', radiusSm: '8px', btnRadius: '50px',
    shadowSm: 'none', shadowMd: '0 4px 16px rgba(0,0,0,0.08)',
    font: `Inter, ${KR}, system-ui, sans-serif`,
    side: { bg: '#000000', text: '#cfcfcf', activeBg: '#ffffff', activeText: '#000000', hover: 'rgba(255,255,255,0.10)', border: 'rgba(255,255,255,0.08)', brandBg: '#ff3d8b' }
  },
  airtable: {
    name: 'Airtable',
    primary: '#181d26', dark: '#0d1218', soft: '#f1f3f5',
    text: '#181d26', muted: '#5b616b',
    border: '#dddddd', borderStrong: '#9297a0',
    bg: '#f8fafc', card: '#ffffff',
    radius: '12px', radiusSm: '8px', btnRadius: '12px',
    shadowSm: 'none', shadowMd: '0 4px 16px rgba(24,29,38,0.08)',
    font: `-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Inter, ${KR}, sans-serif`,
    side: { bg: '#181d26', text: '#c4c8d0', activeBg: '#1b61c9', activeText: '#fff', hover: 'rgba(255,255,255,0.07)', border: 'rgba(255,255,255,0.06)', brandBg: '#1b61c9' }
  }
}

// 설정 화면용 메타(스와치)
export const DESIGN_META = [
  { id: 'base', name: '기본', colors: ['#2563eb', '#0f172a', '#eff4ff'] },
  { id: 'apple', name: 'Apple', colors: ['#0066cc', '#1d1d1f', '#f5f5f7'] },
  { id: 'figma', name: 'Figma', colors: ['#000000', '#ff3d8b', '#dceeb1'] },
  { id: 'airtable', name: 'Airtable', colors: ['#181d26', '#1b61c9', '#f5e9d4'] }
]

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

export function resolveColors({ themeId, primaryColor } = {}) {
  if (primaryColor) {
    return { primary: primaryColor, dark: darken(primaryColor, 0.16), soft: tint(primaryColor, 0.9) }
  }
  const t = THEMES[themeId] || THEMES.blue
  return { primary: t.primary, dark: t.dark, soft: t.soft }
}

// 디자인 팩이 관리하는 CSS 변수(전환 시 초기화 대상)
const DESIGN_VARS = [
  '--font', '--text', '--text-muted', '--border', '--border-strong', '--bg', '--card',
  '--radius', '--radius-sm', '--btn-radius', '--shadow-sm', '--shadow-md',
  '--side-bg', '--side-text', '--side-active-bg', '--side-active-text', '--side-hover',
  '--side-border', '--side-brand-bg'
]

export function applyTheme(cfg = {}) {
  const s = document.documentElement.style
  DESIGN_VARS.forEach((v) => s.removeProperty(v)) // 이전 팩 흔적 제거 → style.css 기본값 복귀

  const pack = DESIGNS[cfg.designId]
  if (pack) {
    const vars = {
      '--primary': pack.primary, '--primary-dark': pack.dark, '--primary-soft': pack.soft,
      '--ring': `0 0 0 3px ${rgba(pack.primary, 0.18)}`,
      '--font': pack.font, '--text': pack.text, '--text-muted': pack.muted,
      '--border': pack.border, '--border-strong': pack.borderStrong,
      '--bg': pack.bg, '--card': pack.card,
      '--radius': pack.radius, '--radius-sm': pack.radiusSm, '--btn-radius': pack.btnRadius,
      '--shadow-sm': pack.shadowSm, '--shadow-md': pack.shadowMd,
      '--side-bg': pack.side.bg, '--side-text': pack.side.text,
      '--side-active-bg': pack.side.activeBg, '--side-active-text': pack.side.activeText,
      '--side-hover': pack.side.hover, '--side-border': pack.side.border, '--side-brand-bg': pack.side.brandBg
    }
    Object.entries(vars).forEach(([k, v]) => s.setProperty(k, v))
  } else {
    const { primary, dark, soft } = resolveColors(cfg)
    s.setProperty('--primary', primary)
    s.setProperty('--primary-dark', dark)
    s.setProperty('--primary-soft', soft)
    s.setProperty('--ring', `0 0 0 3px ${rgba(primary, 0.18)}`)
  }
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
