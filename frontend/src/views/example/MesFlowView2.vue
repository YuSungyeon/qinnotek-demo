<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

// 수주 -> 출하. 화면 중앙의 선 하나가 공정을 따라 모습을 바꾸며 전체를 설명한다.
// 편지 -> 모니터 -> 트럭 -> 상자 -> 창고 -> 달력 -> 창고 -> 벨트 위 상자
//      -> 4개로 분할 -> 로봇 가공 -> 머신비전 검사(불량 2개 배출) -> 포장 -> 수직 트럭 출하
const stages = [
  {
    no: '01',
    key: 'order',
    title: '수주',
    desc: '고객의 주문을 접수하고 품목·수량·납기를 확정합니다.',
    tags: ['주문서', '납기', '수량']
  },
  {
    no: '02',
    key: 'inbound',
    title: '입고',
    desc: '원부자재가 입고되어 수입검사와 등록이 완료됩니다.',
    tags: ['거래명세', '수입검사', 'LOT']
  },
  {
    no: '03',
    key: 'stock',
    title: '재고',
    desc: '입고된 자재는 창고에 적재되어 위치와 수량으로 관리됩니다.',
    tags: ['로케이션', '실시간 수량', '선입선출']
  },
  {
    no: '04',
    key: 'plan',
    title: '생산계획',
    desc: '수주 정보를 기반으로 일정과 작업지시가 잡힙니다.',
    tags: ['작업지시', '설비 배정', '납기 일정']
  },
  {
    no: '05',
    key: 'issue',
    title: '출고',
    desc: '작업지시에 맞춰 자재가 창고에서 출고되어 현장으로 전달됩니다.',
    tags: ['불출 지시', '자재 출고', '현장 전달']
  },
  {
    no: '06',
    key: 'produce',
    title: '생산',
    desc: '설비가 자재를 가공·조립하고 공정 실적이 자동 수집됩니다.',
    tags: ['공정 실적', '설비 가동', '작업자']
  },
  {
    no: '07',
    key: 'inspect',
    title: '검사',
    desc: '머신비전이 품질을 판정하고 불량은 라인에서 걸러집니다.',
    tags: ['머신비전', '불량 배출', '합격 판정']
  },
  {
    no: '08',
    key: 'pack',
    title: '포장',
    desc: '합격한 제품은 박스에 포장되어 출하 준비가 완료됩니다.',
    tags: ['포장 단위', '라벨', '박스 번호']
  },
  {
    no: '09',
    key: 'ship',
    title: '출하',
    desc: '포장된 제품이 차량에 적재되어 고객에게 출발합니다.',
    tags: ['출하지시', '거래명세서', '배송 추적']
  }
]

const SLAT = 40 // 벨트 슬랫 간격(px)
const STAGE_W = 480 // 중앙 연출 좌표계
const STAGE_H = 320
const CX = 240
const CY = 160
const BELT_W = 260 // 데스크톱 벨트 폭 (--belt-w)
const BELT_W_NARROW = 140

const clamp = (v, min, max) => Math.min(max, Math.max(min, v))
const lerp = (a, b, t) => a + (b - a) * t
const mix = (A, B, t) => A.map((p, i) => [lerp(p[0], B[i][0], t), lerp(p[1], B[i][1], t)])
const str = (pts) => pts.map((p) => p[0].toFixed(1) + ',' + p[1].toFixed(1)).join(' ')
const easeIO = (t) => (t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2)

const rgb = (h) => [
  parseInt(h.slice(1, 3), 16),
  parseInt(h.slice(3, 5), 16),
  parseInt(h.slice(5, 7), 16)
]
const mixColor = (a, b, t) =>
  'rgb(' + rgb(a).map((v, i) => Math.round(lerp(v, rgb(b)[i], t))).join(',') + ')'

// 인트로는 딥네이비, 벨트가 드러나면서 밝은 배경으로 넘어간다
const DARK = {
  bg: '#0e1a2f',
  text: '#f1f5f9',
  muted: '#94a3b8',
  line: '#22334d',
  accent: '#60a5fa',
  stroke: '#cbd5e1',
  tag: '#14243d'
}
const LIGHT = {
  bg: '#f0f1f4',
  text: '#111827',
  muted: '#6b7280',
  line: '#e2e5ea',
  accent: '#2563eb',
  stroke: '#5b6472',
  tag: '#ffffff'
}

// ---- 도형 헬퍼 ----
const R = (x1, y1, x2, y2) => [[x1, y1], [x2, y1], [x2, y2], [x1, y2], [x1, y1]]
const L = (x1, y1, x2, y2) => [[x1, y1], [x2, y2]]
const DOT = (x, y, n) => Array.from({ length: n }, () => [x, y])

const BOX_SM = R(202, 202, 278, 262) // 바닥/창고 안의 작은 상자
const NECK_MON = [[225, 205], [225, 232], [240, 232], [255, 232], [255, 205], [225, 205]]
const NECK_TRK = [[300, 225], [300, 150], [345, 150], [375, 185], [375, 225], [300, 225]]
const NECK_OFF = DOT(240, 240, 6)
const NECK_TOP = DOT(240, 84, 6) // 달력 고리가 자라나는 자리

const RACK = {
  postL: L(120, 86, 120, 262),
  postR: L(360, 86, 360, 262),
  shelfA: L(120, 86, 360, 86),
  shelfB: L(120, 146, 360, 146),
  shelfC: L(120, 202, 360, 202),
  st1: R(140, 158, 196, 202),
  st2: R(288, 102, 344, 146),
  ground: L(112, 262, 368, 262)
}
const RACK_OPEN = {
  postL: L(58, 18, 58, 302),
  postR: L(422, 18, 422, 302),
  shelfA: L(46, 26, 434, 26),
  shelfB: L(46, 124, 434, 124),
  shelfC: L(46, 214, 434, 214),
  st1: R(126, 146, 200, 210),
  st2: R(284, 88, 358, 152)
}

// 어두운 화면 구간(수주~출고)의 장면들
const DARK_SCENES = [
  {
    // 0 모니터
    body: R(130, 60, 350, 205),
    neck: NECK_MON,
    ground: L(195, 232, 285, 232),
    m1: L(162, 100, 252, 100),
    m2: L(162, 126, 222, 126),
    ...RACK_OPEN,
    wf: [300, 145, 18],
    wr: [300, 145, 0]
  },
  {
    // 1 트럭
    body: R(110, 110, 300, 225),
    neck: NECK_TRK,
    ground: L(95, 262, 390, 262),
    m1: L(136, 142, 216, 142),
    m2: L(136, 168, 192, 168),
    ...RACK_OPEN,
    wf: [338, 244, 18],
    wr: [176, 244, 18]
  },
  {
    // 2 작은 상자
    body: BOX_SM,
    neck: NECK_OFF,
    ground: L(150, 262, 330, 262),
    m1: L(240, 202, 240, 262),
    m2: L(210, 228, 270, 228),
    ...RACK_OPEN,
    wf: [240, 240, 0],
    wr: [240, 240, 0]
  },
  {
    // 3 창고 (재고)
    body: BOX_SM,
    neck: NECK_TOP,
    m1: L(240, 202, 240, 262),
    m2: L(210, 228, 270, 228),
    ...RACK,
    wf: [240, 240, 0],
    wr: [240, 240, 0]
  },
  {
    // 4 달력 (생산계획) — 위쪽 고리 + 머리글 + 날짜 칸 그리드
    body: BOX_SM,
    neck: [[186, 68], [186, 84], [240, 84], [294, 84], [294, 68], [294, 68]],
    ground: L(150, 246, 330, 246),
    m1: L(166, 102, 226, 102), // 머리글
    m2: L(168, 222, 228, 222), // 아래 메모줄
    postL: L(150, 84, 150, 246),
    postR: L(330, 84, 330, 246),
    shelfA: L(150, 84, 330, 84),
    shelfB: L(150, 118, 330, 118), // 머리글 구분선
    shelfC: L(150, 166, 330, 166), // 주 구분선
    st1: R(168, 132, 208, 160),
    st2: R(168, 172, 208, 200),
    it0: R(220, 132, 260, 160),
    it1: R(272, 132, 312, 160),
    it2: R(220, 172, 260, 200),
    it3: R(272, 172, 312, 200),
    wf: [240, 186, 15], // 작업 예정일 표시
    wr: [240, 186, 0]
  },
  {
    // 5 창고 (출고 직전)
    body: BOX_SM,
    neck: NECK_TOP,
    m1: L(240, 202, 240, 262),
    m2: L(210, 228, 270, 228),
    ...RACK,
    wf: [240, 240, 0],
    wr: [240, 240, 0]
  }
]

