<template>
  <!-- ① 搜索栏 -->
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="searchModel" :label-col="{ style: { width: '80px' } }">
      <a-row :gutter="[16, 8]">
        <a-col :xl="8" :lg="10" :md="12" :sm="24">
          <a-form-item label="账期">
            <a-range-picker
              v-model:value="monthRange"
              picker="month"
              value-format="YYYY-MM"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="状态">
            <a-select
              v-model:value="searchModel.statuses"
              mode="multiple"
              placeholder="全部"
              allow-clear
              :options="statusOptions"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="6" :md="12" :sm="24">
          <search-actions :loading="tableRef?.loading" @search="searchTable" @reset="resetSearch" />
        </a-col>
      </a-row>
    </a-form>
  </a-card>

  <!-- ② 账单列表（只读对账） -->
  <pro-table
    ref="tableRef"
    header-title="支出账单（应付仓库服务费，只读对账）"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 900 }"
    size="middle"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'rackFee'">{{ formatCurrency(record.rackFee, record.currency) }}</template>
      <template v-else-if="column.key === 'operationFee'">
        {{ formatCurrency(operationSubtotal(record), record.currency) }}
      </template>
      <template v-else-if="column.key === 'totalAmount'">
        <span class="total">{{ formatCurrency(record.totalAmount, record.currency) }}</span>
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag :color="EXPENSE_STATUS_MAP[record.status]?.color">
          {{ EXPENSE_STATUS_MAP[record.status]?.label ?? record.status }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'operate'">
        <a @click="openDetail(record)">查看</a>
      </template>
    </template>
  </pro-table>

  <!-- ③ 费用构成抽屉 -->
  <a-drawer v-model:open="detailOpen" :title="`账单明细 · ${detail?.billMonth ?? ''}`" :width="480">
    <template v-if="detail">
      <div class="bill-head">
        <a-tag :color="EXPENSE_STATUS_MAP[detail.status]?.color">
          {{ EXPENSE_STATUS_MAP[detail.status]?.label }}
        </a-tag>
        <span class="bill-total">{{ formatCurrency(detail.totalAmount, detail.currency) }}</span>
      </div>
      <a-alert
        v-if="detail.status === 'DISPUTED' && detail.remark"
        type="error"
        show-icon
        :message="`争议原因：${detail.remark}`"
        style="margin-bottom: 12px"
      />
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="货架租金"
          >{{ formatCurrency(detail.rackFee, detail.currency) }}</a-descriptions-item
        >
        <a-descriptions-item label="入库费"
          >{{ formatCurrency(detail.inboundFee, detail.currency) }}</a-descriptions-item
        >
        <a-descriptions-item label="出库费"
          >{{ formatCurrency(detail.outboundFee, detail.currency) }}</a-descriptions-item
        >
        <a-descriptions-item label="配送费"
          >{{ formatCurrency(detail.deliveryFee, detail.currency) }}</a-descriptions-item
        >
        <a-descriptions-item label="退货费"
          >{{ formatCurrency(detail.returnFee, detail.currency) }}</a-descriptions-item
        >
        <a-descriptions-item label="验货费"
          >{{ formatCurrency(detail.inspectionFee, detail.currency) }}</a-descriptions-item
        >
        <a-descriptions-item label="司机费"
          >{{ formatCurrency(detail.driverFee, detail.currency) }}</a-descriptions-item
        >
        <a-descriptions-item label="操作费小计">
          {{ formatCurrency(operationSubtotal(detail), detail.currency) }}
        </a-descriptions-item>
        <a-descriptions-item label="合计应付">
          <span class="bill-total">{{ formatCurrency(detail.totalAmount, detail.currency) }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="确认时间">{{
          detail.confirmedTime || '—'
        }}</a-descriptions-item>
        <a-descriptions-item label="付款时间">{{ detail.paidTime || '—' }}</a-descriptions-item>
      </a-descriptions>
      <a-alert
        type="info"
        show-icon
        style="margin-top: 12px"
        message="本页为对账视图：结算线下进行，状态由双方线下确认后同步。"
      />
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { SearchActions } from '@/components/Search'
import { mergePageParam } from '@/utils/page-utils'
import { isSuccess } from '@/api'
import { pageExpenseBills, getExpenseBillDetail } from '@/api/wms/operator-finance'
import type { ExpenseBillVO } from '@/api/wms/operator-finance/types'
import { EXPENSE_STATUS_MAP } from '@/api/wms/operator-finance/types'
import { formatCurrency } from '@/views/wms/finance-income/currency'

const tableRef = ref<ProTableInstanceExpose>()
const monthRange = ref<[string, string]>()
const searchModel = reactive<{ statuses?: string[] }>({})
let searchParams: { billMonthStart?: string; billMonthEnd?: string; statuses?: string[] } = {}

const statusOptions = Object.entries(EXPENSE_STATUS_MAP).map(([value, meta]) => ({
  value,
  label: meta.label
}))

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageExpenseBills(pageParam, searchParams)
}
const reloadTable = (resetPageIndex?: boolean) => tableRef.value?.actionRef?.reload(resetPageIndex)
const searchTable = () => {
  searchParams = {
    billMonthStart: monthRange.value?.[0],
    billMonthEnd: monthRange.value?.[1],
    statuses: searchModel.statuses?.length ? searchModel.statuses : undefined
  }
  reloadTable(true)
}
const resetSearch = () => {
  monthRange.value = undefined
  searchModel.statuses = undefined
  searchTable()
}

const columns: ProColumns[] = [
  { title: '账期', dataIndex: 'billMonth', key: 'billMonth', width: 100, fixed: 'left' },
  { title: '货架租金', key: 'rackFee', width: 120, align: 'right' },
  { title: '操作费小计', key: 'operationFee', width: 120, align: 'right' },
  { title: '合计应付', key: 'totalAmount', width: 140, align: 'right' },
  { title: '状态', key: 'status', width: 100, align: 'center' },
  { title: '确认时间', dataIndex: 'confirmedTime', key: 'confirmedTime', width: 170 },
  { title: '付款时间', dataIndex: 'paidTime', key: 'paidTime', width: 170 },
  { title: '操作', key: 'operate', width: 80, align: 'center', fixed: 'right' }
]

const operationSubtotal = (b: ExpenseBillVO) =>
  Number(b.inboundFee ?? 0) +
  Number(b.outboundFee ?? 0) +
  Number(b.deliveryFee ?? 0) +
  Number(b.returnFee ?? 0) +
  Number(b.inspectionFee ?? 0) +
  Number(b.driverFee ?? 0)

// 详情抽屉
const detailOpen = ref(false)
const detail = ref<ExpenseBillVO | null>(null)
async function openDetail(record: ExpenseBillVO) {
  detailOpen.value = true
  detail.value = record
  const res = await getExpenseBillDetail(record.id)
  if (isSuccess(res) && res.data) {
    detail.value = res.data
  }
}
</script>

<script lang="ts">
export default {
  name: 'OperatorExpensePage'
}
</script>

<style scoped>
.total {
  font-weight: 600;
}
.bill-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.bill-total {
  font-size: 18px;
  font-weight: 700;
}
</style>
