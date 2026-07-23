<script setup>
import { RouterLink, RouterView } from 'vue-router'
import Icon from '../../components/Icon.vue'
import { useReviewNotifications } from '../../composables/useReviewNotifications'

const nav = [
  { to: '/admin/companies', icon: 'building', label: '기업 관리', badge: true },
  { to: '/admin/requirements', icon: 'layers', label: '요구 사진 관리' },
  { to: '/admin/managers', icon: 'user', label: '담당자 관리' },
  { to: '/admin/settings', icon: 'settings', label: '관리자 설정' }
]

const { reviewCompanies, bumped } = useReviewNotifications()
</script>

<template>
  <div class="admin">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark"><Icon name="camera" :size="21" /></div>
        <div class="brand-text">
          <strong>사진 검수</strong>
          <span>Admin Console</span>
        </div>
      </div>

      <nav>
        <RouterLink v-for="n in nav" :key="n.to" :to="n.to" class="nav-item">
          <span class="nav-icon"><Icon :name="n.icon" :size="18" /></span>
          <span>{{ n.label }}</span>
          <span
            v-if="n.badge && reviewCompanies > 0"
            class="nav-badge"
            :class="{ bump: bumped }"
          >{{ reviewCompanies }}</span>
        </RouterLink>
      </nav>

      <RouterLink to="/" class="nav-item customer-link">
        <span class="nav-icon"><Icon name="arrowUpRight" :size="18" /></span>
        <span>고객 화면</span>
      </RouterLink>
    </aside>

    <main class="content">
      <div class="content-inner">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<style scoped>
.admin {
  display: flex;
  min-height: 100vh;
}
.sidebar {
  width: 244px;
  flex: 0 0 244px;
  background: var(--side-bg, linear-gradient(180deg, #0f172a 0%, #131c31 100%));
  color: var(--side-text, #e2e8f0);
  padding: 18px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  border-right: 1px solid var(--side-border, rgba(255, 255, 255, 0.06));
  position: sticky;
  top: 0;
  height: 100vh;
}
.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 8px 8px 20px;
}
.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: var(--side-brand-bg, linear-gradient(135deg, var(--primary), var(--primary-dark)));
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.35);
}
.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}
.brand-text strong {
  font-size: 16px;
  font-weight: 800;
  color: #fff;
}
.brand-text span {
  font-size: 11px;
  color: #94a3b8;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}
nav {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 11px 12px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--side-text, #cbd5e1);
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}
.nav-icon {
  width: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.nav-badge {
  margin-left: auto;
  min-width: 22px;
  height: 22px;
  padding: 0 7px;
  border-radius: 999px;
  background: #dc2626;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.nav-badge.bump {
  animation: badge-bump 0.5s ease 0s 2;
}
@keyframes badge-bump {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.25);
  }
}
.nav-item:hover {
  background: var(--side-hover, rgba(255, 255, 255, 0.06));
  color: #fff;
}
.nav-item.router-link-active {
  background: var(--side-active-bg, linear-gradient(135deg, var(--primary), var(--primary-dark)));
  color: var(--side-active-text, #fff);
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.3);
}
.nav-item.router-link-active:hover {
  color: var(--side-active-text, #fff);
}
.customer-link {
  margin-top: auto;
  color: #94a3b8;
  font-weight: 500;
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.customer-link:hover {
  background: rgba(255, 255, 255, 0.05);
}
.content {
  flex: 1;
  min-width: 0;
  padding: 30px 34px;
}
.content-inner {
  max-width: 1120px;
}
@media (max-width: 760px) {
  .admin {
    flex-direction: column;
  }
  .sidebar {
    width: 100%;
    flex: none;
    height: auto;
    position: static;
    flex-direction: row;
    align-items: center;
    padding: 10px 12px;
    gap: 6px;
    overflow-x: auto;
  }
  .brand {
    padding: 0 10px 0 2px;
  }
  .brand-text {
    display: none;
  }
  nav {
    flex-direction: row;
    gap: 4px;
  }
  .nav-item {
    padding: 9px 12px;
  }
  .nav-item span:not(.nav-icon) {
    display: none;
  }
  .customer-link {
    margin-top: 0;
    margin-left: auto;
  }
  .customer-link span:not(.nav-icon) {
    display: none;
  }
  .content {
    padding: 18px 16px;
  }
}
</style>
