<script setup>
import { computed, reactive, ref } from 'vue'

// MES SaaS 로그인 — 로그인 폼은 정석대로, 다만 어떤 제품에 들어가는지는 분명하게.
const HIGHLIGHTS = [
  { t: '실시간 실적 수집', d: '설비에서 올라온 실적·가동률·사이클타임이 사람 손을 거치지 않고 쌓입니다.' },
  { t: 'LOT 이력 추적', d: '입고 자재부터 출하 박스까지 하나의 LOT으로 이어서 추적합니다.' },
  { t: '공정 품질 분석', d: '불량 코드와 설비·작업자·시간대를 교차해 원인을 찾습니다.' }
]
const STATS = [
  ['도입 공장', '120+'],
  ['월간 수집 이벤트', '4.2억건'],
  ['평균 가동률 개선', '+11%p']
]

const form = reactive({ tenant: '', email: '', password: '', keep: true })
const show = ref(false)
const touched = reactive({ tenant: false, email: false, password: false })
const loading = ref(false)
const done = ref(false)
const fail = ref('')

const emailOk = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(form.email))
const tenantOk = computed(() => form.tenant.trim().length >= 2)
const pwOk = computed(() => form.password.length >= 8)
const valid = computed(() => emailOk.value && tenantOk.value && pwOk.value)

function submit() {
  touched.tenant = touched.email = touched.password = true
  fail.value = ''
  if (!valid.value) return
  loading.value = true
  setTimeout(() => {
    loading.value = false
    if (form.password.toLowerCase() === 'password') {
      fail.value = '비밀번호가 올바르지 않습니다. 5회 실패 시 계정이 잠깁니다.'
      return
    }
    done.value = true
  }, 900)
}
</script>

<template>
  <div class="saas">
    <!-- 좌: 제품 소개 -->
    <aside class="brand">
      <div class="brand-in">
        <div class="logo">
          <span class="mark" />
          QINNOTEK <b>MES</b>
        </div>

        <h1>현장의 모든 순간을<br />데이터로 남깁니다</h1>
        <p class="lead">
          수주부터 출하까지 아홉 공정의 기록을 하나로 잇는<br />
          클라우드 제조실행시스템입니다.
        </p>

        <ul class="hl">
          <li v-for="h in HIGHLIGHTS" :key="h.t">
            <i />
            <div>
              <b>{{ h.t }}</b>
              <span>{{ h.d }}</span>
            </div>
          </li>
        </ul>

        <div class="stats">
          <div v-for="s in STATS" :key="s[0]">
            <b>{{ s[1] }}</b>
            <span>{{ s[0] }}</span>
          </div>
        </div>
      </div>
    </aside>

    <!-- 우: 로그인 -->
    <main class="pane">
      <div class="box">
        <Transition name="fade" mode="out-in">
          <form v-if="!done" key="form" class="form" @submit.prevent="submit">
            <p class="mini">MES 클라우드 콘솔</p>
            <h2>로그인</h2>
            <p class="mini-sub">회사 계정으로 접속하세요.</p>

            <label class="lb">
              회사 코드
              <input
                v-model="form.tenant"
                class="in"
                :class="{ bad: touched.tenant && !tenantOk }"
                placeholder="qinnotek"
                autocomplete="organization"
                @blur="touched.tenant = true"
              />
              <span class="suffix">.mes.cloud</span>
            </label>
            <p v-if="touched.tenant && !tenantOk" class="msg">회사 코드를 입력하세요.</p>

            <label class="lb">
              이메일
              <input
                v-model="form.email"
                class="in"
                :class="{ bad: touched.email && !emailOk }"
                type="email"
                placeholder="name@company.com"
                autocomplete="username"
                @blur="touched.email = true"
              />
            </label>
            <p v-if="touched.email && !emailOk" class="msg">올바른 이메일 주소를 입력하세요.</p>

            <label class="lb">
              비밀번호
              <input
                v-model="form.password"
                class="in"
                :class="{ bad: touched.password && !pwOk }"
                :type="show ? 'text' : 'password'"
                placeholder="8자 이상"
                autocomplete="current-password"
                @blur="touched.password = true"
              />
              <button type="button" class="eye" @click="show = !show">
                {{ show ? '숨기기' : '보기' }}
              </button>
            </label>
            <p v-if="touched.password && !pwOk" class="msg">비밀번호는 8자 이상입니다.</p>

            <div class="row">
              <label class="check">
                <input v-model="form.keep" type="checkbox" />
                <span>로그인 상태 유지</span>
              </label>
              <a href="#" @click.prevent>비밀번호 찾기</a>
            </div>

            <p v-if="fail" class="fail">{{ fail }}</p>

            <button class="primary" :disabled="loading">
              <span v-if="!loading">로그인</span>
              <span v-else class="spin" />
            </button>

            <div class="or"><span>또는</span></div>

            <div class="sso">
              <button type="button">
                <span class="ms"><i /><i /><i /><i /></span>
                Microsoft 계정
              </button>
              <button type="button">
                <span class="gg">G</span>
                Google Workspace
              </button>
            </div>

            <p class="foot">
              아직 계정이 없으신가요? <a href="#" @click.prevent>MES 도입 문의</a>
            </p>
          </form>

          <div v-else key="ok" class="ok">
            <div class="ok-mark">✓</div>
            <h2>{{ form.tenant || 'qinnotek' }} 워크스페이스로 이동합니다</h2>
            <p class="mini-sub">{{ form.email }} 으로 로그인했습니다.</p>
            <button class="primary" @click="done = false">다른 계정으로 로그인</button>
          </div>
        </Transition>
      </div>

      <p class="legal">
        <span>ISO 27001 인증</span>
        <span>서울 리전 (ap-northeast-2)</span>
        <span>SLA 99.9%</span>
      </p>
    </main>
  </div>
