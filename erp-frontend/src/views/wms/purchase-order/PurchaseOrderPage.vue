<template>
  <!-- 查询表单 -->
  <purchase-order-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="采购单管理"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 840 }"
    size="middle"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('wms:purchase-order:add')" @click="handleNew" />
      <a-button :loading="exportLoading" @click="handleExport">
        <download-outlined />
        导出
      </a-button>
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <!-- 采购单信息列：采购单号 + 供应商(编码+名称) + SKU数/总数量 -->
      <template v-if="column.key === 'orderInfo'">
        <div class="order-info-cell">
          <div class="order-no">
            <a @click="handleViewDetail(record)">{{ record.orderNo }}</a>
          </div>
          <div class="supplier-info">
            <span class="supplier-code">{{ record.supplierCode }}</span>
            <span class="separator">·</span>
            <span class="supplier-name">{{ record.supplierName }}</span>
          </div>
          <div class="sku-summary">
            <span class="sku-count">{{ record.skuCount }}</span> SKU /
            <span class="quantity-count">{{ record.totalQuantity }}</span> 件
          </div>
        </div>
      </template>

      <!-- 状态/进度列：业务状态Badge + 发货进度 + 入库进度 -->
      <template v-else-if="column.key === 'statusProgress'">
        <div class="status-progress-cell">
          <!-- 业务状态 Badge（始终显示） -->
          <purchase-order-status-badge :status="record.orderStatus" />

          <!-- 生产中：显示进度条 -->
          <template v-if="showProgressBar(record.orderStatus)">
            <div class="progress-row">
              <span class="progress-label">发货</span>
              <a-progress
                :percent="calcPercent(record.totalShippedQuantity, record.totalQuantity)"
                :show-info="false"
                size="small"
                :stroke-color="'#faad14'"
                class="progress-bar"
              />
              <span class="progress-percent"
                >{{ calcPercent(record.totalShippedQuantity, record.totalQuantity) }}%</span
              >
            </div>
            <div class="progress-row">
              <span class="progress-label">入库</span>
              <a-progress
                :percent="calcPercent(record.totalReceivedQuantity, record.totalQuantity)"
                :show-info="false"
                size="small"
                :stroke-color="'#fa8c16'"
                class="progress-bar"
              />
              <span class="progress-percent"
                >{{ calcPercent(record.totalReceivedQuantity, record.totalQuantity) }}%</span
              >
            </div>
          </template>

          <!-- 已完成：显示完成 Tags -->
          <template v-else-if="showCompletedTags(record.orderStatus)">
            <div class="completed-tags">
              <a-tag color="cyan">
                <template #icon><check-outlined /></template>
                全部发货
              </a-tag>
              <a-tag color="green">
                <template #icon><check-outlined /></template>
                全部入库
              </a-tag>
            </div>
          </template>
        </div>
      </template>

      <!-- 金额/付款列：合同总金额 + 首付/尾款状态 -->
      <template v-else-if="column.key === 'amountPayment'">
        <div class="amount-payment-cell">
          <div class="total-amount">
            {{ formatCurrencyAmount(record.totalAmount, record.currencyCode, false) }}
          </div>
          <div class="payment-status" :class="{ paid: record.prepayStatus === 1 }">
            <check-circle-filled v-if="record.prepayStatus === 1" class="status-icon paid" />
            <clock-circle-outlined v-else class="status-icon unpaid" />
            <span class="status-text">首付 {{ record.prepayStatus === 1 ? '已付' : '未付' }}</span>
          </div>
          <div class="payment-status" :class="{ paid: record.balanceStatus === 1 }">
            <check-circle-filled v-if="record.balanceStatus === 1" class="status-icon paid" />
            <clock-circle-outlined v-else class="status-icon unpaid" />
            <span class="status-text">尾款 {{ record.balanceStatus === 1 ? '已付' : '未付' }}</span>
          </div>
        </div>
      </template>

      <!-- 日期信息列：下单日期 + 预计交货 + 实际交货 -->
      <template v-else-if="column.key === 'dateInfo'">
        <div class="date-info-cell">
          <div class="date-row">
            <span class="date-label">下单:</span>
            <span>{{ formatDate(record.orderDate) }}</span>
          </div>
          <div class="date-row">
            <span class="date-label">预计:</span>
            <span>{{ formatDate(record.expectedDeliveryDate) || '-' }}</span>
          </div>
          <div class="date-row" :class="{ 'actual-date': record.actualDeliveryDate }">
            <span class="date-label">实际:</span>
            <span>{{ formatDate(record.actualDeliveryDate) || '-' }}</span>
            <check-outlined v-if="record.actualDeliveryDate" class="check-icon" />
          </div>
        </div>
      </template>

      <!-- 操作列 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a @click="handleViewDetail(record)">查看</a>
          <a
            v-if="hasPermission('wms:purchase-order:edit') && getActions(record).edit"
            @click="handleEdit(record)"
          >
            编辑
          </a>
          <a-dropdown v-if="hasMoreActions(record)">
            <a>更多 <down-outlined /></a>
            <template #overlay>
              <a-menu>
                <a-menu-item v-if="getActions(record).confirm" key="confirm">
                  <a @click="handleConfirm(record)">确认采购单</a>
                </a-menu-item>
                <a-menu-item v-if="getActions(record).startProduction" key="startProduction">
                  <a @click="handleStartProduction(record)">开始生产</a>
                </a-menu-item>
                <a-menu-item v-if="getActions(record).cancel" key="cancel">
                  <confirm-text-button
                    title="确认要取消此采购单吗？"
                    text="取消采购单"
                    @confirm="handleCancel(record)"
                  />
                </a-menu-item>
                <a-menu-item v-if="getActions(record).delete" key="delete">
                  <delete-text-button @confirm="handleDelete(record)" />
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 采购单详情抽屉 -->
  <purchase-order-detail-drawer ref="detailDrawerRef" @update-success="reloadTable" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import {
  DownOutlined,
  CheckOutlined,
  DownloadOutlined,
  CheckCircleFilled,
  ClockCircleOutlined
} from '@ant-design/icons-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import PurchaseOrderPageSearch from './PurchaseOrderPageSearch.vue'
import PurchaseOrderDetailDrawer from './PurchaseOrderDetailDrawer.vue'
import { NewButton, ConfirmTextButton, DeleteTextButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import PurchaseOrderStatusBadge from './components/PurchaseOrderStatusBadge.vue'
import { useAuthorize } from '@/hooks/permission'
import { emitter } from '@/hooks/mitt'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'
import { mergePageParam } from '@/utils/page-utils'
import { doRequest } from '@/utils/axios/request'
import { formatAmount as formatAmountUtil } from '@/utils/amount-utils'
import { formatAmount as formatCurrencyAmount } from '@/utils/currency-utils'
import { remoteFileDownload } from '@/utils/file-utils'
import {
  pagePurchaseOrder,
  confirmPurchaseOrder,
  startProduction,
  cancelPurchaseOrder,
  deletePurchaseOrder,
  exportPurchaseOrder
} from '@/api/wms/purchase-order'
import { getListPageActions } from '@/api/wms/purchase-order/status-utils'
import type { PurchaseOrderPageVO, PurchaseOrderQO } from '@/api/wms/purchase-order/types'

defineOptions({ name: 'PurchaseOrderPage' })

const router = useRouter()

// 鉴权方法
const { hasPermission } = useAuthorize()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const detailDrawerRef = ref<InstanceType<typeof PurchaseOrderDetailDrawer>>()

// ==================== 辅助函数 ====================

/**
 * 获取操作按钮配置
 */
const getActions = (record: PurchaseOrderPageVO) => {
  return getListPageActions(record.orderStatus)
}

/**
 * 判断是否有更多操作
 */
const hasMoreActions = (record: PurchaseOrderPageVO): boolean => {
  const actions = getActions(record)
  return actions.confirm || actions.startProduction || actions.cancel || actions.delete
}

/**
 * 格式化金额
 */
const formatAmount = (amount: number | undefined | null): string => {
  return formatAmountUtil(amount)
}

/**
 * 格式化日期
 */
const formatDate = (date: string | undefined | null): string => {
  if (!date) return ''
  return date.split(' ')[0] || date
}

/**
 * 计算百分比
 */
const calcPercent = (
  current: number | undefined | null,
  total: number | undefined | null
): number => {
  if (!total || total === 0) return 0
  const percent = Math.round(((current || 0) / total) * 100)
  return Math.min(percent, 100)
}

/**
 * 获取进度信息（避免模板重复计算）
 */
const getProgressInfo = (record: PurchaseOrderPageVO) => {
  return {
    shipping: calcPercent(record.totalShippedQuantity, record.totalQuantity),
    receiving: calcPercent(record.totalReceivedQuantity, record.totalQuantity)
  }
}

/**
 * 判断是否需要显示进度条（生产中状态）
 */
const showProgressBar = (orderStatus: string): boolean => {
  return orderStatus === 'IN_PRODUCTION'
}

/**
 * 判断是否需要显示完成标签（已完成状态）
 */
const showCompletedTags = (orderStatus: string): boolean => {
  return orderStatus === 'COMPLETED'
}

// ==================== 表格配置 ====================

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

useTableActivateReload(() => reloadTable(false))

// 查询参数
let searchParams: PurchaseOrderQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pagePurchaseOrder({ ...pageParam, ...searchParams })
}

