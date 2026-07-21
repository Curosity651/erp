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
          <a-form-item label="货主">
            <erp-tenant-select v-model:value="searchModel.erpTenantId" />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="6" :md="12" :sm="24">
          <search-actions :loading="loading" @search="loadData" @reset="resetSearch" />
        </a-col>
      </a-row>
    </a-form>
  </a-card>

  <!-- ② 统计卡 -->
  <a-row :gutter="16" style="margin-bottom: 16px">
    <a-col :span="12">
      <a-card :bordered="false">
        <a-statistic
          title="区间收入合计"
          :value="summary?.totalAmount ?? 0"
          :precision="2"
          prefix="₽"
        />
      </a-card>
    </a-col>
    <a-col :span="12">
      <a-card :bordered="false">
        <a-statistic title="产品使用总次数" :value="summary?.totalCount ?? 0" suffix="次" />
      </a-card>
    </a-col>
  </a-row>

  <!-- ③ 按产品×月汇总 -->
  <a-card :bordered="false" title="收入汇总（按物流产品 × 账期）">
    <a-table
      :data-source="summary?.rows ?? []"
      :loading="loading"
      :pagination="{
        pageSize: 20,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '50', '100']
      }"
      row-key="rowKey"
      size="middle"
    >
      <a-table-column title="账期" data-index="billMonth" :width="100" />
      <a-table-column title="物流产品" :width="200">
        <template #default="{ record }">
          <a @click="openRecords(record)">{{ record.productName }}</a>
        </template>
      </a-table-column>
      <a-table-column title="特性词条" :width="220">
        <template #default="{ record }">
          <a-tag v-for="t in record.tags" :key="t" :color="tagColor(t)">{{ t }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="单价" align="right" :width="120">
        <template #default="{ record }">₽ {{ formatMoney(record.unitPrice) }}</template>
      </a-table-column>
      <a-table-column title="使用次数" data-index="usageCount" align="right" :width="100" />
      <a-table-column title="小计" align="right" :width="140">
        <template #default="{ record }">
          <span class="subtotal">₽ {{ formatMoney(record.subtotal) }}</span>
        </template>
      </a-table-column>
    </a-table>
  </a-card>

  <!-- ④ 明细流水抽屉 -->
  <a-drawer v-model:open="recordsOpen" :title="recordsTitle" :width="720" placement="right">
    <a-table
      :data-source="records"
      :loading="recordsLoading"
      :pagination="{ pageSize: 20 }"
      row-key="id"
      size="small"
    >
      <a-table-column title="时间" data-index="createTime" :width="160" />
      <a-table-column title="货主" data-index="ownerName" :width="140" />
      <a-table-column title="出库单号" data-index="outboundNo" :width="180" />
      <a-table-column title="跟踪号" data-index="trackingNo" :width="140" />
      <a-table-column title="金额" align="right" :width="110">
        <template #default="{ record }">₽ {{ formatMoney(record.amount) }}</template>
      </a-table-column>
    </a-table>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { isSuccess } from '@/api'
import { SearchActions } from '@/components/Search'
import ErpTenantSelect from '@/components/Lov/ErpTenantSelect.vue'
import { getIncomeSummary, listIncomeRecords } from '@/api/wms/operator-finance'
import type {
  IncomeRecord,
  IncomeSummary,
  IncomeSummaryRow
} from '@/api/wms/operator-finance/types'

const loading = ref(false)
const monthRange = ref<[string, string]>()
const searchModel = reactive<{ erpTenantId?: number }>({})

type RowWithKey = IncomeSummaryRow & { rowKey: string }
const summary = ref<(IncomeSummary & { rows: RowWithKey[] }) | null>(null)

async function loadData() {
  loading.value = true
  try {
    const res = await getIncomeSummary({
      monthStart: monthRange.value?.[0],
      monthEnd: monthRange.value?.[1],
      erpTenantId: searchModel.erpTenantId
    })
    if (isSuccess(res) && res.data) {
      summary.value = {
        ...res.data,
        rows: (res.data.rows || []).map(r => ({ ...r, rowKey: `${r.productId}-${r.billMonth}` }))
      }
    }
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  monthRange.value = undefined
  searchModel.erpTenantId = undefined
  loadData()
}

// 明细抽屉
const recordsOpen = ref(false)
const recordsLoading = ref(false)
const recordsTitle = ref('')
const records = ref<IncomeRecord[]>([])

async function openRecords(row: RowWithKey) {
  recordsTitle.value = `明细流水 · ${row.productName} · ${row.billMonth}`
  recordsOpen.value = true
  recordsLoading.value = true
  records.value = []
  try {
    const res = await listIncomeRecords({
      monthStart: row.billMonth,
      monthEnd: row.billMonth,
      erpTenantId: searchModel.erpTenantId,
      productId: row.productId
    })
    if (isSuccess(res) && res.data) {
      records.value = res.data
    }
  } finally {
    recordsLoading.value = false
  }
}

const TAG_COLORS = ['blue', 'green', 'orange', 'purple', 'cyan', 'magenta', 'geekblue', 'volcano']
const tagColor = (tag: string) => {
  let h = 0
  for (let i = 0; i < tag.length; i++) h = (h * 31 + tag.charCodeAt(i)) % 997
  return TAG_COLORS[h % TAG_COLORS.length]
}
const formatMoney = (v?: number) =>
  v == null
    ? '0.00'
    : Number(v).toLocaleString('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

onMounted(() => loadData())
</script>

<script lang="ts">
export default {
  name: 'OperatorIncomePage'
}
</script>

<style scoped>
.subtotal {
  font-weight: 600;
  color: #52c41a;
}
</style>
