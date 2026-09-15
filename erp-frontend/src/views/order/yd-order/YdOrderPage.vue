<template>
  <!-- 查询表单 -->
  <yd-order-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="Yandex 订单"
    row-key="id"
    class-name="order-pro-table"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1400 }"
    :row-selection="rowSelection"
    :pagination="{
      pageSizeOptions: ['10', '20', '50', '100', '200']
    }"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <a-popconfirm
        title="是否确认进行全量订单信息同步?"
        ok-text="是"
        cancel-text="否"
        @confirm="handleSyncAllOrders"
      >
        <a-button type="primary" danger>
          <InteractionOutlined />
          同步全量订单
        </a-button>
      </a-popconfirm>
    </template>

    <!-- 批量操作 -->
    <template #tableAlertOptionRender="slotProps">
      <a-space :size="16">
        <a-dropdown>
          <a @click.prevent> 批量操作&nbsp;<DownOutlined /> </a>
          <template #overlay>
            <a-menu>
              <a-menu-item @click="handleBatchAction('confirm', slotProps.selectedRows)">
                批量确认
              </a-menu-item>
              <a-menu-item @click="handleBatchAction('sync', slotProps.selectedRows)">
                同步选中
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a @click="slotProps.onCleanSelected">
          {{ slotProps.intl.getMessage('alert.clear', '清空') }}
        </a>
      </a-space>
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <!-- 订单基础信息 -->
      <template v-if="column.key === 'orderInfo'">
        <OrderBasicInfoCell
          :platform="record.platform || 'yandex'"
          :platform-order-id="record.platformOrderId"
          :erp-order-id="record.id"
          :shop-name="record.erpShopName"
          fulfillment-type="FBS"
          :order-time="record.platformCreatedAtMoscow"
          timezone="MSK"
        />
      </template>

      <!-- SKU 信息 -->
      <template v-else-if="column.key === 'skuInfo'">
        <SkuInfoCell :items="record.items" />
      </template>

      <!-- 商品总价列 -->
      <template v-else-if="column.key === 'productAmount'">
        <div class="order-cell product-amount">
          <div class="amount-primary">
            {{ formatAmount(record.productTotalAmount, record.productCurrencyCode) }}
          </div>
          <div class="amount-secondary">
            {{ formatAmount(record.productAmountCny, 'CNY') }}
          </div>
        </div>
      </template>

      <!-- 金额 & 状态组合列 -->
      <template v-else-if="column.key === 'amountStatus'">
        <OrderAmountStatusCell
          :total-amount="record.totalAmount"
          :currency-code="record.currencyCode"
          :converted-amount="record.convertedAmount"
          :converted-currency-code="record.convertedCurrencyCode || 'CNY'"
          :erp-status="record.businessStatus"
          :erp-status-mapping="mapOwnerOrderStatus(record.businessStatus)"
          amount-order="total-first"
        />
      </template>

      <!-- 仓库信息 -->
      <template v-else-if="column.key === 'warehouse'">
        <div class="order-cell warehouse">
          <div v-if="record.warehouseId" class="warehouse__name">仓库 {{ record.warehouseId }}</div>
          <div v-else class="warehouse__name text-gray-400">-</div>
        </div>
      </template>

      <!-- 运营状态 -->
      <template v-else-if="column.key === 'operationStatus'">
        <OrderOperationStatusCell :locked="record.locked" />
      </template>

      <!-- 操作列 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="record.locked !== 1" @click="openConfirmDialog([record])">确认</a>
          <a-popconfirm title="是否确认进行同步操作?" @confirm="onSync(record)">
            <a>同步</a>
          </a-popconfirm>
          <a v-if="record.locked !== 1" @click="onLock(record.id)">锁定</a>
          <a v-else @click="onUnlock(record.id)">解锁</a>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 确认发货弹窗 -->
  <OrderConfirmModal
    v-model:open="confirmModal.open"
    v-model:logistics-product-id="confirmModal.logisticsProductId"
    v-model:wms-warehouse-id="confirmModal.wmsWarehouseId"
    :loading="confirmModal.loading"
    :eligible="confirmModal.eligible"
    :ineligible="confirmModal.ineligible"
    @confirm="submitConfirm"
  />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import YdOrderPageSearch from './YdOrderPageSearch.vue'
