<template>
  <PageContainer title="MQTT 状态" description="查看设备 MQTT 上报通道的连接和消息处理情况">
    <template #actions>
      <el-button :icon="Refresh" :loading="loading" @click="loadStatus()">刷新</el-button>
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
          <el-descriptions-item label="连接模式">{{ status?.mode === 'remote' ? '远程 HiveMQ TLS' : '局域网 Mosquitto' }}</el-descriptions-item>
          <el-descriptions-item label="Client ID">{{ status?.clientId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="上报 Topic">{{ status?.topic || '-' }}</el-descriptions-item>
          <el-descriptions-item label="日志 Topic">{{ status?.logTopic || '-' }}</el-descriptions-item>
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

    <div v-if="status?.mode === 'remote'" class="content-section">
      <div class="section-title">远程 HiveMQ 凭证</div>
      <div class="section-body credential-body">
        <el-alert
          type="warning"
          :closable="false"
          title="保存后后端会立即使用新凭证重连。密码只写入宿主机私有配置文件，页面和接口都不会回显。"
        />
        <el-descriptions :column="2" border>
          <el-descriptions-item label="当前账号">{{ remoteCredential?.username || '未配置' }}</el-descriptions-item>
          <el-descriptions-item label="密码状态">{{ remoteCredential?.passwordConfigured ? '已配置' : '未配置' }}</el-descriptions-item>
          <el-descriptions-item label="持久化来源" :span="2">
            {{ remoteCredential?.runtimeOverrideEnabled ? 'MQTT 页面私有配置（重启后继续生效）' : '部署环境私有配置' }}
          </el-descriptions-item>
        </el-descriptions>
        <el-form :model="remoteCredentialForm" label-width="120px" class="credential-form" @submit.prevent>
          <el-form-item label="HiveMQ 用户名">
            <el-input v-model="remoteCredentialForm.username" autocomplete="off" />
          </el-form-item>
          <el-form-item label="HiveMQ 密码">
            <el-input v-model="remoteCredentialForm.password" type="password" show-password autocomplete="new-password" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingRemoteCredential" @click="saveRemoteCredential">保存并立即重连</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div v-else class="content-section">
      <div class="section-title">全局设备 MQTT 凭证</div>
      <div class="section-body credential-body">
        <el-alert
          type="warning"
          :closable="false"
          title="所有设备共用这一套凭证；设备未填写或填写错误时，只会停止日志上传，不影响设备启动和 AI 功能。"
        />
        <el-descriptions :column="2" border>
          <el-descriptions-item label="当前账号">{{ credential?.username || '未配置' }}</el-descriptions-item>
          <el-descriptions-item label="密码状态">{{ credential?.passwordConfigured ? '已配置' : '未配置' }}</el-descriptions-item>
          <el-descriptions-item label="匿名访问">{{ credential?.anonymousAccessEnabled ? '临时开启' : '已关闭' }}</el-descriptions-item>
          <el-descriptions-item label="认证切换">{{ credential?.activationPending ? '等待设备保存凭证后启用' : '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-form :model="credentialForm" label-width="110px" class="credential-form" @submit.prevent>
          <el-form-item label="MQTT 用户名"><el-input v-model="credentialForm.username" autocomplete="off" /></el-form-item>
          <el-form-item label="MQTT 密码"><el-input v-model="credentialForm.password" type="password" show-password autocomplete="new-password" /></el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingCredential" @click="saveCredential">保存全局凭证</el-button>
            <el-button type="danger" :disabled="!credential?.passwordConfigured || !credential?.anonymousAccessEnabled" @click="enableAuthentication">关闭匿名访问并启用认证</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import {
  getMqttGlobalCredential,
  getMqttRemoteCredential,
  getMqttStatus,
  saveMqttGlobalCredential,
  saveMqttRemoteCredential,
  setMqttAuthentication
} from '@/api/mqtt'
import { LIVE_REFRESH_INTERVAL_MS } from '@/constants/refresh'
import { createAutoRefreshController } from '@/utils/autoRefresh'
import type { MqttGlobalCredentialStatus, MqttRemoteCredentialStatus, MqttStatus } from '@/types/mqtt'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const status = ref<MqttStatus>()
const credential = ref<MqttGlobalCredentialStatus>()
const remoteCredential = ref<MqttRemoteCredentialStatus>()
const savingCredential = ref(false)
const savingRemoteCredential = ref(false)
const credentialForm = ref({ username: '', password: '' })
const remoteCredentialForm = ref({ username: '', password: '' })
let statusRequestPending = false

async function loadStatus(showLoading = true, includeCredential = true) {
  if (statusRequestPending) return
  statusRequestPending = true
  if (showLoading) {
    loading.value = true
  }
  try {
    const mqttStatus = await getMqttStatus()
    status.value = mqttStatus
    if (mqttStatus.mode === 'remote') {
      credential.value = undefined
      if (includeCredential) {
        const remoteStatus = await getMqttRemoteCredential()
        remoteCredential.value = remoteStatus
        remoteCredentialForm.value.username = remoteStatus.username || ''
      }
      return
    }
    if (includeCredential) {
      const credentialStatus = await getMqttGlobalCredential()
      credential.value = credentialStatus
      credentialForm.value.username = credentialStatus.username || 'aiot'
    }
  } catch {
    // The shared HTTP interceptor already displays the request error.
  } finally {
    statusRequestPending = false
    if (showLoading) {
      loading.value = false
    }
  }
}

async function saveRemoteCredential() {
  if (!remoteCredentialForm.value.username || !remoteCredentialForm.value.password) {
    ElMessage.warning('请填写 HiveMQ 用户名和密码')
    return
  }
  await ElMessageBox.confirm(
    '保存后后端会立即断开当前 MQTT 连接并使用新凭证重连。确认继续吗？',
    '更新远程 HiveMQ 凭证',
    { type: 'warning', confirmButtonText: '保存并重连', cancelButtonText: '取消' }
  )
  savingRemoteCredential.value = true
  try {
    remoteCredential.value = await saveMqttRemoteCredential(remoteCredentialForm.value)
    remoteCredentialForm.value.password = ''
    await loadStatus(true, false)
    ElMessage.success(status.value?.connected ? '远程凭证已保存，HiveMQ 已重新连接' : '凭证已保存，正在等待 HiveMQ 重连')
  } finally {
    savingRemoteCredential.value = false
  }
}

const autoRefresh = createAutoRefreshController({
  intervalMs: LIVE_REFRESH_INTERVAL_MS,
  isHidden: () => document.hidden,
  isPending: () => statusRequestPending,
  refresh: () => {
    void loadStatus(false, false)
  }
})

async function saveCredential() {
  if (!credentialForm.value.username || !credentialForm.value.password) {
    ElMessage.warning('请填写 MQTT 用户名和密码')
    return
  }
  savingCredential.value = true
  try {
    credential.value = await saveMqttGlobalCredential(credentialForm.value)
    credentialForm.value.password = ''
    ElMessage.success('全局 MQTT 凭证已保存。请在设备配网页填写同一套凭证后，再关闭匿名访问。')
  } finally {
    savingCredential.value = false
  }
}

async function enableAuthentication() {
  await ElMessageBox.confirm(
    '请确认所有需要上传日志的设备均已保存这套全局 MQTT 凭证。保存后仍需执行 stop-all.cmd 再执行 start-all.cmd，认证才会生效。',
    '启用 MQTT 认证',
    { type: 'warning', confirmButtonText: '确认启用', cancelButtonText: '取消' }
  )
  credential.value = await setMqttAuthentication(true)
  ElMessage.success('认证配置已写入。请重启本地 IoT 服务使其生效。')
}

onMounted(() => {
  void loadStatus()
  autoRefresh.start()
  document.addEventListener('visibilitychange', autoRefresh.handleVisibilityChange)
})

onBeforeUnmount(() => {
  autoRefresh.stop()
  document.removeEventListener('visibilitychange', autoRefresh.handleVisibilityChange)
})
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

.credential-body {
  display: grid;
  gap: 16px;
}

.credential-form {
  max-width: 520px;
}
</style>
