<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'

// MES SaaS 로그인 — 관리 대상 하나하나를 놓고
// "예전에는 이렇게 두었고, 지금은 이렇게 관리된다"를 순서대로 보여준다.
// 공정 대상 아이콘 (선형, currentColor 상속)
const ICONS = {
  so: '<rect x="7" y="2.6" width="10" height="3.4" rx="1"/><path d="M17 4.6h1.6A1.4 1.4 0 0 1 20 6v13.6a1.4 1.4 0 0 1-1.4 1.4H5.4A1.4 1.4 0 0 1 4 19.6V6a1.4 1.4 0 0 1 1.4-1.4H7"/><path d="M8 11h8M8 15h5"/>',
  lot: '<path d="M3.6 7.4 12 3l8.4 4.4v9.2L12 21l-8.4-4.4z"/><path d="M3.6 7.4 12 11.9l8.4-4.5M12 11.9V21"/><circle cx="16.4" cy="9" r="1.5"/>',
  stk: '<path d="M4 4.5v15M20 4.5v15"/><path d="M4 4.5h16M4 10h16M4 15h16M4 19.5h16"/><rect x="6.4" y="5.9" width="4.6" height="3.2" rx="0.6"/><rect x="13" y="11.4" width="4.6" height="3.2" rx="0.6"/>',
  wo: '<rect x="3.6" y="5" width="16.8" height="15" rx="2"/><path d="M3.6 9.6h16.8M8 3v4M16 3v4"/><path d="M8.4 14.2l2.1 2.1 5-4.6"/>',
  eqp: '<rect x="3.2" y="13" width="17.6" height="7" rx="1.6"/><path d="M7 13V9.2h10V13"/><path d="M12 9.2V5.6"/><circle cx="12" cy="4.2" r="1.6"/><path d="M7.4 16.6h3M13.6 16.6h3"/>',
  qc: '<path d="M12 3l7 2.8v5.4c0 4.2-2.9 7.6-7 8.6-4.1-1-7-4.4-7-8.6V5.8z"/><path d="M9 11.9l2.2 2.2 4.3-4.5"/>',
  do: '<path d="M2.6 6.6h10.8v10H2.6z"/><path d="M13.4 10h3.9l3.1 3.3v3.3h-7z"/><circle cx="7" cy="18.2" r="1.7"/><circle cx="17" cy="18.2" r="1.7"/>'
}
// 관리 방식 아이콘 — 수기 기록 / 자동 수집
const WAY = {
  manual:
    '<path d="M5.6 3.4h7.6L18 8.2V19a1.6 1.6 0 0 1-1.6 1.6H5.6A1.6 1.6 0 0 1 4 19V5a1.6 1.6 0 0 1 1.6-1.6z"/><path d="M13 3.6v4.8h4.8"/><path d="M7.4 12.6h6M7.4 16h4"/>',
  auto:
    '<circle cx="12" cy="17.8" r="1.7"/><path d="M8.6 14.4a4.9 4.9 0 0 1 6.8 0"/><path d="M5.7 11.3a9 9 0 0 1 12.6 0"/><path d="M12 3.2v4.2M9.6 5.1l2.4 2.3 2.4-2.3"/>'
}

const ITEMS = [
  {
    key: 'so',
    code: 'SO',
    name: '수주',
    before: '주문서와 엑셀 시트로 나눠 두고 진척은 전화로 확인했습니다.',
    after: '수주번호로 채번되어 품목·수량·납기와 공정 진척률이 한 화면에 모입니다.',
    when: '주문 접수 즉시',
    who: '영업 · 생산관리'
  },
  {
    key: 'lot',
    code: 'LOT',
    name: '자재 / LOT',
    before: '입고증과 검사성적서가 서류철에 따로 보관됐습니다.',
    after: '입고 시 LOT을 채번하고 검사 결과를 그 LOT에 붙여 함께 보관합니다.',
    when: '입고 · 검사 시점',
    who: '자재 · 품질'
  },
  {
    key: 'stk',
    code: 'STK',
    name: '재고',
    before: '월말 재고조사로 장부를 맞추고 그 사이는 추정했습니다.',
    after: '입출고 트랜잭션마다 로케이션별 수량이 즉시 증감합니다.',
    when: '트랜잭션 발생 즉시',
    who: '자재 · 생산관리'
  },
  {
    key: 'wo',
    code: 'WO',
    name: '작업지시',
    before: '화이트보드와 구두 지시로 내리고 실적은 사후에 정리했습니다.',
    after: '수주·재고를 근거로 지시가 생성되고 계획 대비 실적이 나란히 붙습니다.',
    when: '생산계획 확정 시',
    who: '생산관리 · 현장'
  },
  {
    key: 'eqp',
    code: 'EQP',
    name: '설비',
    before: '작업일보에 가동시간을 손으로 적어 다음 날 집계했습니다.',
    after: '설비 신호를 그대로 받아 가동·정지·사이클타임이 초 단위로 쌓입니다.',
    when: '초 단위 연속 수집',
    who: '현장 · 설비보전'
  },
  {
    key: 'qc',
    code: 'QC',
    name: '품질',
    before: '검사 결과를 종이에 적고 불량 원인은 기억에 의존했습니다.',
    after: '판정과 불량 코드가 즉시 기록되고 설비·작업자·시간대로 집계됩니다.',
    when: '검사 즉시',
    who: '품질 · 생산관리'
  },
  {
    key: 'do',
    code: 'DO',
    name: '출하',
    before: '출하 뒤 문제가 생기면 어느 자재였는지 되짚기 어려웠습니다.',
    after: '박스·라벨과 LOT이 연결되어 고객까지 이력이 이어집니다.',
    when: '상차 시점',
    who: '물류 · 영업'
  }
]

