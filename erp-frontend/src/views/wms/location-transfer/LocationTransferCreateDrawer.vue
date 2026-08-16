<template>
  <a-drawer
    v-model:open="visible"
    title="新建库位调整单"
    :width="1180"
    :mask-closable="false"
    @close="handleClose"
  >
    <a-alert
      type="info"
      show-icon
      message="只移动可用库存。已被销售出库预占的数量不能移动；不良品只能通过报废流程处理。"
      style="margin-bottom: 16px"
    />

    <a-form layout="vertical">
      <a-row :gutter="12">
        <a-col :span="5">
          <a-form-item label="仓库" required>
            <warehouse-select
              v-model:value="warehouseId"
              placeholder="请选择仓库"
              width="100%"
              @change="handleWarehouseChange"
            />
          </a-form-item>
        </a-col>
        <a-col :span="5">
          <a-form-item label="货主" required>
            <platform-owner-select
              v-model:value="erpTenantId"
              placeholder="请选择货主"
              width="100%"
              @change="handleOwnerChange"
            />
          </a-form-item>
        </a-col>
        <a-col :span="5">
          <a-form-item label="源库位">
            <a-select
              v-model:value="sourceLocationId"
              :options="sourceLocationOptions"
              :loading="targetsLoading"
              placeholder="全部库位"
              show-search
              allow-clear
              :filter-option="filterOption"
            />
          </a-form-item>
        </a-col>
        <a-col :span="5">
          <a-form-item label="SKU">
            <a-input v-model:value="skuKeyword" placeholder="内部SKU或ERP SKU" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="4" class="query-column">
          <a-space>
            <a-button type="primary" :loading="loading" @click="searchSources">查询</a-button>
            <a-button @click="resetFilters">重置</a-button>
          </a-space>
        </a-col>
      </a-row>
      <a-row :gutter="12">
        <a-col :span="6">
          <a-form-item label="调整原因" required>
            <a-select v-model:value="reasonCode" :options="reasonOptions" placeholder="请选择" />
          </a-form-item>
        </a-col>
        <a-col :span="9">
          <a-form-item :label="reasonCode === 'OTHER' ? '原因说明' : '原因说明（选填）'">
            <a-input
              v-model:value="reason"
              :placeholder="reasonCode === 'OTHER' ? '请填写具体原因' : '可补充说明'"
              :maxlength="200"
            />
          </a-form-item>
        </a-col>
        <a-col :span="9">
          <a-form-item label="备注">
            <a-input v-model:value="remark" placeholder="选填" :maxlength="500" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <a-table
      :data-source="sources"
      :columns="columns"
      :pagination="{ pageSize: 10, showSizeChanger: true }"
      :loading="loading"
      row-key="inventoryId"
      size="small"
      :scroll="{ x: 1080 }"
      :row-selection="{
        selectedRowKeys,
        onChange: onSelectChange,
        getCheckboxProps: (record: EditableSource) => ({ disabled: record.availableQuantity <= 0 })
      }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'source'">
          <div>{{ record.locationCode }}</div>
          <div class="subtle">{{ record.zoneName || zoneText(record.zoneType) }}</div>
        </template>
        <template v-else-if="column.key === 'sku'">
          <div>{{ record.warehouseSkuCode || record.skuCode }}</div>
          <div class="subtle">ERP SKU：{{ record.skuCode }}</div>
        </template>
        <template v-else-if="column.key === 'quality'">
          <a-tag color="green">良品</a-tag>
        </template>
        <template v-else-if="column.key === 'quantity'">
          <span>{{ record.quantity }}</span>
          <span v-if="record.reservedQuantity" class="subtle quantity-note">
            （预占 {{ record.reservedQuantity }}）
          </span>
        </template>
        <template v-else-if="column.key === 'moveQuantity'">
          <a-input-number
            v-model:value="record.moveQuantity"
            :min="1"
            :max="record.availableQuantity"
            :precision="0"
            :disabled="!selectedRowKeys.includes(record.inventoryId)"
            style="width: 100px"
          />
        </template>
        <template v-else-if="column.key === 'target'">
          <a-select
            v-model:value="record.targetLocationId"
            :options="targetOptions(record)"
            :disabled="!selectedRowKeys.includes(record.inventoryId)"
            placeholder="选择目标库位"
            show-search
            :filter-option="filterOption"
            style="width: 100%"
          />
        </template>
      </template>
      <template #emptyText>
        {{ searched ? '没有符合条件的可移动库存' : '选择仓库和货主后查询库存' }}
      </template>
    </a-table>

    <template #footer>
      <div class="drawer-footer">
        <span>已选 {{ selectedRowKeys.length }} 条库存</span>
        <a-space>
          <a-button @click="handleClose">取消</a-button>
          <a-button type="primary" :loading="submitting" @click="submit">创建调整单</a-button>
        </a-space>
      </div>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import { isSuccess } from '@/api'
