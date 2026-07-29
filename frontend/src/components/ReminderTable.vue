<template>
  <div v-loading="loading">
    <div class="toolbar">
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>
    <el-table :data="reminders" empty-text="暂无提醒记录">
      <el-table-column prop="remindAt" label="提醒时间" min-width="170" />
      <el-table-column prop="message" label="提醒内容" min-width="180" show-overflow-tooltip />
      <el-table-column v-if="!deviceCode" prop="deviceCode" label="设备编号" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="110">
        <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="triggeredAt" label="实际触发" min-width="170">
        <template #default="{ row }">{{ row.triggeredAt || '-' }}</template>
      </el-table-column>
      <el-table-column label="播报任务" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.deliveryTaskId || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'SCHEDULED'" link type="danger" @click="cancel(row)">取消</el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { cancelReminder, getReminderList } from '@/api/reminders'
import type { Reminder, ReminderStatus } from '@/types/reminder'

const props = defineProps<{ deviceCode?: string }>()
const reminders = ref<Reminder[]>([])
const loading = ref(false)

const labels: Record<ReminderStatus, string> = {
  SCHEDULED: '已安排', TRIGGERING: '触发中', PUBLISHED: '已发布', FAILED: '失败', CANCELED: '已取消'
}

function statusLabel(status: ReminderStatus) { return labels[status] || status }
function statusType(status: ReminderStatus) {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'CANCELED') return 'info'
  if (status === 'TRIGGERING') return 'warning'
  return 'primary'
}

async function load() {
  loading.value = true
  try { reminders.value = await getReminderList(props.deviceCode) } finally { loading.value = false }
}

async function cancel(reminder: Reminder) {
  await ElMessageBox.confirm(`取消“${reminder.message}”的提醒？`, '确认取消', { type: 'warning', confirmButtonText: '取消提醒', cancelButtonText: '返回' })
  await cancelReminder(reminder.id)
  ElMessage.success('提醒已取消')
  await load()
}

watch(() => props.deviceCode, load, { immediate: true })
</script>

<style scoped>
.toolbar { display: flex; justify-content: flex-end; margin-bottom: 12px; }
</style>
