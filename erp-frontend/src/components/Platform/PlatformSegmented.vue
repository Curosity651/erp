<template>
  <a-segmented
    :value="value"
    :options="options"
    :size="size"
    :disabled="disabled"
    @change="handleChange"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { PLATFORM_OPTIONS, type PlatformType } from '@/constants/platform'

defineOptions({ name: 'PlatformSegmented' })

interface Props {
  value?: PlatformType | ''
  size?: 'large' | 'middle' | 'small'
  disabled?: boolean
  /** 是否包含"全部"选项，选中时 value 为空字符串 '' */
  withAll?: boolean
  /** "全部"选项的文案，默认为"全部" */
  allLabel?: string
}

const props = withDefaults(defineProps<Props>(), {
  size: 'small',
  disabled: false,
  withAll: false,
  allLabel: '全部'
})

const emits = defineEmits<{
  (e: 'update:value', value: PlatformType | ''): void
  (e: 'change', value: PlatformType | ''): void
}>()

const options = computed(() => {
  const platformOpts = PLATFORM_OPTIONS.map(opt => ({ label: opt.label, value: opt.value }))
  if (props.withAll) {
    return [{ label: props.allLabel, value: '' }, ...platformOpts]
  }
  return platformOpts
})

function handleChange(value: PlatformType | '') {
  emits('update:value', value)
  emits('change', value)
}
</script>
