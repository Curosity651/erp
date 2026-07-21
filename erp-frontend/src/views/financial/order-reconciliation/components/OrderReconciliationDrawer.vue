<template>
  <a-drawer
    :open="open"
    title="订单财务对账详情"
    :width="680"
    :destroy-on-close="true"
    @close="handleClose"
  >
    <a-spin :spinning="loading">
      <template v-if="detailData">
        <!-- 订单信息 -->
        <a-card title="📦 订单信息" size="small" :bordered="false" class="detail-section">
          <a-descriptions :column="2" size="small">
            <a-descriptions-item label="订单号">
              {{ detailData.platformOrderId }}
            </a-descriptions-item>
            <a-descriptions-item label="RID">
              {{ detailData.rid || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="店铺">
              {{ detailData.shopName }}
            </a-descriptions-item>
            <a-descriptions-item label="商品信息" :span="2">
              <div v-if="detailData.items?.length">
                <div v-for="(item, idx) in detailData.items" :key="idx" style="line-height: 1.8">
                  <span>{{ item.skuCode || item.platformItemId }}</span>
                  <span v-if="item.quantity > 1" style="margin-left: 4px; color: #999">×{{ item.quantity }}</span>
                </div>
              </div>
              <span v-else>-</span>
            </a-descriptions-item>
            <a-descriptions-item label="订单金额">
              ₽ {{ formatNumber(detailData.totalAmountRub) }}
            </a-descriptions-item>
            <a-descriptions-item label="ERP状态">
              {{ mapErpStatus(detailData.erpStatus).label }}
            </a-descriptions-item>
            <a-descriptions-item label="商家处理状态">
              {{ mapWbSupplierStatus(detailData.platformSubstatus).label || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="平台履约状态">
              {{ mapWbPlatformStatus(detailData.platformStatus).label || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="订单时间(MSK)">
              {{ formatDateTime(detailData.orderTimeMoscow) }}
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- 对账状态 -->
        <a-card title="📊 对账状态" size="small" :bordered="false" class="detail-section">
          <a-descriptions :column="1" size="small">
            <a-descriptions-item label="对账状态">
              <a-tag :color="getStatusTagColor(detailData.reconciliationStatus)">
                {{ getStatusText(detailData.reconciliationStatus) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="说明">
              {{ getStatusDesc(detailData.reconciliationStatus) }}
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- 财务汇总 -->
        <a-card title="💰 财务汇总" size="small" :bordered="false" class="detail-section">
          <div class="actual-income">
            <a-tooltip title="实际收入（净额）= 销售金额 - 退货金额 - 罚款 - 扣款" placement="top">
              <span class="label">
                实际收入（净额）
                <span class="formula-hint">?</span>
              </span>
            </a-tooltip>
            <span
              :class="[
                'value',
                (detailData.financialSummary?.actualIncome ?? 0) >= 0 ? 'positive' : 'negative'
              ]"
            >
              {{ formatCurrency(detailData.financialSummary?.actualIncome) }}
            </span>
          </div>
          <a-row :gutter="[16, 8]" class="financial-summary-grid">
            <a-col :span="12">
              <div class="summary-item">
                <span class="label">销售金额</span>
                <span class="value positive">
                  {{ formatCurrency(detailData.financialSummary?.saleAmount) }}
                </span>
              </div>
            </a-col>
            <a-col :span="12">
              <div class="summary-item">
                <span class="label">退货金额</span>
                <span class="value negative">
                  {{ formatCurrency(detailData.financialSummary?.returnAmount) }}
                </span>
              </div>
            </a-col>
            <a-col :span="12">
              <div class="summary-item">
                <span class="label">罚款</span>
                <span class="value negative">
                  {{ formatCurrency(detailData.financialSummary?.penaltyAmount) }}
                </span>
              </div>
            </a-col>
            <a-col :span="12">
              <div class="summary-item">
                <span class="label">扣款</span>
                <span class="value negative">
                  {{ formatCurrency(detailData.financialSummary?.deductionAmount) }}
                </span>
              </div>
            </a-col>
          </a-row>
        </a-card>

        <!-- 关联财务记录 -->
        <a-card size="small" :bordered="false" class="detail-section">
          <template #title>
            <span>📋 关联财务记录</span>
            <a-tag
              v-if="recordPeriodType"
              :color="recordPeriodType === 'weekly' ? 'blue' : 'green'"
              style="margin-left: 8px"
            >
              {{ recordPeriodType === 'weekly' ? '周报' : '日报' }}
            </a-tag>
            <span style="margin-left: 4px; color: #8c8c8c"
              >({{ detailData.financialRecords?.length || 0 }}条)</span
            >
          </template>
          <template v-if="detailData.financialRecords && detailData.financialRecords.length > 0">
            <a-table
              size="small"
              :columns="recordColumns"
              :data-source="detailData.financialRecords"
              :pagination="false"
              :scroll="{ y: 280 }"
              row-key="id"
              class="record-table"
            >
              <template #bodyCell="{ column, record }">
                <!-- 报表信息列 -->
                <template v-if="column.key === 'reportInfo'">
                  <div class="cell-multi-line">
                    <div class="cell-primary">{{ record.realizationreportId }}</div>
                    <div class="cell-secondary">
                      {{ formatDateRange(record.dateFrom, record.dateTo) }}
                    </div>
                  </div>
                </template>
                <!-- 操作类型列 -->
                <template v-else-if="column.key === 'operType'">
                  <div class="cell-multi-line">
                    <div :class="['cell-primary', getOperNameClass(record.supplierOperName)]">
                      <span class="field-label">操作:</span>
                      {{ record.supplierOperName || '-' }}
                    </div>
                    <div v-if="record.docTypeName" class="cell-secondary">
                      <span class="field-label">文档:</span>
                      {{ record.docTypeName }}
                    </div>
                    <div
                      v-if="record.bonusTypeName"
                      class="cell-tertiary"
                      :title="record.bonusTypeName"
                    >
                      <span class="field-label">类型:</span>
                      {{ record.bonusTypeName }}
                    </div>
                  </div>
                </template>
                <!-- 金额列 -->
                <template v-else-if="column.key === 'amount'">
                  <div class="cell-multi-line cell-amount">
                    <div
                      v-if="record.ppvzForPay"
                      :class="['cell-primary', getAmountClass(record.supplierOperName)]"
                    >
                      {{ formatRecordCurrency(record.ppvzForPay, record.currencyName) }}
                    </div>
                    <div v-else class="cell-primary">-</div>
                    <div v-if="record.penalty" class="cell-penalty">
                      罚: {{ formatRecordCurrency(record.penalty, record.currencyName) }}
                    </div>
                    <div v-if="record.deduction" class="cell-deduction">
                      扣: {{ formatRecordCurrency(record.deduction, record.currencyName) }}
                    </div>
                    <div v-if="record.quantity" class="cell-quantity">×{{ record.quantity }}</div>
                  </div>
                </template>
                <!-- 时间列 -->
                <template v-else-if="column.key === 'time'">
                  <span class="cell-time">{{ formatDate(record.saleDt) }}</span>
                </template>
              </template>
            </a-table>
          </template>

          <a-empty v-else description="暂无财务记录" />
        </a-card>
      </template>

      <a-empty v-else-if="!loading" description="暂无数据" />
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import dayjs from 'dayjs'
import { formatDateTime } from '@/utils/financial-utils'
import { getCurrencySymbol } from '@/utils/currency-utils'
import { getOrderReconciliationDetail } from '@/api/financial/reconciliation'
import type {
  OrderReconciliationDetailVO,
  ReconciliationStatus
} from '@/api/financial/reconciliation/types'
import {
  RECONCILIATION_STATUS_TAG_COLOR,
  RECONCILIATION_STATUS_TEXT,
  RECONCILIATION_STATUS_DESC,
  mapErpStatus,
  mapWbSupplierStatus,
  mapWbPlatformStatus
} from '../constants'

// 销售类操作名称
const SALE_OPER_NAMES = ['销售', '合规销售', 'Продажа']
// 退货类操作名称
const RETURN_OPER_NAMES = ['退货', 'Возврат', '销售冲销']

// 格式化数字（千分位）
const formatNumber = (value?: number | null): string => {
  if (value === undefined || value === null) return '-'
  return value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 格式化日期（简短格式）
const formatDate = (value?: string | null): string => {
  if (!value) return '-'
  return dayjs(value).format('MM-DD HH:mm')
}

// 格式化日期范围
const formatDateRange = (dateFrom?: string | null, dateTo?: string | null): string => {
  if (!dateFrom && !dateTo) return '-'
  const from = dateFrom ? dayjs(dateFrom).format('MM-DD') : '?'
  const to = dateTo ? dayjs(dateTo).format('MM-DD') : '?'
  return `${from} ~ ${to}`
}

const props = defineProps<{
  open: boolean
  orderId?: number
  periodType: string
}>()

const emit = defineEmits<{
  (e: 'update:open', open: boolean): void
  (e: 'close'): void
}>()

const loading = ref(false)
const detailData = ref<OrderReconciliationDetailVO | null>(null)

// 获取财务记录的周期类型
const recordPeriodType = computed(() => {
  const records = detailData.value?.financialRecords
  if (records && records.length > 0) {
    return records[0].periodType
  }
  return props.periodType
})

// 获取币种符号
const currencySymbol = computed(() => {
  const name = detailData.value?.financialSummary?.currencyName || 'RUB'
  return getCurrencySymbol(name.toUpperCase()) || name
})

// 财务记录表格列定义
const recordColumns = [
  { title: '报表信息', key: 'reportInfo', width: 110 },
  { title: '操作类型', key: 'operType', ellipsis: true },
  { title: '金额', key: 'amount', width: 120, align: 'right' as const },
  { title: '时间', key: 'time', width: 90, align: 'center' as const }
]

// 格式化货币金额（汇总）
const formatCurrency = (value?: number | null): string => {
  if (value === undefined || value === null) return '-'
  const formatted = value.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
  return `${currencySymbol.value} ${formatted}`
}

// 格式化货币金额（记录行）
const formatRecordCurrency = (value: number | null | undefined, currencyName?: string): string => {
  if (value === undefined || value === null) return '-'
  const symbol = getCurrencySymbol((currencyName || 'RUB').toUpperCase()) || currencyName || '₽'
  const formatted = value.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
  return `${symbol} ${formatted}`
}

// 获取金额样式类
const getAmountClass = (operName?: string): string => {
  if (!operName) return ''
  if (SALE_OPER_NAMES.includes(operName)) return 'positive'
  return 'negative'
}

// 获取操作名称样式类
const getOperNameClass = (operName?: string): string => {
  if (!operName) return ''
  if (SALE_OPER_NAMES.includes(operName)) return 'oper-sale'
  if (RETURN_OPER_NAMES.includes(operName)) return 'oper-return'
  return 'oper-other'
}

// 加载详情数据
const loadDetail = async (orderId: number, periodType: string) => {
  loading.value = true
  detailData.value = null
  try {
    const res = await getOrderReconciliationDetail(orderId, periodType)
    if (res?.code === 200 && res.data) {
      detailData.value = res.data
    }
  } catch (e) {
    console.error('获取订单详情失败:', e)
  } finally {
    loading.value = false
  }
}

// 同时监听 orderId 和 open 变化
watch([() => props.open, () => props.orderId], ([open, orderId]) => {
  if (open && orderId && props.periodType) {
    loadDetail(orderId, props.periodType)
  }
})

// 对账状态辅助方法
const getStatusTagColor = (status: ReconciliationStatus) => {
  return RECONCILIATION_STATUS_TAG_COLOR[status] || 'default'
}

const getStatusText = (status: ReconciliationStatus) => {
  return RECONCILIATION_STATUS_TEXT[status] || '未知'
}

const getStatusDesc = (status: ReconciliationStatus) => {
  return RECONCILIATION_STATUS_DESC[status] || ''
}

// 关闭抽屉
const handleClose = () => {
  emit('update:open', false)
  emit('close')
}
</script>

<style lang="less" scoped>
.detail-section {
  margin-bottom: 8px;

  :deep(.ant-card-head) {
    min-height: 36px;
    padding: 0 12px;

    .ant-card-head-title {
      padding: 8px 0;
      font-size: 14px;
    }
  }

  :deep(.ant-card-body) {
    padding: 12px;
  }
}

.financial-summary-grid {
  .summary-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background: #fafafa;
    border-radius: 4px;

    .label {
      color: rgba(0, 0, 0, 0.65);
      font-size: 13px;
    }

    .value {
      font-weight: 500;
      font-size: 14px;
      font-variant-numeric: tabular-nums;

      &.positive {
        color: #52c41a;
      }

      &.negative {
        color: #ff4d4f;
      }
    }
  }
}

.actual-income {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #e6f7ff;
  border-radius: 4px;
  margin-bottom: 6px;

  .label {
    color: rgba(0, 0, 0, 0.85);
    font-weight: 500;
    font-size: 14px;
    cursor: help;
    display: inline-flex;
    align-items: center;
    gap: 4px;

    .formula-hint {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 14px;
      height: 14px;
      font-size: 10px;
      font-weight: 600;
      color: #1890ff;
      background: rgba(24, 144, 255, 0.1);
      border: 1px solid rgba(24, 144, 255, 0.3);
      border-radius: 50%;
    }
  }

  .value {
    font-weight: 600;
    font-size: 18px;
    font-variant-numeric: tabular-nums;

    &.positive {
      color: #52c41a;
    }

    &.negative {
      color: #ff4d4f;
    }
  }
}

// 财务记录表格样式
.record-table {
  :deep(.ant-table) {
    font-size: 12px;
  }

  :deep(.ant-table-thead > tr > th) {
    padding: 8px 8px;
    background: #fafafa;
    font-weight: 600;
    font-size: 12px;
  }

  :deep(.ant-table-tbody > tr > td) {
    padding: 8px 8px;
    vertical-align: top;
  }

  :deep(.ant-table-tbody > tr:hover > td) {
    background: #f5f5f5;
  }
}

// 多行单元格样式
.cell-multi-line {
  line-height: 1.4;

  .field-label {
    color: #8c8c8c;
    font-weight: 400;
    margin-right: 4px;
  }

  .cell-primary {
    font-weight: 500;
    font-size: 12px;
    color: #262626;
  }

  .cell-secondary {
    font-size: 11px;
    color: #8c8c8c;
    margin-top: 2px;
  }

  .cell-tertiary {
    font-size: 10px;
    color: #1890ff;
    margin-top: 2px;
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

// 金额列样式
.cell-amount {
  text-align: right;

  .cell-primary {
    font-variant-numeric: tabular-nums;
    font-weight: 600;

    &.positive {
      color: #52c41a;
    }

    &.negative {
      color: #ff4d4f;
    }
  }

  .cell-penalty,
  .cell-deduction {
    font-size: 10px;
    color: #ff4d4f;
    margin-top: 2px;
  }

  .cell-quantity {
    font-size: 10px;
    color: #8c8c8c;
    margin-top: 2px;
  }
}

// 操作名称颜色
.oper-sale {
  color: #52c41a !important;
}

.oper-return {
  color: #ff4d4f !important;
}

.oper-other {
  color: #8c8c8c !important;
}

// 时间列样式
.cell-time {
  font-size: 11px;
  color: #8c8c8c;
  font-variant-numeric: tabular-nums;
}
</style>
