<template>
  <a-card :bordered="false" class="search-card">
    <a-form :model="searchModel" layout="inline" class="shipping-search">
      <a-form-item label="货主">
        <platform-owner-select
          v-model:value="searchModel.erpTenantId"
          placeholder="全部"
          width="150px"
        />
      </a-form-item>
      <a-form-item label="仓库">
        <warehouse-select
          v-model:value="searchModel.warehouseId"
          placeholder="全部"
          width="160px"
        />
      </a-form-item>
      <a-form-item label="状态">
        <a-select
          v-model:value="searchModel.fulfillmentStatus"
          :options="statusOptions"
          placeholder="全部"
          allow-clear
          style="width: 120px"
        />
      </a-form-item>
      <a-form-item label="日期范围">
        <a-range-picker
          v-model:value="dateRange"
          value-format="YYYY-MM-DD"
          allow-clear
          style="width: 230px"
        />
      </a-form-item>
      <a-form-item label="操作人员">
        <user-select
          v-model:value="searchModel.shippedBy"
          :options="userOptions"
          :loading="usersLoading"
          placeholder="全部"
          style="width: 150px"
        />
      </a-form-item>
      <a-form-item class="search-actions-item">
        <search-actions :loading="tableRef?.loading" @search="searchTable" @reset="resetSearch" />
      </a-form-item>
    </a-form>
  </a-card>

  <a-alert
    v-if="shipFailures.length"
    type="warning"
    show-icon
    :description="shipFailures.join('；')"
    class="result-alert"
  />

  <pro-table
    ref="tableRef"
    header-title="出库作业"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :row-selection="rowSelection"
    :scroll="{ x: 1760 }"
    size="middle"
  >
    <template #toolBarRender>
      <a-button
        type="primary"
        :disabled="!selectedIds.length"
        :loading="shipping"
        @click="handleShip(selectedIds)"
      >
        批量签出<span v-if="selectedIds.length">（{{ selectedIds.length }}）</span>
      </a-button>
    </template>
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'source'">
        <div>{{ record.sourceOrderNo }}</div>
        <span class="secondary-text">{{ record.sourceType }}</span>
      </template>
      <template v-else-if="column.key === 'product'">
        <div>{{ record.logisticsProductName || '-' }}</div>
        <span class="secondary-text">
          {{ record.logisticsProductActualFee ?? '-' }} {{ record.logisticsProductCurrency || '' }}
        </span>
      </template>
      <template v-else-if="column.key === 'transport'">
        <div>{{ record.carrierName || '-' }} / {{ record.shippingMethod || '-' }}</div>
        <span class="secondary-text">{{ record.trackingNo || '-' }}</span>
      </template>
      <template v-else-if="column.key === 'status'">
        <a-badge
          :status="statusBadge(record.fulfillmentStatus)"
          :text="statusText(record.fulfillmentStatus)"
        />
      </template>
      <template v-else-if="column.key === 'operate'">
        <a
          v-if="record.fulfillmentStatus === 'PACKED'"
          :class="{ disabled: shipping }"
          @click="!shipping && handleShip([record.id])"
          >签出</a
        >
        <span v-else class="secondary-text">
          {{ record.fulfillmentStatus === 'SHIPPED' ? '已签出' : '等待打包' }}
        </span>
      </template>
    </template>
  </pro-table>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { SearchActions } from '@/components/Search'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import UserSelect from '@/components/Lov/UserSelect.vue'
import { useUserData } from '@/hooks/use-user-data'
import { mergePageParam } from '@/utils/page-utils'
import { isSuccess } from '@/api'
import { pageFulfillmentShippingOrders, shipFulfillmentOrders } from '@/api/wms/fulfillment'
import type {
  FulfillmentShippingOrder,
  FulfillmentShippingQuery
} from '@/api/wms/fulfillment/types'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'
import { failedOrderIds } from './workbench-flow'

defineOptions({ name: 'FulfillmentWorkbenchPage' })

const tableRef = ref<ProTableInstanceExpose>()
const selectedIds = ref<number[]>([])
const shipFailures = ref<string[]>([])
const shipping = ref(false)
const dateRange = ref<[string, string]>()
const searchModel = reactive<FulfillmentShippingQuery>({})
let searchParams: FulfillmentShippingQuery = {}
const { allUsers: userOptions, loading: usersLoading, loadAllUsers } = useUserData()

