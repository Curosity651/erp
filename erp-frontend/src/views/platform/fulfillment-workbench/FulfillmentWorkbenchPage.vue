<template>
  <a-tabs v-model:active-key="activeTab" class="workbench-tabs">
    <a-tab-pane key="pick" tab="下架与拣货">
      <fulfillment-shelf-page />
    </a-tab-pane>
    <a-tab-pane key="pack" tab="面单、打包与签出">
      <a-card :bordered="false">
        <template #title>待打包订单</template>
        <template #extra>
          <a-space>
            <a-button type="primary" :disabled="!packedIds.length" @click="handleShip">批量签出</a-button>
            <a-button @click="load">刷新</a-button>
          </a-space>
        </template>
        <a-alert v-if="shipFailures.length" type="warning" show-icon :description="shipFailures.join('；')" />
        <a-table
          row-key="id"
          :data-source="orders"
          :columns="columns"
          :row-selection="rowSelection"
          :pagination="{ pageSize: 20 }"
          :scroll="{ x: 1150 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'label'">
              <a-tag :color="record.labelVerifiedTime ? 'green' : record.labelFetchedTime ? 'blue' : 'default'">
                {{ record.labelVerifiedTime ? '已核验' : record.labelFetchedTime ? '待核验' : '未获取' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'operate'">
              <a-space>
                <a @click="handleLabel(record)">面单</a>
                <a @click="openPack(record)">核验/打包</a>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-tab-pane>
  </a-tabs>

  <a-modal v-model:open="packVisible" title="面单核验与打包" @ok="handlePack">
    <a-form layout="vertical">
      <a-form-item label="扫描面单条码" required>
        <a-input v-model:value="packForm.barcode" placeholder="扫描当前包裹上的平台面单" />
      </a-form-item>
      <a-row :gutter="12">
        <a-col :span="12"><a-form-item label="承运商"><a-input v-model:value="packForm.carrierName" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="跟踪号"><a-input v-model:value="packForm.trackingNo" /></a-form-item></a-col>
      </a-row>
      <a-form-item label="包裹重量（kg）"><a-input-number v-model:value="packForm.packageWeightKg" :min="0.01" style="width:100%" /></a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import {
  listFulfillmentShippingOrders, packFulfillment, printFulfillmentLabel,
  shipFulfillmentOrders, verifyFulfillmentLabel
} from '@/api/wms/fulfillment'
import type { FulfillmentOrder } from '@/api/wms/fulfillment/types'
import FulfillmentShelfPage from '../fulfillment-shelf/FulfillmentShelfPage.vue'

defineOptions({ name: 'FulfillmentWorkbenchPage' })
const activeTab = ref('pick')
const orders = ref<FulfillmentOrder[]>([])
const selectedIds = ref<number[]>([])
const shipFailures = ref<string[]>([])
const packVisible = ref(false)
const current = ref<FulfillmentOrder>()
const packForm = reactive({ barcode: '', carrierName: '', trackingNo: '', packageWeightKg: 1 })
const packedIds = computed(() => selectedIds.value.filter(id => orders.value.find(item => item.id === id)?.fulfillmentStatus === 'PACKED'))
const columns = [
  { title: '履约单号', dataIndex: 'fulfillmentNo', width: 200 },
  { title: '平台订单', dataIndex: 'sourceOrderNo', width: 200 },
  { title: '平台', dataIndex: 'sourceType', width: 90 },
  { title: '收件人', dataIndex: 'recipientName', width: 120 },
  { title: '面单', key: 'label', width: 100 },
  { title: '承运商', dataIndex: 'carrierName', width: 120 },
  { title: '跟踪号', dataIndex: 'trackingNo', width: 170 },
  { title: '状态', dataIndex: 'fulfillmentStatus', width: 120 },
  { title: '操作', key: 'operate', width: 150, fixed: 'right' }
]
const rowSelection = computed(() => ({ selectedRowKeys: selectedIds.value, onChange: (keys: (string | number)[]) => { selectedIds.value = keys.map(Number) } }))
const load = async () => { const result = await listFulfillmentShippingOrders(); if (isSuccess(result)) orders.value = result.data || [] }
const handleLabel = async (record: FulfillmentOrder) => {
  const result = await printFulfillmentLabel(record.id)
  if (isSuccess(result)) {
    message.success('面单已生成，请打印并贴到当前包裹')
    if (result.data.labelUrl) window.open(result.data.labelUrl, '_blank')
    await load()
  }
}
const openPack = (record: FulfillmentOrder) => { current.value = record; Object.assign(packForm, { barcode: '', carrierName: record.carrierName || '', trackingNo: record.trackingNo || '', packageWeightKg: record.packageWeightKg || 1 }); packVisible.value = true }
const handlePack = async () => {
  if (!current.value || !packForm.barcode) return message.warning('请扫描面单条码')
  const verified = await verifyFulfillmentLabel(current.value.id, packForm.barcode)
  if (!isSuccess(verified)) return
  const packed = await packFulfillment(current.value.id, packForm)
  if (isSuccess(packed)) { message.success('打包完成'); packVisible.value = false; await load() }
}
const handleShip = async () => {
  const result = await shipFulfillmentOrders(packedIds.value)
  if (isSuccess(result)) {
    shipFailures.value = Object.entries(result.data.failures || {}).map(([id, reason]) => `${id}: ${reason}`)
    message.success(`成功签出 ${result.data.successIds?.length || 0} 个订单`)
    selectedIds.value = []
    await load()
  }
}
onMounted(load)
</script>

<style scoped>
.workbench-tabs { min-height: 100%; }
</style>
