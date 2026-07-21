<template>
  <a-card class="dashboard-card">
    <template #title>
      <div class="card-title">
        <BarChartOutlined class="card-icon" />
        <span>吞吐趋势</span>
        <span class="title-hint">（单据数）</span>
      </div>
    </template>
    <div v-if="hasData" class="chart-container">
      <v-chart :option="chartOption" autoresize class="chart" />
    </div>
    <div v-else class="empty-state">
      <a-empty description="暂无数据" />
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { BarChartOutlined } from '@ant-design/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import {
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent
} from 'echarts/components'
import type { EChartsOption } from 'echarts'
import type { ThroughputTrendVO } from '@/api/platform-dashboard/types'

use([
  CanvasRenderer,
  BarChart,
  LineChart,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent
])

const props = defineProps<{ data?: ThroughputTrendVO }>()

const hasData = computed(() => (props.data?.dates?.length ?? 0) > 0)

// 只显示 月-日
function shortDate(d: string): string {
  const parts = d.split('-')
  return parts.length >= 3 ? `${parts[1]}-${parts[2]}` : d
}

const chartOption = computed<EChartsOption>(() => {
  const d = props.data
  if (!d || !d.dates.length) return {}
  const dates = d.dates.map(shortDate)
  const showZoom = d.dates.length > 15
  const start = showZoom ? ((d.dates.length - 15) / d.dates.length) * 100 : 0

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['入库单', '出库单', '退货单'],
      top: 8,
      right: 16,
      itemWidth: 14,
      itemHeight: 14,
      textStyle: { fontSize: 12, color: '#595959' }
    },
    grid: {
      left: 48,
      right: 24,
      top: 48,
      bottom: showZoom ? 70 : 40,
      containLabel: false
    },
    dataZoom: showZoom
      ? [
          { type: 'slider', xAxisIndex: 0, start, end: 100, height: 22, bottom: 8 },
          { type: 'inside', xAxisIndex: 0, start, end: 100 }
        ]
      : undefined,
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        fontSize: 12,
        color: '#8c8c8c',
        rotate: dates.length > 12 ? 45 : 0,
        interval: dates.length > 20 ? 'auto' : 0
      },
      axisLine: { lineStyle: { color: '#d9d9d9' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      name: '单据数',
      nameTextStyle: { fontSize: 12, color: '#8c8c8c' },
      axisLabel: { fontSize: 12, color: '#8c8c8c' },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    series: [
      {
        name: '入库单',
        type: 'bar',
        data: d.inbound,
        itemStyle: { color: '#1890ff', borderRadius: [3, 3, 0, 0] },
        barMaxWidth: 22
      },
      {
        name: '出库单',
        type: 'bar',
        data: d.outbound,
        itemStyle: { color: '#52c41a', borderRadius: [3, 3, 0, 0] },
        barMaxWidth: 22
      },
      {
        name: '退货单',
        type: 'line',
        data: d.returns,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: { color: '#fa8c16' },
        lineStyle: { color: '#fa8c16', width: 2 }
      }
    ]
  }
})
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

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-icon {
  color: var(--ant-color-primary);
  font-size: 18px;
}

.title-hint {
  font-size: 12px;
  color: var(--ant-color-text-tertiary);
  font-weight: 400;
}

.chart-container {
  width: 100%;
  height: 360px;
}

.chart-container .chart {
  width: 100%;
  height: 100%;
}

.empty-state {
  width: 100%;
  height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
