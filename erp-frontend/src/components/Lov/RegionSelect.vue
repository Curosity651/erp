<template>
  <a-select
    :value="value"
    :placeholder="placeholder"
    :loading="loading"
    :allow-clear="allowClear"
    :size="size"
    :disabled="disabled"
    :style="{ width }"
    show-search
    :filter-option="filterOption"
    option-label-prop="label"
    @change="handleChange"
  >
    <a-select-option
      v-for="option in regionOptions"
      :key="option.id"
      :value="option.id"
      :label="option.regionName"
    >
      {{ option.regionName }}（{{ option.regionCode }}）
    </a-select-option>
  </a-select>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { isSuccess } from '@/api'
import { listRegionOptions } from '@/api/wms/region'
import type { RegionOptionVO } from '@/api/wms/region/types'

defineOptions({ name: 'RegionSelect' })

const props = withDefaults(defineProps<{
  value?: number
  placeholder?: string
  allowClear?: boolean
  size?: 'large' | 'middle' | 'small'
  disabled?: boolean
  width?: string
}>(), {
  placeholder: '请选择区域',
  allowClear: true,
})

const emit = defineEmits<{
  (e: 'update:value', value: number | undefined): void
  (e: 'change', value: number | undefined, option: RegionOptionVO | undefined): void
}>()

const regionOptions = ref<RegionOptionVO[]>([])
const loading = ref(false)

const fetchOptions = async () => {
  loading.value = true
  try {
    const result = await listRegionOptions()
    if (isSuccess(result)) {
      regionOptions.value = result.data ?? []
    }
  } finally {
    loading.value = false
  }
}

const filterOption = (input: string, option: any) => {
  const label = option.label || ''
  return label.toLowerCase().includes(input.toLowerCase())
}

const handleChange = (val: number | undefined) => {
  emit('update:value', val)
  const option = regionOptions.value.find(o => o.id === val)
  emit('change', val, option)
}

onMounted(fetchOptions)
</script>
