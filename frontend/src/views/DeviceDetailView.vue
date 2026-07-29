<template>
  <PageContainer
    :title="device ? device.name : '设备工作台'"
    :description="device ? `${device.deviceCode} · ${device.type}` : '查看当前设备自己的运行日志与采集数据'"
  >
    <template #actions>
      <el-button :icon="Back" @click="router.push('/devices')">返回设备中心</el-button>
      <el-button type="primary" :icon="DocumentAdd" @click="logDialogVisible = true">新增日志</el-button>
    </template>

    <div v-loading="loading">
        <div class="device-summary">
        <div>
          <div class="summary-label">当前状态</div>
          <StatusTag group="deviceStatus" :value="device?.status" />
        </div>
        <div><div class="summary-label">最后在线</div><div>{{ device?.lastOnlineAt || '-' }}</div></div>
        <div><div class="summary-label">安装位置</div><div>{{ device?.location || '-' }}</div></div>
          <div>
            <div class="summary-label">展示内容</div>
            <div>{{ device?.monitoringMode === 'TELEMETRY' ? '运行日志与采集数据' : '运行日志' }}</div>
          </div>
          <div><div class="summary-label">最近电压</div><div>{{ formatMetric(latestReport?.voltage, ' V') }}</div></div>
          <div><div class="summary-label">当前电量</div><div>{{ latestReport?.batteryPercent == null ? '-' : `${latestReport.batteryPercent}%${latestReport.charging ? '（充电中）' : ''}` }}</div></div>
      </div>

      <el-tabs v-model="activeTab" class="device-tabs">
        <el-tab-pane label="设备概览" name="overview">
          <div class="section-grid">
            <div class="content-section">
              <div class="section-title">设备档案</div>
              <div class="section-body">
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="设备名称">{{ device?.name }}</el-descriptions-item>
                  <el-descriptions-item label="设备编号">{{ device?.deviceCode }}</el-descriptions-item>
                  <el-descriptions-item label="设备类型">{{ device?.type }}</el-descriptions-item>
                  <el-descriptions-item label="厂商">{{ device?.manufacturer || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="型号">{{ device?.model || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="序列号">{{ device?.serialNumber || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="固件版本">{{ device?.firmwareVersion || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="设备描述">{{ device?.description || '-' }}</el-descriptions-item>
                </el-descriptions>
              </div>
            </div>
            <div class="content-section">
              <div class="section-title">当前设备日志统计</div>
              <div class="section-body stat-grid detail-stats">
                <div class="stat-card"><div class="stat-label">日志总数</div><div class="stat-value">{{ device?.logCount || 0 }}</div></div>
                <div class="stat-card"><div class="stat-label">异常日志</div><div class="stat-value">{{ device?.errorLogCount || 0 }}</div></div>
                <div class="stat-card"><div class="stat-label">待处理</div><div class="stat-value">{{ device?.pendingLogCount || 0 }}</div></div>
              </div>
            </div>
          </div>
          <div class="content-section">
            <div class="section-title">最近活动</div>
            <div class="section-body">
              <el-button type="primary" plain @click="activeTab = 'logs'">查看该设备全部日志</el-button>
              <el-button v-if="device?.monitoringMode === 'TELEMETRY'" type="success" plain @click="activeTab = 'data'">查看采集数据与趋势</el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="零件清单" name="components">
          <div class="content-section">
            <div class="section-title">设备零件</div>
            <div class="section-body">
              <el-alert type="info" :closable="false" title="零件清单由人工确认和维护；设备主动上报硬件清单前，系统不会猜测屏幕、开发板或麦克风型号。" />
              <div class="component-actions"><el-button type="primary" @click="openComponentDialog()">新增零件</el-button></div>
              <el-table :data="components">
                <el-table-column prop="name" label="零件" min-width="130" />
                <el-table-column prop="category" label="类别" min-width="100" />
                <el-table-column prop="model" label="型号/规格" min-width="140" />
                <el-table-column prop="quantity" label="数量" width="80" />
                <el-table-column prop="notes" label="备注" min-width="150" show-overflow-tooltip />
                <el-table-column label="操作" width="150"><template #default="{ row }"><el-button link type="primary" @click="openComponentDialog(row)">编辑</el-button><el-button link type="danger" @click="removeComponent(row.id)">删除</el-button></template></el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="运行日志" name="logs">
          <div class="content-section">
            <div class="section-title">当前设备的运行日志</div>
            <div class="section-body">
              <DeviceLogsPanel v-if="device" :device-id="device.id" />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="AI MCP 操作" name="mcp">
          <div class="content-section">
            <div class="section-title">AI MCP 操作记录</div>
            <div class="section-body">
              <el-alert type="info" :closable="false" title="官方 MCP 当前不提供可靠的小智设备编号，以下为全局记录；提醒会在本设备的提醒记录中准确归属。" />
              <el-table :data="mcpExecutions" style="margin-top: 12px">
                <el-table-column prop="createdAt" label="时间" min-width="160" />
                <el-table-column prop="toolName" label="工具" min-width="180" />
                <el-table-column prop="targetSummary" label="目标" min-width="180" />
                <el-table-column label="结果" width="100"><template #default="{ row }"><el-tag :type="row.status === 'SUCCEEDED' ? 'success' : 'danger'">{{ row.status === 'SUCCEEDED' ? '成功' : '失败' }}</el-tag></template></el-table-column>
                <el-table-column prop="resultSummary" label="摘要" min-width="220" show-overflow-tooltip />
              </el-table>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="提醒" name="reminders">
          <div class="content-section">
            <div class="section-title">本设备提醒</div>
            <div class="section-body">
              <ReminderTable v-if="device" :device-code="device.deviceCode" />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane v-if="device?.monitoringMode === 'TELEMETRY'" label="采集数据" name="data">
          <div class="content-section">
            <div class="section-title">数据趋势与可视化</div>
            <div class="section-body">
              <ReportTrendPanel v-if="reportTrend.length" :reports="reportTrend" />
              <el-empty v-else description="该设备还没有上报采集数据" />
            </div>
          </div>
          <div class="content-section">
            <div class="section-title">采集数据整理结果</div>
            <el-table :data="reports">
              <el-table-column prop="reportedAt" label="上报时间" min-width="160" />
              <el-table-column label="状态" width="110"><template #default="{ row }"><StatusTag group="deviceStatus" :value="row.status" /></template></el-table-column>
              <el-table-column label="温度" width="100"><template #default="{ row }">{{ formatMetric(row.temperature, '℃') }}</template></el-table-column>
              <el-table-column label="湿度" width="100"><template #default="{ row }">{{ formatMetric(row.humidity, '%') }}</template></el-table-column>
              <el-table-column label="电压" width="100"><template #default="{ row }">{{ formatMetric(row.voltage, 'V') }}</template></el-table-column>
              <el-table-column label="信号" width="110"><template #default="{ row }">{{ formatMetric(row.signalStrength, 'dBm') }}</template></el-table-column>
              <el-table-column prop="message" label="整理说明" min-width="180" show-overflow-tooltip />
            </el-table>
            <div class="section-body page-actions">
              <el-pagination
                v-model:current-page="reportPage"
                v-model:page-size="reportPageSize"
                background
                layout="total, sizes, prev, pager, next"
                :total="reportTotal"
                @change="loadReports"
              />
            </div>
          </div>
          <div class="content-section">
            <div class="section-title">AI 分析（后续）</div>
            <div class="section-body">
              <el-alert type="info" :closable="false" title="后续可在这里接入异常趋势解释、日报摘要和预测分析；当前阶段先保证原始数据、整理结果和图表可靠。" />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <LogFormDialog :visible="logDialogVisible" mode="create" :default-device-id="device?.id" @cancel="logDialogVisible = false" @success="afterLogSaved" />
    <el-dialog v-model="componentDialogVisible" :title="editingComponent ? '编辑零件' : '新增零件'" width="500px">
      <el-form :model="componentForm" label-width="88px"><el-form-item label="零件名称" required><el-input v-model="componentForm.name" /></el-form-item><el-form-item label="类别"><el-input v-model="componentForm.category" placeholder="如：显示、主控、音频、电源" /></el-form-item><el-form-item label="型号/规格"><el-input v-model="componentForm.model" /></el-form-item><el-form-item label="数量"><el-input-number v-model="componentForm.quantity" :min="1" /></el-form-item><el-form-item label="备注"><el-input v-model="componentForm.notes" type="textarea" /></el-form-item></el-form>
      <template #footer><el-button @click="componentDialogVisible = false">取消</el-button><el-button type="primary" @click="saveComponent">保存</el-button></template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { Back, DocumentAdd } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import LogFormDialog from '@/components/LogFormDialog.vue'
import ReportTrendPanel from '@/components/ReportTrendPanel.vue'
import DeviceLogsPanel from '@/components/DeviceLogsPanel.vue'
import ReminderTable from '@/components/ReminderTable.vue'
import { getDeviceDetail } from '@/api/devices'
import { getMcpToolExecutions, type McpToolExecution } from '@/api/mcpToolExecutions'
import { getDeviceReportList } from '@/api/reports'
import type { Device } from '@/types/device'
import type { DeviceReport } from '@/types/report'
import type { DeviceComponent, DeviceComponentPayload } from '@/types/component'
import { createDeviceComponent, deleteDeviceComponent, getDeviceComponents, updateDeviceComponent } from '@/api/components'
import { PAGE_REFRESH_INTERVAL_MS } from '@/constants/refresh'
import { usePageAutoRefresh } from '@/utils/autoRefresh'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const device = ref<Device>()
const activeTab = ref<'overview' | 'components' | 'logs' | 'data' | 'mcp' | 'reminders'>('overview')
const mcpExecutions = ref<McpToolExecution[]>([])
const reportTrend = ref<DeviceReport[]>([])
const reports = ref<DeviceReport[]>([])
const reportPage = ref(1)
const reportPageSize = ref(10)
const reportTotal = ref(0)
const logDialogVisible = ref(false)
const components = ref<DeviceComponent[]>([])
const componentDialogVisible = ref(false)
const editingComponent = ref<DeviceComponent>()
const componentForm = reactive<DeviceComponentPayload>({ name: '', category: '', model: '', quantity: 1, notes: '' })
const latestReport = computed(() => device.value?.recentReports?.[0])
let requestPending = false

async function loadData(showLoading = true) {
  if (requestPending) return
  requestPending = true
  if (showLoading) loading.value = true
  try {
    const deviceId = Number(route.params.id)
    device.value = await getDeviceDetail(deviceId)
    components.value = await getDeviceComponents(deviceId)
    mcpExecutions.value = await getMcpToolExecutions()
    if (device.value.monitoringMode === 'TELEMETRY') {
      await loadReports()
    }
  } finally {
    requestPending = false
    if (showLoading) loading.value = false
  }
}

usePageAutoRefresh({ intervalMs: PAGE_REFRESH_INTERVAL_MS, isHidden: () => document.hidden, isPending: () => requestPending, refresh: () => void loadData(false) })

async function loadReports() {
  const deviceId = Number(route.params.id)
  const [page, trendPage] = await Promise.all([
    getDeviceReportList({ page: reportPage.value, pageSize: reportPageSize.value, deviceId }),
    getDeviceReportList({ page: 1, pageSize: 100, deviceId })
  ])
  reports.value = page.records
  reportTotal.value = page.total
  reportTrend.value = trendPage.records
}

function afterLogSaved() {
  logDialogVisible.value = false
  void loadData()
}

function openComponentDialog(component?: DeviceComponent) {
  editingComponent.value = component
  componentForm.name = component?.name || ''
  componentForm.category = component?.category || ''
  componentForm.model = component?.model || ''
  componentForm.quantity = component?.quantity || 1
  componentForm.notes = component?.notes || ''
  componentDialogVisible.value = true
}

async function saveComponent() {
  if (!componentForm.name.trim()) return ElMessage.warning('请输入零件名称')
  const deviceId = Number(route.params.id)
  if (editingComponent.value) await updateDeviceComponent(deviceId, editingComponent.value.id, componentForm)
  else await createDeviceComponent(deviceId, componentForm)
  componentDialogVisible.value = false
  components.value = await getDeviceComponents(deviceId)
  ElMessage.success('零件清单已保存')
}

async function removeComponent(id: number) {
  await ElMessageBox.confirm('确定删除该零件吗？', '删除确认', { type: 'warning' })
  await deleteDeviceComponent(Number(route.params.id), id)
  components.value = await getDeviceComponents(Number(route.params.id))
  ElMessage.success('已删除')
}

function formatMetric(value: number | undefined, unit: string) {
  return value === undefined || value === null ? '-' : `${value}${unit}`
}

onMounted(loadData)
</script>

<style scoped>
.device-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr));
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.summary-label {
  margin-bottom: 6px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.device-tabs {
  margin-top: 16px;
}

.detail-stats {
  grid-template-columns: repeat(3, minmax(120px, 1fr));
}
.component-actions { margin: 12px 0; }

@media (max-width: 900px) {
  .device-summary {
    grid-template-columns: repeat(2, minmax(140px, 1fr));
  }
}
</style>
