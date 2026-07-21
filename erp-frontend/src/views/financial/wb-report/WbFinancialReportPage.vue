<template>
  <div class="wb-financial-report-page">
    <!-- 搜索表单 -->
    <wb-financial-report-search :loading="tableRef?.loading" @search="searchTable" />

    <!-- 数据表格 -->
    <pro-table
      ref="tableRef"
      row-key="id"
      :request="tableRequest"
      :columns="columns"
      :scroll="{ x: 2400 }"
      :pagination="{
        pageSizeOptions: ['10', '20', '50', '100', '200']
      }"
    >
      <!-- 表格标题（含周期类型选择器） -->
      <template #headerTitle>
        <div class="table-header-title">
          <span class="title-text">WB财务报表</span>
          <a-divider type="vertical" />
          <a-radio-group
            v-model:value="periodType"
            button-style="solid"
            size="small"
            @change="handlePeriodTypeChange"
          >
            <a-radio-button value="weekly"> <calendar-outlined /> 周报 </a-radio-button>
            <a-radio-button value="daily"> <schedule-outlined /> 日报 </a-radio-button>
          </a-radio-group>
        </div>
      </template>

      <!-- 工具栏按钮 -->
      <template #toolBarRender>
        <a-space>
          <a-button
            v-if="hasPermission('financial:wb-report:sync')"
            type="primary"
            @click="handleSync"
          >
            <template #icon><sync-outlined /></template>
            同步
          </a-button>
          <a-popconfirm
            title="确认导出当前筛选条件下的数据？"
            ok-text="确认"
            cancel-text="取消"
            @confirm="handleExport"
          >
            <a-button v-if="hasPermission('financial:wb-report:export')">
              <template #icon><export-outlined /></template>
              导出
            </a-button>
          </a-popconfirm>
        </a-space>
      </template>

      <!-- 自定义列渲染（数据行）：全部使用插槽 -->
      <template #bodyCell="{ column, record, text }">
        <!-- 操作列 -->
        <template v-if="column.key === 'operate'">
          <a @click="handleViewDetail(record)">查看详情</a>
        </template>

        <!-- 周期类型列 -->
        <template v-else-if="column.dataIndex === 'periodType'">
          <a-tag :color="text === 'weekly' ? 'blue' : 'green'">
            {{ text === 'weekly' ? '周报' : '日报' }}
          </a-tag>
        </template>

        <!-- 日期字段：统一使用 formatDate -->
        <template v-else-if="DATE_FIELDS.includes(column.dataIndex as string)">
          {{ formatDate(text) }}
        </template>

        <!-- 日期时间字段：统一使用 formatDateTime -->
        <template v-else-if="DATETIME_FIELDS.includes(column.dataIndex as string)">
          {{ formatDateTime(text) }}
        </template>

        <!-- 金额字段：统一使用 formatCurrency -->
        <template v-else-if="CURRENCY_FIELDS.includes(column.dataIndex as string)">
          {{ formatCurrency(text, record.currencyName) }}
        </template>

        <!-- 其他字段：直接显示原始值 -->
        <template v-else>
          {{ text }}
        </template>
      </template>

      <!-- 自定义表头：上面中文（主标题），下面英文（副标题，下划线风格） -->
      <template #headerCell="{ title, column }">
        <div v-if="column.key !== 'operate'" class="wb-header-cell">
          <div class="wb-header-zh">
            <component :is="title" />
          </div>
          <div class="wb-header-en">
            {{ toSnakeCase(column.dataIndex as string) }}
          </div>
        </div>
        <template v-else>
          <component :is="title" />
        </template>
      </template>
    </pro-table>

    <!-- 详情弹窗 -->
    <wb-financial-report-detail-modal v-model:open="detailModalVisible" :record="selectedRecord" />

    <!-- 同步弹窗 -->
    <wb-financial-report-sync-modal v-model:open="syncModalVisible" @success="handleSyncSuccess" />
  </div>
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProTableInstanceExpose, TableRequest } from '#/table'
import { message } from 'ant-design-vue'
import {
  SyncOutlined,
  ExportOutlined,
  CalendarOutlined,
  ScheduleOutlined
} from '@ant-design/icons-vue'
import WbFinancialReportSearch from './WbFinancialReportSearch.vue'
import WbFinancialReportDetailModal from './WbFinancialReportDetailModal.vue'
import WbFinancialReportSyncModal from './WbFinancialReportSyncModal.vue'
import { mergePageParam } from '@/utils/page-utils'
import { pageWbReportDetail, exportWbReportDetail } from '@/api/financial/wb-report'
import type {
  WbReportDetailQO,
  WbReportDetailVO,
  WbReportDetailPageParam,
  SyncTaskResponse
} from '@/api/financial/wb-report/types'
import { formatDate, formatDateTime, formatCurrency } from '@/utils/financial-utils'
import { remoteFileDownload } from '@/utils/file-utils'
import { useAuthorize } from '@/hooks/permission'
import dayjs from 'dayjs'
import {
  DATE_FIELDS,
  DATETIME_FIELDS,
  CURRENCY_FIELDS,
  toSnakeCase,
  getColumnsWithOperate
} from './table-config'

