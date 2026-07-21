<template>
  <!-- 查询表单 -->
  <shipping-order-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="物流单管理"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 780 }"
    size="middle"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('wms:shipping-order:add')" @click="handleNew" />
      <a-button @click="handleExport">
        <download-outlined />
        导出
      </a-button>
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <!-- 物流单列：单号 + 创建时间 -->
      <template v-if="column.key === 'shippingInfo'">
        <div class="shipping-info-cell">
          <div class="shipping-no-row">
            <a class="shipping-no" @click="handleViewDetail(record)">{{ record.shippingNo }}</a>
            <a-tooltip title="复制单号">
              <copy-outlined
                v-if="!copied"
                class="copy-icon"
                @click.stop="copy(record.shippingNo)"
              />
              <span v-else class="copy-success">已复制</span>
            </a-tooltip>
          </div>
          <div class="create-time">{{ record.createTime }}</div>
        </div>
      </template>

      <!-- 状态 & 货物列：状态徽章 + 发货/入库统计 + 进度条 -->
      <template v-else-if="column.key === 'statusCargo'">
        <div class="status-cargo-cell">
          <!-- 状态徽章 -->
          <shipping-order-status-badge :status="record.shippingStatus" />

          <!-- 发货/入库 统计行 -->
          <div class="cargo-stats">
            <div class="stat-item">
              <span class="stat-label">发</span>
              <span class="stat-value">{{ record.totalShippedQuantity || 0 }}</span>
            </div>
            <span class="stat-arrow">→</span>
            <div class="stat-item">
              <span class="stat-label">入</span>
              <span class="stat-value">{{ record.totalReceivedQuantity || 0 }}</span>
            </div>
          </div>

          <!-- 进度条（仅在途状态显示） -->
          <div v-if="showInboundProgress(record.shippingStatus)" class="progress-row">
            <a-progress
              :percent="calcPercent(record.totalReceivedQuantity, record.totalShippedQuantity)"
              size="small"
              :show-info="false"
              :stroke-color="'#fa8c16'"
              class="progress-bar"
            />
            <span class="progress-percent">
              {{ calcPercent(record.totalReceivedQuantity, record.totalShippedQuantity) }}%
            </span>
          </div>

          <!-- 已完成：显示完成 Tag -->
          <a-tag
            v-else-if="record.shippingStatus === 'COMPLETED'"
            color="green"
            class="complete-tag"
          >
            <template #icon><check-outlined /></template>
            全部入库
          </a-tag>
        </div>
      </template>

      <!-- 物流信息列：渠道 + 仓库 -->
      <template v-else-if="column.key === 'logisticsInfo'">
        <div class="logistics-info-cell">
          <div class="provider-name">{{ record.providerName }}</div>
          <div class="route-info">
            {{ ShippingMethodMap[record.shippingMethod] }}
            <span class="separator">·</span>
            {{ ShippingRouteMap[record.shippingRoute] }}
          </div>
          <div class="warehouse-row">
            <span>{{ record.targetRegion?.regionName || '-' }}</span>
          </div>
        </div>
      </template>

      <!-- 费用 & 时效列 -->
      <template v-else-if="column.key === 'amountDate'">
        <div class="amount-date-cell">
          <!-- 金额 + 付款状态 -->
          <div class="amount-row">
            <span class="amount">
              <span class="currency">$</span>{{ formatAmount(record.totalAmount) }}
            </span>
            <a-tag :color="record.paymentStatus === 1 ? 'success' : 'default'" class="payment-tag">
              {{ record.paymentStatus === 1 ? '已付' : '待付' }}
            </a-tag>
          </div>

          <!-- 时效：发货日期 → 预计到达 -->
          <div class="date-row">
            <span class="date-value">{{ record.shippingDate }}</span>
            <template v-if="record.estimatedArrivalDate">
              <span class="date-arrow">→</span>
              <span class="date-value">{{ record.estimatedArrivalDate }}</span>
            </template>
          </div>
          <div v-if="record.estimatedDays" class="days-row">
            <span class="days-badge">{{ record.estimatedDays }}天</span>
          </div>
        </div>
      </template>

      <!-- 操作列 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <!-- 编辑/查看 -->
          <template v-if="canEdit(record)">
            <a v-if="hasPermission('wms:shipping-order:edit')" @click="handleEdit(record)">
              编辑
            </a>
            <a v-else @click="handleViewDetail(record)">查看</a>
          </template>
          <template v-else>
            <a @click="handleViewDetail(record)">查看</a>
          </template>

          <!-- 核心业务动作 -->
          <!-- 待发货 -> 确认发货 -->
          <a v-if="canConfirmShip(record)" @click="handleConfirmShip(record)"> 发货 </a>

          <!-- 已发货/部分到货 -> 创建入库单 -->
          <a v-if="canCreateInbound(record)" @click="handleCreateInbound(record)"> 入库 </a>

          <span v-if="canShowDisabledInbound(record)" class="disabled-action">入库</span>

          <a-dropdown v-if="hasMoreActions(record)">
            <a class="more-link">更多 <down-outlined /></a>
            <template #overlay>
              <a-menu>
                <a-menu-item v-if="canConfirmPayment(record)" key="confirmPayment">
                  <a @click="handleConfirmPayment(record)">确认付款</a>
                </a-menu-item>
                <a-menu-item v-if="canDelete(record)" key="delete">
                  <delete-text-button @confirm="handleDelete(record)" />
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 物流单详情抽屉 -->
  <shipping-order-detail-drawer ref="detailDrawerRef" @update-success="reloadTable" />

  <!-- 确认付款弹窗 -->
  <payment-confirm-modal ref="paymentModalRef" @success="reloadTable" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { DownOutlined, DownloadOutlined, CopyOutlined, CheckOutlined } from '@ant-design/icons-vue'
