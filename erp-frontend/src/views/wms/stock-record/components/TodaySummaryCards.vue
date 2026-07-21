<template>
  <a-row :gutter="16" class="today-summary-cards">
    <!-- 今日入库 -->
    <a-col :span="6">
      <a-card :bordered="false" :loading="loading" class="summary-card">
        <a-statistic
          title="今日入库"
          :value="summary?.inQuantity ?? 0"
          :value-style="{ color: '#52c41a' }"
        >
          <template #prefix>
            <arrow-up-outlined :style="{ color: '#52c41a' }" />
          </template>
        </a-statistic>
      </a-card>
    </a-col>

    <!-- 今日出库 -->
    <a-col :span="6">
      <a-card :bordered="false" :loading="loading" class="summary-card">
        <a-statistic
          title="今日出库"
          :value="summary?.outQuantity ?? 0"
          :value-style="{ color: '#ff4d4f' }"
        >
          <template #prefix>
            <arrow-down-outlined :style="{ color: '#ff4d4f' }" />
          </template>
        </a-statistic>
      </a-card>
    </a-col>

    <!-- 今日预占 -->
    <a-col :span="6">
      <a-card :bordered="false" :loading="loading" class="summary-card">
        <a-statistic
          title="今日预占"
          :value="summary?.reserveQuantity ?? 0"
          :value-style="{ color: '#fa8c16' }"
        >
          <template #prefix>
            <lock-outlined :style="{ color: '#fa8c16' }" />
          </template>
        </a-statistic>
      </a-card>
    </a-col>

    <!-- 今日释放 -->
    <a-col :span="6">
      <a-card :bordered="false" :loading="loading" class="summary-card">
        <a-statistic
          title="今日释放"
          :value="summary?.releaseQuantity ?? 0"
          :value-style="{ color: '#13c2c2' }"
        >
          <template #prefix>
            <unlock-outlined :style="{ color: '#13c2c2' }" />
          </template>
        </a-statistic>
      </a-card>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import {
  ArrowUpOutlined,
  ArrowDownOutlined,
  LockOutlined,
  UnlockOutlined
} from '@ant-design/icons-vue'
import type { StockFlowTodaySummaryVO } from '@/api/wms/stock-flow/types'

defineProps<{
  summary: StockFlowTodaySummaryVO | null
  loading: boolean
}>()
</script>

<style scoped>
.today-summary-cards {
  margin-bottom: 16px;
}

.today-summary-cards .summary-card {
  border-radius: 8px;
  transition: all 0.3s;
}

.today-summary-cards .summary-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.today-summary-cards :deep(.ant-statistic-title) {
  font-size: 14px;
  color: var(--text-secondary, #666);
}

.today-summary-cards :deep(.ant-statistic-content) {
  font-size: 24px;
}
</style>