// 장면별 요소 불투명도 (fill = 벨트 위 흰 제품 채움)
const OPS = [
  // body: 박스/화면 외곽선, fill: 갈색 박스 채움, ifill: 제품 흰 채움
  { body: 1, fill: 0, ifill: 0, neck: 1, ground: 1, mark: 1, rack: 0, st: 0, wf: 1, wr: 0, items: 0 },
  { body: 1, fill: 0, ifill: 0, neck: 1, ground: 1, mark: 1, rack: 0, st: 0, wf: 1, wr: 1, items: 0 },
  { body: 1, fill: 1, ifill: 0, neck: 0, ground: 1, mark: 1, rack: 0, st: 0, wf: 0, wr: 0, items: 0 },
  { body: 1, fill: 1, ifill: 0, neck: 0, ground: 1, mark: 1, rack: 1, st: 1, wf: 0, wr: 0, items: 0 },
  { body: 0, fill: 0, ifill: 0, neck: 1, ground: 1, mark: 1, rack: 1, st: 1, wf: 1, wr: 0, items: 1 },
  { body: 1, fill: 1, ifill: 0, neck: 0, ground: 1, mark: 1, rack: 1, st: 1, wf: 0, wr: 0, items: 0 },
  { body: 1, fill: 1, ifill: 0, neck: 0, ground: 1, mark: 1, rack: 1, st: 1, wf: 0, wr: 0, items: 0 },
  { body: 1, fill: 1, ifill: 0, neck: 0, ground: 0, mark: 1, rack: 0, st: 0, wf: 0, wr: 0, items: 0 },
  { body: 0, fill: 0, ifill: 1, neck: 0, ground: 0, mark: 0, rack: 0, st: 0, wf: 0, wr: 0, items: 1 },
  { body: 0, fill: 0, ifill: 1, neck: 0, ground: 0, mark: 0, rack: 0, st: 0, wf: 0, wr: 0, items: 1 },
  { body: 1, fill: 1, ifill: 1, neck: 0, ground: 0, mark: 0, rack: 0, st: 1, wf: 0, wr: 0, items: 1 },
  { body: 1, fill: 1, ifill: 0, neck: 0, ground: 0, mark: 1, rack: 0, st: 1, wf: 0, wr: 0, items: 0 }
]

// 각 공정 설명이 화면 정중앙에 오는 스크롤 위치는 그 섹션의 시작점이다
// (수주 1.0 / 입고 2.7 / 재고 4.5 / 생산계획 5.5 / 출고 6.5 / 생산 7.5 / 검사 8.5 / 포장 9.5 / 출하 10.5vh).
// 모든 전환은 그 지점보다 최소 0.2화면 앞에서 끝난다.
const SCENE_KEYS = [
  [1.55, 2.15], // 0->1 모니터가 트럭으로
  [3.2, 3.48], // 1->2 트럭이 작은 상자로
  [3.56, 4.05], // 2->3 선들이 모여 창고가 된다
  [4.78, 5.2], // 3->4 창고가 달력으로
  [5.72, 6.02], // 4->5 달력이 다시 창고로
  [6.42, 6.72], // 5->6 출고 설명이 지나갈 때 박스가 창고 아래로 나온다
  [6.8, 7.05], // 6->7 상자가 벨트 위로 (벨트 전개)
  [7.08, 7.28], // 7->8 하나씩 쪼개져 나란히 선다
  [7.95, 8.2], // 8->9 검사기를 지나 불량 2개가 좌측 레일로 빠진다
  [8.85, 9.06], // 9->10 합격품 위로 갈색 상자가 씌워진다
  [9.1, 9.28] // 10->11 덮개가 닫히고 테이프가 붙는다
]

const rootEl = ref(null)
const active = ref(-1)
const revealed = ref(new Set())

const y = ref(0)
const vh = ref(1)
const vw = ref(1)
const docMax = ref(1)

const current = computed(() => stages[Math.min(Math.max(active.value, 0), stages.length - 1)])
const total = computed(() => String(stages.length).padStart(2, '0'))

const seg = (a, b) => clamp((y.value - a * vh.value) / Math.max(1, (b - a) * vh.value), 0, 1)

// ---- 인트로(편지 -> 모니터) ----
const foldSide = computed(() => easeIO(seg(0.03, 0.34)))
const foldTop = computed(() => easeIO(seg(0.26, 0.54)))
const fly = computed(() => easeIO(seg(0.44, 0.72)))
const draw = computed(() => seg(0.5, 0.75))
const circIn = computed(() => seg(0.68, 0.79))

// ---- 장면 진행도 ----
const sceneP = computed(() => {
  let p = 0
  for (let i = 0; i < SCENE_KEYS.length; i++) p += easeIO(seg(SCENE_KEYS[i][0], SCENE_KEYS[i][1]))
  return p
})

const beltReveal = computed(() => easeIO(seg(6.86, 7.04)))
const bgT = computed(() => easeIO(seg(6.78, 7.0)))
const rackFade = computed(() => 1 - seg(6.78, 7.0))

const heroOp = computed(() => 1 - seg(0.1, 0.3))
// 접히기 직전에 나타났다가 각 방향이 접히면 사라지는 접힘선
const creaseV = computed(() => Math.min(seg(0.005, 0.05), 1 - foldSide.value * 1.6))
const creaseH = computed(() => Math.min(seg(0.005, 0.05), 1 - foldTop.value * 1.6))
const sheetOp = computed(() => 1 - seg(0.66, 0.8))
const checkOp = computed(() => circIn.value * (1 - clamp(sceneP.value * 2.2, 0, 1)))
// 입고가 중앙에 오기 전에 나타나고, 입고를 지나면 천천히 사라진다
const personOp = computed(() => Math.min(seg(2.2, 2.44), 1 - seg(2.95, 3.42)))
// 공정 설비는 해당 공정이 중앙에 닿기 전에 자리를 잡는다
const robotOp = computed(() => Math.min(seg(7.06, 7.26), 1 - seg(7.72, 7.95)))
const visionOp = computed(() => Math.min(seg(7.88, 8.12), 1 - seg(8.72, 8.95)))
const packerOp = computed(() => Math.min(seg(8.88, 9.12), 1 - seg(9.72, 9.95)))
const truckOp = computed(() => seg(9.92, 10.06))
// 트럭은 아래에서 위로 후진해 들어와 박스를 싣는다
const truckEnter = computed(() => easeIO(seg(9.95, 10.28)))
// 트럭과 제품은 출발하면서 함께 사라진다
const cargoOp = computed(() => 1 - seg(11.15, 11.5))
const rejectOp = computed(() => clamp((sceneP.value - 8 - 0.45) / 0.4, 0, 1))
// 좌측으로 빠진 불량은 배출 레일이 사라질 때 함께 사라진다
const rejectOut = computed(() => 1 - seg(8.72, 8.95))
// 적재가 끝나면 트럭은 박스를 싣고 아래로 빠져나간다
const departY = computed(() => BH.value * 3.2 * easeIO(seg(10.9, 11.5)))

const tail = computed(() => clamp((docMax.value - y.value) / (vh.value * 0.6), 0, 1))
const beltOp = computed(() => Math.min(seg(6.82, 6.96), 1 - seg(9.7, 9.9), tail.value))
// 레일은 생산 작업 중에 멈추고, 포장이 끝나면 멈춘 채로 사라진다
const FREEZE = [
  [7.34, 7.72],
  [9.7, 99]
]
const beltTravel = computed(() => {
  const yy = y.value
  let t = yy
  for (const f of FREEZE) {
    const a = f[0] * vh.value
    const b = f[1] * vh.value
    if (yy > a) t -= Math.min(yy, b) - a
  }
  return t
})
const beltY = computed(() => -(beltTravel.value % SLAT))
const progress = computed(() => clamp(y.value / docMax.value, 0, 1))