const statusOptions = [
  { label: '待打包', value: 'WAITING_PACK' },
  { label: '已打包', value: 'PACKED' },
  { label: '已签出', value: 'SHIPPED' }
]

const tableRequest: TableRequest = (params, sorter, filter) =>
  pageFulfillmentShippingOrders({
    ...mergePageParam(params, sorter, filter),
    ...searchParams
  })

const reloadTable = (resetPageIndex?: boolean) => tableRef.value?.actionRef?.reload(resetPageIndex)
useTableActivateReload(() => reloadTable(false))

const searchTable = () => {
  searchParams = {
    ...searchModel,
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1]
  }
  selectedIds.value = []
  reloadTable(true)
}

const resetSearch = () => {
  Object.assign(searchModel, {
    erpTenantId: undefined,
    warehouseId: undefined,
    fulfillmentStatus: undefined,
    shippedBy: undefined
  })
  dateRange.value = undefined
  searchTable()
}

const rowSelection = computed(() => ({
  fixed: true,
  selectedRowKeys: selectedIds.value,
  getCheckboxProps: (record: FulfillmentShippingOrder) => ({
    disabled: record.fulfillmentStatus !== 'PACKED'
  }),
  onChange: (keys: (string | number)[]) => {
    selectedIds.value = keys.map(Number)
  }
}))

const handleShip = async (ids: number[]) => {
  if (!ids.length || shipping.value) return
  shipping.value = true
  try {
    const result = await shipFulfillmentOrders(ids)
    if (isSuccess(result)) {
      shipFailures.value = Object.entries(result.data.failures || {}).map(
        ([id, reason]) => `${id}: ${reason}`
      )
      message.success(`成功签出 ${result.data.successIds?.length || 0} 个订单`)
      selectedIds.value = failedOrderIds(result.data.failures)
      reloadTable(false)
    }
  } finally {
    shipping.value = false
  }
}

const statusText = (status: string) =>
  ({ WAITING_PACK: '待打包', PACKED: '已打包', SHIPPED: '已签出' })[status] || status

const statusBadge = (status: string) =>
  (({ WAITING_PACK: 'processing', PACKED: 'warning', SHIPPED: 'success' })[status] ||
    'default') as any

const columns: ProColumns[] = [
  {
    title: '履约单号',
    dataIndex: 'fulfillmentNo',
    key: 'fulfillmentNo',
    width: 200,
    fixed: 'left'
  },
  { title: '平台订单', key: 'source', width: 190 },
  { title: '货主', dataIndex: 'ownerName', key: 'ownerName', width: 140, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 150, ellipsis: true },
  { title: '收件人', dataIndex: 'recipientName', key: 'recipientName', width: 120, ellipsis: true },
  { title: '物流产品/费用', key: 'product', width: 190 },
  { title: '承运信息/跟踪号', key: 'transport', width: 250 },
  { title: '重量(kg)', dataIndex: 'packageWeightKg', key: 'packageWeightKg', width: 100 },
  { title: '状态', key: 'status', width: 110, align: 'center' },
  {
    title: '操作人员',
    dataIndex: 'shippedByName',
    key: 'shippedByName',
    width: 110,
    ellipsis: true
  },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '签出时间', dataIndex: 'shippedTime', key: 'shippedTime', width: 170 },
  { title: '操作', key: 'operate', width: 90, align: 'center', fixed: 'right' }
]

onMounted(loadAllUsers)
</script>

<style scoped>
.search-card {
  margin-bottom: 16px;
}
.search-card :deep(.ant-card-body) {
  min-width: 0;
}
.shipping-search {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 20px;
}
.shipping-search :deep(.ant-form-item) {
  flex: 0 0 auto;
  margin: 0;
}
.shipping-search :deep(.ant-form-item-row) {
  flex-wrap: nowrap;
  align-items: center;
}
.shipping-search :deep(.ant-form-item-label) {
  flex: 0 0 auto;
}
.search-actions-item {
  margin: 0;
}
.result-alert {
  margin-bottom: 16px;
}
.secondary-text {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
.disabled {
  color: rgba(0, 0, 0, 0.25);
  cursor: not-allowed;
}
</style>
