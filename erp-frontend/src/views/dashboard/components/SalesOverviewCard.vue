<template>
  <div class="sales-overview-card">
    <div class="card-header">
      <div class="header-left">
        <LineChartOutlined class="header-icon" />
        <span class="header-title">{{ cardTitle }}</span>
      </div>
    </div>
    <div class="card-content">
      <div class="kpi-value">
        <span class="currency">₽</span>
        <span class="amount">{{ formatAmount(data?.totalSales) }}</span>
      </div>
      <div v-if="dateRangeText" class="date-range">{{ dateRangeText }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { LineChartOutlined } from '@ant-design/icons-vue'

interface Props {
  data?: {
    totalSales: number
  }
  serverTime?: string
  quickTimeRange?: string
  dateRange?: { start: string; end: string }
}

const props = defineProps<Props>()

// 动态标题
const cardTitle = computed(() => {
  switch (props.quickTimeRange) {
    case 'today':
      return '今日销售额'
    case 'yesterday':
      return '昨日销售额'
    case 'last7days':
      return '最近7天销售额'
    case 'thisMonth':
      return '本月销售额'
    case 'lastMonth':
      return '上月销售额'
    default:
      return '销售额'
  }
})

// 日期范围显示
const dateRangeText = computed(() => {
  if (!props.dateRange) return ''
  const { start, end } = props.dateRange
  if (start === end) {
    return start
  }
  return `${start} ~ ${end}`
})

// 格式化金额
const formatAmount = (amount?: number) => {
  if (!amount) return '0'
  // 大额数字使用万/亿单位
  if (amount >= 100000000) {
    return (amount / 100000000).toFixed(2) + '亿'
  }
  if (amount >= 10000) {
    return (amount / 10000).toFixed(2) + '万'
  }
  return amount.toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  })
}
</script>

<style scoped>
.sales-overview-card {
  background: linear-gradient(135deg, #1890ff 0%, #0958d9 100%);
  border-radius: 12px;
  padding: 20px;
  color: #ffffff;
  position: relative;
  overflow: hidden;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
  transition: all 0.3s ease;
}

.sales-overview-card:hover {
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.4);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 18px;
  opacity: 0.9;
}

.header-title {
  font-size: 14px;
  font-weight: 500;
  opacity: 0.95;
}

.header-time {
  font-size: 12px;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  opacity: 0.85;
  white-space: nowrap;
  letter-spacing: 0.3px;
}

.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.kpi-value {
  font-size: clamp(28px, 5vw, 48px);
  font-weight: 700;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  line-height: 1.2;
  display: flex;
  align-items: baseline;
  white-space: nowrap;
  overflow: hidden;
}

.kpi-value .currency {
  font-size: clamp(20px, 3.5vw, 32px);
  margin-right: 4px;
  flex-shrink: 0;
}

.kpi-value .amount {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.date-range {
  font-size: 13px;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  opacity: 0.85;
  color: #ffffff;
}

/* 响应式 */
@media (max-width: 1200px) {
  .header-time {
    font-size: 11px;
  }

  .kpi-value {
    font-size: clamp(24px, 4vw, 36px);
  }

  .kpi-value .currency {
    font-size: clamp(18px, 3vw, 24px);
  }
}

@media (max-width: 767px) {
  .sales-overview-card {
    padding: 20px;
    min-height: 180px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 12px;
  }

  .header-time {
    font-size: 10px;
  }

  .kpi-value {
    font-size: 32px;
  }

  .kpi-value .currency {
    font-size: 20px;
  }
}
</style>
