<template>
  <a-drawer v-model:open="visible" width="920" :title="drawerTitle" :footer="null">
    <div class="toolbar">
      <div>
        <div class="warehouse-name">{{ warehouse?.warehouseName }}</div>
        <div class="summary">{{ rowGroups.length }} 排 · {{ locationCount }} 个库位</div>
      </div>
      <a-button v-if="canEdit" type="primary" @click="openCreate()">新增库位</a-button>
    </div>

    <a-spin :spinning="loading">
      <a-empty v-if="!loading && rowGroups.length === 0" description="尚未创建库位" />
      <a-collapse v-else v-model:active-key="activeRows" class="row-collapse">
        <a-collapse-panel v-for="group in rowGroups" :key="group.rackNo">
          <template #header>
            <div class="row-header">
              <strong>{{ group.rackNo }} 排</strong>
              <span>{{ group.locations.length }} 个库位</span>
            </div>
          </template>
          <a-table
            row-key="id"
            size="small"
            :pagination="false"
            :data-source="group.locations"
            :columns="columns"
            :scroll="{ x: 760 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'zone'">
                {{ zoneName(record.zoneId) }}
              </template>
              <template v-else-if="column.key === 'size'">
                {{ record.lengthMm }} × {{ record.widthMm }} × {{ record.heightMm }} mm
              </template>
              <template v-else-if="column.key === 'shared'">
                <a-tag v-if="record.publicShared === 1" color="blue">公共</a-tag>
                <span v-else>普通</span>
              </template>
              <template v-else-if="column.key === 'operate'">
                <a-space>
                  <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
                  <a-button type="link" danger size="small" @click="removeLocation(record)">删除</a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-collapse-panel>
      </a-collapse>
    </a-spin>
  </a-drawer>

  <a-modal
    v-model:open="editorVisible"
    :title="editingId ? '编辑库位' : '新增库位'"
    ok-text="保存"
    :confirm-loading="saving"
    @ok="saveLocation"
  >
    <a-form layout="vertical" class="location-form">
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item label="排号" required>
            <a-input v-model:value="form.rackNo" :disabled="!!editingId" placeholder="如 A1" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="排内顺序" required>
            <a-input-number v-model:value="form.sequenceNo" :disabled="!!editingId" :min="1" :max="9999" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="库位编号" required>
            <a-input v-model:value="form.locationCode" :disabled="!!editingId" placeholder="如 A1-01" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="库位分区" required>
            <a-select v-model:value="form.zoneId" :options="zoneOptions" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="库位类型" required>
            <a-select
              v-model:value="form.locationType"
              :options="[
                { label: '标准库位', value: 'STANDARD' },
                { label: '大件库位', value: 'BIG' },
                { label: '小件库位', value: 'SMALL' }
              ]"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="8"><a-form-item label="长（mm）" required><a-input-number v-model:value="form.lengthMm" :min="1" /></a-form-item></a-col>
        <a-col :span="8"><a-form-item label="宽（mm）" required><a-input-number v-model:value="form.widthMm" :min="1" /></a-form-item></a-col>
        <a-col :span="8"><a-form-item label="高（mm）" required><a-input-number v-model:value="form.heightMm" :min="1" /></a-form-item></a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="8"><a-form-item label="最大承重（kg）" required><a-input-number v-model:value="form.maxWeightKg" :min="0.01" :precision="2" /></a-form-item></a-col>
        <a-col :span="8"><a-form-item label="最多 SKU 种类"><a-input-number v-model:value="form.maxSkuKinds" :min="0" /></a-form-item></a-col>
        <a-col :span="8"><a-form-item label="公共共享"><a-switch v-model:checked="publicShared" /></a-form-item></a-col>
      </a-row>
      <a-alert type="info" show-icon message="库位编号创建后不可修改；有库存或预占时不能删除。" />
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useAdminUser } from '@/hooks/system/use-admin-user'
import { isSuccess } from '@/api'
import {
  createLogicalLocation,
  deleteLogicalLocation,
  listGroupedLocations,
  listZones,
  updateLogicalLocation
} from '@/api/wms/location-mgmt'
import type {
  LogicalLocationSave,
  WarehouseStructure,
  WmsLocation,
  WmsZone
} from '@/api/wms/location-mgmt/types'

const emit = defineEmits<{ success: [] }>()
const { userInfo } = useAdminUser()
const canEdit = computed(() => userInfo.value?.permissions?.includes('wms:warehouse:edit') ?? true)
const visible = ref(false)
const editorVisible = ref(false)
const loading = ref(false)
const saving = ref(false)
const editingId = ref<number>()
const warehouse = ref<WarehouseStructure>()
const grouped = ref<Record<string, WmsLocation[]>>({})
const zones = ref<WmsZone[]>([])
const activeRows = ref<string[]>([])

