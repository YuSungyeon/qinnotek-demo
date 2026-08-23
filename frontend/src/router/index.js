import { createRouter, createWebHistory } from 'vue-router'

import CustomerView from '../views/customer/CustomerView.vue'
import AdminLayout from '../views/admin/AdminLayout.vue'
import AdminCompanies from '../views/admin/AdminCompanies.vue'
import AdminCompanyDetail from '../views/admin/AdminCompanyDetail.vue'
import AdminRequirements from '../views/admin/AdminRequirements.vue'
import AdminManagers from '../views/admin/AdminManagers.vue'
import AdminSettings from '../views/admin/AdminSettings.vue'

const routes = [
  { path: '/', name: 'customer', component: CustomerView },
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
    component: () => import('../views/example/LoginTerminal.vue')
  },
  {
    path: '/example5',
    name: 'example5',
    component: () => import('../views/example/LoginSaas.vue')
  },
  {
    path: '/example6',
    name: 'example6',
    component: () => import('../views/example/LoginBadge.vue')
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
