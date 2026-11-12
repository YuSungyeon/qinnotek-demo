<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

// 수주 -> 출하 공정. 중앙 컨베이어 위 상자가 공정을 통과하며 변화한다.
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
    desc: '수주 정보를 기반으로 생산계획과 작업지시가 생성됩니다.',
    tags: ['작업지시', '설비 배정', '일정']
  },
  {
    no: '05',
    key: 'produce',
    title: '생산',
    desc: '설비를 통해 제품이 가공·조립되고 실적이 자동 수집됩니다.',
    tags: ['공정 실적', '설비 가동', '작업자']
  },
  {
    no: '06',
    key: 'inspect',
    title: '검사',
    desc: '완성된 제품의 품질을 검사하고 합격 여부를 판정합니다.',
    tags: ['검사기준', '불량 유형', '판정']
  },
  {
    no: '07',
    key: 'pack',
    title: '포장',
    desc: '검사에 합격한 제품은 포장되어 출하 준비가 완료됩니다.',
    tags: ['포장 단위', '라벨', '박스 번호']
  },
  {
    no: '08',
    key: 'ship',
    title: '출하',
    desc: '제품이 출하되어 고객에게 전달될 준비를 마칩니다.',
    tags: ['출하지시', '거래명세서', '추적']
  }
]

// 좌/우 작업 일러스트 (선형, currentColor 상속 / .ac = 포인트 색)
const art = {
  order:
    '<rect x="30" y="24" width="140" height="82" rx="8"/><path d="M85 106v16M115 106v16M74 122h52"/><path d="M52 46h52M52 60h52M52 74h32"/><circle class="ac" cx="132" cy="72" r="15"/><path class="ac" d="M126 72l4.5 4.6L139 68"/>',
  inbound:
    '<path d="M96 118V54l40-24 40 24v64"/><path d="M120 118V86h32v32"/><path d="M96 54h80"/><rect x="22" y="74" width="46" height="38" rx="4"/><path d="M22 88h46M45 74v38"/><path class="ac" d="M76 94h18M88 88l6 6-6 6"/><path d="M10 118h182"/>',
  stock:
    '<path d="M40 26v100M160 26v100"/><path d="M40 26h120M40 58h120M40 92h120M40 126h120"/><rect x="52" y="32" width="32" height="24" rx="3"/><rect x="94" y="32" width="24" height="24" rx="3"/><rect class="ac" x="52" y="66" width="36" height="24" rx="3"/><rect x="98" y="66" width="26" height="24" rx="3"/><rect x="52" y="100" width="26" height="24" rx="3"/><rect x="90" y="100" width="34" height="24" rx="3"/>',
  plan:
    '<rect x="24" y="28" width="112" height="94" rx="8"/><path d="M24 54h112M52 20v16M108 20v16"/><path d="M44 70h12M74 70h12M104 70h12M44 94h12M74 94h12"/><circle class="ac" cx="140" cy="96" r="26" fill="var(--ex-bg)"/><path class="ac" d="M140 82v14l10 6"/>',
  produce:
    '<rect x="38" y="106" width="34" height="20" rx="4"/><path d="M55 106V74M55 74l40-26M95 48l30 18"/><circle cx="55" cy="74" r="5"/><circle cx="95" cy="48" r="5"/><path class="ac" d="M125 66v8M125 74l-7 9M125 74l7 9"/><rect class="ac" x="108" y="90" width="34" height="24" rx="3"/><path d="M20 126h150"/>',
  inspect:
    '<rect x="64" y="16" width="60" height="28" rx="6"/><path d="M94 8v8"/><path class="ac beam" d="M82 44L60 92M106 44l22 48"/><rect x="66" y="90" width="56" height="28" rx="4"/><path d="M94 90v28"/><path d="M24 118h140"/><circle class="ac" cx="148" cy="38" r="13"/><path class="ac" d="M142 38l4.5 4.6L154 34"/>',
  pack:
    '<rect x="76" y="20" width="44" height="24" rx="5"/><circle cx="98" cy="32" r="6"/><path d="M98 44v10"/><path class="ac" d="M62 54h72"/><rect x="62" y="54" width="72" height="46" rx="4"/><path d="M98 54v46"/><path d="M22 114h152"/><circle cx="46" cy="108" r="6"/><circle cx="72" cy="108" r="6"/><circle cx="98" cy="108" r="6"/><circle cx="124" cy="108" r="6"/><circle cx="150" cy="108" r="6"/>',
  ship:
    '<path d="M20 42h84v58H20z"/><path d="M104 60h30l18 22v18h-48z"/><path d="M112 66h18l10 12h-28z"/><circle cx="52" cy="110" r="11"/><circle cx="130" cy="110" r="11"/><path d="M8 110h30M66 110h50M144 110h48"/><path class="ac" d="M36 60h34M36 74h22"/>'
}

