<template>
  <a-card :bordered="false" class="search-card">
    <a-form :model="searchModel" layout="inline" class="inbound-search">
      <a-form-item :label="t('platform.inbound.number')">
        <a-input
          v-model:value="searchModel.inboundNo"
          :placeholder="t('platform.common.enter')"
          allow-clear
          style="width: 150px"
        />
      </a-form-item>
      <a-form-item :label="t('platform.common.status')">
        <a-select
          v-model:value="searchModel.orderStatus"
          :placeholder="t('platform.common.all')"
          allow-clear
          :options="statusOptions"
          style="width: 110px"
        />
      </a-form-item>
      <a-form-item :label="t('platform.inbound.dateRange')">
        <a-range-picker
          v-model:value="dateRange"
          value-format="YYYY-MM-DD"
          style="width: 220px"
          allow-clear
        />
      </a-form-item>
      <a-form-item :label="t('platform.common.provider')">
        <wms-operator-select
          v-model:value="searchModel.wmsTenantId"
          :placeholder="t('platform.common.all')"
          width="140px"
          @change="onOperatorChange"
        />
      </a-form-item>
      <a-form-item :label="t('platform.common.owner')">
        <platform-owner-select
          v-model:value="searchModel.erpTenantId"
          :placeholder="t('platform.common.all')"
          width="140px"
          :operator-id="searchModel.wmsTenantId"
        />
      </a-form-item>
      <a-form-item :label="t('platform.common.staff')">
        <user-select
          v-model:value="searchModel.putawayBy"
          :placeholder="t('platform.common.all')"
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
    :header-title="t('platform.inbound.putawayTitle')"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1240 }"
    size="middle"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'status'">
        <span v-if="record.orderStatus === InboundStatus.RECEIVED" class="pending-record">{{ t('platform.inbound.receivedPendingPutaway') }}</span>
        <inbound-status-badge v-else :status="record.orderStatus" />
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="record.orderStatus === InboundStatus.RECEIVED" @click="openPutaway(record)">
            {{ t('platform.inbound.registerPutaway') }}
          </a>
          <a v-else-if="record.orderStatus === InboundStatus.COMPLETED" @click="openDetail(record)">
            {{ t('platform.inbound.detail') }}
          </a>
          <span v-else style="color: rgba(0, 0, 0, 0.25)">{{ t('platform.inbound.unavailable') }}</span>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 上架作业抽屉 -->
  <putaway-drawer ref="putawayDrawerRef" @success="reloadTable" />
  <putaway-detail-drawer ref="putawayDetailDrawerRef" />
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { OperationGroup } from '@/components/Operation'
import { SearchActions } from '@/components/Search'
import { mergePageParam } from '@/utils/page-utils'
import { pageInboundOps } from '@/api/wms/inbound-execution'
import type { PurchaseInboundPageVO, PurchaseInboundQO } from '@/api/wms/purchase-inbound/types'
import { InboundStatus } from '@/api/wms/purchase-inbound/types'
import InboundStatusBadge from '@/views/wms/purchase-inbound/components/InboundStatusBadge.vue'
import WmsOperatorSelect from '@/components/Lov/WmsOperatorSelect.vue'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import UserSelect from '@/components/Lov/UserSelect.vue'
import { useUserData } from '@/hooks/use-user-data'
import PutawayDrawer from './PutawayDrawer.vue'
import PutawayDetailDrawer from './PutawayDetailDrawer.vue'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'

const tableRef = ref<ProTableInstanceExpose>()
const { t } = useI18n()
const { allUsers: userOptions, loading: usersLoading, loadAllUsers } = useUserData()

// 上架页覆盖「已收货/已完成」：上架后单据保留在本页（状态变已完成），只是上架动作不再可点
const PUTAWAY_SCOPE = [InboundStatus.RECEIVED, InboundStatus.COMPLETED]
const statusKey = (status: InboundStatus) =>
  ({
    [InboundStatus.DRAFT]: 'platform.inbound.status.draft',
    [InboundStatus.SUBMITTED]: 'platform.inbound.status.submitted',
    [InboundStatus.RECEIVED]: 'platform.inbound.status.received',
    [InboundStatus.COMPLETED]: 'platform.inbound.status.completed',
    [InboundStatus.CANCELLED]: 'platform.inbound.status.cancelled'
  })[status]
const statusOptions = computed(() => PUTAWAY_SCOPE.map(status => ({ value: status, label: t(statusKey(status)) })))

const searchModel = reactive<PurchaseInboundQO>({
  inboundNo: undefined,
  orderStatus: undefined,
  wmsTenantId: undefined,
  erpTenantId: undefined,
  putawayBy: undefined
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
    putawayBy: searchParams.putawayBy,
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
  searchModel.putawayBy = undefined
  dateRange.value = undefined
  searchTable()
}

const columns = computed<ProColumns[]>(() => [
  { title: t('platform.inbound.number'), dataIndex: 'inboundNo', key: 'inboundNo', width: 220, fixed: 'left' },
  { title: t('platform.common.owner'), dataIndex: 'ownerName', key: 'ownerName', width: 140, ellipsis: true },
  { title: t('platform.common.provider'), dataIndex: 'operatorName', key: 'operatorName', width: 140, ellipsis: true },
  { title: t('platform.common.warehouse'), dataIndex: 'warehouseName', key: 'warehouseName', width: 180 },
  {
    title: t('platform.common.staff'),
    dataIndex: 'putawayByName',
    key: 'putawayByName',
    width: 90,
    ellipsis: true
  },
  { title: t('platform.common.status'), key: 'status', width: 110, align: 'center' },
  { title: t('platform.common.createdAt'), dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: t('platform.common.operation'), key: 'operate', width: 120, align: 'center', fixed: 'right' }
])

const putawayDrawerRef = ref<InstanceType<typeof PutawayDrawer>>()
const putawayDetailDrawerRef = ref<InstanceType<typeof PutawayDetailDrawer>>()
const openPutaway = (record: PurchaseInboundPageVO) => putawayDrawerRef.value?.open(record)
const openDetail = (record: PurchaseInboundPageVO) => putawayDetailDrawerRef.value?.open(record)

onMounted(loadAllUsers)
</script>

<script lang="ts">
export default {
  name: 'InboundPutawayPage'
}
</script>

<style scoped>
.search-card {
  margin-bottom: 16px;
}
.search-card :deep(.ant-card-body) {
  min-width: 0;
}
/* 根据可用宽度自然换行；每个“标签 + 控件”始终作为完整单元排列。 */
.inbound-search {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 20px;
}
.inbound-search :deep(.ant-form-item) {
  flex: 0 0 auto;
  margin: 0;
}
.inbound-search :deep(.ant-form-item-row) {
  flex-wrap: nowrap;
  align-items: center;
}
.inbound-search :deep(.ant-form-item-label) {
  flex: 0 0 auto;
}
.inbound-search .search-actions-item {
  margin: 0;
}
.pending-record {
  color: #d46b08;
}
</style>
