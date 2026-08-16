<template>
  <div class="fulfillment-page">
    <a-card title="订单下架" :bordered="false">
      <template #extra>
        <a-space>
          <a-button :disabled="!selectedIds.length" @click="handleAccept">确认下架</a-button>
          <a-button type="primary" :disabled="!taskReadyIds.length" @click="handleCreateTask">
            生成拣货任务
          </a-button>
          <a-button @click="load">刷新</a-button>
        </a-space>
      </template>
      <a-alert
        v-if="lastFailures.length"
        type="warning"
        show-icon
        :message="`本次有 ${lastFailures.length} 个订单未处理成功`"
        :description="lastFailures.join('；')"
        class="result-alert"
      />
      <a-table
        row-key="id"
        :loading="loading"
        :data-source="orders"
        :columns="columns"
        :row-selection="rowSelection"
        :pagination="{ pageSize: 20 }"
        :scroll="{ x: 1050 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-badge :status="record.fulfillmentStatus === 'WAITING_SHELF' ? 'warning' : 'processing'" />
            {{ fulfillmentStatusText[record.fulfillmentStatus] || record.fulfillmentStatus }}
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card title="拣货任务" :bordered="false">
      <a-table row-key="id" :data-source="tasks" :columns="taskColumns" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'operate'">
            <a @click="drawerRef?.open(record.id)">开始作业</a>
          </template>
        </template>
      </a-table>
    </a-card>
    <picking-task-drawer ref="drawerRef" @changed="load" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import {
  acceptFulfillmentOrders,
  createFulfillmentPickTask,
  listFulfillmentPickTasks,
  listFulfillmentShelfOrders
} from '@/api/wms/fulfillment'
import type { FulfillmentOrder, FulfillmentPickTask } from '@/api/wms/fulfillment/types'
import { fulfillmentStatusText } from '@/api/wms/fulfillment/types'
import PickingTaskDrawer from '../fulfillment-workbench/PickingTaskDrawer.vue'

defineOptions({ name: 'FulfillmentShelfPage' })
const loading = ref(false)
const orders = ref<FulfillmentOrder[]>([])
const tasks = ref<FulfillmentPickTask[]>([])
const selectedIds = ref<number[]>([])
const lastFailures = ref<string[]>([])
const drawerRef = ref<InstanceType<typeof PickingTaskDrawer>>()
const taskReadyIds = computed(() => selectedIds.value.filter(id =>
  orders.value.find(item => item.id === id)?.fulfillmentStatus === 'WAITING_PICK'))

const columns = [
  { title: '履约单号', dataIndex: 'fulfillmentNo', width: 210 },
  { title: '平台订单', dataIndex: 'sourceOrderNo', width: 210 },
  { title: '平台', dataIndex: 'sourceType', width: 100 },
  { title: '仓库', dataIndex: 'warehouseId', width: 100 },
  { title: '收件人', dataIndex: 'recipientName', width: 130 },
  { title: '状态', key: 'status', width: 130 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 }
]
const taskColumns = [
  { title: '任务号', dataIndex: 'taskNo' },
  { title: '订单数', dataIndex: 'orderCount', width: 100 },
  { title: '总件数', dataIndex: 'totalQuantity', width: 100 },
  { title: '状态', dataIndex: 'taskStatus', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'operate', width: 110 }
]
const rowSelection = computed(() => ({
  selectedRowKeys: selectedIds.value,
  onChange: (keys: (string | number)[]) => { selectedIds.value = keys.map(Number) }
}))

const load = async () => {
  loading.value = true
  try {
    const [orderResult, taskResult] = await Promise.all([
      listFulfillmentShelfOrders(), listFulfillmentPickTasks()
    ])
    if (isSuccess(orderResult)) orders.value = orderResult.data || []
    if (isSuccess(taskResult)) tasks.value = taskResult.data || []
  } finally { loading.value = false }
}
const handleAccept = async () => {
  const ids = selectedIds.value.filter(id => orders.value.find(item => item.id === id)?.fulfillmentStatus === 'WAITING_SHELF')
  if (!ids.length) return message.warning('请选择待下架订单')
  const result = await acceptFulfillmentOrders(ids)
  if (isSuccess(result)) {
    lastFailures.value = Object.entries(result.data?.failures || {}).map(([id, reason]) => `${id}: ${reason}`)
    message.success(`成功处理 ${result.data?.successIds?.length || 0} 个订单`)
    selectedIds.value = []
    await load()
  }
}
const handleCreateTask = async () => {
  const result = await createFulfillmentPickTask(taskReadyIds.value)
  if (isSuccess(result)) {
    message.success(`拣货任务 ${result.data.taskNo} 已创建`)
    selectedIds.value = []
    await load()
    drawerRef.value?.open(result.data.id)
  }
}
onMounted(load)
</script>

<style scoped>
.fulfillment-page { display: grid; gap: 16px; }
.result-alert { margin-bottom: 16px; }
</style>
