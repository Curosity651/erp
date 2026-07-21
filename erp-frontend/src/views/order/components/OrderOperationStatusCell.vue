<template>
  <div class="order-cell operation-status">
    <!-- 锁定状态 -->
    <div
      class="operation-item"
      :class="locked === 1 ? 'chip chip-lock-locked' : 'chip chip-lock-unlocked'"
    >
      <span class="operation-icon">{{ locked === 1 ? '🔒' : '🔓' }}</span>
      <span class="operation-text">{{ locked === 1 ? '已锁定' : '未锁定' }}</span>
    </div>

    <!-- 面单状态 -->
    <div
      class="operation-item"
      :class="hasLabel ? 'chip chip-label-available' : 'chip chip-label-empty'"
    >
      <span class="operation-icon">{{ hasLabel ? '📋' : '📄' }}</span>
      <span class="operation-text">{{ hasLabel ? '面单已获取' : '面单待获取' }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  locked: number
  hasLabel: boolean
}

defineProps<Props>()
</script>

<style scoped>
.order-cell.operation-status {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 8px;
}

.operation-item.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  border: 1px solid transparent;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  width: fit-content;
}

.operation-item .operation-icon { font-size: 14px; }
.operation-item .operation-text { font-size: 12px; }

.chip-lock-locked {
  background: #fff1f0;
  color: #a8071a;
  border-color: #ffa39e;
}
.chip-lock-unlocked {
  background: #f6ffed;
  color: #237804;
  border-color: #b7eb8f;
}
.chip-label-available {
  background: #e6f4ff;
  color: #0958d9;
  border-color: #91caff;
}
.chip-label-empty {
  background: #fafafa;
  color: #8c8c8c;
  border-color: #e8e8e8;
}

@media (max-width: 1200px) {
  .operation-item { gap: 4px; }
  .operation-text {
    display: inline-block;
    font-size: 10px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 80px;
  }
}
</style>