defineOptions({ name: 'WbFinancialReportPage' })

// 权限控制
const { hasPermission } = useAuthorize()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()

// 周期类型（默认周报）
const periodType = ref<'weekly' | 'daily'>('weekly')

// 查询参数
let searchParams: WbReportDetailQO = {}

// 详情弹窗
const detailModalVisible = ref(false)
const selectedRecord = ref<WbReportDetailVO | null>(null)

// 同步弹窗
const syncModalVisible = ref(false)

/**
 * 刷新表格
 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

/**
 * 远程加载表格数据
 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter) as WbReportDetailPageParam
  return pageWbReportDetail({ ...pageParam, ...searchParams, periodType: periodType.value })
}

/**
 * 搜索
 */
const searchTable = (params: WbReportDetailQO) => {
  searchParams = { ...params, periodType: periodType.value }
  reloadTable(true)
}

/**
 * 周期类型切换
 */
const handlePeriodTypeChange = () => {
  searchParams = { ...searchParams, periodType: periodType.value }
  reloadTable(true)
}

// 使用共享的表格列配置
const columns = getColumnsWithOperate()

/**
 * 查看详情
 */
const handleViewDetail = (record: WbReportDetailVO) => {
  selectedRecord.value = record
  detailModalVisible.value = true
}

/**
 * 同步
 */
const handleSync = () => {
  syncModalVisible.value = true
}

/**
 * 同步成功回调
 */
const handleSyncSuccess = (response: SyncTaskResponse) => {
  message.success(`同步任务已创建！作业ID: ${response.jobId}, 待同步店铺: ${response.totalShops}个`)
  // 可以选择刷新表格
  // reloadTable()
}

/**
 * 导出（携带周期类型参数）
 */
const handleExport = async () => {
  try {
    const hide = message.loading('正在导出数据...', 0)
    const response = await exportWbReportDetail({ ...searchParams, periodType: periodType.value })
    hide()

    // 文件名包含周期类型
    const periodLabel = periodType.value === 'weekly' ? '周报' : '日报'
    const filename = `WB财务报表_${periodLabel}_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`
    remoteFileDownload(response, filename)

    message.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    message.error('导出失败：' + (error as Error).message)
  }
}
</script>

<style scoped>
.wb-financial-report-page {
  padding: 0;
  background: #f0f2f5;
  min-height: 100vh;
}

/* 表格样式优化 */
:deep(.ant-table) {
  font-size: 14px;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e8e8e8;
}

:deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  font-weight: 600;
  color: #262626;
  padding: 12px 8px;
  font-size: 13px;
}

.wb-header-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.wb-header-zh {
  font-size: 13px;
  font-weight: 600;
}

.wb-header-en {
  font-size: 11px;
  color: #8c8c8c;
}

:deep(.ant-table-tbody > tr) {
  transition: background-color 0.2s ease;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.ant-table-tbody > tr:hover) {
  background: #fafafa;
}

:deep(.ant-table-tbody > tr:last-child) {
  border-bottom: none;
}

:deep(.ant-table-tbody > tr > td) {
  padding: 12px 8px;
}

/* 操作链接样式 */
:deep(.ant-table-tbody a) {
  color: #1890ff;
  text-decoration: none;
  transition: color 0.2s ease;
}

:deep(.ant-table-tbody a:hover) {
  color: #40a9ff;
  text-decoration: underline;
}

/* ProTable 卡片样式 */
:deep(.ant-card) {
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

:deep(.ant-card-head) {
  border-bottom: 1px solid #f0f0f0;
  padding: 16px 24px;
}

:deep(.ant-card-body) {
  padding: 0;
}

/* 分页器样式 */
:deep(.ant-pagination) {
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

/* 表格标题样式 */
.table-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-header-title .title-text {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.table-header-title :deep(.ant-divider) {
  height: 20px;
  margin: 0 4px;
}

.table-header-title :deep(.ant-radio-button-wrapper) {
  padding: 0 12px;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .wb-financial-report-page {
    padding: 0;
  }

  :deep(.ant-table) {
    font-size: 12px;
  }

  :deep(.ant-table-thead > tr > th),
  :deep(.ant-table-tbody > tr > td) {
    padding: 8px 4px;
  }

  .table-header-title {
    flex-wrap: wrap;
  }
}
</style>
