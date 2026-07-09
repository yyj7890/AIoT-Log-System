<template>
  <PageContainer title="MQTT 状态" description="查看设备 MQTT 上报通道的连接和消息处理情况">
    <template #actions>
      <el-button :icon="Refresh" :loading="loading" @click="loadStatus">刷新</el-button>
    </template>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">启用状态</div>
        <div class="stat-value compact">{{ status?.enabled ? '已启用' : '未启用' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">连接状态</div>
        <div class="stat-value compact" :class="status?.connected ? 'ok' : 'bad'">
          {{ status?.connected ? '已连接' : '未连接' }}
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-label">收到消息</div>
        <div class="stat-value">{{ status?.receivedCount ?? 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">处理成功</div>
        <div class="stat-value">{{ status?.handledCount ?? 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">处理失败</div>
        <div class="stat-value">{{ status?.failedCount ?? 0 }}</div>
      </div>
    </div>

    <div class="content-section">
      <div class="section-title">连接配置</div>
      <div class="section-body">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Broker">{{ status?.brokerUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Client ID">{{ status?.clientId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订阅 Topic">{{ status?.topic || '-' }}</el-descriptions-item>
          <el-descriptions-item label="QoS">{{ status?.qos ?? '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <div class="content-section">
      <div class="section-title">运行信息</div>
      <div class="section-body">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="最近连接">{{ status?.lastConnectedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近断开">{{ status?.lastDisconnectedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近消息">{{ status?.lastMessageAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="消息 Topic">{{ status?.lastMessageTopic || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近错误" :span="2">{{ status?.lastError || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import { getMqttStatus } from '@/api/mqtt'
import type { MqttStatus } from '@/types/mqtt'

const loading = ref(false)
const status = ref<MqttStatus>()

async function loadStatus() {
  loading.value = true
  try {
    status.value = await getMqttStatus()
  } finally {
    loading.value = false
  }
}

onMounted(loadStatus)
</script>

<style scoped>
.compact {
  font-size: 24px;
}

.ok {
  color: #2f855a;
}

.bad {
  color: #c2410c;
}
</style>
