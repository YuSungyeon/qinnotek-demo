<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'

// 현장 단말기(HMI) 로그인 — 로그인 행위 자체가 "작업 시작"인 화면.
// 사번+PIN으로 로그인하면 교대조·작업장·설비가 배정되고 그 순간부터 실적이 집계된다.
const WORKCENTERS = [
  { id: 'ASSY-1', name: '조립 1반', machines: 'ASSY-01 · ASSY-02' },
  { id: 'MACH-2', name: '가공 2반', machines: 'CNC-01 · CNC-02' },
  { id: 'QC-1', name: '검사반', machines: 'VIS-01' }
]
const SHIFTS = [
  { id: 'day', name: '주간', time: '08:00 – 20:00' },
  { id: 'night', name: '야간', time: '20:00 – 08:00' }
]
const MACHINES = [
  { id: 'CNC-01', state: 'run', rate: 94, order: 'WO-1042' },
  { id: 'CNC-02', state: 'run', rate: 88, order: 'WO-1043' },
  { id: 'ASSY-01', state: 'idle', rate: 61, order: 'WO-1039' },
  { id: 'VIS-01', state: 'down', rate: 0, order: '—' }
]
const STATE_LABEL = { run: '가동', idle: '대기', down: '정지' }

const EVENTS = [
  ['CNC-01', '실적 등록 +1 (WO-1042)'],
  ['VIS-01', '검사 판정 불량 · 치수'],
  ['ASSY-01', '작업지시 WO-1039 착수'],
  ['CNC-02', '실적 등록 +1 (WO-1043)'],
  ['WH-A', '자재 불출 4 EA · LOT A-2408-11'],
  ['CNC-01', '사이클타임 42s'],
  ['VIS-01', '설비 정지 · 점검 요청'],
  ['ASSY-01', '실적 등록 +2 (WO-1039)']
]

const form = reactive({ empNo: '', pin: '', center: 'ASSY-1', shift: 'day' })
const error = ref('')
const loading = ref(false)
const done = ref(false)

const clock = ref('')
const target = 320
const output = ref(214)
const log = ref([])
let timer = 0
let evtTimer = 0
let evtIdx = 0

const pinFilled = computed(() => form.pin.length)
const progress = computed(() => Math.min(100, Math.round((output.value / target) * 100)))
const center = computed(() => WORKCENTERS.find((w) => w.id === form.center))
const shift = computed(() => SHIFTS.find((s) => s.id === form.shift))

function tick() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  clock.value = p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds())
}

function pushEvent() {
  const e = EVENTS[evtIdx % EVENTS.length]
  evtIdx += 1
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  log.value = [{ id: evtIdx, at: p(d.getHours()) + ':' + p(d.getMinutes()), src: e[0], msg: e[1] }]
    .concat(log.value)
    .slice(0, 6)
  if (e[1].startsWith('실적')) output.value += 1
}

function key(k) {
  error.value = ''
  if (k === 'del') {
    if (form.pin) form.pin = form.pin.slice(0, -1)
    else form.empNo = form.empNo.slice(0, -1)
    return
  }
  if (form.empNo.length < 6) form.empNo += k
  else if (form.pin.length < 4) form.pin += k
}

function submit() {
  if (form.empNo.length < 6) {
    error.value = '사번 6자리를 입력하세요.'
    return
  }
  if (form.pin.length < 4) {
    error.value = 'PIN 4자리를 입력하세요.'
    return
  }
  loading.value = true
  setTimeout(() => {
    loading.value = false
    done.value = true
  }, 900)
}

function reset() {
  done.value = false
  form.empNo = ''
  form.pin = ''
  error.value = ''
}

onMounted(() => {
  tick()
  timer = setInterval(tick, 1000)
  pushEvent()
  evtTimer = setInterval(pushEvent, 2600)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  clearInterval(evtTimer)
})
</script>

