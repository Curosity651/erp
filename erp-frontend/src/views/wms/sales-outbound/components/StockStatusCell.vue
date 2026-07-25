<template>
  <div class="stock-status-cell" :class="statusClass">
    <template v-if="status === 'reserved'">
      <CheckCircleOutlined class="status-icon" />
      <span>已预留</span>
    </template>
    <template v-else-if="status === 'deducted'">
      <CheckCircleOutlined class="status-icon" />
      <span>已扣减</span>
    </template>
    <template v-else-if="status === 'sufficient'">
      <CheckCircleOutlined class="status-icon" />
      <span>充足 ({{ availableStock }})</span>
    </template>
    <template v-else-if="status === 'insufficient'">
      <ExclamationCircleOutlined class="status-icon" />
      <span>不足 ({{ availableStock }}, 缺{{ shortage }})</span>
    </template>
    <template v-else>
      <CloseCircleOutlined class="status-icon" />
      <span>无库存 (缺{{ shortage }})</span>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  CloseCircleOutlined
} from '@ant-design/icons-vue'
import type { StockStatus } from '@/api/wms/sales-outbound/types'

const props = defineProps<{
  status: StockStatus
  availableStock?: number
  shortage?: number
}>()

const statusClass = computed(() => {
  return {
    'status-sufficient': props.status === 'sufficient',
    'status-insufficient': props.status === 'insufficient',
    'status-zero': props.status === 'zero',
    'status-reserved': props.status === 'reserved',
    'status-deducted': props.status === 'deducted'
  }
})
</script>

<style scoped>
.stock-status-cell {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.status-icon {
  font-size: 14px;
}

.status-sufficient {
  color: #52c41a;
}

.status-insufficient {
  color: #faad14;
}

.status-zero {
  color: #ff4d4f;
}

.status-deducted {
  color: #8c8c8c;
}

.status-reserved {
  color: #1677ff;
}
</style>
