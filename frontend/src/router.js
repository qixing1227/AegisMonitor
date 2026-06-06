import { createRouter, createWebHistory } from 'vue-router'
import AlertsPage from './dashboard/AlertsPage.vue'
import HostDetailPage from './dashboard/HostDetailPage.vue'
import HomeDashboard from './dashboard/HomeDashboard.vue'
import ServicesPage from './dashboard/ServicesPage.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/hosts'
    },
    {
      path: '/hosts',
      component: HomeDashboard
    },
    {
      path: '/hosts/:hostId',
      component: HostDetailPage,
      props: true
    },
    {
      path: '/services',
      component: ServicesPage
    },
    {
      path: '/alerts',
      component: AlertsPage
    }
  ]
})
