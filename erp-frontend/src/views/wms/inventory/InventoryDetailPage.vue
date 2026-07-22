<template>
  <inventory-detail-search :loading="tableRef?.loading" @search="handleSearch" />

  <pro-table
    ref="tableRef"
    header-title="库存明细"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 800 }"
    size="middle"
  >
    <!-- 工具栏 -->
    <template #toolBarRender>
      <a-button @click="handleExport">
        <download-outlined />
        导出
      </a-button>
    </template>

    <template #bodyCell="{ column, record }">
      <!-- 仓库信息列 -->
      <template v-if="column.key === 'warehouseInfo'">
        <warehouse-display-cell :display="record.warehouseDisplay" />
      </template>

      <!-- SKU信息列 -->
      <template v-else-if="column.key === 'skuInfo'">
        <sku-brief-cell
          :brief="record.skuBrief"
          class="clickable"
          @click="handleViewFlows(record)"
        />
      </template>

      <!-- 库存数量列 -->
      <template v-else-if="column.key === 'warehouseQuantity'">
        <span class="quantity-cell">{{ record.warehouseQuantity?.toLocaleString() ?? 0 }}</span>
      </template>
      <template v-else-if="column.key === 'availableQuantity'">
        <span :class="['quantity-cell', 'available', { negative: record.availableQuantity < 0 }]">
          {{ record.availableQuantity?.toLocaleString() ?? 0 }}
        </span>
      </template>
      <template v-else-if="column.key === 'reservedQuantity'">
        <span class="quantity-cell reserved">
          {{ record.reservedQuantity?.toLocaleString() ?? 0 }}
        </span>
      </template>
      <template v-else-if="column.key === 'inTransitQuantity'">
        <span class="quantity-cell in-transit">
          {{ record.inTransitQuantity?.toLocaleString() ?? 0 }}
        </span>
      </template>
      <template v-else-if="column.key === 'damagedQuantity'">
        <span :class="['quantity-cell', 'damaged', { 'has-value': record.damagedQuantity > 0 }]">
          {{ record.damagedQuantity?.toLocaleString() ?? 0 }}
        </span>
      </template>

      <!-- 操作列 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a @click="handleViewFlows(record)">查看</a>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 流水抽屉 -->
  <stock-flow-drawer ref="flowDrawerRef" />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { DownloadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { OperationGroup } from '@/components/Operation'
import { mergePageParam } from '@/utils/page-utils'
import { pageInventory } from '@/api/wms/inventory'
import type { InventoryPageVO, InventoryQO } from '@/api/wms/inventory/types'
import { SkuBriefCell } from '@/components/Sku'
import WarehouseDisplayCell from '@/components/Warehouse/WarehouseDisplayCell.vue'
import InventoryDetailSearch from './components/InventoryDetailSearch.vue'
import StockFlowDrawer from './components/StockFlowDrawer.vue'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'

defineOptions({ name: 'InventoryDetailPage' })

const tableRef = ref<ProTableInstanceExpose>()
const flowDrawerRef = ref<InstanceType<typeof StockFlowDrawer>>()
const route = useRoute()

useTableActivateReload(() => tableRef.value?.actionRef?.reload(false))

function routeNumber(value: unknown) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

// 从库存总览跳转时沿用区域/仓库/SKU筛选，保证汇总与明细口径一致。
let searchParams: InventoryQO = {
  regionId: routeNumber(route.query.regionId),
  warehouseId: routeNumber(route.query.warehouseId),
  skuCode: typeof route.query.skuCode === 'string' ? route.query.skuCode : undefined
}

const columns: ProColumns[] = [
  { title: '仓库信息', key: 'warehouseInfo', width: 120, fixed: 'left' },
  { title: 'SKU信息', key: 'skuInfo', width: 200 },
  {
    title: '仓内',
    key: 'warehouseQuantity',
    dataIndex: 'warehouseQuantity',
    width: 60,
    align: 'right'
  },
  {
    title: '可用',
    key: 'availableQuantity',
    dataIndex: 'availableQuantity',
    width: 60,
    align: 'right',
    sorter: true
  },
  {
    title: '占用',
    key: 'reservedQuantity',
    dataIndex: 'reservedQuantity',
    width: 60,
    align: 'right'
  },
  {
    title: '在途',
    key: 'inTransitQuantity',
    dataIndex: 'inTransitQuantity',
    width: 60,
    align: 'right'
  },
  {
    title: '残品',
    key: 'damagedQuantity',
    dataIndex: 'damagedQuantity',
    width: 60,
    align: 'right'
  },
  { title: '更新时间', dataIndex: 'updateTime', width: 160 },
  { title: '操作', key: 'operate', align: 'center', width: 80, fixed: 'right' }
]

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageInventory({
    ...pageParam,
    ...searchParams
  })
}

function handleSearch(params: InventoryQO) {
  searchParams = params
  tableRef.value?.actionRef?.reload(true)
}

function handleViewFlows(record: InventoryPageVO) {
  const skuCode = record.skuBrief?.skuCode || record.skuCode
  const warehouseName = record.warehouseDisplay?.warehouseName || record.warehouseName
  flowDrawerRef.value?.open(record.warehouseId, skuCode, warehouseName)
}

function handleExport() {
  message.info('导出功能开发中...')
}

defineExpose({
  reload: () => tableRef.value?.actionRef?.reload()
})
</script>

<style scoped>
.clickable {
  cursor: pointer;
}

.quantity-cell {
  font-variant-numeric: tabular-nums;
}

.quantity-cell.available {
  color: var(--ant-color-success);
}

.quantity-cell.available.negative {
  color: var(--ant-color-error);
}

.quantity-cell.reserved {
  color: var(--ant-color-warning);
}

.quantity-cell.in-transit {
  color: #722ed1;
}

.quantity-cell.damaged {
  color: var(--ant-color-text-tertiary);
}

.quantity-cell.damaged.has-value {
  color: var(--ant-color-error);
}
</style>
