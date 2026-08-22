<template>
  <a-card :bordered="false" class="filter-card">
    <div class="scan-row">
      <div class="scan-title">
        <scan-outlined />
        <span>扫描格口或平台订单</span>
      </div>
      <a-input-search
        ref="scanInputRef"
        v-model:value="scanCode"
        placeholder="扫描格口码、平台订单号，自动打开对应作业"
        enter-button="定位"
        :loading="locating"
        allow-clear
        @search="handleLocate"
      />
    </div>

    <a-form :model="searchModel" layout="inline" class="package-search">
      <a-form-item label="订单/格口">
        <a-input v-model:value="searchModel.keyword" placeholder="请输入" allow-clear />
      </a-form-item>
      <a-form-item label="平台">
        <a-select
          v-model:value="searchModel.platform"
          :options="platformOptions"
          placeholder="全部"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="状态">
        <a-select
          v-model:value="searchModel.workStatus"
          :options="workStatusOptions"
          placeholder="全部"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="日期范围">
        <a-range-picker
          v-model:value="dateRange"
          value-format="YYYY-MM-DD"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="服务商">
        <wms-operator-select
          v-model:value="searchModel.wmsTenantId"
          placeholder="全部"
          width="150px"
          @change="onOperatorChange"
        />
      </a-form-item>
      <a-form-item label="货主">
        <platform-owner-select
          v-model:value="searchModel.erpTenantId"
          placeholder="全部"
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
    header-title="平台订单打包签出"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1580 }"
    size="middle"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'platformOrder'">
        <div class="primary-code">{{ record.platformOrderId }}</div>
        <div class="secondary-text">来源：{{ record.outboundNo }}</div>
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
        <span v-else class="muted">直接打包</span>
      </template>
      <template v-else-if="column.key === 'skuSummary'">
        {{ record.skuKinds }} 种 · {{ record.totalQty }} 件
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
          <div>{{ record.channelName || record.channelCode || '自动渠道' }}</div>
          <div class="secondary-text">{{ record.trackingNo || '无跟踪号' }}</div>
        </template>
        <span v-else class="muted">{{ record.defaultChannelCode || 'AUTO' }}</span>
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="record.workStatus === 'PENDING_PACK'" @click="openPack(record)">打包</a>
          <a v-else-if="record.workStatus === 'PACKED'" @click="openShip(record)">签出</a>
          <a v-else @click="openPack(record)">查看</a>
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
import { nextTick, reactive, ref } from 'vue'
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
const workStatusOptions = [
  { label: '待打包', value: 'PENDING_PACK' },
  { label: '已打包', value: 'PACKED' },
  { label: '已签出', value: 'SHIPPED' }
]

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
    message.warning('请扫描格口码或平台订单号')
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
      else message.info('该平台订单已经签出')
    } else {
      message.warning(res.message || '没有找到对应平台订单')
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
      NOT_READY: '待生成',
      READY: '已生成',
      EXTERNAL_CONFIRMED: '资料已核对',
      ATTACHED_CONFIRMED: '已贴单'
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
  return { PENDING_PACK: '待打包', PACKED: '已打包', SHIPPED: '已签出' }[status]
}
function workStatusBadge(status: PackageWorkStatus) {
  return ({ PENDING_PACK: 'processing', PACKED: 'warning', SHIPPED: 'success' }[status] ||
    'default') as any
}

const columns: ProColumns[] = [
  { title: '平台订单号', key: 'platformOrder', width: 220, fixed: 'left' },
  { title: '平台/店铺', key: 'platform', width: 150 },
  { title: '货主/服务商', key: 'owner', width: 150 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '分货位置', key: 'sortCode', width: 105 },
  { title: 'SKU/件数', key: 'skuSummary', width: 110 },
  { title: '面单', key: 'labelStatus', width: 110 },
  { title: '作业状态', key: 'workStatus', width: 110 },
  { title: '签出方式/跟踪号', key: 'shipping', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 165 },
  { title: '操作', key: 'operate', width: 90, align: 'center', fixed: 'right' }
]
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
