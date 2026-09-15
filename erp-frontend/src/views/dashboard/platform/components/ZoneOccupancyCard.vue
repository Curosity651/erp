<template>
  <a-card class="dashboard-card">
    <template #title>
      <div class="card-title">
        <PieChartOutlined class="card-icon" />
        <span>{{ t('platform.dashboard.zone.title') }}</span>
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
import { PieChartOutlined } from '@ant-design/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import type { EChartsOption } from 'echarts'
import type { ZoneOccupancyVO, ZoneType } from '@/api/platform-dashboard/types'

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])

const props = defineProps<{ data?: ZoneOccupancyVO[] }>()
const { t, locale } = useI18n()

// 分区语义配色（与库位网格图例一致：标准蓝/不良红/退货绿/暂存灰）
const ZONE_META: Record<ZoneType, { nameKey: string; color: string }> = {
  STANDARD: { nameKey: 'platform.dashboard.zone.standard', color: '#1890ff' },
  DEFECTIVE: { nameKey: 'platform.dashboard.zone.defective', color: '#ff4d4f' },
  RETURN: { nameKey: 'platform.dashboard.zone.return', color: '#52c41a' },
  TEMP: { nameKey: 'platform.dashboard.zone.temp', color: '#8c8c8c' }
}

const hasData = computed(() => (props.data || []).some(z => z.invQty > 0))

const chartOption = computed<EChartsOption>(() => {
  const list = props.data || []
  const byZone = new Map(list.map(z => [z.zone, z]))
  return {
    tooltip: {
      trigger: 'item',
      formatter: (p: any) => {
        const z = byZone.get(p.data.zone as ZoneType)
        const inventory = t('platform.dashboard.zone.inventoryUnits', {
          count: (z?.invQty ?? 0).toLocaleString(locale.value),
          percent: p.percent
        })
        const locations = t('platform.dashboard.zone.locationCount', {
          count: (z?.locationCount ?? 0).toLocaleString(locale.value)
        })
        return `<div style="font-weight:600;margin-bottom:4px;">${p.name}</div>${inventory}<br/>${locations}`
      }
    },
    legend: {
      bottom: 6,
      itemWidth: 12,
      itemHeight: 12,
      textStyle: { fontSize: 12, color: '#595959' }
    },
    series: [
      {
        name: t('platform.dashboard.zone.title'),
        type: 'pie',
        radius: ['42%', '66%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{d}%', fontSize: 12, color: '#595959' },
        data: list.map(z => ({
          name: t(ZONE_META[z.zone].nameKey),
          value: z.invQty,
          zone: z.zone,
          itemStyle: { color: ZONE_META[z.zone].color }
        }))
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

.chart-container {
  width: 100%;
  height: 340px;
}

.chart-container .chart {
  width: 100%;
  height: 100%;
}

.empty-state {
  width: 100%;
  height: 340px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
