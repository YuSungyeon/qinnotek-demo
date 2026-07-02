<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="admin">
    <aside class="sidebar">
      <div class="brand">
        <span class="logo">📸</span>
        <span>사진 검수 관리자</span>
      </div>
      <nav>
        <RouterLink to="/admin/companies" class="nav-item">기업 관리</RouterLink>
        <RouterLink to="/admin/requirements" class="nav-item">요구 사진 관리</RouterLink>
      </nav>
      <RouterLink to="/" class="nav-item customer-link">↗ 고객 화면</RouterLink>
    </aside>
    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin {
  display: flex;
  min-height: 100vh;
}
.sidebar {
  width: 220px;
  flex: 0 0 220px;
  background: #111827;
  color: #e5e7eb;
  padding: 20px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 800;
  font-size: 16px;
  padding: 6px 8px 18px;
}
.logo {
  font-size: 20px;
}
nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.nav-item {
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #cbd5e1;
}
.nav-item:hover {
  background: #1f2937;
  color: #fff;
}
.nav-item.router-link-active {
  background: var(--primary);
  color: #fff;
}
.customer-link {
  margin-top: auto;
  color: #94a3b8;
  font-weight: 500;
}
.content {
  flex: 1;
  padding: 28px 32px;
  max-width: 1100px;
}
@media (max-width: 720px) {
  .admin {
    flex-direction: column;
  }
  .sidebar {
    width: 100%;
    flex: none;
    flex-direction: row;
    align-items: center;
    padding: 10px 14px;
    gap: 8px;
  }
  .brand {
    padding: 0 8px 0 0;
    font-size: 14px;
  }
  .customer-link {
    margin-top: 0;
  }
  .content {
    padding: 18px 16px;
  }
}
</style>
