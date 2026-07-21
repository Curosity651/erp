<template>
  <div class="outbound-item-table">
    <a-table :data-source="items" :columns="columns" :pagination="false" row-key="key" size="small">
      <template #bodyCell="{ column, record, index }">
        <!-- 订单信息 -->
        <template v-if="column.key === 'orderInfo'">
          <div class="order-info-cell">
            <div class="platform-order-id">{{ record.platformOrderId }}</div>
            <div class="platform-tag">
              <a-tag size="small" :color="getPlatformColor(record.platform)">
                {{ record.platform }}
              </a-tag>
            </div>
          </div>
        </template>

        <!-- SKU信息 -->
        <template v-else-if="column.key === 'skuInfo'">
          <sku-brief-cell :brief="record.skuBrief" />
        </template>

        <!-- 库存状态 -->
        <template v-else-if="column.key === 'stockStatus'">
          <StockStatusCell
            v-if="record.stockStatus"
            :status="record.stockStatus"
            :available-stock="record.availableStock"
            :shortage="record.shortage"
          />
          <span v-else class="no-stock-info">-</span>
        </template>

        <!-- 操作列 -->
        <template v-else-if="column.key === 'operate'">
          <a-button v-if="!disabled" type="link" danger size="small" @click="handleRemove(index)">
            移除
          </a-button>
        </template>
      </template>

      <!-- 空状态 -->
      <template #emptyText>
        <a-empty description='请点击"添加订单"选择待出库订单' />
      </template>

      <!-- 合计行 -->
      <template #summary>
        <a-table-summary fixed>
          <a-table-summary-row>
            <a-table-summary-cell :index="0" :col-span="2" align="right">
              <strong>合计</strong>
            </a-table-summary-cell>
            <a-table-summary-cell :index="2" align="center">
              <strong>{{ totalQuantity }}</strong>
            </a-table-summary-cell>
            <a-table-summary-cell :index="3" />
            <a-table-summary-cell v-if="!disabled" :index="4" />
          </a-table-summary-row>
        </a-table-summary>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'
import StockStatusCell from './StockStatusCell.vue'
import type { SkuBriefVO } from '@/api/types'
import type { StockStatus } from '@/api/wms/sales-outbound/types'

export interface OutboundItemFormData {
  key: string // 唯一标识: erpOrderId-skuCode
  erpOrderId: number
  platformOrderId: string
  platform: string
  skuCode: string
  skuBrief?: SkuBriefVO
  quantity: number
  remark?: string
  // 库存状态字段
  availableStock?: number
  shortage?: number
  stockStatus?: StockStatus
}

const props = defineProps<{
  items: OutboundItemFormData[]
  disabled: boolean
}>()

const emits = defineEmits<{
  (e: 'update:items', items: OutboundItemFormData[]): void
  (e: 'remove', index: number): void
}>()

// 表格列定义
const columns = computed(() => {
  const cols = [
    { title: '订单信息', key: 'orderInfo', width: 180 },
    { title: 'SKU信息', key: 'skuInfo', width: 180 },
    { title: '出库数量', dataIndex: 'quantity', width: 90, align: 'center' as const },
    { title: '库存状态', key: 'stockStatus', width: 140 }
  ]
  if (!props.disabled) {
    cols.push({ title: '操作', key: 'operate', width: 80, align: 'center' as const })
  }
  return cols
})

// 合计数量
const totalQuantity = computed(() => {
  return props.items.reduce((sum, item) => sum + item.quantity, 0)
})

// 获取平台颜色
const getPlatformColor = (platform: string): string => {
  const colorMap: Record<string, string> = {
    OZON: 'blue',
    WB: 'purple',
    WILDBERRIES: 'purple'
  }
  return colorMap[platform?.toUpperCase()] || 'default'
}

/**
 * 处理移除
 */
const handleRemove = (index: number) => {
  emits('remove', index)
}
</script>

<style scoped>
.outbound-item-table {
  min-height: 200px;
}

.order-info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.platform-order-id {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  color: #262626;
  font-weight: 600;
}

.platform-tag {
  display: flex;
}

.sku-info-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sku-code {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  color: #262626;
}

.sku-name {
  font-size: 12px;
  color: #8c8c8c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 150px;
}

.no-stock-info {
  color: #bfbfbf;
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