const isNarrow = computed(() => vw.value < 900)
const foldFx = computed(() => (isNarrow.value ? 0.12 : 0.25))
const foldFy = computed(() => (isNarrow.value ? 0.22 : 0.25))

const cineScale = computed(() => (isNarrow.value ? clamp(vw.value / 640, 0.4, 1) : 1))
const cineX = computed(() => vw.value / 2)
const cineY = computed(() => vh.value * (isNarrow.value ? 0.3 : 0.5))
// 벨트 반폭을 연출 좌표계로 환산 (제품·설비 크기의 기준)
const BH = computed(
  () => (isNarrow.value ? BELT_W_NARROW : BELT_W) / 2 / cineScale.value
)

const themeVars = computed(() => {
  const t = bgT.value
  return {
    '--ex-bg': mixColor(DARK.bg, LIGHT.bg, t),
    '--ex-text': mixColor(DARK.text, LIGHT.text, t),
    '--ex-muted': mixColor(DARK.muted, LIGHT.muted, t),
    '--ex-line': mixColor(DARK.line, LIGHT.line, t),
    '--ex-accent': mixColor(DARK.accent, LIGHT.accent, t),
    '--ex-stroke': mixColor(DARK.stroke, LIGHT.stroke, t),
    '--ex-tag': mixColor(DARK.tag, LIGHT.tag, t)
  }
})

// ---- 벨트 위 구간의 치수 ----
const dims = computed(() => {
  const bh = BH.value
  const iw = bh * 0.36 // 제품 한 변 (정사각형)
  const ih = iw
  const hb = iw // 2x2 블록 박스의 반폭
  const sp = bh * 0.5 // 제품 간격
  // sy: 설비가 고정되는 줄 (설명 제목과 같은 높이)
  // vy: 검사기를 지난 제품이 흘러가는 줄 — 불량 배출 레일과 포장기가 여기에 고정된다
  const ty = -44 / cineScale.value
  const sy = CY + ty
  return {
    bh,
    hb,
    iw,
    ih,
    sp,
    rx1: bh * 1.18,
    rx2: bh * 1.76,
    ty,
    sy,
    vy: sy + bh * 0.92
  }
})

// ---- 전체 장면 목록 ----
const scenes = computed(() => {
  const { bh, iw, ih, sp, sy, vy } = dims.value
  const none = DOT(CX, CY, 5)
  const dark = DARK_SCENES.map((s) => ({ it0: none, it1: none, it2: none, it3: none, ...s }))

  const itemAt = (cx, cy) => R(cx - iw / 2, cy - ih / 2, cx + iw / 2, cy + ih / 2)
  const row = (i) => itemAt(CX + (i - 1.5) * sp, sy)

  const packBox = R(CX - bh * 0.62, vy - bh * 0.5, CX + bh * 0.62, vy + bh * 0.5)
  const exitY = 272 + iw // 창고 아래로 나온 박스의 중심
  // 박스를 따라 내려가므로 창고는 위로 물러난다
  const up = (pts) => pts.map((p) => [p[0], p[1] - (exitY - CY)])
  const rackUp = {}
  for (const k of Object.keys(RACK)) rackUp[k] = up(RACK[k])
  const rest = { ...rackUp, neck: NECK_TOP, wf: [CX, CY, 0], wr: [CX, CY, 0] }

  return [
    ...dark,
    {
      // 6 박스가 창고 아래로 나온다 (같은 크기 사각형 4개로 이루어진 하나의 박스)
      ...RACK,
      neck: NECK_TOP,
      body: R(CX - iw, exitY - iw, CX + iw, exitY + iw),
      m1: L(CX, exitY - iw, CX, exitY + iw),
      m2: L(CX - iw, exitY, CX + iw, exitY),
      it0: R(CX - iw, exitY - iw, CX, exitY),
      it1: R(CX, exitY - iw, CX + iw, exitY),
      it2: R(CX - iw, exitY, CX, exitY + iw),
      it3: R(CX, exitY, CX + iw, exitY + iw),
      wf: [CX, CY, 0],
      wr: [CX, CY, 0]
    },
    {
      // 7 박스가 레일 위에 오른다 (창고는 위로 물러난다)
      ...rest,
      body: R(CX - iw, sy - iw, CX + iw, sy + iw),
      m1: L(CX, sy - iw, CX, sy + iw),
      m2: L(CX - iw, sy, CX + iw, sy),
      it0: R(CX - iw, sy - iw, CX, sy),
      it1: R(CX, sy - iw, CX + iw, sy),
      it2: R(CX - iw, sy, CX, sy + iw),
      it3: R(CX, sy, CX + iw, sy + iw)
    },
    {
      // 8 하나씩 쪼개져 나란히 레일 위에 선다
      ...rest,
      body: R(CX - iw, sy - iw, CX + iw, sy + iw),
      m1: L(CX, sy - iw, CX, sy + iw),
      m2: L(CX - iw, sy, CX + iw, sy),
      it0: row(0),
      it1: row(1),
      it2: row(2),
      it3: row(3)
    },
    {
      // 9 검사기를 지난 뒤 — 합격 2개는 아래로, 불량 2개는 좌측 레일로
      ...rest,
      body: R(CX - iw, sy - iw, CX + iw, sy + iw),
      m1: L(CX, sy - iw, CX, sy + iw),
      m2: L(CX - iw, sy, CX + iw, sy),
      it0: itemAt(CX - bh * 1.18, vy),
      it1: itemAt(CX - bh * 0.28, vy),
      it2: itemAt(CX + bh * 0.28, vy),
      it3: itemAt(CX - bh * 1.76, vy),
      st1: R(CX - bh * 0.62, vy - bh * 0.5, CX - bh * 0.62, vy + bh * 0.5),
      st2: R(CX + bh * 0.62, vy - bh * 0.5, CX + bh * 0.62, vy + bh * 0.5)
    },
    {
      // 10 합격품 위로 갈색 상자가 씌워진다 (덮개는 아직 열린 상태)
      ...rest,
      body: packBox,
      m1: L(CX, vy, CX, vy),
      m2: L(CX, vy, CX, vy),
      it0: itemAt(CX - bh * 2.3, vy),
      it1: itemAt(CX - bh * 0.28, vy),
      it2: itemAt(CX + bh * 0.28, vy),
      it3: itemAt(CX - bh * 2.9, vy),
      st1: R(CX - bh * 0.62, vy - bh * 0.5, CX - bh * 0.62, vy + bh * 0.5),
      st2: R(CX + bh * 0.62, vy - bh * 0.5, CX + bh * 0.62, vy + bh * 0.5)
    },
    {
      // 11 덮개가 가운데로 닫히고 테이프가 붙는다
      ...rest,
      body: packBox,
      m1: L(CX, vy, CX, vy),
      m2: L(CX, vy, CX, vy),
      it0: itemAt(CX - bh * 2.3, vy),
      it1: itemAt(CX - bh * 0.28, vy),
      it2: itemAt(CX + bh * 0.28, vy),
      it3: itemAt(CX - bh * 2.9, vy),
      st1: R(CX - bh * 0.62, vy - bh * 0.5, CX, vy + bh * 0.5),
      st2: R(CX, vy - bh * 0.5, CX + bh * 0.62, vy + bh * 0.5)
    }
  ]
})

