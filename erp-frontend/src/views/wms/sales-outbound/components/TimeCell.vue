<template>
  <div class="time-cell">
    <div class="time-row primary">
      <span class="time-label">{{ primaryLabel }}</span>
      <span class="time-value">{{ primaryTime || '-' }}</span>
    </div>
    <div v-if="secondaryTime" class="time-row secondary">
      <span class="time-label">{{ secondaryLabel }}</span>
      <span class="time-value">{{ secondaryTime }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'TimeCell' })

withDefaults(
  defineProps<{
    primaryLabel?: string
    primaryTime: string
    secondaryLabel?: string
    secondaryTime?: string
  }>(),
  {
    primaryLabel: '出库',
    secondaryLabel: '创建'
  }
)
</script>

<style scoped>
.time-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.time-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 13px;
  line-height: 1.4;
}

.time-label {
  min-width: 28px;
  color: rgba(0, 0, 0, 0.45);
  flex-shrink: 0;
}

.time-row.primary .time-value {
  color: rgba(0, 0, 0, 0.88);
  font-variant-numeric: tabular-nums;
}

.time-row.secondary .time-value {
  color: rgba(0, 0, 0, 0.45);
  font-variant-numeric: tabular-nums;
}
</style>
