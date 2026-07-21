<template>
  <div class="statistic-panel">
    <a-card :bordered="false" :loading="loading" class="stat-card">
      <a-statistic
        title="仓内"
        :value="summary?.warehouseQuantity ?? 0"
        :value-style="{ color: 'var(--wms-color-warehouse)' }"
      />
    </a-card>
    <a-card :bordered="false" :loading="loading" class="stat-card">
      <a-statistic
        title="可用"
        :value="summary?.availableQuantity ?? 0"
        :value-style="{ color: 'var(--wms-color-available)' }"
      />
    </a-card>
    <a-card :bordered="false" :loading="loading" class="stat-card">
      <a-statistic
        title="占用"
        :value="summary?.reservedQuantity ?? 0"
        :value-style="{ color: 'var(--wms-color-reserved)' }"
      />
    </a-card>
    <a-card :bordered="false" :loading="loading" class="stat-card">
      <a-statistic
        title="在途"
        :value="summary?.inTransitQuantity ?? 0"
        :value-style="{ color: 'var(--wms-color-in-transit)' }"
      />
    </a-card>
    <a-card :bordered="false" :loading="loading" class="stat-card">
      <a-statistic
        title="残品"
        :value="summary?.damagedQuantity ?? 0"
        :value-style="{ color: 'var(--wms-color-damaged)' }"
      />
    </a-card>
    <a-card :bordered="false" :loading="loading" class="stat-card">
      <a-statistic>
        <template #title>
          <span class="stat-title">
            总体积
            <a-tooltip v-if="summary?.volumeMissingSkuCount" :title="`${summary.volumeMissingSkuCount} 个 SKU 缺少包装尺寸`">
              <warning-outlined class="volume-warning-icon" />
            </a-tooltip>
          </span>
        </template>
        <template #formatter>
          <span class="volume-value">{{ formatVolume(summary?.totalVolume) }}</span>
          <span class="volume-unit">m³</span>
        </template>
      </a-statistic>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { WarningOutlined } from '@ant-design/icons-vue'
import type { InventorySummaryVO } from '@/api/wms/inventory/types'

defineOptions({ name: 'StatisticPanel' })

defineProps<{
  summary: InventorySummaryVO | null
  loading: boolean
}>()

function formatVolume(value: number | null | undefined): string {
  if (value === null || value === undefined) return '--'
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<style scoped>
.statistic-panel {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.stat-card {
  border-radius: var(--ant-border-radius-lg);
}

.stat-card :deep(.ant-card-body) {
  padding: 16px 20px;
}

.stat-card :deep(.ant-statistic-title) {
  font-size: 14px;
  color: var(--ant-color-text-secondary);
  margin-bottom: 4px;
}

.stat-card :deep(.ant-statistic-content-value) {
  font-size: 28px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* 响应式调整 */
@media (max-width: 1400px) {
  .statistic-panel {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .statistic-panel {
    grid-template-columns: repeat(2, 1fr);
  }

  .stat-card :deep(.ant-statistic-content-value) {
    font-size: 24px;
  }
}

/* 体积指标卡样式 */
.stat-title {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.volume-warning-icon {
  color: var(--ant-color-warning);
  font-size: 14px;
  cursor: help;
}

.volume-value {
  color: var(--ant-color-text-secondary);
}

.volume-unit {
  font-size: 14px;
  font-weight: 400;
  margin-left: 4px;
  color: var(--ant-color-text-secondary);
}
</style>
