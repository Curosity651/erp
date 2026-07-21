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
      v-for="option in ownerOptions"
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
import { listErpTenants } from '@/api/tenant'
import type { TenantBrief } from '@/api/tenant/types'

interface Props {
  value?: number
  placeholder?: string
  allowClear?: boolean
  size?: 'large' | 'middle' | 'small'
  disabled?: boolean
  width?: string
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请选择货主',
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
const ownerOptions = ref<TenantBrief[]>([])

function filterOption(input: string, option: any) {
  const label = option.label || ''
  return label.toLowerCase().includes(input.toLowerCase())
}

async function loadOptions() {
  loading.value = true
  try {
    const result = await listErpTenants()
    if (isSuccess(result) && result.data) {
      ownerOptions.value = result.data
    }
  } catch (e) {
    console.error('加载货主选项失败', e)
  } finally {
    loading.value = false
  }
}

function handleChange(value?: number) {
  emits('update:value', value)
  emits(
    'change',
    value,
    ownerOptions.value.find(o => o.id === value)
  )
}

onMounted(loadOptions)
</script>

<script lang="ts">
export default {
  name: 'ErpTenantSelect'
}
</script>