import { doRequest } from '@/utils/axios/request'
import {
  createLogicalLocationTransfer,
  listLogicalTransferSources,
  listLogicalTransferTargets
} from '@/api/wms/location-transfer'
import {
  LocationTransferReasonList,
  type LogicalTransferLocationVO,
  type LogicalTransferSourceVO
} from '@/api/wms/location-transfer/types'

defineOptions({ name: 'LocationTransferCreateDrawer' })
const emit = defineEmits<{ (e: 'success'): void }>()

interface EditableSource extends LogicalTransferSourceVO {
  moveQuantity: number
  targetLocationId?: number
}

const visible = ref(false)
const loading = ref(false)
const targetsLoading = ref(false)
const submitting = ref(false)
const searched = ref(false)
const warehouseId = ref<number>()
const erpTenantId = ref<number>()
const sourceLocationId = ref<number>()
const skuKeyword = ref<string>()
const reasonCode = ref<string>()
const reason = ref<string>()
const remark = ref<string>()
const sources = ref<EditableSource[]>([])
const targets = ref<LogicalTransferLocationVO[]>([])
const selectedRowKeys = ref<number[]>([])
const reasonOptions = LocationTransferReasonList.map(item => ({ ...item }))

const columns = [
  { title: '源库位', key: 'source', width: 150, fixed: 'left' as const },
  { title: '货主', dataIndex: 'ownerName', width: 110, ellipsis: true },
  { title: 'SKU', key: 'sku', width: 230 },
  { title: '品质', key: 'quality', width: 80 },
  { title: '库存/预占', key: 'quantity', width: 130 },
  { title: '可移动', dataIndex: 'availableQuantity', width: 90, align: 'right' as const },
  { title: '本次移动', key: 'moveQuantity', width: 130 },
  { title: '目标库位', key: 'target', width: 260 }
]

const zoneText = (type?: string) =>
  ({ STANDARD: '标准区', TEMP: '暂存区', RETURN: '退货区' })[type || ''] || type || '未设置分区'

function filterOption(input: string, option: any) {
  return String(option?.label || '')
    .toLowerCase()
    .includes(input.toLowerCase())
}

function locationLabel(location: LogicalTransferLocationVO) {
  const usage = location.utilizationPercent == null ? '' : ` · 已用 ${location.utilizationPercent}%`
  return `${location.locationCode} · ${location.zoneName || zoneText(location.zoneType)}${usage}`
}

const sourceLocationOptions = computed(() =>
  targets.value.map(location => ({ label: locationLabel(location), value: location.locationId }))
)

function targetOptions(source: EditableSource) {
  return targets.value
    .filter(target => target.locationId !== source.locationId)
    .map(target => ({ label: locationLabel(target), value: target.locationId }))
}

