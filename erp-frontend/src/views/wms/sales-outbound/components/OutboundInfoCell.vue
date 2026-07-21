<template>
  <div class="outbound-info-cell">
    <div class="info-row">
      <PlatformTag :platform="platform" />
      <a class="outbound-no" @click="$emit('click')">{{ outboundNo }}</a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { PlatformTag } from '@/components/Platform'

defineOptions({ name: 'OutboundInfoCell' })

defineProps<{
  outboundNo: string
  platform: string
}>()

defineEmits<{
  (e: 'click'): void
}>()
</script>

<style scoped>
.outbound-info-cell {
  display: flex;
  flex-direction: column;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.outbound-no {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  cursor: pointer;
}

.outbound-no:hover {
  color: #1677ff;
}
</style>
