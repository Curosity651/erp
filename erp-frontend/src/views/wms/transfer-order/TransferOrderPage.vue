<template>
  <!-- 查询表单 -->
  <transfer-order-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    class="transfer-order-table"
    header-title="调拨单管理"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 800 }"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('wms:transfer-order:add')" @click="handleNew" />
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <!-- 调拨单信息列 -->
      <template v-if="column.key === 'transferInfo'">
        <div class="transfer-info-cell">
          <div class="transfer-no">
            <a @click="handleViewDetail(record)">{{ record.transferNo }}</a>
          </div>
        </div>
      </template>

      <!-- 仓库信息列 -->
      <template v-else-if="column.key === 'warehouseInfo'">
        <div class="warehouse-cell">
          <div class="warehouse-arrow">
            <arrow-down-outlined />
          </div>
          <div class="warehouse-names">
            <div class="warehouse-name">{{ record.fromWarehouseName }}</div>
            <div class="warehouse-name">{{ record.toWarehouseName }}</div>
          </div>
        </div>
      </template>

      <!-- 数量列 -->
      <template v-else-if="column.key === 'quantity'">
        <span class="quantity-text">{{ formatQuantity(record) }}</span>
      </template>

      <!-- 状态列 -->
      <template v-else-if="column.key === 'orderStatus'">
        <a-badge
          :status="
            (TransferOrderStatusBadgeMap[record.orderStatus] || 'default') as
              | 'success'
              | 'default'
              | 'error'
              | 'processing'
              | 'warning'
          "
          :text="TransferOrderStatusMap[record.orderStatus]"
        />
      </template>

      <!-- 状态时间列 -->
      <template v-else-if="column.key === 'statusTime'">
        <span class="status-time">{{ getStatusTime(record) }}</span>
      </template>

      <!-- 操作列 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a @click="handleViewDetail(record)">查看</a>
          <a
            v-if="hasPermission('wms:transfer-order:edit') && canEdit(record)"
            @click="handleEdit(record)"
          >
            编辑
          </a>
          <template v-if="canShip(record)">
            <confirm-text-button
              title="确认要出库吗？出库后将扣减源仓库库存。"
              text="确认出库"
              @confirm="handleShip(record)"
            />
          </template>
          <template v-if="canReceive(record)">
            <a @click="handleReceive(record)">确认入库</a>
          </template>
          <template v-if="canRevoke(record)">
            <a @click="handleRevoke(record)">撤回</a>
          </template>
          <template v-if="canCancel(record)">
            <confirm-text-button
              title="确认要取消此调拨单吗？"
              text="取消"
              @confirm="handleCancel(record)"
            />
          </template>
          <template v-if="canDelete(record)">
            <delete-text-button @confirm="handleDelete(record)" />
          </template>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 调拨单详情抽屉 -->
  <transfer-order-detail-drawer ref="detailDrawerRef" @update-success="reloadTable" />

  <!-- 确认入库弹窗 -->
  <receive-confirm-modal ref="receiveModalRef" @success="reloadTable" />

  <!-- 撤回确认弹窗 -->
  <RevokeConfirmModal ref="revokeModalRef" @success="reloadTable" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { emitter } from '@/hooks/mitt'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'
