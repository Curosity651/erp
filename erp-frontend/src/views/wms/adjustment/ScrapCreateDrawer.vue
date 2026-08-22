<template>
  <a-drawer
    v-model:open="visible"
    title="发起报废"
    :width="820"
    :mask-closable="false"
    @close="handleClose"
  >
    <a-alert
      type="warning"
      show-icon
      style="margin-bottom: 12px"
      message="这里只显示所选货主在本仓不良品区的库存。发起后冻结数量，货主同意后由仓库确认实际销毁。"
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
          <a-form-item label="报废原因">
            <a-input v-model:value="reason" placeholder="可选" :maxlength="500" />
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
          <template v-else-if="column.key === 'scrapQty'">
            <a-input-number
              v-model:value="record.scrapQty"
              :min="1"
              :max="available(record)"
              :precision="0"
              :disabled="!selectedKeys.includes(record.id) || available(record) <= 0"
              style="width: 100%"
            />
          </template>
        </template>
        <template #emptyText>
          <span>{{ erpTenantId && warehouseId ? '该货主在本仓不良品区无可报废库存' : '请先选择货主与仓库' }}</span>
        </template>
      </a-table>
    </a-spin>

    <template #footer>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span>已选 {{ selectedKeys.length }} 条库位库存</span>
        <a-space>
          <a-button @click="handleClose">取消</a-button>
          <a-button type="primary" danger :loading="submitting" @click="submit">发起报废</a-button>
        </a-space>
      </div>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import { isSuccess } from '@/api'
import { doRequest } from '@/utils/axios/request'
import { listScrapBatches, createScrap } from '@/api/wms/adjustment'
import type { ScrapBatchVO } from '@/api/wms/adjustment/types'

defineOptions({ name: 'ScrapCreateDrawer' })
const emit = defineEmits<{ (e: 'success'): void }>()

const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const erpTenantId = ref<number>()
const warehouseId = ref<number>()
const reason = ref<string>()

interface BatchRow extends ScrapBatchVO {
  scrapQty?: number
}
const batches = ref<BatchRow[]>([])
const selectedKeys = ref<number[]>([])

const columns = [
  { title: '库位', dataIndex: 'locationCode', width: 150, ellipsis: true },
  { title: '内部 SKU', dataIndex: 'warehouseSkuCode', width: 190, ellipsis: true },
  { title: '品质', key: 'quality', width: 80, align: 'center' as const },
  { title: '现存', dataIndex: 'quantity', width: 70, align: 'right' as const },
  { title: '可用', key: 'available', width: 70, align: 'right' as const },
  { title: '报废数量', key: 'scrapQty', width: 120 }
]

const available = (r: BatchRow) => (r.quantity || 0) - (r.reservedQty || 0)

const onSelectChange = (keys: (string | number)[]) => {
  selectedKeys.value = keys as number[]
  // 勾选时默认填充报废数量为可用量
  batches.value.forEach(b => {
    if (selectedKeys.value.includes(b.id) && !b.scrapQty) b.scrapQty = available(b)
  })
}

async function reloadBatches() {
  selectedKeys.value = []
  batches.value = []
  if (!erpTenantId.value || !warehouseId.value) return
  loading.value = true
  try {
    const res = await listScrapBatches(erpTenantId.value, warehouseId.value)
    if (isSuccess(res) && res.data) {
      batches.value = res.data.map(b => ({ ...b }))
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
  const rows = batches.value.filter(b => selectedKeys.value.includes(b.id))
  if (rows.length === 0) {
    message.warning('请勾选要报废的库位库存')
    return
  }
  const bad = rows.find(b => !b.scrapQty || b.scrapQty <= 0 || b.scrapQty > available(b))
  if (bad) {
    message.warning(`库位[${bad.locationCode}]报废数量不合法（需在 1~${available(bad)} 之间）`)
    return
  }
  submitting.value = true
  doRequest(
    createScrap({
      warehouseId: warehouseId.value,
      erpTenantId: erpTenantId.value,
      adjustmentReason: reason.value,
      items: rows.map(b => ({
        sourceInventoryId: b.sourceInventoryId || b.id,
        skuCode: b.skuCode,
        quantity: b.scrapQty as number
      }))
    }),
    {
      successMessage: '已发起报废，等待货主确认',
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
  reason.value = undefined
  batches.value = []
  selectedKeys.value = []
}

defineExpose({ open })
</script>
