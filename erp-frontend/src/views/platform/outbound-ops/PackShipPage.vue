<template>
  <a-card :bordered="false" class="filter-card">
    <div class="scan-row">
      <div class="scan-title">
        <scan-outlined />
        <span>{{ t('platform.pack.scanTitle') }}</span>
      </div>
      <a-input-search
        ref="scanInputRef"
        v-model:value="scanCode"
        :placeholder="t('platform.pack.scanPlaceholder')"
        :enter-button="t('platform.pack.locate')"
        :loading="locating"
        allow-clear
        @search="handleLocate"
      />
    </div>

    <a-form :model="searchModel" layout="inline" class="package-search">
      <a-form-item :label="t('platform.pack.orderOrSlot')">
        <a-input v-model:value="searchModel.keyword" :placeholder="t('platform.common.enter')" allow-clear />
      </a-form-item>
      <a-form-item :label="t('dashboard.platform')">
        <a-select
          v-model:value="searchModel.platform"
          :options="platformOptions"
          :placeholder="t('platform.common.all')"
          allow-clear
        />
      </a-form-item>
      <a-form-item :label="t('platform.common.status')">
        <a-select
          v-model:value="searchModel.workStatus"
          :options="workStatusOptions"
          :placeholder="t('platform.common.all')"
          allow-clear
        />
      </a-form-item>
      <a-form-item :label="t('platform.inbound.dateRange')">
        <a-range-picker
          v-model:value="dateRange"
          value-format="YYYY-MM-DD"
          allow-clear
        />
      </a-form-item>
      <a-form-item :label="t('platform.common.provider')">
        <wms-operator-select
          v-model:value="searchModel.wmsTenantId"
          :placeholder="t('platform.common.all')"
          width="150px"
          @change="onOperatorChange"
        />
      </a-form-item>
      <a-form-item :label="t('platform.common.owner')">
        <platform-owner-select
          v-model:value="searchModel.erpTenantId"
          :placeholder="t('platform.common.all')"
          width="150px"
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
    :header-title="t('platform.pack.title')"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1580 }"
    size="middle"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'platformOrder'">
        <div class="primary-code">{{ record.platformOrderId }}</div>
        <div class="secondary-text">{{ t('platform.pack.source', { number: record.outboundNo }) }}</div>
      </template>
      <template v-else-if="column.key === 'platform'">
        <a-tag>{{ platformText(record.platform) }}</a-tag>
        <div class="secondary-text">{{ record.shopName || '-' }}</div>
      </template>
      <template v-else-if="column.key === 'owner'">
        <div>{{ record.ownerName }}</div>
        <div class="secondary-text">{{ record.operatorName || '-' }}</div>
      </template>
      <template v-else-if="column.key === 'sortCode'">
        <a-tag v-if="record.sortCode" color="orange">{{ record.sortCode }}</a-tag>
        <span v-else class="muted">{{ t('platform.pack.direct') }}</span>
      </template>
      <template v-else-if="column.key === 'skuSummary'">
        {{ t('platform.return.skuKindsAndPieces', { kinds: record.skuKinds, pieces: record.totalQty }) }}
      </template>
      <template v-else-if="column.key === 'labelStatus'">
        <a-tag :color="labelColor(record.labelStatus)">
          {{ labelText(record.labelStatus) }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'workStatus'">
        <a-badge
          :status="workStatusBadge(record.workStatus)"
          :text="workStatusText(record.workStatus)"
        />
      </template>
      <template v-else-if="column.key === 'shipping'">
        <template v-if="record.shipStatus === 'SHIPPED'">
          <div>{{ record.channelName || record.channelCode || t('platform.pack.autoChannel') }}</div>
          <div class="secondary-text">{{ record.trackingNo || t('platform.pack.noTracking') }}</div>
        </template>
        <span v-else class="muted">{{ record.defaultChannelCode || 'AUTO' }}</span>
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="record.workStatus === 'PENDING_PACK'" @click="openPack(record)">{{ t('platform.pack.pack') }}</a>
          <a v-else-if="record.workStatus === 'PACKED'" @click="openShip(record)">{{ t('platform.pack.signOut') }}</a>
          <a v-else @click="openPack(record)">{{ t('platform.common.view') }}</a>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <PackModal
    v-model:open="packOpen"
    :order-id="currentRecord?.outboundOrderId"
    :package-id="currentRecord?.id"
    @success="reloadTable"
  />
  <PackageShipModal
    v-model:open="shipOpen"
    :record="currentRecord"
    @success="reloadTable"
  />
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { ScanOutlined } from '@ant-design/icons-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { OperationGroup } from '@/components/Operation'
import { SearchActions } from '@/components/Search'
import WmsOperatorSelect from '@/components/Lov/WmsOperatorSelect.vue'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import { mergePageParam } from '@/utils/page-utils'
import { isSuccess } from '@/api'
import {
  locatePackShipPackage,
  pagePackShipPackages
} from '@/api/wms/outbound-shipping'
import type {
  PackShipPackagePageVO,
  PackShipPackageQO,
  PackageWorkStatus
} from '@/api/wms/outbound-shipping/types'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'
import PackModal from './PackModal.vue'
import PackageShipModal from './PackageShipModal.vue'

const tableRef = ref<ProTableInstanceExpose>()
const { t } = useI18n()
const scanInputRef = ref()
const scanCode = ref('')
const locating = ref(false)
const dateRange = ref<[string, string]>()
const searchModel = reactive<PackShipPackageQO>({})
let searchParams: PackShipPackageQO = {}

const platformOptions = [
  { label: 'Ozon', value: 'ozon' },
  { label: 'WB', value: 'wb' },
  { label: 'Yandex', value: 'yandex' }
]
const workStatusOptions = computed(() => [
  { label: t('platform.pack.status.pending'), value: 'PENDING_PACK' },
  { label: t('platform.pack.status.packed'), value: 'PACKED' },
  { label: t('platform.pack.status.shipped'), value: 'SHIPPED' }
])

const tableRequest: TableRequest = (params, sorter, filter) => {
  return pagePackShipPackages(mergePageParam(params, sorter, filter), searchParams)
}
const reloadTable = (resetPageIndex?: boolean) => tableRef.value?.actionRef?.reload(resetPageIndex)
useTableActivateReload(() => reloadTable(false))

const onOperatorChange = () => {
  searchModel.erpTenantId = undefined
}
const searchTable = () => {
  searchParams = {
    ...searchModel,
    createTimeStart: dateRange.value?.[0],
    createTimeEnd: dateRange.value?.[1]
  }
  reloadTable(true)
}
const resetSearch = () => {
  Object.assign(searchModel, {
    keyword: undefined,
    platform: undefined,
    workStatus: undefined,
    wmsTenantId: undefined,
    erpTenantId: undefined
  })
  dateRange.value = undefined
  searchTable()
}

const currentRecord = ref<PackShipPackagePageVO>()
const packOpen = ref(false)
const shipOpen = ref(false)

function openPack(record: PackShipPackagePageVO) {
  currentRecord.value = record
  packOpen.value = true
}
function openShip(record: PackShipPackagePageVO) {
  currentRecord.value = record
  shipOpen.value = true
}

async function handleLocate() {
  const code = scanCode.value.trim()
  if (!code) {
    message.warning(t('platform.pack.scanRequired'))
    return
  }
  locating.value = true
  try {
    const res = await locatePackShipPackage(code)
    if (isSuccess(res) && res.data) {
      currentRecord.value = res.data
      scanCode.value = ''
      if (res.data.workStatus === 'PENDING_PACK') openPack(res.data)
      else if (res.data.workStatus === 'PACKED') openShip(res.data)
      else message.info(t('platform.pack.alreadyShipped'))
    } else {
      message.warning(res.message || t('platform.pack.notFound'))
    }
  } finally {
    locating.value = false
    await nextTick()
    scanInputRef.value?.focus?.()
  }
}

function platformText(platform?: string) {
  if (!platform) return '-'
  const value = platform.toLowerCase()
  if (value === 'ozon') return 'Ozon'
  if (value === 'wb') return 'WB'
  if (value === 'yandex') return 'Yandex'
  return platform
}
function labelText(status: string) {
  return (
    {
      NOT_READY: t('platform.pack.label.notReady'),
      READY: t('platform.pack.label.ready'),
      EXTERNAL_CONFIRMED: t('platform.pack.label.externalConfirmed'),
      ATTACHED_CONFIRMED: t('platform.pack.label.attachedConfirmed')
    }[status] || status
  )
}
function labelColor(status: string) {
  if (status === 'ATTACHED_CONFIRMED') return 'green'
  if (status === 'READY') return 'blue'
  if (status === 'EXTERNAL_CONFIRMED') return 'gold'
  return 'default'
}
function workStatusText(status: PackageWorkStatus) {
  return {
    PENDING_PACK: t('platform.pack.status.pending'),
    PACKED: t('platform.pack.status.packed'),
    SHIPPED: t('platform.pack.status.shipped')
  }[status]
}
function workStatusBadge(status: PackageWorkStatus) {
  return ({ PENDING_PACK: 'processing', PACKED: 'warning', SHIPPED: 'success' }[status] ||
    'default') as any
}

const columns = computed<ProColumns[]>(() => [
  { title: t('platform.return.receipt.platformOrderNo'), key: 'platformOrder', width: 220, fixed: 'left' },
  { title: t('platform.pack.platformShop'), key: 'platform', width: 150 },
  { title: t('platform.pack.ownerProvider'), key: 'owner', width: 150 },
  { title: t('platform.common.warehouse'), dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: t('platform.pack.sortLocation'), key: 'sortCode', width: 105 },
  { title: t('platform.return.skuSummary'), key: 'skuSummary', width: 110 },
  { title: t('platform.picking.simple.label'), key: 'labelStatus', width: 110 },
  { title: t('platform.pack.workStatus'), key: 'workStatus', width: 110 },
  { title: t('platform.pack.shippingTracking'), key: 'shipping', width: 180 },
  { title: t('platform.common.createdAt'), dataIndex: 'createTime', key: 'createTime', width: 165 },
  { title: t('platform.common.operation'), key: 'operate', width: 90, align: 'center', fixed: 'right' }
])
</script>

<script lang="ts">
export default {
  name: 'PackShipPage'
}
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}
.scan-row {
  display: grid;
  grid-template-columns: 190px minmax(320px, 720px);
  align-items: center;
  gap: 16px;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.scan-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.package-search {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 0;
}
.package-search :deep(.ant-form-item) {
  margin-right: 16px;
  margin-bottom: 0;
}
.package-search :deep(.ant-input) {
  width: 150px;
}
.package-search :deep(.ant-select) {
  width: 130px;
}
.package-search :deep(.ant-picker) {
  width: 240px;
}
.search-actions-item {
  margin-left: auto;
  margin-right: 0 !important;
}
.primary-code {
  font-weight: 500;
}
.secondary-text {
  margin-top: 3px;
  color: #8c8c8c;
  font-size: 12px;
}
.muted {
  color: #8c8c8c;
}
@media (max-width: 900px) {
  .scan-row {
    grid-template-columns: 1fr;
  }
  .search-actions-item {
    margin-left: 0;
  }
}
</style>