async function loadTargets() {
  targets.value = []
  if (!warehouseId.value || !erpTenantId.value) return
  targetsLoading.value = true
  try {
    const res = await listLogicalTransferTargets({
      warehouseId: warehouseId.value,
      erpTenantId: erpTenantId.value
    })
    if (isSuccess(res)) targets.value = res.data || []
  } finally {
    targetsLoading.value = false
  }
}

function handleWarehouseChange() {
  erpTenantId.value = undefined
  sourceLocationId.value = undefined
  sources.value = []
  targets.value = []
  selectedRowKeys.value = []
}

async function handleOwnerChange() {
  sourceLocationId.value = undefined
  sources.value = []
  selectedRowKeys.value = []
  await loadTargets()
}

async function searchSources() {
  if (!warehouseId.value || !erpTenantId.value) {
    message.warning('请先选择仓库和货主')
    return
  }
  loading.value = true
  searched.value = true
  selectedRowKeys.value = []
  try {
    const res = await listLogicalTransferSources({
      warehouseId: warehouseId.value,
      erpTenantId: erpTenantId.value,
      locationId: sourceLocationId.value,
      skuKeyword: skuKeyword.value?.trim()
    })
    sources.value = (isSuccess(res) ? res.data || [] : []).map(row => ({
      ...row,
      moveQuantity: row.availableQuantity
    }))
  } finally {
    loading.value = false
  }
}

function onSelectChange(keys: (string | number)[]) {
  selectedRowKeys.value = keys.map(Number)
}

function resetFilters() {
  sourceLocationId.value = undefined
  skuKeyword.value = undefined
  sources.value = []
  selectedRowKeys.value = []
  searched.value = false
}

function submit() {
  if (!warehouseId.value || !erpTenantId.value) {
    message.warning('请选择仓库和货主')
    return
  }
  if (!reasonCode.value) {
    message.warning('请选择调整原因')
    return
  }
  if (reasonCode.value === 'OTHER' && !reason.value?.trim()) {
    message.warning('选择“其他”时请填写原因说明')
    return
  }
  const selected = sources.value.filter(row => selectedRowKeys.value.includes(row.inventoryId))
  if (!selected.length) {
    message.warning('请选择需要移动的库存')
    return
  }
  const invalid = selected.find(
    row =>
      !row.targetLocationId ||
      !row.moveQuantity ||
      row.moveQuantity < 1 ||
      row.moveQuantity > row.availableQuantity
  )
  if (invalid) {
    message.warning(`请检查 ${invalid.warehouseSkuCode || invalid.skuCode} 的目标库位和移动数量`)
    return
  }
  submitting.value = true
  doRequest(
    createLogicalLocationTransfer({
      warehouseId: warehouseId.value,
      erpTenantId: erpTenantId.value,
      reasonCode: reasonCode.value,
      reason: reason.value?.trim(),
      remark: remark.value?.trim(),
      items: selected.map(row => ({
        sourceInventoryId: row.inventoryId,
        targetLocationId: row.targetLocationId!,
        quantity: row.moveQuantity
      }))
    }),
    {
      successMessage: '库位调整单已创建',
      onSuccess: () => {
        visible.value = false
        emit('success')
      },
      onFinally: () => {
        submitting.value = false
      }
    }
  )
}

function reset() {
  warehouseId.value = undefined
  erpTenantId.value = undefined
  sourceLocationId.value = undefined
  skuKeyword.value = undefined
  reasonCode.value = undefined
  reason.value = undefined
  remark.value = undefined
  sources.value = []
  targets.value = []
  selectedRowKeys.value = []
  searched.value = false
}

function handleClose() {
  visible.value = false
}

function open() {
  reset()
  visible.value = true
}

defineExpose({ open })
</script>

<style scoped>
.query-column {
  display: flex;
  align-items: flex-end;
  padding-bottom: 24px;
}

.drawer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.subtle {
  color: rgb(0 0 0 / 45%);
  font-size: 12px;
}

.quantity-note {
  margin-left: 4px;
}
</style>
