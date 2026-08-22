<template>
  <div class="page">
    <a-form layout="inline" :model="filters" class="filters">
      <a-form-item label="托盘号">
        <a-input
          v-model:value="filters.palletNo"
          allow-clear
          placeholder="输入托盘号"
          style="width: 180px"
          @press-enter="load"
        />
      </a-form-item>
      <a-form-item label="货主">
        <a-select
          v-model:value="filters.erpTenantId"
          allow-clear
          show-search
          option-filter-prop="label"
          placeholder="全部货主"
          :options="ownerOptions"
          style="width: 180px"
        />
      </a-form-item>
      <a-form-item label="服务商">
        <a-select
          v-model:value="filters.wmsTenantId"
          allow-clear
          show-search
          option-filter-prop="label"
          placeholder="全部服务商"
          :options="operatorOptions"
          style="width: 180px"
        />
      </a-form-item>
      <a-form-item label="状态">
        <a-select
          v-model:value="filters.status"
          allow-clear
          placeholder="全部状态"
          :options="statusOptions"
          style="width: 130px"
        />
      </a-form-item>
      <a-form-item label="仓库">
        <a-select
          v-model:value="filters.warehouseId"
          allow-clear
          show-search
          option-filter-prop="label"
          placeholder="全部仓库"
          :options="warehouseOptions"
          style="width: 190px"
        />
      </a-form-item>
      <a-form-item label="层位">
        <a-input
          v-model:value="filters.slotCode"
          allow-clear
          placeholder="输入层位编码"
          style="width: 180px"
          @press-enter="load"
        />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" :loading="loading" @click="load">查询</a-button>
          <a-button @click="resetFilters">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      row-key="id"
      :loading="loading"
      :data-source="rows"
      :columns="columns"
      :pagination="pagination"
      :scroll="{ x: 1200 }"
      size="middle"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
          <a-tag :color="typeColor(record.palletType)">{{ typeText(record.palletType) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'goods'">
          <div class="goods-cell">
            <span v-for="item in record.items" :key="`${item.erpTenantId}-${item.skuCode}`">
              {{ item.skuCode }} × {{ item.quantity }}
            </span>
          </div>
        </template>
        <template v-else-if="column.key === 'owner'">
          <div>{{ record.ownerName || '-' }}</div>
          <div class="secondary">{{ record.wmsTenantName || '-' }}</div>
        </template>
        <template v-else-if="column.key === 'capacity'">
          <a-progress
            v-if="record.capacityPercent"
            :percent="Math.min(record.capacityPercent, 100)"
            size="small"
            :stroke-color="record.capacityPercent >= 100 ? '#52c41a' : '#1677ff'"
          />
          <a-tag v-else color="orange">待校准</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-badge
            :status="statusColor(record.palletStatus)"
            :text="statusText(record.palletStatus)"
          />
        </template>
        <template v-else-if="column.key === 'operate'">
          <a-space size="small">
            <a @click="showDetail(record)">查看</a>
            <a v-if="record.palletStatus !== 'CLOSED'" @click="openCapacity(record)">校准</a>
            <a @click="printLabel(record)">打印</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-drawer v-model:open="detailOpen" title="托盘详情" :width="620">
      <a-descriptions v-if="current" :column="2" bordered size="small">
        <a-descriptions-item label="托盘号">{{ current.palletNo }}</a-descriptions-item>
        <a-descriptions-item label="货主">{{ current.ownerName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="WMS服务商">{{
          current.wmsTenantName || '-'
        }}</a-descriptions-item>
        <a-descriptions-item label="层位">{{ current.slotCode || '-' }}</a-descriptions-item>
        <a-descriptions-item label="类型">{{ typeText(current.palletType) }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{
          statusText(current.palletStatus)
        }}</a-descriptions-item>
        <a-descriptions-item label="容量">{{
          current.capacityPercent ? `${current.capacityPercent}%` : '待校准'
        }}</a-descriptions-item>
        <a-descriptions-item label="重量">{{
          current.actualWeightKg ? `${current.actualWeightKg} kg` : '-'
        }}</a-descriptions-item>
      </a-descriptions>
      <a-table
        v-if="current"
        :data-source="current.items"
        :columns="itemColumns"
        :pagination="false"
        row-key="skuCode"
        size="small"
        class="detail-table"
      />
    </a-drawer>

    <a-modal
      v-model:open="capacityOpen"
      title="校准托盘容量"
      :confirm-loading="saving"
      @ok="saveCapacity"
    >
      <a-form :model="capacity" layout="vertical">
        <a-form-item label="现场占用比例" required>
          <a-slider v-model:value="capacity.capacityPercent" :min="1" :max="100" :marks="marks" />
        </a-form-item>
        <a-form-item label="实际重量">
          <a-input-number
            v-model:value="capacity.actualWeightKg"
            :min="0"
            :precision="2"
            addon-after="kg"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="现场已满">
          <a-switch v-model:checked="capacity.markFull" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { getWarehouseOptions } from '@/api/wms/warehouse'
import { calibratePallet, listPallets } from '@/api/wms/pallet'
import type { PalletSummaryVO } from '@/api/wms/pallet'
import { listAllErpTenants, listWmsOperators } from '@/api/tenant'
import { printPalletLabels } from './pallet-label-print'

const loading = ref(false)
const saving = ref(false)
const rows = ref<PalletSummaryVO[]>([])
const current = ref<PalletSummaryVO>()
const detailOpen = ref(false)
const capacityOpen = ref(false)
const warehouseOptions = ref<{ value: number; label: string }[]>([])
const ownerOptions = ref<{ value: number; label: string }[]>([])
const operatorOptions = ref<{ value: number; label: string }[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 20,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: (total: number) => `共 ${total} 条`
})
const filters = reactive<{
  palletNo?: string
  erpTenantId?: number
  wmsTenantId?: number
  status?: string
  warehouseId?: number
  slotCode?: string
}>({})
const capacity = reactive<{
  palletId?: number
  capacityPercent: number
  actualWeightKg?: number
  markFull: boolean
}>({ capacityPercent: 50, markFull: false })
const marks = { 25: '25%', 50: '50%', 75: '75%', 100: '满' }

const statusOptions = [
  { value: 'PARTIAL', label: '半托' },
  { value: 'FULL', label: '满托' },
  { value: 'CLOSED', label: '已关闭' }
]
const columns = [
  { title: '托盘号', dataIndex: 'palletNo', key: 'palletNo', width: 210, fixed: 'left' },
  { title: '货主 / 服务商', key: 'owner', width: 160 },
  { title: '仓库 / 层位', dataIndex: 'slotCode', key: 'slotCode', width: 160 },
  { title: '类型', key: 'type', width: 110 },
  { title: '货物', key: 'goods', width: 240 },
  { title: '容量', key: 'capacity', width: 180 },
  { title: '状态', key: 'status', width: 110 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'operate', width: 150, fixed: 'right' }
]
const itemColumns = [
  { title: '货主', dataIndex: 'ownerName', key: 'ownerName' },
  { title: 'SKU', dataIndex: 'skuCode', key: 'skuCode' },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '预留', dataIndex: 'reservedQty', key: 'reservedQty', width: 80 }
]

async function load() {
  pagination.current = 1
  loading.value = true
  try {
    const response = await listPallets(filters)
    if (isSuccess(response)) rows.value = response.data || []
  } finally {
    loading.value = false
  }
}

function handleTableChange(next: { current?: number; pageSize?: number }) {
  pagination.current = next.current || 1
  pagination.pageSize = next.pageSize || 20
}

async function resetFilters() {
  filters.palletNo = undefined
  filters.erpTenantId = undefined
  filters.wmsTenantId = undefined
  filters.status = undefined
  filters.warehouseId = undefined
  filters.slotCode = undefined
  await load()
}

function showDetail(record: PalletSummaryVO) {
  current.value = record
  detailOpen.value = true
}
function openCapacity(record: PalletSummaryVO) {
  current.value = record
  capacity.palletId = record.id
  capacity.capacityPercent = record.capacityPercent || 50
  capacity.actualWeightKg = record.actualWeightKg
  capacity.markFull = record.palletStatus === 'FULL'
  capacityOpen.value = true
}
async function saveCapacity() {
  if (!capacity.palletId) return
  saving.value = true
  try {
    const response = await calibratePallet({
      palletId: capacity.palletId,
      capacityPercent: capacity.capacityPercent,
      actualWeightKg: capacity.actualWeightKg,
      markFull: capacity.markFull
    })
    if (isSuccess(response)) {
      message.success('托盘容量已更新')
      capacityOpen.value = false
      await load()
    }
  } finally {
    saving.value = false
  }
}

function printLabel(record: PalletSummaryVO) {
  void printPalletLabels([record])
}

function typeText(value: string) {
  return (
    (
      { SINGLE_FULL: '单品满托', SINGLE_PARTIAL: '单品半托', MIXED: '混托' } as Record<
        string,
        string
      >
    )[value] || value
  )
}
function typeColor(value: string) {
  return (
    ({ SINGLE_FULL: 'green', SINGLE_PARTIAL: 'blue', MIXED: 'orange' } as Record<string, string>)[
      value
    ] || 'default'
  )
}
function statusText(value: string) {
  return (
    (
      {
        PARTIAL: '半托',
        FULL: '满托',
        CLOSED: '已关闭'
      } as Record<string, string>
    )[value] || value
  )
}
function statusColor(value: string) {
  return value === 'FULL'
    ? 'success'
    : value === 'CLOSED'
      ? 'default'
      : 'processing'
}

onMounted(async () => {
  const [warehouseResponse, ownerResponse, operatorResponse] = await Promise.all([
    getWarehouseOptions(),
    listAllErpTenants(),
    listWmsOperators()
  ])
  if (isSuccess(warehouseResponse))
    warehouseOptions.value = (warehouseResponse.data || []).map(item => ({
      value: item.id,
      label: item.warehouseName
    }))
  if (isSuccess(ownerResponse))
    ownerOptions.value = (ownerResponse.data || []).map(item => ({
      value: item.id,
      label: `${item.tenantName}（${item.tenantCode}）`
    }))
  if (isSuccess(operatorResponse))
    operatorOptions.value = (operatorResponse.data || []).map(item => ({
      value: item.id,
      label: `${item.tenantName}（${item.tenantCode}）`
    }))
  await load()
})
</script>

<style scoped>
.page {
  background: #fff;
  padding: 16px;
  min-height: 100%;
}
.filters {
  margin-bottom: 16px;
}
.goods-cell {
  display: grid;
  gap: 2px;
}
.detail-table {
  margin-top: 16px;
}
.secondary {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
</style>
