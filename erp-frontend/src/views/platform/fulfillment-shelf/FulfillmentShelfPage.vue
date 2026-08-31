<template>
  <div class="fulfillment-page">
    <a-card :bordered="false" class="search-card">
      <a-form :model="searchModel" layout="inline" class="shelf-search">
        <a-form-item label="时间范围">
          <a-range-picker
            v-model:value="dateRange"
            value-format="YYYY-MM-DD"
            allow-clear
            style="width: 230px"
          />
        </a-form-item>
        <a-form-item label="货主">
          <a-select
            v-model:value="searchModel.erpTenantId"
            :options="ownerOptions"
            placeholder="全部"
            allow-clear
            show-search
            option-filter-prop="label"
            style="width: 170px"
          />
        </a-form-item>
        <a-form-item label="物流产品">
          <a-select
            v-model:value="searchModel.logisticsProductId"
            :options="logisticsProductOptions"
            placeholder="全部"
            allow-clear
            show-search
            option-filter-prop="label"
            style="width: 190px"
          />
        </a-form-item>
        <a-form-item label="所属仓库">
          <a-select
            v-model:value="searchModel.warehouseId"
            :options="warehouseOptions"
            placeholder="全部"
            allow-clear
            show-search
            option-filter-prop="label"
            style="width: 170px"
          />
        </a-form-item>
        <a-form-item class="search-actions-item">
          <search-actions :loading="loading" @search="handleSearch" @reset="resetSearch" />
        </a-form-item>
      </a-form>
    </a-card>

    <a-card title="订单下架" :bordered="false" class="table-card">
      <template #extra>
        <a-space>
          <a-button :disabled="!selectedIds.length" @click="handleAccept">确认下架</a-button>
          <a-button @click="load">刷新</a-button>
        </a-space>
      </template>
      <a-alert
        v-if="lastFailures.length"
        type="warning"
        show-icon
        closable
        :message="`本次有 ${lastFailures.length} 个订单未处理成功`"
        :description="lastFailures.join('；')"
        class="result-alert"
        @close="clearFailures"
      />
      <a-table
        row-key="id"
        :loading="loading"
        :data-source="orders"
        :columns="columns"
        :row-selection="rowSelection"
        :pagination="{ pageSize: 20 }"
        :scroll="{ x: 1520 }"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-badge :status="record.fulfillmentStatus === 'WAITING_SHELF' ? 'warning' : 'processing'" />
            {{ fulfillmentStatusText[record.fulfillmentStatus] || record.fulfillmentStatus }}
          </template>
          <template v-else-if="column.key === 'owner'">
            {{ ownerNameMap.get(record.erpTenantId) || `货主 #${record.erpTenantId}` }}
          </template>
          <template v-else-if="column.key === 'logisticsProduct'">
            <div>{{ record.logisticsProductName || '-' }}</div>
            <span v-if="productFeeText(record)" class="secondary-text">
              {{ productFeeText(record) }}
            </span>
          </template>
          <template v-else-if="column.key === 'warehouse'">
            {{ warehouseNameMap.get(record.warehouseId) || `仓库 #${record.warehouseId}` }}
          </template>
          <template v-else-if="column.key === 'dispatch'">
            <template v-if="canRedispatch(record)">
              <a-button type="link" size="small" :loading="retryingId === record.id" @click="handleRedispatch(record)">
                重新派单
              </a-button>
              <div v-if="record.dispatchError" class="dispatch-error">{{ record.dispatchError }}</div>
            </template>
            <span v-else>{{ record.dispatchStatus === 'SUCCEEDED' ? '已派单' : '-' }}</span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { listAllErpTenants } from '@/api/tenant'
import type { TenantBrief } from '@/api/tenant/types'
import { getWarehouseOptions } from '@/api/wms/warehouse'
import type { WarehouseOptionVO } from '@/api/wms/warehouse/types'
import {
  acceptFulfillmentOrders,
  listFulfillmentShelfOrders,
  redispatchFulfillmentOrders
} from '@/api/wms/fulfillment'
import type { FulfillmentOrder, FulfillmentShelfOrderQuery } from '@/api/wms/fulfillment/types'
import { fulfillmentStatusText } from '@/api/wms/fulfillment/types'
import { SearchActions } from '@/components/Search'
import { isShelfOrderSelectable } from '../fulfillment-workbench/workbench-flow'
import { createFailureAlertState } from './failure-alert'

