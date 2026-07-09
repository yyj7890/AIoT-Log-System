<template>
  <el-tag :type="tagType" effect="light" round>{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useEnumStore } from '@/stores/enumStore'
import type { EnumMap } from '@/types/api'

const props = defineProps<{
  group: keyof EnumMap
  value?: string
}>()

const enumStore = useEnumStore()

const label = computed(() => enumStore.label(props.group, props.value))
const tagType = computed(() => {
  if (['NORMAL', 'RESOLVED'].includes(props.value || '')) return 'success'
  if (['ABNORMAL', 'ERROR'].includes(props.value || '')) return 'danger'
  if (['WARNING', 'PENDING'].includes(props.value || '')) return 'warning'
  if (['OFFLINE'].includes(props.value || '')) return 'info'
  if (['DEVICE'].includes(props.value || '')) return 'warning'
  if (['SYSTEM'].includes(props.value || '')) return 'info'
  return 'primary'
})
</script>
