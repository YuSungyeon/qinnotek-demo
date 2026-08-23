<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'

// 사원증 태그 로그인 — 현장에서 가장 흔한 인증 방식(RFID 태그)을 그대로 화면으로 옮긴 버전.
// 태그하면 사람이 아니라 "오늘 이 사람에게 배정된 일"이 먼저 뜬다.
const WORKER = {
  name: '김현우',
  empNo: '240117',
  team: '조립 1반',
  skills: 'CNC 운전 · 육안검사',
  initial: '김'
}
const TODAY = [
  { wo: 'WO-1042', item: 'BRK-220 브래킷', qty: '160 EA', machine: 'CNC-02' },
  { wo: 'WO-1043', item: 'BRK-220 브래킷', qty: '120 EA', machine: 'CNC-01' },
  { wo: 'WO-1039', item: 'HSG-118 하우징', qty: '40 EA', machine: 'ASSY-01' }
]
const TICKER = [
  'CNC-01 실적 +1 · WO-1042',
  'VIS-01 판정 불량 2 EA · 치수',
  'WH-A 자재 불출 4 EA · LOT A-2408-11',
  'ASSY-01 작업지시 착수 · WO-1039',
  'CNC-02 사이클타임 42s',
  '출하 DO-0088 상차 완료'
]

const step = ref('idle') // idle · reading · profile · started
const useKeypad = ref(false)
const manual = reactive({ empNo: '', pin: '' })
const err = ref('')
const clock = ref('')
let timer = 0

const totalQty = computed(() => TODAY.reduce((a, t) => a + parseInt(t.qty, 10), 0))

function tap() {
  if (step.value !== 'idle') return
  step.value = 'reading'
  setTimeout(() => (step.value = 'profile'), 1100)
}
function start() {
  step.value = 'started'
}
function reset() {
  step.value = 'idle'
  useKeypad.value = false
  manual.empNo = ''
  manual.pin = ''
  err.value = ''
}
function manualSubmit() {
  if (manual.empNo.length < 6 || manual.pin.length < 4) {
    err.value = '사번 6자리와 PIN 4자리를 입력하세요.'
    return
  }
  err.value = ''
  step.value = 'reading'
  setTimeout(() => (step.value = 'profile'), 800)
}

