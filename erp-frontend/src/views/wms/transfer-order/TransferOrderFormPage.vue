<template>
  <page-container :page-header-render="false" ghost>
    <template #customHeader>
      <div class="tx-header">
        <a-button type="text" @click="handleBack">
          <arrow-left-outlined />
          返回列表
        </a-button>
        <a-divider type="vertical" />
        <span class="tx-title">{{ pageTitle }}</span>
        <a-tag v-if="transferNo" style="margin-left: 12px">{{ transferNo }}</a-tag>
      </div>
    </template>

    <a-card :bordered="false">
      <a-form :label-col="{ style: { width: '96px' } }">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="源仓库" required>
              <WarehouseSelect
                v-model:value="formModel.fromWarehouseId"
                placeholder="调出仓库"
                :exclude-id="formModel.toWarehouseId"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="货主" required>
              <PlatformOwnerSelect
                v-model:value="formModel.erpTenantId"
                placeholder="货物归属货主"
                width="100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="目标仓库" required>
              <WarehouseSelect
                v-model:value="formModel.toWarehouseId"
                placeholder="调入仓库"
                :exclude-id="formModel.fromWarehouseId"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注">
          <a-input v-model:value="formModel.remark" placeholder="选填" :maxlength="500" style="max-width: 520px" />
        </a-form-item>
      </a-form>

      <a-alert
        v-if="formModel.toWarehouseId && formModel.erpTenantId && targetLocations.length === 0"
        type="warning"
        show-icon
        style="margin-bottom: 12px"
        message="该货主的服务商在目标仓库没有租用库位，无法调拨到该仓。请换目标仓库，或先为该服务商在目标仓分配货架。"
      />

      <div class="items-head">
        <span>调拨明细（源批次 → 目标库位）</span>
        <a-button size="small" type="dashed" :disabled="!canAddRow" @click="addRow">+ 添加一行</a-button>
      </div>

      <a-table
        size="small"
        row-key="_key"
        :data-source="items"
        :pagination="false"
        :columns="columns"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'source'">
            <a-select
              v-model:value="record.sourcePhysicalInventoryId"
              style="width: 100%"
              placeholder="选源批次"
              :options="sourceOptions"
              show-search
              option-filter-prop="label"
              @change="() => onSourceChange(record)"
            />
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number
              v-model:value="record.quantity"
              :min="1"
              :max="record._available || undefined"
              style="width: 100%"
            />
            <span v-if="record._available != null" class="avail">可用 {{ record._available }}</span>
          </template>
          <template v-else-if="column.key === 'target'">
            <a-select
              v-model:value="record.targetLocationCode"
              style="width: 100%"
              placeholder="选目标库位"
              :options="targetOptions"
              show-search
              option-filter-prop="label"
            />
          </template>
          <template v-else-if="column.key === 'op'">
            <a-button type="link" danger size="small" @click="removeRow(record)">删除</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <template #footer>
      <a-space>
        <a-button @click="handleBack">取消</a-button>
        <a-button :loading="submitLoading" @click="() => submit(false)">保存草稿</a-button>
        <a-button type="primary" :loading="submitLoading" @click="() => submit(true)">
          保存并发出
        </a-button>
      </a-space>
    </template>
  </page-container>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import PageContainer from '#/layout/components/PageContainer'
import { emitter } from '@/hooks/mitt'
import { isSuccess } from '@/api'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import {
  createTransferOrder,
  updateTransferOrder,
  getTransferOrderDetail,
  shipTransferOrder,
  listSourceBatches,
  listTargetLocations
} from '@/api/wms/transfer-order'
import type {
  TransferOrderDTO,
  TransferSourceBatchVO,
  TransferTargetLocationVO
} from '@/api/wms/transfer-order/types'

defineOptions({ name: 'TransferOrderFormPage' })

const route = useRoute()
const router = useRouter()

