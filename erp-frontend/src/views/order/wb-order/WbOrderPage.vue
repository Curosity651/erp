<template>
  <!-- 查询表单 -->
  <wb-order-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="Wildberries 订单"
    row-key="id"
    class-name="order-pro-table"
    :request="tableRequest"
    :columns="columns"
    :row-selection="rowSelection"
    :scroll="{ x: 1360 }"
    :pagination="{
      pageSizeOptions: ['10', '20', '50', '100', '200', '400']
    }"
  >
    <!-- 批量操作：使用 tableAlertOptionRender 展示勾选后操作 -->
    <template #tableAlertOptionRender="slotProps">
      <a-space :size="16">
        <a-dropdown>
          <a @click.prevent> 批量操作&nbsp;<DownOutlined /> </a>
          <template #overlay>
            <a-menu>
              <a-menu-item
                key="confirm"
                @click="handleBatchAction('confirm', slotProps.selectedRows)"
                >批量确认
              </a-menu-item>
              <a-menu-item key="print" @click="handleBatchAction('print', slotProps.selectedRows)"
                >批量打印面单
              </a-menu-item>
              <a-menu-item key="sync" @click="handleBatchAction('sync', slotProps.selectedRows)"
                >同步选中
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a @click="slotProps.onCleanSelected">
          {{ slotProps.intl.getMessage('alert.clear', '清空') }}
        </a>
      </a-space>
    </template>

    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <export-confirm-button title="确认导出当前查询条件下的订单?" :on-export="handleExport" />
      <a-button @click="openHistory">
        <HistoryOutlined />
        打印历史
      </a-button>
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

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <!-- 订单基础信息 -->
      <template v-if="column.key === 'orderInfo'">
        <OrderBasicInfoCell
          :platform="record.platform"
          :platform-order-id="record.platformOrderId"
          :erp-order-id="record.id"
          :shop-name="record.erpShopName"
          :fulfillment-type="record.fulfillmentType"
          :order-time="record.platformCreatedAtMoscow"
          timezone="MSK"
        />
      </template>

      <!-- SKU 信息 -->
      <template v-else-if="column.key === 'skuInfo'">
        <SkuInfoCell :items="record.items" />
      </template>

      <!-- 金额 & 状态组合列 -->
      <template v-else-if="column.key === 'amountStatus'">
        <OrderAmountStatusCell
          :total-amount="record.totalAmount"
          :currency-code="record.currencyCode"
          :converted-amount="record.convertedAmount"
          :converted-currency-code="record.convertedCurrencyCode"
          :erp-status="record.erpStatus"
          :erp-status-mapping="mapErpStatus(record.erpStatus)"
          :platform-status="record.platformStatus"
          :platform-status-mapping="mapWbPlatformStatus(record.platformStatus)"
          :platform-substatus="record.platformSubstatus"
          :platform-substatus-mapping="mapWbSupplierStatus(record.platformSubstatus)"
          platform-status-label="平台履约状态"
          platform-substatus-label="商家处理状态"
          amount-order="converted-first"
        />
      </template>

      <!-- 仓库信息（WB 使用办公点名称+地址） -->
      <template v-else-if="column.key === 'warehouse'">
        <div
          class="order-cell warehouse"
          :title="
            record.destinationWarehouseName
              ? record.destinationWarehouseName + ' ' + (record.destinationWarehouseAddress || '')
              : record.warehouseId || '-'
          "
        >
          <div class="warehouse__name">
            {{ record.destinationWarehouseName || record.warehouseId || '-' }}
          </div>
          <div v-if="record.destinationWarehouseName" class="warehouse__addr minor">
            {{ record.destinationWarehouseAddress }}
          </div>
        </div>
      </template>

      <!-- 运营状态（锁定 + 面单） -->
      <template v-else-if="column.key === 'operationStatus'">
        <OrderOperationStatusCell :locked="record.locked" :has-label="record.hasLabel" />
      </template>

      <!-- 操作列 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="record.locked !== 1" @click="onOpenConfirmDialog([record])">确认</a>
          <a v-if="record.locked !== 1" @click="onOpenPrintDialog([record])">打印面单</a>
          <a-popconfirm title="是否确认进行同步操作?" @confirm="onSync([record])">
            <a>同步</a>
          </a-popconfirm>
          <a v-if="record.locked !== 1" @click="onLock(record.id)">锁定</a>
          <a v-else @click="onUnlock(record.id)">解锁</a>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <WbOrderPrintDialog
    v-model:open="printDialog.open"
    :rows="printDialog.rows"
    @printed="onPrinted"
  />

  <!-- 打印历史对话框 -->
  <LabelBatchHistoryDialog v-model:open="historyDialog.open" platform="Wildberries" />

  <!-- 确认前弹窗 -->
  <OrderConfirmModal
    v-model:open="confirmModal.open"
    :loading="confirmModal.loading"
    :eligible="confirmModal.eligible"
    :ineligible="confirmModal.ineligible"
    @confirm="onConfirmSubmit"
  />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import WbOrderPageSearch from './WbOrderPageSearch.vue'
