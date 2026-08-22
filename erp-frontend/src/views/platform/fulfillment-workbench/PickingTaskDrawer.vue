<template>
  <a-drawer v-model:open="visible" title="逐单取货与打包" width="860" destroy-on-close>
    <a-descriptions v-if="detail" :column="3" size="small" bordered>
      <a-descriptions-item label="任务号">{{ detail.task.taskNo }}</a-descriptions-item>
      <a-descriptions-item label="订单进度">{{ completedOrders }}/{{ detail.task.orderCount }}</a-descriptions-item>
      <a-descriptions-item label="总件数">{{ detail.task.totalQuantity }}</a-descriptions-item>
    </a-descriptions>

    <a-result v-if="detail && !currentOrder" status="success" title="本拣货任务已经全部完成" />
    <template v-else-if="currentOrder">
      <div class="current-order">
        <div>
          <span class="section-label">当前订单</span>
          <strong>{{ currentOrder.fulfillmentNo }}</strong>
          <span class="muted">{{ currentOrder.sourceOrderNo }}</span>
        </div>
        <a-tag color="blue">第 {{ detail?.currentOrder?.taskOrder.sequenceNo }} 单</a-tag>
      </div>

      <a-alert
        v-if="picking"
        type="info"
        show-icon
        message="只处理当前订单：按下方位置取货，扫描库位码和箱上的内部 SKU 条形码。"
        class="workflow-alert"
      />
      <a-form v-if="picking" layout="vertical" @finish="submitScan">
        <a-row :gutter="12">
          <a-col :span="8"><a-form-item label="履约订单号"><a-input v-model:value="scan.fulfillmentNo" disabled /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="库位码" required><a-input v-model:value="scan.locationCode" autofocus /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="内部 SKU" required><a-input v-model:value="scan.warehouseSkuCode" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="8"><a-form-item label="本次数量" required><a-input-number v-model:value="scan.quantity" :min="1" style="width:100%" /></a-form-item></a-col>
          <a-col :span="16" class="scan-action"><a-button type="primary" html-type="submit">确认取货</a-button></a-col>
        </a-row>
      </a-form>

      <a-alert
        v-else
        type="success"
        show-icon
        message="当前订单已拣齐，请立即打印并粘贴平台面单，完成打包后才会开放下一订单。"
        class="workflow-alert"
      />
      <a-form v-if="!picking" layout="vertical">
        <a-space class="label-actions">
          <a-button type="primary" @click="handleLabel">打印平台面单</a-button>
          <a-tag :color="currentOrder.labelVerifiedTime ? 'green' : currentOrder.labelFetchedTime ? 'blue' : 'default'">
            {{ currentOrder.labelVerifiedTime ? '面单已核验' : currentOrder.labelFetchedTime ? '面单待核验' : '尚未获取面单' }}
          </a-tag>
        </a-space>
        <a-form-item label="扫描已粘贴的面单条码" required>
          <a-input v-model:value="packForm.barcode" placeholder="扫描当前订单包裹上的平台面单" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="8"><a-form-item label="承运商" required><a-input v-model:value="packForm.carrierName" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="运输方式" required><a-input v-model:value="packForm.shippingMethod" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="跟踪号" required><a-input v-model:value="packForm.trackingNo" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="8"><a-form-item label="包裹重量（kg）" required><a-input-number v-model:value="packForm.packageWeightKg" :min="0.01" style="width:100%" /></a-form-item></a-col>
          <a-col :span="16" class="scan-action"><a-button type="primary" @click="handlePack">完成打包，处理下一单</a-button></a-col>
        </a-row>
      </a-form>

      <a-table
        row-key="id"
        size="small"
        :data-source="detail?.currentOrder?.routeLines || []"
        :columns="columns"
        :pagination="false"
        class="line-table"
      />
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import {
  getFulfillmentPickTask, packFulfillment, printFulfillmentLabel,
  scanFulfillmentPickLine, verifyFulfillmentLabel
} from '@/api/wms/fulfillment'
import type { FulfillmentPickTaskDetail } from '@/api/wms/fulfillment/types'
import { canPrintLabel } from './workbench-flow'