const emptyForm = (): LogicalLocationSave => ({
  warehouseId: undefined,
  zoneId: 0,
  rackNo: '',
  sequenceNo: 1,
  locationCode: '',
  locationType: 'STANDARD',
  lengthMm: 1200,
  widthMm: 1000,
  heightMm: 1800,
  maxWeightKg: 1000,
  maxSkuKinds: 0,
  publicShared: 0
})
const form = reactive<LogicalLocationSave>(emptyForm())
const publicShared = computed({
  get: () => form.publicShared === 1,
  set: value => (form.publicShared = value ? 1 : 0)
})
const drawerTitle = computed(() => `库位管理${warehouse.value ? ` · ${warehouse.value.warehouseName}` : ''}`)
const rowGroups = computed(() =>
  Object.entries(grouped.value)
    .map(([rackNo, locations]) => ({
      rackNo,
      locations: [...locations].sort((a, b) => (a.columnNo || 0) - (b.columnNo || 0))
    }))
    .sort((a, b) => a.rackNo.localeCompare(b.rackNo, 'zh-CN', { numeric: true }))
)
const locationCount = computed(() => rowGroups.value.reduce((sum, row) => sum + row.locations.length, 0))
const zoneOptions = computed(() => zones.value.map(zone => ({ label: zone.zoneName, value: zone.id })))
const columns = [
  { title: '库位编号', dataIndex: 'locationCode', width: 120 },
  { title: '分区', key: 'zone', width: 110 },
  { title: '尺寸', key: 'size', width: 210 },
  { title: '承重(kg)', dataIndex: 'maxWeightKg', width: 100 },
  { title: 'SKU上限', dataIndex: 'maxSkuKinds', width: 90 },
  { title: '共享', key: 'shared', width: 80 },
  { title: '操作', key: 'operate', width: 120, fixed: 'right' as const }
]

function zoneName(zoneId?: number) {
  return zones.value.find(zone => zone.id === zoneId)?.zoneName || '-'
}

async function loadData() {
  if (!warehouse.value) return
  loading.value = true
  try {
    const [locationRes, zoneRes] = await Promise.all([
      listGroupedLocations(warehouse.value.id),
      listZones(warehouse.value.id)
    ])
    if (!isSuccess(locationRes)) throw new Error(locationRes.message || '库位加载失败')
    if (!isSuccess(zoneRes)) throw new Error(zoneRes.message || '分区加载失败')
    grouped.value = locationRes.data || {}
    zones.value = zoneRes.data || []
    activeRows.value = Object.keys(grouped.value)
  } catch (error: any) {
    message.error(error?.message || '库位加载失败')
  } finally {
    loading.value = false
  }
}

function open(record: WarehouseStructure) {
  warehouse.value = record
  visible.value = true
  loadData()
}

function resetForm() {
  Object.assign(form, emptyForm(), { warehouseId: warehouse.value?.id })
}

function openCreate(rackNo?: string) {
  editingId.value = undefined
  resetForm()
  if (rackNo) form.rackNo = rackNo
  const current = grouped.value[rackNo || ''] || []
  form.sequenceNo = current.reduce((max, item) => Math.max(max, item.columnNo || 0), 0) + 1
  if (form.rackNo) form.locationCode = `${form.rackNo}-${String(form.sequenceNo).padStart(2, '0')}`
  form.zoneId = zones.value[0]?.id || 0
  editorVisible.value = true
}

function openEdit(record: WmsLocation) {
  editingId.value = record.id
  Object.assign(form, {
    warehouseId: record.warehouseId,
    zoneId: record.zoneId || 0,
    rackNo: record.rackNo,
    sequenceNo: record.columnNo,
    locationCode: record.locationCode,
    locationType: record.locationType || 'STANDARD',
    lengthMm: record.lengthMm || 1,
    widthMm: record.widthMm || 1,
    heightMm: record.heightMm || 1,
    maxWeightKg: record.maxWeightKg || 0.01,
    maxSkuKinds: record.maxSkuKinds || 0,
    publicShared: record.publicShared || 0
  })
  editorVisible.value = true
}

function validateForm() {
  if (!form.zoneId || !form.locationType) return '请选择库位分区和类型'
  if (!editingId.value && (!form.rackNo?.trim() || !form.sequenceNo || !form.locationCode?.trim())) {
    return '请填写排号、排内顺序和库位编号'
  }
  if (!form.lengthMm || !form.widthMm || !form.heightMm || !form.maxWeightKg) return '请填写完整容量参数'
  return ''
}

async function saveLocation() {
  const error = validateForm()
  if (error) return message.warning(error)
  saving.value = true
  try {
    const payload = { ...form }
    const res = editingId.value
      ? await updateLogicalLocation(editingId.value, payload)
      : await createLogicalLocation(payload)
    if (!isSuccess(res)) return message.error(res.message || '保存失败')
    message.success('库位已保存')
    editorVisible.value = false
    await loadData()
    emit('success')
  } finally {
    saving.value = false
  }
}

function removeLocation(record: WmsLocation) {
  Modal.confirm({
    title: `删除库位 ${record.locationCode}？`,
    content: '只有库存和预占均为零的库位才能删除。',
    okType: 'danger',
    async onOk() {
      const res = await deleteLogicalLocation(record.id)
      if (!isSuccess(res)) throw new Error(res.message || '删除失败')
      message.success('库位已删除')
      await loadData()
      emit('success')
    }
  })
}

defineExpose({ open })
</script>

<style scoped>
.toolbar,
.row-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.toolbar { margin-bottom: 16px; }
.warehouse-name { font-size: 16px; font-weight: 600; }
.summary { margin-top: 4px; color: #8c8c8c; }
.row-collapse { background: transparent; }
.row-header { width: 100%; padding-right: 12px; }
.row-header span { color: #8c8c8c; font-size: 12px; }
.location-form :deep(.ant-input-number) { width: 100%; }
</style>
