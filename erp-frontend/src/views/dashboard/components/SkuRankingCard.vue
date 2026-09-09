<template>
  <a-card class="dashboard-card" :class="cardClass">
    <template #title>
      <div class="flex items-center gap-2">
        <component :is="icon" class="card-icon" />
        <span>{{ title }}</span>
      </div>
    </template>
    <template #extra>
      <a-button type="link" size="small" @click="handleViewMore">
        {{ t('dashboard.viewMore') }}
        <RightOutlined />
      </a-button>
    </template>
    <a-table
      :columns="columns"
      :data-source="tableData"
      :pagination="false"
      size="small"
      :row-class-name="getRowClassName"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'rank'">
          <div class="rank-cell">
            <component :is="getRankBadge(index)" v-if="getRankBadge(index)" class="rank-badge" />
            <span v-else class="rank-number">{{ index + 1 }}</span>
          </div>
        </template>

        <template v-if="column.key === 'sku'">
          <div class="sku-cell">
            <span class="sku-text" :class="{ unmapped: !record.isMapped }">{{ record.sku }}</span>
            <a-tag v-if="!record.isMapped" color="orange" size="small" class="unmapped-tag"
              >{{ t('dashboard.unmapped') }}</a-tag
            >
          </div>
        </template>

        <template v-if="column.key === 'quantity'">
          <span class="quantity-value">{{ record.quantity }}</span>
        </template>

        <template v-if="column.key === 'amount'">
          <span class="amount-value">₽{{ formatAmount(record.amount) }}</span>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { FireOutlined, WarningOutlined, RightOutlined } from '@ant-design/icons-vue'
import type { SkuItem, DashboardFilters } from '@/api/dashboard/types'
import { useDashboardFilterStore } from '@/stores/dashboard-filter-store'

interface Props {
  data?: SkuItem[]
  type: 'top' | 'bottom'
  // 当前筛选条件，用于跳转时传递
  filters?: DashboardFilters
}

const props = defineProps<Props>()
const router = useRouter()
const dashboardFilterStore = useDashboardFilterStore()
const { t, locale } = useI18n()

const title = computed(() =>
  props.type === 'top' ? t('dashboard.hotSku') : t('dashboard.slowSku')
)
const icon = computed(() => (props.type === 'top' ? FireOutlined : WarningOutlined))
const cardClass = computed(() => (props.type === 'top' ? 'top-ranking' : 'bottom-ranking'))

// 跳转到SKU排名详情页
function handleViewMore() {
  // 通过 store 传递筛选条件
  if (props.filters) {
    dashboardFilterStore.setFilters(props.filters)
  }
  router.push({
    path: '/statistics/sku-ranking',
    query: {
      sortOrder: props.type === 'top' ? 'desc' : 'asc'
    }
  })
}

const columns = computed(() => [
  { title: t('dashboard.rank'), key: 'rank', width: 60, align: 'center' as const },
  { title: 'SKU', key: 'sku', ellipsis: true },
  { title: t('dashboard.quantity'), key: 'quantity', width: 80, align: 'right' as const },
  { title: t('dashboard.amount'), key: 'amount', width: 120, align: 'right' as const }
])

const tableData = computed(() => props.data || [])

const getRankBadge = (index: number) => {
  if (props.type === 'top') {
    if (index === 0) return () => h('span', { style: 'font-size: 20px' }, '🥇')
    if (index === 1) return () => h('span', { style: 'font-size: 20px' }, '🥈')
    if (index === 2) return () => h('span', { style: 'font-size: 20px' }, '🥉')
  }
  return null
}

const getRowClassName = (_record: unknown, index: number) => {
  if (props.type === 'top' && index < 3) {
    return `rank-${index + 1}`
  }
  return ''
}

const formatAmount = (amount: number) => {
  return (
    amount?.toLocaleString(locale.value, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }) || '0.00'
  )
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

.top-ranking,
.bottom-ranking {
  position: relative;
  overflow: hidden;
}

.top-ranking::before,
.bottom-ranking::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  border-radius: 12px 12px 0 0;
}

.top-ranking::before {
  background: linear-gradient(90deg, #ff4d4f 0%, #ff7875 100%);
}

.bottom-ranking::before {
  background: linear-gradient(90deg, #8c8c8c 0%, #bfbfbf 100%);
}

.rank-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-cell .rank-badge {
  font-size: 20px;
}

.rank-cell .rank-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--ant-color-fill-quaternary);
  color: var(--ant-color-text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.sku-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sku-cell .sku-text {
  font-weight: 500;
}

.sku-cell .sku-text.unmapped {
  color: var(--ant-color-text-tertiary);
}

.sku-cell .unmapped-tag {
  font-size: 11px;
  padding: 0 6px;
  line-height: 18px;
}

.quantity-value,
.amount-value {
  font-weight: 600;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
}

/* 表格样式 */
:deep(.ant-table-thead > tr > th) {
  border-bottom: 2px solid var(--ant-color-border-secondary);
}

:deep(.ant-table-tbody > tr:hover > td) {
  background: var(--ant-control-item-bg-active-hover);
}

:deep(.ant-table-tbody > tr.rank-1 > td) {
  background: rgba(250, 173, 20, 0.05);
}

:deep(.ant-table-tbody > tr.rank-2 > td) {
  background: rgba(140, 140, 140, 0.05);
}

:deep(.ant-table-tbody > tr.rank-3 > td) {
  background: rgba(212, 56, 13, 0.05);
}
</style>
