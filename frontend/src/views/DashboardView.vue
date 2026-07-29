<template>
  <PageContainer title="首页统计" description="设备状态、待处理日志和最近异常维护记录">
    <div v-loading="loading" class="stat-grid">
      <div v-for="item in stats" :key="item.label" class="stat-card">
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value">{{ item.value }}</div>
      </div>
    </div>

    <div class="section-grid">
      <div class="content-section">
        <div class="section-title">最近异常日志</div>
        <el-table :data="summary?.recentErrorLogs || []" size="small">
          <el-table-column prop="title" label="标题" min-width="160" />
          <el-table-column prop="deviceName" label="设备" min-width="130" />
          <el-table-column label="等级" width="96">
            <template #default="{ row }"><StatusTag group="logLevel" :value="row.level" /></template>
          </el-table-column>
          <el-table-column label="状态" width="96">
            <template #default="{ row }"><StatusTag group="logStatus" :value="row.status" /></template>
          </el-table-column>
        </el-table>
      </div>

      <div class="content-section">
        <div class="section-title">最近维护记录</div>
        <el-table :data="summary?.recentMaintenanceLogs || []" size="small">
          <el-table-column prop="title" label="标题" min-width="160" />
          <el-table-column prop="deviceName" label="设备" min-width="130" />
          <el-table-column label="状态" width="96">
            <template #default="{ row }"><StatusTag group="logStatus" :value="row.status" /></template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" min-width="150" />
        </el-table>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import { getDashboardSummary } from '@/api/dashboard'
import type { DashboardSummary } from '@/types/dashboard'
import { PAGE_REFRESH_INTERVAL_MS } from '@/constants/refresh'
import { usePageAutoRefresh } from '@/utils/autoRefresh'

const loading = ref(false)
const summary = ref<DashboardSummary>()
let requestPending = false

const stats = computed(() => [
  { label: '设备总数', value: summary.value?.deviceTotal ?? 0 },
  { label: '正常设备', value: summary.value?.normalDeviceCount ?? 0 },
  { label: '异常设备', value: summary.value?.abnormalDeviceCount ?? 0 },
  { label: '离线设备', value: summary.value?.offlineDeviceCount ?? 0 },
  { label: '维护中设备', value: summary.value?.maintenanceDeviceCount ?? 0 },
  { label: '待处理日志', value: summary.value?.pendingLogCount ?? 0 }
])

async function loadData(showLoading = true) {
  if (requestPending) return
  requestPending = true
  if (showLoading) loading.value = true
  try {
    summary.value = await getDashboardSummary()
  } finally {
    requestPending = false
    if (showLoading) loading.value = false
  }
}

usePageAutoRefresh({ intervalMs: PAGE_REFRESH_INTERVAL_MS, isHidden: () => document.hidden, isPending: () => requestPending, refresh: () => void loadData(false) })

onMounted(loadData)
</script>
