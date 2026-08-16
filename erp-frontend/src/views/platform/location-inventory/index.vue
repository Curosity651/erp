<template>
  <div class="page">
    <div class="toolbar">
      <a-space wrap>
        <a-select
          v-model:value="warehouseId"
          :options="warehouseOptions"
          placeholder="选择仓库"
          style="width: 220px"
        />
        <a-segmented v-model:value="viewMode" :options="viewOptions" />
        <a-button :loading="loading" @click="load">刷新</a-button>
      </a-space>
      <a-space>
        <span>库位 {{ rows.length }}</span>
        <span>库存 {{ totalQuantity }} 件</span>
        <span>预占 {{ reservedQuantity }} 件</span>
      </a-space>
    </div>

    <a-spin :spinning="loading">
      <location-grid-view
        v-if="viewMode === 'grid'"
        :rows="rows"
        @select="showDetail"
      />
      <location-tree-table v-else :tree="tree" @select="showDetail" />
    </a-spin>

    <location-inventory-drawer ref="drawerRef" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { getWarehouseOptions } from '@/api/wms/warehouse'
import { getLocationInventoryGrid } from '@/api/wms/location-inventory'
import type {
  LocationInventoryGrid,
  LocationInventoryTree
} from '@/api/wms/location-inventory/types'
import LocationGridView from './LocationGridView.vue'
import LocationTreeTable from './LocationTreeTable.vue'
import LocationInventoryDrawer from './LocationInventoryDrawer.vue'

defineOptions({ name: 'LocationInventoryPage' })

const route = useRoute()
const loading = ref(false)
const warehouseId = ref<number>()
const warehouseOptions = ref<{ value: number; label: string }[]>([])
const viewMode = ref<'grid' | 'tree'>('grid')
const rows = ref<LocationInventoryGrid[]>([])
const drawerRef = ref<InstanceType<typeof LocationInventoryDrawer>>()
const viewOptions = [
  { label: '网格视图', value: 'grid' },
  { label: '列表视图', value: 'tree' }
]

const totalQuantity = computed(() => rows.value.reduce((sum, row) => sum + row.totalQuantity, 0))
const reservedQuantity = computed(() =>
  rows.value.reduce((sum, row) => sum + row.reservedQuantity, 0)
)
const tree = computed<LocationInventoryTree>(() =>
  rows.value.reduce<LocationInventoryTree>((groups, row) => {
    const rack = row.rackNo || '未分排'
    groups[rack] = [...(groups[rack] || []), row]
    return groups
  }, {})
)

const load = async () => {
  if (!warehouseId.value) return
  loading.value = true
  try {
    const gridResponse = await getLocationInventoryGrid(warehouseId.value)
    if (!isSuccess(gridResponse)) {
      message.error(gridResponse.message || '库位库存加载失败')
      return
    }
    rows.value = gridResponse.data || []
  } finally {
    loading.value = false
  }
}

const showDetail = (locationId: number) => drawerRef.value?.show(locationId)

watch(warehouseId, load)

onMounted(async () => {
  const response = await getWarehouseOptions()
  if (!isSuccess(response)) return
  warehouseOptions.value = (response.data || []).map(item => ({
    value: item.id,
    label: item.warehouseName
  }))
  const queryWarehouseId = Number(route.query.warehouseId)
  warehouseId.value =
    queryWarehouseId && warehouseOptions.value.some(item => item.value === queryWarehouseId)
      ? queryWarehouseId
      : warehouseOptions.value[0]?.value
})
</script>

<style scoped>
.page {
  min-height: 100%;
  padding: 20px;
  background: #fff;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

@media (max-width: 900px) {
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
