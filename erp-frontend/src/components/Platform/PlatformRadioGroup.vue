<template>
  <a-radio-group :value="value" :disabled="disabled" :size="size" @change="handleChange">
    <template v-if="buttonStyle">
      <a-radio-button v-for="option in PLATFORM_OPTIONS" :key="option.value" :value="option.value">
        {{ option.label }}
      </a-radio-button>
    </template>
    <template v-else>
      <a-radio v-for="option in PLATFORM_OPTIONS" :key="option.value" :value="option.value">
        {{ option.label }}
      </a-radio>
    </template>
  </a-radio-group>
</template>

<script setup lang="ts">
import { PLATFORM_OPTIONS, type PlatformType } from '@/constants/platform'

defineOptions({ name: 'PlatformRadioGroup' })

interface Props {
  value?: PlatformType
  disabled?: boolean
  /** 是否使用按钮样式，默认 false */
  buttonStyle?: boolean
  /** 按钮尺寸 */
  size?: 'large' | 'middle' | 'small'
}

withDefaults(defineProps<Props>(), {
  disabled: false,
  buttonStyle: false,
  size: 'middle'
})

const emits = defineEmits<{
  (e: 'update:value', value?: PlatformType): void
  (e: 'change', value?: PlatformType): void
}>()

function handleChange(e: any) {
  const value = e.target.value as PlatformType | undefined
  emits('update:value', value)
  emits('change', value)
}
</script>
