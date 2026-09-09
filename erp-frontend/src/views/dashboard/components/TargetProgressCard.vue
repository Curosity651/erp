<template>
  <a-card class="dashboard-card">
    <template #title>
      <div class="flex items-center gap-2">
        <component :is="icon" class="card-icon" />
        <span>{{ title }}</span>
      </div>
    </template>
    <div class="target-progress-card">
      <div v-if="data?.hasTarget" class="progress-content">
        <div class="progress-percentage" :class="percentageClass">
          {{ formatPercent(data.progress) }}
        </div>
        <a-progress
          :percent="data.progress"
          :stroke-color="progressColor"
          :show-info="false"
          :stroke-width="12"
          class="progress-bar"
        />
        <div class="progress-detail">
          <span class="detail-label">{{ t('dashboard.target') }}</span>
          <span class="detail-value">₽{{ formatAmount(data.target) }}</span>
          <a-divider type="vertical" />
          <span class="detail-label">{{ t('dashboard.current') }}</span>
          <span class="detail-value">₽{{ formatAmount(data.current) }}</span>
        </div>
      </div>
      <a-empty v-else :description="t('dashboard.targetNotSet')" :image-style="{ height: '50px' }">
        <a-button type="primary" size="small" @click="goToTargetSetting">{{ t('dashboard.setTarget') }}</a-button>
      </a-empty>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { CalendarOutlined, TrophyOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const { t, locale } = useI18n()

interface Props {
  data?: {
    target: number
    current: number
    progress: number
    hasTarget: boolean
  }
  type: 'monthly' | 'yearly'
}

const props = defineProps<Props>()

const title = computed(() =>
  props.type === 'monthly' ? t('dashboard.monthlyTarget') : t('dashboard.yearlyTarget')
)
const icon = computed(() => (props.type === 'monthly' ? CalendarOutlined : TrophyOutlined))

const progressColor = computed(() => {
  const progress = props.data?.progress || 0
  if (progress >= 100) {
    return { '0%': '#52c41a', '100%': '#73d13d' }
  } else if (progress >= 50) {
    return { '0%': '#1890ff', '100%': '#40a9ff' }
  } else {
    return { '0%': '#faad14', '100%': '#ffc53d' }
  }
})

const percentageClass = computed(() => {
  const progress = props.data?.progress || 0
  if (progress >= 100) return 'success'
  if (progress >= 50) return 'normal'
  return 'warning'
})

const formatAmount = (amount?: number) => {
  if (!amount) return '0'
  // 大额数字使用万/亿单位，不显示小数
  if (amount >= 100000000) {
    return t('dashboard.hundredMillion', { value: (amount / 100000000).toFixed(1) })
  }
  if (amount >= 10000) {
    return t('dashboard.tenThousand', { value: (amount / 10000).toFixed(1) })
  }
  return amount.toLocaleString(locale.value, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  })
}

const formatPercent = (percent?: number) => {
  if (!percent) return '0.00%'
  return `${percent.toFixed(2)}%`
}

// 跳转到销售目标设置页面
const goToTargetSetting = () => {
  router.push('/statistics/sales-target')
}
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

.target-progress-card .progress-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-percentage {
  font-size: 36px;
  font-weight: 700;
  color: var(--ant-color-primary);
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  line-height: 1;
}

.progress-percentage.success {
  color: var(--ant-color-success);
}

.progress-percentage.warning {
  color: var(--ant-color-warning);
}

.progress-bar :deep(.ant-progress-bg) {
  border-radius: 6px;
}

.progress-detail {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: var(--ant-color-text-tertiary);
}

.progress-detail .detail-label {
  color: var(--ant-color-text-tertiary);
  white-space: nowrap;
}

.progress-detail .detail-value {
  color: var(--ant-color-text);
  font-weight: 500;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  word-break: keep-all;
  white-space: nowrap;
}

/* 响应式 */
@media (max-width: 767px) {
  .progress-percentage {
    font-size: 28px;
  }
}
</style>
