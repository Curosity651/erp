<template>
  <a-select
    v-model:value="innerValue"
    placeholder="请选择物流商"
    show-search
    :filter-option="filterOption"
    :options="providerOptions"
    :disabled="disabled"
    :allow-clear="allowClear"
    @change="handleChange"
  />
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { getLogisticsProviderOptions } from '@/api/wms/logistics-provider'
import type { LogisticsProviderOptionVO } from '@/api/wms/logistics-provider/types'
import { isSuccess } from '@/api'

defineOptions({ name: 'LogisticsProviderSelect' })

const props = withDefaults(
  defineProps<{
    value?: number
    disabled?: boolean
    allowClear?: boolean
  }>(),
  {
    value: undefined,
    disabled: false,
    allowClear: true
  }
)

const emits = defineEmits<{
  (e: 'update:value', value: number | undefined): void
  (e: 'change', value: number | undefined): void
}>()

const innerValue = ref<number | undefined>(props.value)
const providerOptions = ref<{ label: string; value: number }[]>([])

// 同步外部值
watch(
  () => props.value,
  val => {
    innerValue.value = val
  }
)

// 下拉搜索过滤
const filterOption = (input: string, option: { label: string }) => {
  return option.label.toLowerCase().includes(input.toLowerCase())
}

// 值变化处理
const handleChange = (value: number | undefined) => {
  emits('update:value', value)
  emits('change', value)
}

// 加载物流商选项
const loadProviderOptions = async () => {
  try {
    const result = await getLogisticsProviderOptions()
    if (isSuccess(result) && result.data) {
      providerOptions.value = result.data.map((item: LogisticsProviderOptionVO) => ({
        label: item.providerName,
        value: item.id
      }))
    }
  } catch (e) {
    console.error('加载物流商选项失败', e)
  }
}

onMounted(() => {
  loadProviderOptions()
})
</script>
