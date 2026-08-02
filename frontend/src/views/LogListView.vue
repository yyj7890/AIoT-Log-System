<template>
  <PageContainer title="日志管理" description="筛选、创建、处理和维护设备运行日志">
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增日志</el-button>
    </template>

    <div class="content-section">
      <div class="section-body">
        <div class="filter-bar log-filter">
          <el-select v-model="query.deviceId" clearable filterable placeholder="所属设备">
            <el-option v-for="device in devices" :key="device.id" :label="`${device.name} / ${device.deviceCode}`" :value="device.id" />
          </el-select>
          <el-select v-model="query.logType" clearable placeholder="日志类型">
            <el-option v-for="item in enumStore.enums.logType" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="query.level" clearable placeholder="日志等级">
            <el-option v-for="item in enumStore.enums.logLevel" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="query.status" clearable placeholder="处理状态">
            <el-option v-for="item in enumStore.enums.logStatus" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="query.source" clearable placeholder="日志来源">
            <el-option v-for="item in enumStore.enums.logSource" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="query.tagId" clearable placeholder="标签">
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
          <el-input v-model="query.keyword" clearable placeholder="标题或内容关键词" />
          <div class="time-range-field">
            <el-date-picker
              v-model="timeRange"
              class="time-range-picker"
              type="datetimerange"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </div>
          <div class="page-actions">
            <el-button :icon="Refresh" @click="reset">重置</el-button>
            <template v-if="selectionMode">
              <el-button @click="exitSelectionMode">取消</el-button>
              <el-button type="danger" :icon="Delete" :disabled="!selectedLogs.length" @click="batchRemove">删除选中（{{ selectedLogs.length }}）</el-button>
            </template>
            <el-button v-else type="danger" :icon="Delete" @click="enterSelectionMode">批量删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="content-section">
      <el-table ref="logTableRef" v-loading="loading" :data="logs" row-key="id" @selection-change="handleSelectionChange">
        <el-table-column v-if="selectionMode" type="selection" width="52" reserve-selection />
        <el-table-column label="日志标题" min-width="210" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="log-title-cell">{{ displayRuntimeTitle(row.title) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="deviceName" label="所属设备" min-width="150" />
        <el-table-column label="类型" width="110"><template #default="{ row }"><StatusTag group="logType" :value="row.logType" /></template></el-table-column>
        <el-table-column label="等级" width="100"><template #default="{ row }"><StatusTag group="logLevel" :value="row.level" /></template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="{ row }"><StatusTag group="logStatus" :value="row.status" /></template></el-table-column>
        <el-table-column label="标签" min-width="160">
          <template #default="{ row }">
            <div class="tag-list"><el-tag v-for="tag in row.tags" :key="tag.id" size="small">{{ tag.name }}</el-tag><span v-if="!row.tags?.length" class="empty-text">-</span></div>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="110"><template #default="{ row }"><StatusTag group="logSource" :value="row.source" /></template></el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="150" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" :icon="View" @click="showDetail(row)" />
              <el-button size="small" :icon="Edit" @click="openEdit(row)" />
              <el-dropdown trigger="click" @command="handleStatusCommand(row.id, $event)">
                <el-button size="small" :icon="Switch" />
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="item in enumStore.enums.logStatus" :key="item.value" :command="item.value">{{ item.label }}</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-popconfirm title="确认删除该日志？" @confirm="remove(row.id)">
                <template #reference><el-button size="small" type="danger" :icon="Delete" /></template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="section-body page-actions">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" background layout="total, sizes, prev, pager, next" :total="total" @change="loadData()" />
      </div>
    </div>

    <LogFormDialog :visible="dialogVisible" :mode="dialogMode" :log="currentLog" @cancel="dialogVisible = false" @success="afterSaved" />
    <el-dialog v-model="detailVisible" title="日志详情" width="640px" @closed="handleDetailClosed">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标题">{{ currentLog ? displayRuntimeTitle(currentLog.title) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备">{{ currentLog?.deviceName }} / {{ currentLog?.deviceCode }}</el-descriptions-item>
        <el-descriptions-item label="类型"><StatusTag group="logType" :value="currentLog?.logType" /></el-descriptions-item>
        <el-descriptions-item label="等级"><StatusTag group="logLevel" :value="currentLog?.level" /></el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag group="logStatus" :value="currentLog?.status" /></el-descriptions-item>
        <el-descriptions-item label="来源"><StatusTag group="logSource" :value="currentLog?.source" /></el-descriptions-item>
        <el-descriptions-item label="内容">{{ currentLog ? displayRuntimeContent(currentLog.content) : '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Delete, Edit, Plus, Refresh, Switch, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import LogFormDialog from '@/components/LogFormDialog.vue'
import { deleteLog, deleteLogs, getLogDetail, getLogList, updateLogStatus } from '@/api/logs'
import { ApiRequestError } from '@/api/http'
import { LIVE_REFRESH_INTERVAL_MS } from '@/constants/refresh'
import { createAutoRefreshController } from '@/utils/autoRefresh'
import { getDeviceList } from '@/api/devices'
import { getTagList } from '@/api/tags'
import { useEnumStore } from '@/stores/enumStore'
import type { Device } from '@/types/device'
import type { LogQuery, LogRecord, LogStatus } from '@/types/log'
import type { Tag } from '@/types/tag'

const route = useRoute()
const enumStore = useEnumStore()
const loading = ref(false)
const logs = ref<LogRecord[]>([])
const logTableRef = ref<{ clearSelection: () => void } | null>(null)
const devices = ref<Device[]>([])
const tags = ref<Tag[]>([])
const total = ref(0)
const selectedLogs = ref<LogRecord[]>([])
const selectionMode = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const currentLog = ref<LogRecord | null>(null)
const timeRange = ref<[string, string] | null>(null)
const autoSearchReady = ref(false)
let autoSearchTimer: ReturnType<typeof setTimeout> | undefined
let logRequestPending = false
let detailRequestPending = false
let detailRequestLogId: number | null = null
let detailRequestVersion = 0
let detailRefreshQueued = false
const query = reactive<LogQuery>({
  page: 1,
  pageSize: 10,
  deviceId: route.query.deviceId ? Number(route.query.deviceId) : undefined,
  logType: '',
  level: '',
  status: '',
  source: '',
  tagId: undefined,
  keyword: ''
})

const runtimeTitleMap: Record<string, string> = {
  '设备运行事件：startup': '设备启动',
  '设备运行事件：firmware_started': '固件启动',
  '设备运行事件：wifi_connected': 'Wi-Fi 已连接',
  '设备运行事件：wifi_disconnected': 'Wi-Fi 已断开',
  '设备运行事件：mqtt_connected': '日志 MQTT 已连接',
  '设备运行事件：mqtt_connect_failed': '日志 MQTT 连接失败',
  '设备运行事件：mqtt_connection_failed': '日志 MQTT 连接失败',
  '设备运行事件：mqtt_reconnected': '日志 MQTT 连接已恢复',
  '设备运行事件：mqtt_connection_recovered': '日志 MQTT 连接已恢复',
  '设备运行事件：local_ai_server_discovered': '已发现本地 AI 服务',
  '设备运行事件：local_ai_connected': '已连接本地 AI 服务',
  '设备运行事件：local_ai_connection_failed': '本地 AI 服务连接失败',
  '设备运行事件：local_ai_websocket_hello_completed': '本地 AI WebSocket 握手完成',
  '设备运行事件：local_ai_discovery_failed': '未发现本地 AI 服务',
  '设备运行事件：local_ai_fallback_to_official': '已回退官方 AI',
  '设备运行事件：local_mqtt_broker_discovered': '已发现本地日志 MQTT 服务',
  '设备运行事件：mqtt_disconnected': '日志 MQTT 已断开',
  '设备运行事件：official_protocol_connected': '官方 AI 协议已连接',
  '设备运行事件：official_protocol_disconnected': '官方 AI 协议已断开',
  '设备运行事件：official_protocol_error': '官方 AI 协议异常'
}

const runtimeContentMap: Record<string, string> = {
  'Firmware initialization started': '固件开始初始化',
  'Firmware initialization completed': '固件启动完成',
  'Firmware startup completed': '固件启动完成',
  'Wi-Fi connected': 'Wi-Fi 已连接',
  'Log MQTT connected': '日志 MQTT 已连接',
  'Log MQTT connection failed and was retried': '日志 MQTT 连接失败，正在重试',
  'Log MQTT connection recovered': '日志 MQTT 连接已恢复',
  'Remote log MQTT TLS connected': '远程日志 MQTT（TLS 8883）已连接',
  'Remote log MQTT TLS connection failed and was retried': '远程日志 MQTT（TLS 8883）连接失败，正在重试',
  'Remote log MQTT TLS connection recovered': '远程日志 MQTT（TLS 8883）连接已恢复',
  'Local AI server discovered': '已发现本地 AI 服务',
  'Local AI server connected': '已连接本地 AI 服务',
  'Local AI server connection failed': '本地 AI 服务连接失败',
  'Local AI WebSocket hello completed': '本地 AI WebSocket 握手完成',
  'No valid local AI discovery response this boot': '本次启动未发现本地 AI 服务',
  'Local AI unavailable; official AI connected': '本地 AI 不可用，已回退官方 AI',
  'Official AI protocol connected': '官方 AI 协议已连接',
  'Official AI protocol connected or reconnected': '官方 AI 协议已连接或重连'
}

function displayRuntimeTitle(title: string) {
  return runtimeTitleMap[title] ?? title
}

function displayRuntimeContent(content: string) {
  const localizedContent = runtimeContentMap[content] ?? content
  const eventSummaries = localizedContent
    .split(/\r?\n/)
    .map(compactRuntimeEvent)
    .filter(Boolean)

  return eventSummaries.length > 1 ? eventSummaries.join(' → ') : (eventSummaries[0] ?? localizedContent)
}

function compactRuntimeEvent(line: string) {
  const match = line.match(/^【(.+?)】\s*(.*)$/)
  if (!match) {
    const eventContent = line.trim()
    return runtimeContentMap[eventContent] ?? eventContent
  }

  const eventTitle = match[1].trim()
  const eventContent = runtimeContentMap[match[2].trim()] ?? match[2].trim()
  return !eventContent || eventContent === eventTitle ? eventTitle : eventContent
}

async function loadOptions() {
  const [devicePage, tagList] = await Promise.all([getDeviceList({ page: 1, pageSize: 100 }), getTagList()])
  devices.value = devicePage.records
  tags.value = tagList
}

async function loadData(showLoading = true) {
  if (logRequestPending) return
  logRequestPending = true
  if (showLoading) {
    loading.value = true
  }
  try {
    query.startTime = timeRange.value?.[0]
    query.endTime = timeRange.value?.[1]
    const page = await getLogList(query)
    logs.value = page.records
    total.value = page.total
    syncOpenDetail(page.records)
  } catch {
    // 请求层已经负责提示错误；这里吞掉异常，确保首次失败不会终止轮询生命周期。
  } finally {
    logRequestPending = false
    if (showLoading) {
      loading.value = false
    }
  }
}

function scheduleAutoSearch() {
  if (!autoSearchReady.value) return
  if (autoSearchTimer) {
    clearTimeout(autoSearchTimer)
  }
  autoSearchTimer = setTimeout(() => {
    query.page = 1
    void loadData()
  }, 300)
}

function updateCurrentLog(latestLog: LogRecord) {
  if (!currentLog.value || currentLog.value.id !== latestLog.id) return
  Object.assign(currentLog.value, latestLog)
}

function syncOpenDetail(latestRecords: LogRecord[]) {
  if (!detailVisible.value || !currentLog.value) return

  const currentId = currentLog.value.id
  const latestLog = latestRecords.find((log) => log.id === currentId)
  if (latestLog) {
    detailRefreshQueued = false
    detailRequestVersion += 1
    updateCurrentLog(latestLog)
    return
  }

  void refreshLogDetail(currentId, false)
}

async function refreshLogDetail(id: number, showError: boolean) {
  if (detailRequestPending) {
    detailRefreshQueued = true
    return
  }

  detailRequestPending = true
  detailRefreshQueued = false
  detailRequestLogId = id
  const requestVersion = ++detailRequestVersion
  try {
    const latestLog = await getLogDetail(id, true)
    if (
      detailVisible.value
      && currentLog.value?.id === id
      && detailRequestVersion === requestVersion
    ) {
      updateCurrentLog(latestLog)
    }
  } catch (error) {
    if (
      error instanceof ApiRequestError
      && error.code === 404
      && detailVisible.value
      && currentLog.value?.id === id
    ) {
      detailVisible.value = false
      ElMessage.warning('该日志已被删除')
    } else if (showError) {
      ElMessage.error('日志详情加载失败')
    }
  } finally {
    if (detailRequestLogId === id) {
      detailRequestPending = false
      detailRequestLogId = null
    }

    const activeId = detailVisible.value ? currentLog.value?.id : undefined
    if (activeId !== undefined && detailRefreshQueued && !detailRequestPending) {
      void refreshLogDetail(activeId, false)
    }
  }
}

const autoRefresh = createAutoRefreshController({
  intervalMs: LIVE_REFRESH_INTERVAL_MS,
  isHidden: () => document.hidden,
  isPending: () => logRequestPending,
  refresh: () => {
    void loadData(false)
  }
})

function reset() {
  Object.assign(query, { page: 1, pageSize: 10, deviceId: undefined, logType: '', level: '', status: '', source: '', tagId: undefined, keyword: '', startTime: undefined, endTime: undefined })
  timeRange.value = null
  void loadData()
}

function openCreate() {
  currentLog.value = null
  dialogMode.value = 'create'
  dialogVisible.value = true
}

function openEdit(log: LogRecord) {
  currentLog.value = log
  dialogMode.value = 'edit'
  dialogVisible.value = true
}

function showDetail(log: LogRecord) {
  currentLog.value = log
  detailVisible.value = true
  detailRequestVersion += 1
  void refreshLogDetail(log.id, true)
}

function handleDetailClosed() {
  detailRefreshQueued = false
  detailRequestVersion += 1
}

async function changeStatus(id: number, status: LogStatus) {
  await updateLogStatus(id, status)
  ElMessage.success('状态已更新')
  void loadData()
}

function handleStatusCommand(id: number, command: string | number | object) {
  changeStatus(id, command as LogStatus)
}

async function remove(id: number) {
  await deleteLog(id)
  ElMessage.success('删除成功')
  void loadData()
}

function handleSelectionChange(selection: LogRecord[]) {
  selectedLogs.value = selectionMode.value ? selection : []
}

function enterSelectionMode() {
  selectionMode.value = true
}

function exitSelectionMode() {
  logTableRef.value?.clearSelection()
  selectedLogs.value = []
  selectionMode.value = false
}

async function batchRemove() {
  const ids = selectedLogs.value.map((log) => log.id)
  if (!ids.length) return

  await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 条日志吗？此操作不可恢复。`, '批量删除日志', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteLogs(ids)
  exitSelectionMode()
  ElMessage.success(`已删除 ${ids.length} 条日志`)
  void loadData()
}

function afterSaved() {
  dialogVisible.value = false
  void loadData()
}

onMounted(() => {
  document.addEventListener('visibilitychange', autoRefresh.handleVisibilityChange)
  window.addEventListener('focus', autoRefresh.resume)
  window.addEventListener('online', autoRefresh.resume)
  window.addEventListener('pageshow', autoRefresh.resume)
  autoRefresh.start()
  autoSearchReady.value = true
  void loadOptions().catch(() => undefined)
  void loadData()
})

onBeforeUnmount(() => {
  if (autoSearchTimer) {
    clearTimeout(autoSearchTimer)
  }
  autoRefresh.stop()
  document.removeEventListener('visibilitychange', autoRefresh.handleVisibilityChange)
  window.removeEventListener('focus', autoRefresh.resume)
  window.removeEventListener('online', autoRefresh.resume)
  window.removeEventListener('pageshow', autoRefresh.resume)
})

watch(
  () => [query.deviceId, query.logType, query.level, query.status, query.source, query.tagId, query.keyword, timeRange.value?.[0], timeRange.value?.[1]],
  scheduleAutoSearch
)
</script>

<style scoped>
.log-filter {
  grid-template-columns: repeat(4, minmax(160px, 1fr));
}

.time-range-field {
  grid-column: span 2;
  min-width: 0;
}

.time-range-picker {
  width: 100%;
  min-width: 0;
}

.time-range-picker :deep(.el-range-input) {
  min-width: 0;
}

.log-title-cell {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .log-filter {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }
}

@media (max-width: 720px) {
  .log-filter {
    grid-template-columns: 1fr;
  }

  .time-range-field {
    grid-column: span 1;
  }
}
</style>
