<template>
  <div class="order-reconciliation-panel">
    <!-- 筛选区域 -->
    <div class="filter-section">
      <a-form layout="inline" :model="formState" class="search-form">
        <!-- 周期类型（必填） -->
        <a-form-item label="周期类型" required>
          <a-radio-group v-model:value="formState.periodType" button-style="solid">
            <a-radio-button
              v-for="item in PERIOD_TYPE_OPTIONS"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>

        <!-- 对账状态（多选） -->
        <a-form-item label="对账状态">
          <a-select
            v-model:value="formState.reconciliationStatus"
            mode="multiple"
            :options="RECONCILIATION_STATUS_OPTIONS"
            placeholder="全部状态"
            allow-clear
            :max-tag-count="2"
            style="min-width: 200px"
          />
        </a-form-item>

        <!-- 店铺选择 -->
        <a-form-item label="店铺">
          <shop-select-input
            v-model="formState.shopIds"
            :multiple="true"
            placeholder="选择店铺"
            style="width: 200px"
          />
        </a-form-item>

        <!-- 订单时间范围 -->
        <a-form-item label="订单时间">
          <a-range-picker
            v-model:value="dateRange"
            :placeholder="['开始日期', '结束日期']"
            value-format="YYYY-MM-DD"
            style="width: 240px"
            @change="handleDateChange"
          />
        </a-form-item>

        <!-- 关键词搜索 -->
        <a-form-item label="关键词">
          <a-input
            v-model:value="formState.keyword"
            placeholder="订单号/商品编码"
            allow-clear
            style="width: 180px"
            @press-enter="handleSearch"
          />
        </a-form-item>
      </a-form>

      <!-- 操作按钮区域 -->
      <div class="action-buttons">
        <a-space>
          <a-button type="primary" :loading="tableRef?.loading" @click="handleSearch">
            <template #icon><search-outlined /></template>
            搜索
          </a-button>
          <a-button @click="handleReset">
            <template #icon><reload-outlined /></template>
            重置
          </a-button>
          <a-button :loading="exportLoading" @click="handleExport">
            <template #icon><export-outlined /></template>
            导出
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- 统计概览 - 卡片式布局 -->
    <div class="stats-overview">
      <a-spin :spinning="statsLoading">
        <div class="stats-cards">
          <!-- 订单总数 -->
          <div class="stat-card total">
            <div class="stat-icon">
              <unordered-list-outlined />
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ (statsData.totalOrders ?? 0).toLocaleString() }}</div>
              <div class="stat-label">订单总数</div>
            </div>
          </div>

          <!-- 待履约 -->
          <div class="stat-card pending">
            <div class="stat-icon">
              <clock-circle-outlined />
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ (statsData.pendingCount ?? 0).toLocaleString() }}</div>
              <div class="stat-label">待履约</div>
            </div>
            <div class="stat-percent">
              {{
                (statsData.totalOrders ?? 0) > 0
                  ? (((statsData.pendingCount ?? 0) / statsData.totalOrders) * 100).toFixed(1)
                  : 0
              }}%
            </div>
          </div>

          <!-- 运输中 -->
          <div class="stat-card in-transit">
            <div class="stat-icon">
              <car-outlined />
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ (statsData.inTransitCount ?? 0).toLocaleString() }}</div>
              <div class="stat-label">运输中</div>
            </div>
            <div class="stat-percent">
              {{
                (statsData.totalOrders ?? 0) > 0
                  ? (((statsData.inTransitCount ?? 0) / statsData.totalOrders) * 100).toFixed(1)
                  : 0
              }}%
            </div>
          </div>

          <!-- 已取消 -->
          <div class="stat-card canceled">
            <div class="stat-icon">
              <close-circle-outlined />
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ (statsData.canceledCount ?? 0).toLocaleString() }}</div>
              <div class="stat-label">已取消</div>
            </div>
            <div class="stat-percent">
              {{
                (statsData.totalOrders ?? 0) > 0
                  ? (((statsData.canceledCount ?? 0) / statsData.totalOrders) * 100).toFixed(1)
                  : 0
              }}%
            </div>
          </div>

          <!-- 已对账 -->
          <div class="stat-card matched">
            <div class="stat-icon">
              <check-circle-outlined />
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ (statsData.matchedCount ?? 0).toLocaleString() }}</div>
              <div class="stat-label">已对账</div>
            </div>
            <div class="stat-percent">
              {{
                (statsData.totalOrders ?? 0) > 0
                  ? (((statsData.matchedCount ?? 0) / statsData.totalOrders) * 100).toFixed(1)
                  : 0
              }}%
            </div>
          </div>

          <!-- 异常 -->
          <div class="stat-card anomaly">
            <div class="stat-icon">
              <warning-outlined />
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ (statsData.anomalyCount ?? 0).toLocaleString() }}</div>
              <div class="stat-label">异常</div>
            </div>
            <div class="stat-percent">
              {{
                (statsData.totalOrders ?? 0) > 0
                  ? (((statsData.anomalyCount ?? 0) / statsData.totalOrders) * 100).toFixed(1)
                  : 0
              }}%
            </div>
          </div>
        </div>
      </a-spin>
    </div>

    <!-- 数据表格 -->
    <pro-table
      ref="tableRef"
      header-title="订单财务对账数据"
      row-key="orderId"
      :request="tableRequest"
      :columns="columns"
      :scroll="{ x: 1300 }"
      :pagination="{
        pageSizeOptions: ['10', '20', '50', '100']
      }"
      :search="false"
    >
      <template #bodyCell="{ column, record }">
        <!-- 订单信息列：与 WbOrderPage 布局一致 -->
        <template v-if="column.key === 'orderInfo'">
          <OrderBasicInfoCell
            :platform="record.platform"
            :platform-order-id="record.platformOrderId"
            :erp-order-id="record.orderId"
            :shop-name="record.shopName"
            :fulfillment-type="record.fulfillmentType"
            :order-time="record.orderTimeMoscow"
            timezone="MSK"
          >
            <template v-if="record.rid" #extra>
              <span class="rid-value" :title="record.rid">{{ record.rid }}</span>
            </template>
          </OrderBasicInfoCell>
        </template>

        <!-- 商品信息列：使用 SkuInfoCell 与 WbOrderPage 保持一致 -->
        <template v-else-if="column.key === 'skuInfo'">
          <SkuInfoCell :items="record.items" />
        </template>

        <!-- 金额&状态列：使用 WbAmountStatusCell 组件 -->
        <template v-else-if="column.key === 'amountStatus'">
          <WbAmountStatusCell
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
          />
        </template>

        <!-- 对账信息列：合并对账状态和财务汇总 -->
        <template v-else-if="column.key === 'reconciliationInfo'">
          <div class="reconciliation-info-cell">
            <!-- 对账状态标签 -->
            <div class="status-header">
              <a-tooltip :title="getStatusDesc(record.reconciliationStatus)">
                <a-tag :color="getStatusTagColor(record.reconciliationStatus)" class="status-tag">
                  {{ getStatusText(record.reconciliationStatus) }}
                </a-tag>
              </a-tooltip>
              <span class="record-count">
                <file-text-outlined />
                {{ record.financialRecordCount }} 条记录
              </span>
            </div>
            <!-- 财务数据 -->
            <div class="finance-data">
              <div class="finance-row">
                <span class="finance-label">销售</span>
                <span class="finance-value">{{
                  formatCurrencyAmount(record.saleAmount, record.financialCurrencyName)
                }}</span>
              </div>
              <div class="finance-row">
                <span class="finance-label">收入</span>
                <span
                  :class="[
                    'finance-value',
                    'income',
                    record.actualIncome >= 0 ? 'positive' : 'negative'
                  ]"
                >
                  {{ formatCurrencyAmount(record.actualIncome, record.financialCurrencyName) }}
                </span>
              </div>
            </div>
          </div>
        </template>

        <!-- 操作列：查看详情按钮 -->
        <template v-else-if="column.key === 'operate'">
          <a @click="handleViewDetail(record)">查看详情</a>
        </template>
      </template>
    </pro-table>

    <!-- 订单对账详情抽屉 -->
    <order-reconciliation-drawer
      v-model:open="drawerVisible"
      :order-id="currentOrderId"
      :period-type="formState.periodType"
      @close="handleDrawerClose"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  ExportOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  WarningOutlined,
  UnorderedListOutlined,
  FileTextOutlined,
  CarOutlined,
  CloseCircleOutlined
} from '@ant-design/icons-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import ShopSelectInput from '@/components/ShopSelectInput.vue'
import { remoteFileDownload } from '@/utils/file-utils'
import { mergePageParam } from '@/utils/page-utils'
import { getCurrencySymbol } from '@/utils/currency-utils'
import {
  pageOrderReconciliation,
  getOrderReconciliationStats,
  exportOrderReconciliation
} from '@/api/financial/reconciliation'
import type {
  OrderReconciliationVO,
  OrderReconciliationQO,
  OrderReconciliationStatsVO,
  ReconciliationStatus,
  PeriodType
} from '@/api/financial/reconciliation/types'
import {
  PERIOD_TYPE_OPTIONS,
  RECONCILIATION_STATUS_OPTIONS,
  RECONCILIATION_STATUS_TAG_COLOR,
  RECONCILIATION_STATUS_TEXT,
  RECONCILIATION_STATUS_DESC,
  mapErpStatus,
  mapWbSupplierStatus,
  mapWbPlatformStatus
} from './constants'
import { OrderBasicInfoCell, WbAmountStatusCell } from '@/views/order/components'
import SkuInfoCell from '@/components/Sku/SkuInfoCell.vue'
import OrderReconciliationDrawer from './components/OrderReconciliationDrawer.vue'

