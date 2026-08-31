<template>
  <a-card :bordered="false" class="search-card">
    <a-form :model="search" layout="inline" class="location-transfer-search">
      <a-form-item label="调整单号">
        <a-input
          v-model:value="search.transferNo"
          placeholder="请输入"
          allow-clear
          style="width: 150px"
        />
      </a-form-item>
      <a-form-item label="状态">
        <a-select
          v-model:value="search.orderStatus"
          :options="statusOptions"
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
          v-model:value="search.wmsTenantId"
          placeholder="全部"
          width="140px"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="货主">
        <platform-owner-select
          v-model:value="search.erpTenantId"
          :operator-id="search.wmsTenantId"
          placeholder="全部"
          width="140px"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="操作员">
        <user-select
          v-model:value="search.operatorUserId"
          placeholder="全部"
          :options="userOptions"
          :loading="usersLoading"
          style="width: 140px"
        />
      </a-form-item>
      <a-form-item class="search-actions-item">
        <search-actions :loading="tableRef?.loading" @search="searchTable" @reset="resetSearch" />
      </a-form-item>
    </a-form>
  </a-card>

  <pro-table
    ref="tableRef"
    header-title="库位调整单"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1640 }"
    size="middle"
  >
    <template #toolBarRender>
      <a-button v-if="hasPermission('wms:location:transfer')" type="primary" @click="handleNew">
        <plus-outlined />
        新建调整单
      </a-button>
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'no'">
        <a @click="handleViewDetail(record)">{{ record.transferNo }}</a>
      </template>
      <template v-else-if="column.key === 'statistics'">
        {{ record.itemCount }} 条 · {{ record.totalQuantity }} 件
      </template>
      <template v-else-if="column.key === 'status'">
        <a-badge :status="badge(record.orderStatus)" :text="statusText(record.orderStatus)" />
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a @click="handleViewDetail(record)">查看</a>
          <confirm-text-button
            v-if="record.orderStatus === 'PLANNED' && hasPermission('wms:location:transfer')"
            title="确认取消该调整计划吗？已预留的库存将被释放。"
            text="取消"
            @confirm="handleCancel(record)"
          />
          <template
            v-if="record.orderStatus === 'PENDING' && hasPermission('wms:location:transfer')"
          >
            <confirm-text-button
              title="确认执行该调整单吗？将按明细逐条移库并置为已完成，不可撤销。"
              text="调整完成"
              @confirm="handleComplete(record)"
            />
            <confirm-text-button
              title="确认撤销该调整单吗？"
              text="撤销"
              @confirm="handleCancel(record)"
            />
          </template>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <location-transfer-create-drawer ref="createRef" @success="reloadTable" />
  <location-transfer-detail-drawer ref="detailRef" />
</template>

<script setup lang="ts">
import { onMounted, ref, reactive } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { OperationGroup } from '@/components/Operation'
import { ConfirmTextButton } from '@/components/Button'
import { SearchActions } from '@/components/Search'
import WmsOperatorSelect from '@/components/Lov/WmsOperatorSelect.vue'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import UserSelect from '@/components/Lov/UserSelect.vue'
import { useUserData } from '@/hooks/use-user-data'
import { useAuthorize } from '@/hooks/permission'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'
import { mergePageParam } from '@/utils/page-utils'
import { doRequest } from '@/utils/axios/request'
import {
  pageLocationTransfer,
  completeLocationTransfer,
  cancelLocationTransfer
} from '@/api/wms/location-transfer'
import {
  LocationTransferReasonList,
  LocationTransferStatusList
} from '@/api/wms/location-transfer/types'
import type {
  LocationTransferPageVO,
  LocationTransferQO,
  LocationTransferStatus
} from '@/api/wms/location-transfer/types'
import LocationTransferCreateDrawer from './LocationTransferCreateDrawer.vue'
import LocationTransferDetailDrawer from './LocationTransferDetailDrawer.vue'

defineOptions({ name: 'LocationTransferPage' })