import { useClipboard } from '@vueuse/core'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import ShippingOrderPageSearch from './ShippingOrderPageSearch.vue'
import ShippingOrderDetailDrawer from './ShippingOrderDetailDrawer.vue'
import PaymentConfirmModal from './components/PaymentConfirmModal.vue'
import { NewButton, DeleteTextButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import ShippingOrderStatusBadge from './components/ShippingOrderStatusBadge.vue'
import { useAuthorize } from '@/hooks/permission'
import { emitter } from '@/hooks/mitt'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'
import { mergePageParam } from '@/utils/page-utils'
import { doRequest } from '@/utils/axios/request'
import { formatAmount as formatAmountUtil } from '@/utils/amount-utils'
import {
  pageShippingOrder,
  confirmShip,
  deleteShippingOrder,
  exportShippingOrder
} from '@/api/wms/shipping-order'
import {
  ShippingStatusMap,
  ShippingMethodMap,
  ShippingRouteMap,
  ShippingStatus
} from '@/api/wms/shipping-order/types'
import type { ShippingOrderPageVO, ShippingOrderQO } from '@/api/wms/shipping-order/types'

defineOptions({ name: 'ShippingOrderPage' })

const router = useRouter()
const { copy, copied } = useClipboard({ legacy: true })

// 鉴权方法
const { hasPermission } = useAuthorize()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const detailDrawerRef = ref<InstanceType<typeof ShippingOrderDetailDrawer>>()
const paymentModalRef = ref<InstanceType<typeof PaymentConfirmModal>>()

// ==================== 辅助函数 ====================

/**
 * 格式化金额
 */
const formatAmount = (amount: number | undefined | null): string => {
  return formatAmountUtil(amount)
}

/**
 * 计算百分比
 */
const calcPercent = (
  received: number | undefined | null,
  total: number | undefined | null
): number => {
  if (!total || total === 0) return 0
  const percent = Math.round(((received || 0) / total) * 100)
  return Math.min(percent, 100)
}

/**
 * 是否显示入库进度条
 */
const showInboundProgress = (status: string): boolean => {
  return [
    ShippingStatus.SHIPPED,
    ShippingStatus.PARTIAL_ARRIVED,
    ShippingStatus.ALL_ARRIVED
  ].includes(status as ShippingStatus)
}

/**
 * 是否可编辑
 */
const canEdit = (record: ShippingOrderPageVO): boolean => {
  return record.shippingStatus !== ShippingStatus.COMPLETED
}

/**
 * 是否可确认发货
 */
const canConfirmShip = (record: ShippingOrderPageVO): boolean => {
  return (
    record.shippingStatus === ShippingStatus.PENDING && hasPermission('wms:shipping-order:edit')
  )
}

/**
 * 是否可确认付款
 */
const canConfirmPayment = (record: ShippingOrderPageVO): boolean => {
  return record.paymentStatus === 0 && hasPermission('wms:shipping-order:edit')
}

/**
 * 是否可创建入库单
 */
const canCreateInbound = (record: ShippingOrderPageVO): boolean => {
  return (
    [ShippingStatus.SHIPPED, ShippingStatus.PARTIAL_ARRIVED].includes(record.shippingStatus) &&
    (record.pendingInboundQuantity ?? 0) > 0 &&
    hasPermission('wms:purchase-inbound:add')
  )
}

const canShowDisabledInbound = (record: ShippingOrderPageVO): boolean => {
  return (
    [ShippingStatus.SHIPPED, ShippingStatus.PARTIAL_ARRIVED].includes(record.shippingStatus) &&
    (record.pendingInboundQuantity ?? 0) <= 0 &&
    hasPermission('wms:purchase-inbound:add')
  )
}

/**
 * 是否可删除
 */
const canDelete = (record: ShippingOrderPageVO): boolean => {
  return (
    record.shippingStatus === ShippingStatus.PENDING &&
    record.paymentStatus !== 1 &&
    hasPermission('wms:shipping-order:del')
  )
}

/**
 * 判断是否有更多操作
 */
const hasMoreActions = (record: ShippingOrderPageVO): boolean => {
  return canConfirmPayment(record) || canDelete(record)
}

// ==================== 表格配置 ====================

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

useTableActivateReload(() => reloadTable(false))

// 监听刷新事件
onMounted(() => {
  emitter.on('refresh-shipping-order-list', () => reloadTable(true))
})

onUnmounted(() => {
  emitter.off('refresh-shipping-order-list')
})

// 查询参数
let searchParams: ShippingOrderQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageShippingOrder({ ...pageParam, ...searchParams })
}