// 중앙 상자 안에서 바뀌는 아이콘
const boxIcons = {
  order:
    '<rect x="8" y="3" width="8" height="4" rx="1"/><path d="M16 5h2a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h2"/><path d="M8 12h8M8 16h5"/>',
  inbound:
    '<path d="M20 9v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V9"/><rect x="3" y="4" width="18" height="5" rx="1"/><path d="M12 11.5v6M9 14.5l3 3 3-3"/>',
  stock:
    '<polygon points="12 2 3 6.5 12 11 21 6.5 12 2"/><polyline points="3 16.5 12 21 21 16.5"/><polyline points="3 11.5 12 16 21 11.5"/>',
  plan:
    '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/><path d="M7.5 14h2M14.5 14h2M7.5 17.5h2M14.5 17.5h2"/>',
  produce:
    '<circle cx="12" cy="12" r="3.4"/><path d="M12 3v2.4M12 18.6V21M21 12h-2.4M5.4 12H3M18.4 5.6l-1.7 1.7M7.3 16.7l-1.7 1.7M18.4 18.4l-1.7-1.7M7.3 7.3L5.6 5.6"/>',
  inspect: '<circle cx="12" cy="12" r="9"/><path d="M8.3 12.3l2.6 2.6 4.8-5.2"/>',
  pack:
    '<rect x="3" y="7" width="18" height="13" rx="2"/><path d="M3 11h18M12 7v13"/><path d="M8 3.5L12 7l4-3.5"/>',
  ship:
    '<path d="M2.5 6h11v11h-11z"/><path d="M13.5 10h3.6l3.9 4v3h-7.5z"/><circle cx="7" cy="18.6" r="1.9"/><circle cx="17" cy="18.6" r="1.9"/>'
}

const SLAT = 40 // 벨트 슬랫 간격(px)

const rootEl = ref(null)
const active = ref(-1) // -1 = 히어로 (상자 등장 전)
const revealed = ref(new Set())
const beltY = ref(0)
const beltOpacity = ref(0)
const progress = ref(0)
const bumping = ref(false)

const current = computed(() => stages[Math.min(Math.max(active.value, 0), stages.length - 1)])

const clamp = (v, min, max) => Math.min(max, Math.max(min, v))

let raf = 0
let bumpTimer = 0
let ioActive = null
let ioReveal = null

function onScroll() {
  if (raf) return
  raf = requestAnimationFrame(() => {
    raf = 0
    const vh = window.innerHeight
    const y = window.scrollY
    const max = document.documentElement.scrollHeight - vh

    beltY.value = -(y % SLAT)
    progress.value = max > 0 ? clamp(y / max, 0, 1) : 0

    // 히어로에서 서서히 등장하고, 마지막 섹션에서 사라진다
    const fadeIn = clamp(y / (vh * 0.5), 0, 1)
    const fadeOut = clamp((max - y - vh * 0.1) / (vh * 0.45), 0, 1)
    beltOpacity.value = Math.min(fadeIn, fadeOut)
  })
}

function setActive(i) {
  if (active.value === i) return
  active.value = i
  if (i < 0) return
  bumping.value = false
  clearTimeout(bumpTimer)
  requestAnimationFrame(() => {
    bumping.value = true
    bumpTimer = setTimeout(() => (bumping.value = false), 600)
  })
}

onMounted(() => {
  const sections = rootEl.value.querySelectorAll('[data-idx]')

  // 화면 중앙 밴드에 걸린 섹션을 현재 공정으로
  ioActive = new IntersectionObserver(
    (entries) => {
      entries.forEach((e) => {
        if (e.isIntersecting) setActive(Number(e.target.dataset.idx))
      })
    },
    { rootMargin: '-48% 0px -48% 0px', threshold: 0 }
  )

  // 등장 애니메이션 (한 번만)
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
  onScroll()
})

onBeforeUnmount(() => {
  ioActive?.disconnect()
  ioReveal?.disconnect()
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', onScroll)
  if (raf) cancelAnimationFrame(raf)
  clearTimeout(bumpTimer)
})
</script>

