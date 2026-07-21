<template>
  <div class="transfer-item-table">
    <a-table
      :data-source="items"
      :columns="columns"
      :pagination="false"
      row-key="key"
      size="small"
      :scroll="{ x: 600 }"
    >
      <template #bodyCell="{ column, record, index }">
        <!-- SKU信息 - 使用 SkuBriefCell -->
        <template v-if="column.key === 'skuInfo'">
          <sku-brief-cell :brief="record.skuBrief" />
        </template>

        <!-- 可用库存 -->
        <template v-else-if="column.key === 'availableQuantity'">
          <span class="available-quantity">{{ record.availableQuantity }}</span>
        </template>

        <!-- 调拨数量 -->
        <template v-else-if="column.key === 'quantity'">
          <a-input-number
            v-if="!disabled"
            v-model:value="items[index].quantity"
            :min="1"
            :max="record.availableQuantity"
            :precision="0"
            style="width: 100%"
            @change="handleQuantityChange"
          />
          <span v-else>{{ record.quantity }}</span>
        </template>

        <!-- 备注 -->
        <template v-else-if="column.key === 'remark'">
          <a-input
            v-if="!disabled"
            v-model:value="items[index].remark"
            placeholder="备注"
            :maxlength="200"
            @change="handleRemarkChange"
          />
          <span v-else>{{ record.remark || '-' }}</span>
        </template>

        <!-- 操作列 -->
        <template v-else-if="column.key === 'operate'">
          <a-button v-if="!disabled" type="link" danger size="small" @click="handleRemove(index)">
            移除
          </a-button>
        </template>
      </template>

      <!-- 合计行 -->
      <template #summary>
        <a-table-summary fixed>
          <a-table-summary-row>
            <a-table-summary-cell :index="0" align="right">
              <strong>合计</strong>
            </a-table-summary-cell>
            <a-table-summary-cell :index="1" align="center">
              <strong>{{ totalAvailable }}</strong>
            </a-table-summary-cell>
            <a-table-summary-cell :index="2" align="center">
              <strong class="total-quantity">{{ totalQuantity }}</strong>
            </a-table-summary-cell>
            <a-table-summary-cell v-if="!disabled" :index="3" />
          </a-table-summary-row>
        </a-table-summary>
      </template>
    </a-table>

    <div v-if="items.length === 0" class="empty-tip">请点击"添加商品"选择要调拨的商品</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'
import type { SkuBriefVO } from '@/api/common/sku-types'

export interface TransferItemFormData {
  key: string // 唯一标识: skuCode
  skuCode: string
  skuBrief?: SkuBriefVO
  availableQuantity: number
  quantity: number
  remark?: string
}

const props = defineProps<{
  items: TransferItemFormData[]
  disabled: boolean
}>()

const emits = defineEmits<{
  (e: 'update:items', items: TransferItemFormData[]): void
  (e: 'remove', index: number): void
  (e: 'change'): void
}>()

// 表格列定义
const columns = computed(() => {
  const cols = [
    { title: 'SKU信息', key: 'skuInfo', width: 280 },
    { title: '可用库存', key: 'availableQuantity', width: 100, align: 'center' as const },
    { title: '调拨数量', key: 'quantity', width: 120, align: 'center' as const },
    { title: '备注', key: 'remark', width: 180 }
  ]
  if (!props.disabled) {
    cols.push({ title: '操作', key: 'operate', width: 80, align: 'center' as const })
  }
  return cols
})

// 合计可用库存
const totalAvailable = computed(() => {
  return props.items.reduce((sum, item) => sum + item.availableQuantity, 0)
})

// 合计调拨数量
const totalQuantity = computed(() => {
  return props.items.reduce((sum, item) => sum + (item.quantity || 0), 0)
})

/**
 * 处理数量变化
 */
const handleQuantityChange = () => {
  emits('change')
}

/**
 * 处理移除
 */
const handleRemove = (index: number) => {
  emits('remove', index)
}
</script>

<style scoped>
.transfer-item-table {
  min-height: 200px;
}

.available-quantity {
  color: #52c41a;
  font-weight: 500;
}

.total-quantity {
  color: #1890ff;
}

.empty-tip {
  text-align: center;
  padding: 40px 0;
  color: #8c8c8c;
}

:deep(.ant-table-summary) {
  background: #fafafa;
}
</style>
