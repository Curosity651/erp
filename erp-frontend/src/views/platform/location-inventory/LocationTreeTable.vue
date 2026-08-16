<template>
  <a-table
    :data-source="treeRows"
    :columns="columns"
    :pagination="false"
    :scroll="{ x: 980 }"
    row-key="key"
    size="middle"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'locationCode'">
        <strong v-if="record.isRack">{{ record.locationCode }}</strong>
        <a v-else @click="$emit('select', record.locationId)">{{ record.locationCode }}</a>
      </template>
      <template v-else-if="column.key === 'capacity' && !record.isRack">
        <a-progress
          :percent="Math.min(100, record.utilizationPercent)"
          size="small"
          :status="record.volumeExceeded || record.weightExceeded ? 'exception' : 'normal'"
        />
      </template>
      <template v-else-if="column.key === 'operate' && !record.isRack">
        <a @click="$emit('select', record.locationId)">查看</a>
      </template>
    </template>
  </a-table>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { LocationInventoryGrid, LocationInventoryTree } from '@/api/wms/location-inventory/types'

interface TreeRow extends Partial<LocationInventoryGrid> {
  key: string
  locationCode: string
  isRack?: boolean
  children?: TreeRow[]
}

const props = defineProps<{ tree: LocationInventoryTree }>()
defineEmits<{ (event: 'select', locationId: number): void }>()

const treeRows = computed<TreeRow[]>(() =>
  Object.entries(props.tree).map(([rack, rows]) => ({
    key: `rack-${rack}`,
    locationCode: rack,
    isRack: true,
    totalQuantity: rows.reduce((sum, row) => sum + row.totalQuantity, 0),
    availableQuantity: rows.reduce((sum, row) => sum + row.availableQuantity, 0),
    reservedQuantity: rows.reduce((sum, row) => sum + row.reservedQuantity, 0),
    children: rows.map(row => ({ ...row, key: `location-${row.locationId}` }))
  }))
)

const columns = [
  { title: '排 / 库位', dataIndex: 'locationCode', key: 'locationCode', width: 220 },
  { title: '类型', dataIndex: 'locationType', key: 'locationType', width: 120 },
  { title: '总件数', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 100 },
  { title: '可用', dataIndex: 'availableQuantity', key: 'availableQuantity', width: 100 },
  { title: '预占', dataIndex: 'reservedQuantity', key: 'reservedQuantity', width: 100 },
  { title: 'SKU 种类', dataIndex: 'skuKindCount', key: 'skuKindCount', width: 110 },
  { title: '容量使用', key: 'capacity', width: 180 },
  { title: '操作', key: 'operate', width: 80, fixed: 'right' }
]
</script>