import { OperationGroup } from '@/components/Operation'
import { mergePageParam } from '@/utils/page-utils'
import {
  pageErpOrder,
  confirmOrders,
  lockOrder,
  unlockOrder,
  syncOrders,
  syncAllOrders,
  exportOrders
} from '@/api/order/wb-order'
import { mapErpStatus, createStatusMapper } from '@/utils/order-status-mapper'
import { WB_PLATFORM_STATUS_MAP, WB_SUPPLIER_STATUS_MAP } from '@/api/order/wb-order/types'
import type { ErpOrderQO, WbOrderPageVO } from '@/api/order/wb-order/types'
import WbOrderPrintDialog from './WbOrderPrintDialog.vue'
import LabelBatchHistoryDialog from '@/views/order/label/LabelBatchHistoryDialog.vue'
import { ExportConfirmButton } from '@/components/Button'
import { message } from 'ant-design-vue'
import {
  OrderBasicInfoCell,
  OrderAmountStatusCell,
  OrderOperationStatusCell,
  OrderConfirmModal
} from '@/views/order/components'
import { remoteFileDownload } from '@/utils/file-utils'
import { HistoryOutlined, InteractionOutlined, DownOutlined } from '@ant-design/icons-vue'
import SkuInfoCell from '@/components/Sku/SkuInfoCell.vue'
import { useOrderConfirm } from '@/views/order/hooks/useOrderConfirm.ts'
import { useOrderLock } from '@/views/order/hooks/useOrderLock.ts'
import { useOrderSync } from '@/views/order/hooks/useOrderSync.ts'
import { useOrderPrint } from '@/views/order/hooks/useOrderPrint.ts'

defineOptions({ name: 'WbOrderPage' })

// 状态映射
const mapWbPlatformStatus = createStatusMapper(WB_PLATFORM_STATUS_MAP)
const mapWbSupplierStatus = createStatusMapper(WB_SUPPLIER_STATUS_MAP)

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

// 查询参数
let searchParams: ErpOrderQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const defaultSorter = { platformCreatedAt: 'descend' } as any
  const pageParam = mergePageParam(params, { ...defaultSorter, ...sorter }, filter)
  return pageErpOrder({ ...pageParam, ...searchParams })
}

/* 查询订单主表 */
const searchTable = (params: ErpOrderQO) => {
  searchParams = params
  reloadTable(true)
}

const columns: ProColumns[] = [
  { title: '订单基础信息', key: 'orderInfo', width: 200, fixed: 'left' },
  { title: '商品信息', key: 'skuInfo', ellipsis: true, width: 360 },
  { title: '金额 & 状态', key: 'amountStatus', width: 220 },
  { title: '仓库信息', key: 'warehouse', ellipsis: true, width: 150 },
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

// 使用 composables
const {
  confirmModal,
  openConfirmDialog: onOpenConfirmDialog,
  submitConfirm: onConfirmSubmit
} = useOrderConfirm<WbOrderPageVO>({
  canConfirm: row => {
    const platformOk = (row?.platform || '').toLowerCase() === 'wildberries'
    const status = row?.erpStatus
    const statusOk = status === 'PENDING' || status === 'READY_TO_SHIP'
    return platformOk && statusOk && !!row?.platformOrderId && row?.locked !== 1
  },
  reasonOf: row => {
    if ((row?.platform || '').toLowerCase() !== 'wildberries') return '非 Wildberries 平台'
    if (!row?.platformOrderId) return '缺少平台订单号'
    if (row?.locked === 1) return '订单已锁定'
    const status = row?.erpStatus
    if (status !== 'PENDING' && status !== 'READY_TO_SHIP') return `状态不支持：${status || '-'}`
    return '未知原因'
  },
  confirmApi: confirmOrders,
  reloadTable: () => reloadTable()
})

const { onLock, onUnlock } = useOrderLock({
  lockApi: lockOrder,
  unlockApi: unlockOrder,
  reloadTable: () => reloadTable()
})

const { onSync, handleSyncAllOrders } = useOrderSync({
  platform: 'wildberries',
  syncApi: syncOrders,
  syncAllApi: syncAllOrders,
  reloadTable: () => reloadTable()
})

const {
  printDialog,
  historyDialog,
  openPrintDialog: onOpenPrintDialog,
  openHistory
} = useOrderPrint<WbOrderPageVO>()

const onPrinted = () => {
  reloadTable()
}

// 批量操作处理
const handleBatchAction = (action: 'confirm' | 'print' | 'sync', rows: WbOrderPageVO[]) => {
  const list = Array.isArray(rows) ? rows : []
  if (!list.length) return
  if (action === 'confirm') {
    onOpenConfirmDialog(list)
  } else if (action === 'print') {
    onOpenPrintDialog(list)
  } else if (action === 'sync') {
    onSync(list)
  }
}

// 导出订单
const handleExport = async () => {
  try {
    const hide = message.loading('正在导出订单...', 0)
    const response = await exportOrders(searchParams)
    hide()
    remoteFileDownload(response, `Wildberries订单导出_${Date.now()}.xlsx`)
    message.success('导出成功')
  } catch (error) {
    message.error('导出失败：' + (error as Error).message)
  }
}
</script>

<style scoped>
@import '../styles/order-table.css';
</style>
