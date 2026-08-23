<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'

// MES SaaS 로그인 — "도입하면 무엇이 어떻게 바뀌는가"를 로그인 옆에서 보여준다.
const ROWS = [
  {
    label: '실적 집계',
    before: '작업일보를 손으로 적고 다음 날 취합합니다.',
    after: '설비가 올린 실적이 그 자리에서 반영됩니다.'
  },
  {
    label: '재고 파악',
    before: '월말 재고조사로 장부를 맞춰봐야 압니다.',
    after: '입고·불출 즉시 차감되어 항상 현재고입니다.'
  },
  {
    label: '불량 관리',
    before: '누가 언제 만든 것인지 기억에 의존합니다.',
    after: '설비·작업자·시간대를 교차해 원인을 좁힙니다.'
  },
  {
    label: '이력 추적',
    before: '출하 뒤 문제가 생기면 되짚기 어렵습니다.',
    after: 'LOT 하나로 입고부터 출하까지 이어서 봅니다.'
  }
]
const METRICS = [
  { label: '실적 반영 지연', before: 24, after: 0, unit: 'h', afterText: '실시간' },
  { label: '재고 정확도', before: 82, after: 99, unit: '%' },
  { label: '불량 원인 규명률', before: 30, after: 87, unit: '%' }
]

const after = ref(true)
const shown = reactive(METRICS.map((m) => m.after))
let tween = 0

watch(after, (v) => {
  clearInterval(tween)
  const from = shown.slice()
  const to = METRICS.map((m) => (v ? m.after : m.before))
  let t = 0
  tween = setInterval(() => {
    t += 0.08
    const k = t >= 1 ? 1 : 1 - Math.pow(1 - t, 3)
    to.forEach((val, i) => (shown[i] = Math.round(from[i] + (val - from[i]) * k)))
    if (t >= 1) clearInterval(tween)
  }, 16)
})
onBeforeUnmount(() => clearInterval(tween))

// ---- 로그인 ----
const form = reactive({ tenant: '', email: '', password: '', keep: true })
const touched = reactive({ tenant: false, email: false, password: false })
const show = ref(false)
const loading = ref(false)
const done = ref(false)
const fail = ref('')

const emailOk = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(form.email))
const tenantOk = computed(() => form.tenant.trim().length >= 2)
const pwOk = computed(() => form.password.length >= 8)

function submit() {
  touched.tenant = touched.email = touched.password = true
  fail.value = ''
  if (!(emailOk.value && tenantOk.value && pwOk.value)) return
  loading.value = true
  setTimeout(() => {
    loading.value = false
    if (form.password.toLowerCase() === 'password') {
      fail.value = '비밀번호가 올바르지 않습니다. 5회 실패 시 계정이 잠깁니다.'
      return
    }
    done.value = true
  }, 850)
}
</script>