</template>

<style scoped>
.saas {
  --ink: #0f172a;
  --muted: #64748b;
  --line: #e3e7ee;
  --brand: #2563eb;
  --bg: #f6f7f9;

  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  background: var(--bg);
  color: var(--ink);
  letter-spacing: -0.01em;
}

/* ---- 제품 소개 ---- */
.brand {
  position: relative;
  padding: 56px 60px;
  display: flex;
  align-items: center;
  background: radial-gradient(900px 500px at 15% 0%, #1b3358 0%, #0f1c33 55%, #0b1526 100%);
  color: #e9eef8;
  overflow: hidden;
}
.brand::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: 46px 46px;
  mask-image: radial-gradient(700px 500px at 20% 10%, #000 0%, transparent 75%);
  pointer-events: none;
}
.brand-in {
  position: relative;
  z-index: 1;
  max-width: 520px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.16em;
}
.logo b {
  color: #7fb0ff;
}
.mark {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: linear-gradient(135deg, #4c8dff, #2563eb);
  box-shadow: 0 0 0 4px rgba(76, 141, 255, 0.16);
}
.brand h1 {
  margin: 44px 0 0;
  font-size: 40px;
  line-height: 1.3;
  font-weight: 700;
  letter-spacing: -0.035em;
}
.lead {
  margin: 18px 0 0;
  font-size: 15px;
  line-height: 1.75;
  color: #9fb2ce;
}
.hl {
  list-style: none;
  margin: 38px 0 0;
  padding: 0;
  display: grid;
  gap: 18px;
}
.hl li {
  display: flex;
  gap: 14px;
}
.hl i {
  flex: none;
  width: 8px;
  height: 8px;
  margin-top: 7px;
  border-radius: 50%;
  background: #4c8dff;
  box-shadow: 0 0 0 4px rgba(76, 141, 255, 0.18);
}
.hl b {
  display: block;
  font-size: 15px;
  margin-bottom: 4px;
}
.hl span {
  font-size: 13.5px;
  line-height: 1.65;
  color: #92a6c4;
}
.stats {
  margin-top: 44px;
  padding-top: 26px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  gap: 40px;
}
.stats b {
  display: block;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.stats span {
  font-size: 12.5px;
  color: #92a6c4;
}

/* ---- 로그인 ---- */
.pane {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px 32px;
}
.box {
  width: 100%;
  max-width: 400px;
  padding: 34px 34px 30px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 20px;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.07);
}
.mini {
  margin: 0;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  color: var(--brand);
}
.box h2 {
  margin: 10px 0 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.03em;
}
.mini-sub {
  margin: 8px 0 0;
  font-size: 13.5px;
  color: var(--muted);
}

.lb {
  position: relative;
  display: block;
  margin-top: 18px;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--muted);
}
.in {
  display: block;
  width: 100%;
  margin-top: 7px;
  padding: 12px 14px;
  border: 1px solid #d7dce5;
  border-radius: 11px;
  font-size: 14.5px;
  font-family: inherit;
  color: var(--ink);
  background: #fff;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.in::placeholder {
  color: #aab3c2;
}
.in:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.14);
}
.in.bad {
  border-color: #e05a4f;
}
.suffix {
  position: absolute;
  right: 14px;
  bottom: 13px;
  font-size: 13px;
  font-weight: 600;
  color: #aab3c2;
}
.eye {
  position: absolute;
  right: 10px;
  bottom: 10px;
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
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}
.check {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  font-weight: 600;
}
.check input {
  width: 15px;
  height: 15px;
  accent-color: var(--brand);
}
.row a {
  color: var(--brand);
  font-weight: 600;
}
.fail {
  margin: 14px 0 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fdf1f0;
  border: 1px solid #f5cfcb;
  color: #b93c30;
  font-size: 12.5px;
}

.primary {
  width: 100%;
  margin-top: 18px;
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
  transition: background 0.15s;
}
.primary:hover {
  background: #1d4ed8;
}
.primary:disabled {
  opacity: 0.7;
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
  margin: 22px 0 16px;
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
  gap: 9px;
}
.sso button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 11px 0;
  border: 1px solid #d7dce5;
  border-radius: 11px;
  background: #fff;
  color: var(--ink);
  font-size: 13.5px;
  font-weight: 600;
  font-family: inherit;
  transition: background 0.15s, border-color 0.15s;
}
.sso button:hover {
  background: #f7f9fc;
  border-color: #c4ccd9;
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
  margin: 20px 0 0;
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
  padding: 8px 0;
}
.ok-mark {
  width: 54px;
  height: 54px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: #e8f5ee;
  color: #16a34a;
  font-size: 26px;
  display: grid;
  place-items: center;
}

.legal {
  margin: 22px 0 0;
  display: flex;
  gap: 18px;
  font-size: 11.5px;
  color: #97a1b1;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}
.fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 940px) {
  .saas {
    grid-template-columns: 1fr;
  }
  .brand {
    padding: 40px 28px 34px;
  }
  .brand h1 {
    margin-top: 28px;
    font-size: 30px;
  }
  .hl {
    margin-top: 26px;
    gap: 14px;
  }
  .stats {
    margin-top: 28px;
    padding-top: 20px;
    gap: 26px;
  }
  .pane {
    padding: 32px 20px 44px;
  }
}
</style>
