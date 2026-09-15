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
          v-model:value="searchModel.receiveBy"
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
    :header-title="t('platform.inbound.receiveTitle')"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1240 }"
    size="middle"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'status'">
        <inbound-status-badge :status="record.orderStatus" />
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="record.orderStatus === InboundStatus.SUBMITTED" @click="openReceive(record)">
            {{ t('platform.inbound.receive') }}
          </a>
          <template v-if="canPrintReceivedGoods(record)">
            <a @click="printSkuLabels(record)">{{ t('platform.inbound.productLabels') }}</a>
            <a @click="printGoodsReference(record)">{{ t('platform.inbound.goodsReference') }}</a>
          </template>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 收货作业抽屉（扫码枪驱动） -->
  <receive-scan-drawer ref="receiveDrawerRef" @success="onReceived" />
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { isSuccess } from '@/api'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { OperationGroup } from '@/components/Operation'
import { SearchActions } from '@/components/Search'
import { mergePageParam } from '@/utils/page-utils'
import { getInboundOpsDetail, pageInboundOps } from '@/api/wms/inbound-execution'
import type {
  PurchaseInboundDetailVO,
  PurchaseInboundPageVO,
  PurchaseInboundQO
} from '@/api/wms/purchase-inbound/types'
import { InboundStatus } from '@/api/wms/purchase-inbound/types'
import InboundStatusBadge from '@/views/wms/purchase-inbound/components/InboundStatusBadge.vue'
import WmsOperatorSelect from '@/components/Lov/WmsOperatorSelect.vue'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import UserSelect from '@/components/Lov/UserSelect.vue'
import { useUserData } from '@/hooks/use-user-data'
import ReceiveScanDrawer from './ReceiveScanDrawer.vue'
import {
  printReceivedGoodsReference,
  printReceivedSkuLabels
} from './received-goods-print'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'

const router = useRouter()
const { t, locale } = useI18n()
const tableRef = ref<ProTableInstanceExpose>()
const { allUsers: userOptions, loading: usersLoading, loadAllUsers } = useUserData()

// 收货页覆盖「已提交/已收货/已完成」三个阶段：收货后单据保留在本页（状态变已收货），只是收货动作不再可点
const RECEIVE_SCOPE = [InboundStatus.SUBMITTED, InboundStatus.RECEIVED, InboundStatus.COMPLETED]
const statusKey = (status: InboundStatus) =>
  ({
    [InboundStatus.DRAFT]: 'platform.inbound.status.draft',
    [InboundStatus.SUBMITTED]: 'platform.inbound.status.submitted',
    [InboundStatus.RECEIVED]: 'platform.inbound.status.received',
    [InboundStatus.COMPLETED]: 'platform.inbound.status.completed',
    [InboundStatus.CANCELLED]: 'platform.inbound.status.cancelled'
  })[status]
const statusOptions = computed(() => RECEIVE_SCOPE.map(status => ({ value: status, label: t(statusKey(status)) })))

const searchModel = reactive<PurchaseInboundQO>({
  inboundNo: undefined,
  orderStatus: undefined,
  wmsTenantId: undefined,
  erpTenantId: undefined,
  receiveBy: undefined
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
  // 选定单一状态则按其过滤；否则展示收货页作用域内全部（含已收货，保留可见）
  const statusFilter = searchParams.orderStatus
    ? { orderStatus: searchParams.orderStatus }
    : { orderStatuses: RECEIVE_SCOPE }
  return pageInboundOps({
    ...pageParam,
    inboundNo: searchParams.inboundNo,
    wmsTenantId: searchParams.wmsTenantId,
    erpTenantId: searchParams.erpTenantId,
    receiveBy: searchParams.receiveBy,
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
  searchModel.receiveBy = undefined
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
    dataIndex: 'receiveByName',
    key: 'receiveByName',
    width: 90,
    ellipsis: true
  },
  { title: t('platform.common.status'), key: 'status', width: 110, align: 'center' },
  { title: t('platform.common.createdAt'), dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: t('platform.common.operation'), key: 'operate', width: 230, align: 'center', fixed: 'right' }
])

const receiveDrawerRef = ref<InstanceType<typeof ReceiveScanDrawer>>()
const openReceive = (record: PurchaseInboundPageVO) => receiveDrawerRef.value?.open(record)
const canPrintReceivedGoods = (record: PurchaseInboundPageVO) =>
  [InboundStatus.RECEIVED, InboundStatus.COMPLETED].includes(record.orderStatus as InboundStatus)

type ReceivedGoodsPrinter = (
  detail: PurchaseInboundDetailVO,
  printPage?: Window | null
) => Promise<unknown>

const printReceivedGoods = async (
  record: PurchaseInboundPageVO,
  title: string,
  printer: ReceivedGoodsPrinter
) => {
  const printPage = window.open('', '_blank', 'width=920,height=760')
  if (!printPage) {
    message.error(t('platform.inbound.printBlocked'))
    return
  }
  printPage.document.write(
    `<!doctype html><html lang="${locale.value}"><head><meta charset="UTF-8"><title>${title}</title></head>` +
      `<body style="font-family:Arial,Microsoft YaHei;padding:24px">${t('platform.inbound.loading')}</body></html>`
  )
  printPage.document.close()
  try {
    const response = await getInboundOpsDetail(record.id)
    if (!isSuccess(response) || !response.data) throw new Error(t('platform.inbound.detailLoadFailed'))
    await printer(response.data, printPage)
  } catch (error) {
    printPage.close()
    message.error(error instanceof Error ? error.message : t('platform.inbound.generateFailed', { title }))
  }
}

const printSkuLabels = (record: PurchaseInboundPageVO) =>
  printReceivedGoods(record, t('platform.inbound.productLabels'), printReceivedSkuLabels)

const printGoodsReference = (record: PurchaseInboundPageVO) =>
  printReceivedGoods(record, t('platform.inbound.goodsReference'), printReceivedGoodsReference)

onMounted(loadAllUsers)

// 收货完成：单据状态已流转为「已收货」，仍保留在本页；询问是否前往上架
const onReceived = () => {
  reloadTable()
  Modal.confirm({
    title: t('platform.inbound.receivedTitle'),
    content: t('platform.inbound.receivedPrompt'),
    okText: t('platform.inbound.goPutaway'),
    cancelText: t('platform.inbound.stay'),
    onOk: () => router.push('/ops/putaway')
  })
}
</script>

<script lang="ts">
export default {
  name: 'InboundReceivePage'
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
</style>
