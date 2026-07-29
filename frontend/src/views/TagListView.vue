<template>
  <PageContainer title="标签管理" description="维护日志分类标签">
    <div class="content-section">
      <div class="section-body tag-create">
        <el-input v-model="form.name" placeholder="标签名称" clearable />
        <el-color-picker v-model="form.color" />
        <el-button type="primary" :icon="Plus" :loading="saving" @click="submit">新增标签</el-button>
      </div>
    </div>

    <div class="content-section">
      <el-table v-loading="loading" :data="tags" row-key="id">
        <el-table-column label="标签名称" min-width="180">
          <template #default="{ row }">
            <el-tag :color="row.color" effect="plain">{{ row.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-popconfirm title="确认删除该标签？" @confirm="remove(row.id)">
              <template #reference><el-button size="small" type="danger" :icon="Delete" /></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import { createTag, deleteTag, getTagList } from '@/api/tags'
import type { Tag } from '@/types/tag'
import { PAGE_REFRESH_INTERVAL_MS } from '@/constants/refresh'
import { usePageAutoRefresh } from '@/utils/autoRefresh'

const loading = ref(false)
const saving = ref(false)
const tags = ref<Tag[]>([])
const form = reactive({ name: '', color: '#409EFF' })
let requestPending = false

async function loadData(showLoading = true) {
  if (requestPending) return
  requestPending = true
  if (showLoading) loading.value = true
  try {
    tags.value = await getTagList()
  } finally {
    requestPending = false
    if (showLoading) loading.value = false
  }
}

usePageAutoRefresh({ intervalMs: PAGE_REFRESH_INTERVAL_MS, isHidden: () => document.hidden, isPending: () => requestPending, refresh: () => void loadData(false) })

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入标签名称')
    return
  }
  saving.value = true
  try {
    await createTag({ name: form.name.trim(), color: form.color })
    ElMessage.success('新增成功')
    form.name = ''
    await loadData()
  } finally {
    saving.value = false
  }
}

async function remove(id: number) {
  await deleteTag(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.tag-create {
  display: grid;
  grid-template-columns: minmax(220px, 360px) 40px auto;
  gap: 12px;
  align-items: center;
  justify-content: start;
}
</style>
