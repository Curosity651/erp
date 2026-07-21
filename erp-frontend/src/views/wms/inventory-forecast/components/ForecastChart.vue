<template>
  <div class="forecast-chart-container">
    <v-chart v-if="forecastList.length > 0" :option="chartOption" autoresize class="chart" />
    <a-empty v-else description="暂无预测数据" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { theme } from 'ant-design-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import {
  TooltipComponent,
  GridComponent,
  MarkLineComponent,
  DataZoomComponent
} from 'echarts/components'
import type { EChartsOption } from 'echarts'
import type { ForecastDayVO } from '@/api/wms/inventory-forecast/types'
import { formatDateChinese } from '@/utils/date'
import { buildXAxisLabels } from '../utils/chart-builders'
import { STATIC_CHART_CONFIG } from '../utils/chart-types'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  TooltipComponent,
  GridComponent,
  MarkLineComponent,
  DataZoomComponent
])

defineOptions({ name: 'ForecastChart' })

const { token } = theme.useToken()

interface Props {
  /** 预测数据列表 */
  forecastList: ForecastDayVO[]
  /** 安全库存数量 */
  safetyStock: number
  /** 有效安全库存数量（动态计算） */
  effectiveSafetyStock?: number
  /** 图表高度，默认 400px */
  height?: number | string
  /** 是否显示数据缩放控件 */
  showDataZoom?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  height: 400,
  showDataZoom: true
})

// 静态配置（不变，避免重复序列化）
const staticOption = computed(() => ({
  ...STATIC_CHART_CONFIG,
  legend: {
    data: ['期末库存', '预计入库'],
    top: 0,
    textStyle: {
      fontSize: 12,
      color: token.value.colorTextSecondary
    }
  }
}))

const chartOption = computed<EChartsOption>(() => {
  if (!props.forecastList || props.forecastList.length === 0) {
    return {}
  }

  // 使用工具函数构建配置
  const xAxisLabels = buildXAxisLabels(props.forecastList)

  const closingStocks = props.forecastList.map(d => d.closingStock)
  const incomingData = props.forecastList.map(d => d.incoming || 0)

  // 计算 Y 轴最大值：考虑库存数据、入库数据、安全库存线
  const dataMax = Math.max(
    ...closingStocks,
    ...incomingData,
    props.effectiveSafetyStock ?? props.safetyStock
  )
  const yAxisMax = Math.ceil(dataMax * 1.2) // 留 20% 空间

  return {
    ...staticOption.value,
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e8e8e8',
      borderWidth: 1,
      padding: [12, 16],
      textStyle: {
        color: '#262626',
        fontSize: 13
      },
      formatter: (params: any) => {
        const dataIndex = params[0].dataIndex
        const item = props.forecastList[dataIndex]

        // 日期格式化
        const dateStr = formatDateChinese(item.date)

        return `
          <div style="min-width: 200px;">
            <div style="font-weight: 600; margin-bottom: 8px; font-size: 14px;">
              📅 ${dateStr}
            </div>
            <div style="border-top: 1px solid #f0f0f0; padding-top: 8px; padding-bottom: 8px;">
              <div style="font-size: 16px; font-weight: 600; color: ${token.value.colorPrimary}; margin-bottom: 4px;">
                📦 期末库存：${item.closingStock}件
              </div>
            </div>
            <div style="border-top: 1px dashed #d9d9d9; padding-top: 8px; color: #8c8c8c; font-size: 12px;">
              <span style="color: ${token.value.colorSuccess};">入库 ${item.incoming} 件</span> | 销量 ${item.sales} 件
            </div>
          </div>
        `
      }
    },
    xAxis: {
      type: 'category',
      data: xAxisLabels,
      boundaryGap: true,
      axisLabel: {
        fontSize: 11,
        color: '#8c8c8c',
        rotate: 45,
        interval: 2
      },
      axisLine: { lineStyle: { color: '#d9d9d9' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      name: '库存数量',
      max: yAxisMax, // 确保包含安全库存线
      nameTextStyle: {
        fontSize: 12,
        color: '#8c8c8c'
      },
      axisLabel: {
        fontSize: 12,
        color: '#8c8c8c'
      },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: {
        lineStyle: {
          color: '#fafafa',
          type: 'dashed'
        }
      }
    },
    dataZoom: props.showDataZoom
      ? [
          {
            type: 'slider',
            show: true,
            xAxisIndex: [0],
            start: 0,
            end: 100,
            height: 20,
            bottom: 10,
            borderColor: 'transparent',
            backgroundColor: '#f5f5f5',
            fillerColor: 'rgba(24, 144, 255, 0.2)',
            handleStyle: { color: token.value.colorPrimary },
            textStyle: {
              fontSize: 11,
              color: '#8c8c8c'
            }
          }
        ]
      : undefined,
    series: [
      // Series 1: 库存曲线（主角）
      {
        type: 'line',
        name: '期末库存',
        data: closingStocks,
        lineStyle: {
          width: 2,
          color: token.value.colorPrimary // 使用 antd token 实际颜色值
        },
        symbol: 'none', // 不显示数据点，保持线条流畅
        emphasis: {
          disabled: false,
          lineStyle: {
            width: 2,
            color: token.value.colorPrimary
          }
        },
        markLine: {
          silent: true,
          symbol: 'none',
          data: [
            // 安全库存线(金色粗虚线)
            {
              yAxis: props.effectiveSafetyStock ?? props.safetyStock,
              lineStyle: {
                color: '#faad14',
                type: 'dashed',
                width: 2
              },
              label: {
                show: true,
                position: 'insideEndTop',
                formatter: `⚠ 安全水位: ${props.effectiveSafetyStock ?? props.safetyStock}`,
                color: '#faad14',
                fontSize: 12,
                fontWeight: 'bold'
              }
            }
          ]
        },
        z: 3 // 确保曲线在色块上方
      },
      // Series 2: 入库柱状图
      {
        type: 'bar',
        name: '预计入库',
        data: incomingData,
        barWidth: '40%',
        itemStyle: {
          color: token.value.colorSuccess, // 使用 antd token 实际颜色值
          borderRadius: [2, 2, 0, 0]
        },
        z: 2
      }
    ]
  }
})
</script>

<style scoped>
.forecast-chart-container {
  width: 100%;
  height: 360px;
}

.chart {
  width: 100%;
  height: 100%;
}
</style>
