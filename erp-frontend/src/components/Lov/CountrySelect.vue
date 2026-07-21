<template>
  <a-select
    v-bind="$attrs"
    :value="value"
    :placeholder="placeholder"
    :size="size"
    :disabled="disabled"
    :loading="loading"
    :allow-clear="allowClear"
    show-search
    :filter-option="filterOption"
    @change="handleChange"
    @select="handleSelect"
  >
    <a-select-option
      v-for="country in countries"
      :key="country.code"
      :value="country.code"
      :label="`${country.flag} ${country.name} (${country.englishName})`"
    >
      {{ country.flag }} {{ country.name }} ({{ country.englishName }})
    </a-select-option>
  </a-select>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { countries as countriesData, type CountryOption } from '@/utils/countries'

interface Props {
  value?: string
  placeholder?: string
  size?: 'large' | 'middle' | 'small'
  disabled?: boolean
  loading?: boolean
  allowClear?: boolean
}

interface Emits {
  (e: 'update:value', value: string | undefined): void
  (e: 'change', value: string | undefined, option?: CountryOption): void
  (e: 'select', value: string, option: CountryOption): void
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请选择销售国家',
  size: 'middle',
  disabled: false,
  loading: false,
  allowClear: true
})

const emit = defineEmits<Emits>()

// 国家数据
const countries = computed<CountryOption[]>(() => countriesData)

// 搜索过滤函数
const filterOption = (input: string, option: any) => {
  const country = countries.value.find(c => c.code === option.value)
  if (!country) return false

  const searchText = input.toLowerCase()
  return (
    country.name.toLowerCase().includes(searchText) ||
    country.englishName.toLowerCase().includes(searchText) ||
    country.code.toLowerCase().includes(searchText)
  )
}

// 处理值变化
const handleChange = (value: string | undefined) => {
  const selectedCountry = value ? countries.value.find(c => c.code === value) : undefined
  emit('update:value', value)
  emit('change', value, selectedCountry)
}

// 处理选择
const handleSelect = (value: string) => {
  const selectedCountry = countries.value.find(c => c.code === value)
  if (selectedCountry) {
    emit('select', value, selectedCountry)
  }
}

// 国家数据可以通过单独导入 countriesData 获取
</script>

<script lang="ts">
export default {
  name: 'CountrySelect',
  inheritAttrs: false
}
</script>
