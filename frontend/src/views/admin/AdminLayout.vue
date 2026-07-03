<script setup>
import { RouterLink, RouterView } from 'vue-router'

const nav = [
  { to: '/admin/companies', icon: '🏢', label: '기업 관리' },
  { to: '/admin/requirements', icon: '🗂️', label: '요구 사진 관리' },
  { to: '/admin/settings', icon: '⚙️', label: '관리자 설정' }
]
</script>

<template>
  <div class="admin">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">📸</div>
        <div class="brand-text">
          <strong>사진 검수</strong>
          <span>Admin Console</span>
        </div>
      </div>

      <nav>
        <RouterLink v-for="n in nav" :key="n.to" :to="n.to" class="nav-item">
          <span class="nav-icon">{{ n.icon }}</span>
          <span>{{ n.label }}</span>
        </RouterLink>
      </nav>

      <RouterLink to="/" class="nav-item customer-link">
        <span class="nav-icon">↗</span>
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
  background: linear-gradient(180deg, #0f172a 0%, #131c31 100%);
  color: #e2e8f0;
  padding: 18px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
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
  font-size: 20px;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4);
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
  color: #cbd5e1;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}
.nav-icon {
  font-size: 17px;
  width: 20px;
  text-align: center;
}
.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}
.nav-item.router-link-active {
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  color: #fff;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
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
