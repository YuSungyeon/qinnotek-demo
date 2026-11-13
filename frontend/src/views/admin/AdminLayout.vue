<script setup>
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { Bell, Building2, FileText, Layers, Settings2, Users } from 'lucide-vue-next'
import { useReviewNotifications } from '../../composables/useReviewNotifications'

// 관리자 화면 틀: 고객 화면과 같은 '떠 있는 알약형 상단 바' (design.md nav-pill).
// 메뉴가 4개뿐이라 사이드바 없이 상단에 두고, 본문은 가운데 정렬로 넓게 쓴다.
const nav = [
  { to: '/admin/companies', icon: Building2, label: '기업' },
  { to: '/admin/requirements', icon: Layers, label: '요청 자료' },
  { to: '/admin/managers', icon: Users, label: '담당자' },
  { to: '/admin/settings', icon: Settings2, label: '설정' }
]

const route = useRoute()
const router = useRouter()
// 기업 상세(/admin/companies/:id)에서도 '기업' 메뉴가 켜져 있게 경로 앞부분으로 비교한다
const isActive = (to) => route.path === to || route.path.startsWith(to + '/')

// 검수 대기 기업 수 (60초 폴링). 오른쪽 파란 알약 - 화면에서 파랑은 "지금 결정할 것"에만 쓴다.
const { reviewCompanies, bumped } = useReviewNotifications()
function openReviewList() {
  router.push({ path: '/admin/companies', query: { status: 'UNDER_REVIEW' } })
}
</script>

<template>
  <div class="admin">
    <header class="nav-wrap">
      <nav class="nav">
        <RouterLink to="/admin/companies" class="brand" aria-label="자료 검수 홈">
          <span class="brand-mark"><FileText :size="18" :stroke-width="2.3" /></span>
          <span class="brand-name">자료 검수</span>
        </RouterLink>

        <div class="menu">
          <RouterLink v-for="n in nav" :key="n.to" :to="n.to" class="menu-item" :class="{ on: isActive(n.to) }">
            <component :is="n.icon" :size="16" />
            <span class="menu-label">{{ n.label }}</span>
          </RouterLink>
        </div>

        <button
          v-if="reviewCompanies > 0"
          class="review-pill"
          :class="{ bump: bumped }"
          :title="`검수 대기 기업 ${reviewCompanies}곳`"
          @click="openReviewList"
        >
          <Bell :size="15" />
          <span><span class="review-text">검수 대기 </span>{{ reviewCompanies }}<span class="review-text">곳</span></span>
        </button>
      </nav>
    </header>

    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin {
  min-height: 100vh;
  background: var(--canvas);
}

/* 상단 바: 화면 가장자리에서 떨어져 떠 있고, 스크롤해도 따라온다 */
.nav-wrap {
  position: sticky;
  top: 0;
  z-index: 30;
  padding: 16px 16px 0;
  display: flex;
  justify-content: center;
  pointer-events: none; /* 바 바깥 여백은 아래 콘텐츠 클릭을 막지 않게 */
}
.nav {
  pointer-events: auto;
  width: 100%;
  max-width: 1120px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  background: var(--canvas-soft);
  border-radius: var(--r-full);
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-right: 8px;
}
.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 30%;
  background: var(--ink);
  color: var(--on-ink);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.brand-name {
  font-size: 16px;
  font-weight: var(--w-heading);
  white-space: nowrap;
}
.menu {
  flex: 1;
  min-width: 0;
  display: flex;
  gap: 2px;
}
.menu-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 38px;
  padding: 0 14px;
  border-radius: var(--r-full);
  font-size: 15px;
  font-weight: 600;
  color: var(--muted);
  white-space: nowrap;
}
.menu-item:hover {
  color: var(--ink);
}
.menu-item.on {
  background: var(--canvas);
  color: var(--ink);
}
.review-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 38px;
  padding: 0 14px;
  border: 0;
  border-radius: var(--r-full);
  background: var(--accent);
  color: var(--on-ink);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}
/* 새 제출이 들어오면 잠깐 커졌다 돌아온다 */
.review-pill.bump {
  animation: bump 0.6s ease 2;
}
@keyframes bump {
  50% {
    transform: scale(1.08);
  }
}

.content {
  max-width: 1120px;
  margin: 0 auto;
  padding: 44px 16px 96px;
}

@media (max-width: 840px) {
  .brand-name {
    display: none;
  }
}
/* 휴대폰: 메뉴는 아이콘만, 검수 대기는 숫자만 */
@media (max-width: 640px) {
  .menu-label,
  .review-text {
    display: none;
  }
  .menu-item {
    padding: 0 12px;
  }
  .review-pill {
    padding: 0 12px;
  }
  .content {
    padding-top: 32px;
  }
}
</style>
