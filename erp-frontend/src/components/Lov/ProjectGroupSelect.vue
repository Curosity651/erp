<template>
  <a-select
    v-bind="$attrs"
    :loading="loading"
    :options="projectGroupOptions"
    :field-names="{ label: 'name', value: 'code' }"
    placeholder="请选择项目组"
    show-search
    allow-clear
    :filter-option="filterOption"
    @focus="loadProjectGroups"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getProjectGroupList } from '@/api/system/project-group'
import type { ProjectGroupListVO } from '@/api/system/project-group/types'

defineOptions({ name: 'ProjectGroupSelect' })

// 状态
const loading = ref(false)
const projectGroups = ref<ProjectGroupListVO[]>([])

// 计算属性
const projectGroupOptions = computed(() =>
  projectGroups.value
    .filter(group => group.status === 1) // 只显示启用状态的项目组
    .map(group => ({
      label: group.name,
      value: group.code,
      ...group
    }))
)

// 过滤选项
const filterOption = (input: string, option: any) => {
  return option.label.toLowerCase().includes(input.toLowerCase())
}

// 加载项目组数据
const loadProjectGroups = async () => {
  if (projectGroups.value.length > 0) return // 已加载过数据

  loading.value = true
  try {
    const result = await getProjectGroupList()
    projectGroups.value = result.data
  } catch (error) {
    console.error('加载项目组数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 组件挂载时预加载数据
onMounted(() => {
  loadProjectGroups()
})
</script>