<template>
  <div class="page">
    <header class="nav">
      <div class="logo"><span class="mark" /> QINNOTEK <b>MES</b></div>
      <nav>
        <a href="#" @click.prevent>제품 소개</a>
        <a href="#" @click.prevent>도입 사례</a>
        <a href="#" class="cta" @click.prevent>도입 문의</a>
      </nav>
    </header>

    <main class="wrap">
      <!-- 무엇이 어떻게 바뀌는가 -->
      <section class="change">
        <p class="kicker">MES 클라우드</p>
        <h1>같은 현장, 달라지는 것은<br />기록이 남는 방식입니다</h1>

        <div class="toggle" role="tablist">
          <button :class="{ on: !after }" @click="after = false">도입 전</button>
          <button :class="{ on: after }" @click="after = true">MES 도입 후</button>
        </div>

        <ul class="rows">
          <li v-for="r in ROWS" :key="r.label" :class="{ after }">
            <span class="rl">{{ r.label }}</span>
            <span class="rt" :key="after ? 'a' : 'b'">{{ after ? r.after : r.before }}</span>
          </li>
        </ul>

        <div class="metrics">
          <div v-for="(m, i) in METRICS" :key="m.label" :class="{ after }">
            <b>
              <template v-if="after && m.afterText">{{ m.afterText }}</template>
              <template v-else>{{ shown[i] }}<i>{{ m.unit }}</i></template>
            </b>
            <span>{{ m.label }}</span>
          </div>
        </div>

        <p class="note">
          숫자는 국내 중소 제조 30개사 도입 6개월 시점 평균입니다.
        </p>
      </section>

      <!-- 로그인 -->
      <section class="login">
        <Transition name="fade" mode="out-in">
          <form v-if="!done" key="f" @submit.prevent="submit">
            <h2>로그인</h2>
            <p class="sub">회사 계정으로 MES 콘솔에 접속합니다.</p>

            <label>
              회사 코드
              <span class="wrapin">
                <input
                  v-model="form.tenant"
                  :class="{ bad: touched.tenant && !tenantOk }"
                  placeholder="qinnotek"
                  autocomplete="organization"
                  @blur="touched.tenant = true"
                />
                <em>.mes.cloud</em>
              </span>
            </label>
            <p v-if="touched.tenant && !tenantOk" class="msg">회사 코드를 입력하세요.</p>

            <label>
              이메일
              <input
                v-model="form.email"
                :class="{ bad: touched.email && !emailOk }"
                type="email"
                placeholder="name@company.com"
                autocomplete="username"
                @blur="touched.email = true"
              />
            </label>
            <p v-if="touched.email && !emailOk" class="msg">올바른 이메일 주소를 입력하세요.</p>

            <label>
              비밀번호
              <span class="wrapin">
                <input
                  v-model="form.password"
                  :class="{ bad: touched.password && !pwOk }"
                  :type="show ? 'text' : 'password'"
                  placeholder="8자 이상"
                  autocomplete="current-password"
                  @blur="touched.password = true"
                />
                <button type="button" class="eye" @click="show = !show">
                  {{ show ? '숨기기' : '보기' }}
                </button>
              </span>
            </label>
            <p v-if="touched.password && !pwOk" class="msg">비밀번호는 8자 이상입니다.</p>

            <div class="row">
              <label class="chk">
                <input v-model="form.keep" type="checkbox" /><span>로그인 상태 유지</span>
              </label>
              <a href="#" @click.prevent>비밀번호 찾기</a>
            </div>

            <p v-if="fail" class="fail">{{ fail }}</p>

            <button class="primary" :disabled="loading">
              <span v-if="!loading">로그인</span><span v-else class="spin" />
            </button>

            <div class="or"><span>또는</span></div>
            <div class="sso">
              <button type="button"><span class="ms"><i /><i /><i /><i /></span>Microsoft</button>
              <button type="button"><span class="gg">G</span>Google</button>
            </div>

            <p class="foot">처음이신가요? <a href="#" @click.prevent>30일 무료 체험</a></p>
          </form>

          <div v-else key="ok" class="ok">
            <div class="ok-mark">✓</div>
            <h2>{{ form.tenant || 'qinnotek' }} 콘솔로 이동합니다</h2>
            <p class="sub">{{ form.email }} 으로 로그인했습니다.</p>
            <button class="primary" @click="done = false">다른 계정으로 로그인</button>
          </div>
        </Transition>
      </section>
    </main>

    <footer class="legal">
      <span>ISO 27001</span><span>서울 리전 (ap-northeast-2)</span><span>SLA 99.9%</span>
    </footer>
  </div>
</template>

<style scoped>
.page {
  --ink: #10192b;
  --muted: #6b7a90;
  --line: #e4e8ee;
  --brand: #2563eb;
  --ok: #16a34a;

  min-height: 100vh;
  background: #f7f8fa;
  color: var(--ink);
  display: flex;
  flex-direction: column;
  letter-spacing: -0.01em;
}

.nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 40px;
  border-bottom: 1px solid var(--line);
  background: #fff;
}
.logo {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.14em;
}
.logo b {
  color: var(--brand);
}
.mark {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: linear-gradient(135deg, #4c8dff, #2563eb);
}
.nav nav {
  display: flex;
  align-items: center;
  gap: 26px;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--muted);
}
.nav .cta {
  padding: 8px 15px;
  border-radius: 999px;
  background: var(--ink);
  color: #fff;
}

.wrap {
  flex: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(360px, 0.92fr);
  gap: 56px;
  align-items: center;
  padding: 52px 40px;
  max-width: 1240px;
  width: 100%;
  margin: 0 auto;
}

/* ---- 무엇이 어떻게 바뀌는가 ---- */
.kicker {
  margin: 0;
  font-size: 11.5px;
  font-weight: 800;
  letter-spacing: 0.18em;
  color: var(--brand);
}
.change h1 {
  margin: 14px 0 0;
  font-size: 36px;
  line-height: 1.34;
  font-weight: 700;
  letter-spacing: -0.035em;
}
.toggle {
  display: inline-flex;
  margin-top: 26px;
  padding: 4px;
  border-radius: 999px;
  background: #eceff4;
}
.toggle button {
  padding: 8px 18px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--muted);
  font-size: 13px;
  font-weight: 700;
  font-family: inherit;
  transition: all 0.18s;
}
.toggle button.on {
  background: #fff;
  color: var(--ink);
  box-shadow: 0 2px 6px rgba(16, 25, 43, 0.09);
}

.rows {
  list-style: none;
  margin: 24px 0 0;
  padding: 0;
  border-top: 1px solid var(--line);
}
.rows li {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 16px;
  align-items: baseline;
  padding: 15px 2px;
  border-bottom: 1px solid var(--line);
}
.rl {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--muted);
}
.rt {
  font-size: 15px;
  line-height: 1.6;
  color: #8b98ab;
}
.rows li.after .rt {
  color: var(--ink);
  font-weight: 600;
}
.rt {
  animation: swapin 0.22s ease;
}
@keyframes swapin {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
}

.metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 26px;
}
.metrics > div {
  padding: 16px 18px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
  transition: border-color 0.25s, box-shadow 0.25s;
}
.metrics > div.after {
  border-color: #c9dcff;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.08);
}
.metrics b {
  display: block;
  font-size: 27px;
  font-weight: 700;
  letter-spacing: -0.03em;
  font-variant-numeric: tabular-nums;
  color: #8b98ab;
  transition: color 0.25s;
}
.metrics > div.after b {
  color: var(--brand);
}
.metrics i {
  font-style: normal;
  font-size: 15px;
  margin-left: 2px;
}
.metrics span {
  display: block;
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--muted);
}
.note {
  margin: 16px 0 0;
  font-size: 12px;
  color: #97a1b1;
}

/* ---- 로그인 ---- */
.login {
  padding: 32px 32px 28px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 22px;
  box-shadow: 0 24px 60px rgba(16, 25, 43, 0.08);
}
.login h2 {
  margin: 0;
  font-size: 25px;
  font-weight: 700;
  letter-spacing: -0.03em;
}
.sub {
  margin: 8px 0 0;
  font-size: 13.5px;
  color: var(--muted);
}
label {
  display: block;
  margin-top: 17px;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--muted);
}
.wrapin {
  position: relative;
  display: block;
}
input:not([type='checkbox']) {
  display: block;
  width: 100%;
  margin-top: 7px;
  padding: 12px 14px;
  border: 1px solid #d8dde6;
  border-radius: 11px;
  font-size: 14.5px;
  font-family: inherit;
  color: var(--ink);
  background: #fff;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
input::placeholder {
  color: #adb5c2;
}
input:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.13);
}
input.bad {
  border-color: #e05a4f;
}
.wrapin em {
  position: absolute;
  right: 14px;
  bottom: 13px;
  font-style: normal;
  font-size: 13px;
  font-weight: 600;
  color: #adb5c2;
}
.eye {
  position: absolute;
  right: 9px;
  bottom: 9px;
  padding: 5px 9px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--muted);
  font-size: 12px;
  font-weight: 700;
  font-family: inherit;
}
.eye:hover {
  background: #f1f4f9;
}
.msg {
  margin: 6px 0 0;
  font-size: 12px;
  color: #e05a4f;
}
.row {
  margin-top: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}
.chk {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: var(--muted);
}
.chk input {
  width: 15px;
  height: 15px;
  accent-color: var(--brand);
}
.row a {
  color: var(--brand);
  font-weight: 600;
}
.fail {
  margin: 13px 0 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fdf1f0;
  border: 1px solid #f5cfcb;
  color: #b93c30;
  font-size: 12.5px;
}
.primary {
  width: 100%;
  margin-top: 17px;
  padding: 13px 0;
  border: 0;
  border-radius: 11px;
  background: var(--brand);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  font-family: inherit;
  display: flex;
  justify-content: center;
  align-items: center;
}
.primary:hover {
  background: #1d4ed8;
}
.spin {
  width: 17px;
  height: 17px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.or {
  position: relative;
  margin: 20px 0 14px;
  text-align: center;
}
.or::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 1px;
  background: var(--line);
}
.or span {
  position: relative;
  padding: 0 12px;
  background: #fff;
  font-size: 12px;
  color: #9aa3b2;
}
.sso {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 9px;
}
.sso button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  padding: 11px 0;
  border: 1px solid #d8dde6;
  border-radius: 11px;
  background: #fff;
  color: var(--ink);
  font-size: 13.5px;
  font-weight: 600;
  font-family: inherit;
}
.sso button:hover {
  background: #f7f9fc;
}
.ms {
  display: grid;
  grid-template-columns: 7px 7px;
  gap: 2px;
}
.ms i {
  width: 7px;
  height: 7px;
}
.ms i:nth-child(1) {
  background: #f25022;
}
.ms i:nth-child(2) {
  background: #7fba00;
}
.ms i:nth-child(3) {
  background: #00a4ef;
}
.ms i:nth-child(4) {
  background: #ffb900;
}
.gg {
  width: 16px;
  height: 16px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #ea4335;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}
.foot {
  margin: 18px 0 0;
  text-align: center;
  font-size: 13px;
  color: var(--muted);
}
.foot a {
  color: var(--brand);
  font-weight: 600;
}
.ok {
  text-align: center;
}
.ok-mark {
  width: 54px;
  height: 54px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: #e8f5ee;
  color: var(--ok);
  font-size: 26px;
  display: grid;
  place-items: center;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.legal {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding: 20px;
  border-top: 1px solid var(--line);
  font-size: 11.5px;
  color: #97a1b1;
}

@media (max-width: 980px) {
  .nav {
    padding: 16px 20px;
  }
  .nav nav a:not(.cta) {
    display: none;
  }
  .wrap {
    grid-template-columns: 1fr;
    gap: 32px;
    padding: 32px 20px 40px;
  }
  .change h1 {
    font-size: 27px;
  }
  .metrics {
    grid-template-columns: 1fr;
  }
  .rows li {
    grid-template-columns: 76px 1fr;
    gap: 12px;
  }
  .login {
    padding: 26px 22px 24px;
  }
}
</style>
