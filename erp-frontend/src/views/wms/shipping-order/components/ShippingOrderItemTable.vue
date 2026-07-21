<template>
  <div class="item-table-container">
    <a-table
      :data-source="modelValue"
      :columns="columns"
      :pagination="false"
      size="small"
      row-key="purchaseOrderItemId"
    >
      <template #emptyText>
        <a-empty description="暂无货物明细，请点击「从采购单添加」按钮添加" />
      </template>
      <!-- 采购单号 -->
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'purchaseOrderNo'">
          {{ record.purchaseOrderNo || '-' }}
        </template>

        <!-- SKU信息 -->
        <template v-else-if="column.key === 'skuInfo'">
          <sku-brief-cell :brief="record.skuBrief" />
        </template>

        <!-- 可发货数量 -->
        <template v-else-if="column.key === 'availableQuantity'">
          {{ record.availableQuantity || '-' }}
        </template>

        <!-- 发货数量 -->
        <template v-else-if="column.key === 'quantity'">
          <a-input-number
            v-model:value="record.quantity"
            :min="1"
            :max="record.availableQuantity || 999999"
            :disabled="disabled"
            size="small"
            style="width: 100px"
            @change="handleQuantityChange"
          />
        </template>

        <!-- 操作 -->
        <template v-else-if="column.key === 'action'">
          <a-button v-if="!disabled" type="link" danger size="small" @click="handleRemove(index)">
            删除
          </a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import type { ShippingOrderItemDTO } from '@/api/wms/shipping-order/types'
import type { SkuBriefVO } from '@/api/wms/inventory/types'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'

type ItemWithDisplay = ShippingOrderItemDTO & {
  purchaseOrderNo?: string
  skuBrief?: SkuBriefVO
  availableQuantity?: number
}

const props = defineProps<{
  modelValue: ItemWithDisplay[]
  disabled: boolean
}>()

const emits = defineEmits<{
  (e: 'update:modelValue', value: ItemWithDisplay[]): void
}>()

const columns = [
  { title: '采购单号', key: 'purchaseOrderNo', width: 140 },
  { title: 'SKU信息', key: 'skuInfo', width: 240 },
  { title: '可发货数量', key: 'availableQuantity', width: 100, align: 'center' },
  { title: '发货数量', key: 'quantity', width: 120, align: 'center' },
  { title: '操作', key: 'action', width: 80, align: 'center' }
]

// 处理数量变化
const handleQuantityChange = () => {
  emits('update:modelValue', [...props.modelValue])
}

// 删除明细
const handleRemove = (index: number) => {
  const newItems = [...props.modelValue]
  newItems.splice(index, 1)
  emits('update:modelValue', newItems)
}
</script>

<style scoped>
.item-table-container {
  min-height: 200px;
}
</style>
