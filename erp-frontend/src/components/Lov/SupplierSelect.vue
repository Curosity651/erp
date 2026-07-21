<template>
  <a-select
    v-model:value="modelValue"
    :placeholder="placeholder"
    :loading="loading"
    :allow-clear="allowClear"
    :size="size"
    show-search
    :filter-option="false"
    :not-found-content="loading ? '搜索中...' : '暂无数据'"
    option-label-prop="label"
    @search="handleSearch"
    @change="handleChange"
    @clear="handleClear"
  >
    <a-select-option
      v-for="option in supplierOptions"
      :key="option.supplierCode"
      :value="option.supplierCode"
      :label="option.name"
    >
      <div class="supplier-option">
        <span class="supplier-code">{{ option.supplierCode }}</span>
        <span class="supplier-name">{{ option.name }}</span>
      </div>
    </a-select-option>
  </a-select>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { getSupplierOptions } from '@/api/product/supplier'
import type { SupplierPageVO } from '@/api/product/supplier/types'

interface Props {
  value?: string
  placeholder?: string
  allowClear?: boolean
  size?: 'large' | 'middle' | 'small'
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请选择供应商',
  allowClear: true,
  size: 'middle'
})

const emits = defineEmits<{
  (e: 'update:value', value?: string): void
  (e: 'change', value?: string, option?: SupplierPageVO): void
  (e: 'select', option: SupplierPageVO): void
}>()

// 双向绑定
const modelValue = computed({
  get: () => props.value,
  set: value => emits('update:value', value)
})

// 状态
const loading = ref(false)
const supplierOptions = ref<SupplierPageVO[]>([])
const searchKeyword = ref('')

// 防抖搜索
let searchTimer: NodeJS.Timeout | null = null

/**
 * 加载供应商选项
 */
const loadSupplierOptions = async (keyword?: string) => {
  loading.value = true
  try {
    const result = await getSupplierOptions(keyword)
    supplierOptions.value = result.data || []
  } catch (error) {
    console.error('加载供应商选项失败:', error)
    message.error('加载供应商选项失败')
  } finally {
    loading.value = false
  }
}

/**
 * 处理搜索
 */
const handleSearch = (value: string) => {
  searchKeyword.value = value

  // 清除之前的定时器
  if (searchTimer) {
    clearTimeout(searchTimer)
  }

  // 防抖搜索
  searchTimer = setTimeout(() => {
    loadSupplierOptions(value.trim() || undefined)
  }, 300)
}

/**
 * 处理选择变化
 */
const handleChange = (value?: string) => {
  const selectedOption = supplierOptions.value.find(option => option.supplierCode === value)
  emits('change', value, selectedOption)

  if (selectedOption) {
    emits('select', selectedOption)
  }
}

/**
 * 处理清除
 */
const handleClear = () => {
  emits('change', undefined, undefined)
}

// 组件挂载时加载供应商选项
onMounted(() => {
  loadSupplierOptions()
})

// 监听值变化，如果当前值不在选项中，则重新加载
watch(
  () => props.value,
  newValue => {
    if (newValue && !supplierOptions.value.find(option => option.supplierCode === newValue)) {
      // 如果当前值不在选项中，尝试加载包含该值的选项
      loadSupplierOptions(newValue)
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.supplier-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 4px 0;
  min-height: 32px;
}

.supplier-code {
  font-weight: 600;
  color: #262626;
  font-size: 14px;
  line-height: 1.4;
  flex-shrink: 0;
}

.supplier-name {
  color: #595959;
  font-size: 14px;
  line-height: 1.4;
  text-align: right;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 60%;
}

/* 修复选中后的对齐问题 */
:deep(.ant-select-selection-item) {
  display: flex !important;
  align-items: center !important;
  line-height: 1.5 !important;
  height: 100% !important;
}

/* 确保不同尺寸下的正确对齐 */
:deep(.ant-select-single .ant-select-selector .ant-select-selection-item) {
  line-height: 30px !important;
  height: 30px !important;
  display: flex !important;
  align-items: center !important;
}

:deep(.ant-select-large.ant-select-single .ant-select-selector .ant-select-selection-item) {
  line-height: 38px !important;
  height: 38px !important;
  display: flex !important;
  align-items: center !important;
}

:deep(.ant-select-small.ant-select-single .ant-select-selector .ant-select-selection-item) {
  line-height: 22px !important;
  height: 22px !important;
  display: flex !important;
  align-items: center !important;
}
</style>