defineOptions({ name: 'FulfillmentShelfPage' })
const loading = ref(false)
const retryingId = ref<number>()
const orders = ref<FulfillmentOrder[]>([])
const selectedIds = ref<number[]>([])
const {
  failures: lastFailures,
  show: showFailures,
  clear: clearFailures,
  dispose: disposeFailureAlert
} = createFailureAlertState()
const owners = ref<TenantBrief[]>([])
const warehouses = ref<WarehouseOptionVO[]>([])
const logisticsProductOptions = ref<Array<{ label: string; value: number }>>([])
const dateRange = ref<[string, string]>()
const searchModel = reactive<Pick<FulfillmentShelfOrderQuery, 'erpTenantId' | 'logisticsProductId' | 'warehouseId'>>({
  erpTenantId: undefined,
  logisticsProductId: undefined,
  warehouseId: undefined
})
let appliedQuery: FulfillmentShelfOrderQuery = {}

const ownerOptions = computed(() => owners.value.map(owner => ({
  label: owner.tenantName,
  value: owner.id
})))
const ownerNameMap = computed(() => new Map(owners.value.map(owner => [owner.id, owner.tenantName])))
const warehouseOptions = computed(() => warehouses.value.map(warehouse => ({
  label: warehouse.warehouseName,
  value: warehouse.id
})))
const warehouseNameMap = computed(() => new Map(
  warehouses.value.map(warehouse => [warehouse.id, warehouse.warehouseName])
))

