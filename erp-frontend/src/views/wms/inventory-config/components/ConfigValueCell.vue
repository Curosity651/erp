<template>
  <div class="config-value-cell">
    <span :class="['value-dot', isOverridden ? 'dot-overridden' : 'dot-inherited']" />
    <span :class="['value-text', { 'value-inherited': !isOverridden }]">
      {{ displayValue }}
    </span>
    <a-tag v-if="isOverridden" color="blue" size="small">已覆盖</a-tag>
    <span v-else class="inherit-label">继承默认</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

defineOptions({ name: 'ConfigValueCell' })

const props = defineProps<{
  value: number | null | undefined
  defaultValue: number
  prefix?: string
  suffix?: string
}>()

const isOverridden = computed(() => {
  return props.value !== null && props.value !== undefined
})

const displayValue = computed(() => {
  const val = isOverridden.value ? props.value : props.defaultValue
  const prefix = props.prefix ?? ''
  const suffix = props.suffix ?? ''
  return `${prefix}${val}${suffix}`
})
</script>

<style scoped>
.config-value-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.value-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-overridden {
  background-color: var(--ant-color-primary);
}

.dot-inherited {
  border: 1.5px solid var(--ant-color-border);
  background-color: transparent;
}

.value-text {
  font-weight: 500;
  color: var(--ant-color-text);
}

.value-inherited {
  font-weight: 400;
  color: var(--ant-color-text-secondary);
}

.inherit-label {
  font-size: 12px;
  color: var(--ant-color-text-tertiary);
}
</style>
