<template>
  <a-tree-select
    v-model:value="modelValue"
    :tree-data="treeData"
    :loading="loading"
    :placeholder="placeholder"
    :allow-clear="allowClear"
    :multiple="multiple"
    :tree-checkable="multiple"
    :show-checked-strategy="TreeSelect.SHOW_PARENT"
    tree-node-filter-prop="title"
    :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
    @change="handleChange"
  />
</template>

<script setup lang="ts">
import { TreeSelect } from 'ant-design-vue'
import { listCategory } from '@/api/product/category'
import type { CategoryPageVO } from '@/api/product/category/types'

interface Props {
  value?: number | number[]
  placeholder?: string
  allowClear?: boolean
  multiple?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请选择品类',
  allowClear: true,
  multiple: false
})

const emits = defineEmits<{
  (e: 'update:value', value: number | number[]): void
  (e: 'change', value: number | number[], option: any): void
}>()

// 双向绑定
const modelValue = computed({
  get: () => props.value,
  set: value => emits('update:value', value)
})

// 树形数据
const treeData = ref<any[]>([])
const loading = ref(false)

// 构建树形结构
const buildTreeData = (categories: CategoryPageVO[]): any[] => {
  const map = new Map<number, any>()
  const roots: any[] = []

  // 先创建所有节点
  categories.forEach(category => {
    map.set(category.id, {
      key: category.id,
      value: category.id,
      title: category.name,
      children: []
    })
  })

  // 构建父子关系
  categories.forEach(category => {
    const node = map.get(category.id)
    if (category.parentId === 0) {
      roots.push(node)
    } else {
      const parent = map.get(category.parentId)
      if (parent) {
        parent.children.push(node)
      }
    }
  })

  return roots
}

// 加载品类数据
const loadCategories = async () => {
  loading.value = true
  try {
    const response = await listCategory()
    const categories = response.data || []
    treeData.value = buildTreeData(categories)
  } catch (error) {
    console.error('加载品类数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 处理值变化
const handleChange = (value: number | number[], option: any) => {
  emits('change', value, option)
}

// 组件挂载时加载数据
onMounted(() => {
  loadCategories()
})
</script>
