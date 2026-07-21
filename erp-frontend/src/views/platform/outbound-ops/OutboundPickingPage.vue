<template>
  <!-- 搜索 -->
  <a-card :bordered="false" style="margin-bottom: 16px">
    <a-form :model="searchModel" layout="inline" class="outbound-search">
      <a-form-item label="出库单号">
        <a-input
          v-model:value="searchModel.outboundNo"
          placeholder="请输入"
          allow-clear
          style="width: 150px"
        />
      </a-form-item>
      <a-form-item label="状态">
        <a-select
          v-model:value="searchModel.status"
          :options="OUTBOUND_STATUS_OPTIONS"
          placeholder="全部"
          allow-clear
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
    header-title="下架(拣货)"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1320 }"
    size="middle"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'skuSummary'">
        {{ record.skuKinds }} 种 · {{ record.totalQty }} 件
      </template>
      <template v-else-if="column.key === 'pickMode'">
        {{ record.pickMode ? PICK_MODE_TEXT[record.pickMode as PickMode] : '—' }}
      </template>
      <template v-else-if="column.key === 'status'">
        <a-badge
          :status="(OUTBOUND_STATUS_BADGE[record.status as OutboundStatus] as any) || 'default'"
          :text="OUTBOUND_STATUS_TEXT[record.status as OutboundStatus] || record.status"
        />
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="record.status === 'PENDING'" @click="openPick(record)">下架</a>
          <a
            v-else-if="record.status === 'PICKING' || record.status === 'PICKED'"
            @click="openPickList(record)"
            >拣货单</a
          >
          <a
            v-else-if="record.status === 'BACKORDER'"
            style="color: #ff4d4f"
            @click="openPick(record)"
            >缺货详情</a
          >
          <span v-else style="color: rgba(0, 0, 0, 0.25)">—</span>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 下架作业弹窗 -->
  <PickModal v-model:open="pickOpen" :order-id="currentId" @success="reloadTable" />

  <!-- 拣货单抽屉 -->
  <PickListDrawer v-model:open="pickListOpen" :order-id="currentId" />
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { OperationGroup } from '@/components/Operation'
import { SearchActions } from '@/components/Search'
import WmsOperatorSelect from '@/components/Lov/WmsOperatorSelect.vue'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import { mergePageParam } from '@/utils/page-utils'
import { pagePickingOrders } from '@/api/wms/outbound-picking'
import type {
  OutboundOrderVO,
  OutboundPickingQO,
  OutboundStatus,
  PickMode
} from '@/api/wms/outbound-picking/types'
import {
  OUTBOUND_STATUS_TEXT,
  OUTBOUND_STATUS_BADGE,
  OUTBOUND_STATUS_OPTIONS,
  PICK_MODE_TEXT
} from './constants'
import PickModal from './PickModal.vue'
import PickListDrawer from './PickListDrawer.vue'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'

const tableRef = ref<ProTableInstanceExpose>()

const searchModel = reactive<OutboundPickingQO>({
  outboundNo: undefined,
  status: undefined,
  wmsTenantId: undefined,
  erpTenantId: undefined
})
// 日期范围（[开始, 结束]，value-format 已转字符串）
const dateRange = ref<[string, string]>()
let searchParams: OutboundPickingQO = {}

// 服务商变更时清空货主（避免残留跨服务商的货主选择）
const onOperatorChange = () => {
  searchModel.erpTenantId = undefined
}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pagePickingOrders(pageParam, searchParams)
}
const reloadTable = (resetPageIndex?: boolean) => tableRef.value?.actionRef?.reload(resetPageIndex)

useTableActivateReload(() => reloadTable(false))
const searchTable = () => {
  searchParams = {
    ...searchModel,
    createTimeStart: dateRange.value?.[0],
    createTimeEnd: dateRange.value?.[1]
  }
  reloadTable(true)
}
const resetSearch = () => {
  searchModel.outboundNo = undefined
  searchModel.status = undefined
  searchModel.wmsTenantId = undefined
  searchModel.erpTenantId = undefined
  dateRange.value = undefined
  searchTable()
}

const columns: ProColumns[] = [
  { title: '出库单号', dataIndex: 'outboundNo', key: 'outboundNo', width: 190, fixed: 'left' },
  { title: '货主', dataIndex: 'ownerName', key: 'ownerName', width: 140, ellipsis: true },
  { title: '服务商', dataIndex: 'operatorName', key: 'operatorName', width: 140, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 140 },
  { title: 'SKU/件数', key: 'skuSummary', width: 120 },
  { title: '下架模式', key: 'pickMode', width: 110 },
  {
    title: '拣货员',
    dataIndex: 'pickerName',
    key: 'pickerName',
    width: 100,
    customRender: ({ value }) => value || '—'
  },
  { title: '状态', key: 'status', width: 110 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'operate', width: 100, align: 'center', fixed: 'right' }
]

// ---- 下架 / 拣货单 ----
const pickOpen = ref(false)
const pickListOpen = ref(false)
const currentId = ref<number>()

const openPick = (record: OutboundOrderVO) => {
  currentId.value = record.id
  pickOpen.value = true
}
const openPickList = (record: OutboundOrderVO) => {
  currentId.value = record.id
  pickListOpen.value = true
}
</script>

<script lang="ts">
export default {
  name: 'OutboundPickingPage'
}
</script>

<style scoped>
/* 搜索栏：所有筛选项一排排列，查询/重置按钮靠右对齐 */
.outbound-search {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  row-gap: 8px;
}
.outbound-search :deep(.ant-form-item) {
  margin-right: 12px;
}
.outbound-search .search-actions-item {
  margin-left: auto;
  margin-right: 0;
}
</style>
