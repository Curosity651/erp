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
      v-for="option in visibleOptions"
      :key="option.id"
      :value="option.id"
      :label="option.tenantName"
    >
      {{ option.tenantName }}
    </a-select-option>
  </a-select>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { isSuccess } from '@/api'
import { listAllErpTenants } from '@/api/tenant'
import type { TenantBrief } from '@/api/tenant/types'

interface Props {
  value?: number
  placeholder?: string
  allowClear?: boolean
  size?: 'large' | 'middle' | 'small'
  disabled?: boolean
  width?: string
  /** 仅显示某服务商名下的货主（与服务商下拉联动）；不传则显示全部货主 */
  operatorId?: number
  /** 进一步限制为指定货主 ID；传空数组表示没有可选货主。 */
  allowedIds?: number[]
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

const visibleOptions = computed(() => {
  let options = props.operatorId
    ? ownerOptions.value.filter(o => o.parentWmsTenantId === props.operatorId)
    : ownerOptions.value
  if (props.allowedIds !== undefined) {
    const allowed = new Set(props.allowedIds)
    options = options.filter(o => allowed.has(o.id))
  }
  return options
})

function filterOption(input: string, option: any) {
  const label = option.label || ''
  return label.toLowerCase().includes(input.toLowerCase())
}

async function loadOptions() {
  loading.value = true
  try {
    const result = await listAllErpTenants()
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

// 服务商变化时，若当前所选货主不在该服务商名下则清空
watch(
  () => [props.operatorId, props.allowedIds],
  () => {
    if (props.value && !visibleOptions.value.some(o => o.id === props.value)) {
      handleChange(undefined)
    }
  }
)

onMounted(loadOptions)
</script>

<script lang="ts">
export default {
  name: 'PlatformOwnerSelect'
}
</script>