/* 查询物流单 */
const searchTable = (params: ShippingOrderQO) => {
  searchParams = params
  reloadTable(true)
}

// ==================== 列定义 ====================
// 采用列合并策略：8列 → 5列，消除水平滚动，信息分组更合理

const columns: ProColumns[] = [
  {
    title: '物流单',
    key: 'shippingInfo',
    width: 160,
    fixed: 'left'
  },
  {
    title: '状态 & 货物',
    key: 'statusCargo',
    width: 180
  },
  {
    title: '物流信息',
    key: 'logisticsInfo',
    width: 160
  },
  {
    title: '费用 & 时效',
    key: 'amountDate',
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

/* 新建物流单 - 导航到表单页面 */
const handleNew = () => {
  router.push('/wms/shipping-order/form/create')
}

/* 编辑物流单 - 导航到表单页面 */
const handleEdit = (record: ShippingOrderPageVO) => {
  router.push(`/wms/shipping-order/form/edit/${record.id}`)
}

/* 查看详情 */
const handleViewDetail = (record: ShippingOrderPageVO) => {
  detailDrawerRef.value?.open(record.id)
}

/* 确认发货 */
const handleConfirmShip = (record: ShippingOrderPageVO) => {
  Modal.confirm({
    title: '确认发货',
    content: `确定要将物流单 ${record.shippingNo} 标记为已发货吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: () => {
      doRequest(confirmShip(record.id), {
        successMessage: '确认发货成功',
        onSuccess: () => reloadTable()
      })
    }
  })
}

/* 创建入库单 */
const handleCreateInbound = (record: ShippingOrderPageVO) => {
  router.push({
    path: '/wms/purchase-inbound/form/create',
    query: { shippingOrderId: record.id }
  })
}

/* 确认付款 */
const handleConfirmPayment = (record: ShippingOrderPageVO) => {
  paymentModalRef.value?.open(record.id, record.shippingNo)
}

/* 删除物流单 */
const handleDelete = (record: ShippingOrderPageVO) => {
  doRequest(deleteShippingOrder([record.id]), {
    successMessage: '删除成功',
    onSuccess: () => reloadTable(true)
  })
}

/* 导出 */
const handleExport = async () => {
  try {
    const response = await exportShippingOrder(searchParams)
    // 创建下载链接
    const blob = new Blob([response.data as BlobPart], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `物流单_${new Date().toISOString().split('T')[0]}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    message.error('导出失败')
  }
}
</script>

<style scoped>
/* ==================== 表格基础样式 ==================== */
:deep(.ant-table-tbody > tr > td) {
  vertical-align: top;
  padding: 12px 8px;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 600;
}

/* ==================== 通用单元格布局 ==================== */
.shipping-info-cell,
.status-cargo-cell,
.logistics-info-cell,
.amount-date-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  line-height: 22px;
}

/* ==================== 物流单列 ==================== */
.shipping-no-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.shipping-no {
  font-weight: 500;
  font-size: 14px;
  color: #1890ff;
}

.shipping-no:hover {
  color: #40a9ff;
}

.copy-icon {
  color: #bfbfbf;
  cursor: pointer;
  font-size: 12px;
  transition: color 0.2s;
}

.copy-icon:hover {
  color: #1890ff;
}

.copy-success {
  font-size: 12px;
  color: #52c41a;
}

.create-time {
  font-size: 12px;
  color: #8c8c8c;
}

/* ==================== 状态 & 货物列 ==================== */
.cargo-stats {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #595959;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.stat-label {
  font-size: 12px;
  color: #8c8c8c;
}

.stat-value {
  font-weight: 500;
  color: #262626;
}

.stat-arrow {
  color: #d9d9d9;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.progress-bar {
  flex: 1;
  max-width: 100px;
}

.progress-bar :deep(.ant-progress-inner) {
  background-color: #f5f5f5;
}

.progress-percent {
  font-size: 12px;
  color: #595959;
  min-width: 32px;
  text-align: right;
}

.complete-tag {
  margin: 0;
}

/* ==================== 物流信息列 ==================== */
.provider-name {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

.route-info {
  font-size: 12px;
  color: #8c8c8c;
}

.route-info .separator {
  margin: 0 4px;
  color: #d9d9d9;
}

.warehouse-row {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #595959;
}

.arrow-icon {
  color: #8c8c8c;
}

/* ==================== 费用 & 时效列 ==================== */
.amount-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.amount {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.currency {
  color: #8c8c8c;
}

.payment-tag {
  margin: 0;
}

.date-row {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #8c8c8c;
}

.date-arrow {
  color: #d9d9d9;
}

.days-row {
  font-size: 12px;
  color: #1890ff;
}

/* ==================== 操作列 ==================== */
.more-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.disabled-action {
  color: rgba(0, 0, 0, 0.25);
  cursor: not-allowed;
}
</style>
