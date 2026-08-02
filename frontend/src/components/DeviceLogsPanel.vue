<template>
  <div>
    <div class="device-log-toolbar">
      <el-select v-model="query.logType" clearable placeholder="日志类型">
        <el-option v-for="item in enumStore.enums.logType" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="query.level" clearable placeholder="日志等级">
        <el-option v-for="item in enumStore.enums.logLevel" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="query.status" clearable placeholder="处理状态">
        <el-option v-for="item in enumStore.enums.logStatus" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input v-model="query.keyword" clearable placeholder="搜索标题或内容" />
      <el-button :icon="Refresh" :loading="loading" @click="loadData()">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="logs" row-key="id">
      <el-table-column label="标题" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">{{ localizeTitle(row.title) }}</template>
      </el-table-column>
      <el-table-column label="类型" width="110"><template #default="{ row }"><StatusTag group="logType" :value="row.logType" /></template></el-table-column>
      <el-table-column label="等级" width="100"><template #default="{ row }"><StatusTag group="logLevel" :value="row.level" /></template></el-table-column>
      <el-table-column label="状态" width="110"><template #default="{ row }"><StatusTag group="logStatus" :value="row.status" /></template></el-table-column>
      <el-table-column label="来源" width="110"><template #default="{ row }"><StatusTag group="logSource" :value="row.source" /></template></el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" min-width="160" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }"><el-button size="small" :icon="View" @click="showDetail(row)" /></template>
      </el-table-column>
    </el-table>

    <div class="section-body page-actions">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @change="loadData()"
      />
    </div>

    <el-dialog v-model="detailVisible" title="设备日志详情" width="680px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标题">{{ currentLog ? localizeTitle(currentLog.title) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="类型"><StatusTag group="logType" :value="currentLog?.logType" /></el-descriptions-item>
        <el-descriptions-item label="等级"><StatusTag group="logLevel" :value="currentLog?.level" /></el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag group="logStatus" :value="currentLog?.status" /></el-descriptions-item>
        <el-descriptions-item label="时间">{{ currentLog?.updatedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内容"><div class="log-content">{{ currentLog ? localizeContent(currentLog.content) : '-' }}</div></el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Refresh, View } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { getLogList } from '@/api/logs'
import { LIVE_REFRESH_INTERVAL_MS } from '@/constants/refresh'
import { useEnumStore } from '@/stores/enumStore'
import { usePageAutoRefresh } from '@/utils/autoRefresh'
import type { LogQuery, LogRecord } from '@/types/log'

const props = defineProps<{ deviceId: number }>()
const enumStore = useEnumStore()
const loading = ref(false)
const logs = ref<LogRecord[]>([])
const total = ref(0)
const detailVisible = ref(false)
const currentLog = ref<LogRecord>()
let requestPending = false
let searchTimer: ReturnType<typeof setTimeout> | undefined

const query = reactive<LogQuery>({
  page: 1,
  pageSize: 10,
  deviceId: props.deviceId,
  logType: '',
  level: '',
  status: '',
  keyword: ''
})

const titleMap: Record<string, string> = {
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
  '设备运行事件：official_protocol_connected': '官方 AI 协议已连接',
  '设备运行事件：official_protocol_disconnected': '官方 AI 协议已断开',
  '设备运行事件：official_protocol_error': '官方 AI 协议异常'
}

const contentMap: Record<string, string> = {
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
  'Official AI protocol connected': '官方 AI 协议已连接'
}

function localizeTitle(title: string) {
  return titleMap[title] ?? title
}

function localizeContent(content: string) {
  return content
    .split(/\r?\n/)
    .map((line) => {
      const match = line.match(/^【(.+?)】\s*(.*)$/)
      const text = match ? match[2].trim() : line.trim()
      return contentMap[text] ?? text
    })
    .filter(Boolean)
    .join(' → ')
}

async function loadData(showLoading = true) {
  if (requestPending) return
  requestPending = true
  if (showLoading) loading.value = true
  try {
    const page = await getLogList(query)
    logs.value = page.records
    total.value = page.total
    if (detailVisible.value && currentLog.value) {
      const latest = page.records.find((item) => item.id === currentLog.value?.id)
      if (latest) currentLog.value = latest
    }
  } finally {
    requestPending = false
    if (showLoading) loading.value = false
  }
}

function showDetail(log: LogRecord) {
  currentLog.value = log
  detailVisible.value = true
}

function scheduleSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    query.page = 1
    void loadData()
  }, 300)
}

usePageAutoRefresh({
  intervalMs: LIVE_REFRESH_INTERVAL_MS,
  isHidden: () => document.hidden,
  isPending: () => requestPending,
  refresh: () => void loadData(false)
})

watch(() => props.deviceId, (deviceId) => {
  query.deviceId = deviceId
  query.page = 1
  void loadData()
})
watch(() => [query.logType, query.level, query.status, query.keyword], scheduleSearch)

onMounted(() => {
  void loadData()
})

onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
})
</script>

<style scoped>
.device-log-toolbar {
  display: grid;
  grid-template-columns: repeat(3, minmax(130px, 180px)) minmax(200px, 1fr) auto;
  gap: 12px;
  margin-bottom: 16px;
}

.log-content {
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 980px) {
  .device-log-toolbar {
    grid-template-columns: repeat(2, minmax(140px, 1fr));
  }
}
</style>
