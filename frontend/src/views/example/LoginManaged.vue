<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'

// MES SaaS 로그인 — "무엇을 어떻게 관리하는가"를 로그인 옆에서 항목별로 보여준다.
const OBJECTS = [
  {
    key: 'so',
    name: '수주',
    code: 'SO',
    lead: '품목 · 수량 · 납기 · 고객',
    count: '진행 17건',
    what: '주문 품목과 수량, 약속한 납기와 고객 정보',
    how: '접수와 동시에 수주번호로 채번하고 공정 진척률을 자동 계산',
    when: '주문 접수 즉시',
    who: '영업 · 생산관리'
  },
  {
    key: 'lot',
    name: '자재 / LOT',
    code: 'LOT',
    lead: '입고 LOT · 수입검사 · 공급처',
    count: 'LOT 1,204건',
    what: '입고된 자재의 LOT 번호, 수입검사 결과, 공급처와 입고일',
    how: '입고 시 LOT을 채번하고 검사 결과를 그 LOT에 붙여 보관',
    when: '입고 · 검사 시점',
    who: '자재 · 품질'
  },
  {
    key: 'stock',
    name: '재고',
    code: 'STK',
    lead: '로케이션 · 수량 · 선입선출',
    count: '38 품목',
    what: '창고 로케이션별 현재고와 예약 수량',
    how: '입출고 트랜잭션마다 증감하고 선입선출 기준으로 불출 대상 결정',
    when: '트랜잭션 발생 즉시',
    who: '자재 · 생산관리'
  },
  {
    key: 'wo',
    name: '작업지시',
    code: 'WO',
    lead: '지시 수량 · 설비 · 인원 · 일정',
    count: '오늘 12건',
    what: '어떤 설비에서 누가 언제 얼마를 만들지에 대한 지시',
    how: '수주와 재고를 근거로 생성하고 계획 대비 실적을 나란히 비교',
    when: '생산계획 확정 시',
    who: '생산관리 · 현장'
  },
  {
    key: 'mc',
    name: '설비',
    code: 'EQP',
    lead: '가동 · 정지 · 사이클타임',
    count: '가동 9대',
    what: '설비별 가동 상태, 사이클타임, 생산 실적과 정지 사유',
    how: '설비 신호를 그대로 수집 — 작업일보를 손으로 적지 않음',
    when: '초 단위 연속 수집',
    who: '현장 · 설비보전'
  },
  {
    key: 'qc',
    name: '품질',
    code: 'QC',
    lead: '판정 · 불량 코드 · 검사자',
    count: '불량률 1.9%',
    what: '검사 판정 결과와 불량 코드, 검사자와 검사 조건',
    how: '검사 시점에 기록하고 설비·작업자·시간대와 교차해 원인 집계',
    when: '검사 즉시',
    who: '품질 · 생산관리'
  },
  {
    key: 'sh',
    name: '출하',
    code: 'DO',
    lead: '박스 · 라벨 · 차량 · 명세',
    count: '오늘 6건',
    what: '포장 박스 번호와 라벨, 출하 차량과 거래명세',
    how: '포장 단위와 LOT을 연결해 고객까지 이어지는 출하 이력 생성',
    when: '상차 시점',
    who: '물류 · 영업'
  }
]

const sel = ref(0)
const auto = ref(true)
let timer = 0
const current = computed(() => OBJECTS[sel.value])

function pick(i) {
  sel.value = i
  auto.value = false
  clearInterval(timer)
}
onMounted(() => {
  timer = setInterval(() => {
    if (auto.value) sel.value = (sel.value + 1) % OBJECTS.length
  }, 3800)
})
onBeforeUnmount(() => clearInterval(timer))

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
      <a href="#" class="cta" @click.prevent>도입 문의</a>
    </header>

    <main class="wrap">
      <!-- 무엇을 어떻게 관리하는가 -->
      <section class="board">
        <p class="kicker">MES 클라우드가 관리하는 것</p>
        <h1>현장에서 올라온 값이<br />어디에 어떻게 남는지</h1>

        <div class="objs">
          <ul class="list">
            <li v-for="(o, i) in OBJECTS" :key="o.key">
              <button :class="{ on: sel === i }" @click="pick(i)">
                <span class="code">{{ o.code }}</span>
                <span class="name">{{ o.name }}</span>
                <span class="lead">{{ o.lead }}</span>
                <span class="cnt">{{ o.count }}</span>
              </button>
            </li>
          </ul>

          <div class="detail" :key="current.key">
              <p class="d-head">
                <span class="code">{{ current.code }}</span>
                {{ current.name }}
              </p>
              <dl>
                <div><dt>무엇을</dt><dd>{{ current.what }}</dd></div>
                <div><dt>어떻게</dt><dd>{{ current.how }}</dd></div>
                <div><dt>언제</dt><dd>{{ current.when }}</dd></div>
                <div><dt>누가 보나</dt><dd>{{ current.who }}</dd></div>
              </dl>
          </div>
        </div>

        <p class="note">
          모든 기록은 누가 언제 바꿨는지와 함께 남고, 권한이 있는 사람에게만 보입니다.
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

            <p class="foot">
              계정은 회사 관리자가 발급합니다. <a href="#" @click.prevent>관리자 문의</a>
            </p>
          </form>

          <div v-else key="ok" class="ok">
            <div class="ok-mark">✓</div>
            <h2>{{ form.tenant || 'qinnotek' }} 콘솔로 이동합니다</h2>
            <p class="sub">{{ form.email }} 계정의 권한으로 화면이 구성됩니다.</p>
            <button class="primary" @click="done = false">다른 계정으로 로그인</button>
          </div>
        </Transition>
      </section>
    </main>
  </div>
</template>

