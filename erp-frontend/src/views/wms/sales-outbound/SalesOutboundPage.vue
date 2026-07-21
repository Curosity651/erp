<template>
  <!-- 查询表单 -->
  <sales-outbound-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 970 }"
    size="middle"
  >
    <!-- 标题区域：标题 + 平台筛选 -->
    <template #headerTitle>
      <div class="header-title-wrapper">
        <span class="header-title">销售出库单</span>
        <PlatformSegmented v-model:value="activePlatform" with-all @change="handlePlatformChange" />
      </div>
    </template>

    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('wms:sales-outbound:add')" @click="handleNew" />
      <a-button @click="handleExport">
        <DownloadOutlined />
        导出
      </a-button>
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <!-- 出库单信息列 -->
      <template v-if="column.key === 'outboundInfo'">
        <OutboundInfoCell
          :outbound-no="record.outboundNo"
          :platform="record.platform"
          @click="handleViewDetail(record)"
        />
      </template>

      <!-- 仓库列 -->
      <template v-else-if="column.key === 'warehouse'">
        <div class="warehouse-cell">
          <Warehouse :size="14" class="warehouse-icon" />
          <span class="warehouse-name">{{ record.warehouseName }}</span>
        </div>
      </template>

      <!-- 状态列 -->
      <template v-else-if="column.key === 'status'">
        <SalesOutboundStatusBadge :status="record.orderStatus" />
      </template>

      <!-- 出库内容列 -->
      <template v-else-if="column.key === 'statistics'">
        <StatisticsCell
          :order-count="record.orderCount"
          :sku-count="record.skuCount"
          :total-quantity="record.totalQuantity"
        />
      </template>

      <!-- 时间列 -->
      <template v-else-if="column.key === 'time'">
        <TimeCell
          primary-label="出库"
          :primary-time="record.outboundDate"
          secondary-label="创建"
          :secondary-time="record.createTime"
        />
      </template>

      <!-- 操作列 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a @click="handleViewDetail(record)">详情</a>
          <a
            v-if="hasPermission('wms:sales-outbound:edit') && canEdit(record)"
            @click="handleEdit(record)"
          >
            编辑
          </a>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 出库单详情抽屉 -->
  <sales-outbound-detail-drawer ref="detailDrawerRef" @update-success="reloadTable" />

  <!-- 新建出库单弹窗 -->
  <create-outbound-modal ref="createModalRef" @confirm="handleCreateConfirm" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import { Warehouse } from 'lucide-vue-next'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import SalesOutboundPageSearch from './SalesOutboundPageSearch.vue'
import SalesOutboundDetailDrawer from './SalesOutboundDetailDrawer.vue'
import CreateOutboundModal from './components/CreateOutboundModal.vue'
import type { CreateOutboundForm } from './components/CreateOutboundModal.vue'
import OutboundInfoCell from './components/OutboundInfoCell.vue'
import SalesOutboundStatusBadge from './components/SalesOutboundStatusBadge.vue'
import StatisticsCell from './components/StatisticsCell.vue'
import TimeCell from './components/TimeCell.vue'
import { NewButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useAuthorize } from '@/hooks/permission'
import { emitter } from '@/hooks/mitt'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'
import { mergePageParam } from '@/utils/page-utils'
import { remoteFileDownload } from '@/utils/file-utils'
import { pageSalesOutbound, exportSalesOutbound } from '@/api/wms/sales-outbound'
import { OutboundOrderStatus } from '@/api/wms/sales-outbound/types'
import type { SalesOutboundPageVO, SalesOutboundQO } from '@/api/wms/sales-outbound/types'
import { PlatformSegmented, type PlatformType } from '@/components/Platform'

defineOptions({ name: 'SalesOutboundPage' })

const router = useRouter()

// 鉴权方法
const { hasPermission } = useAuthorize()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const detailDrawerRef = ref<InstanceType<typeof SalesOutboundDetailDrawer>>()
const createModalRef = ref<InstanceType<typeof CreateOutboundModal>>()

// 当前选中的平台
const activePlatform = ref<PlatformType | ''>('')

// ==================== 辅助函数 ====================

/**
 * 是否可编辑
 */
const canEdit = (record: SalesOutboundPageVO): boolean => {
  return record.orderStatus === OutboundOrderStatus.DRAFT
}

// ==================== 表格配置 ====================

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

useTableActivateReload(() => reloadTable(false))

// 查询参数
let searchParams: SalesOutboundQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  const queryParams = { ...pageParam, ...searchParams }
  // 添加平台筛选
  if (activePlatform.value) {
    queryParams.platform = activePlatform.value
  }
  return pageSalesOutbound(queryParams)
}

/* 查询出库单 */
const searchTable = (params: SalesOutboundQO) => {
  searchParams = params
  reloadTable(true)
}

/* 平台Tab切换 */
const handlePlatformChange = (platform: string) => {
  activePlatform.value = platform
  reloadTable(true)
}

// ==================== 列定义 ====================

const columns: ProColumns[] = [
  {
    title: '出库单',
    key: 'outboundInfo',
    width: 240,
    fixed: 'left'
  },
  {
    title: '仓库',
    key: 'warehouse',
    width: 120
  },
  {
    title: '状态',
    key: 'status',
    width: 100
  },
  {
    title: '出库内容',
    key: 'statistics',
    width: 130
  },
  {
    title: '时间',
    key: 'time',
    width: 180
  },
  {
    title: '备注',
    dataIndex: 'remark',
    ellipsis: true,
    width: 160
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

/* 新建出库单 - 打开前置弹窗 */
const handleNew = () => {
  createModalRef.value?.open()
}

/* 前置弹窗确认 - 导航到表单页面 */
const handleCreateConfirm = (form: CreateOutboundForm) => {
  router.push({
    path: '/wms/sales-outbound/form/create',
    query: {
      platform: form.platform,
      warehouseId: form.warehouseId?.toString(),
      warehouseName: form.warehouseName
    }
  })
}

/* 编辑出库单 - 导航到表单页面 */
const handleEdit = (record: SalesOutboundPageVO) => {
  router.push(`/wms/sales-outbound/form/edit/${record.id}`)
}

/* 查看详情 */
const handleViewDetail = (record: SalesOutboundPageVO) => {
  detailDrawerRef.value?.open(record.id)
}

/* 导出 */
const handleExport = async () => {
  try {
    // 与列表查询口径一致：并入当前平台 Tab 筛选
    const exportParams: SalesOutboundQO = { ...searchParams }
    if (activePlatform.value) {
      exportParams.platform = activePlatform.value
    }
    const response = await exportSalesOutbound(exportParams)
    remoteFileDownload(response)
    message.success('导出成功')
  } catch (e) {
    message.error('导出失败')
  }
}

// 监听表单页面的刷新事件
onMounted(() => {
  emitter.on('refresh-sales-outbound-list', () => reloadTable())
})

onUnmounted(() => {
  emitter.off('refresh-sales-outbound-list')
})
</script>

<style scoped>
/* ==================== 标题区域 ==================== */
.header-title-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title-wrapper :deep(.ant-segmented-item) {
  min-height: 28px;
  line-height: 28px;
  align-content: center;
}

/* ==================== 仓库列 ==================== */
.warehouse-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.warehouse-icon {
  color: #8c8c8c;
  flex-shrink: 0;
}

.warehouse-name {
  font-size: 13px;
  color: #262626;
}
</style>