<template>
  <div class="mes" ref="rootEl">
    <!-- 상단 진행 바 -->
    <div class="prog" :style="{ transform: 'scaleX(' + progress + ')' }" />

    <header class="topbar">
      <span class="brand">MES FLOW</span>
      <span class="counter" :style="{ opacity: beltOpacity }">
        {{ current.no }} <i>/</i> 08
      </span>
    </header>

    <!-- 고정 컨베이어 벨트 -->
    <div class="belt" :style="{ opacity: beltOpacity }" aria-hidden="true">
      <div class="rail rail-l"><div class="rollers" :style="{ transform: 'translateY(' + beltY + 'px)' }" /></div>
      <div class="surface">
        <div class="slats" :style="{ transform: 'translateY(' + beltY + 'px)' }" />
      </div>
      <div class="rail rail-r"><div class="rollers" :style="{ transform: 'translateY(' + beltY + 'px)' }" /></div>
    </div>

    <!-- 벨트 위 상자 (카메라는 항상 같은 거리에서 내려다본다) -->
    <div class="box-cam" :style="{ opacity: beltOpacity }" aria-hidden="true">
      <div class="box" :class="{ 'is-bump': bumping }">
        <span class="flap flap-t" />
        <span class="flap flap-b" />
        <span class="seam" />
        <Transition name="swap" mode="out-in">
          <svg
            class="box-icon"
            :key="current.key"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
            stroke-linejoin="round"
            v-html="boxIcons[current.key]"
          />
        </Transition>
      </div>
    </div>

    <!-- 히어로 -->
    <section class="hero" data-idx="-1">
      <p class="eyebrow">MANUFACTURING EXECUTION SYSTEM</p>
      <h1>수주에서 출하까지,<br />하나의 흐름으로</h1>
      <p class="hero-sub">
        원부자재가 8개의 공정을 지나 제품이 되기까지<br />
        MES가 기록하는 과정을 따라가 보세요.
      </p>
      <div class="scroll-cue"><span /></div>
    </section>

    <!-- 공정 -->
    <section
      v-for="(s, i) in stages"
      :key="s.key"
      class="stage"
      :class="{ flip: i % 2 === 1, 'is-in': revealed.has(i) }"
      :data-idx="i"
    >
      <div class="col col-art">
        <svg
          class="art"
          viewBox="0 0 200 140"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          v-html="art[s.key]"
        />
      </div>
      <div class="col-gap" />
      <div class="col col-txt">
        <p class="no">{{ s.no }}</p>
        <h2 class="title">{{ s.title }}</h2>
        <p class="desc">{{ s.desc }}</p>
        <ul class="tags">
          <li v-for="t in s.tags" :key="t">{{ t }}</li>
        </ul>
      </div>
    </section>

    <!-- 마무리 -->
    <section class="outro" data-idx="8">
      <h2>모든 공정이 데이터로 남습니다</h2>
      <p>
        어느 자재가, 어떤 설비를 거쳐, 누구의 손에서, 언제 출하되었는지<br />
        MES는 8개 공정의 기록을 하나의 흐름으로 연결합니다.
      </p>
      <router-link class="home" to="/">처음으로 돌아가기</router-link>
    </section>
  </div>
</template>

<style scoped>
.mes {
  --ex-bg: #f5f5f7;
  --ex-line: #e2e5ea;
  --ex-text: #111827;
  --ex-muted: #6b7280;
  --ex-accent: #2563eb;
  --belt-w: 260px;
  --box: 190px;

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
  transition: opacity 0.2s linear;
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
  background-image: repeating-linear-gradient(
    to bottom,
    #9aa2b1 0 10px,
    transparent 10px 40px
  );
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

/* ---- 벨트 위 상자 ---- */
.box-cam {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 5;
  pointer-events: none;
  transition: opacity 0.2s linear;
}
.box {
  position: relative;
  width: var(--box);
  height: var(--box);
  border-radius: 12px;
  background: #fff;
  border: 1px solid #dcdfe5;
  box-shadow: 0 26px 50px rgba(15, 23, 42, 0.28);
  display: grid;
  place-items: center;
  color: var(--ex-accent);
}
.box.is-bump {
  animation: settle 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}
@keyframes settle {
  0% {
    transform: scale(1.06) rotate(-0.6deg);
  }
  60% {
    transform: scale(0.985) rotate(0.3deg);
  }
  100% {
    transform: none;
  }
}
.flap {
  position: absolute;
  left: 10px;
  right: 10px;
  height: 1px;
  background: var(--ex-line);
}
.flap-t {
  top: 22%;
}
.flap-b {
  bottom: 22%;
}
.seam {
  position: absolute;
  top: 22%;
  bottom: 22%;
  left: 50%;
  width: 1px;
  background: var(--ex-line);
}
.box-icon {
  position: relative;
  width: 46%;
  height: 46%;
}
.swap-enter-active,
.swap-leave-active {
  transition: opacity 0.28s ease, transform 0.28s ease;
}
.swap-enter-from {
  opacity: 0;
  transform: translateY(8px) scale(0.94);
}
.swap-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.94);
}