// ===================== 表格引用 =====================
const tableRef = ref<ProTableInstanceExpose>()

// ===================== 搜索参数 =====================
const formState = reactive<{
  periodType: PeriodType
  reconciliationStatus: ReconciliationStatus[]
  shopIds: number[]
  startDate?: string
  endDate?: string
  keyword: string
}>({
  periodType: 'weekly',
  reconciliationStatus: [],
  shopIds: [],
  startDate: '',
  endDate: '',
  keyword: ''
})

// 日期范围（用于 RangePicker 组件）
const dateRange = ref<[string, string] | null>(null)

// 初始化默认日期范围（近30天）
const initDefaultDateRange = () => {
  const end = undefined
  const start = undefined
  dateRange.value = null
  formState.startDate = start
  formState.endDate = end
}

// 日期变化处理
const handleDateChange = (dates: [string, string] | null) => {
  if (dates && dates.length === 2) {
    formState.startDate = dates[0]
    formState.endDate = dates[1]
  } else {
    formState.startDate = ''
    formState.endDate = ''
  }
}

// ===================== 统计数据 =====================
const statsLoading = ref(false)
const statsData = ref<OrderReconciliationStatsVO>({
  totalOrders: 0,
  pendingCount: 0,
  inTransitCount: 0,
  canceledCount: 0,
  matchedCount: 0,
  anomalyCount: 0
})

