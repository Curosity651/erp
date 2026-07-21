<template>
  <div class="exchange-rate-card-wrapper">
    <div class="card-header">
      <div class="header-left">
        <GlobalOutlined class="header-icon" />
        <span class="header-title">汇率信息</span>
      </div>
      <div v-if="data?.date" class="header-date">{{ data.date }}</div>
    </div>
    <div class="exchange-rate-card">
      <div v-if="ratesWithReverse.length > 0" class="rates-grid">
        <div v-for="rate in ratesWithReverse" :key="rate.currency" class="rate-card">
          <div class="rate-currency">
            <span class="currency-symbol">{{ rate.currencySymbol }}</span>
            <span class="currency-code">{{ rate.currency }}</span>
          </div>
          <div class="rate-forward">→¥ {{ formatRate(rate.forwardRate) }}</div>
          <div class="rate-reverse">¥→ {{ formatRate(rate.reverseRate) }}</div>
        </div>
      </div>
      <a-empty v-else description="暂无汇率数据" :image="simpleImage" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { GlobalOutlined } from '@ant-design/icons-vue'
import { Empty } from 'ant-design-vue'
import { getCurrencySymbol } from '@/utils/currency-utils'

interface Props {
  data?: {
    date: string
    rates: Array<{
      currency: string
      rate: number
    }>
  }
}

const props = defineProps<Props>()

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

// 扩展汇率数据，添加反向汇率
const ratesWithReverse = computed(() => {
  if (!props.data?.rates) return []

  return props.data.rates.map(rate => {
    try {
      return {
        currency: rate.currency,
        currencySymbol: getCurrencySymbol(rate.currency),
        forwardRate: rate.rate, // 目标币种 → CNY
        reverseRate: rate.rate && rate.rate !== 0 ? 1 / rate.rate : 0 // CNY → 目标币种
      }
    } catch (error) {
      console.error('汇率计算失败:', error)
      return {
        currency: rate.currency,
        currencySymbol: getCurrencySymbol(rate.currency),
        forwardRate: rate.rate,
        reverseRate: 0
      }
    }
  })
})

// 格式化汇率
const formatRate = (rate?: number) => {
  if (!rate) return '0.0000'
  return rate.toFixed(4)
}
</script>

<style scoped>
.exchange-rate-card-wrapper {
  background: var(--ant-color-bg-container);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  height: 100%;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
}

.exchange-rate-card-wrapper:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
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
  color: var(--ant-color-primary);
}

.header-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--ant-color-text);
}

.header-date {
  font-size: 12px;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  color: var(--ant-color-text-tertiary);
  white-space: nowrap;
}

.exchange-rate-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.rates-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 12px;
}

.rate-card {
  background: var(--ant-color-fill-quaternary);
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 6px;
  padding: 12px 8px;
  text-align: center;
  transition: all 0.3s ease;
}

.rate-card:hover {
  background: #e6f7ff;
  border-color: var(--ant-color-primary);
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
}

.rate-currency {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  margin-bottom: 8px;
}

.currency-symbol {
  font-size: 20px;
  font-weight: 700;
  color: var(--ant-color-text);
  line-height: 1;
}

.currency-code {
  font-size: 11px;
  color: var(--ant-color-text-tertiary);
  font-weight: 500;
}

.rate-forward {
  font-size: 13px;
  color: var(--ant-color-primary);
  font-weight: 500;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  margin-bottom: 4px;
  line-height: 1.4;
}

.rate-reverse {
  font-size: 12px;
  color: var(--ant-color-text-secondary);
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  line-height: 1.4;
}

/* 响应式 */
@media (max-width: 1000px) {
  .rates-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 767px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 12px;
  }

  .header-date {
    font-size: 11px;
  }

  .rates-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .rate-card {
    padding: 10px 6px;
  }

  .rate-currency {
    margin-bottom: 6px;
  }

  .currency-symbol {
    font-size: 18px;
  }

  .currency-code {
    font-size: 10px;
  }

  .rate-forward {
    font-size: 12px;
    margin-bottom: 3px;
  }

  .rate-reverse {
    font-size: 11px;
  }
}
</style>
