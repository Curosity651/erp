<template>
  <a-card class="dashboard-card">
    <template #title>
      <div class="card-title">
        <AppstoreOutlined class="card-icon" />
        <span>库位占用率</span>
      </div>
    </template>
    <div v-if="rows.length > 0" class="wh-list">
      <div v-for="w in rows" :key="w.warehouseId" class="wh-row">
        <div class="wh-line">
          <span class="wh-name">{{ w.warehouseName }}</span>
          <span class="wh-nums">
            {{ w.used.toLocaleString() }}/{{ w.total.toLocaleString() }}
            <span class="wh-rate" :style="{ color: rateColor(w.rate) }">{{ w.rate }}%</span>
          </span>
        </div>
        <a-progress
          :percent="w.rate"
          :stroke-color="rateColor(w.rate)"
          :show-info="false"
          :stroke-width="10"
        />
      </div>
    </div>
    <div v-else class="empty-state">
      <a-empty description="暂无数据" />
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { AppstoreOutlined } from '@ant-design/icons-vue'
import type { WarehouseCapacityVO } from '@/api/platform-dashboard/types'

const props = defineProps<{ data?: WarehouseCapacityVO[] }>()

const rows = computed(() =>
  (props.data || [])
    .map(w => ({
      ...w,
      rate: w.total > 0 ? Math.round((w.used / w.total) * 100) : 0
    }))
    .sort((a, b) => b.rate - a.rate)
)

// 占用率配色：高危红 / 偏高橙 / 正常蓝
function rateColor(rate: number): string {
  if (rate >= 90) return '#ff4d4f'
  if (rate >= 75) return '#fa8c16'
  return '#1890ff'
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

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-icon {
  color: var(--ant-color-primary);
  font-size: 18px;
}

.wh-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 300px;
}

.wh-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.wh-line {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 13px;
}

.wh-name {
  font-weight: 500;
  color: var(--ant-color-text);
}

.wh-nums {
  color: var(--ant-color-text-secondary);
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
}

.wh-rate {
  margin-left: 8px;
  font-weight: 700;
}

.empty-state {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
