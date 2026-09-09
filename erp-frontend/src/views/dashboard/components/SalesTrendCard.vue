<template>
  <a-card class="dashboard-card">
    <template #title>
      <div class="flex items-center gap-2">
        <LineChartOutlined class="card-icon" />
        <span>{{ t('dashboard.salesTrend') }}</span>
      </div>
    </template>
    <div v-if="chartData.length > 0" class="chart-container">
      <v-chart :option="chartOption" autoresize class="chart" />
    </div>
    <div v-else class="empty-state">
      <a-empty :description="t('dashboard.noData')" />
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { LineChartOutlined } from '@ant-design/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent
} from 'echarts/components'
import type { EChartsOption } from 'echarts'
import type { SalesTrendVO } from '@/api/dashboard/types'

use([
  CanvasRenderer,
  BarChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent
])

interface Props {
  data?: SalesTrendVO
}

const props = defineProps<Props>()
const { t, locale } = useI18n()

const chartData = computed(() => props.data?.data || [])

// 格式化时间显示
const formatTime = (time: string) => {
  if (time.includes(':')) {
    // 小时格式，直接返回
    return time
  } else {
    // 日期格式，只显示月-日
    const parts = time.split('-')
    return parts.length >= 3 ? `${parts[1]}-${parts[2]}` : time
  }
}

// ECharts 配置
const chartOption = computed<EChartsOption>(() => {
  if (!chartData.value || chartData.value.length === 0) {
    return {}
  }

  const times = chartData.value.map(item => formatTime(item.time))
  const totalSales = chartData.value.map(item => item.totalSales)
  const effectiveSales = chartData.value.map(item => item.effectiveSales)

  // 根据数据量决定默认显示范围
  const dataLength = chartData.value.length
  let startValue = 0
  let endValue = 100

  if (dataLength > 30) {
    // 超过30个数据点，默认显示最后30个
    startValue = Math.max(0, ((dataLength - 30) / dataLength) * 100)
    endValue = 100
  }

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        crossStyle: {
          color: '#999'
        }
      },
      formatter: (params: any) => {
        const dataIndex = params[0].dataIndex
        const originalTime = chartData.value[dataIndex].time
        let result = `<div style="font-weight: 600; margin-bottom: 8px;">${originalTime}</div>`

        params.forEach((item: any) => {
          const color = item.color
          const marker =
            item.seriesType === 'bar'
              ? `<span style="display: inline-block; width: 10px; height: 10px; background: ${color}; margin-right: 8px;"></span>`
              : `<span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${color}; margin-right: 8px;"></span>`

          result += `
						<div style="display: flex; justify-content: space-between; align-items: center; margin: 4px 0;">
							<span>
								${marker}
								${item.seriesName}
							</span>
          <span style="font-weight: 600; margin-left: 20px;">₽${item.value.toLocaleString(locale.value, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
              })}</span>
						</div>
					`
        })

        return result
      }
    },
    legend: {
      data: [t('dashboard.totalSales'), t('dashboard.deliveredSales')],
      top: 10,
      right: 20,
      itemWidth: 14,
      itemHeight: 14,
      textStyle: {
        fontSize: 12,
        color: '#595959'
      }
    },
    grid: {
      left: 60,
      right: 40,
      top: 50,
      bottom: dataLength > 10 ? 80 : 40,
      containLabel: false
    },
    dataZoom: [
      {
        type: 'slider',
        show: dataLength > 10,
        xAxisIndex: 0,
        start: startValue,
        end: endValue,
        height: 24,
        bottom: 10,
        borderColor: '#d9d9d9',
        fillerColor: 'rgba(24, 144, 255, 0.15)',
        handleStyle: {
          color: '#1890ff',
          borderColor: '#1890ff'
        },
        moveHandleStyle: {
          color: '#1890ff'
        },
        textStyle: {
          color: '#8c8c8c',
          fontSize: 11
        },
        dataBackground: {
          lineStyle: {
            color: '#d9d9d9',
            width: 1
          },
          areaStyle: {
            color: 'rgba(24, 144, 255, 0.1)'
          }
        },
        selectedDataBackground: {
          lineStyle: {
            color: '#1890ff',
            width: 1
          },
          areaStyle: {
            color: 'rgba(24, 144, 255, 0.2)'
          }
        }
      },
      {
        type: 'inside',
        xAxisIndex: 0,
        start: startValue,
        end: endValue,
        zoomOnMouseWheel: true,
        moveOnMouseMove: true,
        moveOnMouseWheel: false
      }
    ],
    xAxis: {
      type: 'category',
      data: times,
      axisLabel: {
        fontSize: 12,
        color: '#8c8c8c',
        rotate: times.length > 10 ? 45 : 0,
        interval: times.length > 20 ? 'auto' : 0
      },
      axisLine: {
        lineStyle: {
          color: '#d9d9d9'
        }
      },
      axisTick: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      name: t('dashboard.salesRub'),
      nameTextStyle: {
        fontSize: 12,
        color: '#8c8c8c',
        padding: [0, 0, 0, 0]
      },
      axisLabel: {
        fontSize: 12,
        color: '#8c8c8c',
        formatter: (value: number) => {
          if (value >= 10000) {
            return t('dashboard.tenThousand', { value: (value / 10000).toFixed(1) })
          }
          return value.toLocaleString()
        }
      },
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      splitLine: {
        lineStyle: {
          color: '#f0f0f0',
          type: 'dashed'
        }
      }
    },
    series: [
      {
        name: t('dashboard.totalSales'),
        type: 'bar',
        data: totalSales,
        itemStyle: {
          color: '#d9d9d9',
          opacity: 0.6
        },
        emphasis: {
          itemStyle: {
            color: '#bfbfbf'
          }
        },
        barWidth: '60%',
        z: 1
      },
      {
        name: t('dashboard.deliveredSales'),
        type: 'line',
        data: effectiveSales,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: {
          color: '#1890ff'
        },
        lineStyle: {
          color: '#1890ff',
          width: 3
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: 'rgba(24, 144, 255, 0.3)'
              },
              {
                offset: 1,
                color: 'rgba(24, 144, 255, 0.05)'
              }
            ]
          }
        },
        emphasis: {
          itemStyle: {
            color: '#40a9ff',
            borderColor: '#fff',
            borderWidth: 2
          }
        },
        z: 2
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

.card-icon {
  color: var(--ant-color-primary);
  font-size: 18px;
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
