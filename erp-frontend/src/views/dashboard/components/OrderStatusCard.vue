<template>
  <a-card class="dashboard-card">
    <template #title>
      <div class="flex items-center gap-2">
        <PieChartOutlined class="card-icon" />
        <span>订单状态分布</span>
      </div>
    </template>
    <div class="order-status-card">
      <!-- ECharts 饼图 -->
      <div v-if="tableData.length > 0" class="chart-container">
        <v-chart :option="chartOption" autoresize class="chart" />
      </div>

      <!-- 数据表格 -->
      <a-table
        :columns="columns"
        :data-source="tableData"
        :pagination="false"
        size="small"
        :row-class-name="(record: any, index: number) => (index % 2 === 0 ? 'even-row' : 'odd-row')"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusName(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'count'">
            <span class="count-value">{{ record.count }}</span>
          </template>
          <template v-else-if="column.key === 'amount'">
            <span class="amount-value">₽{{ formatAmount(record.amount) }}</span>
          </template>
        </template>

        <template #summary>
          <a-table-summary-row class="summary-row">
            <a-table-summary-cell :index="0">
              <span class="summary-label">合计</span>
            </a-table-summary-cell>
            <a-table-summary-cell :index="1" align="right">
              <span class="summary-value">{{ totalCount }}</span>
            </a-table-summary-cell>
            <a-table-summary-cell :index="2" align="right">
              <span class="summary-value">₽{{ formatAmount(totalAmount) }}</span>
            </a-table-summary-cell>
          </a-table-summary-row>
        </template>
      </a-table>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { PieChartOutlined } from '@ant-design/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import type { OrderStatusVO } from '@/api/dashboard/types'
import { ERP_STATUS_MAP, type ErpStatusKey } from '@/api/order/types'

// 注册 ECharts 组件
use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])

interface Props {
  data?: OrderStatusVO[]
}

const props = defineProps<Props>()

const columns = [
  { title: '状态', key: 'status', width: 120 },
  { title: '数量', key: 'count', width: 80, align: 'right' },
  { title: '金额', key: 'amount', align: 'right' }
]

const tableData = computed(() => props.data || [])

const totalCount = computed(() => tableData.value.reduce((sum, item) => sum + item.count, 0))

const totalAmount = computed(() => tableData.value.reduce((sum, item) => sum + item.amount, 0))

// 状态颜色映射
const statusColorMap: Record<ErpStatusKey, string> = {
  READY_TO_SHIP: 'warning',
  SHIPPED: 'processing',
  ARRIVED_AT_PLATFORM_WAREHOUSE: 'purple',
  DELIVERED: 'success',
  CANCELED: 'error',
  RETURNED: 'default'
}

const getStatusName = (status: string) => {
  const statusKey = status as ErpStatusKey
  return ERP_STATUS_MAP[statusKey]?.label || status
}

const getStatusColor = (status: string) => {
  const statusKey = status as ErpStatusKey
  return statusColorMap[statusKey] || 'default'
}

const formatAmount = (amount: number) => {
  return (
    amount?.toLocaleString('zh-CN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }) || '0.00'
  )
}

// 获取图表颜色
const chartColorMap: Record<ErpStatusKey, string> = {
  READY_TO_SHIP: '#faad14',
  SHIPPED: '#1890ff',
  ARRIVED_AT_PLATFORM_WAREHOUSE: '#722ed1',
  DELIVERED: '#52c41a',
  CANCELED: '#ff4d4f',
  RETURNED: '#8c8c8c'
}

const getChartColor = (status: string): string => {
  const statusKey = status as ErpStatusKey
  return chartColorMap[statusKey] || '#d9d9d9'
}

// ECharts 配置
const chartOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c}单 ({d}%)'
  },
  legend: {
    orient: 'horizontal',
    bottom: 0,
    left: 'center',
    itemGap: 20,
    textStyle: {
      fontSize: 12,
      color: '#595959'
    }
  },
  series: [
    {
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        formatter: '{d}%',
        fontSize: 12,
        color: '#595959'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold'
        },
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.3)'
        }
      },
      data: tableData.value.map(item => ({
        name: getStatusName(item.status),
        value: item.count,
        itemStyle: {
          color: getChartColor(item.status)
        }
      }))
    }
  ]
}))
</script>

<style scoped>
.dashboard-card {
  height: 100%;
  border-radius: 12px;
}

.dashboard-card :deep(.ant-card-head) {
  min-height: auto;
  padding: 16px 20px;
  border-bottom: 1px solid var(--ant-color-border-secondary);
}

.dashboard-card :deep(.ant-card-body) {
  padding: 20px;
}

.card-icon {
  color: var(--ant-color-primary);
  font-size: 18px;
}

.order-status-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chart-container {
  width: 100%;
  height: 300px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.chart-container .chart {
  width: 100%;
  height: 100%;
}

/* 表格样式 */
:deep(.ant-table-thead > tr > th) {
  border-bottom: 2px solid var(--ant-color-border-secondary);
}

:deep(.ant-table-tbody > tr.even-row > td) {
  background: var(--ant-color-fill-quaternary);
}

:deep(.ant-table-tbody > tr.odd-row > td) {
  background: var(--ant-color-bg-container);
}

:deep(.ant-table-tbody > tr:hover > td) {
  background: var(--ant-control-item-bg-active-hover) !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid var(--ant-color-border-secondary);
}

:deep(.ant-table-summary) {
  background: var(--ant-color-fill-quaternary);
}

.summary-row .summary-label {
  color: var(--ant-color-text);
  font-weight: 600;
}

.summary-row .summary-value {
  color: var(--ant-color-primary);
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  font-weight: 600;
}

.count-value,
.amount-value {
  font-weight: 600;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
}
</style>
