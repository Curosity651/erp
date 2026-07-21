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
      v-for="option in warehouseOptions"
      :key="option.id"
      :value="option.id"
      :label="option.warehouseName"
    >
      <div class="warehouse-option">
        <span class="warehouse-name">{{ option.warehouseName }}</span>
        <a-tag v-if="option.warehouseType" size="small" :color="getTypeColor(option.warehouseType)">
          {{ getTypeName(option.warehouseType) }}
        </a-tag>
      </div>
    </a-select-option>
  </a-select>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { isSuccess } from '@/api'
import { getWarehouseOptions } from '@/api/wms/warehouse'
import type { WarehouseOptionVO } from '@/api/wms/warehouse/types'

interface Props {
  value?: number
  placeholder?: string
  allowClear?: boolean
  size?: 'large' | 'middle' | 'small'
  disabled?: boolean
  width?: string
  /** 仓库类型过滤 */
  warehouseType?: string
  /** 排除的仓库ID */
  excludeId?: number
  /** 区域ID过滤 */
  regionId?: number
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请选择仓库',
  allowClear: true,
  size: 'middle',
  disabled: false,
  width: '200px'
})

const emits = defineEmits<{
  (e: 'update:value', value?: number): void
  (e: 'change', value?: number, option?: WarehouseOptionVO): void
}>()

// 状态
const loading = ref(false)
const warehouseOptions = ref<WarehouseOptionVO[]>([])

// 仓库类型颜色
function getTypeColor(type: string): string {
  const colorMap: Record<string, string> = {
    OWN: 'blue',
    FBO: 'green'
  }
  return colorMap[type] || 'default'
}

// 仓库类型名称
function getTypeName(type: string): string {
  const nameMap: Record<string, string> = {
    OWN: '自有仓',
    FBO: 'FBO仓'
  }
  return nameMap[type] || type
}

// 本地搜索过滤
function filterOption(input: string, option: any) {
  const label = option.label || ''
  return label.toLowerCase().includes(input.toLowerCase())
}

// 加载仓库选项
async function loadOptions() {
  loading.value = true
  try {
    const result = await getWarehouseOptions()
    if (isSuccess(result) && result.data) {
      let options = result.data
      // 如果指定了区域ID，进行过滤
      if (props.regionId) {
        options = options.filter(o => o.regionId === props.regionId)
      }
      // 如果指定了仓库类型，进行过滤
      if (props.warehouseType) {
        options = options.filter(o => o.warehouseType === props.warehouseType)
      }
      // 如果指定了排除ID，进行过滤
      if (props.excludeId) {
        options = options.filter(o => o.id !== props.excludeId)
      }
      warehouseOptions.value = options
      // 过滤条件变化后，若当前选中项已不在可选项中则清空，避免提交不符合条件的仓库
      if (props.value != null && !warehouseOptions.value.some(o => o.id === props.value)) {
        handleChange(undefined)
      }
    }
  } catch (e) {
    console.error('加载仓库选项失败', e)
  } finally {
    loading.value = false
  }
}

// 变更事件
function handleChange(value?: number) {
  emits('update:value', value)
  const option = warehouseOptions.value.find(o => o.id === value)
  emits('change', value, option)
}

// 监听过滤条件变化重新加载
watch(
  () => [props.warehouseType, props.excludeId, props.regionId],
  () => {
    loadOptions()
  }
)

onMounted(() => {
  loadOptions()
})
</script>

<style lang="less" scoped>
.warehouse-option {
  display: flex;
  align-items: center;
  gap: 8px;

  .warehouse-name {
    flex: 1;
  }
}
</style>
