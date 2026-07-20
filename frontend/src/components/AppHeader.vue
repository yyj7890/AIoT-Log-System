<template>
  <el-header class="header">
    <div>
      <div class="system-title">AIoT 智能设备运行日志管理系统</div>
      <div class="page-title">{{ pageTitle }}</div>
    </div>
    <div class="header-meta">
      <span class="runtime-label">系统运行时长 {{ formattedUptime }}</span>
      <el-tag effect="plain" type="success">后端已接入</el-tag>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getSystemRuntime } from '@/api/system'

const route = useRoute()
const pageTitle = computed(() => String(route.meta.title || ''))
const uptimeSeconds = ref<number | null>(null)
let uptimeTimer: ReturnType<typeof setInterval> | undefined
let runtimeRequestPending = false
let ticksSinceSync = 0

const formattedUptime = computed(() => formatDuration(uptimeSeconds.value))

function formatDuration(seconds: number | null) {
  if (seconds === null) return '--'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainingSeconds = seconds % 60
  const time = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(remainingSeconds).padStart(2, '0')}`
  return days > 0 ? `${days} 天 ${time}` : time
}

async function syncRuntime() {
  if (runtimeRequestPending) return
  runtimeRequestPending = true
  try {
    const runtime = await getSystemRuntime()
    uptimeSeconds.value = runtime.uptimeSeconds
    ticksSinceSync = 0
  } catch {
    // 顶部辅助状态获取失败时保持现有显示，不影响其他页面功能。
  } finally {
    runtimeRequestPending = false
  }
}

function tickRuntime() {
  if (uptimeSeconds.value !== null) {
    uptimeSeconds.value += 1
  }
  ticksSinceSync += 1
  if (ticksSinceSync >= 60) {
    // 即使本次同步失败，也等待下一个周期再重试，避免后端暂时不可用时每秒请求。
    ticksSinceSync = 0
    void syncRuntime()
  }
}

function handlePageResume() {
  if (!document.hidden) {
    void syncRuntime()
  }
}

onMounted(() => {
  void syncRuntime()
  uptimeTimer = setInterval(tickRuntime, 1000)
  document.addEventListener('visibilitychange', handlePageResume)
  window.addEventListener('focus', handlePageResume)
  window.addEventListener('online', handlePageResume)
})

onBeforeUnmount(() => {
  if (uptimeTimer) {
    clearInterval(uptimeTimer)
  }
  document.removeEventListener('visibilitychange', handlePageResume)
  window.removeEventListener('focus', handlePageResume)
  window.removeEventListener('online', handlePageResume)
})
</script>

<style scoped>
.header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  border-bottom: 1px solid var(--app-border);
  background: #fff;
}

.system-title {
  color: #18202f;
  font-size: 16px;
  font-weight: 700;
}

.page-title {
  margin-top: 2px;
  color: var(--app-muted);
  font-size: 13px;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.runtime-label {
  color: var(--app-muted);
  font-size: 13px;
  white-space: nowrap;
}

@media (max-width: 720px) {
  .runtime-label {
    display: none;
  }
}
</style>