// ---- 공정 설비 (벨트 위/옆에 놓이는 고정 도형) ----
const machines = computed(() => {
  const { bh, iw, ih, sp, sy, vy } = dims.value
  const armX = CX + sp * 0.5
  const T = (kx, ky) => [CX + bh * kx, vy + bh * ky] // 트럭 좌표 헬퍼
  const RT = (a, b, c, d) => str(R(CX + bh * a, vy + bh * b, CX + bh * c, vy + bh * d))
  return {
    // 생산: 레일 바깥에 고정된 로봇 (제품 줄 sy 에서 작업)
    robotBase: str(R(CX + bh * 1.06, sy - bh * 0.1, CX + bh * 1.42, sy + bh * 0.34)),
    robotArm: str([
      [CX + bh * 1.24, sy - bh * 0.1],
      [CX + bh * 0.86, sy - bh * 0.74],
      [armX, sy - ih * 0.75]
    ]),
    robotJoint: [CX + bh * 1.24, sy - bh * 0.1],
    robotElbow: [CX + bh * 0.86, sy - bh * 0.74],
    robotGrip: str([
      [armX - iw * 0.4, sy - ih * 0.28],
      [armX, sy - ih * 0.75],
      [armX + iw * 0.4, sy - ih * 0.28]
    ]),

    // 검사: 벨트를 가로지르는 갠트리 + 카메라 (설명 제목과 같은 높이에 고정)
    visionBeamBar: str(L(CX - bh * 1.06, sy, CX + bh * 1.06, sy)),
    visionPillarL: str(R(CX - bh * 1.06, sy - bh * 0.26, CX - bh * 0.94, sy + bh * 0.26)),
    visionPillarR: str(R(CX + bh * 0.94, sy - bh * 0.26, CX + bh * 1.06, sy + bh * 0.26)),
    visionHousing: str(R(CX - bh * 0.4, sy - bh * 0.24, CX + bh * 0.4, sy + bh * 0.24)),
    visionHood: str(R(CX - bh * 0.22, sy + bh * 0.24, CX + bh * 0.22, sy + bh * 0.34)),
    visionLens: [CX, sy + bh * 0.06, iw * 0.3],
    visionLed: str(L(CX + bh * 0.26, sy - bh * 0.12, CX + bh * 0.32, sy - bh * 0.12)),
    visionScan: str([
      [CX - bh * 0.44, vy - ih * 0.56],
      [CX, sy + bh * 0.36],
      [CX + bh * 0.44, vy - ih * 0.56]
    ]),

    // 검사와 포장 사이: 좌측으로 빠지는 불량 배출 레일 (고정)
    rejectRail: str(R(CX - bh * 2.25, vy - ih * 0.82, CX - bh * 0.86, vy + ih * 0.82)),
    rejectSlats: [0.3, 0.6, 0.9, 1.2, 1.5, 1.8, 2.1].map((k) =>
      str(L(CX - bh * k, vy - ih * 0.82, CX - bh * k, vy + ih * 0.82))
    ),
    rejectArrow: str([
      [CX - bh * 1.98, vy + ih * 0.28],
      [CX - bh * 2.12, vy],
      [CX - bh * 1.98, vy - ih * 0.28]
    ]),
    rejectGate: str([
      [CX - bh * 0.86, vy - ih * 1.5],
      [CX - bh * 0.86, vy - ih * 0.82]
    ]),

    // 포장: 제품 줄에 고정된 케이스 실러 (여기서 덮개가 닫힌다)
    packFrameL: str(R(CX - bh * 1.02, vy - bh * 0.56, CX - bh * 0.84, vy + bh * 0.56)),
    packFrameR: str(R(CX + bh * 0.84, vy - bh * 0.56, CX + bh * 1.02, vy + bh * 0.56)),
    packHead: str(R(CX - bh * 0.26, vy - bh * 0.82, CX + bh * 0.26, vy - bh * 0.54)),
    packRoll: [CX, vy - bh * 0.68, iw * 0.26],
    packFeed: str(L(CX, vy - bh * 0.54, CX, vy - bh * 0.46)),
    packArmL: str(L(CX - bh * 0.84, vy - bh * 0.2, CX - bh * 0.66, vy - bh * 0.2)),
    packArmR: str(L(CX + bh * 0.84, vy - bh * 0.2, CX + bh * 0.66, vy - bh * 0.2)),

    // 출하: 위에서 내려다본 트럭 — 탑박스(화물칸)와 운전석을 분리하고 바퀴는 옆으로 내놓는다
    truckWheels: [
      [-1.08, -0.44, -0.88, -0.06],
      [0.88, -0.44, 1.08, -0.06],
      [-1.08, 0.12, -0.88, 0.5],
      [0.88, 0.12, 1.08, 0.5],
      [-0.94, 1.26, -0.74, 1.62],
      [0.74, 1.26, 0.94, 1.62]
    ].map((w) => RT(w[0], w[1], w[2], w[3])),
    truckRails: [RT(-0.58, 0.66, -0.44, 1.12), RT(0.44, 0.66, 0.58, 1.12)],
    truckCargo: RT(-0.9, -0.86, 0.9, 0.8),
    truckCargoInner: RT(-0.78, -0.74, 0.78, 0.6),
    truckRibs: [-0.62, 0.62].map((k) =>
      str(L(CX - bh * 0.78, vy + bh * k, CX + bh * 0.78, vy + bh * k))
    ),
    truckSeams: [-0.42, 0.42].map((k) =>
      str(L(CX + bh * k, vy - bh * 0.74, CX + bh * k, vy + bh * 0.6))
    ),
    truckCorners: [
      RT(-0.9, -0.86, -0.74, -0.7),
      RT(0.74, -0.86, 0.9, -0.7),
      RT(-0.9, 0.64, -0.74, 0.8),
      RT(0.74, 0.64, 0.9, 0.8)
    ],
    truckDoor: RT(-0.9, -0.86, 0.9, -0.68),
    truckHandle: str(L(CX - bh * 0.12, vy - bh * 0.77, CX + bh * 0.12, vy - bh * 0.77)),
    truckCab: RT(-0.76, 1.02, 0.76, 1.8),
    truckRoof: RT(-0.58, 1.1, 0.58, 1.46),
    truckVent: RT(-0.2, 1.16, 0.2, 1.34),
    truckWind: str([
      T(-0.58, 1.52),
      T(0.58, 1.52),
      T(0.48, 1.74),
      T(-0.48, 1.74),
      T(-0.58, 1.52)
    ]),
    truckBumper: RT(-0.6, 1.8, 0.6, 1.9),
    truckMirrors: [RT(-0.92, 1.44, -0.76, 1.58), RT(0.76, 1.44, 0.92, 1.58)],
    truckLights: [RT(-0.48, 1.81, -0.28, 1.89), RT(0.28, 1.81, 0.48, 1.89)],
    truckTrail: [
      str(L(CX - bh * 0.34, vy + bh * 2.02, CX - bh * 0.34, vy + bh * 2.32)),
      str(L(CX, vy + bh * 2.08, CX, vy + bh * 2.44)),
      str(L(CX + bh * 0.34, vy + bh * 2.02, CX + bh * 0.34, vy + bh * 2.32))
    ]
  }
})

// 설비는 자기 공정에서 고정되어 있고, 공정을 넘길 때 라인이 흐르듯 위로 물러난다
const stationT = (a1, a2, b1, b2) =>
  computed(() => {
    const d = BH.value * 1.35
    return (
      'translate(0 ' +
      (d * (1 - easeIO(seg(a1, a2))) - d * easeIO(seg(b1, b2))).toFixed(1) +
      ')'
    )
  })
const robotT = stationT(7.06, 7.26, 7.72, 7.95)
const visionT = stationT(7.88, 8.12, 8.72, 8.95)
const packerT = stationT(8.88, 9.12, 9.72, 9.95)

// ---- 장면 보간 ----
const geo = computed(() => {
  const list = scenes.value
  const p = sceneP.value
  const i0 = clamp(Math.floor(p), 0, list.length - 1)
  const i1 = Math.min(i0 + 1, list.length - 1)
  const t = clamp(p - i0, 0, 1)
  const A = list[i0]
  const B = list[i1]
  const line = (k) => str(mix(A[k], B[k], t))
  const op = {}
  for (const k of Object.keys(OPS[0])) op[k] = lerp(OPS[i0][k], OPS[i1][k], t)
  const body = mix(A.body, B.body, t)
  // 분할 구간에서는 조각이 하나씩 떨어져 나온다
  const split = i0 === 7
  const items = [0, 1, 2, 3].map((i) => {
    const ti = split ? clamp((t - i * 0.16) / 0.52, 0, 1) : t
    return {
      d: str(mix(A['it' + i], B['it' + i], ti)),
      op: lerp(OPS[i0].items, OPS[i1].items, ti)
    }
  })
  if (split) {
    const f = 1 - clamp(t * 2.4, 0, 1)
    op.body = f
    op.mark = f
  }
  return {
    body: str(body),
    neck: line('neck'),
    ground: line('ground'),
    m1: line('m1'),
    m2: line('m2'),
    postL: line('postL'),
    postR: line('postR'),
    shelfA: line('shelfA'),
    shelfB: line('shelfB'),
    shelfC: line('shelfC'),
    st1: line('st1'),
    st2: line('st2'),
    items,
    wf: A.wf.map((v, i) => lerp(v, B.wf[i], t)),
    wr: A.wr.map((v, i) => lerp(v, B.wr[i], t)),
    op,
    top: body[0][1],
    bottom: body[2][1]
  }
})

