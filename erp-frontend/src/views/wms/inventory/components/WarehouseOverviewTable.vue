<template>
  <div class="warehouse-overview-table">
    <!-- 工具栏 -->
    <div class="wms-table-toolbar">
      <span class="wms-table-toolbar__total">共 {{ data.length }} 个仓库</span>
      <a-button :loading="loading" @click="emit('refresh')">
        <reload-outlined :spin="loading" />
        刷新
      </a-button>
    </div>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :loading="loading"
      :pagination="false"
      row-key="warehouseId"
      size="middle"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'warehouseInfo'">
          <warehouse-display-cell :display="record.warehouseDisplay" />
        </template>
        <template v-else-if="column.key === 'warehouseQuantity'">
          <span class="wms-quantity-cell">{{ record.warehouseQuantity?.toLocaleString() ?? 0 }}</span>
        </template>
        <template v-else-if="column.key === 'availableQuantity'">
          <span class="wms-quantity-cell wms-quantity-cell--available">
            {{ record.availableQuantity?.toLocaleString() ?? 0 }}
          </span>
        </template>
        <template v-else-if="column.key === 'inTransitQuantity'">
          <span class="wms-quantity-cell wms-quantity-cell--in-transit">
            {{ record.inTransitQuantity?.toLocaleString() ?? 0 }}
          </span>
        </template>
        <template v-else-if="column.key === 'damagedQuantity'">
          <span :class="['wms-quantity-cell', 'wms-quantity-cell--damaged', { 'has-value': record.damagedQuantity > 0 }]">
            {{ record.damagedQuantity?.toLocaleString() ?? 0 }}
          </span>
        </template>
        <template v-else-if="column.key === 'totalVolume'">
          <span class="wms-volume-cell">{{ formatVolume(record.totalVolume) }}</span>
        </template>
        <template v-else-if="column.key === 'operate'">
          <a @click="handleViewDetail(record)">明细</a>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ReloadOutlined } from '@ant-design/icons-vue'
import type { TableColumnsType } from 'ant-design-vue'
import type { WarehouseSummaryVO } from '@/api/wms/inventory/types'
import WarehouseDisplayCell from '@/components/Warehouse/WarehouseDisplayCell.vue'

defineOptions({ name: 'WarehouseOverviewTable' })

defineProps<{
  data: WarehouseSummaryVO[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'view-detail', warehouseId: number): void
  (e: 'refresh'): void
}>()

const columns: TableColumnsType = [
  { title: '仓库', key: 'warehouseInfo', width: 200 },
  { title: 'SKU数', dataIndex: 'skuCount', width: 80, align: 'right' },
  { title: '仓内', key: 'warehouseQuantity', width: 100, align: 'right' },
  { title: '可用', key: 'availableQuantity', width: 100, align: 'right' },
  { title: '占用', dataIndex: 'reservedQuantity', width: 100, align: 'right' },
  { title: '在途', key: 'inTransitQuantity', width: 100, align: 'right' },
  { title: '残品', key: 'damagedQuantity', width: 100, align: 'right' },
  { title: '体积 (m³)', key: 'totalVolume', width: 110, align: 'right' },
  { title: '操作', key: 'operate', width: 80, align: 'center' }
]

function formatVolume(value: number | null | undefined): string {
  if (value === null || value === undefined) return '--'
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function handleViewDetail(record: WarehouseSummaryVO) {
  emit('view-detail', record.warehouseId)
}
</script>

<style scoped>
/* 数量单元格样式 */
.wms-quantity-cell {
  font-variant-numeric: tabular-nums;
}

.wms-quantity-cell--available {
  color: var(--wms-color-available);
}

.wms-quantity-cell--in-transit {
  color: var(--wms-color-in-transit);
}

.wms-quantity-cell--damaged {
  color: var(--ant-color-text-tertiary);
}

.wms-quantity-cell--damaged.has-value {
  color: var(--wms-color-damaged);
}

.wms-volume-cell {
  font-variant-numeric: tabular-nums;
  color: var(--ant-color-text-secondary);
}

/* 表格工具栏 */
.wms-table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.wms-table-toolbar__total {
  font-size: 14px;
  color: var(--ant-color-text-tertiary);
}

.warehouse-overview-table .warehouse-info-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.warehouse-overview-table .warehouse-info-cell .name {
  font-weight: 500;
}

.warehouse-overview-table .warehouse-info-cell .code {
  font-size: 12px;
  color: var(--ant-color-text-tertiary);
  font-family: monospace;
}
</style>
