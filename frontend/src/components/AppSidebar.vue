<template>
  <el-aside class="sidebar" width="224px">
    <div class="brand">
      <div class="brand-mark">AI</div>
      <div>
        <div class="brand-title">AIoT Log</div>
        <div class="brand-subtitle">运行日志管理</div>
      </div>
    </div>
    <el-menu :default-active="activePath" router class="nav-menu">
      <el-menu-item index="/dashboard">
        <el-icon><DataAnalysis /></el-icon>
        <span>首页统计</span>
      </el-menu-item>
      <el-menu-item index="/devices">
        <el-icon><Cpu /></el-icon>
        <span>设备中心</span>
      </el-menu-item>
      <el-menu-item index="/environment">
        <el-icon><Sunny /></el-icon>
        <span>环境监测</span>
      </el-menu-item>
      <el-menu-item index="/alert-rules">
        <el-icon><Bell /></el-icon>
        <span>告警规则</span>
      </el-menu-item>
      <el-menu-item index="/reminders">
        <el-icon><AlarmClock /></el-icon>
        <span>提醒管理</span>
      </el-menu-item>
      <el-menu-item index="/mqtt">
        <el-icon><Connection /></el-icon>
        <span>MQTT 状态</span>
      </el-menu-item>
      <el-menu-item index="/tags">
        <el-icon><PriceTag /></el-icon>
        <span>标签管理</span>
      </el-menu-item>
    </el-menu>
  </el-aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { AlarmClock, Bell, Connection, Cpu, DataAnalysis, PriceTag, Sunny } from '@element-plus/icons-vue'

const route = useRoute()
const activePath = computed(() => {
  if (route.path.startsWith('/devices')) return '/devices'
  if (route.path.startsWith('/environment')) return '/environment'
  if (route.path.startsWith('/logs')) return '/logs'
  if (route.path.startsWith('/alert-rules')) return '/alert-rules'
  if (route.path.startsWith('/reminders')) return '/reminders'
  if (route.path.startsWith('/mqtt')) return '/mqtt'
  return route.path
})
</script>

<style scoped>
.sidebar {
  height: 100vh;
  flex-shrink: 0;
  overflow: hidden;
  background: var(--app-sidebar);
  color: #fff;
}

.brand {
  display: flex;
  gap: 10px;
  align-items: center;
  height: 64px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  background: #2f855a;
  font-weight: 800;
}

.brand-title {
  font-size: 15px;
  font-weight: 700;
}

.brand-subtitle {
  margin-top: 2px;
  color: #aab4c2;
  font-size: 12px;
}

.nav-menu {
  height: calc(100vh - 64px);
  overflow-y: auto;
  border-right: 0;
  background: transparent;
}

:deep(.el-menu-item) {
  color: #c9d2df;
}

:deep(.el-menu-item:hover),
:deep(.el-menu-item.is-active) {
  background: rgba(47, 133, 90, 0.22);
  color: #fff;
}
</style>
