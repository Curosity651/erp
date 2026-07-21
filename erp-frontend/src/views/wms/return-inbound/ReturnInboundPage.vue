<template>
  <!-- 查询表单 -->
  <return-inbound-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="退货入库单管理"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1200 }"
    size="middle"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('wms:return-inbound:add')" @click="handleNew" />
      <a-button @click="handleExport">
        <download-outlined />
        导出
      </a-button>
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <!-- 退货单信息列 -->
      <template v-if="column.key === 'returnInfo'">
        <div class="return-info-cell">
          <div class="return-no">
            <rollback-outlined class="return-icon" />
            <a @click="handleViewDetail(record)">{{ record.returnNo }}</a>
          </div>
          <div class="source-order">平台订单号: {{ record.platformOrderId }}</div>
        </div>
      </template>

      <!-- 平台列 -->
      <template v-else-if="column.key === 'platform'">
        <PlatformTag :platform="record.platform" />
      </template>

      <!-- SKU 列使用 SkuBriefCell -->
      <template v-else-if="column.key === 'sku'">
        <SkuBriefCell :brief="record.skuBrief" />
      </template>

      <!-- 退货原因列 -->
      <template v-else-if="column.key === 'returnReason'">
        <span>{{ ReturnReasonMap[record.returnReason] || record.returnReason || '-' }}</span>
      </template>

      <template v-else-if="column.key === 'returnStatus'">
        <a-badge
          :status="(RETURN_STATUS_BADGE[record.returnStatus] as any) || 'default'"
          :text="RETURN_STATUS_TEXT[record.returnStatus] || record.returnStatus"
        />
      </template>

      <!-- 数量列 -->
      <template v-else-if="column.key === 'quantity'">
        <div class="quantity-cell">
          <div>退货: {{ record.totalQuantity }}</div>
          <div class="qualified">入库: {{ record.qualifiedQuantity }}</div>
          <div class="unqualified">不合格: {{ record.unqualifiedQuantity }}</div>
        </div>
      </template>

      <!-- 操作列 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a @click="handleViewDetail(record)">查看</a>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 退货单详情抽屉 -->
  <return-inbound-detail-drawer ref="detailDrawerRef" />

  <!-- 新建退货入库弹窗（两步式） -->
  <return-inbound-modal ref="returnInboundModalRef" @success="reloadTable" />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { RollbackOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import ReturnInboundPageSearch from './ReturnInboundPageSearch.vue'
import ReturnInboundDetailDrawer from './ReturnInboundDetailDrawer.vue'
import ReturnInboundModal from './ReturnInboundModal.vue'
import { PlatformTag } from '@/components/Platform'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'
import { NewButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useAuthorize } from '@/hooks/permission'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'
import { mergePageParam } from '@/utils/page-utils'
import { remoteFileDownload } from '@/utils/file-utils'
import { pageReturnInbound, exportReturnInbound } from '@/api/wms/return-inbound'
import { ReturnReasonMap } from '@/api/wms/return-inbound/types'
import type { ReturnInboundPageVO, ReturnInboundQO } from '@/api/wms/return-inbound/types'

defineOptions({ name: 'ReturnInboundPage' })

// 鉴权方法
const { hasPermission } = useAuthorize()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const detailDrawerRef = ref<InstanceType<typeof ReturnInboundDetailDrawer>>()
const returnInboundModalRef = ref<InstanceType<typeof ReturnInboundModal>>()

// ==================== 表格配置 ====================

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

useTableActivateReload(() => reloadTable(false))

// 查询参数
let searchParams: ReturnInboundQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageReturnInbound({ ...pageParam, ...searchParams })
}

/* 查询退货单 */
const searchTable = (params: ReturnInboundQO) => {
  searchParams = params
  reloadTable(true)
}

// ==================== 列定义 ====================

const columns: ProColumns[] = [
  {
    title: '退货单信息',
    key: 'returnInfo',
    width: 220,
    fixed: 'left'
  },
  {
    title: '平台',
    key: 'platform',
    width: 100
  },
  {
    title: 'SKU',
    key: 'sku',
    width: 200
  },
  {
    title: '入库仓库',
    dataIndex: 'warehouseName',
    width: 120
  },
  {
    title: '退货日期',
    dataIndex: 'returnDate',
    width: 120
  },
  {
    title: '退货原因',
    key: 'returnReason',
    width: 120
  },
  {
    title: '状态',
    key: 'returnStatus',
    width: 110
  },
  {
    title: '数量',
    key: 'quantity',
    width: 140
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 160,
    sorter: true
  },
  {
    key: 'operate',
    title: '操作',
    align: 'center',
    width: 80,
    fixed: 'right'
  }
]

const RETURN_STATUS_TEXT: Record<string, string> = {
  RETURN_PENDING: '待仓库收货',
  QC_PENDING: '待质检',
  COMPLETED: '已完成',
  CLOSED: '已关闭'
}
const RETURN_STATUS_BADGE: Record<string, string> = {
  RETURN_PENDING: 'warning',
  QC_PENDING: 'processing',
  COMPLETED: 'success',
  CLOSED: 'default'
}

// ==================== 操作方法 ====================

/* 新建退货单 - 打开两步式弹窗 */
const handleNew = () => {
  returnInboundModalRef.value?.open()
}

/* 查看详情 */
const handleViewDetail = (record: ReturnInboundPageVO) => {
  detailDrawerRef.value?.open(record.id)
}

/* 导出 */
const handleExport = async () => {
  try {
    const response = await exportReturnInbound(searchParams)
    remoteFileDownload(response)
    message.success('导出成功')
  } catch (e) {
    message.error('导出失败')
  }
}
</script>

<style scoped>
:deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 600;
}

/* ==================== 退货单信息列 ==================== */
.return-info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.return-no {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 14px;
}

.return-icon {
  color: #faad14;
  font-size: 14px;
}

.source-order {
  font-size: 12px;
  color: #8c8c8c;
  margin-left: 20px;
}

/* ==================== 数量列 ==================== */
.quantity-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
}

.quantity-cell .qualified {
  color: #52c41a;
}

.quantity-cell .unqualified {
  color: #ff4d4f;
}
</style>
