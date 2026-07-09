import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '首页统计' } },
        { path: 'devices', name: 'devices', component: () => import('@/views/DeviceListView.vue'), meta: { title: '设备管理' } },
        { path: 'devices/:id', name: 'device-detail', component: () => import('@/views/DeviceDetailView.vue'), meta: { title: '设备详情' } },
        { path: 'logs', name: 'logs', component: () => import('@/views/LogListView.vue'), meta: { title: '日志管理' } },
        { path: 'alert-rules', name: 'alert-rules', component: () => import('@/views/AlertRuleListView.vue'), meta: { title: '告警规则' } },
        { path: 'mqtt', name: 'mqtt', component: () => import('@/views/MqttStatusView.vue'), meta: { title: 'MQTT 状态' } },
        { path: 'tags', name: 'tags', component: () => import('@/views/TagListView.vue'), meta: { title: '标签管理' } }
      ]
    }
  ]
})

export default router