<template>
  <div class="term">
    <header class="bar">
      <div class="who">
        <span class="dot" />
        <b>QINNOTEK</b> 1공장 · 조립 라인 A
      </div>
      <div class="meta">
        <span class="shift">{{ shift.name }} 교대</span>
        <span class="clock">{{ clock }}</span>
      </div>
    </header>

    <main class="grid">
      <!-- 라인 현황 -->
      <section class="status">
        <div class="kpi">
          <div class="kpi-head">
            <span>오늘 생산 실적</span>
            <b>{{ output }} <i>/ {{ target }} EA</i></b>
          </div>
          <div class="bar-track"><div class="bar-fill" :style="{ width: progress + '%' }" /></div>
          <div class="kpi-foot">
            <span>달성률 {{ progress }}%</span>
            <span>불량 6 EA · 1.9%</span>
          </div>
        </div>

        <ul class="machines">
          <li v-for="m in MACHINES" :key="m.id" :class="m.state">
            <div class="m-top">
              <b>{{ m.id }}</b>
              <span class="tag">{{ STATE_LABEL[m.state] }}</span>
            </div>
            <div class="gauge"><i :style="{ width: m.rate + '%' }" /></div>
            <div class="m-foot">
              <span>{{ m.order }}</span>
              <span>{{ m.rate }}%</span>
            </div>
          </li>
        </ul>

        <div class="log">
          <p class="log-head">실시간 수집 로그</p>
          <TransitionGroup name="log" tag="ul">
            <li v-for="l in log" :key="l.id">
              <span class="at">{{ l.at }}</span>
              <span class="src">{{ l.src }}</span>
              <span class="msg">{{ l.msg }}</span>
            </li>
          </TransitionGroup>
        </div>
      </section>

      <!-- 로그인 = 작업 시작 -->
      <section class="panel">
        <Transition name="swap" mode="out-in">
          <div v-if="!done" key="form" class="card">
            <p class="eyebrow">작업 시작</p>
            <h1>사번으로 로그인</h1>
            <p class="sub">로그인하면 교대조와 작업장이 배정되고 실적 집계가 시작됩니다.</p>

            <div class="fields">
              <div class="field" :class="{ on: !form.pin }">
                <label>사번</label>
                <div class="value">{{ form.empNo || '——————' }}</div>
              </div>
              <div class="field" :class="{ on: form.empNo.length === 6 }">
                <label>PIN</label>
                <div class="value pin">
                  <i v-for="n in 4" :key="n" :class="{ f: n <= pinFilled }" />
                </div>
              </div>
            </div>

            <div class="keypad">
              <button v-for="n in 9" :key="n" @click="key(String(n))">{{ n }}</button>
              <button class="ghost" @click="key('del')">지움</button>
              <button @click="key('0')">0</button>
              <button class="go" :disabled="loading" @click="submit">
                <span v-if="!loading">확인</span>
                <span v-else class="spin" />
              </button>
            </div>

            <div class="picks">
              <div class="pick">
                <label>작업장</label>
                <div class="chips">
                  <button
                    v-for="w in WORKCENTERS"
                    :key="w.id"
                    :class="{ sel: form.center === w.id }"
                    @click="form.center = w.id"
                  >
                    {{ w.name }}
                  </button>
                </div>
              </div>
              <div class="pick">
                <label>교대조</label>
                <div class="chips">
                  <button
                    v-for="s in SHIFTS"
                    :key="s.id"
                    :class="{ sel: form.shift === s.id }"
                    @click="form.shift = s.id"
                  >
                    {{ s.name }}
                  </button>
                </div>
              </div>
            </div>

            <p v-if="error" class="err">{{ error }}</p>
            <p v-else class="hint">사원증 태그 또는 사번 6자리 + PIN 4자리</p>
          </div>

          <div v-else key="done" class="card done">
            <div class="check">✓</div>
            <h1>{{ form.empNo }} 님, 작업을 시작합니다</h1>
            <dl class="assign">
              <div><dt>작업장</dt><dd>{{ center.name }}</dd></div>
              <div><dt>배정 설비</dt><dd>{{ center.machines }}</dd></div>
              <div><dt>교대조</dt><dd>{{ shift.name }} · {{ shift.time }}</dd></div>
              <div><dt>지시 건수</dt><dd>3건 · 목표 320 EA</dd></div>
            </dl>
            <p class="sub">지금부터 이 단말기의 모든 실적이 사번 {{ form.empNo }}로 기록됩니다.</p>
            <button class="out" @click="reset">로그아웃</button>
          </div>
        </Transition>
      </section>
    </main>
  </div>
