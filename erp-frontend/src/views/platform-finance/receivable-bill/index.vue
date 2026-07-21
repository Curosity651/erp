<template>
  <div class="receivable-bill-panel">
    <!-- 搜索栏 -->
    <div class="filter-section">
      <a-form layout="inline" :model="formState" class="search-form">
        <a-form-item label="账期">
          <a-range-picker
            v-model:value="monthRange"
            picker="month"
            :placeholder="['起始账期', '结束账期']"
            style="width: 240px"
            @change="handleMonthChange"
          />
        </a-form-item>

        <a-form-item label="WMS服务商">
          <a-select
            v-model:value="formState.wmsTenantId"
            :options="operatorOptions"
            :loading="operatorLoading"
            placeholder="全部服务商"
            allow-clear
            show-search
            option-filter-prop="label"
            style="width: 200px"
          />
        </a-form-item>

        <a-form-item label="状态">
          <a-select
            v-model:value="formState.statuses"
            mode="multiple"
            :options="BILL_STATUS_OPTIONS"
            placeholder="全部状态"
            allow-clear
            :max-tag-count="2"
            style="min-width: 200px"
          />
        </a-form-item>
      </a-form>

      <div class="action-buttons">
        <a-space>
          <a-button type="primary" :loading="tableRef?.loading" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            搜索
          </a-button>
          <a-button @click="handleReset">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
          <a-button type="primary" ghost @click="generateVisible = true">
            <template #icon><PlusOutlined /></template>
            生成/重算账单
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- 账单列表 -->
    <pro-table
      ref="tableRef"
      header-title="应收账单（平台 → WMS 服务商）"
      row-key="id"
      :request="tableRequest"
      :columns="columns"
      :scroll="{ x: 1200 }"
      :pagination="{ pageSizeOptions: ['10', '20', '50', '100'] }"
      :search="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="BILL_STATUS_COLOR[record.status as BillStatus]">
            {{ BILL_STATUS_TEXT[record.status as BillStatus] }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'operate'">
          <a @click="handleView(record)">查看</a>
        </template>
      </template>
    </pro-table>

    <!-- 详情抽屉 -->
    <ReceivableBillDrawer
      v-model:open="drawerVisible"
      :bill-id="currentId"
      @changed="handleChanged"
    />

    <!-- 生成/重算弹窗 -->
    <GenerateBillModal v-model:open="generateVisible" @generated="handleChanged" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons-vue'
import type { Dayjs } from 'dayjs'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { mergePageParam } from '@/utils/page-utils'
import { isSuccess } from '@/api'
import { listWmsOperators } from '@/api/tenant'
import { pageMonthlyBill } from '@/api/platform-finance/receivable'
import type {
  MonthlyBillVO,
  MonthlyBillQO,
  BillStatus
} from '@/api/platform-finance/receivable/types'
import {
  BILL_STATUS_OPTIONS,
  BILL_STATUS_TEXT,
  BILL_STATUS_COLOR,
  formatMoney,
  operationSubtotal
} from './constants'
import ReceivableBillDrawer from './components/ReceivableBillDrawer.vue'
import GenerateBillModal from './components/GenerateBillModal.vue'

const tableRef = ref<ProTableInstanceExpose>()

// ===== 搜索条件 =====
const formState = reactive<{
  billMonthStart?: string
  billMonthEnd?: string
  wmsTenantId?: number
  statuses: BillStatus[]
}>({
  billMonthStart: undefined,
  billMonthEnd: undefined,
  wmsTenantId: undefined,
  statuses: []
})

const monthRange = ref<[Dayjs, Dayjs] | null>(null)

function handleMonthChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    formState.billMonthStart = dates[0].format('YYYY-MM')
    formState.billMonthEnd = dates[1].format('YYYY-MM')
  } else {
    formState.billMonthStart = undefined
    formState.billMonthEnd = undefined
  }
}

// ===== WMS 服务商下拉 =====
const operatorLoading = ref(false)
const operatorOptions = ref<{ label: string; value: number }[]>([])

async function loadOperators() {
  operatorLoading.value = true
  try {
    const res = await listWmsOperators()
    if (isSuccess(res) && res.data) {
      operatorOptions.value = res.data.map(o => ({ label: o.tenantName, value: o.id }))
    }
  } catch (e) {
    console.error('加载 WMS 服务商失败', e)
  } finally {
    operatorLoading.value = false
  }
}

// ===== 列 =====
const columns: ProColumns[] = [
  { title: '账期', dataIndex: 'billMonth', key: 'billMonth', width: 90, fixed: 'left' },
  { title: 'WMS服务商', dataIndex: 'wmsTenantName', key: 'wmsTenantName', width: 160 },
  {
    title: '货架租金',
    dataIndex: 'rackFee',
    key: 'rackFee',
    width: 120,
    align: 'right',
    customRender: ({ value }) => formatMoney(value)
  },
  {
    title: '操作费小计',
    key: 'opSubtotal',
    width: 120,
    align: 'right',
    customRender: ({ record }) => formatMoney(operationSubtotal(record as MonthlyBillVO))
  },
  {
    title: '合计',
    dataIndex: 'totalAmount',
    key: 'totalAmount',
    width: 130,
    align: 'right',
    customRender: ({ value }) => formatMoney(value)
  },
  { title: '状态', key: 'status', width: 90, align: 'center' },
  { title: '确认时间', dataIndex: 'confirmedTime', key: 'confirmedTime', width: 160 },
  { title: '付款时间', dataIndex: 'paidTime', key: 'paidTime', width: 160 },
  { title: '操作', key: 'operate', width: 80, fixed: 'right', align: 'center' }
]

// ===== 表格请求 =====
function getQO(): MonthlyBillQO {
  return {
    billMonthStart: formState.billMonthStart,
    billMonthEnd: formState.billMonthEnd,
    wmsTenantId: formState.wmsTenantId,
    statuses: formState.statuses.length ? formState.statuses : undefined
  }
}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageMonthlyBill(pageParam, getQO())
}

function reload(resetPageIndex = false) {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

function handleSearch() {
  reload(true)
}

function handleReset() {
  formState.billMonthStart = undefined
  formState.billMonthEnd = undefined
  formState.wmsTenantId = undefined
  formState.statuses = []
  monthRange.value = null
  reload(true)
}

// ===== 详情抽屉 =====
const drawerVisible = ref(false)
const currentId = ref<number>()

function handleView(record: MonthlyBillVO) {
  currentId.value = record.id
  drawerVisible.value = true
}

// ===== 生成弹窗 =====
const generateVisible = ref(false)

// 状态流转 / 生成成功后刷新列表
function handleChanged() {
  reload(false)
}

onMounted(loadOperators)
</script>

<script lang="ts">
export default {
  name: 'ReceivableBillPage'
}
</script>

<style lang="less" scoped>
.receivable-bill-panel {
  .filter-section {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    padding: 16px;
    background: #fafafa;
    border-radius: 6px;
    border: 1px solid #f0f0f0;
    margin-bottom: 12px;

    .search-form {
      flex: 1;

      :deep(.ant-form-item) {
        margin-bottom: 8px;
        margin-right: 16px;
      }
    }

    .action-buttons {
      flex-shrink: 0;
      padding-top: 4px;
    }
  }
}
</style>
