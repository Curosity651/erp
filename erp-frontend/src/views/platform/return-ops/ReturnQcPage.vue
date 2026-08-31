<template>
  <!-- 搜索 -->
  <a-card :bordered="false" style="margin-bottom: 16px">
    <a-form :model="searchModel" layout="inline" class="returnqc-search">
      <a-form-item label="退货单号">
        <a-input
          v-model:value="searchModel.returnNo"
          placeholder="请输入"
          allow-clear
          style="width: 150px"
        />
      </a-form-item>
      <a-form-item label="状态">
        <a-select
          v-model:value="searchModel.status"
          :options="RETURN_STATUS_OPTIONS"
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
    header-title="退货质检"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1240 }"
    size="middle"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'skuSummary'">
        {{ record.skuKinds }} 种 · {{ record.totalQty }} 件
      </template>
      <template v-else-if="column.key === 'status'">
        <a-badge
          :status="(RETURN_STATUS_BADGE[record.status as ReturnStatus] as any) || 'default'"
          :text="RETURN_STATUS_TEXT[record.status as ReturnStatus] || record.status"
        />
      </template>
      <template v-else-if="column.key === 'operatorUser'">
        {{ operationInfo(record).name }}
      </template>
      <template v-else-if="column.key === 'operationTime'">
        {{ operationInfo(record).time }}
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="record.status === 'RETURN_PENDING'" @click="openReceive(record)">收货</a>
          <a v-if="record.status === 'RETURN_PENDING'" class="danger-link" @click="handleCloseReturn(record)">关闭</a>
          <a v-else-if="record.status === 'QC_PENDING'" @click="openQc(record)">质检</a>
          <a v-else-if="record.status === 'COMPLETED'" @click="openQc(record, true)">查看</a>
          <span v-if="record.status === 'CLOSED'" style="color: rgba(0, 0, 0, 0.25)">—</span>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <ReturnReceiveModal v-model:open="receiveOpen" :order-id="currentId" @success="reloadTable" />
  <ReturnQcModal
    v-model:open="qcOpen"
    :order-id="currentId"
    :readonly="qcReadonly"
    @success="reloadTable"
  />
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { OperationGroup } from '@/components/Operation'
import { SearchActions } from '@/components/Search'
import WmsOperatorSelect from '@/components/Lov/WmsOperatorSelect.vue'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import { mergePageParam } from '@/utils/page-utils'
import { closeReturn, pageReturns } from '@/api/wms/return-qc'
import { isSuccess } from '@/api'
import type { ReturnOrderVO, ReturnQO, ReturnStatus } from '@/api/wms/return-qc/types'
import { RETURN_STATUS_TEXT, RETURN_STATUS_BADGE, RETURN_STATUS_OPTIONS } from './constants'
import ReturnReceiveModal from './ReturnReceiveModal.vue'
import ReturnQcModal from './ReturnQcModal.vue'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'

const tableRef = ref<ProTableInstanceExpose>()

const searchModel = reactive<ReturnQO>({
  returnNo: undefined,
  status: undefined,
  wmsTenantId: undefined,
  erpTenantId: undefined
})
// 日期范围（[开始, 结束]，value-format 已转字符串）
const dateRange = ref<[string, string]>()
let searchParams: ReturnQO = {}

// 服务商变更时清空货主（避免残留跨服务商的货主选择）
const onOperatorChange = () => {
  searchModel.erpTenantId = undefined
}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageReturns(pageParam, searchParams)
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
  searchModel.returnNo = undefined
  searchModel.status = undefined
  searchModel.wmsTenantId = undefined
  searchModel.erpTenantId = undefined
  dateRange.value = undefined
  searchTable()
}

const columns: ProColumns[] = [
  { title: '退货单号', dataIndex: 'returnNo', key: 'returnNo', width: 190, fixed: 'left' },
  { title: '货主', dataIndex: 'ownerName', key: 'ownerName', width: 140, ellipsis: true },
  { title: '服务商', dataIndex: 'operatorName', key: 'operatorName', width: 140, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 140 },
  { title: 'SKU/件数', key: 'skuSummary', width: 120 },
  { title: '状态', key: 'status', width: 120 },
  { title: '操作人', key: 'operatorUser', width: 120, ellipsis: true },
  { title: '操作时间', key: 'operationTime', width: 170 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'operate', width: 130, align: 'center', fixed: 'right' }
]

const operationInfo = (record: ReturnOrderVO) => {
  if (record.status === 'COMPLETED') return { name: record.qcByName || '-', time: record.qcTime || '-' }
  if (record.status === 'CLOSED') return { name: record.closedByName || '-', time: record.closedTime || '-' }
  if (record.status === 'QC_PENDING') return { name: record.receivedByName || '-', time: record.receivedTime || '-' }
  return { name: '-', time: '-' }
}

const receiveOpen = ref(false)
const qcOpen = ref(false)
const qcReadonly = ref(false)
const currentId = ref<number>()

const openReceive = (record: ReturnOrderVO) => {
  currentId.value = record.id
  receiveOpen.value = true
}
const openQc = (record: ReturnOrderVO, readonly = false) => {
  currentId.value = record.id
  qcReadonly.value = readonly
  qcOpen.value = true
}

const handleCloseReturn = (record: ReturnOrderVO) => {
  Modal.confirm({
    title: '关闭退货单',
    content: '仅用于未收到货物或拒收。关闭后将释放全部申报数量，且不能恢复。',
    okText: '确认关闭',
    okType: 'danger',
    async onOk() {
      const res = await closeReturn(record.id)
      if (!isSuccess(res)) throw new Error(res.message || '关闭失败')
      message.success('退货单已关闭，申报数量已释放')
      reloadTable(false)
    }
  })
}

</script>

<script lang="ts">
export default {
  name: 'ReturnQcPage'
}
</script>

<style scoped>
/* 搜索栏：所有筛选项一排排列，查询/重置按钮靠右对齐 */
.returnqc-search {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  row-gap: 8px;
}
.returnqc-search :deep(.ant-form-item) {
  margin-right: 12px;
}
.returnqc-search .search-actions-item {
  margin-left: auto;
  margin-right: 0;
}
.danger-link {
  color: #ff4d4f;
}
</style>
