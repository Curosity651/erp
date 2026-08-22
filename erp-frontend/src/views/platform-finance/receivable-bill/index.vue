<template>
  <div class="receivable-page">
    <a-card :bordered="false" class="search-card">
      <a-form layout="inline" :model="filters" class="receivable-search">
        <a-form-item label="账期">
          <a-range-picker
            v-model:value="monthRange"
            picker="month"
            :placeholder="['开始账期', '结束账期']"
            style="width: 240px"
            @change="handleMonthChange"
          />
        </a-form-item>
        <a-form-item label="WMS服务商">
          <a-select
            v-model:value="filters.wmsTenantId"
            :options="operatorOptions"
            :loading="operatorLoading"
            placeholder="全部服务商"
            allow-clear
            show-search
            option-filter-prop="label"
            style="width: 190px"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="filters.statuses"
            mode="multiple"
            :options="BILL_STATUS_OPTIONS"
            placeholder="全部状态"
            allow-clear
            :max-tag-count="2"
            style="width: 190px"
          />
        </a-form-item>
        <a-form-item class="search-actions-item">
          <search-actions :loading="tableRef?.loading" @search="reload(true)" @reset="reset" />
        </a-form-item>
      </a-form>
    </a-card>

    <pro-table
      ref="tableRef"
      header-title="应收账单（平台 → WMS服务商）"
      row-key="id"
      :request="tableRequest"
      :columns="columns"
      :scroll="{ x: 1200 }"
      :pagination="{ pageSizeOptions: ['10', '20', '50', '100'] }"
      :search="false"
    >
      <template #toolBarRender>
        <a-space>
          <a-button @click="manualVisible = true">
            <template #icon><PlusOutlined /></template>
            登记补充费用
          </a-button>
          <a-button type="primary" @click="generateVisible = true">
            <template #icon><PlusOutlined /></template>
            生成账单
          </a-button>
        </a-space>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="BILL_STATUS_COLOR[record.status as BillStatus]">
            {{ BILL_STATUS_TEXT[record.status as BillStatus] }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'operate'">
          <a @click="view(record)">查看</a>
        </template>
      </template>
    </pro-table>

    <ReceivableBillDrawer
      v-model:open="drawerVisible"
      :bill-id="currentId"
      @changed="reload(false)"
    />
    <GenerateBillModal v-model:open="generateVisible" @generated="reload(false)" />
    <ManualChargeModal
      v-model:open="manualVisible"
      :operator-options="operatorOptions"
      @saved="reload(false)"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { Dayjs } from 'dayjs'
import { PlusOutlined } from '@ant-design/icons-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { SearchActions } from '@/components/Search'
import { mergePageParam } from '@/utils/page-utils'
import { isSuccess } from '@/api'
import { listWmsOperators } from '@/api/tenant'
import { pageMonthlyBill } from '@/api/platform-finance/receivable'
import type {
  BillStatus,
  MonthlyBillQO,
  MonthlyBillVO
} from '@/api/platform-finance/receivable/types'
import {
  BILL_STATUS_COLOR,
  BILL_STATUS_OPTIONS,
  BILL_STATUS_TEXT,
  formatMoney,
  operationSubtotal
} from './constants'
import GenerateBillModal from './components/GenerateBillModal.vue'
import ManualChargeModal from './components/ManualChargeModal.vue'
import ReceivableBillDrawer from './components/ReceivableBillDrawer.vue'

const tableRef = ref<ProTableInstanceExpose>()
const filters = reactive<MonthlyBillQO>({ statuses: [] })
const monthRange = ref<[Dayjs, Dayjs] | null>(null)
const operatorLoading = ref(false)
const operatorOptions = ref<{ label: string; value: number }[]>([])
const drawerVisible = ref(false)
const generateVisible = ref(false)
const manualVisible = ref(false)
const currentId = ref<number>()

const columns: ProColumns[] = [
  { title: '账期', dataIndex: 'billMonth', width: 90, fixed: 'left' },
  { title: 'WMS服务商', dataIndex: 'wmsTenantName', width: 160 },
  {
    title: '货架租金',
    dataIndex: 'rackFee',
    width: 120,
    align: 'right',
    customRender: ({ value }) => formatMoney(value)
  },
  {
    title: '操作费小计',
    width: 120,
    align: 'right',
    customRender: ({ record }) => formatMoney(operationSubtotal(record as MonthlyBillVO))
  },
  {
    title: '合计应收',
    dataIndex: 'totalAmount',
    width: 130,
    align: 'right',
    customRender: ({ value }) => formatMoney(value)
  },
  { title: '状态', key: 'status', width: 90, align: 'center' },
  { title: '确认时间', dataIndex: 'confirmedTime', width: 160 },
  { title: '付款时间', dataIndex: 'paidTime', width: 160 },
  { title: '操作', key: 'operate', width: 80, fixed: 'right', align: 'center' }
]

const tableRequest: TableRequest = (params, sorter, filter) =>
  pageMonthlyBill(mergePageParam(params, sorter, filter), {
    ...filters,
    statuses: filters.statuses?.length ? filters.statuses : undefined
  })

function handleMonthChange(dates: [Dayjs, Dayjs] | null) {
  filters.billMonthStart = dates?.[0]?.format('YYYY-MM')
  filters.billMonthEnd = dates?.[1]?.format('YYYY-MM')
}

function reload(resetPage = false) {
  tableRef.value?.actionRef?.reload(resetPage)
}

function reset() {
  Object.assign(filters, {
    billMonthStart: undefined,
    billMonthEnd: undefined,
    wmsTenantId: undefined,
    statuses: []
  })
  monthRange.value = null
  reload(true)
}

function view(record: MonthlyBillVO) {
  currentId.value = record.id
  drawerVisible.value = true
}

onMounted(async () => {
  operatorLoading.value = true
  try {
    const response = await listWmsOperators()
    if (isSuccess(response)) {
      operatorOptions.value = (response.data || []).map(row => ({
        label: row.tenantName,
        value: row.id
      }))
    }
  } finally {
    operatorLoading.value = false
  }
})
</script>

<style scoped lang="less">
.receivable-page {
  min-height: 100%;
}
.search-card {
  margin-bottom: 16px;
}
.search-card :deep(.ant-card-body) {
  min-width: 0;
}
.receivable-search {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 20px;
}
.receivable-search :deep(.ant-form-item) {
  flex: 0 0 auto;
  margin: 0;
}
.receivable-search :deep(.ant-form-item-row) {
  flex-wrap: nowrap;
  align-items: center;
}
.receivable-search :deep(.ant-form-item-label) {
  flex: 0 0 auto;
}
.receivable-search .search-actions-item {
  margin: 0;
}
</style>