const sel = ref(0)
const auto = ref(true)
const item = computed(() => ITEMS[sel.value])
let timer = 0
let resume = 0

function pick(i) {
  sel.value = i
  auto.value = false
  clearTimeout(resume)
  resume = setTimeout(() => (auto.value = true), 8000)
}
onMounted(() => {
  timer = setInterval(() => {
    if (auto.value) sel.value = (sel.value + 1) % ITEMS.length
  }, 3200)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  clearTimeout(resume)
})

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
        <a href="#" class="cta" @click.prevent>도입 문의</a>
      </nav>
    </header>

    <main class="wrap">
      <!-- 관리 대상 × 무엇이 어떻게 바뀌는가 -->
      <section class="story">
        <p class="kicker">MES 클라우드</p>
        <h1>무엇을 어떻게 관리하고,<br />그래서 무엇이 달라지는지</h1>

        <!-- 관리 대상 아이콘 레일 -->
        <div class="rail">
          <button
            v-for="(it, i) in ITEMS"
            :key="it.key"
            :class="{ on: sel === i }"
            :title="it.name"
            @click="pick(i)"
          >
            <svg viewBox="0 0 24 24" v-html="ICONS[it.key]" />
            <span>{{ it.name }}</span>
          </button>
        </div>

        <div class="board" :key="item.key">
          <div class="head">
            <span class="tile"><svg viewBox="0 0 24 24" v-html="ICONS[item.key]" /></span>
            <div class="ht">
              <b>{{ item.name }}</b>
              <span>{{ item.code }} · {{ item.when }} · {{ item.who }}</span>
            </div>
          </div>

          <div class="ba">
            <div class="col before">
              <p class="tag">
                <svg viewBox="0 0 24 24" v-html="WAY.manual" />
                지금까지
              </p>
              <p class="txt">{{ item.before }}</p>
            </div>
            <div class="arrow">
              <svg viewBox="0 0 24 24"><path d="M4 12h15M13 6l6 6-6 6" /></svg>
            </div>
            <div class="col after">
              <p class="tag">
                <svg viewBox="0 0 24 24" v-html="WAY.auto" />
                MES에서는
              </p>
              <p class="txt">{{ item.after }}</p>
            </div>
          </div>

          <div class="bar"><i :class="{ run: auto }" :key="sel" /></div>
        </div>

        <!-- 전체 흐름에서 지금 보고 있는 지점 -->
        <div class="flow">
          <span v-for="(it, i) in ITEMS" :key="it.key" :class="{ on: sel === i, past: i < sel }">
            <svg viewBox="0 0 24 24" v-html="ICONS[it.key]" />
          </span>
        </div>

        <p class="note">
          일곱 가지 대상이 같은 방식으로 관리됩니다 — 발생한 자리에서 수집하고,
          누가 언제 바꿨는지와 함께 남기고, 권한이 있는 사람에게 보여줍니다.
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
  --ink: #101a2c;
  --muted: #6d7c92;
  --line: #e4e8ee;
  --brand: #2563eb;

  min-height: 100vh;
  background: #f5f7fa;
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
  background: #fff;
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
  background: linear-gradient(135deg, #4c8dff, #2563eb);
}
.nav nav {
  display: flex;
  align-items: center;
  gap: 24px;
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
  grid-template-columns: minmax(0, 1.14fr) minmax(360px, 0.86fr);
  gap: 50px;
  align-items: center;
  padding: 46px 40px 56px;
  max-width: 1280px;
  width: 100%;
  margin: 0 auto;
}