import dayjs from 'dayjs'
import { ArrowDownOutlined } from '@ant-design/icons-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import TransferOrderPageSearch from './TransferOrderPageSearch.vue'
import TransferOrderDetailDrawer from './TransferOrderDetailDrawer.vue'
import ReceiveConfirmModal from './components/ReceiveConfirmModal.vue'
import RevokeConfirmModal from './components/RevokeConfirmModal.vue'
import { NewButton, ConfirmTextButton, DeleteTextButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { mergePageParam } from '@/utils/page-utils'
import { doRequest } from '@/utils/axios/request'
import {
  pageTransferOrder,
  shipTransferOrder,
  cancelTransferOrder,
  deleteTransferOrder
} from '@/api/wms/transfer-order'
import {
  TransferOrderStatusMap,
  TransferOrderStatusBadgeMap,
  TransferOrderStatusMeta,
  TransferOrderStatus
} from '@/api/wms/transfer-order/types'
import type { TransferOrderPageVO, TransferOrderQO } from '@/api/wms/transfer-order/types'
import { useTransferOrderActions } from './composables/useTransferOrderActions'

defineOptions({ name: 'TransferOrderPage' })

const router = useRouter()

// 权限判断方法
const { hasPermission, canEdit, canShip, canReceive, canRevoke, canCancel, canDelete } =
  useTransferOrderActions()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const detailDrawerRef = ref<InstanceType<typeof TransferOrderDetailDrawer>>()
const receiveModalRef = ref<InstanceType<typeof ReceiveConfirmModal>>()
const revokeModalRef = ref<InstanceType<typeof RevokeConfirmModal>>()

// ==================== 生命周期 ====================

// 监听表单页保存成功事件，刷新列表
onMounted(() => {
  emitter.on('refresh-transfer-order-list', () => reloadTable())
})

onUnmounted(() => {
  emitter.off('refresh-transfer-order-list')
})

// ==================== 辅助函数 ====================

/**
 * 获取状态时间显示
 */
const getStatusTime = (record: TransferOrderPageVO): string => {
  const meta = TransferOrderStatusMeta[record.orderStatus as TransferOrderStatus]
  if (!meta) return '-'
  const time = record[meta.timeField]
  return time ? `${meta.timePrefix} ${dayjs(time).format('MM-DD HH:mm')}` : '-'
}

/**
 * 格式化数量显示
 */
const formatQuantity = (record: TransferOrderPageVO): string => {
  return `${record.skuCount} SKU / ${record.totalQuantity} 件`
}

// ==================== 表格配置 ====================

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

useTableActivateReload(() => reloadTable(false))

// 查询参数
let searchParams: TransferOrderQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageTransferOrder({ ...pageParam, ...searchParams })
}

/* 查询调拨单 */
const searchTable = (params: TransferOrderQO) => {
  searchParams = params
  reloadTable(true)
}

// ==================== 列定义 ====================

const columns: ProColumns[] = [
  {
    title: '调拨单信息',
    key: 'transferInfo',
    width: 100,
    fixed: 'left'
  },
  {
    title: '仓库信息',
    key: 'warehouseInfo',
    width: 80
  },
  {
    title: '数量',
    key: 'quantity',
    width: 80
  },
  {
    title: '状态',
    key: 'orderStatus',
    width: 60
  },
  {
    title: '状态时间',
    key: 'statusTime',
    width: 120
  },
  {
    key: 'operate',
    title: '操作',
    align: 'center',
    width: 120,
    fixed: 'right'
  }
]

// ==================== 操作方法 ====================

/* 新建调拨单 - 导航到表单页面 */
const handleNew = () => {
  router.push('/wms/transfer-order/form/create')
}

/* 编辑调拨单 - 导航到表单页面 */
const handleEdit = (record: TransferOrderPageVO) => {
  router.push(`/wms/transfer-order/form/edit/${record.id}`)
}

/* 查看详情 */
const handleViewDetail = (record: TransferOrderPageVO) => {
  detailDrawerRef.value?.open(record.id)
}

/* 确认出库 */
const handleShip = (record: TransferOrderPageVO) => {
  doRequest(shipTransferOrder(record.id), {
    successMessage: '确认出库成功',
    onSuccess: () => reloadTable()
  })
}

/* 确认入库 */
const handleReceive = (record: TransferOrderPageVO) => {
  receiveModalRef.value?.open(record.id)
}

/* 撤回调拨单 */
const handleRevoke = (record: TransferOrderPageVO) => {
  revokeModalRef.value?.open(record)
}

/* 取消调拨单 */
const handleCancel = (record: TransferOrderPageVO) => {
  doRequest(cancelTransferOrder(record.id), {
    successMessage: '取消成功',
    onSuccess: () => reloadTable()
  })
}

/* 删除调拨单 */
const handleDelete = (record: TransferOrderPageVO) => {
  doRequest(deleteTransferOrder(record.id), {
    successMessage: '删除成功',
    onSuccess: () => reloadTable()
  })
}
</script>

<style scoped>
.transfer-order-table :deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
}

.transfer-order-table :deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 600;
}

/* ==================== 调拨单信息列 ==================== */
.transfer-info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.transfer-no {
  font-weight: 600;
  font-size: 14px;
}

.transfer-type {
  /* 标签左对齐，与单号起始位置一致 */
}

/* ==================== 仓库信息列 ==================== */
.warehouse-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.warehouse-arrow {
  color: #8c8c8c;
  font-size: 14px;
  flex-shrink: 0;
}

.warehouse-names {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.warehouse-name {
  font-size: 13px;
  color: #262626;
}

/* ==================== 数量列 ==================== */
.quantity-text {
  font-size: 13px;
  color: #595959;
}

/* ==================== 状态时间列 ==================== */
.status-time {
  font-size: 13px;
  color: #595959;
}
</style>