/* 查询采购单 */
const searchTable = (params: PurchaseOrderQO) => {
  searchParams = params
  reloadTable(true)
}

// ==================== 列定义 ====================

const columns: ProColumns[] = [
  {
    title: '采购单信息',
    key: 'orderInfo',
    width: 200,
    fixed: 'left'
  },
  {
    title: '状态/进度',
    key: 'statusProgress',
    width: 200
  },
  {
    title: '金额/付款',
    key: 'amountPayment',
    width: 160
  },
  {
    title: '日期信息',
    key: 'dateInfo',
    width: 140
  },
  {
    key: 'operate',
    title: '操作',
    align: 'center',
    width: 140,
    fixed: 'right'
  }
]

// ==================== 操作方法 ====================

/* 新建采购单 - 导航到表单页面 */
const handleNew = () => {
  router.push('/wms/purchase-order/form/create')
}

/**
 * 导出按钮点击 - 导出当前筛选条件的数据
 */
const exportLoading = ref(false)
const handleExport = async () => {
  exportLoading.value = true
  try {
    const response = await exportPurchaseOrder(searchParams)
    remoteFileDownload(response)
    message.success('导出成功')
  } catch (error) {
    message.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

/* 编辑采购单 - 导航到表单页面 */
const handleEdit = (record: PurchaseOrderPageVO) => {
  router.push(`purchase-order/form/edit/${record.id}`)
}

/* 查看详情 */
const handleViewDetail = (record: PurchaseOrderPageVO) => {
  detailDrawerRef.value?.open(record.id)
}

/* 确认采购单 */
const handleConfirm = (record: PurchaseOrderPageVO) => {
  Modal.confirm({
    title: '确认采购单',
    content: `确定要确认采购单 ${record.orderNo} 吗？确认后将无法修改基本信息。`,
    okText: '确定',
    cancelText: '取消',
    onOk: () => {
      doRequest(confirmPurchaseOrder(record.id), {
        successMessage: '确认成功',
        onSuccess: () => reloadTable()
      })
    }
  })
}

/* 开始生产 */
const handleStartProduction = (record: PurchaseOrderPageVO) => {
  Modal.confirm({
    title: '开始生产',
    content: `确定要将采购单 ${record.orderNo} 标记为生产中吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: () => {
      doRequest(startProduction(record.id), {
        successMessage: '操作成功',
        onSuccess: () => reloadTable()
      })
    }
  })
}

/* 取消采购单 */
const handleCancel = (record: PurchaseOrderPageVO) => {
  doRequest(cancelPurchaseOrder(record.id), {
    successMessage: '取消成功',
    onSuccess: () => reloadTable()
  })
}

/* 删除采购单 */
const handleDelete = (record: PurchaseOrderPageVO) => {
  doRequest(deletePurchaseOrder(record.id), {
    successMessage: '删除成功',
    onSuccess: () => reloadTable(true)
  })
}

// ==================== 生命周期 ====================

// 监听刷新列表事件
onMounted(() => {
  emitter.on('refresh-purchase-order-list', () => reloadTable())
})

onUnmounted(() => {
  emitter.off('refresh-purchase-order-list')
})
</script>

<style scoped>
:deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
  padding: 12px 8px;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 600;
}

/* ==================== 采购单信息列 ==================== */
.order-info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.order-no {
  font-weight: 600;
  font-size: 14px;
  color: #1890ff;
  cursor: pointer;
}

.supplier-info {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.supplier-code {
  font-family: 'SFMono-Regular', Consolas, 'Courier New', monospace;
  color: #595959;
}

.separator {
  color: #d9d9d9;
}

.supplier-name {
  color: #8c8c8c;
}

.sku-summary {
  font-size: 12px;
  color: #8c8c8c;
}

.sku-summary .sku-count,
.sku-summary .quantity-count {
  color: #595959;
  font-weight: 500;
  font-family: 'SFMono-Regular', Consolas, 'Courier New', monospace;
}

/* ==================== 状态/进度列 ==================== */
.status-progress-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
}

.progress-row .progress-label {
  font-size: 12px;
  color: #8c8c8c;
  min-width: 24px;
  flex-shrink: 0;
}

.progress-bar {
  flex: 1;
  min-width: 60px;
  max-width: 80px;
}

.progress-bar :deep(.ant-progress-inner) {
  background-color: #f0f0f0;
}

.progress-percent {
  font-size: 12px;
  color: #595959;
  font-family: 'SFMono-Regular', Consolas, 'Courier New', monospace;
  min-width: 32px;
  text-align: right;
}

.completed-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}

.completed-tags :deep(.ant-tag) {
  margin-right: 0;
}

/* ==================== 金额/付款列 ==================== */
.amount-payment-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.total-amount {
  font-weight: 600;
  font-size: 14px;
  color: #f5222d;
}

.payment-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.payment-status .status-icon {
  font-size: 14px;
}

.payment-status .status-icon.paid {
  color: #52c41a;
}

.payment-status .status-icon.unpaid {
  color: #bfbfbf;
}

.payment-status .status-text {
  color: #8c8c8c;
}

.payment-status.paid .status-text {
  color: #52c41a;
}

/* ==================== 日期信息列 ==================== */
.date-info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.date-row {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #595959;
}

.date-row .date-label {
  color: #8c8c8c;
  font-size: 12px;
  min-width: 32px;
}

/* 实际交货日期绿色标识 */
.date-row.actual-date {
  color: #52c41a;
  font-weight: 500;
}

.date-row.actual-date .date-label {
  color: #52c41a;
}

.check-icon {
  color: #52c41a;
  font-size: 12px;
  margin-left: 2px;
}
</style>
