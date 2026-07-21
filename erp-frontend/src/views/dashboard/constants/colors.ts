/**
 * Dashboard 色彩系统
 * 定义统一的颜色令牌，确保视觉一致性
 */

export const colors = {
  // 主色系
  primary: {
    base: '#1890ff',
    light: '#40a9ff',
    lighter: '#69c0ff',
    dark: '#096dd9',
    darker: '#0050b3'
  },

  // 语义色系
  success: {
    base: '#52c41a',
    light: '#73d13d',
    lighter: '#95de64',
    dark: '#389e0d'
  },

  warning: {
    base: '#faad14',
    light: '#ffc53d',
    lighter: '#ffd666',
    dark: '#d48806'
  },

  error: {
    base: '#ff4d4f',
    light: '#ff7875',
    lighter: '#ffa39e',
    dark: '#cf1322'
  },

  // 中性色系
  neutral: {
    text: {
      primary: '#262626',
      secondary: '#595959',
      tertiary: '#8c8c8c',
      disabled: '#bfbfbf'
    },
    border: {
      base: '#d9d9d9',
      light: '#f0f0f0'
    },
    background: {
      page: '#f5f7fa',
      card: '#ffffff',
      hover: '#fafafa',
      active: '#f5f5f5'
    }
  },

  // 渐变色系
  gradient: {
    primary: 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)',
    success: 'linear-gradient(135deg, #52c41a 0%, #73d13d 100%)',
    warning: 'linear-gradient(135deg, #faad14 0%, #ffc53d 100%)',
    gold: 'linear-gradient(135deg, #faad14 0%, #ffd666 100%)',
    silver: 'linear-gradient(135deg, #8c8c8c 0%, #bfbfbf 100%)',
    bronze: 'linear-gradient(135deg, #d4380d 0%, #ff7a45 100%)'
  }
}

export default colors
