<template>
  <a-select
    v-bind="$attrs"
    :loading="loading"
    :options="brandOptions"
    :field-names="{ label: 'name', value: 'name' }"
    placeholder="请选择品牌"
    show-search
    allow-clear
    :filter-option="filterOption"
    @focus="loadBrands"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getBrandList } from '@/api/product/brand'
import type { BrandListVO } from '@/api/product/brand/types'

defineOptions({ name: 'BrandSelect' })

// 状态
const loading = ref(false)
const brands = ref<BrandListVO[]>([])

// 计算属性
const brandOptions = computed(() =>
  brands.value
    .filter(brand => brand.status === 1) // 只显示启用状态的品牌
    .map(brand => ({
      label: brand.name,
      value: brand.name,
      ...brand
    }))
)

// 过滤选项
const filterOption = (input: string, option: any) => {
  return option.label.toLowerCase().includes(input.toLowerCase())
}

// 加载品牌数据
const loadBrands = async () => {
  if (brands.value.length > 0) return // 已加载过数据

  loading.value = true
  try {
    const result = await getBrandList()
    brands.value = result.data
  } catch (error) {
    console.error('加载品牌数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 组件挂载时预加载数据
onMounted(() => {
  loadBrands()
})
</script>
