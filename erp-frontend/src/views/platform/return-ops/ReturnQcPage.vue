<template>
  <!-- 搜索 -->
  <a-card :bordered="false" style="margin-bottom: 16px">
    <a-form :model="searchModel" layout="inline" class="returnqc-search">
      <a-form-item :label="t('platform.return.number')">
        <a-input
          v-model:value="searchModel.returnNo"
          :placeholder="t('platform.common.enter')"
          allow-clear
          style="width: 150px"
        />
      </a-form-item>
      <a-form-item :label="t('platform.common.status')">
        <a-select
          v-model:value="searchModel.status"
          :options="returnStatusOptions"
          :placeholder="t('platform.common.all')"
          allow-clear
          style="width: 110px"
        />
      </a-form-item>
      <a-form-item :label="t('platform.return.dateRange')">
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
      <a-form-item class="search-actions-item">
        <search-actions :loading="tableRef?.loading" @search="searchTable" @reset="resetSearch" />
      </a-form-item>
    </a-form>
  </a-card>

  <pro-table
    ref="tableRef"
    :header-title="t('platform.return.title')"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1240 }"
    size="middle"
  >
    <template #toolBarRender>
      <a-button type="primary" @click="receiptOpen = true"
        ><plus-outlined />{{ t('platform.return.register') }}</a-button
      >
    </template>
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'skuSummary'">
        {{ t('platform.return.skuKindsAndPieces', { kinds: record.skuKinds, pieces: record.totalQty }) }}
      </template>
      <template v-else-if="column.key === 'status'">
        <a-badge
          :status="(RETURN_STATUS_BADGE[record.status as ReturnStatus] as any) || 'default'"
          :text="returnStatusText[record.status as ReturnStatus] || record.status"
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
          <a v-if="record.status === 'PENDING_OWNER'" @click="openProcess(record, true)">{{ t('platform.common.view') }}</a>
          <a
            v-if="record.status === 'PENDING_OWNER'"
            class="danger-link"
            @click="handleCloseReturn(record)"
            >{{ t('platform.common.close') }}</a
          >
          <a v-else-if="record.status === 'PENDING_OPERATION'" @click="openProcess(record)"
            >{{ t('platform.return.execute') }}</a
          >
          <a v-else-if="record.status === 'COMPLETED'" @click="openProcess(record, true)">{{ t('platform.common.view') }}</a>
          <span v-if="record.status === 'CLOSED'" style="color: rgba(0, 0, 0, 0.25)">—</span>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <ReturnReceiptModal v-model:open="receiptOpen" @success="reloadTable" />
  <ReturnProcessModal
    v-model:open="processOpen"
    :order-id="currentId"
    :readonly="processReadonly"
    @success="reloadTable"
  />
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
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
import { RETURN_STATUS_BADGE, RETURN_STATUS_I18N_KEYS } from './constants'
import ReturnReceiptModal from './ReturnReceiptModal.vue'
import ReturnProcessModal from './ReturnProcessModal.vue'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'

const tableRef = ref<ProTableInstanceExpose>()
const { t } = useI18n()

const returnStatusText = computed<Record<ReturnStatus, string>>(() =>
  Object.fromEntries(
    Object.entries(RETURN_STATUS_I18N_KEYS).map(([status, key]) => [status, t(key)])
  ) as Record<ReturnStatus, string>
)
const returnStatusOptions = computed(() =>
  Object.entries(returnStatusText.value).map(([value, label]) => ({
    value: value as ReturnStatus,
    label
  }))
)

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

const columns = computed<ProColumns[]>(() => [
  { title: t('platform.return.number'), dataIndex: 'returnNo', key: 'returnNo', width: 190, fixed: 'left' },
  { title: t('platform.common.owner'), dataIndex: 'ownerName', key: 'ownerName', width: 140, ellipsis: true },
  { title: t('platform.common.provider'), dataIndex: 'operatorName', key: 'operatorName', width: 140, ellipsis: true },
  { title: t('platform.common.warehouse'), dataIndex: 'warehouseName', key: 'warehouseName', width: 140 },
  { title: t('platform.return.skuSummary'), key: 'skuSummary', width: 120 },
  { title: t('platform.common.status'), key: 'status', width: 120 },
  { title: t('platform.return.operator'), key: 'operatorUser', width: 120, ellipsis: true },
  { title: t('platform.return.operationTime'), key: 'operationTime', width: 170 },
  { title: t('platform.common.createdAt'), dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: t('platform.common.operation'), key: 'operate', width: 130, align: 'center', fixed: 'right' }
])

const operationInfo = (record: ReturnOrderVO) => {
  if (record.status === 'COMPLETED')
    return { name: record.processedByName || '-', time: record.processedTime || '-' }
  if (record.status === 'CLOSED')
    return { name: record.closedByName || '-', time: record.closedTime || '-' }
  if (record.status === 'PENDING_OPERATION')
    return { name: record.dispositionByName || '-', time: record.dispositionTime || '-' }
  if (record.status === 'PENDING_OWNER')
    return { name: record.receivedByName || '-', time: record.receivedTime || '-' }
  return { name: '-', time: '-' }
}

const receiptOpen = ref(false)
const processOpen = ref(false)
const processReadonly = ref(false)
const currentId = ref<number>()

const openProcess = (record: ReturnOrderVO, readonly = false) => {
  currentId.value = record.id
  processReadonly.value = readonly
  processOpen.value = true
}

const handleCloseReturn = (record: ReturnOrderVO) => {
  Modal.confirm({
    title: t('platform.return.closeTitle'),
    content: t('platform.return.closeDescription'),
    okText: t('platform.return.confirmClose'),
    cancelText: t('platform.common.cancel'),
    okType: 'danger',
    async onOk() {
      const res = await closeReturn(record.id)
      if (!isSuccess(res)) throw new Error(res.message || t('platform.return.closeFailed'))
      message.success(t('platform.return.closed'))
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
