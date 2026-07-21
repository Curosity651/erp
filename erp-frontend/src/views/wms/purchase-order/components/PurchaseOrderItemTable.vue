<template>
  <div class="purchase-order-item-table">
    <a-table
      :data-source="items"
      :columns="columns"
      :pagination="false"
      size="small"
      row-key="tempId"
    >
      <template #bodyCell="{ column, record, index }">
        <!-- SKU信息 -->
        <template v-if="column.key === 'skuInfo'">
          <sku-brief-cell :brief="getSkuBrief(record.skuCode)" />
        </template>

        <!-- 数量 -->
        <template v-else-if="column.key === 'quantity'">
          <a-input-number
            v-if="!disabled"
            v-model:value="record.quantity"
            :min="1"
            :precision="0"
            style="width: 100%"
            @change="handleQuantityChange(index)"
          />
          <span v-else>{{ record.quantity }}</span>
        </template>

        <!-- 单价 -->
        <template v-else-if="column.key === 'unitPrice'">
          <a-input-number
            v-if="!disabled"
            v-model:value="record.unitPrice"
            :min="0"
            :precision="2"
            style="width: 100%"
            @change="handlePriceChange(index)"
          />
          <span v-else>{{ formatAmountDisplay(record.unitPrice) }}</span>
        </template>

        <!-- 金额 -->
        <template v-else-if="column.key === 'amount'">
          <span class="amount-text">{{ formatAmountDisplay(record.amount || 0) }}</span>
        </template>

        <!-- 备注 -->
        <template v-else-if="column.key === 'remark'">
          <a-input
            v-if="!disabled"
            v-model:value="record.remark"
            placeholder="备注"
            :maxlength="200"
          />
          <span v-else>{{ record.remark || '-' }}</span>
        </template>

        <!-- 操作 -->
        <template v-else-if="column.key === 'action'">
          <a-button
            v-if="!disabled"
            type="link"
            size="small"
            danger
            @click="handleRemoveItem(index)"
          >
            <delete-outlined />
          </a-button>
        </template>
      </template>

      <!-- 汇总行 -->
      <template #summary>
        <a-table-summary fixed>
          <a-table-summary-row class="summary-row">
            <a-table-summary-cell :index="0" :col-span="2">
              <span class="summary-label">合计</span>
            </a-table-summary-cell>
            <a-table-summary-cell :index="2">
              <span class="summary-value">{{ totalQuantity }}</span>
            </a-table-summary-cell>
            <a-table-summary-cell :index="3" />
            <a-table-summary-cell :index="4">
              <span class="summary-value amount-text">{{ formatAmountDisplay(totalAmount) }}</span>
            </a-table-summary-cell>
            <a-table-summary-cell :index="5" />
            <a-table-summary-cell :index="6" />
          </a-table-summary-row>
        </a-table-summary>
      </template>
    </a-table>

    <!-- SKU 选择弹窗 -->
    <sku-select-modal
      v-model:open="skuSelectorVisible"
      :multiple="true"
      @confirm="handleSkuSelected"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { DeleteOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import type { PurchaseOrderItemDTO, SkuBriefVO } from '@/api/wms/purchase-order/types'
import SkuSelectModal from '@/components/Sku/SkuSelectModal.vue'
import { SkuBriefCell } from '@/components/Sku'
import type { SkuRow } from '@/components/Sku/types'
import { calculateItemAmount } from '@/utils/amount-utils'
import { formatAmount } from '@/utils/currency-utils'

interface ItemWithTempId extends PurchaseOrderItemDTO {
  tempId: string
  amount?: number
}

interface Props {
  modelValue: PurchaseOrderItemDTO[]
  disabled?: boolean
  currencyCode?: string
  skuBriefMap?: Record<string, SkuBriefVO>
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  currencyCode: 'CNY',
  skuBriefMap: () => ({})
})

