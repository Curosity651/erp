<template>
  <a-card class="dashboard-card">
    <template #title>
      <div class="flex items-center gap-2">
        <AppstoreOutlined class="card-icon" />
        <span>平台履约分布</span>
      </div>
    </template>
    <div class="platform-fulfillment-card">
      <!-- ECharts 堆叠柱状图 -->
      <div v-if="tableData.length > 0" class="chart-container">
        <v-chart :option="chartOption" autoresize class="chart" />
      </div>

      <!-- 数据表格 -->
      <a-table
        :columns="columns"
        :data-source="tableData"
        :pagination="false"
        size="small"
        :row-class-name="getRowClassName"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'platform'">
            <div class="platform-cell">
              <a-tag :color="getPlatformColor(record.platform)">
                {{ getPlatformName(record.platform) }}
              </a-tag>
            </div>
          </template>
          <template v-else-if="column.key === 'fbs'">
            <div class="data-cell">
              <div class="count">数量：{{ record.fbs.count.toLocaleString() }}</div>
              <div class="amount">₽{{ formatAmount(record.fbs.amount) }}</div>
            </div>
          </template>
          <template v-else-if="column.key === 'fbo'">
            <div class="data-cell">
              <div class="count">数量：{{ record.fbo.count.toLocaleString() }}</div>
              <div class="amount">₽{{ formatAmount(record.fbo.amount) }}</div>
            </div>
          </template>
          <template v-else-if="column.key === 'total'">
            <div class="data-cell total">
              <div class="count">数量：{{ record.total.count.toLocaleString() }}</div>
              <div class="amount">₽{{ formatAmount(record.total.amount) }}</div>
            </div>
          </template>
        </template>
      </a-table>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { AppstoreOutlined } from '@ant-design/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import type { EChartsOption } from 'echarts'
import type { PlatformFulfillmentVO } from '@/api/dashboard/types'
import { getPlatformLabel } from '@/components/Platform'

use([CanvasRenderer, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

interface Props {
  data?: PlatformFulfillmentVO[]
}

const props = defineProps<Props>()

const columns = [
  { title: '平台', key: 'platform', dataIndex: 'platform', width: 120, align: 'left' },
  { title: 'FBS', key: 'fbs', width: 200, align: 'right' },
  { title: 'FBO', key: 'fbo', width: 200, align: 'right' },
  { title: '合计', key: 'total', width: 200, align: 'right' }
]

const tableData = computed(() => props.data || [])

const platformColorMap: Record<string, string> = {
  wildberries: 'purple',
  ozon: 'blue'
}

const getPlatformName = (platform: string) => getPlatformLabel(platform)
const getPlatformColor = (platform: string) => platformColorMap[platform] || 'default'

const getRowClassName = (_record: any, index: number) => {
  return index % 2 === 0 ? 'even-row' : 'odd-row'
}

const formatAmount = (amount: number) => {
  return amount.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// ECharts 配置
const chartOption = computed<EChartsOption>(() => {
  if (!props.data || props.data.length === 0) {
    return {}
  }

  const platforms = props.data.map(item => getPlatformName(item.platform))
  const fbsAmounts = props.data.map(item => item.fbs.amount)
  const fboAmounts = props.data.map(item => item.fbo.amount)

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const platform = params[0].axisValue
        let result = `<div style="font-weight: 600; margin-bottom: 8px;">${platform}</div>`

        params.forEach((item: any) => {
          const color = item.color
          result += `
						<div style="display: flex; justify-content: space-between; align-items: center; margin: 4px 0;">
							<span>
								<span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${color}; margin-right: 8px;"></span>
								${item.seriesName}
							</span>
							<span style="font-weight: 600; margin-left: 20px;">₽${item.value.toLocaleString('zh-CN', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
              })}</span>
						</div>
					`
        })

        // 计算总计
        const total = params.reduce((sum: number, item: any) => sum + item.value, 0)
        result += `
					<div style="display: flex; justify-content: space-between; align-items: center; margin-top: 8px; padding-top: 8px; border-top: 1px solid #f0f0f0;">
						<span style="font-weight: 600;">合计</span>
						<span style="font-weight: 600; color: #1890ff;">₽${total.toLocaleString('zh-CN', {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2
            })}</span>
					</div>
				`

        return result
      }
    },
    legend: {
      data: ['FBS', 'FBO'],
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
      bottom: 40,
      containLabel: false
    },
    xAxis: {
      type: 'category',
      data: platforms,
      axisLabel: {
        fontSize: 12,
        color: '#8c8c8c'
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
      name: '金额（₽）',
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
            return `${(value / 10000).toFixed(1)}万`
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
        name: 'FBS',
        type: 'bar',
        stack: 'total',
        data: fbsAmounts,
        itemStyle: {
          color: '#52c41a',
          borderRadius: [0, 0, 0, 0]
        },
        emphasis: {
          itemStyle: {
            color: '#73d13d'
          }
        },
        barWidth: '50%'
      },
      {
        name: 'FBO',
        type: 'bar',
        stack: 'total',
        data: fboAmounts,
        itemStyle: {
          color: '#1890ff',
          borderRadius: [4, 4, 0, 0]
        },
        emphasis: {
          itemStyle: {
            color: '#40a9ff'
          }
        },
        barWidth: '50%'
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

.platform-fulfillment-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chart-container {
  width: 100%;
  height: 280px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.chart-container .chart {
  width: 100%;
  height: 100%;
}

/* 表格样式 - 只保留必要的自定义 */
:deep(.ant-table-thead > tr > th) {
  border-bottom: 2px solid var(--ant-color-border-secondary);
}

:deep(.ant-table-tbody > tr.even-row > td) {
  background: var(--ant-color-bg-container);
}

:deep(.ant-table-tbody > tr.odd-row > td) {
  background: var(--ant-color-fill-quaternary);
}

:deep(.ant-table-tbody > tr:hover > td) {
  background: var(--ant-control-item-bg-active-hover) !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid var(--ant-color-border-secondary);
}

.platform-cell {
  display: flex;
  align-items: center;
}

.data-cell .count {
  font-size: 12px;
  color: var(--ant-color-text);
  margin-bottom: 4px;
  font-weight: 500;
}

.data-cell .amount {
  font-size: 12px;
  font-weight: 600;
  color: var(--ant-color-text);
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
}

.data-cell.total .count,
.data-cell.total .amount {
  font-weight: 600;
  color: var(--ant-color-primary);
}
</style>