function tick() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  clock.value = p(d.getHours()) + ':' + p(d.getMinutes())
}
onMounted(() => {
  tick()
  timer = setInterval(tick, 10000)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <div class="badge">
    <!-- 배경: 계속 흐르는 라인 -->
    <div class="line" aria-hidden="true">
      <span v-for="n in 9" :key="n" class="pkg" :style="{ animationDelay: n * -2.1 + 's' }" />
    </div>

    <header class="top">
      <div class="place">
        <span class="pulse" />
        1공장 · 조립 라인 A
      </div>
      <div class="now">
        <span class="shift">야간 교대</span>
        <b>{{ clock }}</b>
      </div>
    </header>

    <main class="stage">
      <Transition name="step" mode="out-in">
        <!-- 대기 / 태그 -->
        <section v-if="step === 'idle'" key="idle" class="card">
          <p class="eyebrow">근무 인증</p>
          <h1>사원증을 태그하세요</h1>
          <p class="sub">태그하면 오늘 배정된 작업지시가 이 단말기에 연결됩니다.</p>

          <button class="reader" @click="tap">
            <span class="ring r1" />
            <span class="ring r2" />
            <span class="ring r3" />
            <span class="nfc">
              <i /><i /><i />
            </span>
          </button>

          <p class="tapmsg">리더기에 카드를 가까이</p>

          <div class="alt">
            <button class="link" @click="useKeypad = !useKeypad">
              사원증이 없으신가요? 사번으로 로그인
            </button>
            <div v-if="useKeypad" class="manual">
              <input v-model="manual.empNo" maxlength="6" inputmode="numeric" placeholder="사번 6자리" />
              <input
                v-model="manual.pin"
                maxlength="4"
                type="password"
                inputmode="numeric"
                placeholder="PIN"
              />
              <button class="go" @click="manualSubmit">확인</button>
            </div>
            <p v-if="err" class="err">{{ err }}</p>
          </div>
        </section>

        <!-- 인식 중 -->
        <section v-else-if="step === 'reading'" key="reading" class="card center">
          <div class="scanner">
            <span class="sweep" />
          </div>
          <h1>사원증 확인 중</h1>
          <p class="sub">근무 자격과 오늘 배정을 불러오고 있습니다.</p>
        </section>

        <!-- 오늘 배정 -->
        <section v-else-if="step === 'profile'" key="profile" class="card wide">
          <div class="who">
            <span class="ava">{{ WORKER.initial }}</span>
            <div>
              <b>{{ WORKER.name }}</b>
              <span>{{ WORKER.empNo }} · {{ WORKER.team }}</span>
            </div>
            <span class="ok-chip">자격 확인</span>
          </div>

          <p class="skills">{{ WORKER.skills }}</p>

          <div class="assign">
            <p class="assign-head">
              오늘 배정 <b>{{ TODAY.length }}건</b>
              <em>목표 {{ totalQty }} EA</em>
            </p>
            <ul>
              <li v-for="t in TODAY" :key="t.wo">
                <b>{{ t.wo }}</b>
                <span>{{ t.item }}</span>
                <em>{{ t.machine }}</em>
                <i>{{ t.qty }}</i>
              </li>
            </ul>
          </div>

          <div class="acts">
            <button class="ghost" @click="reset">취소</button>
            <button class="go big" @click="start">작업 시작</button>
          </div>
        </section>

        <!-- 시작됨 -->
        <section v-else key="started" class="card center">
          <div class="done-mark">✓</div>
          <h1>작업이 시작되었습니다</h1>
          <p class="sub">
            지금부터 이 라인의 실적·불량·가동 기록이<br />
            {{ WORKER.name }}({{ WORKER.empNo }})으로 남습니다.
          </p>
          <div class="live">
            <span class="pulse" /> 실적 수집 중 · WO-1042
          </div>
          <button class="ghost" @click="reset">퇴근 / 로그아웃</button>
        </section>
      </Transition>
    </main>

    <footer class="ticker">
      <div class="track">
        <span v-for="(t, i) in TICKER.concat(TICKER)" :key="i">{{ t }}</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.badge {
  --bg: #070d18;
  --panel: rgba(20, 32, 54, 0.72);
  --line: rgba(120, 155, 205, 0.18);
  --text: #eaf0fa;
  --muted: #8ea1bf;
  --accent: #59a2ff;
  --ok: #34d399;

  position: relative;
  min-height: 100vh;
  background: radial-gradient(1000px 560px at 50% 0%, #14243d 0%, var(--bg) 62%);
  color: var(--text);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  letter-spacing: -0.01em;
}

/* 배경 라인 */
.line {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 92px;
  margin-top: -46px;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
  background: repeating-linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.045) 0 2px,
    transparent 2px 46px
  );
  opacity: 0.55;
}
.pkg {
  position: absolute;
  top: 50%;
  width: 34px;
  height: 34px;
  margin-top: -17px;
  border-radius: 6px;
  background: rgba(120, 155, 205, 0.16);
  border: 1px solid var(--line);
  animation: run 19s linear infinite;
}
@keyframes run {
  from {
    left: -6%;
  }
  to {
    left: 106%;
  }
}

.top,
.stage,
.ticker {
  position: relative;
  z-index: 1;
}
.top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  font-size: 13px;
  color: var(--muted);
}
.place {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}
.pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--ok);
  box-shadow: 0 0 0 0 rgba(52, 211, 153, 0.5);
  animation: pulse 2s ease-out infinite;
}
@keyframes pulse {
  to {
    box-shadow: 0 0 0 10px rgba(52, 211, 153, 0);
  }
}
.now {
  display: flex;
  align-items: center;
  gap: 14px;
}
.shift {
  padding: 4px 11px;
  border: 1px solid var(--line);
  border-radius: 999px;
  font-weight: 700;
  color: var(--text);
}
.now b {
  font-size: 19px;
  font-variant-numeric: tabular-nums;
}