const { hasPermission } = useAuthorize()
const tableRef = ref<ProTableInstanceExpose>()
const createRef = ref<InstanceType<typeof LocationTransferCreateDrawer>>()
const detailRef = ref<InstanceType<typeof LocationTransferDetailDrawer>>()
const { allUsers: userOptions, loading: usersLoading, loadAllUsers } = useUserData()

const statusOptions = LocationTransferStatusList.map(s => ({ label: s.label, value: s.value }))
const statusText = (s: LocationTransferStatus) =>
  LocationTransferStatusList.find(x => x.value === s)?.label || s
const badge = (s: LocationTransferStatus) =>
  (LocationTransferStatusList.find(x => x.value === s)?.badge as any) || 'default'

const search = reactive<LocationTransferQO>({
  transferNo: undefined,
  wmsTenantId: undefined,
  erpTenantId: undefined,
  operatorUserId: undefined,
  orderStatus: undefined
})
// 日期范围（[开始, 结束]，按创建时间过滤）
const dateRange = ref<[string, string]>()
let searchParams: LocationTransferQO = {}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageLocationTransfer({ ...pageParam, ...searchParams })
}
const reloadTable = (reset?: boolean) => tableRef.value?.actionRef?.reload(reset)

useTableActivateReload(() => reloadTable(false))
const searchTable = () => {
  searchParams = {
    ...search,
    createTimeStart: dateRange.value?.[0],
    createTimeEnd: dateRange.value?.[1]
  }
  reloadTable(true)
}
const resetSearch = () => {
  search.transferNo = undefined
  search.wmsTenantId = undefined
  search.erpTenantId = undefined
  search.operatorUserId = undefined
  search.orderStatus = undefined
  dateRange.value = undefined
  searchTable()
}

const columns: ProColumns[] = [
  { title: '调整单号', key: 'no', width: 170, fixed: 'left' },
  {
    title: '来源',
    dataIndex: 'sourceType',
    width: 110,
    customRender: ({ text }) => (text === 'SALES_OUTBOUND' ? '销售出库' : '人工创建')
  },
  { title: '关联单号', dataIndex: 'sourceNo', width: 170, ellipsis: true },
  { title: '所属服务商', dataIndex: 'operatorName', width: 130, ellipsis: true },
  { title: '货主', dataIndex: 'ownerName', width: 130, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', width: 120, ellipsis: true },
  { title: '操作员', dataIndex: 'operatorUserName', width: 90, ellipsis: true },
  { title: '明细', key: 'statistics', width: 130 },
  { title: '状态', key: 'status', width: 110 },
  {
    title: '调整原因',
    dataIndex: 'reason',
    width: 180,
    ellipsis: true,
    customRender: ({ text, record }) =>
      text ||
      LocationTransferReasonList.find(item => item.value === record.reasonCode)?.label ||
      (record.reasonCode === 'OUTBOUND_PICKABLE_SHORTAGE' ? '销售出库准备' : '-')
  },
  { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { key: 'operate', title: '操作', align: 'center', width: 180, fixed: 'right' }
]

const handleNew = () => createRef.value?.open()
const handleViewDetail = (r: LocationTransferPageVO) => detailRef.value?.open(r.id)
const handleComplete = (r: LocationTransferPageVO) => {
  doRequest(completeLocationTransfer(r.id), {
    successMessage: '库位调整已完成',
    onSuccess: () => {
      reloadTable()
    }
  })
}
const handleCancel = (r: LocationTransferPageVO) => {
  doRequest(cancelLocationTransfer(r.id), {
    successMessage: '已撤销',
    onSuccess: () => reloadTable()
  })
}

onMounted(loadAllUsers)
</script>

<style scoped>
.search-card {
  margin-bottom: 16px;
}

.search-card :deep(.ant-card-body) {
  min-width: 0;
}

.location-transfer-search {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 20px;
}

.location-transfer-search :deep(.ant-form-item) {
  flex: 0 0 auto;
  margin: 0;
}

.location-transfer-search :deep(.ant-form-item-row) {
  flex-wrap: nowrap;
  align-items: center;
}

.location-transfer-search :deep(.ant-form-item-label) {
  flex: 0 0 auto;
}

.location-transfer-search .search-actions-item {
  margin: 0;
}
</style>
