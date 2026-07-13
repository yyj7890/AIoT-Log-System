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

    <div class="content-section">
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
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import { getMqttGlobalCredential, getMqttStatus, saveMqttGlobalCredential, setMqttAuthentication } from '@/api/mqtt'
import type { MqttGlobalCredentialStatus, MqttStatus } from '@/types/mqtt'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const status = ref<MqttStatus>()
const credential = ref<MqttGlobalCredentialStatus>()
const savingCredential = ref(false)
const credentialForm = ref({ username: '', password: '' })

async function loadStatus() {
  loading.value = true
  try {
    const [mqttStatus, credentialStatus] = await Promise.all([getMqttStatus(), getMqttGlobalCredential()])
    status.value = mqttStatus
    credential.value = credentialStatus
    credentialForm.value.username = credentialStatus.username || 'aiot'
  } finally {
    loading.value = false
  }
}

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

.credential-body {
  display: grid;
  gap: 16px;
}

.credential-form {
  max-width: 520px;
}
</style>
