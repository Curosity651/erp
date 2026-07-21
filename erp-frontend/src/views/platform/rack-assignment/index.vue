<template>
  <location-mgmt-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="货架分配"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 720 }"
    size="middle"
  >
    <template #bodyCell="{ column, record }">
      <!-- 仓库信息 -->
      <template v-if="column.key === 'warehouseInfo'">
        <div class="wh-info-cell">
          <div class="wh-name">
            <a v-if="record.locationGenerated === 1" @click="handleManage(record)">
              {{ record.warehouseName }}
            </a>
            <span v-else>{{ record.warehouseName }}</span>
          </div>
          <div class="wh-sub">
            <span class="wh-code">{{ record.warehouseCode }}</span>
            <span class="separator">·</span>
            <span class="wh-type">{{ record.warehouseType }}</span>
          </div>
        </div>
      </template>

      <!-- 分配情况：总排数 / 已分配 / 未分配 -->
      <template v-else-if="column.key === 'rackStat'">
        <template v-if="record.locationGenerated === 1">
          <span class="rack-stat">
            总 <b>{{ record.rackTotal ?? 0 }}</b> 排 ·
            <span class="assigned">已分配 {{ record.rackAssigned ?? 0 }}</span> ·
            <span class="idle">未分配 {{ record.rackUnassigned ?? 0 }}</span>
          </span>
        </template>
        <span v-else class="muted">未设计</span>
      </template>

      <!-- 库位状态 -->
      <template v-else-if="column.key === 'genStatus'">
        <a-tag v-if="record.locationGenerated === 1" color="green">已生成</a-tag>
        <a-tag v-else color="default">未生成</a-tag>
      </template>

      <!-- 操作 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="record.locationGenerated === 1" @click="handleManage(record)">管理货架分配</a>
          <a-tooltip v-else title="请先在『库位管理』生成库位">
            <span style="color: #bfbfbf">管理货架分配</span>
          </a-tooltip>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <rack-assignment-drawer ref="drawerRef" @success="reloadTable" />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { isSuccess } from '@/api'
import { OperationGroup } from '@/components/Operation'
import { listStructureWarehouses } from '@/api/wms/location-mgmt'
import type { WarehouseStructure } from '@/api/wms/location-mgmt/types'
import { previewRacks } from '@/api/wms/rack'
import LocationMgmtSearch from '@/views/wms/location-mgmt/LocationMgmtSearch.vue'
import type { LocationMgmtQuery } from '@/views/wms/location-mgmt/LocationMgmtSearch.vue'
import RackAssignmentDrawer from './RackAssignmentDrawer.vue'

defineOptions({ name: 'RackAssignmentPage' })

type RackWarehouseRow = WarehouseStructure & {
  rackTotal?: number
  rackAssigned?: number
  rackUnassigned?: number
}

const tableRef = ref<ProTableInstanceExpose>()
const drawerRef = ref<InstanceType<typeof RackAssignmentDrawer>>()

let searchParams: LocationMgmtQuery = {}

// 仓库数量少：一次性取回后本地筛选 + 分页，保证分配后立即刷新
const tableRequest: TableRequest = async params => {
  const res = await listStructureWarehouses()
  const all: WarehouseStructure[] = isSuccess(res) ? res.data || [] : []

  const kw = searchParams
  const filtered = all.filter(w => {
    if (kw.warehouseName && !(w.warehouseName || '').includes(kw.warehouseName)) return false
    if (
      kw.warehouseCode &&
      !(w.warehouseCode || '').toLowerCase().includes(kw.warehouseCode.toLowerCase())
    ) {
      return false
    }
    if (kw.generatedStatus !== undefined && (w.locationGenerated ?? 0) !== kw.generatedStatus) {
      return false
    }
    return true
  })

  const current = params.current || 1
  const pageSize = params.pageSize || 10
  const start = (current - 1) * pageSize
  const pageRecords = filtered.slice(start, start + pageSize)

  // 汇总每个（已生成库位的）仓库的货架分配情况：总排数/已分配/未分配
  await Promise.all(
    pageRecords.map(async w => {
      const row = w as RackWarehouseRow
      if (w.locationGenerated !== 1) return
      const pr = await previewRacks(w.id)
      const racks = isSuccess(pr) ? pr.data || [] : []
      row.rackTotal = racks.length
      row.rackAssigned = racks.filter(r => r.status === 'OCCUPIED').length
      row.rackUnassigned = racks.filter(r => r.status === 'IDLE').length
    })
  )

  return {
    code: 200,
    message: '',
    data: { records: pageRecords, total: filtered.length }
  }
}

const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

const searchTable = (params: LocationMgmtQuery) => {
  searchParams = params
  reloadTable(true)
}

const handleManage = (record: WarehouseStructure) => {
  drawerRef.value?.open(record)
}

const columns: ProColumns[] = [
  { title: '仓库信息', key: 'warehouseInfo', width: 220, fixed: 'left' },
  { title: '分配情况', key: 'rackStat', width: 240 },
  { title: '库位状态', key: 'genStatus', width: 110, align: 'center' },
  { title: '操作', key: 'operate', width: 140, align: 'center', fixed: 'right' }
]
</script>

<style scoped>
.wh-info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.wh-name {
  font-weight: 600;
  font-size: 14px;
  color: #1890ff;
  cursor: pointer;
}

.wh-sub {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.wh-code {
  font-family: 'SFMono-Regular', Consolas, 'Courier New', monospace;
  color: #595959;
}

.separator {
  color: #d9d9d9;
}

.wh-type {
  color: #8c8c8c;
}

.muted {
  color: #bfbfbf;
}

.rack-stat {
  font-size: 13px;
  color: #595959;
}

.rack-stat b {
  color: #1677ff;
}

.rack-stat .assigned {
  color: #52c41a;
}

.rack-stat .idle {
  color: #8c8c8c;
}
</style>
