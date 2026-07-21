import { theme } from 'ant-design-vue'
import type { ForecastStatus } from '@/api/wms/inventory-forecast/types'

/** 状态标签配置 */
const STATUS_LABELS: Record<ForecastStatus, string> = {
  SUFFICIENT: '充足',
  LOW: '偏低',
  CRITICAL: '告急',
  STOCKOUT: '断货',
  NO_SALES: '待观察'
}

/**
 * 库存预测状态相关逻辑
 */
export function useForecastStatus() {
  const { token } = theme.useToken()

  /**
   * 获取状态标签文本
   */
  function getStatusLabel(status: ForecastStatus): string {
    return STATUS_LABELS[status] || status
  }

  /**
   * 格式化可售天数显示
   * @param sellableDays 可售天数（首次 stock ≤ 0）
   * @param status 库存状态
   * @param forecastDays 预测周期（用于判断是否超出）
   */
  function formatSellableDays(
    sellableDays: number | null,
    status: ForecastStatus,
    forecastDays: number = 30
  ): string {
    if (status === 'STOCKOUT') return '已断货'
    if (status === 'NO_SALES') return '待观察'
    if (sellableDays === null) return '—'
    if (sellableDays >= forecastDays) return `>${forecastDays}天`
    return `${sellableDays}天`
  }

  /**
   * 获取状态对应的语义色（用于 JS/ECharts）
   */
  function getStatusColor(status: ForecastStatus): string {
    const colorMap: Record<ForecastStatus, string> = {
      SUFFICIENT: token.value.colorSuccess,
      LOW: token.value.colorWarning,
      CRITICAL: '#fa541c', // volcano
      STOCKOUT: token.value.colorError,
      NO_SALES: token.value.colorTextQuaternary
    }
    return colorMap[status] || token.value.colorTextSecondary
  }

  /**
   * 获取状态对应的 Tag 颜色名（用于 a-tag :color）
   */
  function getStatusTagColor(status: ForecastStatus): string {
    const colorMap: Record<ForecastStatus, string> = {
      SUFFICIENT: 'green',
      LOW: 'orange',
      CRITICAL: 'volcano',
      STOCKOUT: 'red',
      NO_SALES: 'default'
    }
    return colorMap[status] || 'default'
  }

  /**
   * 获取状态对应的背景色（用于 CSS）
   */
  function getStatusBgColor(status: ForecastStatus): string {
    const colorMap: Record<ForecastStatus, string> = {
      SUFFICIENT: token.value.colorSuccessBg,
      LOW: token.value.colorWarningBg,
      CRITICAL: '#fff2e8', // volcano-1
      STOCKOUT: token.value.colorErrorBg,
      NO_SALES: token.value.colorFillQuaternary
    }
    return colorMap[status] || token.value.colorFillQuaternary
  }

  /**
   * 根据可售天数和阈值判断 Tag 颜色
   */
  function getSellableDaysTagColor(days: number | null, threshold: number): string {
    if (days === null) return 'default' // NO_SALES
    if (days <= 0) return 'red' // STOCKOUT
    if (days <= threshold) return 'volcano' // CRITICAL
    if (days <= threshold * 2) return 'orange' // LOW
    return 'green' // SUFFICIENT
  }

  return {
    getStatusLabel,
    formatSellableDays,
    getStatusColor,
    getStatusTagColor,
    getStatusBgColor,
    getSellableDaysTagColor
  }
}