const formMode = computed(() => route.params.mode as 'create' | 'edit')
const isEdit = computed(() => formMode.value === 'edit')
const transferOrderId = computed(() => {
  const id = route.params.id as string
  return id ? Number(id) : undefined
})
const pageTitle = computed(() => (isEdit.value ? '编辑调拨单' : '新建调拨单'))

const submitLoading = ref(false)
const transferNo = ref('')

interface ItemRow {
  _key: number
  sourcePhysicalInventoryId?: number
  quantity?: number
  targetLocationCode?: string
  _sku?: string
  _available?: number
}

const formModel = reactive<TransferOrderDTO>({
  id: undefined,
  transferType: 'NORMAL',
  fromWarehouseId: undefined as unknown as number,
  toWarehouseId: undefined as unknown as number,
  erpTenantId: undefined as unknown as number,
  remark: '',
  items: []
})

const items = ref<ItemRow[]>([])
const sourceBatches = ref<TransferSourceBatchVO[]>([])
const targetLocations = ref<TransferTargetLocationVO[]>([])
let keySeq = 1

const columns = [
  { title: '源批次（A仓）', key: 'source', width: 320 },
  { title: '数量', key: 'qty', width: 140 },
  { title: '目标库位（B仓·服务商租用）', key: 'target', width: 260 },
  { title: '操作', key: 'op', width: 70 }
]

const sourceOptions = computed(() =>
  sourceBatches.value.map(b => ({
    label: `${b.locationCode || '-'} · ${b.skuCode} · 可用${b.quantity - (b.reservedQty || 0)}`,
    value: b.id
  }))
)
const targetOptions = computed(() =>
  targetLocations.value.map(l => ({
    label: l.zoneName ? `${l.locationCode}（${l.zoneName}）` : l.locationCode,
    value: l.locationCode
  }))
)

const canAddRow = computed(
  () => !!formModel.fromWarehouseId && !!formModel.erpTenantId && !!formModel.toWarehouseId
)

async function loadSourceBatches() {
  if (!formModel.fromWarehouseId || !formModel.erpTenantId) {
    sourceBatches.value = []
    return
  }
  const res = await listSourceBatches({
    fromWarehouseId: formModel.fromWarehouseId,
    erpTenantId: formModel.erpTenantId
  })
  sourceBatches.value = isSuccess(res) ? res.data || [] : []
}

async function loadTargetLocations() {
  if (!formModel.toWarehouseId || !formModel.erpTenantId) {
    targetLocations.value = []
    return
  }
  const res = await listTargetLocations({
    toWarehouseId: formModel.toWarehouseId,
    erpTenantId: formModel.erpTenantId
  })
  targetLocations.value = isSuccess(res) ? res.data || [] : []
}

watch(
  () => [formModel.fromWarehouseId, formModel.erpTenantId],
  () => {
    loadSourceBatches()
    // 源变了，清掉各行已选源批次
    items.value.forEach(r => {
      r.sourcePhysicalInventoryId = undefined
      r._sku = undefined
      r._available = undefined
    })
  }
)
watch(
  () => [formModel.toWarehouseId, formModel.erpTenantId],
  () => {
    loadTargetLocations()
    items.value.forEach(r => (r.targetLocationCode = undefined))
  }
)

function addRow() {
  items.value.push({ _key: keySeq++ })
}
function removeRow(row: ItemRow) {
  items.value = items.value.filter(r => r._key !== row._key)
}
function onSourceChange(row: ItemRow) {
  const b = sourceBatches.value.find(x => x.id === row.sourcePhysicalInventoryId)
  row._sku = b?.skuCode
  row._available = b ? b.quantity - (b.reservedQty || 0) : undefined
  if (row.quantity == null && row._available) row.quantity = row._available
}