const cineStyle = computed(() => ({
  left: cineX.value + 'px',
  top: cineY.value + 'px',
  transform: 'translate(-50%, -50%) scale(' + cineScale.value.toFixed(3) + ')'
}))
const drawStyle = computed(() => ({ strokeDashoffset: (1 - draw.value).toFixed(3) }))
// 갈색(크라프트) 상자 — 위에서 내려다본 형태
const KRAFT_FILL = '#d9b68a'
const KRAFT_STROKE = '#a67c47'
const kraftStyle = computed(() => {
  const f = geo.value.op.fill
  return {
    fill: KRAFT_FILL,
    fillOpacity: f,
    stroke: mixColor(mixColor(DARK.stroke, LIGHT.stroke, bgT.value), KRAFT_STROKE, f)
  }
})

const cross = (x, y) => {
  const r = dims.value.iw * 0.24
  return 'M' + (x - r) + ' ' + (y - r) + 'l' + 2 * r + ' ' + 2 * r + 'M' + (x + r) + ' ' + (y - r) + 'l' + -2 * r + ' ' + 2 * r
}
const cargoTransform = computed(() => 'translate(0 ' + departY.value.toFixed(1) + ')')
const truckTransform = computed(
  () => 'translate(0 ' + (BH.value * 3.0 * (1 - truckEnter.value)).toFixed(1) + ')'
)

// 벨트는 창고에서 나온 상자 높이에서부터 위아래로 펼쳐진다
const beltClip = computed(() => {
  const e = beltReveal.value
  if (e >= 1) return 'inset(0 0 0 0)'
  const s = cineScale.value
  const H = vh.value
  const elTop = -0.08 * H
  const elH = 1.16 * H
  const top = cineY.value + (geo.value.top - CY) * s
  const bot = cineY.value + (geo.value.bottom - CY) * s
  const a = clamp((top - elTop) / elH, 0, 1) * (1 - e)
  const b = clamp(1 - (bot - elTop) / elH, 0, 1) * (1 - e)
  return 'inset(' + (a * 100).toFixed(2) + '% 0 ' + (b * 100).toFixed(2) + '% 0)'
})

// ---- 편지 접기 ----
const pct = (v) => (v * 100).toFixed(2) + '%'
const face = (t) => {
  const k = clamp((t - 0.42) / 0.14, 0, 1)
  return (
    'rgb(' +
    Math.round(lerp(255, 222, k)) +
    ',' +
    Math.round(lerp(255, 227, k)) +
    ',' +
    Math.round(lerp(255, 236, k)) +
    ')'
  )
}
// 들려 올라간 면의 음영 — 경첩 쪽은 어둡고 바깥(가까운) 모서리는 밝다
const shade = (dir, t) => {
  const a = (0.16 * clamp(t * 1.4, 0, 1)).toFixed(3)
  const b = (0.5 * clamp(t * 1.4, 0, 1)).toFixed(3)
  return (
    'linear-gradient(to ' +
    dir +
    ', rgba(15, 23, 42, ' +
    a +
    ') 0%, rgba(255, 255, 255, ' +
    b +
    ') 78%)'
  )
}
// 들려 올라간 면이 종이 위에 드리우는 그림자 띠
const foldShade = (dir, frac, t) => ({
  opacity: (Math.sin(Math.PI * clamp(t, 0, 1)) * 0.95).toFixed(3),
  background:
    'linear-gradient(to ' + dir + ', rgba(15, 23, 42, 0.26), rgba(15, 23, 42, 0) 92%)',
  [dir === 'bottom' || dir === 'top' ? 'height' : 'width']: pct(frac * (0.35 + 0.65 * t))
})
// 접히는 면이 아래 종이에 드리우는 그림자
const castShadow = (t, dx, dy) => {
  const a = (0.3 * clamp(t * 1.6, 0, 1)).toFixed(3)
  return dx + 'px ' + dy + 'px 30px rgba(2, 8, 20, ' + a + ')'
}
const sheetStyle = computed(() => ({
  opacity: sheetOp.value,
  transform:
    'translateY(' +
    lerp(0, -27.5 * cineScale.value, fly.value).toFixed(1) +
    'px) scale(' +
    lerp(1, 0.32, fly.value).toFixed(3) +
    ')'
}))
const vCut = computed(() => pct(foldFy.value * foldTop.value))
const fsL = computed(() => ({
  left: pct(foldFx.value),
  ...foldShade('right', foldFx.value, foldSide.value)
}))
const fsR = computed(() => ({
  right: pct(foldFx.value),
  ...foldShade('left', foldFx.value, foldSide.value)
}))
const fsT = computed(() => ({
  top: pct(foldFy.value),
  ...foldShade('bottom', foldFy.value, foldTop.value)
}))
const fsB = computed(() => ({
  bottom: pct(foldFy.value),
  ...foldShade('top', foldFy.value, foldTop.value)
}))
const panelC = computed(() => ({
  clipPath:
    'inset(' + vCut.value + ' ' + pct(foldFx.value) + ' ' + vCut.value + ' ' + pct(foldFx.value) + ')'
}))
const panelL = computed(() => ({
  backgroundImage: shade('left', foldSide.value),
  boxShadow: castShadow(foldSide.value, 14, 0),
  zIndex: 2,
  background: face(foldSide.value),
  clipPath: 'inset(' + vCut.value + ' ' + pct(1 - foldFx.value) + ' ' + vCut.value + ' 0)',
  transformOrigin: pct(foldFx.value) + ' 50%',
  transform: 'rotateY(' + (180 * foldSide.value).toFixed(2) + 'deg)'
}))
const panelR = computed(() => ({
  backgroundImage: shade('right', foldSide.value),
  boxShadow: castShadow(foldSide.value, -14, 0),
  zIndex: 2,
  background: face(foldSide.value),
  clipPath: 'inset(' + vCut.value + ' 0 ' + vCut.value + ' ' + pct(1 - foldFx.value) + ')',
  transformOrigin: pct(1 - foldFx.value) + ' 50%',
  transform: 'rotateY(' + (-180 * foldSide.value).toFixed(2) + 'deg)'
}))
const panelT = computed(() => ({
  backgroundImage: shade('top', foldTop.value),
  boxShadow: castShadow(foldTop.value, 0, 16),
  zIndex: foldTop.value > 0.001 ? 3 : 1,
  background: face(foldTop.value),
  clipPath:
    'inset(0 ' + pct(foldFx.value) + ' ' + pct(1 - foldFy.value) + ' ' + pct(foldFx.value) + ')',
  transformOrigin: '50% ' + pct(foldFy.value),
  transform: 'rotateX(' + (-180 * foldTop.value).toFixed(2) + 'deg)'
}))
const panelB = computed(() => ({
  backgroundImage: shade('bottom', foldTop.value),
  boxShadow: castShadow(foldTop.value, 0, -16),
  zIndex: foldTop.value > 0.001 ? 3 : 1,
  background: face(foldTop.value),
  clipPath:
    'inset(' + pct(1 - foldFy.value) + ' ' + pct(foldFx.value) + ' 0 ' + pct(foldFx.value) + ')',
  transformOrigin: '50% ' + pct(1 - foldFy.value),
  transform: 'rotateX(' + (180 * foldTop.value).toFixed(2) + 'deg)'
}))

let raf = 0
let ioActive = null
let ioReveal = null

function measure() {
  vh.value = window.innerHeight || 1
  vw.value = window.innerWidth || 1
  y.value = window.scrollY
  docMax.value = Math.max(1, document.documentElement.scrollHeight - window.innerHeight)
}

function onScroll() {
  if (raf) return
  raf = requestAnimationFrame(() => {
    raf = 0
    measure()
  })
}