const loadStats = async () => {
  statsLoading.value = true
  try {
    const res = await getOrderReconciliationStats(getSearchParams())
    if (res?.code === 200 && res.data) {
      statsData.value = res.data
    }
  } catch (e) {
    console.error('获取订单统计数据失败:', e)
  } finally {
    statsLoading.value = false
  }
}

// ===================== 格式化货币金额 =====================
const formatCurrencyAmount = (amount?: number | null, currencyName?: string | null): string => {
  if (amount === undefined || amount === null) return '-'
  const code = (currencyName || 'RUB').toUpperCase()
  const symbol = getCurrencySymbol(code) || code
  const formatted = amount.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
  return `${symbol} ${formatted}`
}

// ===================== 表格列定义 =====================
const columns: ProColumns[] = [
  {
    title: '订单信息',
    key: 'orderInfo',
    width: 120,
    fixed: 'left'
  },
  {
    title: '商品信息',
    key: 'skuInfo',
    width: 180
  },
  {
    title: '金额 & 状态',
    key: 'amountStatus',
    width: 100
  },
  {
    title: '对账信息',
    key: 'reconciliationInfo',
    width: 100
  },
  {
    title: '操作',
    key: 'operate',
    width: 80,
    fixed: 'right',
    align: 'center'
  }
]

