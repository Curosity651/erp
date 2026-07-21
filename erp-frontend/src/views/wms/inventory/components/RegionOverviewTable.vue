<template>
  <a-table
    :data-source="data"
    :columns="columns"
    :loading="loading"
    :pagination="false"
    row-key="regionId"
    size="middle"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'regionAvailable'">
        <span :class="record.regionAvailable < 0 ? 'text-danger' : ''">
          {{ record.regionAvailable }}
        </span>
      </template>
      <template v-else-if="column.key === 'operate'">
        <a @click="$emit('view-detail', record.regionId)">查看明细</a>
      </template>
    </template>
  </a-table>
</template>

<script setup lang="ts">
import type { RegionSummaryVO } from '@/api/wms/inventory/types'

defineOptions({ name: 'RegionOverviewTable' })

defineProps<{
  data: RegionSummaryVO[]
  loading: boolean
}>()

defineEmits<{
  (e: 'view-detail', regionId: number): void
}>()

const columns = [
  { title: '区域名称', dataIndex: 'regionName', key: 'regionName', width: 120 },
  { title: '自有仓数', dataIndex: 'ownWarehouseCount', key: 'ownWarehouseCount', width: 100, align: 'center' },
  { title: '区域可售', dataIndex: 'regionAvailable', key: 'regionAvailable', width: 100, align: 'right' },
  { title: '区域预占', dataIndex: 'regionReserved', key: 'regionReserved', width: 100, align: 'right' },
  { title: '区域在途', dataIndex: 'regionInTransit', key: 'regionInTransit', width: 100, align: 'right' },
  { title: '区域残品', dataIndex: 'regionDamaged', key: 'regionDamaged', width: 100, align: 'right' },
  { title: '操作', key: 'operate', width: 100, align: 'center' }
]
</script>

<style scoped>
.text-danger {
  color: #ff4d4f;
  font-weight: 500;
}
</style>