onMounted(() => {
  const sections = rootEl.value.querySelectorAll('[data-idx]')

  ioActive = new IntersectionObserver(
    (entries) => {
      let best = null
      let bestD = Infinity
      entries.forEach((e) => {
        if (!e.isIntersecting) return
        const r = e.target.getBoundingClientRect()
        const d = Math.abs(r.top + r.height / 2 - window.innerHeight / 2)
        if (d < bestD) {
          bestD = d
          best = e.target
        }
      })
      if (best) active.value = Number(best.dataset.idx)
    },
    { rootMargin: '-48% 0px -48% 0px', threshold: 0 }
  )

  ioReveal = new IntersectionObserver(
    (entries) => {
      entries.forEach((e) => {
        if (!e.isIntersecting) return
        const idx = Number(e.target.dataset.idx)
        if (idx < 0) return
        const next = new Set(revealed.value)
        next.add(idx)
        revealed.value = next
        ioReveal.unobserve(e.target)
      })
    },
    { rootMargin: '0px 0px -18% 0px', threshold: 0.2 }
  )

  sections.forEach((el) => {
    ioActive.observe(el)
    ioReveal.observe(el)
  })

  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', onScroll)
  measure()
})

onBeforeUnmount(() => {
  ioActive?.disconnect()
  ioReveal?.disconnect()
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', onScroll)
  if (raf) cancelAnimationFrame(raf)
})
</script>

<template>
  <div class="mes" ref="rootEl" :style="themeVars">
    <div class="prog" :style="{ transform: 'scaleX(' + progress + ')' }" />

    <header class="topbar">
      <span class="brand">MES FLOW</span>
      <span class="counter" :style="{ opacity: active >= 0 ? 1 : 0 }">
        {{ current.no }} <i>/</i> {{ total }}
      </span>
    </header>

    <!-- 고정 컨베이어 벨트 (출고 구간에서 상자와 함께 드러난다) -->
    <div class="belt" :style="{ opacity: beltOp, clipPath: beltClip }" aria-hidden="true">
      <div class="rail rail-l">
        <div class="rollers" :style="{ transform: 'translateY(' + beltY + 'px)' }" />
      </div>
      <div class="surface">
        <div class="slats" :style="{ transform: 'translateY(' + beltY + 'px)' }" />
      </div>
      <div class="rail rail-r">
        <div class="rollers" :style="{ transform: 'translateY(' + beltY + 'px)' }" />
      </div>
    </div>

    <!-- 중앙 연출 -->
    <svg
      class="cine"
      :style="cineStyle"
      :width="STAGE_W"
      :height="STAGE_H"
      :viewBox="'0 0 ' + STAGE_W + ' ' + STAGE_H"
      fill="none"
      stroke="currentColor"
      stroke-width="2.4"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      <!-- 창고 / 달력 -->
      <polyline :points="geo.shelfA" :style="{ opacity: geo.op.rack * rackFade }" />
      <polyline :points="geo.shelfB" :style="{ opacity: geo.op.rack * rackFade }" />
      <polyline :points="geo.shelfC" :style="{ opacity: geo.op.rack * rackFade }" />
      <polyline :points="geo.postL" :style="{ opacity: geo.op.rack * rackFade }" />
      <polyline :points="geo.postR" :style="{ opacity: geo.op.rack * rackFade }" />

      <!-- 바닥 / 받침 -->
      <polyline
        :points="geo.ground"
        pathLength="1"
        stroke-dasharray="1"
        :style="[drawStyle, { opacity: geo.op.ground }]"
      />
      <polyline
        :points="geo.neck"
        pathLength="1"
        stroke-dasharray="1"
        :style="[drawStyle, { opacity: geo.op.neck }]"
      />

      <!-- 생산: 고정된 로봇 -->
      <g class="mach" :transform="robotT" :style="{ opacity: robotOp }">
        <polyline class="unit" :points="machines.robotBase" />
        <polyline :points="machines.robotArm" />
        <polyline :points="machines.robotGrip" />
        <circle :cx="machines.robotJoint[0]" :cy="machines.robotJoint[1]" r="6" />
        <circle :cx="machines.robotElbow[0]" :cy="machines.robotElbow[1]" r="6" />
      </g>

      <!-- 검사: 고정된 머신비전 + 좌측 불량 배출 레일 -->
      <g :transform="visionT" :style="{ opacity: visionOp }">
        <polyline class="railstrip" :points="machines.rejectRail" />
        <polyline
          v-for="(sl, i) in machines.rejectSlats"
          :key="'rs' + i"
          class="railslat"
          :points="sl"
        />
        <g class="mach">
          <polyline :points="machines.rejectGate" />
          <polyline :points="machines.rejectArrow" />
          <polyline :points="machines.visionBeamBar" />
          <polyline class="unit" :points="machines.visionPillarL" />
          <polyline class="unit" :points="machines.visionPillarR" />
          <polyline class="unit" :points="machines.visionHousing" />
          <polyline class="unit" :points="machines.visionHood" />
          <circle
            class="ac"
            :cx="machines.visionLens[0]"
            :cy="machines.visionLens[1]"
            :r="machines.visionLens[2]"
          />
          <polyline class="ac" :points="machines.visionLed" />
          <polyline class="ac beam" :points="machines.visionScan" />
        </g>
      </g>

      <!-- 포장: 고정된 케이스 실러 -->
      <g class="mach" :transform="packerT" :style="{ opacity: packerOp }">
        <polyline class="unit" :points="machines.packFrameL" />
        <polyline class="unit" :points="machines.packFrameR" />
        <polyline :points="machines.packArmL" />
        <polyline :points="machines.packArmR" />
        <polyline class="unit" :points="machines.packHead" />
        <circle
          class="ac"
          :cx="machines.packRoll[0]"
          :cy="machines.packRoll[1]"
          :r="machines.packRoll[2]"
        />
        <polyline :points="machines.packFeed" />
      </g>

      <!-- 제품과 차량은 함께 출발한다 -->
      <g :transform="cargoTransform" :style="{ opacity: cargoOp }">
        <!-- 수직으로 선 트럭 (화물칸 위, 운전석 아래) -->
        <g class="truck" :transform="truckTransform" :style="{ opacity: truckOp }">
          <!-- 바퀴와 프레임은 차체 아래로 -->
          <polyline v-for="(w, i) in machines.truckWheels" :key="'w' + i" class="t-wheel" :points="w" />
          <polyline v-for="(r, i) in machines.truckRails" :key="'rl' + i" class="t-chassis" :points="r" />
          <polyline v-for="(m, i) in machines.truckMirrors" :key="'m' + i" class="t-dark" :points="m" />
          <!-- 탑박스(화물칸) -->
          <polyline class="t-body" :points="machines.truckCargo" />
          <polyline class="t-inner" :points="machines.truckCargoInner" />
          <polyline v-for="(r, i) in machines.truckRibs" :key="'r' + i" class="t-rib" :points="r" />
          <polyline v-for="(sm, i) in machines.truckSeams" :key="'sm' + i" class="t-rib" :points="sm" />
          <polyline class="t-door" :points="machines.truckDoor" />
          <polyline class="t-handle" :points="machines.truckHandle" />
          <polyline v-for="(c, i) in machines.truckCorners" :key="'c' + i" class="t-corner" :points="c" />
          <!-- 운전석 -->
          <polyline class="t-cab" :points="machines.truckCab" />
          <polyline class="t-roof" :points="machines.truckRoof" />
          <polyline class="t-vent" :points="machines.truckVent" />
          <polyline class="t-glass" :points="machines.truckWind" />
          <polyline class="t-dark" :points="machines.truckBumper" />
          <polyline v-for="(l, i) in machines.truckLights" :key="'l' + i" class="t-light" :points="l" />
          <polyline
            v-for="(t, i) in machines.truckTrail"
            :key="'t' + i"
            class="trail mach"
            :points="t"
          />
        </g>

        <!-- 상자 -->
        <polyline
          :points="geo.body"
          pathLength="1"
          stroke-dasharray="1"
          :style="[drawStyle, kraftStyle, { opacity: geo.op.body }]"
        />

        <!-- 4개로 쪼개진 제품 -->
        <polyline
          v-for="(it, i) in geo.items"
          :key="'it' + i"
          class="prod"
          :points="it.d"
          :style="{
            opacity: it.op * (i === 0 || i === 3 ? rejectOut : 1),
            fillOpacity: it.op * geo.op.ifill * (i === 0 || i === 3 ? rejectOut : 1)
          }"
        />
        <!-- 포장 덮개 -->
        <polyline :points="geo.st1" :style="[kraftStyle, { opacity: geo.op.st }]" />
        <polyline :points="geo.st2" :style="[kraftStyle, { opacity: geo.op.st }]" />
        <polyline
          :points="geo.m1"
          pathLength="1"
          stroke-dasharray="1"
          :style="[drawStyle, { opacity: geo.op.mark }]"
        />
        <polyline
          :points="geo.m2"
          pathLength="1"
          stroke-dasharray="1"
          :style="[drawStyle, { opacity: geo.op.mark }]"
        />
        <!-- 불량 표시 -->
        <g class="bad" :style="{ opacity: rejectOp * geo.items[0].op * rejectOut }">
          <path :d="cross(240 - dims.rx1, dims.vy)" />
          <path :d="cross(240 - dims.rx2, dims.vy)" />
        </g>
      </g>

      <!-- 모니터 체크 / 바퀴 -->
      <circle
        class="ac"
        :cx="geo.wr[0]"
        :cy="geo.wr[1]"
        :r="geo.wr[2]"
        :style="{ opacity: geo.op.wr * circIn }"
      />
      <circle
        class="ac"
        :cx="geo.wf[0]"
        :cy="geo.wf[1]"
        :r="geo.wf[2]"
        :style="{ opacity: geo.op.wf * circIn }"
      />
      <path class="ac" d="M291.5 145.5l6 6 12-13" :style="{ opacity: checkOp }" />

      <!-- 태블릿으로 수량을 검수하는 담당자 -->
      <g :style="{ opacity: personOp }">
        <circle cx="46" cy="186" r="9.5" />
        <path d="M35 232v-20a11 11 0 0 1 11-11 11 11 0 0 1 11 11v20" />
        <path d="M40 232l-2 30M52 232l2 30" />
        <path d="M56 213l9 6" />
        <rect class="ac" x="63" y="199" width="25" height="18" rx="2.5" />
        <path class="ac" d="M69 206h13M69 211h8" />
      </g>
    </svg>

    <!-- 편지처럼 접히는 첫 화면 -->
    <div class="sheet" :style="sheetStyle" aria-hidden="true">
      <div class="panel" :style="panelC">
        <div class="hero-copy" :style="{ opacity: heroOp }">
          <p class="eyebrow">MANUFACTURING EXECUTION SYSTEM</p>
          <h1>수주에서 출하까지,<br />하나의 흐름으로</h1>
          <p class="hero-sub">
            주문서 한 장이 제품이 되어 나가기까지<br />
            MES가 기록하는 과정을 따라가 보세요.
          </p>
          <div class="scroll-cue"><span /></div>
        </div>
      </div>
      <!-- 미리 그어진 접힘선 -->
      <div class="creases" aria-hidden="true">
        <span class="cr cr-v" :style="{ left: pct(foldFx), opacity: creaseV }" />
        <span class="cr cr-v" :style="{ left: pct(1 - foldFx), opacity: creaseV }" />
        <span class="cr cr-h" :style="{ top: pct(foldFy), opacity: creaseH }" />
        <span class="cr cr-h" :style="{ top: pct(1 - foldFy), opacity: creaseH }" />
      </div>
      <div class="fshade fs-x" :style="fsL" />
      <div class="fshade fs-x" :style="fsR" />
      <div class="panel fold" :style="panelL" />
      <div class="panel fold" :style="panelR" />
      <div class="fshade fs-y" :style="fsT" />
      <div class="fshade fs-y" :style="fsB" />
      <div class="panel fold" :style="panelT" />
      <div class="panel fold" :style="panelB" />
    </div>

    <!-- 첫 화면 스크롤 길이 확보용 -->
    <section class="hero-space" data-idx="-1" />

    <template v-for="(s, i) in stages" :key="s.key">
      <section
        class="stage"
        :class="{ flip: i % 2 === 1, 'is-in': revealed.has(i) }"
        :data-idx="i"
      >
        <div class="col col-txt">
          <p class="no">{{ s.no }}</p>
          <h2 class="title">{{ s.title }}</h2>
          <p class="desc">{{ s.desc }}</p>
          <ul class="tags">
            <li v-for="t in s.tags" :key="t">{{ t }}</li>
          </ul>
        </div>
      </section>
      <!-- 수주 -> 입고 사이: 모니터가 트럭으로 바뀌는 구간 -->
      <div v-if="i === 0" class="gap-a" aria-hidden="true" />
      <!-- 입고 -> 재고 사이: 트럭이 작은 상자가 되고 창고가 세워지는 구간 -->
      <div v-if="i === 1" class="gap-b" aria-hidden="true" />
      <!-- 출하 뒤: 트럭이 출발하는 구간 -->
      <div v-if="i === stages.length - 1" class="gap-c" aria-hidden="true" />
    </template>

    <section class="outro" :data-idx="stages.length">
      <h2>모든 공정이 데이터로 남습니다</h2>
      <p>
        어느 자재가, 어떤 설비를 거쳐, 누구의 손에서, 언제 출하되었는지<br />
        MES는 아홉 공정의 기록을 하나의 흐름으로 연결합니다.
      </p>
      <router-link class="home" to="/">처음으로 돌아가기</router-link>
    </section>
  </div>
