<template>
  <div class="selection-pool" :class="{ 'is-empty': items.length === 0 }">
    <!-- 空状态 -->
    <span v-if="items.length === 0" class="placeholder">
      {{ placeholder }}
    </span>

    <!-- 已选列表 -->
    <template v-else>
      <a-tag
        v-for="item in displayItems"
        :key="getKey(item)"
        closable
        class="selection-tag"
        @close="handleRemove(item)"
      >
        {{ getLabel(item) }}
      </a-tag>

      <!-- 超出数量提示 -->
      <a-tooltip v-if="overflowCount > 0" :title="overflowTooltip">
        <a-tag class="overflow-tag">+{{ overflowCount }}</a-tag>
      </a-tooltip>

      <!-- 清空按钮 -->
      <a-button type="link" size="small" class="clear-btn" @click="handleClear">
        清空
      </a-button>
    </template>
  </div>
</template>

<script setup lang="ts" generic="T, K extends string | number = number">
import { computed } from 'vue'

interface Props {
  /** 已选项列表 */
  items: T[]
  /** 主键字段名或获取函数 */
  rowKey: keyof T | ((item: T) => K)
  /** 显示文本字段名或获取函数 */
  labelKey: keyof T | ((item: T) => string)
  /** 空状态提示文本 */
  placeholder?: string
  /** 最多显示的 tag 数量，超出显示 +N */
  maxDisplay?: number
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '暂未选择',
  maxDisplay: 10
})

const emit = defineEmits<{
  (e: 'remove', key: K): void
  (e: 'clear'): void
}>()

// 获取 key
const getKey = (item: T): K => {
  return typeof props.rowKey === 'function'
    ? props.rowKey(item)
    : (item[props.rowKey] as K)
}

// 获取显示文本
const getLabel = (item: T): string => {
  return typeof props.labelKey === 'function'
    ? props.labelKey(item)
    : String(item[props.labelKey])
}

// 显示的项（限制数量）
const displayItems = computed(() => {
  if (props.maxDisplay && props.items.length > props.maxDisplay) {
    return props.items.slice(0, props.maxDisplay)
  }
  return props.items
})

// 溢出数量
const overflowCount = computed(() => {
  if (!props.maxDisplay) return 0
  return Math.max(0, props.items.length - props.maxDisplay)
})

// 溢出提示
const overflowTooltip = computed(() => {
  if (overflowCount.value === 0) return ''
  const hiddenItems = props.items.slice(props.maxDisplay)
  return hiddenItems.map(item => getLabel(item)).join('、')
})

// 移除单项
const handleRemove = (item: T) => {
  emit('remove', getKey(item))
}

// 清空全部
const handleClear = () => {
  emit('clear')
}
</script>

<style scoped>
.selection-pool {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 8px 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  margin-bottom: 12px;
}

.selection-pool.is-empty {
  justify-content: center;
}

.placeholder {
  color: #bfbfbf;
  font-size: 13px;
}

.selection-tag {
  margin: 0;
}

.overflow-tag {
  margin: 0;
  background: #e6f4ff;
  border-color: #91caff;
  color: #1677ff;
  cursor: default;
}

.clear-btn {
  padding: 0 4px;
  height: auto;
  font-size: 12px;
  color: #8c8c8c;
}

.clear-btn:hover {
  color: #ff4d4f;
}
</style>