/* ---- 히어로 ---- */
.hero {
  position: relative;
  z-index: 2;
  min-height: 100vh;
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
  color: var(--ex-accent);
}
.hero h1 {
  margin: 0;
  font-size: 46px;
  font-weight: 700;
  line-height: 1.32;
  letter-spacing: -0.03em;
}
.hero-sub {
  margin: 22px 0 0;
  font-size: 16px;
  line-height: 1.7;
  color: var(--ex-muted);
}
.scroll-cue {
  margin-top: 56px;
  width: 22px;
  height: 36px;
  border: 1.5px solid var(--ex-line);
  border-radius: 12px;
  display: grid;
  justify-items: center;
  padding-top: 7px;
}
.scroll-cue span {
  width: 3px;
  height: 7px;
  border-radius: 2px;
  background: var(--ex-accent);
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

/* ---- 공정 섹션 ---- */
.stage {
  position: relative;
  z-index: 2;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr var(--belt-w) 1fr;
  align-items: center;
}
.col-art {
  grid-column: 1;
  grid-row: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 44px;
  color: #9aa2b1;
  opacity: 0;
  transform: translateX(-28px);
  transition: opacity 0.8s ease, transform 0.8s cubic-bezier(0.22, 1, 0.36, 1);
}
.col-txt {
  grid-column: 3;
  grid-row: 1;
  padding: 0 44px;
  text-align: left;
  opacity: 0;
  transform: translateY(22px);
  transition: opacity 0.8s ease 0.08s, transform 0.8s cubic-bezier(0.22, 1, 0.36, 1) 0.08s;
}
.col-gap {
  grid-column: 2;
  grid-row: 1;
}
.flip .col-art {
  grid-column: 3;
  justify-content: flex-start;
  transform: translateX(28px);
}
.flip .col-txt {
  grid-column: 1;
  text-align: right;
}
.stage.is-in .col-art,
.stage.is-in .col-txt {
  opacity: 1;
  transform: none;
}
.art {
  width: 100%;
  max-width: 300px;
}
.art :deep(.ac) {
  stroke: var(--ex-accent);
}
.art :deep(.beam) {
  stroke-dasharray: 5 7;
  opacity: 0.7;
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
  background: #fff;
  font-size: 12px;
  font-weight: 600;
  color: var(--ex-muted);
}

/* ---- 마무리 ---- */
.outro {
  position: relative;
  z-index: 2;
  min-height: 78vh;
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
  background: #fff;
  font-size: 14px;
  font-weight: 600;
  transition: border-color 0.2s, color 0.2s;
}
.home:hover {
  border-color: var(--ex-accent);
  color: var(--ex-accent);
}

/* ---- 반응형: 벨트를 좌측 레일로 ---- */
@media (max-width: 900px) {
  .mes {
    --belt-w: 96px;
    --box: 72px;
  }
  .belt,
  .box-cam {
    left: 54px;
  }
  .rail {
    width: 12px;
  }
  .stage {
    display: flex;
    flex-direction: column;
    min-height: auto;
    padding: 84px 20px 84px 120px;
  }
  .col-txt {
    order: 1;
  }
  .col-art {
    order: 2;
  }
  .col-gap {
    display: none;
  }
  .col-art,
  .col-txt,
  .flip .col-art,
  .flip .col-txt {
    grid-column: auto;
    padding: 0;
    text-align: left;
    justify-content: flex-start;
    transform: translateY(20px);
  }
  .flip .desc {
    margin-left: 0;
  }
  .flip .tags {
    justify-content: flex-start;
  }
  .art {
    max-width: 200px;
    margin-top: 22px;
  }
  .hero,
  .outro {
    padding-left: 120px;
    padding-right: 20px;
    text-align: left;
    align-items: flex-start;
  }
  .hero h1 {
    font-size: 32px;
  }
  .title,
  .outro h2 {
    font-size: 24px;
  }
  .desc,
  .outro p {
    max-width: none;
  }
  .topbar {
    /* 좌측 벨트 레일과 겹치지 않도록 */
    padding: 16px 20px 16px 120px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mes *,
  .mes *::before,
  .mes *::after {
    animation: none !important;
    transition: none !important;
  }
  .col-art,
  .col-txt {
    opacity: 1 !important;
    transform: none !important;
  }
}
</style>
