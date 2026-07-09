<template>
  <el-dialog :model-value="visible" :title="mode === 'create' ? '新增日志' : '编辑日志'" width="640px" @close="emit('cancel')">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item label="所属设备" prop="deviceId">
        <el-select v-model="form.deviceId" class="full" filterable>
          <el-option v-for="device in devices" :key="device.id" :label="`${device.name} / ${device.deviceCode}`" :value="device.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="日志标题" prop="title">
        <el-input v-model="form.title" />
      </el-form-item>
      <el-form-item label="日志内容" prop="content">
        <el-input v-model="form.content" type="textarea" :rows="4" />
      </el-form-item>
      <el-form-item label="日志类型" prop="logType">
        <el-select v-model="form.logType" class="full">
          <el-option v-for="item in enumStore.enums.logType" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="日志等级" prop="level">
        <el-select v-model="form.level" class="full">
          <el-option v-for="item in enumStore.enums.logLevel" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="处理状态" prop="status">
        <el-select v-model="form.status" class="full">
          <el-option v-for="item in enumStore.enums.logStatus" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="标签">
        <el-select v-model="form.tagIds" class="full" multiple collapse-tags collapse-tags-tooltip>
          <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('cancel')">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { createLog, updateLog } from '@/api/logs'
import { getDeviceList } from '@/api/devices'
import { getTagList } from '@/api/tags'
import { useEnumStore } from '@/stores/enumStore'
import type { Device } from '@/types/device'
import type { LogLevel, LogPayload, LogRecord, LogStatus, LogType } from '@/types/log'
import type { Tag } from '@/types/tag'

const props = defineProps<{
  visible: boolean
  mode: 'create' | 'edit'
  log?: LogRecord | null
  defaultDeviceId?: number
}>()

const emit = defineEmits<{
  success: []
  cancel: []
}>()

const enumStore = useEnumStore()
const formRef = ref<FormInstance>()
const saving = ref(false)
const devices = ref<Device[]>([])
const tags = ref<Tag[]>([])
const form = reactive<LogPayload>({
  deviceId: 0,
  title: '',
  content: '',
  logType: 'RUNNING',
  level: 'INFO',
  status: 'PENDING',
  tagIds: []
})

const rules: FormRules<LogPayload> = {
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  logType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  level: [{ required: true, message: '请选择等级', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

watch(
  () => props.visible,
  async (visible) => {
    if (!visible) return
    const [devicePage, tagList] = await Promise.all([getDeviceList({ page: 1, pageSize: 100 }), getTagList()])
    devices.value = devicePage.records
    tags.value = tagList
    form.deviceId = props.log?.deviceId || props.defaultDeviceId || devices.value[0]?.id || 0
    form.title = props.log?.title || ''
    form.content = props.log?.content || ''
    form.logType = (props.log?.logType || 'RUNNING') as LogType
    form.level = (props.log?.level || 'INFO') as LogLevel
    form.status = (props.log?.status || 'PENDING') as LogStatus
    form.tagIds = props.log?.tags?.map((tag) => tag.id) || []
  }
)

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (props.mode === 'edit' && props.log) {
      await updateLog(props.log.id, form)
    } else {
      await createLog(form)
    }
    ElMessage.success('保存成功')
    emit('success')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
