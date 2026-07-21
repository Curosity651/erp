import type { MarkAreaComponentOption, MarkLineComponentOption } from 'echarts'
import type { ForecastDayVO } from '@/api/wms/inventory-forecast/types'
import { ZONE_COLORS, CHART_COLORS } from './chart-types'

/**
 * 构建预警区域色块（LOW/CRITICAL/STOCKOUT）
 *
 * 算法：遍历预测列表，将连续的同状态区域合并为一个色块
 *
 * @param forecastList 预测数据列表
 * @returns ECharts markArea 配置数据
 */
export function buildZoneMarkAreas(
  forecastList: ForecastDayVO[]
): MarkAreaComponentOption['data'] {
  const zones: Array<[
    { xAxis: string; itemStyle: { color: string } },
    { xAxis: string }
  ]> = []

  let currentZone: { status: string; start: string } | null = null

  forecastList.forEach((day, index) => {
    // 只标注 LOW/CRITICAL/STOCKOUT 区域
    const isWarningStatus = ['LOW', 'CRITICAL', 'STOCKOUT'].includes(day.status)

    if (isWarningStatus) {
      if (!currentZone || currentZone.status !== day.status) {
        // 状态变化，结束旧区域，开始新区域
        if (currentZone) {
          zones.push([
            {
              xAxis: currentZone.start,
              itemStyle: { color: getZoneColor(currentZone.status) }
            },
            { xAxis: forecastList[index - 1].date }
          ])
        }
        currentZone = { status: day.status, start: day.date }
      }
    } else {
      // 进入 SUFFICIENT/NO_SALES，结束当前区域
      if (currentZone) {
        zones.push([
          {
            xAxis: currentZone.start,
            itemStyle: { color: getZoneColor(currentZone.status) }
          },
          { xAxis: forecastList[index - 1].date }
        ])
        currentZone = null
      }
    }
  })

  // 处理末尾区域
  if (currentZone) {
    zones.push([
      {
        xAxis: currentZone.start,
        itemStyle: { color: getZoneColor(currentZone.status) }
      },
      { xAxis: forecastList[forecastList.length - 1].date }
    ])
  }

  return zones
}

/**
 * 构建断货日标注线
 *
 * @param forecastList 预测数据列表
 * @returns ECharts markLine 配置，如果无断货则返回 undefined
 */
export function buildStockoutMarkLine(
  forecastList: ForecastDayVO[]
): MarkLineComponentOption | undefined {
  const stockoutDay = forecastList.find(d => d.closingStock <= 0)
  if (!stockoutDay) return undefined

  return {
    silent: false,
    symbol: 'none',
    label: {
      position: 'end',
      formatter: `⚠ 断货: ${stockoutDay.date}`,
      fontSize: 12,
      fontWeight: 'bold',
      color: CHART_COLORS.stockout
    },
    lineStyle: {
      color: CHART_COLORS.stockout,
      type: 'solid',
      width: 2,
      shadowBlur: 4,
      shadowColor: 'rgba(255, 77, 79, 0.3)'
    },
    data: [{ xAxis: stockoutDay.date }],
    z: 4
  }
}

/**
 * 构建 X 轴日期标签
 *
 * 格式：2026-02-10 → 2/10
 *
 * @param forecastList 预测数据列表
 * @returns 格式化后的日期标签数组
 */
export function buildXAxisLabels(forecastList: ForecastDayVO[]): string[] {
  return forecastList.map(d => {
    const [year, month, day] = d.date.split('-')
    return `${parseInt(month)}/${parseInt(day)}`
  })
}

/**
 * 获取预警区域颜色
 * @internal
 */
function getZoneColor(status: string): string {
  const colorMap: Record<string, string> = {
    LOW: ZONE_COLORS.LOW,
    CRITICAL: ZONE_COLORS.CRITICAL,
    STOCKOUT: ZONE_COLORS.STOCKOUT
  }
  return colorMap[status] || 'transparent'
}
