<template>
  <el-container class="layout">
    <AppSidebar />
    <el-container class="layout-content">
      <AppHeader />
      <el-main class="layout-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import AppSidebar from '@/components/AppSidebar.vue'
import { useEnumStore } from '@/stores/enumStore'

const enumStore = useEnumStore()

onMounted(() => {
  enumStore.loadEnums()
})
</script>

<style scoped>
.layout {
  height: 100vh;
  overflow: hidden;
}

.layout-content {
  height: 100vh;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
}

.layout-main {
  background: #f4f6f8;
  padding: 18px;
  overflow: auto;
}
</style>
