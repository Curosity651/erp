<template>
  <!-- 查询表单 -->
  <stocktake-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 800 }"
  >
    <!-- 标题区域：Tabs -->
    <template #headerTitle>
      <a-tabs
        v-model:active-key="activeStatus"
        size="small"
        :tab-bar-style="{ marginBottom: 0 }"
        @change="handleTabChange"
      >
        <a-tab-pane key="ALL" tab="全部" />
        <a-tab-pane key="COUNTING" tab="盘点中" />
        <a-tab-pane key="REVIEWING" tab="待复核" />
        <a-tab-pane key="CONFIRMED" tab="已确认" />
        <a-tab-pane key="CANCELLED" tab="已取消" />
      </a-tabs>
    </template>

    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('wms:stocktake:add')" @click="handleNew" />
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <!-- 盘点单号列 -->
      <template v-if="column.dataIndex === 'stocktakeNo'">
        <a @click="handleViewDetail(record)">{{ record.stocktakeNo }}</a>
      </template>

      <!-- 状态列 -->
      <template v-else-if="column.key === 'orderStatus'">
        <a-badge
          :status="getStatusBadgeStatus(record.orderStatus)"
          :text="StocktakeStatusMap[record.orderStatus]"
        />
      </template>

      <!-- 仓库信息列 -->
      <template v-else-if="column.key === 'warehouse'">
        <div class="warehouse-cell">
          <span class="warehouse-name">{{ record.warehouseName }}</span>
          <a-tag :color="getModeColor(record.stocktakeMode)" style="margin-top: 4px">
            {{ getModeText(record.stocktakeMode) }}
          </a-tag>
        </div>
      </template>

      <!-- 盘点进度列 -->
      <template v-else-if="column.key === 'progress'">
        <div class="stats-row">
          <!-- 盘点中：显示进度 -->
          <template v-if="isInProgress(record)">
            <span class="stat-text">
              {{ record.completedLocationCount ?? 0 }}/{{ record.locationCount ?? 0 }} 库位
            </span>
            <a-progress
              :percent="getProgressPercent(record)"
              size="small"
              :show-info="false"
              :status="getProgressPercent(record) === 100 ? 'success' : 'active'"
              style="width: 80px"
            />
          </template>
          <!-- 已确认：显示差异 -->
          <template v-else-if="isConfirmed(record)">
            <span class="stat-text">{{ record.skuCount }} SKU</span>
            <span class="stat-text">·</span>
            <span v-if="record.diffCount > 0" class="stat-text-danger">
              差异 {{ record.diffCount }} 项
            </span>
            <span v-else class="stat-text-success">无差异</span>
          </template>
          <!-- 已取消：仅显示总数 -->
          <template v-else>
            <span class="stat-text">{{ record.skuCount }} SKU</span>
          </template>
        </div>
      </template>

      <!-- 创建时间列（格式化） -->
      <template v-else-if="column.dataIndex === 'createTime'">
        <span style="font-family: monospace">{{ formatDateTime(record.createTime) }}</span>
      </template>

      <template v-else-if="column.dataIndex === 'operatorNames'">
        <span>{{ record.operatorNames || '-' }}</span>
      </template>

      <!-- 操作列 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <!-- 盘点中高亮录入按钮 -->
          <a
            v-if="isInProgress(record) && hasPermission('wms:stocktake:input')"
            class="primary-action"
            @click="handleInput(record)"
          >
            录入
          </a>
          <a @click="handleViewDetail(record)">查看</a>
          <a v-if="isConfirmed(record)" @click="handlePrintPallets(record)">打印托盘</a>

          <a
            v-if="record.orderStatus === 'REVIEWING' && hasPermission('wms:stocktake:confirm')"
            class="primary-action"
            @click="handleReview(record)"
          >
            复核
          </a>

          <template v-if="isInProgress(record)">
            <confirm-text-button
              v-if="hasPermission('wms:stocktake:cancel')"
              title="确认要取消此盘点单吗？"
              text="取消"
              @confirm="handleCancel(record)"
            />
          </template>

          <template v-if="isCancelled(record)">
            <delete-text-button
              v-if="hasPermission('wms:stocktake:del')"
              @confirm="handleDelete(record)"
            />
          </template>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 盘点单详情抽屉 -->
  <stocktake-detail-drawer ref="detailDrawerRef" />

  <!-- 新建盘点单弹窗 -->
  <stocktake-form-modal ref="formModalRef" @submit-success="handleFormSuccess" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import StocktakePageSearch from './StocktakePageSearch.vue'
