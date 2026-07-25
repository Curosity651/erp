<template>
  <a-drawer
    v-model:open="visible"
    title="新建库位调整单"
    :width="960"
    :mask-closable="false"
    @close="handleClose"
  >
    <a-alert
      type="info"
      show-icon
      style="margin-bottom: 12px"
      message="支持物理库位与虚拟库位之间的四向移库。移入物理库位时会校验服务商租赁范围、分区和容量；创建后为“待调整”，点击“调整完成”才真正移动库存。"
    />

    <a-form layout="vertical">
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item label="货主" required>
            <platform-owner-select
              v-model:value="erpTenantId"
              placeholder="请选择货主"
              width="100%"
              @change="reloadBatches"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="仓库" required>
            <warehouse-select
              v-model:value="warehouseId"
              placeholder="请选择仓库"
              width="100%"
              @change="reloadBatches"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="调整原因" required>
            <a-select v-model:value="reasonCode" :options="reasonOptions" placeholder="请选择" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="reasonCode === 'OTHER' ? '原因说明' : '原因说明（选填）'">
            <a-input
              v-model:value="reason"
              :placeholder="reasonCode === 'OTHER' ? '请填写具体原因' : '可补充说明'"
              :maxlength="200"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="备注">
            <a-input v-model:value="remark" placeholder="可选" :maxlength="500" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <a-spin :spinning="loading">
      <a-table
        :data-source="batches"
        :columns="columns"
        :pagination="false"
        row-key="id"
        size="small"
        :row-selection="{
          selectedRowKeys: selectedKeys,
          onChange: onSelectChange,
          getCheckboxProps: (r: any) => ({ disabled: available(r) <= 0 })
        }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'quality'">
            <a-tag :color="record.quality === 'DAMAGED' ? 'red' : 'green'">
              {{ record.quality === 'DAMAGED' ? '次品' : '良品' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'available'">
            {{ available(record) }}
          </template>
          <template v-else-if="column.key === 'moveQty'">
            <a-input-number
              v-model:value="record.moveQty"
              :min="1"
              :max="available(record)"
              :precision="0"
              :disabled="!selectedKeys.includes(record.id) || available(record) <= 0"
              style="width: 100%"
            />
          </template>
          <template v-else-if="column.key === 'target'">
            <a-select
              v-model:value="record.targetCode"
              :options="targetOptions(record.id)"
              :loading="targetLoading[record.id]"
              :disabled="!selectedKeys.includes(record.id)"
              placeholder="选择目标库位"
              show-search
              style="width: 100%"
              :not-found-content="targetLoading[record.id] ? undefined : '无可用目标库位'"
            />
          </template>
        </template>
        <template #emptyText>
          <span>{{ erpTenantId && warehouseId ? '该货主在本仓无可移库批次' : '请先选择货主与仓库' }}</span>
        </template>
      </a-table>
    </a-spin>

    <template #footer>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span>已选 {{ selectedKeys.length }} 个批次</span>
        <a-space>
          <a-button @click="handleClose">取消</a-button>
          <a-button type="primary" :loading="submitting" @click="submit">创建调整单</a-button>
        </a-space>
      </div>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import { isSuccess } from '@/api'
import { doRequest } from '@/utils/axios/request'
import { listOwnerBatches } from '@/api/wms/adjustment'
import type { PhysicalBatchVO } from '@/api/wms/adjustment/types'
import { createLocationTransfer, listTargetCandidates } from '@/api/wms/location-transfer'
import {
  LocationTransferReasonList,
  type TargetLocationVO
} from '@/api/wms/location-transfer/types'

defineOptions({ name: 'LocationTransferCreateDrawer' })
const emit = defineEmits<{ (e: 'success'): void }>()

const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const erpTenantId = ref<number>()
const warehouseId = ref<number>()
const reasonCode = ref<string>()
const reason = ref<string>()
const remark = ref<string>()
const reasonOptions = LocationTransferReasonList.map(item => ({ ...item }))

interface BatchRow extends PhysicalBatchVO {
  moveQty?: number
  targetCode?: string
}
const batches = ref<BatchRow[]>([])
const selectedKeys = ref<number[]>([])

// 每个批次的目标库位候选与加载态
const candidatesMap = reactive<Record<number, TargetLocationVO[]>>({})
const targetLoading = reactive<Record<number, boolean>>({})

const columns = [
  { title: 'SKU', dataIndex: 'skuCode', width: 150, ellipsis: true },
  { title: '源库位', dataIndex: 'locationCode', width: 100 },
  { title: '品质', key: 'quality', width: 70, align: 'center' as const },
  { title: '现存', dataIndex: 'quantity', width: 60, align: 'right' as const },
  { title: '可用', key: 'available', width: 60, align: 'right' as const },
  { title: '移动数量', key: 'moveQty', width: 110 },
  { title: '目标库位', key: 'target', width: 200 }
]

const available = (r: BatchRow) => (r.quantity || 0) - (r.reservedQty || 0)

const targetOptions = (batchId: number) =>
  (candidatesMap[batchId] || []).map(t => ({
    label:
      (t.isVirtual === 1 ? '虚拟 · ' : '') +
      (t.zoneName ? `${t.locationCode}（${t.zoneName}）` : t.locationCode),
    value: t.locationCode
  }))

async function loadCandidates(batchId: number) {
  if (candidatesMap[batchId]) return
  targetLoading[batchId] = true
  try {
    const res = await listTargetCandidates(batchId)
    if (isSuccess(res) && res.data) candidatesMap[batchId] = res.data
    else candidatesMap[batchId] = []
  } finally {
    targetLoading[batchId] = false
  }
}

const onSelectChange = (keys: (string | number)[]) => {
  const next = keys as number[]
  selectedKeys.value = next
  batches.value.forEach(b => {
    if (next.includes(b.id)) {
      if (!b.moveQty) b.moveQty = available(b)
      loadCandidates(b.id)
    }
  })
}

async function reloadBatches() {
  selectedKeys.value = []
  batches.value = []
  Object.keys(candidatesMap).forEach(k => delete candidatesMap[Number(k)])
  if (!erpTenantId.value || !warehouseId.value) return
  loading.value = true
  try {
    const res = await listOwnerBatches(erpTenantId.value, warehouseId.value)
    if (isSuccess(res) && res.data) {
      batches.value = res.data.filter(b => (b.quantity || 0) > 0).map(b => ({ ...b }))
    }
  } finally {
    loading.value = false
  }
}

function submit() {
  if (!erpTenantId.value || !warehouseId.value) {
    message.warning('请选择货主与仓库')
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
  const rows = batches.value.filter(b => selectedKeys.value.includes(b.id))
  if (rows.length === 0) {
    message.warning('请勾选要移库的批次')
    return
  }
  const badQty = rows.find(b => !b.moveQty || b.moveQty <= 0 || b.moveQty > available(b))
  if (badQty) {
    message.warning(`批次[${badQty.locationCode}]移动数量不合法（需在 1~${available(badQty)} 之间）`)
    return
  }
  const noTarget = rows.find(b => !b.targetCode)
  if (noTarget) {
    message.warning(`批次[${noTarget.locationCode}]未选择目标库位`)
    return
  }
  submitting.value = true
  doRequest(
    createLocationTransfer({
      warehouseId: warehouseId.value,
      erpTenantId: erpTenantId.value,
      reasonCode: reasonCode.value,
      reason: reason.value?.trim(),
      remark: remark.value,
      items: rows.map(b => ({
        physicalInventoryId: b.id,
        quantity: b.moveQty as number,
        targetLocationCode: b.targetCode as string
      }))
    }),
    {
      successMessage: '已创建库位调整单（待调整）',
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

function handleClose() {
  visible.value = false
}

function open() {
  visible.value = true
  erpTenantId.value = undefined
  warehouseId.value = undefined
  reasonCode.value = undefined
  reason.value = undefined
  remark.value = undefined
  batches.value = []
  selectedKeys.value = []
  Object.keys(candidatesMap).forEach(k => delete candidatesMap[Number(k)])
}

defineExpose({ open })
</script>
