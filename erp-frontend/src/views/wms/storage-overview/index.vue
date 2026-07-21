<template>
  <div class="storage-overview">
    <div class="page-head">
      <div>
        <h2 class="title">仓储概览</h2>
        <div class="sub">查看名下海外仓的仓库、被分配的容量与占用（只读）</div>
      </div>
      <a-button size="small" :loading="loading" @click="loadSummary">刷新</a-button>
    </div>

    <!-- ① 汇总卡 -->
    <a-row :gutter="[12, 12]" class="tiles">
      <a-col :xs="12" :sm="8" :md="4">
        <div class="tile"><div class="k">我的仓库数</div><div class="v">{{ fmt(overview.warehouseCount) }}</div></div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="4">
        <div class="tile"><div class="k">分配货架数</div><div class="v">{{ fmt(overview.rackCount) }}</div></div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="4">
        <div class="tile"><div class="k">分配库位数</div><div class="v">{{ fmt(overview.allocatedLocations) }}</div></div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="4">
        <div class="tile"><div class="k">已占用库位</div><div class="v">{{ fmt(overview.occupiedLocations) }}</div></div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="4">
        <div class="tile hl"><div class="k">占用率</div><div class="v">{{ pct(overview.occupancyRate) }}</div></div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="4">
        <div class="tile"><div class="k">在库总件数</div><div class="v">{{ fmt(overview.onHandQty) }}</div></div>
      </a-col>
    </a-row>

    <!-- ② 仓库列表 -->
    <a-card size="small" class="wh-card" :bordered="true">
      <template #title>仓库列表 <span class="hint">点任意行查看该仓的货架 / 货主明细</span></template>
      <a-table
        :columns="whColumns"
        :data-source="overview.warehouses"
        :loading="loading"
        row-key="warehouseId"
        :pagination="false"
        size="middle"
        :custom-row="rowClick"
        :row-class-name="() => 'row-link'"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="wh-name">{{ record.warehouseName }}</div>
            <div class="wh-sub">{{ record.regionName || '—' }} · {{ record.warehouseCode }}</div>
          </template>
          <template v-else-if="column.key === 'occ'">
            <div class="occ">
              <a-progress
                :percent="Number(record.occupancyRate)"
                :show-info="false"
                :stroke-color="barColor(record.occupancyRate)"
                :size="[null, 7]"
              />
              <span class="pct" :class="{ warn: isFull(record.occupancyRate) }">{{ pct(record.occupancyRate) }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'chev'"><span class="chev">›</span></template>
        </template>
      </a-table>
    </a-card>

    <StorageWarehouseDrawer ref="drawerRef" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { isSuccess } from '@/api'
import { getStorageSummary } from '@/api/wms/storage-overview'
import type { StorageOverview, StorageWarehouse } from '@/api/wms/storage-overview/types'
import StorageWarehouseDrawer from './StorageWarehouseDrawer.vue'

const loading = ref(false)
const overview = reactive<StorageOverview>({
  warehouseCount: 0,
  rackCount: 0,
  allocatedLocations: 0,
  occupiedLocations: 0,
  occupancyRate: 0,
  onHandQty: 0,
  warehouses: []
})

const drawerRef = ref<InstanceType<typeof StorageWarehouseDrawer>>()

const whColumns = [
  { title: '仓库 / 区域', key: 'name' },
  { title: '分配货架', dataIndex: 'rackCount', align: 'right', width: 96 },
  { title: '分配库位', dataIndex: 'allocatedLocations', align: 'right', width: 96 },
  { title: '已占用', dataIndex: 'occupiedLocations', align: 'right', width: 88 },
  { title: '占用率', key: 'occ', width: 200 },
  { title: '在库件数', dataIndex: 'onHandQty', align: 'right', width: 110 },
  { title: '', key: 'chev', width: 40, align: 'right' }
]

const fmt = (n?: number) => (n == null ? '0' : Number(n).toLocaleString('en-US'))
const pct = (n?: number) => `${n == null ? 0 : n}%`
const isFull = (n?: number) => Number(n) >= 95
const barColor = (n?: number) => (isFull(n) ? '#b0803a' : '#546a90')

const rowClick = (record: StorageWarehouse) => ({
  onClick: () => drawerRef.value?.open(record)
})

async function loadSummary() {
  loading.value = true
  try {
    const res = await getStorageSummary()
    if (isSuccess(res) && res.data) {
      Object.assign(overview, res.data)
    }
  } catch (e) {
    console.error('加载仓储概览失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(loadSummary)
</script>

<style scoped>
.storage-overview {
  padding: 16px;
}
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 14px;
}
.title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}
.sub {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}
.tiles {
  margin-bottom: 14px;
}
.tile {
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 8px;
  padding: 12px 14px;
}
.tile .k {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 6px;
  white-space: nowrap;
}
.tile .v {
  font-size: 22px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
}
.tile.hl {
  border-color: #546a90;
}
.tile.hl .v {
  color: #546a90;
}
.wh-card :deep(.row-link) {
  cursor: pointer;
}
.wh-name {
  font-weight: 600;
}
.wh-sub {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
.hint {
  font-size: 12px;
  font-weight: 400;
  color: rgba(0, 0, 0, 0.4);
  margin-left: 8px;
}
.occ {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pct {
  font-variant-numeric: tabular-nums;
  width: 52px;
  text-align: right;
  flex: none;
}
.pct.warn {
  color: #b0803a;
}
.chev {
  color: #546a90;
  font-weight: 700;
  font-size: 16px;
}
</style>
