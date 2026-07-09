<template>
  <PageContainer title="设备详情" description="查看设备档案、日志统计和最近运行记录">
    <template #actions>
      <el-button :icon="Back" @click="router.push('/devices')">返回</el-button>
      <el-button type="primary" :icon="DocumentAdd" @click="logDialogVisible = true">新增日志</el-button>
    </template>

    <div v-loading="loading" class="section-grid">
      <div class="content-section">
        <div class="section-title">设备基础信息</div>
        <div class="section-body">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="设备名称">{{ device?.name }}</el-descriptions-item>
            <el-descriptions-item label="设备编号">{{ device?.deviceCode }}</el-descriptions-item>
            <el-descriptions-item label="设备类型">{{ device?.type }}</el-descriptions-item>
            <el-descriptions-item label="安装位置">{{ device?.location || '-' }}</el-descriptions-item>
            <el-descriptions-item label="当前状态"><StatusTag group="deviceStatus" :value="device?.status" /></el-descriptions-item>
            <el-descriptions-item label="最后在线">{{ device?.lastOnlineAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备描述">{{ device?.description || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      <div class="content-section">
        <div class="section-title">日志统计</div>
        <div class="section-body stat-grid detail-stats">
          <div class="stat-card"><div class="stat-label">日志总数</div><div class="stat-value">{{ device?.logCount || 0 }}</div></div>
          <div class="stat-card"><div class="stat-label">异常日志</div><div class="stat-value">{{ device?.errorLogCount || 0 }}</div></div>
          <div class="stat-card"><div class="stat-label">待处理日志</div><div class="stat-value">{{ device?.pendingLogCount || 0 }}</div></div>
        </div>
      </div>
    </div>

    <div class="content-section">
      <div class="section-title">最近上报数据</div>
      <el-table :data="device?.recentReports || []">
        <el-table-column prop="reportedAt" label="上报时间" min-width="150" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag group="deviceStatus" :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="温度" width="100">
          <template #default="{ row }">{{ formatMetric(row.temperature, '℃') }}</template>
        </el-table-column>
        <el-table-column label="湿度" width="100">
          <template #default="{ row }">{{ formatMetric(row.humidity, '%') }}</template>
        </el-table-column>
        <el-table-column label="电压" width="100">
          <template #default="{ row }">{{ formatMetric(row.voltage, 'V') }}</template>
        </el-table-column>
        <el-table-column label="信号" width="100">
          <template #default="{ row }">{{ formatMetric(row.signalStrength, 'dBm') }}</template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="180" show-overflow-tooltip />
      </el-table>
    </div>

    <div class="content-section">
      <div class="section-title">上报趋势</div>
      <div class="section-body">
        <ReportTrendPanel v-if="reportTrend.length" :reports="reportTrend" />
        <div v-else class="empty-text">暂无上报数据</div>
      </div>
    </div>

    <div class="content-section">
      <div class="section-title">
        最近日志
        <el-button text type="primary" @click="router.push(`/logs?deviceId=${device?.id}`)">查看全部</el-button>
      </div>
      <el-table :data="device?.recentLogs || []">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="类型" width="110"><template #default="{ row }"><StatusTag group="logType" :value="row.logType" /></template></el-table-column>
        <el-table-column label="等级" width="100"><template #default="{ row }"><StatusTag group="logLevel" :value="row.level" /></template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="{ row }"><StatusTag group="logStatus" :value="row.status" /></template></el-table-column>
        <el-table-column label="来源" width="110"><template #default="{ row }"><StatusTag group="logSource" :value="row.source" /></template></el-table-column>
        <el-table-column label="标签" min-width="160">
          <template #default="{ row }"><div class="tag-list"><el-tag v-for="tag in row.tags" :key="tag.id" size="small">{{ tag.name }}</el-tag></div></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="150" />
      </el-table>
    </div>

    <LogFormDialog :visible="logDialogVisible" mode="create" :default-device-id="device?.id" @cancel="logDialogVisible = false" @success="afterLogSaved" />
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Back, DocumentAdd } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import LogFormDialog from '@/components/LogFormDialog.vue'
import ReportTrendPanel from '@/components/ReportTrendPanel.vue'
import { getDeviceDetail } from '@/api/devices'
import { getDeviceReportList } from '@/api/reports'
import type { Device } from '@/types/device'
import type { DeviceReport } from '@/types/report'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const device = ref<Device>()
const reportTrend = ref<DeviceReport[]>([])
const logDialogVisible = ref(false)

async function loadData() {
  loading.value = true
  try {
    const deviceId = Number(route.params.id)
    const [deviceDetail, reportPage] = await Promise.all([
      getDeviceDetail(deviceId),
      getDeviceReportList({ page: 1, pageSize: 20, deviceId })
    ])
    device.value = deviceDetail
    reportTrend.value = reportPage.records
  } finally {
    loading.value = false
  }
}

function afterLogSaved() {
  logDialogVisible.value = false
  loadData()
}

function formatMetric(value: number | undefined, unit: string) {
  return value === undefined || value === null ? '-' : `${value}${unit}`
}

onMounted(loadData)
</script>

<style scoped>
.detail-stats {
  grid-template-columns: repeat(3, minmax(120px, 1fr));
}
</style>