// ===================== 获取搜索参数 =====================
const getSearchParams = (): OrderReconciliationQO => ({
  periodType: formState.periodType,
  reconciliationStatus:
    formState.reconciliationStatus.length > 0 ? formState.reconciliationStatus : undefined,
  shopIds: formState.shopIds.length > 0 ? formState.shopIds : undefined,
  startDate: formState.startDate || undefined,
  endDate: formState.endDate || undefined,
  keyword: formState.keyword || undefined
})

// ===================== 表格请求 =====================
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageOrderReconciliation(pageParam, getSearchParams())
}

// ===================== 搜索与重置 =====================
const handleSearch = () => {
  tableRef.value?.actionRef?.reload(true)
  loadStats()
}

const handleReset = () => {
  formState.periodType = 'weekly'
  formState.reconciliationStatus = []
  formState.shopIds = []
  formState.keyword = ''
  initDefaultDateRange()
  tableRef.value?.actionRef?.reload(true)
  loadStats()
}

// ===================== 刷新表格 =====================
const reload = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

// ===================== 辅助方法 =====================
const getStatusTagColor = (status: ReconciliationStatus) => {
  return RECONCILIATION_STATUS_TAG_COLOR[status] || 'default'
}

const getStatusText = (status: ReconciliationStatus) => {
  return RECONCILIATION_STATUS_TEXT[status] || '未知'
}

const getStatusDesc = (status: ReconciliationStatus) => {
  return RECONCILIATION_STATUS_DESC[status] || ''
}

// ===================== 导出 =====================
const exportLoading = ref(false)

const handleExport = async () => {
  exportLoading.value = true
  try {
    const response = await exportOrderReconciliation(getSearchParams())
    remoteFileDownload(response)
    message.success('导出成功')
  } catch (e) {
    message.error('导出失败')
    console.error('导出失败:', e)
  } finally {
    exportLoading.value = false
  }
}

// ===================== 详情抽屉 =====================
const drawerVisible = ref(false)
const currentOrderId = ref<number>()

const handleViewDetail = (record: OrderReconciliationVO) => {
  currentOrderId.value = record.orderId
  drawerVisible.value = true
}

const handleDrawerClose = () => {
  drawerVisible.value = false
  currentOrderId.value = undefined
}

// ===================== 暴露方法给父组件 =====================
defineExpose({
  reload
})

// ===================== 生命周期 =====================
onMounted(() => {
  initDefaultDateRange()
  loadStats()
})
</script>