</template>

<style scoped>
.mes {
  /* 아래 토큰들은 스크롤에 따라 인라인 스타일로 덮어써진다 (딥네이비 -> 밝은 배경) */
  --ex-bg: #0e1a2f;
  --ex-paper: #ffffff;
  --ex-line: #22334d;
  --ex-text: #f1f5f9;
  --ex-muted: #94a3b8;
  --ex-accent: #60a5fa;
  --ex-stroke: #cbd5e1;
  --ex-tag: #14243d;
  --belt-w: 260px;

  position: relative;
  background: var(--ex-bg);
  color: var(--ex-text);
  overflow-x: hidden;
  letter-spacing: -0.01em;
}

/* ---- 상단 ---- */
.prog {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--ex-accent);
  transform-origin: 0 50%;
  z-index: 30;
}
.topbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 28px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--ex-muted);
  z-index: 20;
  pointer-events: none;
}
.counter {
  color: var(--ex-text);
  transition: opacity 0.3s;
}
.counter i {
  font-style: normal;
  color: var(--ex-line);
  margin: 0 2px;
}

/* ---- 편지 접기 ---- */
/* 딥네이비 배경 위에 놓인 한 장의 편지지 */
.sheet {
  position: fixed;
  inset: 4.5vh 5.5vw;
  z-index: 12;
  pointer-events: none;
  overflow: hidden; /* 접히는 면이 종이 밖으로 나가지 않도록 */
  will-change: transform, opacity;
  filter: drop-shadow(0 26px 60px rgba(2, 8, 20, 0.55));
}
.creases {
  position: absolute;
  inset: 0;
  z-index: 1;
}
.cr {
  position: absolute;
  background: rgba(15, 23, 42, 0.16);
}
.cr-v {
  top: 0;
  bottom: 0;
  width: 1px;
}
.cr-h {
  left: 0;
  right: 0;
  height: 1px;
}
/* 접히는 면이 종이 위에 드리우는 그림자 */
.fshade {
  position: absolute;
  pointer-events: none;
}
.fs-x {
  top: 0;
  bottom: 0;
  z-index: 1;
}
.fs-y {
  left: 0;
  right: 0;
  z-index: 2;
}
.panel {
  position: absolute;
  inset: 0;
  background: var(--ex-paper);
}

.hero-copy {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 0 24px;
}
.eyebrow {
  margin: 0 0 20px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: #2563eb;
}
.hero-copy h1 {
  margin: 0;
  color: #111827;
  font-size: 46px;
  font-weight: 700;
  line-height: 1.32;
  letter-spacing: -0.03em;
}
.hero-sub {
  margin: 22px 0 0;
  font-size: 16px;
  line-height: 1.7;
  color: #6b7280;
}
.scroll-cue {
  margin-top: 56px;
  width: 22px;
  height: 36px;
  border: 1.5px solid #e2e5ea;
  border-radius: 12px;
  display: grid;
  justify-items: center;
  padding-top: 7px;
}
.scroll-cue span {
  width: 3px;
  height: 7px;
  border-radius: 2px;
  background: #2563eb;
  animation: cue 1.6s ease-in-out infinite;
}
@keyframes cue {
  0%,
  100% {
    transform: translateY(0);
    opacity: 1;
  }
  60% {
    transform: translateY(12px);
    opacity: 0;
  }
}

