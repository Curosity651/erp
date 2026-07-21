<template>
  <span
    :class="[
      'mapping-badge',
      isMapped ? 'is-mapped' : 'is-empty',
      sizeClass,
      disabled && 'is-disabled'
    ]"
    role="button"
    :tabindex="disabled ? -1 : 0"
    :title="computedTitle"
    :aria-label="computedTitle"
    @click="handleClick"
    @keydown.space.prevent="handleClick"
    @keydown.enter.prevent="handleClick"
  >
    <span class="mapping-badge__icon" aria-hidden="true">
      <svg
        v-if="isMapped"
        width="12"
        height="12"
        viewBox="0 0 24 24"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M13.5 10.5L10.5 13.5M8 8L11 5C12.657 3.343 15.343 3.343 17 5C18.657 6.657 18.657 9.343 17 11L14 14M10 10L7 13C5.343 14.657 5.343 17.343 7 19C8.657 20.657 11.343 20.657 13 19L16 16"
          stroke="currentColor"
          stroke-width="1.6"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
      <svg
        v-else
        width="12"
        height="12"
        viewBox="0 0 24 24"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M12 5V19M5 12H19"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </span>
    <span v-if="isMapped" class="mapping-badge__count">{{ count }}</span>
    <span v-else class="mapping-badge__text">添加</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

// M6：Badge 为纯展示公共组件，只负责展示 + 点击抛 open 事件；映射详情弹窗改由业务视图层(SkuPage)持有，
// 杜绝原本 components/ → views/ 的反向依赖（分层倒置）。
const props = defineProps<{
  count?: number
  size?: 'sm' | 'md'
  title?: string
  disabled?: boolean
  skuCode: string
  skuName?: string
  skuFiles?: any
}>()

const emit = defineEmits<{ (e: 'open'): void }>()

const isMapped = computed(() => (props.count || 0) > 0)
const sizeClass = computed(() => (props.size === 'md' ? 'size-md' : 'size-sm'))
const computedTitle = computed(() => {
  if (props.title) return props.title
  const c = props.count || 0
  return c > 0 ? `已有 ${c} 个映射，点击查看` : '暂无映射，点击添加'
})

const disabled = computed(() => props.disabled)

const handleClick = () => {
  if (disabled.value) return
  emit('open')
}
</script>

<style scoped>
.mapping-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  border: 1px solid #d9d9d9;
  user-select: none;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    color 0.15s ease;
}
.size-sm {
  min-height: 18px;
}
.size-md {
  min-height: 22px;
  font-size: 12px;
  padding: 3px 10px;
}
.mapping-badge.is-mapped {
  background: #f5f5f5;
  color: #262626;
}
.mapping-badge.is-empty {
  background: transparent;
  color: #8c8c8c;
  border-style: dashed;
}
.mapping-badge:hover:not(.is-disabled) {
  background: #efefef;
  border-color: #bfbfbf;
}
.mapping-badge.is-empty:hover:not(.is-disabled) {
  background: #fafafa;
  color: #595959;
}
.mapping-badge.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.mapping-badge__icon {
  display: inline-flex;
}
.mapping-badge__count {
  font-weight: 700;
  letter-spacing: 0.2px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}
.mapping-badge__text {
  font-weight: 600;
}
</style>