<style lang="less" scoped>
.order-reconciliation-panel {
  .filter-section {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    padding: 16px;
    background: #fafafa;
    border-radius: 6px;
    border: 1px solid #f0f0f0;
    margin-bottom: 12px;

    .search-form {
      flex: 1;

      :deep(.ant-form-item) {
        margin-bottom: 8px;
        margin-right: 16px;
      }
    }

    .action-buttons {
      flex-shrink: 0;
      padding-top: 4px;
    }
  }

  .stats-overview {
    margin-bottom: 16px;

    .stats-cards {
      display: grid;
      grid-template-columns: repeat(6, 1fr);
      gap: 12px;
    }

    .stat-card {
      position: relative;
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 14px 16px;
      background: #fff;
      border-radius: 8px;
      border: 1px solid #f0f0f0;
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
      transition: all 0.3s ease;
      overflow: hidden;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 0;
        bottom: 0;
        width: 4px;
        border-radius: 8px 0 0 8px;
      }

      &:hover {
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
        transform: translateY(-2px);
      }

      .stat-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 36px;
        height: 36px;
        border-radius: 8px;
        font-size: 18px;
        flex-shrink: 0;
      }

      .stat-content {
        flex: 1;
        min-width: 0;
      }

      .stat-value {
        font-size: 20px;
        font-weight: 600;
        line-height: 1.2;
        color: #262626;
      }

      .stat-label {
        font-size: 12px;
        color: #8c8c8c;
        margin-top: 2px;
      }

      .stat-percent {
        position: absolute;
        top: 10px;
        right: 12px;
        font-size: 11px;
        font-weight: 500;
        padding: 2px 6px;
        border-radius: 10px;
      }

      // 总数卡片
      &.total {
        &::before {
          background: linear-gradient(180deg, #1890ff, #69c0ff);
        }

        .stat-icon {
          background: rgba(24, 144, 255, 0.1);
          color: #1890ff;
        }
      }

      // 待履约卡片
      &.pending {
        &::before {
          background: linear-gradient(180deg, #8c8c8c, #bfbfbf);
        }

        .stat-icon {
          background: rgba(140, 140, 140, 0.1);
          color: #8c8c8c;
        }

        .stat-percent {
          background: rgba(140, 140, 140, 0.1);
          color: #595959;
        }
      }

      // 运输中卡片
      &.in-transit {
        &::before {
          background: linear-gradient(180deg, #1890ff, #69c0ff);
        }

        .stat-icon {
          background: rgba(24, 144, 255, 0.1);
          color: #1890ff;
        }

        .stat-percent {
          background: rgba(24, 144, 255, 0.1);
          color: #1890ff;
        }
      }

      // 已取消卡片
      &.canceled {
        &::before {
          background: linear-gradient(180deg, #faad14, #ffc53d);
        }

        .stat-icon {
          background: rgba(250, 173, 20, 0.1);
          color: #faad14;
        }

        .stat-percent {
          background: rgba(250, 173, 20, 0.1);
          color: #d48806;
        }
      }

      // 已对账卡片
      &.matched {
        &::before {
          background: linear-gradient(180deg, #52c41a, #95de64);
        }

        .stat-icon {
          background: rgba(82, 196, 26, 0.1);
          color: #52c41a;
        }

        .stat-percent {
          background: rgba(82, 196, 26, 0.1);
          color: #52c41a;
        }
      }

      // 异常卡片
      &.anomaly {
        &::before {
          background: linear-gradient(180deg, #ff4d4f, #ff7875);
        }

        .stat-icon {
          background: rgba(255, 77, 79, 0.1);
          color: #ff4d4f;
        }

        .stat-percent {
          background: rgba(255, 77, 79, 0.1);
          color: #cf1322;
        }

        .stat-value {
          color: #cf1322;
        }
      }
    }

    // 响应式：中屏幕三列
    @media (max-width: 1400px) {
      .stats-cards {
        grid-template-columns: repeat(3, 1fr);
      }
    }

    // 响应式：小屏幕两列
    @media (max-width: 1000px) {
      .stats-cards {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    // 响应式：更小屏幕单列
    @media (max-width: 600px) {
      .stats-cards {
        grid-template-columns: 1fr;
      }
    }
  }

  // RID 样式（与 OrderBasicInfoCell 的设计风格一致）
  .rid-value {
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    font-size: 12px;
    color: #8c8c8c;
    font-variant-numeric: tabular-nums;
    font-feature-settings:
      'tnum' 1,
      'lnum' 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 190px;
  }

  // 对账信息单元格（合并对账状态与财务汇总）
  .reconciliation-info-cell {
    display: flex;
    flex-direction: column;
    gap: 8px;

    // 状态头部：对账状态标签 + 记录数
    .status-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;

      .status-tag {
        margin: 0;
        font-weight: 600;
        font-size: 12px;
      }

      .record-count {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 11px;
        color: #8c8c8c;
        white-space: nowrap;

        :deep(.anticon) {
          font-size: 12px;
        }
      }
    }

    // 财务数据区域
    .finance-data {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding: 8px 10px;
      background: #fafafa;
      border-radius: 6px;
      border: 1px solid #f0f0f0;
    }

    .finance-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 12px;
      line-height: 1.5;

      .finance-label {
        color: #8c8c8c;
        font-size: 11px;
      }

      .finance-value {
        font-weight: 500;
        color: #262626;
        font-variant-numeric: tabular-nums;
        font-feature-settings: 'tnum' 1;

        &.income {
          font-weight: 600;

          &.positive {
            color: #52c41a;
          }

          &.negative {
            color: #ff4d4f;
          }
        }
      }
    }
  }
}

// 表格增强样式
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
  padding: 14px 12px;
  font-size: 13px;
}

:deep(.ant-table-tbody > tr) {
  transition: background-color 0.2s ease;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.ant-table-tbody > tr:hover) {
  background: #fafafa;
}

:deep(.ant-table-tbody > tr > td) {
  padding: 10px 12px;
  vertical-align: middle;
}
</style>