const columns = [
  { title: '履约单号', dataIndex: 'fulfillmentNo', width: 210, fixed: 'left' as const },
  { title: '平台订单', dataIndex: 'sourceOrderNo', width: 210 },
  { title: '平台', dataIndex: 'sourceType', width: 100 },
  { title: '货主', key: 'owner', width: 130 },
  { title: '物流产品', key: 'logisticsProduct', width: 190 },
  { title: '所属仓库', key: 'warehouse', width: 150 },
  { title: '收件人', dataIndex: 'recipientName', width: 130 },
  { title: '状态', key: 'status', width: 130 },
  { title: '派单', key: 'dispatch', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', width: 180, fixed: 'right' as const }
]
const rowSelection = computed(() => ({
  fixed: true,
  selectedRowKeys: selectedIds.value,
  onChange: (keys: (string | number)[]) => { selectedIds.value = keys.map(Number) },
  getCheckboxProps: (record: FulfillmentOrder) => ({
    disabled: !isShelfOrderSelectable(record.fulfillmentStatus)
  })
}))

const load = async () => {
  loading.value = true
  try {
    const orderResult = await listFulfillmentShelfOrders(appliedQuery)
    if (isSuccess(orderResult)) {
      orders.value = orderResult.data || []
      mergeLogisticsProductOptions(orders.value)
      selectedIds.value = selectedIds.value.filter(id =>
        isShelfOrderSelectable(orders.value.find(item => item.id === id)?.fulfillmentStatus))
    }
  } finally { loading.value = false }
}
const loadOwners = async () => {
  const result = await listAllErpTenants()
  if (isSuccess(result)) owners.value = result.data || []
}
const loadWarehouses = async () => {
  const result = await getWarehouseOptions()
  if (isSuccess(result)) warehouses.value = result.data || []
}
const mergeLogisticsProductOptions = (items: FulfillmentOrder[]) => {
  const options = new Map(logisticsProductOptions.value.map(option => [option.value, option]))
  items.forEach(item => {
    if (item.logisticsProductId) {
      options.set(item.logisticsProductId, {
        value: item.logisticsProductId,
        label: item.logisticsProductName || `物流产品 #${item.logisticsProductId}`
      })
    }
  })
  logisticsProductOptions.value = Array.from(options.values())
}
const productFeeText = (record: FulfillmentOrder) => {
  const fee = record.logisticsProductActualFee ?? record.logisticsProductDefaultFee
  return fee === undefined || fee === null ? '' : `${fee} ${record.logisticsProductCurrency || ''}`.trim()
}
const handleSearch = () => {
  appliedQuery = {
    erpTenantId: searchModel.erpTenantId,
    logisticsProductId: searchModel.logisticsProductId,
    warehouseId: searchModel.warehouseId,
    startTime: dateRange.value?.[0] ? `${dateRange.value[0]} 00:00:00` : undefined,
    endTime: dateRange.value?.[1] ? `${dateRange.value[1]} 23:59:59` : undefined
  }
  selectedIds.value = []
  load()
}
const resetSearch = () => {
  searchModel.erpTenantId = undefined
  searchModel.logisticsProductId = undefined
  searchModel.warehouseId = undefined
  dateRange.value = undefined
  appliedQuery = {}
  selectedIds.value = []
  load()
}
const handleAccept = async () => {
  const ids = selectedIds.value.filter(id => orders.value.find(item => item.id === id)?.fulfillmentStatus === 'WAITING_SHELF')
  if (!ids.length) return message.warning('请选择待下架订单')
  const result = await acceptFulfillmentOrders(ids)
  if (isSuccess(result)) {
    showFailures(Object.entries(result.data?.failures || {}).map(([id, reason]) => `${id}: ${reason}`))
    const tasks = result.data?.tasks || []
    if (tasks.length) {
      Modal.info({
        title: '下架及拣货任务创建完成',
        width: 560,
        content: h('div', { class: 'dispatch-summary' }, tasks.map(task => h('div', {
          class: 'dispatch-summary-row'
        }, `${warehouseNameMap.value.get(task.warehouseId) || `仓库 #${task.warehouseId}`}：${task.taskNo}，${task.orderCount} 单 · ${task.totalQuantity} 件`)))
      })
    } else if (!lastFailures.value.length) {
      message.success('订单下架完成')
    }
    selectedIds.value = []
    await load()
  }
}
const canRedispatch = (record: FulfillmentOrder) =>
  record.fulfillmentStatus === 'WAITING_PICK' && record.dispatchStatus !== 'SUCCEEDED'
const handleRedispatch = async (record: FulfillmentOrder) => {
  retryingId.value = record.id
  try {
    const result = await redispatchFulfillmentOrders([record.id])
    if (!isSuccess(result)) return
    const failure = result.data?.failures?.[String(record.id)]
    if (failure) message.error(failure)
    else message.success('拣货任务已重新创建')
    await load()
  } finally {
    retryingId.value = undefined
  }
}
onMounted(() => {
  loadOwners()
  loadWarehouses()
  load()
})
onBeforeUnmount(disposeFailureAlert)
</script>

<style scoped>
.fulfillment-page {
  min-width: 0;
}
.search-card {
  min-width: 0;
  margin-bottom: 16px;
}
.search-card :deep(.ant-card-body),
.table-card,
.table-card :deep(.ant-card-body) {
  min-width: 0;
}
.table-card :deep(.ant-card-body) {
  overflow: hidden;
}
.result-alert {
  margin-bottom: 16px;
}
/* 与入库收货保持一致：筛选项按可用宽度自然换行，标签与控件不拆开。 */
.shelf-search {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 20px;
}
.shelf-search :deep(.ant-form-item) {
  flex: 0 0 auto;
  margin: 0;
}
.shelf-search :deep(.ant-form-item-row) {
  flex-wrap: nowrap;
  align-items: center;
}
.shelf-search :deep(.ant-form-item-label) {
  flex: 0 0 auto;
}
.shelf-search .search-actions-item {
  margin: 0;
}
.secondary-text {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
.dispatch-error {
  max-width: 160px;
  color: #cf1322;
  font-size: 12px;
  white-space: normal;
}
:global(.dispatch-summary) {
  display: grid;
  gap: 8px;
}
:global(.dispatch-summary-row) {
  line-height: 24px;
}
</style>
