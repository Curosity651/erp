<template>
  <a-card class="dashboard-card">
    <template #title>
      <div class="card-title">
        <TrophyOutlined class="card-icon" />
        <span>{{ title }}</span>
        <span class="title-hint">({{ unit }})</span>
      </div>
    </template>
    <div v-if="hasData" class="chart-container">
      <v-chart :option="chartOption" autoresize class="chart" />
    </div>
    <div v-else class="empty-state">
      <a-empty :description="t('platform.dashboard.noData')" />
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { TrophyOutlined } from '@ant-design/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { TooltipComponent, GridComponent } from 'echarts/components'
import type { EChartsOption } from 'echarts'
import type { RankingItemVO } from '@/api/platform-dashboard/types'

use([CanvasRenderer, BarChart, TooltipComponent, GridComponent])

const props = defineProps<{
  title: string
  data?: RankingItemVO[]
  unit: string
  color?: string
}>()
const { t, locale } = useI18n()

const hasData = computed(() => (props.data?.length ?? 0) > 0)

const chartOption = computed<EChartsOption>(() => {
  const list = (props.data || []).slice(0, 10)
  // 横向条形从上到下降序：echarts y 轴 category 需反转
  const ordered = [...list].reverse()
  const barColor = props.color || '#1890ff'
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const p = params[0]
        return `${p.name}<br/><b>${p.value.toLocaleString(locale.value)}</b> ${props.unit}`
      }
    },
    grid: { left: 8, right: 40, top: 10, bottom: 10, containLabel: true },
    xAxis: {
      type: 'value',
      axisLabel: { fontSize: 12, color: '#8c8c8c' },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    yAxis: {
      type: 'category',
      data: ordered.map(o => o.operatorName),
      axisLabel: { fontSize: 12, color: '#595959' },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [
      {
        type: 'bar',
        data: ordered.map(o => o.qty),
        barMaxWidth: 18,
        itemStyle: { color: barColor, borderRadius: [0, 4, 4, 0] },
        label: {
          show: true,
          position: 'right',
          fontSize: 12,
          color: '#8c8c8c',
          formatter: (p: any) => p.value.toLocaleString(locale.value)
        }
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
