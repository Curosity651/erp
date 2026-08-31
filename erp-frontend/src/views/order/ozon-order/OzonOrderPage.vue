<template>
  <!-- 查询表单 -->
  <ozon-order-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="Ozon 订单"
    row-key="id"
    class-name="order-pro-table"
    :request="tableRequest"
    :columns="columns"
    :row-selection="rowSelection"
    :scroll="{ x: 1620 }"
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
              <!-- 只有全部是 FBS 订单时才显示确认和打印 -->
              <a-menu-item
                v-if="isAllFbsOrders(slotProps.selectedRows)"
                key="confirm"
                @click="handleBatchAction('confirm', slotProps.selectedRows)"
                >批量确认
              </a-menu-item>
              <a-menu-item
                v-if="isAllFbsOrders(slotProps.selectedRows)"
                key="print"
                @click="handleBatchAction('print', slotProps.selectedRows)"
                >批量打印面单
              </a-menu-item>
              <a-menu-item
                v-if="isAllFbsOrders(slotProps.selectedRows)"
                key="pick"
                @click="handleBatchAction('pick', slotProps.selectedRows)"
                >打印拣货单
              </a-menu-item>
              <a-menu-item
                v-if="isAllFbsOrders(slotProps.selectedRows)"
                key="act"
                @click="handleBatchAction('act', slotProps.selectedRows)"
                >准备发运
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
      <a-button @click="ruleDialogOpen = true">交接规则</a-button>
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

      <!-- 商品总价列 -->
      <template v-else-if="column.key === 'productAmount'">
        <div
          class="order-cell product-amount"
          :title="`商品总价: ${formatAmount(
            record.productTotalAmount,
            record.productCurrencyCode
          )} / ${formatAmount(record.productAmountCny, 'CNY')}`"
        >
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
          :erp-status="record.erpStatus"
          :erp-status-mapping="mapErpStatus(record.erpStatus)"
          :platform-status="record.platformStatus"
          :platform-status-mapping="mapOzonStatus(record.platformStatus)"
          :platform-substatus="record.platformSubstatus"
          :platform-substatus-mapping="mapOzonSubstatus(record.platformSubstatus)"
          platform-status-label="平台状态"
          platform-substatus-label="子状态"
          amount-order="total-first"
        />
      </template>

      <!-- 仓库信息 -->
      <template v-else-if="column.key === 'warehouse'">
        <div
          class="order-cell warehouse"
          :title="
            record.warehouseName
              ? `${record.warehouseName} (ID: ${record.warehouseId || '-'})`
              : '-'
          "
        >
          <div v-if="record.warehouseName" class="warehouse__name">
            {{ record.warehouseName }}
          </div>
          <div v-else class="warehouse__name text-gray-400">-</div>
          <a-tag
            v-if="warehouseTypeOf(record.warehouseName)"
            :color="WAREHOUSE_TYPE_META[warehouseTypeOf(record.warehouseName)!].color"
            class="warehouse__tag"
          >
            {{ WAREHOUSE_TYPE_META[warehouseTypeOf(record.warehouseName)!].label }}
          </a-tag>
        </div>
      </template>

      <!-- 物流信息（Ozon 特有） -->
      <template v-else-if="column.key === 'logistics'">
        <div class="order-cell logistics">
          <!-- FBO 订单 -->
          <div v-if="record.fulfillmentType === 'FBO'" class="logistics__fbo">
            <a-tag color="blue">Ozon 仓配</a-tag>
          </div>
          <!-- FBS 订单 -->
          <div v-else-if="record.fulfillmentType === 'FBS'" class="logistics__fbs">
            <div v-if="record.tplProviderName" class="logistics__provider">
              {{ record.tplProviderName }}
            </div>
            <div v-else class="text-gray-400">-</div>
            <div v-if="record.deliveryMethodName" class="logistics__method minor">
              {{ record.deliveryMethodName }}
            </div>
            <div v-if="record.trackingNumber" class="logistics__tracking minor">
              追踪: {{ record.trackingNumber }}
            </div>
          </div>
          <div v-else class="text-gray-400">-</div>
        </div>
      </template>

      <!-- 运营状态（锁定 + 面单） -->
      <template v-else-if="column.key === 'operationStatus'">
        <OrderOperationStatusCell :locked="record.locked" :has-label="record.hasLabel" />
      </template>

      <!-- 操作列 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <!-- 只有 FBS 订单且未锁定才显示确认和打印 -->
          <a
            v-if="record.fulfillmentType === 'FBS' && record.locked !== 1"
            @click="onOpenConfirmDialog([record])"
            >确认</a
          >
          <a
            v-if="record.fulfillmentType === 'FBS' && record.locked !== 1"
            @click="onOpenPrintDialog([record])"
            >打印面单</a
          >
          <a-popconfirm title="是否确认进行同步操作?" @confirm="onSync([record])">
            <a>同步</a>
          </a-popconfirm>
          <a v-if="record.locked !== 1" @click="onLock(record.id)">锁定</a>
          <a v-else @click="onUnlock(record.id)">解锁</a>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <OzonOrderPrintDialog
    v-model:open="printDialog.open"
    :rows="printDialog.rows"
    @printed="onPrinted"
  />

  <!-- 拣货单对话框（按店铺一份） -->
  <OzonOrderPickListDialog
    v-model:open="pickDialog.open"
    :rows="pickDialog.rows"
    @generated="reloadTable"
  />

  <!-- 准备发运（生成运单 act）对话框 -->
  <OzonOrderActDialog
    v-model:open="actDialog.open"
    :rows="actDialog.rows"
    @generated="reloadTable"
  />

  <OzonDeliveryRuleDialog v-model:open="ruleDialogOpen" />

  <!-- 打印历史对话框 -->
  <LabelBatchHistoryDialog v-model:open="historyDialog.open" platform="Ozon" />

  <!-- 确认前弹窗 -->
  <OrderConfirmModal
    v-model:open="confirmModal.open"
    v-model:logistics-product-id="confirmModal.logisticsProductId"
    v-model:wms-warehouse-id="confirmModal.wmsWarehouseId"
    :loading="confirmModal.loading"
    :eligible="confirmModal.eligible"
    :ineligible="confirmModal.ineligible"
    @confirm="onConfirmSubmit"
  />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import OzonOrderPageSearch from './OzonOrderPageSearch.vue'
import { OperationGroup } from '@/components/Operation'
import { mergePageParam } from '@/utils/page-utils'
import {
  pageOzonOrder,
  confirmOzonOrders,
  lockOzonOrder,
  unlockOzonOrder,
  syncOzonOrders,
  syncAllOzonOrders,
  exportOzonOrders
} from '@/api/order/ozon-order'
import { OZON_STATUS_MAP, OZON_SUBSTATUS_MAP } from '@/api/order/ozon-order/types'
import type { OzonOrderQO, OzonOrderPageVO } from '@/api/order/ozon-order/types'
import OzonOrderPrintDialog from './OzonOrderPrintDialog.vue'
import OzonOrderPickListDialog from './OzonOrderPickListDialog.vue'
import OzonOrderActDialog from './OzonOrderActDialog.vue'
import OzonDeliveryRuleDialog from './OzonDeliveryRuleDialog.vue'
import { warehouseTypeOf, WAREHOUSE_TYPE_META } from './warehouse-type'
import LabelBatchHistoryDialog from '@/views/order/label/LabelBatchHistoryDialog.vue'
import { ExportConfirmButton } from '@/components/Button'
import { message, Modal } from 'ant-design-vue'
import { formatAmount } from '@/utils/currency-utils'
import {
  OrderBasicInfoCell,
  OrderAmountStatusCell,
  OrderOperationStatusCell,
  OrderConfirmModal
} from '@/views/order/components'
import { remoteFileDownload } from '@/utils/file-utils'
import { HistoryOutlined, InteractionOutlined, DownOutlined } from '@ant-design/icons-vue'
import SkuInfoCell from '@/components/Sku/SkuInfoCell.vue'
import { mapErpStatus, createStatusMapper } from '@/utils/order-status-mapper'
import { useOrderConfirm } from '@/views/order/hooks/useOrderConfirm.ts'
import { useOrderLock } from '@/views/order/hooks/useOrderLock.ts'
import { useOrderSync } from '@/views/order/hooks/useOrderSync.ts'
import { useOrderPrint } from '@/views/order/hooks/useOrderPrint.ts'

defineOptions({ name: 'OzonOrderPage' })

// 状态映射
const mapOzonStatus = createStatusMapper(OZON_STATUS_MAP)
const mapOzonSubstatus = createStatusMapper(OZON_SUBSTATUS_MAP)

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const ruleDialogOpen = ref(false)

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

// 查询参数
let searchParams: OzonOrderQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const defaultSorter = { platformCreatedAt: 'descend' } as any
  const pageParam = mergePageParam(params, { ...defaultSorter, ...sorter }, filter)
  return pageOzonOrder({ ...pageParam, ...searchParams })
}

/* 查询订单主表 */
const searchTable = (params: OzonOrderQO) => {
  searchParams = params
  reloadTable(true)
}

const columns: ProColumns[] = [
  { title: '订单基础信息', key: 'orderInfo', width: 200, fixed: 'left' },
  { title: '商品信息', key: 'skuInfo', ellipsis: true, width: 380 },
  { title: '商品总价', key: 'productAmount', width: 140 },
  { title: '订单金额 & 状态', key: 'amountStatus', width: 220 },
  { title: '仓库信息', key: 'warehouse', ellipsis: true, width: 100 },
  { title: '配送方式', key: 'logistics', ellipsis: true, width: 180 },
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

// Ozon 特有：判断是否全部为 FBS 订单
const isAllFbsOrders = (orders: OzonOrderPageVO[]) => {
  return orders && orders.length > 0 && orders.every(o => o?.fulfillmentType === 'FBS')
}

// Ozon 确认响应处理（逐条检查，展示失败明细）
const handleOzonConfirmSuccess = (res: any) => {
  const data = res?.data
  if (data?.items) {
    const successCount = data.items.filter((i: any) => i.success).length
    const failCount = data.items.filter((i: any) => !i.success).length
    if (failCount > 0) {
      const failMessages = data.items
        .filter((i: any) => !i.success)
        .map((i: any) => `订单${i.orderId}: ${i.message}`)
        .join('\n')
      Modal.warning({
        title: `确认完成：成功 ${successCount}，失败 ${failCount}`,
        content: failMessages,
        width: 520
      })
    } else {
      message.success(`确认发货成功：${successCount} 单`)
    }
  }
}

// 使用 composables
const {
  confirmModal,
  openConfirmDialog: onOpenConfirmDialog,
  submitConfirm: onConfirmSubmit
} = useOrderConfirm<OzonOrderPageVO>({
  canConfirm: row => {
    const platformOk = (row?.platform || '').toLowerCase() === 'ozon'
    const isFbs = row?.fulfillmentType === 'FBS'
    const statusOk = row?.erpStatus === 'READY_TO_SHIP'
    return platformOk && isFbs && statusOk && !!row?.platformOrderId && row?.locked !== 1
  },
  reasonOf: row => {
    if ((row?.platform || '').toLowerCase() !== 'ozon') return '非 Ozon 平台'
    if (row?.fulfillmentType !== 'FBS') return 'FBO 订单不支持确认操作'
    if (!row?.platformOrderId) return '缺少平台订单号'
    if (row?.locked === 1) return '订单已锁定'
    if (row?.erpStatus !== 'READY_TO_SHIP') return `状态不支持：${row?.erpStatus || '-'}`
    return '未知原因'
  },
  confirmApi: confirmOzonOrders,
  reloadTable: () => reloadTable(),
  onConfirmSuccess: handleOzonConfirmSuccess
})

const { onLock, onUnlock } = useOrderLock({
  lockApi: lockOzonOrder,
  unlockApi: unlockOzonOrder,
  reloadTable: () => reloadTable()
})

const { onSync, handleSyncAllOrders } = useOrderSync({
  platform: 'ozon',
  syncApi: syncOzonOrders,
  syncAllApi: syncAllOzonOrders,
  reloadTable: () => reloadTable()
})

const {
  printDialog,
  historyDialog,
  pickDialog,
  actDialog,
  openPrintDialog: onOpenPrintDialog,
  openHistory,
  openPickDialog: onOpenPickDialog,
  openActDialog: onOpenActDialog
} = useOrderPrint<OzonOrderPageVO>()

const onPrinted = () => {
  reloadTable()
}

// 批量操作处理（Ozon 特有：FBS/FBO 验证）
const handleBatchAction = (
  action: 'confirm' | 'print' | 'pick' | 'act' | 'sync',
  rows: OzonOrderPageVO[]
) => {
  const list = Array.isArray(rows) ? rows : []
  if (!list.length) return

  // 确认/打印面单/拣货单/准备发运 均仅支持 FBS 订单
  if (action !== 'sync' && !isAllFbsOrders(list)) {
    message.warning('该操作仅支持 FBS 订单')
    return
  }

  if (action === 'confirm') {
    onOpenConfirmDialog(list)
  } else if (action === 'print') {
    onOpenPrintDialog(list)
  } else if (action === 'pick') {
    onOpenPickDialog(list)
  } else if (action === 'act') {
    onOpenActDialog(list)
  } else if (action === 'sync') {
    onSync(list)
  }
}

// 导出订单
const handleExport = async () => {
  try {
    const hide = message.loading('正在导出订单...', 0)
    const response = await exportOzonOrders(searchParams)
    hide()
    remoteFileDownload(response, `Ozon订单导出_${Date.now()}.xlsx`)
    message.success('导出成功')
  } catch (error) {
    message.error('导出失败：' + (error as Error).message)
  }
}
</script>

<style scoped>
@import '../styles/order-table.css';

/* ========== 仓库列：大仓/小仓标签 ========== */
.warehouse__tag {
  margin-top: 2px;
  margin-inline-end: 0;
}

/* ========== 物流列（Ozon 特有） ========== */
.logistics {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.logistics__fbs {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.logistics__provider {
  font-weight: 600;
  font-size: 14px;
  color: #262626;
}

.logistics__method {
  font-size: 13px;
  color: #8c8c8c;
}

.logistics__tracking {
  font-size: 13px;
  color: #8c8c8c;
}
</style>
