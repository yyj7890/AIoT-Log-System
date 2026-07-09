<template>
  <el-dialog :model-value="visible" :title="mode === 'create' ? '新增设备' : '编辑设备'" width="560px" @close="emit('cancel')">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item label="设备名称" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="设备编号" prop="deviceCode">
        <el-input v-model="form.deviceCode" :disabled="mode === 'edit'" />
      </el-form-item>
      <el-form-item label="设备类型" prop="type">
        <el-input v-model="form.type" />
      </el-form-item>
      <el-form-item label="安装位置">
        <el-input v-model="form.location" />
      </el-form-item>
      <el-form-item label="当前状态" prop="status">
        <el-select v-model="form.status" class="full">
          <el-option v-for="item in enumStore.enums.deviceStatus" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="设备描述">
        <el-input v-model="form.description" type="textarea" :rows="3" />
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
import { createDevice, updateDevice } from '@/api/devices'
import { useEnumStore } from '@/stores/enumStore'
import type { Device, DevicePayload, DeviceStatus } from '@/types/device'

const props = defineProps<{
  visible: boolean
  mode: 'create' | 'edit'
  device?: Device | null
}>()

const emit = defineEmits<{
  success: []
  cancel: []
}>()

const enumStore = useEnumStore()
const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive<DevicePayload>({
  name: '',
  deviceCode: '',
  type: '',
  location: '',
  status: 'NORMAL',
  description: ''
})

const rules: FormRules<DevicePayload> = {
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  deviceCode: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  type: [{ required: true, message: '请输入设备类型', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

watch(
  () => [props.visible, props.device],
  () => {
    form.name = props.device?.name || ''
    form.deviceCode = props.device?.deviceCode || ''
    form.type = props.device?.type || ''
    form.location = props.device?.location || ''
    form.status = (props.device?.status || 'NORMAL') as DeviceStatus
    form.description = props.device?.description || ''
  },
  { immediate: true }
)

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (props.mode === 'edit' && props.device) {
      await updateDevice(props.device.id, form)
    } else {
      await createDevice(form)
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