/* ---- 스토리 ---- */
.kicker {
  margin: 0;
  font-size: 11.5px;
  font-weight: 800;
  letter-spacing: 0.18em;
  color: var(--brand);
}
.story h1 {
  margin: 14px 0 0;
  font-size: 34px;
  line-height: 1.34;
  font-weight: 700;
  letter-spacing: -0.035em;
}
/* 아이콘 공통 */
svg {
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.rail {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
  margin-top: 24px;
}
.rail button {
  display: grid;
  justify-items: center;
  gap: 7px;
  padding: 12px 4px 10px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
  color: #93a0b3;
  font-size: 11.5px;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.18s;
}
.rail button svg {
  width: 24px;
  height: 24px;
}
.rail button:hover {
  border-color: #c8d3e2;
  color: var(--ink);
}
.rail button.on {
  border-color: #b9d0ff;
  background: #eef4ff;
  color: var(--brand);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.12);
}

.board {
  margin-top: 18px;
  padding: 22px 24px 18px;
  border-radius: 20px;
  background: #101d33;
  color: #e7eefa;
  box-shadow: 0 24px 50px rgba(16, 29, 51, 0.22);
  animation: rise 0.28s ease;
}
@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
}
.head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.09);
}
.tile {
  flex: none;
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  background: rgba(91, 157, 255, 0.16);
  color: #8bbaff;
}
.tile svg {
  width: 26px;
  height: 26px;
}
.ht b {
  display: block;
  font-size: 18px;
}
.ht span {
  font-size: 12px;
  color: #8ea3c2;
}

.ba {
  display: grid;
  grid-template-columns: 1fr 28px 1fr;
  align-items: stretch;
  gap: 6px;
  padding: 18px 0 16px;
}
.col {
  padding: 14px 16px;
  border-radius: 14px;
}
.col.before {
  background: rgba(255, 255, 255, 0.045);
}
.col.after {
  background: rgba(91, 157, 255, 0.13);
  border: 1px solid rgba(91, 157, 255, 0.3);
}
.tag {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0 0 9px;
  font-size: 10.5px;
  font-weight: 800;
  letter-spacing: 0.12em;
}
.tag svg {
  width: 17px;
  height: 17px;
  stroke-width: 1.6;
}
.col.before .tag {
  color: #7d8ea9;
}
.col.after .tag {
  color: #7fb0ff;
}
.txt {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.65;
}
.col.before .txt {
  color: #93a4bf;
}
.arrow {
  display: grid;
  place-items: center;
  color: #5b7ba6;
}
.arrow svg {
  width: 20px;
  height: 20px;
}

.bar {
  height: 3px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}
.bar i {
  display: block;
  height: 100%;
  width: 0;
  border-radius: 999px;
  background: #5b9dff;
}
.bar i.run {
  animation: fill 3.2s linear;
}
@keyframes fill {
  from {
    width: 0;
  }
  to {
    width: 100%;
  }
}

/* 전체 흐름 표시 */
.flow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-top: 16px;
  padding: 0 6px;
}
.flow span {
  position: relative;
  flex: 1;
  display: grid;
  place-items: center;
  height: 34px;
  color: #c3ccda;
  transition: color 0.2s, transform 0.2s;
}
.flow span::before {
  content: '';
  position: absolute;
  left: -50%;
  right: 50%;
  top: 50%;
  height: 1px;
  background: #dee3ea;
}
.flow span:first-child::before {
  display: none;
}
.flow span svg {
  position: relative;
  width: 19px;
  height: 19px;
  padding: 4px;
  background: #f5f7fa;
}
.flow span.past {
  color: #9fb6dc;
}
.flow span.on {
  color: var(--brand);
  transform: scale(1.22);
}
.note {
  margin: 18px 0 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--muted);
}

/* ---- 로그인 ---- */
.login {
  padding: 30px 30px 26px;
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
  color: #16a34a;
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

@media (max-width: 1000px) {
  .nav {
    padding: 16px 20px;
  }
  .nav nav a:not(.cta) {
    display: none;
  }
  .wrap {
    grid-template-columns: 1fr;
    gap: 30px;
    padding: 30px 20px 40px;
  }
  .story h1 {
    font-size: 26px;
  }
  .rail {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
  .ba {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  .arrow {
    transform: rotate(90deg);
  }
  .login {
    padding: 26px 22px 24px;
  }
}
</style>
