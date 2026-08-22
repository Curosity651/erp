<template>
  <pro-table
    ref="tableRef"
    header-title="人工出库"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 980 }"
    size="middle"
  >
    <template #toolBarRender>
      <new-button v-if="hasPermission('wms:custom-outbound:add')" @click="formRef?.open()" />
    </template>
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'fulfillmentNo'">
        <div class="order-cell">
          <span>{{ record.fulfillmentNo }}</span>
          <small>{{ record.createTime || '-' }}</small>
        </div>
      </template>
      <template v-else-if="column.key === 'warehouse'">
        {{ warehouseNames.get(record.warehouseId) || `仓库 ${record.warehouseId}` }}
      </template>
      <template v-else-if="column.key === 'status'">
        <a-badge :status="statusBadge(record.fulfillmentStatus)" />
        {{ fulfillmentStatusText[record.fulfillmentStatus] || record.fulfillmentStatus }}
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="record.fulfillmentStatus === 'DRAFT'" @click="formRef?.open(record.id)">编辑</a>
          <confirm-text-button
            v-if="record.fulfillmentStatus === 'DRAFT'"
            text="提交"
            title="提交后将预占库存并进入海外仓待下架，是否继续？"
            @confirm="handleSubmit(record.id)"
          />
          <delete-text-button
            v-if="record.fulfillmentStatus === 'DRAFT'"
            @confirm="handleDelete(record.id)"
          />
        </operation-group>
      </template>
    </template>
  </pro-table>
  <manual-fulfillment-form-page ref="formRef" @saved="reload" />
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { NewButton, ConfirmTextButton, DeleteTextButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useAuthorize } from '@/hooks/permission'
import { isSuccess } from '@/api'
import { getWarehouseOptions } from '@/api/wms/warehouse'
import {
  deleteManualFulfillment,
  listManualFulfillment,
  submitManualFulfillment
} from '@/api/wms/fulfillment'
import type { FulfillmentStatus } from '@/api/wms/fulfillment/types'
import { fulfillmentStatusText } from '@/api/wms/fulfillment/types'
import ManualFulfillmentFormPage from './ManualFulfillmentFormPage.vue'

defineOptions({ name: 'ManualFulfillmentPage' })
const { hasPermission } = useAuthorize()
const tableRef = ref<ProTableInstanceExpose>()
const formRef = ref<InstanceType<typeof ManualFulfillmentFormPage>>()
const warehouseOptions = ref<{ id: number; warehouseName: string }[]>([])
const warehouseNames = computed(() => new Map(warehouseOptions.value.map((item) => [item.id, item.warehouseName])))

const columns: ProColumns[] = [
  { title: '人工出库单号', key: 'fulfillmentNo', width: 220, fixed: 'left' },
  { title: '仓库', key: 'warehouse', width: 160 },
  { title: '物流产品', dataIndex: 'logisticsProductName', width: 160 },
  { title: '收件人', dataIndex: 'recipientName', width: 130 },
  { title: '联系电话', dataIndex: 'recipientPhone', width: 150 },
  { title: '状态', key: 'status', width: 130 },
  { title: '操作', key: 'operate', width: 170, fixed: 'right' }
]

const tableRequest: TableRequest = async () => {
  const result = await listManualFulfillment()
  return {
    code: result.code,
    message: result.message,
    data: { records: result.data || [], total: result.data?.length || 0 }
  }
}

const reload = () => tableRef.value?.actionRef?.reload(false)
const handleSubmit = async (id: number) => {
  const result = await submitManualFulfillment(id)
  if (isSuccess(result)) reload()
}
const handleDelete = async (id: number) => {
  const result = await deleteManualFulfillment(id)
  if (isSuccess(result)) reload()
}
const statusBadge = (status: FulfillmentStatus) => {
  if (status === 'SHIPPED') return 'success'
  if (status === 'CANCELLED' || status === 'EXCEPTION') return 'error'
  if (status === 'DRAFT') return 'default'
  return 'processing'
}

onMounted(async () => {
  const result = await getWarehouseOptions()
  if (isSuccess(result)) warehouseOptions.value = result.data || []
})
</script>

<style scoped>
.order-cell { display: flex; flex-direction: column; gap: 2px; }
.order-cell small { color: #8c8c8c; }
</style>