const emit = defineEmits<{ changed: [] }>()
const visible = ref(false)
const detail = ref<FulfillmentPickTaskDetail>()
const scan = reactive({ fulfillmentNo: '', locationCode: '', warehouseSkuCode: '', quantity: 1 })
const packForm = reactive({ barcode: '', carrierName: '', shippingMethod: '', trackingNo: '', packageWeightKg: 1 })
const currentOrder = computed(() => detail.value?.currentOrder?.fulfillmentOrder)
const picking = computed(() => !canPrintLabel(detail.value?.currentOrder?.taskOrder.orderStatus))
const completedOrders = computed(() => detail.value?.orders.filter(item => item.orderStatus === 'COMPLETED').length || 0)
const columns = [
  { title: '取货位置', dataIndex: 'locationCode', width: 120 },
  { title: '内部 SKU', dataIndex: 'warehouseSkuCode' },
  { title: '商品 SKU', dataIndex: 'skuCode' },
  { title: '取货进度', customRender: ({ record }: any) => `${record.pickedQuantity}/${record.plannedQuantity}`, width: 100 }
]

watch(currentOrder, order => {
  if (!order) return
  scan.fulfillmentNo = order.fulfillmentNo
  scan.locationCode = ''
  scan.warehouseSkuCode = ''
  scan.quantity = 1
  Object.assign(packForm, {
    barcode: '', carrierName: order.carrierName || '', shippingMethod: order.shippingMethod || '',
    trackingNo: order.trackingNo || '', packageWeightKg: order.packageWeightKg || 1
  })
})
const load = async (id: number) => {
  const result = await getFulfillmentPickTask(id)
  if (isSuccess(result)) detail.value = result.data
}
const open = async (id: number) => { visible.value = true; await load(id) }
const submitScan = async () => {
  if (!detail.value || !scan.locationCode || !scan.warehouseSkuCode) return message.warning('请扫描库位码和内部 SKU')
  const result = await scanFulfillmentPickLine({ taskId: detail.value.task.id, ...scan })
  if (isSuccess(result)) { message.success('取货已记录'); await load(detail.value.task.id); emit('changed') }
}
const handleLabel = async () => {
  if (!currentOrder.value) return
  const result = await printFulfillmentLabel(currentOrder.value.id)
  if (isSuccess(result)) {
    message.success('面单已生成，请打印并贴到当前包裹')
    if (result.data.labelUrl) window.open(result.data.labelUrl, '_blank')
    await load(detail.value!.task.id)
  }
}
const handlePack = async () => {
  if (!currentOrder.value || !packForm.barcode) return message.warning('请扫描已粘贴的面单条码')
  if (!packForm.carrierName || !packForm.shippingMethod || !packForm.trackingNo) return message.warning('请填写承运商、运输方式和跟踪号')
  const verified = await verifyFulfillmentLabel(currentOrder.value.id, packForm.barcode)
  if (!isSuccess(verified)) return
  const packed = await packFulfillment(currentOrder.value.id, packForm)
  if (isSuccess(packed)) { message.success('当前订单打包完成，下一订单已开放'); await load(detail.value!.task.id); emit('changed') }
}
defineExpose({ open })
</script>

<style scoped>
.current-order { display: flex; justify-content: space-between; align-items: center; margin-top: 18px; }
.current-order > div { display: flex; align-items: baseline; gap: 12px; }
.section-label, .muted { color: rgba(0, 0, 0, 0.45); }
.workflow-alert { margin: 14px 0; }
.label-actions { margin-bottom: 14px; }
.scan-action { display: flex; align-items: end; padding-bottom: 24px; }
.line-table { margin-top: 8px; }
</style>