</template>

<style scoped>
.term {
  --bg: #0a1120;
  --panel: #111c30;
  --line: #1f2f4a;
  --text: #e8eef7;
  --muted: #8497b3;
  --accent: #4c8dff;
  --run: #34d399;
  --idle: #fbbf24;
  --down: #f87171;

  min-height: 100vh;
  background: radial-gradient(1200px 600px at 20% -10%, #16243c 0%, var(--bg) 60%);
  color: var(--text);
  display: flex;
  flex-direction: column;
  letter-spacing: -0.01em;
}

.bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 26px;
  border-bottom: 1px solid var(--line);
  font-size: 13px;
  color: var(--muted);
}
.who b {
  color: var(--text);
  letter-spacing: 0.06em;
  margin-right: 6px;
}
.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--run);
  margin-right: 9px;
  box-shadow: 0 0 0 4px rgba(52, 211, 153, 0.15);
}
.meta {
  display: flex;
  align-items: center;
  gap: 16px;
}
.shift {
  padding: 4px 11px;
  border: 1px solid var(--line);
  border-radius: 999px;
  font-weight: 700;
  color: var(--text);
}
.clock {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  font-variant-numeric: tabular-nums;
}

.grid {
  flex: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(380px, 0.85fr);
  gap: 26px;
  padding: 26px;
}

/* ---- 라인 현황 ---- */
.status {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}
.kpi {
  padding: 20px 22px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--panel);
}
.kpi-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 13px;
  color: var(--muted);
}
.kpi-head b {
  font-size: 30px;
  color: var(--text);
  font-variant-numeric: tabular-nums;
}
.kpi-head i {
  font-style: normal;
  font-size: 14px;
  color: var(--muted);
  font-weight: 600;
}
.bar-track {
  margin: 14px 0 9px;
  height: 8px;
  border-radius: 999px;
  background: #0c1526;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent), #7cb0ff);
  transition: width 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}
.kpi-foot {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  color: var(--muted);
}

.machines {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}
.machines li {
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--panel);
}
.m-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}
.tag {
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}
.run .tag {
  background: rgba(52, 211, 153, 0.16);
  color: var(--run);
}
.idle .tag {
  background: rgba(251, 191, 36, 0.16);
  color: var(--idle);
}
.down .tag {
  background: rgba(248, 113, 113, 0.16);
  color: var(--down);
}
.gauge {
  margin: 12px 0 8px;
  height: 5px;
  border-radius: 999px;
  background: #0c1526;
  overflow: hidden;
}
.gauge i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--muted);
}
.run .gauge i {
  background: var(--run);
}
.idle .gauge i {
  background: var(--idle);
}
.down .gauge i {
  background: var(--down);
}
.m-foot {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}

.log {
  flex: 1;
  padding: 16px 18px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--panel);
  min-height: 0;
}
.log-head {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--muted);
}
.log ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 7px;
}
.log li {
  display: flex;
  gap: 12px;
  font-size: 12.5px;
  color: var(--muted);
}
.log .at {
  font-variant-numeric: tabular-nums;
}
.log .src {
  min-width: 62px;
  color: var(--accent);
  font-weight: 700;
}
.log .msg {
  color: var(--text);
}
.log-enter-active {
  transition: opacity 0.4s ease, transform 0.4s ease;
}
.log-enter-from {
  opacity: 0;
  transform: translateY(-6px);
}
.log-move {
  transition: transform 0.4s ease;
}

