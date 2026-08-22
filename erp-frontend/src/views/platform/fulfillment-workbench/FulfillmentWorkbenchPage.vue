<template>
  <div class="workbench-page">
    <fulfillment-shelf-page />
    <a-card title="已打包待签出" :bordered="false">
      <template #extra>
        <a-space>
          <a-button type="primary" :disabled="!selectedIds.length" @click="handleShip">批量签出</a-button>
          <a-button @click="load">刷新</a-button>
        </a-space>
      </template>
      <a-alert v-if="shipFailures.length" type="warning" show-icon :description="shipFailures.join('；')" class="result-alert" />
      <a-table
        row-key="id"
        :data-source="orders"
        :columns="columns"
        :row-selection="rowSelection"
        :pagination="{ pageSize: 20 }"
        :scroll="{ x: 1280 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'product'">
            <div>{{ record.logisticsProductName || '-' }}</div>
            <span class="fee">{{ record.logisticsProductActualFee ?? '-' }} {{ record.logisticsProductCurrency || '' }}</span>
          </template>
          <template v-else-if="column.key === 'transport'">
            <div>{{ record.carrierName || '-' }} / {{ record.shippingMethod || '-' }}</div>
            <span class="fee">{{ record.trackingNo || '-' }}</span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { listFulfillmentShippingOrders, shipFulfillmentOrders } from '@/api/wms/fulfillment'
import type { FulfillmentOrder } from '@/api/wms/fulfillment/types'
import FulfillmentShelfPage from '../fulfillment-shelf/FulfillmentShelfPage.vue'
import { failedOrderIds } from './workbench-flow'

defineOptions({ name: 'FulfillmentWorkbenchPage' })
const orders = ref<FulfillmentOrder[]>([])
const selectedIds = ref<number[]>([])
const shipFailures = ref<string[]>([])
const columns = [
  { title: '履约单号', dataIndex: 'fulfillmentNo', width: 190 },
  { title: '平台订单', dataIndex: 'sourceOrderNo', width: 190 },
  { title: '平台', dataIndex: 'sourceType', width: 90 },
  { title: '收件人', dataIndex: 'recipientName', width: 110 },
  { title: '物流产品/费用', key: 'product', width: 180 },
  { title: '承运信息/跟踪号', key: 'transport', width: 260 },
  { title: '重量(kg)', dataIndex: 'packageWeightKg', width: 100 },
  { title: '状态', dataIndex: 'fulfillmentStatus', width: 100 }
]
const rowSelection = computed(() => ({ selectedRowKeys: selectedIds.value, onChange: (keys: (string | number)[]) => { selectedIds.value = keys.map(Number) } }))
const load = async () => {
  const result = await listFulfillmentShippingOrders()
  if (isSuccess(result)) orders.value = (result.data || []).filter(item => item.fulfillmentStatus === 'PACKED')
}
const handleShip = async () => {
  const result = await shipFulfillmentOrders(selectedIds.value)
  if (isSuccess(result)) {
    shipFailures.value = Object.entries(result.data.failures || {}).map(([id, reason]) => `${id}: ${reason}`)
    message.success(`成功签出 ${result.data.successIds?.length || 0} 个订单`)
    selectedIds.value = failedOrderIds(result.data.failures)
    await load()
  }
}
onMounted(load)
</script>

<style scoped>
.workbench-page { display: grid; gap: 16px; }
.result-alert { margin-bottom: 16px; }
.fee { color: rgba(0, 0, 0, 0.45); font-size: 12px; }
</style>
