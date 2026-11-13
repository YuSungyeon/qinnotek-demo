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
  },
  // 그 밖의 주소(예전 시안 주소 등)는 빈 화면 대신 고객 첫 화면("링크를 확인해주세요")으로
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})
