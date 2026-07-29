<template>
  <PageContainer title="提醒管理" description="查看小智创建的提醒、播报状态，并取消尚未触发的提醒。">
    <el-alert type="info" :closable="false" title="提醒由小智语音创建；这里用于查询和取消。已发布不等同于已播放，最终以设备 ACK 与实际播报为准。" />
    <div class="filters">
      <el-input v-model="deviceCode" clearable placeholder="按设备编号筛选（可选）" @clear="deviceCode = ''" />
      <el-button type="primary" @click="appliedDeviceCode = deviceCode.trim()">查询</el-button>
    </div>
    <div class="content-section"><ReminderTable :device-code="appliedDeviceCode || undefined" /></div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import PageContainer from '@/components/PageContainer.vue'
import ReminderTable from '@/components/ReminderTable.vue'

const deviceCode = ref('')
const appliedDeviceCode = ref('')
</script>

<style scoped>
.filters { display: flex; gap: 12px; max-width: 520px; margin: 16px 0; }
</style>
