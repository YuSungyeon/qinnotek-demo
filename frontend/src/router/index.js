import { createRouter, createWebHistory } from 'vue-router'

import CustomerView from '../views/customer/CustomerView.vue'
import AdminLayout from '../views/admin/AdminLayout.vue'
import AdminCompanies from '../views/admin/AdminCompanies.vue'
import AdminCompanyDetail from '../views/admin/AdminCompanyDetail.vue'
import AdminRequirements from '../views/admin/AdminRequirements.vue'

const routes = [
  { path: '/', name: 'customer', component: CustomerView },
  {
    path: '/admin',
    component: AdminLayout,
    children: [
      { path: '', redirect: '/admin/companies' },
      { path: 'companies', name: 'admin-companies', component: AdminCompanies },
      { path: 'companies/:id', name: 'admin-company-detail', component: AdminCompanyDetail, props: true },
      { path: 'requirements', name: 'admin-requirements', component: AdminRequirements }
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