const emits = defineEmits<{
  (e: 'update:modelValue', value: PurchaseOrderItemDTO[]): void
  (e: 'change', totalAmount: number): void
  (e: 'skuBriefUpdate', skuBriefMap: Record<string, SkuBriefVO>): void
}>()

// 内部数据（带临时ID）
const items = ref<ItemWithTempId[]>([])

// SKU 选择弹窗可见性
const skuSelectorVisible = ref(false)

// 已添加的 SKU 编码集合（用于去重校验）
const addedSkuCodes = computed(() => new Set(items.value.map(item => item.skuCode).filter(Boolean)))

// 获取 SKU 展示信息
const getSkuBrief = (skuCode: string): SkuBriefVO | undefined => {
  return props.skuBriefMap[skuCode]
}

// 生成临时ID
let tempIdCounter = 0
const generateTempId = () => `temp_${++tempIdCounter}_${Date.now()}`

// 监听外部数据变化（仅在初始化或外部主动更新时同步）
// 使用 JSON.stringify 比较避免 deep watch 的性能开销
let isInternalUpdate = false
let lastModelValueSnapshot = ''

watch(
  () => JSON.stringify(props.modelValue.map(item => ({ id: item.id, skuCode: item.skuCode }))),
  () => {
    // 如果是内部更新触发的，跳过
    if (isInternalUpdate) {
      isInternalUpdate = false
      return
    }

    const currentSnapshot = JSON.stringify(props.modelValue.map(item => ({ id: item.id, skuCode: item.skuCode })))
    if (currentSnapshot === lastModelValueSnapshot) {
      return
    }
    lastModelValueSnapshot = currentSnapshot

    items.value = (props.modelValue || []).map(item => ({
      ...item,
      tempId: (item as ItemWithTempId).tempId || generateTempId(),
      amount: calculateItemAmount(item.quantity || 0, item.unitPrice || 0)
    }))
  },
  { immediate: true }
)

// 同步数据到外部
const syncToParent = () => {
  isInternalUpdate = true
  const result: PurchaseOrderItemDTO[] = items.value.map(item => ({
    id: item.id,
    skuCode: item.skuCode,
    quantity: item.quantity,
    unitPrice: item.unitPrice,
    remark: item.remark
  }))
  emits('update:modelValue', result)
  emits('change', totalAmount.value)
}

// 计算总数量
const totalQuantity = computed(() => {
  return items.value.reduce((sum, item) => sum + (item.quantity || 0), 0)
})

// 计算总金额
const totalAmount = computed(() => {
  return items.value.reduce((sum, item) => sum + (item.amount || 0), 0)
})

// 打开 SKU 选择弹窗
const openSkuSelector = () => {
  skuSelectorVisible.value = true
}

// SKU 选择确认
const handleSkuSelected = (selectedSkus: SkuRow[]) => {
  if (!selectedSkus || selectedSkus.length === 0) return

  const duplicateSkus: string[] = []
  const newItems: ItemWithTempId[] = []
  const newDisplayMap: Record<string, SkuBriefVO> = {}

  for (const sku of selectedSkus) {
    // 检查是否重复
    if (addedSkuCodes.value.has(sku.skuCode)) {
      duplicateSkus.push(sku.skuCode)
      continue
    }

    // 构建 SKU 展示信息（使用新的 SkuRow 结构）
    newDisplayMap[sku.skuCode] = {
      skuCode: sku.skuCode,
      skuName: sku.chineseName || sku.skuCode,
      mainImage: sku.mainImage ? `${sku.mainImage}` : ''
    }

    // 添加新的明细行
    newItems.push({
      tempId: generateTempId(),
      skuCode: sku.skuCode,
      quantity: 1,
      unitPrice: 0,
      amount: 0,
      remark: ''
    })
  }

  // 显示重复提示
  if (duplicateSkus.length > 0) {
    message.warning(`以下 SKU 已存在，已跳过：${duplicateSkus.join(', ')}`)
  }

  // 添加新明细
  if (newItems.length > 0) {
    items.value.push(...newItems)
    // 发送 SKU 展示信息更新事件
    emits('skuBriefUpdate', newDisplayMap)
    syncToParent()
    message.success(`成功添加 ${newItems.length} 条明细`)
  }
}

