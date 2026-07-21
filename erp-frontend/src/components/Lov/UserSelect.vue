<template>
  <a-select
    v-model:value="selectedValue"
    :placeholder="placeholder"
    :size="size"
    :disabled="disabled"
    :mode="mode"
    show-search
    :filter-option="false"
    :loading="loading"
    :not-found-content="loading ? '加载中...' : filteredUsers.length === 0 ? '暂无匹配用户' : null"
    allow-clear
    @search="handleSearch"
    @change="handleChange"
    @clear="handleClear"
  >
    <a-select-option
      v-for="user in filteredUsers"
      :key="user.value"
      :value="user.value"
      :label="user.name"
    >
      {{ user.name }}
    </a-select-option>
  </a-select>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { UserOption } from '@/hooks/use-user-data'

export type { UserOption }

type SelectValue = number | number[] | undefined

interface Props {
  value?: SelectValue
  placeholder?: string
  size?: 'large' | 'middle' | 'small'
  disabled?: boolean
  loading?: boolean
  options?: UserOption[]
  mode?: 'multiple' | 'tags'
}

interface Emits {
  (e: 'update:value', value: SelectValue): void
  (e: 'change', value: SelectValue, option?: UserOption | UserOption[]): void
}

defineOptions({ name: 'UserSelect' })

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请选择用户',
  size: 'middle',
  disabled: false,
  loading: false,
  options: () => [],
  mode: undefined
})

const emit = defineEmits<Emits>()

// 本地状态
const searchKeyword = ref('')

// 计算属性
const selectedValue = computed({
  get: () => props.value,
  set: value => emit('update:value', value)
})

// 过滤后的用户列表
const filteredUsers = computed(() => {
  if (!searchKeyword.value) {
    return props.options
  }

  const keyword = searchKeyword.value.toLowerCase()
  return props.options.filter(
    user =>
      user.name.toLowerCase().includes(keyword) ||
      (user.username && user.username.toLowerCase().includes(keyword))
  )
})

// 处理搜索 - 本地搜索，无需防抖
const handleSearch = (value: string) => {
  searchKeyword.value = value
}

// 处理选择变化
const handleChange = (value: SelectValue) => {
  if (props.mode === 'multiple' || props.mode === 'tags') {
    const values = value as number[]
    const selectedUsers = values
      ?.map(v => props.options.find(user => user.value === v))
      .filter(Boolean) as UserOption[]
    emit('change', value, selectedUsers)
  } else {
    const selectedUser = value ? props.options.find(user => user.value === value) : undefined
    emit('change', value, selectedUser)
  }
}

// 处理清空
const handleClear = () => {
  searchKeyword.value = ''
  const emptyValue = props.mode === 'multiple' || props.mode === 'tags' ? [] : undefined
  emit('update:value', emptyValue)
  emit('change', emptyValue, undefined)
}
</script>
