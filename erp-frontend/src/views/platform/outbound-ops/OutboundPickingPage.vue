<template>
  <!-- 搜索 -->
  <a-card :bordered="false" style="margin-bottom: 16px">
    <a-form :model="searchModel" layout="inline" class="outbound-search">
      <a-form-item :label="t('platform.outbound.number')">
        <a-input
          v-model:value="searchModel.outboundNo"
          :placeholder="t('platform.common.enter')"
          allow-clear
          style="width: 150px"
        />
      </a-form-item>
      <a-form-item :label="t('platform.common.status')">
        <a-select
          v-model:value="searchModel.status"
          :options="outboundStatusOptions"
          :placeholder="t('platform.common.all')"
          allow-clear
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
      <a-form-item class="search-actions-item">
        <search-actions :loading="tableRef?.loading" @search="searchTable" @reset="resetSearch" />
      </a-form-item>
    </a-form>
  </a-card>

  <pro-table
    ref="tableRef"
    :header-title="t('platform.outbound.pickingTitle')"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :row-selection="rowSelection"
    :scroll="{ x: 1320 }"
    size="middle"
  >
    <template #toolBarRender>
      <a-button type="primary" :disabled="selectedOrderIds.length === 0" @click="batchOpen = true">
        <AppstoreAddOutlined />
        {{ t('platform.outbound.batchCreate') }}<span v-if="selectedOrderIds.length">{{ t('platform.outbound.selectedCount', { count: selectedOrderIds.length }) }}</span>
      </a-button>
    </template>
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'skuSummary'">
        {{ t('platform.return.skuKindsAndPieces', { kinds: record.skuKinds, pieces: record.totalQty }) }}
      </template>
      <template v-else-if="column.key === 'pickMode'">
        <template v-if="record.pickTaskNo">
          <a class="task-link" @click="openPickList(record)">
            <a-tag :color="record.pickMode === 'WAVE' ? 'blue' : 'default'">
              {{ pickModeText[record.pickMode as PickMode] || record.pickMode }}
            </a-tag>
            <div class="task-no">{{ record.pickTaskNo }}</div>
            <div class="task-scope">
              {{ t('platform.outbound.taskScope', { count: record.pickTaskOutboundOrderCount || 1 }) }}
              <template v-if="record.sourceType === 'SALES'">
                · {{ t('platform.outbound.salesScope', { count: record.pickTaskSalesOrderCount || 0 }) }}
              </template>
            </div>
          </a>
        </template>
        <span v-else>—</span>
      </template>
      <template v-else-if="column.key === 'status'">
        <a-badge
          :status="(OUTBOUND_STATUS_BADGE[record.status as OutboundStatus] as any) || 'default'"
          :text="outboundStatusText[record.status as OutboundStatus] || record.status"
        />
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="record.status === 'PENDING'" @click="openPick(record)">{{ t('platform.outbound.pick') }}</a>
          <a
            v-else-if="record.status === 'PICKING' || record.status === 'PICKED'"
            @click="openPickList(record)"
            >{{ t('platform.outbound.taskDetail') }}</a
          >
          <a
            v-else-if="record.status === 'BACKORDER'"
            style="color: #ff4d4f"
            @click="openPick(record)"
            >{{ t('platform.outbound.shortageDetail') }}</a
          >
          <span v-else style="color: rgba(0, 0, 0, 0.25)">—</span>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 下架作业弹窗 -->
  <PickModal v-model:open="pickOpen" :order-id="currentId" @success="reloadTable" />

  <!-- 拣货单抽屉 -->
  <PickListDrawer v-model:open="pickListOpen" :order-id="currentId" @success="reloadTable(false)" />

  <BatchPickModal
    v-model:open="batchOpen"
    :order-ids="selectedOrderIds"
    @success="handleBatchSuccess"
  />
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { AppstoreAddOutlined } from '@ant-design/icons-vue'
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
  OUTBOUND_STATUS_BADGE,
  OUTBOUND_STATUS_I18N_KEYS,
  PICK_MODE_I18N_KEYS
} from './constants'
import PickModal from './PickModal.vue'
import PickListDrawer from './PickListDrawer.vue'
import BatchPickModal from './BatchPickModal.vue'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'

const tableRef = ref<ProTableInstanceExpose>()
const { t } = useI18n()
const outboundStatusText = computed<Record<OutboundStatus, string>>(() =>
  Object.fromEntries(Object.entries(OUTBOUND_STATUS_I18N_KEYS).map(([status, key]) => [status, t(key)])) as Record<OutboundStatus, string>
)
const outboundStatusOptions = computed(() =>
  (['PENDING', 'PICKING', 'PICKED', 'BACKORDER'] as OutboundStatus[]).map(value => ({
    value,
    label: outboundStatusText.value[value]
  }))
)
const pickModeText = computed<Record<PickMode, string>>(() =>
  Object.fromEntries(Object.entries(PICK_MODE_I18N_KEYS).map(([mode, key]) => [mode, t(key)])) as Record<PickMode, string>
)
const selectedOrderIds = ref<number[]>([])
const batchOpen = ref(false)
const rowSelection = computed(() => ({
  selectedRowKeys: selectedOrderIds.value,
  onChange: (keys: number[]) => {
    selectedOrderIds.value = keys
  },
  getCheckboxProps: (record: OutboundOrderVO) => ({ disabled: record.status !== 'PENDING' })
}))

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
const handleBatchSuccess = () => {
  selectedOrderIds.value = []
  reloadTable(false)
}

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

const columns = computed<ProColumns[]>(() => [
  { title: t('platform.outbound.number'), dataIndex: 'outboundNo', key: 'outboundNo', width: 190, fixed: 'left' },
  { title: t('platform.common.owner'), dataIndex: 'ownerName', key: 'ownerName', width: 140, ellipsis: true },
  { title: t('platform.common.provider'), dataIndex: 'operatorName', key: 'operatorName', width: 140, ellipsis: true },
  { title: t('platform.common.warehouse'), dataIndex: 'warehouseName', key: 'warehouseName', width: 140 },
  { title: t('platform.return.skuSummary'), key: 'skuSummary', width: 120 },
  { title: t('platform.outbound.pickTask'), key: 'pickMode', width: 245 },
  {
    title: t('platform.picking.operator'),
    dataIndex: 'pickerName',
    key: 'pickerName',
    width: 100,
    customRender: ({ value }) => value || '—'
  },
  { title: t('platform.common.status'), key: 'status', width: 110 },
  { title: t('platform.common.createdAt'), dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: t('platform.common.operation'), key: 'operate', width: 100, align: 'center', fixed: 'right' }
])

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
.task-no {
  margin-top: 3px;
  color: #8c8c8c;
  font-size: 11px;
}
.task-link {
  display: inline-block;
  line-height: 1.35;
}
.task-scope {
  margin-top: 2px;
  color: #595959;
  font-size: 12px;
  white-space: nowrap;
}
</style>
