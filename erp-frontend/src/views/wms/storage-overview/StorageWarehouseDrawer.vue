<template>
  <a-drawer
    v-model:open="visible"
    :width="720"
    :title="title"
    placement="right"
  >
    <div v-if="wh" class="dh-meta">
      {{ wh.regionName || '—' }} · 分配 {{ wh.rackCount }} 架 / {{ wh.allocatedLocations }} 位 · 占用
      {{ wh.occupancyRate }}%
    </div>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="racks" tab="货架维度">
        <a-table
          :columns="rackColumns"
          :data-source="racks"
          :loading="loadingRacks"
          row-key="rackNo"
          :pagination="false"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'rackNo'">
              <span class="rack-tag">{{ record.rackNo }}</span>
            </template>
            <template v-else-if="column.key === 'occ'">
              <div class="occ">
                <a-progress
                  :percent="Number(record.occupancyRate)"
                  :show-info="false"
                  :stroke-color="barColor(record.occupancyRate)"
                  :size="[null, 7]"
                />
                <span class="pct" :class="{ warn: isFull(record.occupancyRate) }">{{ record.occupancyRate }}%</span>
              </div>
            </template>
            <template v-else-if="column.key === 'fee'">
              {{ record.monthlyFee == null ? '—' : '¥' + Number(record.monthlyFee).toLocaleString('en-US') }}
            </template>
            <template v-else-if="column.key === 'to'">
              <span>{{ record.effectiveTo || '长期' }}</span>
              <a-tag v-if="record.expiringSoon" color="orange" class="ml">临期</a-tag>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <a-tab-pane key="owners" tab="货主维度">
        <a-table
          :columns="ownerColumns"
          :data-source="owners"
          :loading="loadingOwners"
          row-key="erpTenantId"
          :pagination="false"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'owner'">
              <span class="owner-tag">{{ record.ownerName || ('租户' + record.erpTenantId) }}</span>
              <span class="owner-sub">货主 · 租户{{ record.erpTenantId }}</span>
            </template>
            <template v-else-if="column.key === 'share'">
              <div class="occ">
                <a-progress
                  :percent="Number(record.sharePct)"
                  :show-info="false"
                  stroke-color="#546a90"
                  :size="[null, 7]"
                />
                <span class="pct">{{ record.sharePct }}%</span>
              </div>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>
  </a-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { getWarehouseRacks, getWarehouseOwners } from '@/api/wms/storage-overview'
import type { StorageWarehouse, StorageRack, StorageOwner } from '@/api/wms/storage-overview/types'

const visible = ref(false)
const wh = ref<StorageWarehouse | null>(null)
const activeTab = ref<'racks' | 'owners'>('racks')

const racks = ref<StorageRack[]>([])
const owners = ref<StorageOwner[]>([])
const loadingRacks = ref(false)
const loadingOwners = ref(false)

const title = computed(() => (wh.value ? wh.value.warehouseName : '仓库明细'))

const rackColumns = [
  { title: '货架', key: 'rackNo', width: 90 },
  { title: '库位数', dataIndex: 'locationCount', align: 'right', width: 80 },
  { title: '已占用', dataIndex: 'occupiedCount', align: 'right', width: 80 },
  { title: '占用率', key: 'occ', width: 190 },
  { title: '月租', key: 'fee', align: 'right', width: 96 },
  { title: '到期日', key: 'to', width: 130 }
]

const ownerColumns = [
  { title: '货主', key: 'owner', width: 180 },
  { title: '占用库位', dataIndex: 'occupiedLocations', align: 'right', width: 90 },
  { title: '在库件数', dataIndex: 'onHandQty', align: 'right', width: 100 },
  { title: 'SKU 数', dataIndex: 'skuCount', align: 'right', width: 80 },
  { title: '占本仓比例', key: 'share', width: 190 }
]

const isFull = (n?: number) => Number(n) >= 95
const barColor = (n?: number) => (isFull(n) ? '#b0803a' : '#546a90')

async function loadRacks(id: number) {
  loadingRacks.value = true
  try {
    const res = await getWarehouseRacks(id)
    racks.value = res.data || []
  } finally {
    loadingRacks.value = false
  }
}

async function loadOwners(id: number) {
  loadingOwners.value = true
  try {
    const res = await getWarehouseOwners(id)
    owners.value = res.data || []
  } finally {
    loadingOwners.value = false
  }
}

function open(record: StorageWarehouse) {
  wh.value = record
  activeTab.value = 'racks'
  racks.value = []
  owners.value = []
  visible.value = true
  loadRacks(record.warehouseId)
  loadOwners(record.warehouseId)
}

defineExpose({ open })
</script>

<style scoped>
.dh-meta {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.5);
  margin-bottom: 8px;
}
.rack-tag,
.owner-tag {
  font-weight: 600;
}
.owner-sub {
  font-size: 12px;
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
.ml {
  margin-left: 6px;
}
</style>
