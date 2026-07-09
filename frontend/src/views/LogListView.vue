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
          </div>
        </div>
      </div>
    </div>

    <div class="content-section">
      <el-table v-loading="loading" :data="logs" row-key="id">
        <el-table-column prop="title" label="日志标题" min-width="180" />
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
        <el-table-column prop="createdAt" label="创建时间" min-width="150" />
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
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" background layout="total, sizes, prev, pager, next" :total="total" @change="loadData" />
      </div>
    </div>

    <LogFormDialog :visible="dialogVisible" :mode="dialogMode" :log="currentLog" @cancel="dialogVisible = false" @success="afterSaved" />
    <el-dialog v-model="detailVisible" title="日志详情" width="640px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标题">{{ currentLog?.title }}</el-descriptions-item>
        <el-descriptions-item label="设备">{{ currentLog?.deviceName }} / {{ currentLog?.deviceCode }}</el-descriptions-item>
        <el-descriptions-item label="类型"><StatusTag group="logType" :value="currentLog?.logType" /></el-descriptions-item>
        <el-descriptions-item label="等级"><StatusTag group="logLevel" :value="currentLog?.level" /></el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag group="logStatus" :value="currentLog?.status" /></el-descriptions-item>
        <el-descriptions-item label="来源"><StatusTag group="logSource" :value="currentLog?.source" /></el-descriptions-item>
        <el-descriptions-item label="内容">{{ currentLog?.content }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Delete, Edit, Plus, Refresh, Switch, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import LogFormDialog from '@/components/LogFormDialog.vue'
import { deleteLog, getLogList, updateLogStatus } from '@/api/logs'
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
const devices = ref<Device[]>([])
const tags = ref<Tag[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const currentLog = ref<LogRecord | null>(null)
const timeRange = ref<[string, string] | null>(null)
const autoSearchReady = ref(false)
let autoSearchTimer: ReturnType<typeof setTimeout> | undefined
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

async function loadOptions() {
  const [devicePage, tagList] = await Promise.all([getDeviceList({ page: 1, pageSize: 100 }), getTagList()])
  devices.value = devicePage.records
  tags.value = tagList
}

async function loadData() {
  loading.value = true
  try {
    query.startTime = timeRange.value?.[0]
    query.endTime = timeRange.value?.[1]
    const page = await getLogList(query)
    logs.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function scheduleAutoSearch() {
  if (!autoSearchReady.value) return
  if (autoSearchTimer) {
    clearTimeout(autoSearchTimer)
  }
  autoSearchTimer = setTimeout(() => {
    query.page = 1
    loadData()
  }, 300)
}

function reset() {
  Object.assign(query, { page: 1, pageSize: 10, deviceId: undefined, logType: '', level: '', status: '', source: '', tagId: undefined, keyword: '', startTime: undefined, endTime: undefined })
  timeRange.value = null
  loadData()
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
}

async function changeStatus(id: number, status: LogStatus) {
  await updateLogStatus(id, status)
  ElMessage.success('状态已更新')
  loadData()
}

function handleStatusCommand(id: number, command: string | number | object) {
  changeStatus(id, command as LogStatus)
}

async function remove(id: number) {
  await deleteLog(id)
  ElMessage.success('删除成功')
  loadData()
}

function afterSaved() {
  dialogVisible.value = false
  loadData()
}

onMounted(async () => {
  await loadOptions()
  await loadData()
  autoSearchReady.value = true
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