import StocktakeDetailDrawer from './StocktakeDetailDrawer.vue'
import StocktakeFormModal from './StocktakeFormModal.vue'
import { NewButton, DeleteTextButton, ConfirmTextButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useAuthorize } from '@/hooks/permission'
import { emitter } from '@/hooks/mitt'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'
import { mergePageParam } from '@/utils/page-utils'
import { doRequest } from '@/utils/axios/request'
import {
  pageStocktake,
  cancelStocktake,
  deleteStocktake,
  getStocktakePallets
} from '@/api/wms/stocktake'
import type { StocktakePageVO, StocktakeQO, StocktakeStatus } from '@/api/wms/stocktake/types'
import { isSuccess } from '@/api'
import { printPalletLabels } from '@/views/platform/pallet/pallet-label-print'

defineOptions({ name: 'StocktakePage' })

const router = useRouter()

// 鉴权方法
const { hasPermission } = useAuthorize()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const detailDrawerRef = ref<InstanceType<typeof StocktakeDetailDrawer>>()
const formModalRef = ref<InstanceType<typeof StocktakeFormModal>>()

// 盘点单状态映射
const StocktakeStatusMap: Record<string, string> = {
  DRAFT: '草稿',
  READY: '待执行',
  COUNTING: '盘点中',
  REVIEWING: '待复核',
  CONFIRMED: '已确认',
  CANCELLED: '已取消'
}

// ==================== 辅助函数 ====================

/**
 * 获取状态颜色
 */
/**
 * 获取状态徽标状态
 */
const getStatusBadgeStatus = (status: StocktakeStatus): string => {
  const fullMap: Record<string, string> = {
    COUNTING: 'processing',
    REVIEWING: 'warning',
    CONFIRMED: 'success',
    CANCELLED: 'default'
  }
  return fullMap[status] || 'default'
}

/**
 * 是否为盘点中状态
 */
const isInProgress = (record: StocktakePageVO): boolean => {
  return record.orderStatus === 'COUNTING'
}

const getModeText = (mode?: string) =>
  ({ FULL: '全仓盘点', CYCLE: '循环盘点', SPECIAL: '专项盘点' })[mode || ''] || '历史盘点'

const getModeColor = (mode?: string) =>
  ({ FULL: 'blue', CYCLE: 'cyan', SPECIAL: 'orange' })[mode || ''] || 'default'

/**
 * 是否为已取消状态
 */
const isCancelled = (record: StocktakePageVO): boolean => {
  return record.orderStatus === 'CANCELLED'
}

/**
 * 是否为已确认状态
 */
const isConfirmed = (record: StocktakePageVO): boolean => {
  return record.orderStatus === 'CONFIRMED'
}

/**
 * 计算进度百分比
 */
const getProgressPercent = (record: StocktakePageVO): number => {
  if (!record.locationCount) return 0
  return Math.round(((record.completedLocationCount || 0) / record.locationCount) * 100)
}

/**
 * 格式化日期时间（保留到分钟）
 */
const formatDateTime = (dateTime: string | undefined): string => {
  if (!dateTime) return '-'
  return dateTime.substring(0, 16) // YYYY-MM-DD HH:mm
}

// ==================== 表格配置 ====================

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

useTableActivateReload(() => reloadTable(false))