function validate(): boolean {
  if (!formModel.fromWarehouseId || !formModel.toWarehouseId || !formModel.erpTenantId) {
    message.warning('请选择源仓库、货主、目标仓库')
    return false
  }
  if (formModel.fromWarehouseId === formModel.toWarehouseId) {
    message.warning('源仓库与目标仓库不能相同')
    return false
  }
  if (items.value.length === 0) {
    message.warning('请至少添加一行调拨明细')
    return false
  }
  for (const r of items.value) {
    if (!r.sourcePhysicalInventoryId || !r.targetLocationCode || !r.quantity) {
      message.warning('每行都需选择源批次、目标库位并填写数量')
      return false
    }
    if (r._available != null && r.quantity > r._available) {
      message.warning(`数量超过源批次可用（${r._available}）`)
      return false
    }
  }
  return true
}

async function submit(ship: boolean) {
  if (!validate()) return
  submitLoading.value = true
  try {
    const dto: TransferOrderDTO = {
      id: formModel.id,
      transferType: 'NORMAL',
      fromWarehouseId: formModel.fromWarehouseId,
      toWarehouseId: formModel.toWarehouseId,
      erpTenantId: formModel.erpTenantId,
      remark: formModel.remark,
      items: items.value.map(r => ({
        sourcePhysicalInventoryId: r.sourcePhysicalInventoryId as number,
        targetLocationCode: r.targetLocationCode as string,
        quantity: r.quantity as number
      }))
    }
    let orderId = formModel.id
    if (isEdit.value && orderId) {
      const res = await updateTransferOrder(dto)
      if (!isSuccess(res)) {
        message.error(res.message || '保存失败')
        return
      }
    } else {
      const res = await createTransferOrder(dto)
      if (!isSuccess(res) || !res.data) {
        message.error(res.message || '保存失败')
        return
      }
      orderId = res.data as number
    }
    if (ship && orderId) {
      const shipRes = await shipTransferOrder(orderId)
      if (!isSuccess(shipRes)) {
        message.error(shipRes.message || '已保存，但发出失败：' + (shipRes.message || ''))
        return
      }
      message.success('已保存并发出（在途）')
    } else {
      message.success('已保存草稿')
    }
    goBackToList()
  } finally {
    submitLoading.value = false
  }
}

async function loadForEdit() {
  if (!isEdit.value || !transferOrderId.value) return
  const res = await getTransferOrderDetail(transferOrderId.value)
  if (!isSuccess(res) || !res.data) {
    message.error('加载调拨单失败')
    return
  }
  const d = res.data
  formModel.id = transferOrderId.value
  formModel.fromWarehouseId = d.fromWarehouseId as number
  formModel.toWarehouseId = d.toWarehouseId as number
  formModel.erpTenantId = d.erpTenantId as number
  formModel.remark = d.remark
  transferNo.value = d.transferNo || ''
  await Promise.all([loadSourceBatches(), loadTargetLocations()])
  items.value = (d.items || []).map(it => {
    const row: ItemRow = {
      _key: keySeq++,
      sourcePhysicalInventoryId: it.sourcePhysicalInventoryId,
      quantity: it.quantity,
      targetLocationCode: it.targetLocationCode,
      _sku: it.skuCode
    }
    const b = sourceBatches.value.find(x => x.id === it.sourcePhysicalInventoryId)
    row._available = b ? b.quantity - (b.reservedQty || 0) : undefined
    return row
  })
}

function goBackToList() {
  const currentPath = route.fullPath
  emitter.emit('refresh-transfer-order-list')
  router.push('/ops/transfer-order')
  emitter.emit('close-current-tab', currentPath)
}
function handleBack() {
  goBackToList()
}

onMounted(() => {
  if (isEdit.value) loadForEdit()
  else addRow()
})
</script>

<style scoped>
.tx-header {
  display: flex;
  align-items: center;
  padding: 8px 4px;
}
.tx-title {
  font-size: 16px;
  font-weight: 600;
}
.items-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 0 8px;
  font-weight: 600;
}
.avail {
  font-size: 11px;
  color: rgba(0, 0, 0, 0.45);
  margin-left: 4px;
}
</style>
