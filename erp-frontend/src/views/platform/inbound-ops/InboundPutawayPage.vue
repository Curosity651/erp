<template>
  <a-card :bordered="false" style="margin-bottom: 16px">
    <a-form :model="searchModel" layout="inline" class="inbound-search">
      <a-form-item label="入库单号">
        <a-input
          v-model:value="searchModel.inboundNo"
          placeholder="请输入"
          allow-clear
          style="width: 150px"
        />
      </a-form-item>
      <a-form-item label="状态">
        <a-select
          v-model:value="searchModel.orderStatus"
          placeholder="全部"
          allow-clear
          :options="statusOptions"
          style="width: 110px"
        />
      </a-form-item>
      <a-form-item label="日期范围">
        <a-range-picker
          v-model:value="dateRange"
          value-format="YYYY-MM-DD"
          style="width: 220px"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="服务商">
        <wms-operator-select
          v-model:value="searchModel.wmsTenantId"
          placeholder="全部"
          width="140px"
          @change="onOperatorChange"
        />
      </a-form-item>
      <a-form-item label="货主">
        <platform-owner-select
          v-model:value="searchModel.erpTenantId"
          placeholder="全部"
          width="140px"
          :operator-id="searchModel.wmsTenantId"
        />
      </a-form-item>
      <a-form-item class="search-actions-item">
        <search-actions :loading="tableRef?.loading" @search="searchTable" @reset="resetSearch" />
      </a-form-item>
    </a-form>
  </a-card>

  <pro-table
    ref="tableRef"
    header-title="入库上架"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1100 }"
    size="middle"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'status'">
        <inbound-status-badge :status="record.orderStatus" />
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="record.orderStatus === InboundStatus.RECEIVED" @click="openPutaway(record)">
            上架
          </a>
          <span v-else style="color: rgba(0, 0, 0, 0.25)">已上架</span>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 上架作业抽屉 -->
  <putaway-drawer ref="putawayDrawerRef" @success="reloadTable" />
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { OperationGroup } from '@/components/Operation'
import { SearchActions } from '@/components/Search'
import { mergePageParam } from '@/utils/page-utils'
import { pageInboundOps } from '@/api/wms/inbound-execution'
import type { PurchaseInboundPageVO, PurchaseInboundQO } from '@/api/wms/purchase-inbound/types'
import { InboundStatus, InboundStatusMap } from '@/api/wms/purchase-inbound/types'
import InboundStatusBadge from '@/views/wms/purchase-inbound/components/InboundStatusBadge.vue'
import WmsOperatorSelect from '@/components/Lov/WmsOperatorSelect.vue'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import PutawayDrawer from './PutawayDrawer.vue'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'

const tableRef = ref<ProTableInstanceExpose>()

// 上架页覆盖「已收货/已完成」：上架后单据保留在本页（状态变已完成），只是上架动作不再可点
const PUTAWAY_SCOPE = [InboundStatus.RECEIVED, InboundStatus.COMPLETED]
const statusOptions = PUTAWAY_SCOPE.map(s => ({ value: s, label: InboundStatusMap[s] }))

const searchModel = reactive<PurchaseInboundQO>({
  inboundNo: undefined,
  orderStatus: undefined,
  wmsTenantId: undefined,
  erpTenantId: undefined
})
// 日期范围（[开始, 结束]，value-format 已转字符串）
const dateRange = ref<[string, string]>()
let searchParams: PurchaseInboundQO = {}

// 服务商变更时清空货主（避免残留跨服务商的货主选择）
const onOperatorChange = () => {
  searchModel.erpTenantId = undefined
}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  const statusFilter = searchParams.orderStatus
    ? { orderStatus: searchParams.orderStatus }
    : { orderStatuses: PUTAWAY_SCOPE }
  return pageInboundOps({
    ...pageParam,
    inboundNo: searchParams.inboundNo,
    wmsTenantId: searchParams.wmsTenantId,
    erpTenantId: searchParams.erpTenantId,
    inboundDateStart: searchParams.inboundDateStart,
    inboundDateEnd: searchParams.inboundDateEnd,
    ...statusFilter
  })
}
const reloadTable = (resetPageIndex?: boolean) => tableRef.value?.actionRef?.reload(resetPageIndex)

useTableActivateReload(() => reloadTable(false))
const searchTable = () => {
  searchParams = {
    ...searchModel,
    inboundDateStart: dateRange.value?.[0],
    inboundDateEnd: dateRange.value?.[1]
  }
  reloadTable(true)
}
const resetSearch = () => {
  searchModel.inboundNo = undefined
  searchModel.orderStatus = undefined
  searchModel.wmsTenantId = undefined
  searchModel.erpTenantId = undefined
  dateRange.value = undefined
  searchTable()
}

const columns: ProColumns[] = [
  { title: '入库单号', dataIndex: 'inboundNo', key: 'inboundNo', width: 220, fixed: 'left' },
  { title: '货主', dataIndex: 'ownerName', key: 'ownerName', width: 140, ellipsis: true },
  { title: '服务商', dataIndex: 'operatorName', key: 'operatorName', width: 140, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 180 },
  { title: '状态', key: 'status', width: 110, align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'operate', width: 120, align: 'center', fixed: 'right' }
]

const putawayDrawerRef = ref<InstanceType<typeof PutawayDrawer>>()
const openPutaway = (record: PurchaseInboundPageVO) => putawayDrawerRef.value?.open(record)
</script>

<script lang="ts">
export default {
  name: 'InboundPutawayPage'
}
</script>

<style scoped>
/* 搜索栏：所有筛选项一排排列，查询/重置按钮靠右对齐 */
.inbound-search {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  row-gap: 8px;
}
.inbound-search :deep(.ant-form-item) {
  margin-right: 12px;
}
.inbound-search .search-actions-item {
  margin-left: auto;
  margin-right: 0;
}
</style>