/* ---- 중앙 연출 SVG ---- */
.cine {
  position: fixed;
  z-index: 6;
  color: var(--ex-stroke);
  pointer-events: none;
  overflow: visible;
}
.cine .ac {
  stroke: var(--ex-accent);
}
/* 설비는 밝은 배경과 어두운 벨트 양쪽에서 모두 보이는 중간 톤 */
.cine .mach {
  stroke: #9aa2b1;
}
/* 벨트 위 제품은 흰 면으로 채워 어두운 벨트와 구분한다 */
.cine .prod {
  fill: var(--ex-paper);
  stroke: #7c8695;
}
.cine .beam {
  stroke-dasharray: 6 9;
  opacity: 0.8;
}
.cine .trail {
  stroke-dasharray: 5 8;
  opacity: 0.6;
}
/* 설비 몸체는 불투명하게 채워 벨트와 겹치지 않게 */
.cine .unit {
  fill: #eef0f4;
}
/* 출하 트럭 — 선이 아닌 채색 일러스트 */
.cine .truck polyline {
  stroke: none;
}
.cine .t-body {
  fill: #dfe4ec;
  stroke: #9aa3b4;
  stroke-width: 2;
}
.cine .t-inner {
  fill: #eef1f6;
}
.cine .t-rib {
  stroke: #c3cbdb;
  stroke-width: 1.5;
}
.cine .t-corner {
  fill: #97a1b5;
}
.cine .t-handle {
  stroke: #7c8798;
  stroke-width: 2.2;
}
.cine .t-door {
  fill: #b9c2d2;
}
.cine .t-chassis {
  fill: #6b7488;
}
.cine .t-cab {
  fill: #3f4c66;
  stroke: #2c3648;
  stroke-width: 2;
}
.cine .t-roof {
  fill: #556482;
}
.cine .t-vent {
  fill: #5a6a88;
}
.cine .t-glass {
  fill: #1e2b40;
}
.cine .t-dark {
  fill: #2c3648;
}
.cine .t-wheel {
  fill: #2f3646;
}
.cine .t-light {
  fill: #f7c96b;
}
/* 불량 배출 레일 */
.cine .railstrip {
  fill: #232937;
  stroke: #7c8695;
}
.cine .railslat {
  stroke: rgba(255, 255, 255, 0.16);
  stroke-width: 1.6;
}
.cine .bad {
  stroke: #e0654f;
  stroke-width: 2.8;
}

/* ---- 컨베이어 벨트 ---- */
.belt {
  position: fixed;
  top: -8vh;
  height: 116vh;
  left: 50%;
  width: var(--belt-w);
  transform: translateX(-50%);
  display: flex;
  z-index: 0;
  pointer-events: none;
  -webkit-mask-image: linear-gradient(to bottom, transparent 0, #000 12%, #000 88%, transparent 100%);
  mask-image: linear-gradient(to bottom, transparent 0, #000 12%, #000 88%, transparent 100%);
}
.rail {
  position: relative;
  width: 22px;
  flex: none;
  background: #cfd4dc;
  overflow: hidden;
}
.rail-l {
  box-shadow: inset -1px 0 0 rgba(15, 23, 42, 0.12);
}
.rail-r {
  box-shadow: inset 1px 0 0 rgba(15, 23, 42, 0.12);
}
.rollers {
  position: absolute;
  left: 0;
  right: 0;
  top: -40px;
  height: calc(100% + 80px);
  background-image: repeating-linear-gradient(to bottom, #9aa2b1 0 10px, transparent 10px 40px);
  will-change: transform;
}
.surface {
  position: relative;
  flex: 1;
  background: #232937;
  overflow: hidden;
}
.surface::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    rgba(0, 0, 0, 0.35),
    rgba(0, 0, 0, 0) 16%,
    rgba(0, 0, 0, 0) 84%,
    rgba(0, 0, 0, 0.35)
  );
}
.slats {
  position: absolute;
  left: 0;
  right: 0;
  top: -40px;
  height: calc(100% + 80px);
  background-image: repeating-linear-gradient(
    to bottom,
    rgba(255, 255, 255, 0.13) 0 2px,
    rgba(255, 255, 255, 0) 2px 40px
  );
  will-change: transform;
}

/* ---- 섹션 ---- */
.hero-space {
  height: 100vh;
}
/* 공정 사이 여백 (중앙 연출이 다음 모습으로 바뀌는 구간) */
.gap-a {
  height: 70vh;
}
.gap-b {
  height: 80vh;
}
.gap-c {
  height: 50vh;
}
.stage {
  position: relative;
  z-index: 2;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr var(--belt-w) 1fr;
  align-items: center;
}
.col-txt {
  grid-column: 3;
  grid-row: 1;
  padding: 0 44px 0 84px;
  text-align: left;
  opacity: 0;
  transform: translateY(22px);
  transition: opacity 0.8s ease 0.08s, transform 0.8s cubic-bezier(0.22, 1, 0.36, 1) 0.08s;
}
.flip .col-txt {
  grid-column: 1;
  padding: 0 84px 0 44px;
  text-align: right;
}
.stage.is-in .col-txt {
  opacity: 1;
  transform: none;
}
.no {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.16em;
  color: var(--ex-accent);
}
.title {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.03em;
}
.desc {
  margin: 14px 0 0;
  max-width: 320px;
  font-size: 15px;
  line-height: 1.75;
  color: var(--ex-muted);
}
.flip .desc {
  margin-left: auto;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  list-style: none;
  margin: 22px 0 0;
  padding: 0;
}
.flip .tags {
  justify-content: flex-end;
}
.tags li {
  padding: 5px 12px;
  border: 1px solid var(--ex-line);
  border-radius: 999px;
  background: var(--ex-tag);
  font-size: 12px;
  font-weight: 600;
  color: var(--ex-muted);
}

/* ---- 마무리 ---- */
.outro {
  position: relative;
  z-index: 2;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 0 24px 80px;
}
.outro h2 {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.03em;
}
.outro p {
  margin: 18px 0 0;
  font-size: 15px;
  line-height: 1.8;
  color: var(--ex-muted);
}
.home {
  margin-top: 34px;
  padding: 12px 22px;
  border: 1px solid var(--ex-line);
  border-radius: 999px;
  background: var(--ex-tag);
  font-size: 14px;
  font-weight: 600;
  transition: border-color 0.2s, color 0.2s;
}
.home:hover {
  border-color: var(--ex-accent);
  color: var(--ex-accent);
}

/* ---- 반응형: 벨트를 좌측 레일로, 설명은 아래쪽으로 ---- */
@media (max-width: 900px) {
  .mes {
    --belt-w: 140px;
  }
  .rail {
    width: 14px;
  }
  /* 스크롤 위치와 공정이 1:1로 맞아야 하므로 모든 구간이 한 화면 높이 */
  .stage {
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    min-height: 100vh;
    padding: 0 16px 7vh;
  }
  /* 설명은 벨트 위를 덮는 카드로 (중앙 연출은 위쪽에 그대로 보인다) */
  .col-txt,
  .flip .col-txt {
    grid-column: auto;
    padding: 16px 16px 18px;
    text-align: left;
    transform: translateY(20px);
    background: var(--ex-bg);
    border: 1px solid var(--ex-line);
    border-radius: 16px;
  }
  .flip .desc {
    margin-left: 0;
  }
  .flip .tags {
    justify-content: flex-start;
  }
  .hero-copy h1 {
    font-size: 30px;
  }
  .hero-sub {
    font-size: 14px;
  }
  .no {
    margin-bottom: 6px;
    font-size: 12px;
  }
  .title,
  .outro h2 {
    font-size: 22px;
  }
  .desc {
    margin-top: 10px;
    font-size: 14px;
    line-height: 1.6;
  }
  .tags {
    margin-top: 14px;
    gap: 6px;
  }
  .tags li {
    padding: 4px 10px;
    font-size: 11px;
  }
  .desc,
  .outro p {
    max-width: none;
  }
  .outro {
    padding: 0 18px 60px;
    text-align: left;
    align-items: flex-start;
  }
  .topbar {
    padding: 16px 20px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mes *,
  .mes *::before,
  .mes *::after {
    animation: none !important;
    transition: none !important;
  }
}
</style>
