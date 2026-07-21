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
      v-for="option in operatorOptions"
      :key="option.id"
      :value="option.id"
      :label="option.tenantName"
    >
      {{ option.tenantName }}
    </a-select-option>
  </a-select>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { isSuccess } from '@/api'
import { listWmsOperators } from '@/api/tenant'
import type { TenantBrief } from '@/api/tenant/types'

interface Props {
  value?: number
  placeholder?: string
  allowClear?: boolean
  size?: 'large' | 'middle' | 'small'
  disabled?: boolean
  width?: string
}

withDefaults(defineProps<Props>(), {
  placeholder: '请选择服务商',
  allowClear: true,
  size: 'middle',
  disabled: false,
  width: '100%'
})

const emits = defineEmits<{
  (e: 'update:value', value?: number): void
  (e: 'change', value?: number, option?: TenantBrief): void
}>()

const loading = ref(false)
const operatorOptions = ref<TenantBrief[]>([])

function filterOption(input: string, option: any) {
  const label = option.label || ''
  return label.toLowerCase().includes(input.toLowerCase())
}

async function loadOptions() {
  loading.value = true
  try {
    const result = await listWmsOperators()
    if (isSuccess(result) && result.data) {
      operatorOptions.value = result.data
    }
  } catch (e) {
    console.error('加载服务商选项失败', e)
  } finally {
    loading.value = false
  }
}

function handleChange(value?: number) {
  emits('update:value', value)
  emits(
    'change',
    value,
    operatorOptions.value.find(o => o.id === value)
  )
}

onMounted(loadOptions)
</script>

<script lang="ts">
export default {
  name: 'WmsOperatorSelect'
}
</script>
