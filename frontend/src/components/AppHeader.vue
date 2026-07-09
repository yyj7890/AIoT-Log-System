<template>
  <el-header class="header">
    <div>
      <div class="system-title">AIoT 智能设备运行日志管理系统</div>
      <div class="page-title">{{ pageTitle }}</div>
    </div>
    <div class="header-meta">
      <el-tag effect="plain" type="success">后端已接入</el-tag>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const pageTitle = computed(() => String(route.meta.title || ''))
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
</style>