import { OperationGroup } from '@/components/Operation'
import { mergePageParam } from '@/utils/page-utils'
import {
  pageYdOrder,
  syncYdOrdersByIds,
  syncAllYdOrders,
  lockYdOrder,
  unlockYdOrder
} from '@/api/order/yd-order'
import type { YdOrderQO, YdOrderPageVO } from '@/api/order/yd-order/types'
import { message } from 'ant-design-vue'
import { formatAmount } from '@/utils/currency-utils'
import SkuInfoCell from '@/components/Sku/SkuInfoCell.vue'
import { InteractionOutlined, DownOutlined } from '@ant-design/icons-vue'
import {
  OrderBasicInfoCell,
  OrderAmountStatusCell,
  OrderOperationStatusCell,
  OrderConfirmModal
} from '@/views/order/components'
import { mapOwnerOrderStatus } from '@/utils/order-status-mapper'
import { useOrderConfirm } from '@/views/order/hooks/useOrderConfirm.ts'
import { useOrderLock } from '@/views/order/hooks/useOrderLock.ts'
import { useOrderSync } from '@/views/order/hooks/useOrderSync.ts'

defineOptions({ name: 'YdOrderPage' })

const tableRef = ref<ProTableInstanceExpose>()

const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

let searchParams: YdOrderQO = {}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const defaultSorter = { platformCreatedAt: 'descend' } as any
  const pageParam = mergePageParam(params, { ...defaultSorter, ...sorter }, filter)
  return pageYdOrder({ ...pageParam, ...searchParams })
}

const searchTable = (params: YdOrderQO) => {
  searchParams = params
  reloadTable(true)
}

const columns: ProColumns[] = [
  { title: '订单基础信息', key: 'orderInfo', width: 200, fixed: 'left' },
  { title: '商品信息', key: 'skuInfo', ellipsis: true, width: 380 },
  { title: '商品总价', key: 'productAmount', width: 140 },
  { title: '订单金额 & 状态', key: 'amountStatus', width: 220 },
  { title: '仓库信息', key: 'warehouse', ellipsis: true, width: 100 },
  { title: '运营状态', key: 'operationStatus', width: 140 },
  {
    key: 'operate',
    title: '操作',
    align: 'center',
    width: 200,
    fixed: 'right'
  }
]

// 行选择
const rowSelection = {
  type: 'checkbox',
  alwaysShowAlert: true
}

// Yandex 确认条件
const canConfirm = (row: YdOrderPageVO) => {
  const isYandex = (row?.platform || '').toLowerCase() === 'yandex'
  const statusOk = row?.erpStatus === 'READY_TO_SHIP'
  const hasPlatformOrderId = !!row?.platformOrderId
  const notLocked = row?.locked !== 1
  return isYandex && statusOk && hasPlatformOrderId && notLocked
}

const confirmReasonOf = (row: YdOrderPageVO) => {
  if ((row?.platform || '').toLowerCase() !== 'yandex') return '非 Yandex 平台'
  if (!row?.platformOrderId) return '缺少平台订单号'
  if (row?.locked === 1) return '订单已锁定'
  if (row?.erpStatus !== 'READY_TO_SHIP') return `状态不支持：${row?.erpStatus || '-'}`
  return '未知原因'
}

// 使用 composables
const { confirmModal, openConfirmDialog, submitConfirm } = useOrderConfirm<YdOrderPageVO>({
  canConfirm,
  reasonOf: confirmReasonOf,
  reloadTable: () => reloadTable(),
})

const { onLock, onUnlock } = useOrderLock({
  lockApi: lockYdOrder,
  unlockApi: unlockYdOrder,
  reloadTable: () => reloadTable()
})

const { onSync, handleSyncAllOrders } = useOrderSync({
  platform: 'yandex',
  syncApi: syncYdOrdersByIds,
  syncAllApi: syncAllYdOrders,
  reloadTable: () => reloadTable()
})

// 批量操作
function handleBatchAction(action: string, selectedRows: YdOrderPageVO[]) {
  switch (action) {
    case 'confirm':
      openConfirmDialog(selectedRows)
      break
    case 'sync':
      onSync(selectedRows)
      break
  }
}
</script>

<style scoped>
@import '../styles/order-table.css';
</style>
