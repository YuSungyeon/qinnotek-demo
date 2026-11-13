import { createRouter, createWebHistory } from 'vue-router'

import CustomerView from '../views/customer/CustomerView.vue'
import AdminLayout from '../views/admin/AdminLayout.vue'
import AdminCompanies from '../views/admin/AdminCompanies.vue'
import AdminCompanyDetail from '../views/admin/AdminCompanyDetail.vue'
import AdminRequirements from '../views/admin/AdminRequirements.vue'
import AdminManagers from '../views/admin/AdminManagers.vue'
import AdminSettings from '../views/admin/AdminSettings.vue'

const routes = [
  // 고객은 관리자가 보낸 제출 링크(/s/토큰)로만 들어온다
  { path: '/', name: 'customer-home', component: CustomerView },
  { path: '/s/:token', name: 'customer', component: CustomerView, props: true },
  {
    path: '/example',
    name: 'example',
    component: () => import('../views/example/MesFlowView.vue')
  },
  {
    path: '/example2',
    name: 'example2',
    component: () => import('../views/example/MesFlowView2.vue')
  },
  {
    path: '/example3',
    name: 'example3',
    component: () => import('../views/example/MesFlowView3.vue')
  },
  {
    path: '/example4',
    name: 'example4',
    component: () => import('../views/example/LoginChange.vue')
  },
  {
    path: '/example5',
    name: 'example5',
    component: () => import('../views/example/LoginSaas.vue')
  },
  {
    path: '/example6',
    name: 'example6',
    component: () => import('../views/example/LoginManaged.vue')
  },
  {
    path: '/example7',
    name: 'example7',
    component: () => import('../views/example/LoginStory.vue')
  },
  {
    path: '/example8',
    name: 'example8',
    component: () => import('../views/example/LoginStoryIcon.vue')
  },
  {
    path: '/example9',
    name: 'example9',
    component: () => import('../views/example/LinkAccess.vue')
  },
  {
    // 시안: 관리자 화면 design.md 스타일 (실제 /admin 은 그대로)
    path: '/example10',
    name: 'example10',
    component: () => import('../views/example/AdminRedesign.vue')
  },
  {
    path: '/admin',
    component: AdminLayout,
    children: [
      { path: '', redirect: '/admin/companies' },
      { path: 'companies', name: 'admin-companies', component: AdminCompanies },
      { path: 'companies/:id', name: 'admin-company-detail', component: AdminCompanyDetail, props: true },
      { path: 'requirements', name: 'admin-requirements', component: AdminRequirements },
      { path: 'managers', name: 'admin-managers', component: AdminManagers },
      { path: 'settings', name: 'admin-settings', component: AdminSettings }
    ]
  }
]

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})
