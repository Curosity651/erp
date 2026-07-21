/**
 * 图表配置常量
 */

// 颜色系统（基于 antd 语义色）
export const CHART_COLORS = {
  // 主体元素
  inventory: 'var(--ant-color-primary)',           // 库存曲线：品牌蓝
  incoming: 'var(--ant-color-success)',            // 入库柱：成功绿
  sales: 'var(--ant-color-text-tertiary)',         // 销量线：三级文本灰
  safety: 'var(--ant-color-text-quaternary)',      // 安全线：四级文本灰
  stockout: 'var(--ant-color-error)',              // 断货线：错误红
} as const

// 预警色块（极低透明度，分层递增）
export const ZONE_COLORS = {
  LOW: 'rgba(250, 173, 20, 0.05)',       // 金色，5%透明度
  CRITICAL: 'rgba(255, 77, 79, 0.08)',   // 红色，8%透明度
  STOCKOUT: 'rgba(255, 77, 79, 0.12)',   // 红色，12%透明度
} as const

// ECharts 静态配置（不变部分）
export const STATIC_CHART_CONFIG = {
  grid: {
    left: '3%',
    right: '4%',
    bottom: '15%',
    top: '10%',
    containLabel: true
  },
  tooltip: {
    trigger: 'axis' as const,
    axisPointer: { type: 'cross' as const },
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderColor: '#e8e8e8',
    borderWidth: 1,
    padding: [12, 16],
    textStyle: {
      color: '#262626',
      fontSize: 13
    }
  }
} as const
