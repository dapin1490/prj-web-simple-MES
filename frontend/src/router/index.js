import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import OrderPlanningView from '../views/OrderPlanningView.vue'
import MonitoringView from '../views/MonitoringView.vue'
import QualityReportingView from '../views/QualityReportingView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/dashboard',
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: HomeView,
    },
    {
      path: '/orders',
      name: 'orders-planning',
      component: OrderPlanningView,
    },
    {
      path: '/monitoring',
      name: 'execution-monitoring',
      component: MonitoringView,
    },
    {
      path: '/quality',
      name: 'quality-reporting',
      component: QualityReportingView,
    },
  ],
})

export default router
