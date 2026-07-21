/**
 * Dashboard 字体系统
 * 定义统一的字体族、字号、字重和行高
 */

export const typography = {
  fontFamily: {
    base: "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif",
    number: "'DIN Alternate', 'Helvetica Neue', Arial, sans-serif"
  },

  fontSize: {
    xs: '12px',
    sm: '14px',
    base: '14px',
    lg: '16px',
    xl: '20px',
    xxl: '24px',
    kpi: '48px',
    progress: '36px'
  },

  fontWeight: {
    normal: 400,
    medium: 500,
    semibold: 600,
    bold: 700
  },

  lineHeight: {
    tight: 1.2,
    base: 1.5,
    relaxed: 1.8
  }
}

export default typography