.stage {
  flex: 1;
  display: grid;
  place-items: center;
  padding: 20px;
}
.card {
  width: min(440px, 92vw);
  padding: 34px 32px 30px;
  border: 1px solid var(--line);
  border-radius: 24px;
  background: var(--panel);
  backdrop-filter: blur(18px);
  box-shadow: 0 40px 90px rgba(2, 8, 20, 0.55);
  text-align: center;
}
.card.wide {
  width: min(520px, 94vw);
  text-align: left;
}
.card.center {
  display: grid;
  justify-items: center;
}
.eyebrow {
  margin: 0 0 10px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.2em;
  color: var(--accent);
}
.card h1 {
  margin: 0;
  font-size: 25px;
  font-weight: 700;
  letter-spacing: -0.03em;
}
.sub {
  margin: 10px 0 0;
  font-size: 13.5px;
  line-height: 1.65;
  color: var(--muted);
}

/* 리더기 */
.reader {
  position: relative;
  width: 168px;
  height: 168px;
  margin: 30px auto 14px;
  border: 0;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 45%, #1d3355 0%, #16253f 70%);
  display: grid;
  place-items: center;
  cursor: pointer;
}
.reader::after {
  content: '';
  position: absolute;
  inset: 22px;
  border-radius: 50%;
  border: 1px solid var(--line);
  background: rgba(89, 162, 255, 0.06);
}
.ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1.5px solid rgba(89, 162, 255, 0.5);
  animation: ripple 2.6s ease-out infinite;
}
.r2 {
  animation-delay: 0.85s;
}
.r3 {
  animation-delay: 1.7s;
}
@keyframes ripple {
  0% {
    transform: scale(0.55);
    opacity: 0;
  }
  25% {
    opacity: 0.8;
  }
  100% {
    transform: scale(1.15);
    opacity: 0;
  }
}
.nfc {
  position: relative;
  display: grid;
  gap: 5px;
  justify-items: center;
  z-index: 1;
}
.nfc i {
  display: block;
  border: 2.5px solid var(--accent);
  border-bottom-color: transparent;
  border-left-color: transparent;
  border-right-color: transparent;
  border-radius: 50%;
}
.nfc i:nth-child(1) {
  width: 18px;
  height: 18px;
}
.nfc i:nth-child(2) {
  width: 34px;
  height: 34px;
  margin-top: -14px;
}
.nfc i:nth-child(3) {
  width: 50px;
  height: 50px;
  margin-top: -22px;
  opacity: 0.55;
}
.tapmsg {
  margin: 0;
  font-size: 12.5px;
  color: var(--muted);
}

.alt {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px dashed var(--line);
}
.link {
  border: 0;
  background: transparent;
  color: var(--accent);
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
}
.manual {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1.3fr 1fr auto;
  gap: 8px;
}
.manual input {
  padding: 11px 12px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: rgba(8, 15, 28, 0.7);
  color: var(--text);
  font-size: 14px;
  font-family: inherit;
  outline: none;
}
.manual input:focus {
  border-color: var(--accent);
}
.manual input::placeholder {
  color: #64789a;
}
.err {
  margin: 10px 0 0;
  font-size: 12.5px;
  color: #f88;
}

/* 인식 중 */
.scanner {
  position: relative;
  width: 132px;
  height: 132px;
  margin: 6px auto 22px;
  border-radius: 24px;
  border: 1px solid var(--line);
  background: rgba(89, 162, 255, 0.07);
  overflow: hidden;
}
.sweep {
  position: absolute;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, transparent, var(--accent), transparent);
  box-shadow: 0 0 18px rgba(89, 162, 255, 0.8);
  animation: sweep 1.1s ease-in-out infinite;
}
@keyframes sweep {
  0% {
    top: 6%;
  }
  50% {
    top: 92%;
  }
  100% {
    top: 6%;
  }
}