// 查询参数
let searchParams: StocktakeQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  // 合并 Tab 状态
  const statusParam =
    activeStatus.value === 'ALL' ? {} : { orderStatus: activeStatus.value as StocktakeStatus }
  return pageStocktake({ ...pageParam, ...searchParams, ...statusParam })
}

/* 查询盘点单 */
const searchTable = (params: StocktakeQO) => {
  searchParams = params
  reloadTable(true)
}

// ==================== 列定义 ====================

const columns: ProColumns[] = [
  {
    title: '盘点单号',
    dataIndex: 'stocktakeNo',
    width: 120,
    fixed: 'left'
  },
  {
    title: '状态',
    dataIndex: 'orderStatus',
    width: 100
  },
  {
    title: '盘点仓库',
    key: 'warehouse',
    width: 120
  },
  {
    title: '盘点进度',
    key: 'progress',
    width: 140
  },
  {
    title: '操作人',
    dataIndex: 'operatorNames',
    width: 130
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 140
  },
  {
    key: 'operate',
    title: '操作',
    align: 'center',
    width: 150,
    fixed: 'right'
  }
]

// ==================== 操作方法 ====================

/* 新建盘点单 - 打开弹窗 */
const handleNew = () => {
  formModalRef.value?.open()
}

/* 新建盘点单成功回调 */
const handleFormSuccess = () => {
  reloadTable(true)
}

/* 查看详情 */
const handleViewDetail = (record: StocktakePageVO) => {
  detailDrawerRef.value?.open(record.id)
}

/* 录入盘点 - 导航到录入页面 */
const handleInput = (record: StocktakePageVO) => {
  router.push(`/wms/stocktake/input/${record.id}`)
}

const handleReview = (record: StocktakePageVO) => {
  router.push(`/wms/stocktake/review/${record.id}`)
}

/* 取消盘点单 */
const handleCancel = (record: StocktakePageVO) => {
  doRequest(cancelStocktake(record.id), {
    successMessage: '取消成功',
    onSuccess: () => reloadTable()
  })
}

/* 删除盘点单 */
const handleDelete = (record: StocktakePageVO) => {
  doRequest(deleteStocktake([record.id]), {
    successMessage: '删除成功',
    onSuccess: () => reloadTable(true)
  })
}

const handlePrintPallets = async (record: StocktakePageVO) => {
  const printPage = window.open('', '_blank')
  if (!printPage) {
    message.warning('浏览器阻止了打印窗口，请允许本站弹出窗口后重试')
    return
  }
  try {
    const result = await getStocktakePallets(record.id)
    if (!isSuccess(result)) {
      printPage.close()
      return
    }
    const pallets = result.data || []
    if (!pallets.length) {
      printPage.close()
      message.info('该盘点单没有需要更新的托盘标签')
      return
    }
    await printPalletLabels(pallets, printPage)
  } catch (error: any) {
    printPage.close()
    message.error(error?.message || '托盘标签加载失败')
  }
}

// ==================== Tabs 逻辑 ====================

const activeStatus = ref('ALL')

const handleTabChange = () => {
  reloadTable(true)
}

// 监听盘点确认事件，刷新列表
onMounted(() => {
  emitter.on('stocktake-confirmed', () => reloadTable(true))
})

onUnmounted(() => {
  emitter.off('stocktake-confirmed')
})
</script>

<style scoped lang="less">
@import '@/styles/antd-less-bridge.less';

// 表头加粗
:deep(.ant-table-thead > tr > th) {
  font-weight: 600;
}

// 单元格垂直居中
:deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
}

// 仓库单元格
.warehouse-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.warehouse-name {
  font-weight: 500;
  color: @heading-color;
}

// 进度统计行
.stats-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-text {
  font-size: 13px;
  color: #595959;
}

.stat-text-danger {
  color: #ff4d4f;
  font-weight: 600;
}

.stat-text-success {
  color: #52c41a;
}

// 操作按钮高亮
.primary-action {
  color: #1677ff;
  font-weight: 600;
}
</style>