// 删除明细
const handleRemoveItem = (index: number) => {
  items.value.splice(index, 1)
  syncToParent()
}

// 数量变化
const handleQuantityChange = (index: number) => {
  const item = items.value[index]
  item.amount = calculateItemAmount(item.quantity || 0, item.unitPrice || 0)
  syncToParent()
}

// 单价变化
const handlePriceChange = (index: number) => {
  const item = items.value[index]
  item.amount = calculateItemAmount(item.quantity || 0, item.unitPrice || 0)
  syncToParent()
}

// 格式化金额 - 使用工具函数（带货币符号）
const formatAmountDisplay = (amount: number): string => {
  return formatAmount(amount, props.currencyCode, false)
}

// 检查 SKU 是否重复
const checkDuplicateSku = (skuCode: string): boolean => {
  return addedSkuCodes.value.has(skuCode)
}

// 表格列定义
const columns = [
  { title: '序号', width: 50, customRender: ({ index }: { index: number }) => index + 1 },
  { title: 'SKU信息', key: 'skuInfo', width: 280 },
  { title: '数量', key: 'quantity', width: 100, align: 'center' as const },
  { title: '单价', key: 'unitPrice', width: 120, align: 'right' as const },
  { title: '金额', key: 'amount', width: 120, align: 'right' as const },
  { title: '备注', key: 'remark', width: 150 },
  { title: '操作', key: 'action', width: 60, align: 'center' as const, fixed: 'right' as const }
]

// 暴露方法
defineExpose({
  getTotalAmount: () => totalAmount.value,
  getItems: () => items.value,
  checkDuplicateSku,
  openSkuSelector,
  validate: () => {
    if (items.value.length === 0) {
      return { valid: false, message: '请至少添加一条采购明细' }
    }
    for (let i = 0; i < items.value.length; i++) {
      const item = items.value[i]
      if (!item.skuCode) {
        return { valid: false, message: `第${i + 1}行请选择SKU` }
      }
      if (!item.quantity || item.quantity <= 0) {
        return { valid: false, message: `第${i + 1}行数量必须大于0` }
      }
      if (item.unitPrice === undefined || item.unitPrice < 0) {
        return { valid: false, message: `第${i + 1}行单价不能为负数` }
      }
    }
    // 检查 SKU 重复
    const skuCodes = items.value.map(item => item.skuCode).filter(Boolean)
    const uniqueCodes = new Set(skuCodes)
    if (skuCodes.length !== uniqueCodes.size) {
      return { valid: false, message: '存在重复的 SKU，请检查' }
    }
    return { valid: true, message: '' }
  }
})
</script>

<style scoped>
.purchase-order-item-table {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
}

.amount-text {
  color: #f5222d;
  font-weight: 500;
}

.summary-label {
  font-weight: 600;
  color: #262626;
}

.summary-value {
  font-weight: 600;
  color: #262626;
}

:deep(.ant-table-summary) {
  background: #fafafa;
}

:deep(.ant-table-summary .ant-table-cell) {
  background: #fafafa;
}

:deep(.summary-row .ant-table-cell) {
  background: #fafafa !important;
}

/* 固定列表头背景色 */
:deep(.ant-table-thead > tr > th.ant-table-cell-fix-right) {
  background: #fafafa !important;
}

/* 固定列数据行背景色 */
:deep(.ant-table-tbody > tr > td.ant-table-cell-fix-right) {
  background: #fff;
}

/* 固定列合计行背景色 */
:deep(.ant-table-summary .ant-table-cell-fix-right) {
  background: #fafafa !important;
}
</style>
