<template>
  <div class="sku-select-input">
    <a-input-group compact>
      <!-- 单选回显：文本输入框 -->
      <a-input
        v-if="!isMultiple"
        :value="displayText"
        :placeholder="placeholder"
        readonly
        style="width: calc(100% - 63px)"
      >
        <template #suffix>
          <close-circle-filled v-if="hasValue" class="clear-icon" @click="clearSelection" />
        </template>
      </a-input>

      <!-- 多选回显：使用 a-select 多选模式，支持单个删除 -->
      <a-select
        v-else
        :value="multipleValueForView"
        mode="multiple"
        :open="false"
        :placeholder="placeholder"
        :max-tag-count="maxTagCount"
        :max-tag-text-length="maxTagTextLength"
        :options="multipleOptionsForView"
        style="width: calc(100% - 63px)"
        allow-clear
        @change="handleViewChange"
      />

      <a-button type="default" @click="openModal">选择</a-button>
    </a-input-group>

    <!-- SKU 选择弹窗 -->
    <sku-select-modal
      v-model:open="visible"
      :multiple="multiple"
      :initial-selected-codes="currentSelectedCodes"
      @confirm="handleConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { CloseCircleFilled } from '@ant-design/icons-vue'
import SkuSelectModal from '@/components/Sku/SkuSelectModal.vue'
import { listSkuByCodes } from '@/api/product/sku'
import { isSuccess } from '@/api'
import type { SkuRow } from '@/components/Sku/types'

const props = withDefaults(
  defineProps<{
    /**
     * 当前选中SKU：
     * - 单选模式下：string | undefined
     * - 多选模式下：string[]
     */
    modelValue?: string | string[] | undefined
    /** 是否多选，默认为 false（单选） */
    multiple?: boolean
    placeholder?: string
    /** 多选时最多显示的 tag 数量，超出显示 +N */
    maxTagCount?: number | 'responsive'
    /** 多选时单个 tag 的最大长度 */
    maxTagTextLength?: number
  }>(),
  {
    placeholder: '请选择SKU',
    modelValue: undefined,
    multiple: false,
    maxTagCount: 1,
    maxTagTextLength: 8
  }
)

const emits = defineEmits<{
  (e: 'update:modelValue', v: string | string[] | undefined): void
  (e: 'sku-selected', row: SkuRow | SkuRow[] | null): void
}>()

const visible = ref(false)

// cache: skuCode -> display name
const labelMap = ref<Record<string, string>>({})
const loadingCodes = new Set<string>()

const isMultiple = computed(() => !!props.multiple)

const hasValue = computed(() => {
  if (isMultiple.value) {
    return Array.isArray(props.modelValue) && props.modelValue.length > 0
  }
  return typeof props.modelValue === 'string' && props.modelValue.length > 0
})

// 当前选中的 SKU 编码列表（用于传递给弹窗）
const currentSelectedCodes = computed(() => {
  if (!props.modelValue) return []
  return Array.isArray(props.modelValue) ? props.modelValue : [props.modelValue]
})

// 单选展示文本
const displayText = computed(() => {
  if (!hasValue.value || isMultiple.value) return ''
  const code = props.modelValue as string
  return labelMap.value[code] ?? code
})

// 多选模式用于回显的 value/option 列表（用 a-select 渲染）
const multipleValueForView = computed(() => {
  if (!isMultiple.value || !Array.isArray(props.modelValue)) return []
  return props.modelValue as string[]
})

const multipleOptionsForView = computed(() => {
  if (!isMultiple.value || !Array.isArray(props.modelValue)) return []
  const codes = props.modelValue as string[]
  return codes.map(code => ({ value: code, label: labelMap.value[code] ?? code }))
})

// 根据外部 v-model 加载显示名称
watch(
  () => props.modelValue,
  async val => {
    if (!val || (Array.isArray(val) && val.length === 0)) {
      return
    }

    const codes = Array.isArray(val) ? val : [val]
    // 找出需要加载的 codes
    const needLoadCodes = codes.filter(code => !(code in labelMap.value) && !loadingCodes.has(code))

    if (needLoadCodes.length > 0) {
      await fetchLabelsByCodes(needLoadCodes)
    }
  },
  { immediate: true }
)

const openModal = () => {
  visible.value = true
}

/**
 * 弹窗确认回调
 */
const handleConfirm = (rows: SkuRow[]) => {
  if (!rows || rows.length === 0) {
    return
  }

  const skuCodes = rows.map(r => r.skuCode)

  // 缓存显示名
  rows.forEach(row => {
    const displayName = row.chineseName || row.skuCode
    labelMap.value[row.skuCode] = displayName
  })

  if (isMultiple.value) {
    emits('update:modelValue', skuCodes)
    emits('sku-selected', rows)
  } else {
    emits('update:modelValue', skuCodes[0])
    emits('sku-selected', rows[0] ?? null)
  }
}

// 仅用于多选时在 a-select 上删除单个 tag 时同步
const handleViewChange = (values: string[]) => {
  if (!isMultiple.value) return
  const uniqueCodes = [...new Set(values || [])]
  emits('update:modelValue', uniqueCodes)
}

const clearSelection = () => {
  emits('update:modelValue', isMultiple.value ? [] : undefined)
  emits('sku-selected', isMultiple.value ? [] : null)
}

/**
 * 批量加载 SKU 显示名称
 */
async function fetchLabelsByCodes(codes: string[]) {
  if (codes.length === 0) return

  // 标记正在加载
  codes.forEach(code => loadingCodes.add(code))

  try {
    const result = await listSkuByCodes(codes)
    if (isSuccess(result) && result.data) {
      result.data.forEach(row => {
        labelMap.value[row.skuCode] = row.chineseName || row.skuCode
      })
    }
  } catch (e) {
    // 静默失败
    console.error('加载 SKU 显示名称失败', e)
  } finally {
    codes.forEach(code => loadingCodes.delete(code))
  }
}
</script>

<style scoped>
.sku-select-input {
  width: 100%;
}

.clear-icon {
  color: rgba(0, 0, 0, 0.25);
  font-size: 12px;
  cursor: pointer;
  transition: color 0.3s;
}

.clear-icon:hover {
  color: rgba(0, 0, 0, 0.45);
}
</style>
