<template>
  <div class="forecast-status-cards">
    <a-row :gutter="12">
      <a-col v-for="item in cardItems" :key="item.status" :span="item.span">
        <div
          class="status-card"
          :class="[
            `status-card--${item.status.toLowerCase().replace('_', '-')}`,
            { 'status-card--active': modelValue === item.status }
          ]"
          @click="handleClick(item.status)"
        >
          <a-statistic :value="item.count" :value-style="{ color: item.valueColor }">
            <template #title>
              <div class="status-card__title" :style="{ color: item.labelColor }">
                <component :is="item.icon" :size="14" />
                <span>{{ item.label }}</span>
              </div>
            </template>
          </a-statistic>
          <div class="status-card__threshold">{{ item.threshold }}</div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { theme } from 'ant-design-vue'
import type { ForecastStatus, ForecastStatusCounts } from '@/api/wms/inventory-forecast/types'
import {
  ShieldCheckIcon,
  TrendingDownIcon,
  AlertCircleIcon,
  PackageXIcon,
  BarChart2Icon
} from '@/components/Icon'

defineOptions({ name: 'ForecastStatusCards' })

const props = defineProps<{
  statusCounts: ForecastStatusCounts
  thresholdDays: number
}>()

const modelValue = defineModel<ForecastStatus | null>({ required: true })

const { token } = theme.useToken()

interface CardItem {
  status: ForecastStatus
  label: string
  count: number
  threshold: string
  icon: typeof ShieldCheckIcon
  span: number
  labelColor: string
  valueColor: string
}

const cardItems = computed<CardItem[]>(() => [
  {
    status: 'SUFFICIENT',
    label: '充足',
    count: props.statusCounts.SUFFICIENT,
    threshold: `>${props.thresholdDays * 2}天`,
    icon: ShieldCheckIcon,
    span: 5,
    labelColor: token.value.colorSuccess,
    valueColor: token.value.colorSuccess
  },
  {
    status: 'LOW',
    label: '偏低',
    count: props.statusCounts.LOW,
    threshold: `${props.thresholdDays + 1}~${props.thresholdDays * 2}天`,
    icon: TrendingDownIcon,
    span: 5,
    labelColor: token.value.colorWarning,
    valueColor: token.value.colorWarning
  },
  {
    status: 'CRITICAL',
    label: '告急',
    count: props.statusCounts.CRITICAL,
    threshold: `1~${props.thresholdDays}天`,
    icon: AlertCircleIcon,
    span: 5,
    labelColor: '#fa541c',
    valueColor: '#fa541c'
  },
  {
    status: 'STOCKOUT',
    label: '断货',
    count: props.statusCounts.STOCKOUT,
    threshold: '≤0天',
    icon: PackageXIcon,
    span: 5,
    labelColor: token.value.colorError,
    valueColor: token.value.colorError
  },
  {
    status: 'NO_SALES',
    label: '待观察',
    count: props.statusCounts.NO_SALES,
    threshold: '无销量',
    icon: BarChart2Icon,
    span: 4,
    labelColor: token.value.colorTextSecondary,
    valueColor: token.value.colorTextSecondary
  }
])

function handleClick(status: ForecastStatus) {
  // 再次点击已选中的卡片 = 取消筛选
  if (modelValue.value === status) {
    modelValue.value = null
  } else {
    modelValue.value = status
  }
}
</script>

<style scoped>
.forecast-status-cards {
  margin-bottom: 24px;
}

.status-card {
  padding: 16px;
  border-radius: var(--ant-border-radius-lg);
  cursor: pointer;
  text-align: center;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.status-card:hover {
  opacity: 0.85;
}

.status-card--active {
  border-color: var(--ant-color-primary);
}

/* 充足 - 绿色系 */
.status-card--sufficient {
  background: var(--ant-color-success-bg);
}

/* 偏低 - 橙色系 */
.status-card--low {
  background: var(--ant-color-warning-bg);
}

/* 告急 - volcano 色系 */
.status-card--critical {
  background: #fff2e8;
}

/* 断货 - 红色系 */
.status-card--stockout {
  background: var(--ant-color-error-bg);
}

/* 待观察 - 灰色系 */
.status-card--no-sales {
  background: var(--ant-color-fill-quaternary);
}

.status-card__title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 500;
}

.status-card__threshold {
  font-size: 12px;
  color: var(--ant-color-text-tertiary);
  margin-top: 4px;
}

/* 覆盖 a-statistic 默认样式 */
.status-card :deep(.ant-statistic-content) {
  font-size: 28px;
  font-weight: 600;
}

.status-card :deep(.ant-statistic-title) {
  margin-bottom: 4px;
}
</style>
