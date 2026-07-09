<template>
  <PageContainer title="告警规则" description="配置设备上报数据的异常判断阈值">
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增规则</el-button>
    </template>

    <div class="content-section">
      <div class="section-body">
        <div class="filter-bar rule-filter">
          <el-select v-model="query.deviceId" clearable filterable placeholder="适用设备">
            <el-option v-for="device in devices" :key="device.id" :label="`${device.name} / ${device.deviceCode}`" :value="device.id" />
          </el-select>
          <el-select v-model="query.enabled" clearable placeholder="启用状态">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
          <div class="page-actions">
            <el-button :icon="Search" @click="loadData">查询</el-button>
            <el-button :icon="Refresh" @click="reset">重置</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="content-section">
      <el-table v-loading="loading" :data="rules" row-key="id">
        <el-table-column prop="name" label="规则名称" min-width="160" />
        <el-table-column label="适用设备" min-width="180">
          <template #default="{ row }">{{ row.deviceName ? `${row.deviceName} / ${row.deviceCode}` : '全部设备' }}</template>
        </el-table-column>
        <el-table-column label="指标" width="110"><template #default="{ row }">{{ enumStore.label('alertMetric', row.metric) }}</template></el-table-column>
        <el-table-column label="条件" width="110"><template #default="{ row }">{{ enumStore.label('alertOperator', row.operator) }}</template></el-table-column>
        <el-table-column prop="thresholdValue" label="阈值" width="100" />
        <el-table-column label="等级" width="100"><template #default="{ row }"><StatusTag group="logLevel" :value="row.level" /></template></el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'" effect="light">{{ row.enabled ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="150" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" :icon="Edit" @click="openEdit(row)" />
              <el-popconfirm title="确认删除该规则？" @confirm="remove(row.id)">
                <template #reference><el-button size="small" type="danger" :icon="Delete" /></template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增告警规则' : '编辑告警规则'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rulesConfig" label-width="96px">
        <el-form-item label="规则名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="适用设备">
          <el-select v-model="form.deviceId" clearable filterable placeholder="不选则适用于全部设备">
            <el-option v-for="device in devices" :key="device.id" :label="`${device.name} / ${device.deviceCode}`" :value="device.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="指标" prop="metric">
          <el-select v-model="form.metric">
            <el-option v-for="item in enumStore.enums.alertMetric" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="条件" prop="operator">
          <el-select v-model="form.operator">
            <el-option v-for="item in enumStore.enums.alertOperator" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值" prop="thresholdValue"><el-input-number v-model="form.thresholdValue" :precision="2" :step="1" class="full-input" /></el-form-item>
        <el-form-item label="告警等级" prop="level">
          <el-select v-model="form.level">
            <el-option v-for="item in enumStore.enums.logLevel" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import { createAlertRule, deleteAlertRule, getAlertRules, updateAlertRule } from '@/api/alertRules'
import { getDeviceList } from '@/api/devices'
import { useEnumStore } from '@/stores/enumStore'
import type { AlertRule, AlertRulePayload, AlertRuleQuery } from '@/types/alertRule'
import type { Device } from '@/types/device'

const enumStore = useEnumStore()
const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const currentId = ref<number>()
const rules = ref<AlertRule[]>([])
const devices = ref<Device[]>([])
const formRef = ref<FormInstance>()
const query = reactive<AlertRuleQuery>({})
const form = reactive<AlertRulePayload>({
  name: '',
  metric: 'temperature',
  operator: 'GT',
  thresholdValue: 80,
  level: 'WARNING',
  enabled: true
})

const rulesConfig: FormRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  metric: [{ required: true, message: '请选择指标', trigger: 'change' }],
  operator: [{ required: true, message: '请选择条件', trigger: 'change' }],
  thresholdValue: [{ required: true, message: '请输入阈值', trigger: 'blur' }],
  level: [{ required: true, message: '请选择等级', trigger: 'change' }]
}

async function loadOptions() {
  const devicePage = await getDeviceList({ page: 1, pageSize: 100 })
  devices.value = devicePage.records
}

async function loadData() {
  loading.value = true
  try {
    rules.value = await getAlertRules(query)
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(query, { deviceId: undefined, enabled: undefined })
  loadData()
}

function resetForm() {
  Object.assign(form, { name: '', deviceId: undefined, metric: 'temperature', operator: 'GT', thresholdValue: 80, level: 'WARNING', enabled: true })
}

function openCreate() {
  dialogMode.value = 'create'
  currentId.value = undefined
  resetForm()
  dialogVisible.value = true
}

function openEdit(rule: AlertRule) {
  dialogMode.value = 'edit'
  currentId.value = rule.id
  Object.assign(form, {
    name: rule.name,
    deviceId: rule.deviceId,
    metric: rule.metric,
    operator: rule.operator,
    thresholdValue: rule.thresholdValue,
    level: rule.level,
    enabled: rule.enabled
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  if (dialogMode.value === 'create') {
    await createAlertRule(form)
    ElMessage.success('规则已创建')
  } else if (currentId.value) {
    await updateAlertRule(currentId.value, form)
    ElMessage.success('规则已更新')
  }
  dialogVisible.value = false
  loadData()
}

async function remove(id: number) {
  await deleteAlertRule(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(async () => {
  await enumStore.loadEnums()
  await loadOptions()
  await loadData()
})
</script>

<style scoped>
.rule-filter {
  grid-template-columns: repeat(2, minmax(180px, 1fr)) auto;
}

.full-input {
  width: 100%;
}
</style>
