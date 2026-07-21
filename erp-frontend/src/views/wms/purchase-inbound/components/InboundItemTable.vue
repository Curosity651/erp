<template>
  <div class="inbound-item-table">
    <a-table
      :data-source="items"
      :columns="columns"
      :pagination="false"
      :row-selection="disabled ? undefined : rowSelection"
      row-key="shippingOrderItemId"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <!-- 采购单号 -->
        <template v-if="column.key === 'purchaseOrderNo'">
          <div class="purchase-order-cell">
            <div class="order-no">{{ record.purchaseOrderNo }}</div>
            <div v-if="record.expectedDeliveryDate" class="delivery-date">
              预计交货: {{ record.expectedDeliveryDate }}
            </div>
          </div>
        </template>

        <!-- SKU信息 -->
        <template v-else-if="column.key === 'skuInfo'">
          <sku-brief-cell :brief="record.skuBrief" />
        </template>
      </template>

      <!-- 合计行（建单阶段仅声明应到数量，实到由平台收货时录入） -->
      <template #summary>
        <a-table-summary fixed>
          <a-table-summary-row>
            <a-table-summary-cell v-if="!disabled" :index="0" />
            <a-table-summary-cell :index="disabled ? 0 : 1" :col-span="2" align="right">
              <strong>合计</strong>
            </a-table-summary-cell>
            <a-table-summary-cell :index="disabled ? 2 : 3" align="center">
              <strong>{{ totalExpected }}</strong>
            </a-table-summary-cell>
          </a-table-summary-row>
        </a-table-summary>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TableRowSelection } from 'ant-design-vue/es/table/interface'
import { SkuBriefCell } from '@/components/Sku'
import type { InboundItemFormData } from '@/api/wms/purchase-inbound/types'
import { useInboundItemSummary } from '../hooks/useInboundItemSummary'

const props = defineProps<{
  items: InboundItemFormData[]
  disabled: boolean
}>()

const emits = defineEmits<{
  (e: 'update:items', items: InboundItemFormData[]): void
}>()

// 表格列定义（建单阶段不录实到/未到，这两列在详情/收货页才出现）
const columns = computed(() => {
  return [
    { title: '采购单号', key: 'purchaseOrderNo', width: 150 },
    { title: 'SKU信息', key: 'skuInfo', width: 220 },
    { title: '应到数量', dataIndex: 'expectedQuantity', width: 90, align: 'center' as const }
  ]
})

// 选中的行keys
const selectedRowKeys = computed(() => {
  return props.items.filter(item => item.selected).map(item => item.shippingOrderItemId)
})

// 行选择配置
const rowSelection = computed<TableRowSelection>(() => ({
  type: 'checkbox',
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[]) => {
    const newItems = props.items.map(item => ({
      ...item,
      selected: keys.includes(item.shippingOrderItemId)
    }))
    emits('update:items', newItems)
  }
}))

// 合计计算（只计算选中的项）
const { totalExpected } = useInboundItemSummary(
  computed(() => props.items),
  true
)
</script>

<style scoped>
.inbound-item-table {
  min-height: 200px;
}

.purchase-order-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.order-no {
  font-weight: 600;
  color: #262626;
}

.delivery-date {
  font-size: 11px;
  color: #8c8c8c;
}

.short-quantity {
  color: #f5222d;
  font-weight: 600;
}

:deep(.ant-table-summary) {
  background: #fafafa;
}
</style>
