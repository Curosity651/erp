<template>
  <location-mgmt-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="库位管理"
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
            <a @click="handleManage(record)">{{ record.warehouseName }}</a>
          </div>
          <div class="wh-sub">
            <span class="wh-code">{{ record.warehouseCode }}</span>
            <span class="separator">·</span>
            <span class="wh-type">{{ record.warehouseType }}</span>
          </div>
        </div>
      </template>

      <!-- 结构 -->
      <template v-else-if="column.key === 'structure'">
        <span v-if="record.rackRows && record.rackColumns">
          {{ record.rackRows }} 排 × {{ record.rackColumns }} 列
        </span>
        <span v-else class="muted">未设计</span>
      </template>

      <!-- 库位数 -->
      <template v-else-if="column.key === 'locationCount'">
        <span class="count">{{ locationCount(record) }}</span>
      </template>

      <!-- 状态 -->
      <template v-else-if="column.key === 'genStatus'">
        <a-tag v-if="record.locationGenerated === 1" color="green">已生成</a-tag>
        <a-tag v-else color="default">未生成</a-tag>
      </template>

      <!-- 操作 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a @click="handleManage(record)">管理库位</a>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <warehouse-location-drawer ref="drawerRef" @success="reloadTable" />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { isSuccess } from '@/api'
import { OperationGroup } from '@/components/Operation'
import { listStructureWarehouses } from '@/api/wms/location-mgmt'
import type { WarehouseStructure } from '@/api/wms/location-mgmt/types'
import LocationMgmtSearch from './LocationMgmtSearch.vue'
import type { LocationMgmtQuery } from './LocationMgmtSearch.vue'
import WarehouseLocationDrawer from './WarehouseLocationDrawer.vue'

defineOptions({ name: 'LocationMgmtPage' })

const tableRef = ref<ProTableInstanceExpose>()
const drawerRef = ref<InstanceType<typeof WarehouseLocationDrawer>>()

let searchParams: LocationMgmtQuery = {}

const locationCount = (w: WarehouseStructure): number =>
  w.locationGenerated === 1 ? (w.rackRows || 0) * (w.rackColumns || 0) : 0

// 仓库数量很少，一次性取回后本地筛选 + 分页，保证生成/改结构后立即刷新
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
  return {
    code: 200,
    message: '',
    data: { records: filtered.slice(start, start + pageSize), total: filtered.length }
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
  { title: '结构', key: 'structure', width: 140 },
  { title: '库位数', key: 'locationCount', width: 90, align: 'center' },
  { title: '状态', key: 'genStatus', width: 100, align: 'center' },
  { title: '操作', key: 'operate', width: 120, align: 'center', fixed: 'right' }
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

.count {
  font-family: 'SFMono-Regular', Consolas, 'Courier New', monospace;
  color: #595959;
}

.muted {
  color: #bfbfbf;
}
</style>
