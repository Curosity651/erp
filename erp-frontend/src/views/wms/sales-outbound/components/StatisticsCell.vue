<template>
  <div class="statistics-cell">
    <!-- 主指标：出库总量 -->
    <div class="stat-primary">{{ totalQuantity }} 件</div>
    <!-- 次级指标：订单 + SKU -->
    <div class="stat-secondary">
      <span class="stat-item">{{ orderCount }} 单</span>
      <span class="stat-dot">·</span>
      <span class="stat-item">{{ skuCount }} SKU</span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StatisticsCell' })

defineProps<{
  orderCount: number
  skuCount: number
  totalQuantity: number
}>()
</script>

<style scoped>
.statistics-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 主指标：数量 + 单位同等大小 */
.stat-primary {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  font-variant-numeric: tabular-nums;
  line-height: 1.4;
}

/* 次级指标 */
.stat-secondary {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  font-variant-numeric: tabular-nums;
}

.stat-item {
  white-space: nowrap;
}

.stat-dot {
  color: rgba(0, 0, 0, 0.25);
}
</style>