/* 프로필 */
.who {
  display: flex;
  align-items: center;
  gap: 14px;
}
.ava {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #3f7fe0, #2c5fb5);
  font-size: 18px;
  font-weight: 700;
}
.who b {
  display: block;
  font-size: 19px;
}
.who span {
  font-size: 12.5px;
  color: var(--muted);
}
.ok-chip {
  margin-left: auto;
  padding: 5px 11px;
  border-radius: 999px;
  background: rgba(52, 211, 153, 0.14);
  color: var(--ok);
  font-size: 11.5px;
  font-weight: 700;
}
.skills {
  margin: 14px 0 0;
  font-size: 12.5px;
  color: var(--muted);
}

.assign {
  margin-top: 20px;
  border: 1px solid var(--line);
  border-radius: 16px;
  overflow: hidden;
}
.assign-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 0;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
  font-size: 12.5px;
  color: var(--muted);
}
.assign-head b {
  color: var(--text);
}
.assign-head em {
  margin-left: auto;
  font-style: normal;
  font-variant-numeric: tabular-nums;
}
.assign ul {
  list-style: none;
  margin: 0;
  padding: 4px 0;
}
.assign li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 16px;
  font-size: 13px;
}
.assign li b {
  color: var(--accent);
  font-size: 12.5px;
}
.assign li span {
  color: var(--text);
}
.assign li em {
  margin-left: auto;
  font-style: normal;
  font-size: 12px;
  color: var(--muted);
}
.assign li i {
  font-style: normal;
  font-size: 12.5px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  min-width: 54px;
  text-align: right;
}

.acts {
  margin-top: 22px;
  display: flex;
  gap: 10px;
}
.ghost {
  padding: 12px 18px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: transparent;
  color: var(--muted);
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
}
.ghost:hover {
  color: var(--text);
}
.go {
  padding: 12px 18px;
  border: 0;
  border-radius: 12px;
  background: var(--accent);
  color: #06101f;
  font-size: 14px;
  font-weight: 800;
  font-family: inherit;
  cursor: pointer;
}
.go:hover {
  background: #74b2ff;
}
.go.big {
  flex: 1;
}

.done-mark {
  width: 58px;
  height: 58px;
  margin-bottom: 18px;
  border-radius: 50%;
  background: rgba(52, 211, 153, 0.16);
  color: var(--ok);
  font-size: 27px;
  display: grid;
  place-items: center;
}
.live {
  margin: 22px 0 20px;
  padding: 9px 16px;
  display: flex;
  align-items: center;
  gap: 9px;
  border: 1px solid var(--line);
  border-radius: 999px;
  font-size: 12.5px;
  color: var(--muted);
}

/* 하단 티커 */
.ticker {
  border-top: 1px solid var(--line);
  overflow: hidden;
  padding: 11px 0;
  background: rgba(7, 13, 24, 0.6);
}
.track {
  display: flex;
  gap: 46px;
  white-space: nowrap;
  animation: slide 34s linear infinite;
  font-size: 12px;
  color: var(--muted);
}
.track span::before {
  content: '·';
  margin-right: 14px;
  color: var(--accent);
}
@keyframes slide {
  to {
    transform: translateX(-50%);
  }
}

.step-enter-active,
.step-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.step-enter-from {
  opacity: 0;
  transform: translateY(12px) scale(0.98);
}
.step-leave-to {
  opacity: 0;
  transform: translateY(-12px) scale(0.98);
}

@media (max-width: 640px) {
  .card {
    padding: 26px 22px 24px;
  }
  .reader {
    width: 138px;
    height: 138px;
  }
  .manual {
    grid-template-columns: 1fr 1fr;
  }
  .manual .go {
    grid-column: span 2;
  }
}
</style>