/* ---- 로그인 카드 ---- */
.panel {
  display: flex;
  align-items: stretch;
  min-width: 0;
}
.card {
  flex: 1;
  padding: 28px 28px 24px;
  border: 1px solid var(--line);
  border-radius: 20px;
  background: linear-gradient(180deg, #16243c, #101a2c);
  box-shadow: 0 30px 60px rgba(2, 8, 20, 0.45);
  display: flex;
  flex-direction: column;
}
.eyebrow {
  margin: 0 0 8px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.18em;
  color: var(--accent);
}
.card h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.03em;
}
.sub {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--muted);
}

.fields {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 10px;
  margin: 22px 0 16px;
}
.field {
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #0c1526;
}
.field.on {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(76, 141, 255, 0.14);
}
.field label {
  display: block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--muted);
}
.value {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.16em;
  font-variant-numeric: tabular-nums;
  min-height: 28px;
}
.value.pin {
  display: flex;
  gap: 10px;
  align-items: center;
  height: 28px;
}
.value.pin i {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid var(--line);
}
.value.pin i.f {
  background: var(--accent);
  border-color: var(--accent);
}

.keypad {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 9px;
}
.keypad button {
  padding: 15px 0;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #0e1a2e;
  color: var(--text);
  font-size: 19px;
  font-weight: 700;
  font-family: inherit;
  transition: background 0.15s, border-color 0.15s, transform 0.05s;
}
.keypad button:hover {
  background: #16243c;
  border-color: #2b4066;
}
.keypad button:active {
  transform: translateY(1px);
}
.keypad .ghost {
  font-size: 14px;
  color: var(--muted);
}
.keypad .go {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
  font-size: 16px;
}
.keypad .go:hover {
  background: #3d7ef0;
}
.spin {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.45);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.picks {
  margin-top: 18px;
  display: grid;
  gap: 12px;
}
.pick label {
  display: block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--muted);
  margin-bottom: 7px;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}
.chips button {
  padding: 7px 13px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  color: var(--muted);
  font-size: 12.5px;
  font-weight: 600;
  font-family: inherit;
  transition: all 0.15s;
}
.chips button.sel {
  border-color: var(--accent);
  background: rgba(76, 141, 255, 0.14);
  color: #cfe0ff;
}

.err,
.hint {
  margin: 16px 0 0;
  font-size: 12.5px;
  text-align: center;
}
.hint {
  color: var(--muted);
}
.err {
  color: var(--down);
  font-weight: 600;
}

/* ---- 로그인 완료 ---- */
.card.done {
  justify-content: center;
  text-align: center;
}
.check {
  width: 56px;
  height: 56px;
  margin: 0 auto 18px;
  border-radius: 50%;
  background: rgba(52, 211, 153, 0.16);
  color: var(--run);
  font-size: 28px;
  display: grid;
  place-items: center;
}
.assign {
  margin: 22px 0 0;
  display: grid;
  gap: 1px;
  border: 1px solid var(--line);
  border-radius: 14px;
  overflow: hidden;
  background: var(--line);
  text-align: left;
}
.assign > div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  background: #0e1a2e;
  font-size: 13.5px;
}
.assign dt {
  color: var(--muted);
}
.assign dd {
  margin: 0;
  font-weight: 700;
}
.out {
  margin-top: 22px;
  padding: 11px 20px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  color: var(--muted);
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
}
.out:hover {
  color: var(--text);
  border-color: #2b4066;
}

.swap-enter-active,
.swap-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.swap-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.swap-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@media (max-width: 980px) {
  .grid {
    grid-template-columns: 1fr;
    padding: 18px;
    gap: 18px;
  }
  .clock {
    font-size: 17px;
  }
  .card {
    padding: 22px 20px 20px;
  }
}
</style>