<style scoped>
.page {
  --bg: #080e1a;
  --card: #0f1a2c;
  --line: #1e2d45;
  --text: #e9eff8;
  --muted: #8b9db8;
  --brand: #5b9dff;

  min-height: 100vh;
  background: radial-gradient(1100px 620px at 12% -10%, #14243d 0%, var(--bg) 58%);
  color: var(--text);
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
  background: linear-gradient(135deg, #5b9dff, #2f6fe0);
}
.cta {
  padding: 8px 16px;
  border: 1px solid var(--line);
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
}
.cta:hover {
  color: var(--text);
  border-color: #2c4165;
}

.wrap {
  flex: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(360px, 0.88fr);
  gap: 52px;
  align-items: center;
  padding: 46px 40px 56px;
  max-width: 1260px;
  width: 100%;
  margin: 0 auto;
}

/* ---- 관리 대상 보드 ---- */
.kicker {
  margin: 0;
  font-size: 11.5px;
  font-weight: 800;
  letter-spacing: 0.18em;
  color: var(--brand);
}
.board h1 {
  margin: 14px 0 0;
  font-size: 34px;
  line-height: 1.34;
  font-weight: 700;
  letter-spacing: -0.035em;
}
.objs {
  margin-top: 26px;
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 16px;
  align-items: start;
}
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 6px;
}
.list button {
  width: 100%;
  display: grid;
  grid-template-columns: 38px 1fr;
  grid-template-areas: 'code name' 'code lead';
  gap: 1px 11px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  color: var(--text);
  text-align: left;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.16s, border-color 0.16s;
}
.list button:hover {
  background: rgba(91, 157, 255, 0.06);
}
.list button.on {
  background: rgba(91, 157, 255, 0.1);
  border-color: rgba(91, 157, 255, 0.4);
}
.code {
  grid-area: code;
  align-self: center;
  padding: 3px 0;
  border-radius: 6px;
  background: rgba(91, 157, 255, 0.14);
  color: var(--brand);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-align: center;
}
.name {
  grid-area: name;
  font-size: 14px;
  font-weight: 700;
}
.lead {
  grid-area: lead;
  font-size: 11.5px;
  color: var(--muted);
}
.cnt {
  display: none;
}

.detail {
  padding: 20px 22px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--card);
  box-shadow: 0 24px 50px rgba(2, 8, 20, 0.4);
}
.d-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 14px;
  font-size: 16px;
  font-weight: 700;
}
.d-head .code {
  width: 40px;
}
.detail dl {
  margin: 0;
  display: grid;
  gap: 11px;
}
.detail dl > div {
  display: grid;
  grid-template-columns: 62px 1fr;
  gap: 12px;
  align-items: baseline;
}
.detail dt {
  font-size: 11.5px;
  font-weight: 700;
  color: var(--brand);
}
.detail dd {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.6;
  color: #cfdaea;
}
.detail {
  animation: det 0.24s ease;
}
@keyframes det {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
}
.note {
  margin: 20px 0 0;
  font-size: 12.5px;
  color: var(--muted);
}

/* ---- 로그인 ---- */
.login {
  padding: 30px 30px 26px;
  border: 1px solid var(--line);
  border-radius: 22px;
  background: linear-gradient(180deg, #132135, #0e1728);
  box-shadow: 0 30px 70px rgba(2, 8, 20, 0.5);
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
  border: 1px solid var(--line);
  border-radius: 11px;
  font-size: 14.5px;
  font-family: inherit;
  color: var(--text);
  background: #0b1424;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
input::placeholder {
  color: #5d738f;
}
input:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(91, 157, 255, 0.16);
}
input.bad {
  border-color: #e07a6f;
}
.wrapin em {
  position: absolute;
  right: 14px;
  bottom: 13px;
  font-style: normal;
  font-size: 13px;
  font-weight: 600;
  color: #5d738f;
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
  color: var(--text);
}
.msg {
  margin: 6px 0 0;
  font-size: 12px;
  color: #f0917f;
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
  background: rgba(224, 122, 111, 0.12);
  border: 1px solid rgba(224, 122, 111, 0.35);
  color: #f0917f;
  font-size: 12.5px;
}
.primary {
  width: 100%;
  margin-top: 17px;
  padding: 13px 0;
  border: 0;
  border-radius: 11px;
  background: var(--brand);
  color: #06101f;
  font-size: 15px;
  font-weight: 800;
  font-family: inherit;
  display: flex;
  justify-content: center;
  align-items: center;
}
.primary:hover {
  background: #78b0ff;
}
.spin {
  width: 17px;
  height: 17px;
  border: 2px solid rgba(6, 16, 31, 0.35);
  border-top-color: #06101f;
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
  background: #101b2d;
  font-size: 12px;
  color: var(--muted);
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
  border: 1px solid var(--line);
  border-radius: 11px;
  background: transparent;
  color: var(--text);
  font-size: 13.5px;
  font-weight: 600;
  font-family: inherit;
}
.sso button:hover {
  background: rgba(91, 157, 255, 0.07);
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
  background: rgba(52, 211, 153, 0.16);
  color: #34d399;
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

@media (max-width: 1040px) {
  .objs {
    grid-template-columns: 1fr;
  }
  .list {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    display: grid;
  }
  .lead {
    display: none;
  }
  .list button {
    grid-template-columns: 38px 1fr;
    grid-template-areas: 'code name';
    align-items: center;
  }
}
@media (max-width: 980px) {
  .nav {
    padding: 16px 20px;
  }
  .wrap {
    grid-template-columns: 1fr;
    gap: 30px;
    padding: 30px 20px 40px;
  }
  .board h1 {
    font-size: 26px;
  }
  .login {
    padding: 26px 22px 24px;
  }
}
</style>
