<template>
  <PageContainer title="设备中心" description="先添加设备，再进入设备工作台查看它自己的日志与采集数据">
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增设备</el-button>
    </template>

    <div class="content-section">
      <div class="section-body">
        <div class="filter-bar">
          <el-input v-model="query.keyword" clearable placeholder="设备名称或编号" />
          <el-input v-model="query.type" clearable placeholder="设备类型" />
          <el-select v-model="query.status" clearable placeholder="设备状态">
            <el-option v-for="item in enumStore.enums.deviceStatus" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <div class="page-actions">
            <el-button :icon="Search" @click="loadData">查询</el-button>
            <el-button :icon="Refresh" @click="reset">重置</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="content-section">
      <el-table v-loading="loading" :data="devices" row-key="id" class="device-table" @row-click="openDevice">
        <el-table-column label="设备名称" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click.stop="openDevice(row)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="deviceCode" label="设备编号" min-width="150" />
        <el-table-column prop="type" label="设备类型" min-width="130" />
        <el-table-column label="内容" min-width="150">
          <template #default="{ row }">{{ row.monitoringMode === 'TELEMETRY' ? '日志 + 采集数据' : '运行日志' }}</template>
        </el-table-column>
        <el-table-column prop="location" label="安装位置" min-width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag group="deviceStatus" :value="row.status" /></template>
        </el-table-column>
        <el-table-column prop="lastOnlineAt" label="最后在线" min-width="150" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="table-actions" @click.stop>
              <el-button size="small" type="primary" :icon="View" @click="openDevice(row)">打开设备</el-button>
              <el-button size="small" :icon="Edit" @click="openEdit(row)" />
              <el-button size="small" :icon="DocumentAdd" @click="openLog(row.id)" />
              <el-popconfirm title="确认删除该设备？" @confirm="remove(row.id)">
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

    <DeviceFormDialog :visible="deviceDialogVisible" :mode="deviceDialogMode" :device="currentDevice" @cancel="deviceDialogVisible = false" @success="afterDeviceSaved" />
    <LogFormDialog :visible="logDialogVisible" mode="create" :default-device-id="defaultDeviceId" @cancel="logDialogVisible = false" @success="afterLogSaved" />
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Delete, DocumentAdd, Edit, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import DeviceFormDialog from '@/components/DeviceFormDialog.vue'
import LogFormDialog from '@/components/LogFormDialog.vue'
import { deleteDevice, getDeviceList } from '@/api/devices'
import { useEnumStore } from '@/stores/enumStore'
import type { Device, DeviceQuery } from '@/types/device'
import { PAGE_REFRESH_INTERVAL_MS } from '@/constants/refresh'
import { usePageAutoRefresh } from '@/utils/autoRefresh'

const router = useRouter()
const enumStore = useEnumStore()
const loading = ref(false)
const devices = ref<Device[]>([])
const total = ref(0)
const currentDevice = ref<Device | null>(null)
const deviceDialogVisible = ref(false)
const deviceDialogMode = ref<'create' | 'edit'>('create')
const logDialogVisible = ref(false)
const defaultDeviceId = ref<number>()
const query = reactive<DeviceQuery>({ page: 1, pageSize: 10, keyword: '', type: '', status: '' })
let requestPending = false

async function loadData(showLoading = true) {
  if (requestPending) return
  requestPending = true
  if (showLoading) loading.value = true
  try {
    const page = await getDeviceList(query)
    devices.value = page.records
    total.value = page.total
  } finally {
    requestPending = false
    if (showLoading) loading.value = false
  }
}

usePageAutoRefresh({ intervalMs: PAGE_REFRESH_INTERVAL_MS, isHidden: () => document.hidden, isPending: () => requestPending, refresh: () => void loadData(false) })

function reset() {
  Object.assign(query, { page: 1, pageSize: 10, keyword: '', type: '', status: '' })
  loadData()
}

function openCreate() {
  currentDevice.value = null
  deviceDialogMode.value = 'create'
  deviceDialogVisible.value = true
}

function openDevice(device: Device) {
  router.push(`/devices/${device.id}`)
}

function openEdit(device: Device) {
  currentDevice.value = device
  deviceDialogMode.value = 'edit'
  deviceDialogVisible.value = true
}

function openLog(deviceId: number) {
  defaultDeviceId.value = deviceId
  logDialogVisible.value = true
}

async function remove(id: number) {
  await deleteDevice(id)
  ElMessage.success('删除成功')
  loadData()
}

function afterDeviceSaved() {
  deviceDialogVisible.value = false
  loadData()
}

function afterLogSaved() {
  logDialogVisible.value = false
}

onMounted(loadData)
</script>

<style scoped>
.device-table :deep(.el-table__row) {
  cursor: pointer;
}
</style>
