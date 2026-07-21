<template>
  <a-select
    :value="value"
    :placeholder="placeholder"
    :allow-clear="allowClear"
    :size="size"
    :disabled="disabled"
    :style="{ width }"
    @change="handleChange"
  >
    <a-select-option v-if="withAll" :value="''">
      {{ allLabel }}
    </a-select-option>
    <a-select-option v-for="option in PLATFORM_OPTIONS" :key="option.value" :value="option.value">
      {{ option.label }}
    </a-select-option>
  </a-select>
</template>

<script setup lang="ts">
import { PLATFORM_OPTIONS, type PlatformType } from '@/constants/platform'

defineOptions({ name: 'PlatformSelect' })

interface Props {
  value?: PlatformType
  placeholder?: string
  allowClear?: boolean
  size?: 'large' | 'middle' | 'small'
  disabled?: boolean
  width?: string
  /** 是否包含"全部"选项，选中时 value 为 undefined */
  withAll?: boolean
  /** "全部"选项的文案，默认为"全部" */
  allLabel?: string
}

withDefaults(defineProps<Props>(), {
  placeholder: '请选择平台',
  allowClear: true,
  size: 'middle',
  disabled: false,
  width: '200px',
  withAll: false,
  allLabel: '全部'
})

const emits = defineEmits<{
  (e: 'update:value', value?: PlatformType): void
  (e: 'change', value?: PlatformType): void
}>()

function handleChange(value?: PlatformType) {
  emits('update:value', value)
  emits('change', value)
}
</script>
