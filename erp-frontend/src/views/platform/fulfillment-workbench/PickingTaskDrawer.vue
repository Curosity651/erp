<template>
  <a-drawer v-model:open="visible" title="顺序拣货" width="760" destroy-on-close>
    <a-descriptions v-if="detail" :column="3" size="small" bordered>
      <a-descriptions-item label="任务号">{{ detail.task.taskNo }}</a-descriptions-item>
      <a-descriptions-item label="订单数">{{ detail.task.orderCount }}</a-descriptions-item>
      <a-descriptions-item label="总件数">{{ detail.task.totalQuantity }}</a-descriptions-item>
    </a-descriptions>
    <a-alert
      type="info"
      show-icon
      message="按顺序扫描履约订单号、库位码和内部 SKU 标签；当前订单全部拣齐后才会进入下一订单。"
      class="scan-help"
    />
    <a-form layout="vertical" @finish="submitScan">
      <a-row :gutter="12">
        <a-col :span="12"><a-form-item label="履约订单号"><a-input v-model:value="scan.fulfillmentNo" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="库位码"><a-input v-model:value="scan.locationCode" /></a-form-item></a-col>
        <a-col :span="16"><a-form-item label="内部 SKU"><a-input v-model:value="scan.warehouseSkuCode" /></a-form-item></a-col>
        <a-col :span="8"><a-form-item label="数量"><a-input-number v-model:value="scan.quantity" :min="1" style="width:100%" /></a-form-item></a-col>
      </a-row>
      <a-button type="primary" html-type="submit">确认扫描</a-button>
    </a-form>
    <a-table
      v-if="detail"
      row-key="id"
      size="small"
      :data-source="detail.lines"
      :columns="columns"
      :pagination="false"
      class="line-table"
    />
  </a-drawer>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { getFulfillmentPickTask, scanFulfillmentPickLine } from '@/api/wms/fulfillment'
import type { FulfillmentPickTaskDetail } from '@/api/wms/fulfillment/types'

const emit = defineEmits<{ changed: [] }>()
const visible = ref(false)
const detail = ref<FulfillmentPickTaskDetail>()
const scan = reactive({ fulfillmentNo: '', locationCode: '', warehouseSkuCode: '', quantity: 1 })
const columns = [
  { title: '库位', dataIndex: 'locationCode', width: 110 },
  { title: '内部 SKU', dataIndex: 'warehouseSkuCode' },
  { title: '商品 SKU', dataIndex: 'skuCode' },
  { title: '进度', customRender: ({ record }: any) => `${record.pickedQuantity}/${record.plannedQuantity}`, width: 90 }
]
const load = async (id: number) => {
  const result = await getFulfillmentPickTask(id)
  if (isSuccess(result)) detail.value = result.data
}
const open = async (id: number) => { visible.value = true; await load(id) }
const submitScan = async () => {
  if (!detail.value) return
  const result = await scanFulfillmentPickLine({ taskId: detail.value.task.id, ...scan })
  if (isSuccess(result)) {
    message.success('扫描已记录')
    await load(detail.value.task.id)
    emit('changed')
  }
}
defineExpose({ open })
</script>

<style scoped>
.scan-help { margin: 16px 0; }
.line-table { margin-top: 18px; }
</style>
