<template>
  <div class="fund-panel">
    <div class="fund-summary-strip">
      <button
        v-for="account in accounts"
        :key="accountKey(account)"
        type="button"
        class="fund-summary-item"
        :class="{ active: accountKey(account) === selectedAccountKey, negative: account.negative }"
        @click="selectAccount(account)"
      >
        <span class="fund-label">{{ account.wmsTenantName || 'WMS服务商' }} · {{ account.currency }}</span>
        <span class="fund-balance">{{ money(account.balance) }} <small>{{ account.currency }}</small></span>
        <span class="fund-detail">累计充值 {{ money(account.rechargeAmount) }} · 物流费用 {{ money(account.chargeAmount) }}</span>
      </button>
      <div v-if="!accounts.length" class="fund-summary-item empty-account">
        <span class="fund-label">WMS资金账户</span>
        <span class="fund-balance">暂无余额</span>
        <span class="fund-detail">登记充值后显示账户信息</span>
      </div>
    </div>

    <a-tabs class="account-detail-tabs" @change="onAccountTabChange">
      <a-tab-pane key="recharge" tab="充值记录">
        <a-table
          :data-source="recharges"
          :columns="rechargeColumns"
          row-key="id"
          size="small"
          :scroll="{ x: 1000 }"
          :pagination="{ pageSize: 10 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'amount'">{{ money(record.amount) }} {{ record.currency }}</template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="statusColor[record.status]">{{ statusText[record.status] }}</a-tag>
            </template>
            <template v-else-if="column.key === 'voucher'">
              <a v-if="record.voucherUrl" :href="record.voucherUrl" target="_blank">查看凭证</a>
              <span v-else>--</span>
            </template>
            <template v-else-if="column.key === 'operate'">
              <a-popconfirm
                v-if="record.status === 'PENDING'"
                title="确认取消这条充值申请？"
                @confirm="cancel(record.id)"
              >
                <a>取消</a>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <a-tab-pane key="ledger" tab="资金流水">
        <div class="ledger-toolbar">
          <span class="ledger-note">按月汇总，展开月份查看单笔流水</span>
          <a-button :disabled="!selectedAccount" @click="exportOpen = true">
            <template #icon><Download :size="16" /></template>
            导出流水
          </a-button>
        </div>
        <a-table
          v-model:expandedRowKeys="expandedMonthKeys"
          :loading="ledgerLoading"
          :data-source="ledgerMonths"
          :columns="ledgerMonthColumns"
          row-key="month"
          size="small"
          :scroll="{ x: 870 }"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'money'">{{ money(record[column.dataIndex]) }} {{ record.currency }}</template>
            <template v-else-if="column.key === 'net'">
              <span :class="record.netChange < 0 ? 'out' : 'in'">
                {{ record.netChange > 0 ? '+' : '' }}{{ money(record.netChange) }} {{ record.currency }}
              </span>
            </template>
          </template>
          <template #expandedRowRender="{ record }">
            <a-table
              :data-source="record.details"
              :columns="ledgerDetailColumns"
              :row-key="(row: FundLedger) => `${row.businessNo}-${row.occurredTime}`"
              size="small"
              :pagination="false"
            >
              <template #bodyCell="{ column, record: detail }">
                <template v-if="column.key === 'amount'">
                  <span :class="detail.amount < 0 ? 'out' : 'in'">
                    {{ detail.amount > 0 ? '+' : '' }}{{ money(detail.amount) }} {{ detail.currency }}
                  </span>
                </template>
              </template>
            </a-table>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <RechargeCreateModal v-model:open="rechargeOpen" target="owner" @saved="load" />
    <a-modal v-model:open="exportOpen" title="导出资金流水" :confirm-loading="exporting" @ok="submitExport">
      <a-form layout="vertical">
        <a-form-item label="资金账户">
          <a-input :value="selectedAccountLabel" disabled />
        </a-form-item>
        <a-form-item label="导出日期范围" required>
          <a-range-picker v-model:value="exportRange" style="width: 100%" />
        </a-form-item>
      </a-form>
      <a-alert type="info" show-icon message="Excel 包含“月度汇总”和“流水明细”两个工作表。" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { Download } from 'lucide-vue-next'
import { isSuccess } from '@/api'
import {
  cancelOwnerRecharge,
  exportOwnerLedger,
  getOwnerFundAccounts,
  getOwnerLedgerMonths,
  getOwnerRecharges
} from '@/api/fund-settlement'
import type { FundAccount, FundLedger, FundLedgerMonth, RechargeOrder, RechargeStatus } from '@/api/fund-settlement/types'
import { remoteFileDownload } from '@/utils/file-utils'
import RechargeCreateModal from './RechargeCreateModal.vue'
import { defaultExpandedMonths } from './owner-ledger-view'

const emit = defineEmits<{ updated: [value: string] }>()

const accounts = ref<FundAccount[]>([])
const recharges = ref<RechargeOrder[]>([])
const ledgerMonths = ref<FundLedgerMonth[]>([])
const loading = ref(false)
const ledgerLoading = ref(false)
const rechargeOpen = ref(false)
const exportOpen = ref(false)
const exporting = ref(false)
const selectedAccount = ref<FundAccount>()
const expandedMonthKeys = ref<string[]>([])
const exportRange = ref<[Dayjs, Dayjs]>([dayjs().startOf('month'), dayjs()])

const rechargeColumns = [
  { title: '充值单号', dataIndex: 'rechargeNo', width: 180, fixed: 'left' as const },
  { title: '金额', key: 'amount', width: 130 },
  { title: '付款时间', dataIndex: 'paymentTime', width: 170 },
  { title: '凭证', key: 'voucher', width: 90 },
  { title: '状态', key: 'status', width: 100 },
  { title: '审核人', dataIndex: 'reviewerName', width: 120 },
  { title: '备注', dataIndex: 'remark', width: 180 },
  { title: '操作', key: 'operate', width: 80, fixed: 'right' as const }
]
const ledgerMonthColumns = [
  { title: '月份', dataIndex: 'month', width: 100, fixed: 'left' as const },
  { title: '充值合计', dataIndex: 'rechargeAmount', key: 'money', width: 145, align: 'right' as const },
  { title: '物流费用', dataIndex: 'chargeAmount', key: 'money', width: 145, align: 'right' as const },
  { title: '冲正金额', dataIndex: 'reversalAmount', key: 'money', width: 135, align: 'right' as const },
  { title: '本月净变动', key: 'net', width: 155, align: 'right' as const },
  { title: '月初余额', dataIndex: 'openingBalance', key: 'money', width: 145, align: 'right' as const },
  { title: '月末/当前余额', dataIndex: 'closingBalance', key: 'money', width: 160, align: 'right' as const }
]
const ledgerDetailColumns = [
  { title: '时间', dataIndex: 'occurredTime', width: 175 },
  { title: '类型', dataIndex: 'entryTypeLabel', width: 110 },
  { title: '业务单据', dataIndex: 'businessLabel', width: 190 },
  { title: '说明', dataIndex: 'descriptionLabel' },
  { title: '操作人', dataIndex: 'operatorName', width: 120 },
  { title: '金额', key: 'amount', width: 155, align: 'right' as const }
]
const statusText: Record<RechargeStatus, string> = {
  PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已取消', REVERSED: '已冲正'
}
const statusColor: Record<RechargeStatus, string> = {
  PENDING: 'orange', APPROVED: 'green', REJECTED: 'red', CANCELLED: 'default', REVERSED: 'purple'
}

const selectedAccountKey = computed(() => selectedAccount.value ? accountKey(selectedAccount.value) : '')
const selectedAccountLabel = computed(() => selectedAccount.value
  ? `${selectedAccount.value.wmsTenantName || 'WMS服务商'} / ${selectedAccount.value.currency}`
  : '-')
const money = (value: number) => Number(value || 0).toLocaleString('zh-CN', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
})

function accountKey(account: FundAccount) {
  return `${account.wmsTenantId}-${account.currency}`
}

async function loadLedger() {
  if (!selectedAccount.value) {
    ledgerMonths.value = []
    expandedMonthKeys.value = []
    return
  }
  ledgerLoading.value = true
  try {
    const result = await getOwnerLedgerMonths({ currency: selectedAccount.value.currency })
    if (isSuccess(result)) {
      ledgerMonths.value = result.data || []
      expandedMonthKeys.value = defaultExpandedMonths(ledgerMonths.value)
    }
  }
  finally {
    ledgerLoading.value = false
  }
}

async function selectAccount(account: FundAccount) {
  selectedAccount.value = account
  await loadLedger()
}

function onAccountTabChange(key: string) {
  if (key === 'ledger') expandedMonthKeys.value = []
}

async function load() {
  loading.value = true
  try {
    const previousKey = selectedAccountKey.value
    const [accountResult, rechargeResult] = await Promise.all([getOwnerFundAccounts(), getOwnerRecharges()])
    if (isSuccess(accountResult)) {
      accounts.value = accountResult.data || []
      selectedAccount.value = accounts.value.find(account => accountKey(account) === previousKey) || accounts.value[0]
    }
    if (isSuccess(rechargeResult)) recharges.value = rechargeResult.data || []
    await loadLedger()
    emit('updated', new Date().toLocaleString('zh-CN', { hour12: false }))
  }
  finally {
    loading.value = false
  }
}

async function submitExport() {
  if (!selectedAccount.value || !exportRange.value) {
    message.warning('请选择资金账户和导出日期范围')
    return
  }
  exporting.value = true
  try {
    const response = await exportOwnerLedger({
      wmsTenantId: selectedAccount.value.wmsTenantId,
      currency: selectedAccount.value.currency,
      startDate: exportRange.value[0].format('YYYY-MM-DD'),
      endDate: exportRange.value[1].format('YYYY-MM-DD')
    })
    remoteFileDownload(response)
    exportOpen.value = false
    message.success('资金流水已导出')
  }
  finally {
    exporting.value = false
  }
}

async function cancel(id: number) {
  const result = await cancelOwnerRecharge(id)
  if (isSuccess(result)) {
    message.success('已取消')
    await load()
  }
}

onMounted(load)

function openRecharge() {
  rechargeOpen.value = true
}

defineExpose({
  loading,
  openRecharge,
  refresh: load
})
</script>

<style scoped>
.fund-summary-strip{display:flex;gap:10px;overflow-x:auto;padding:1px 1px 3px}
.fund-summary-item{flex:1 0 210px;min-height:88px;padding:11px 14px;border:1px solid #e8e8e8;border-radius:6px;background:#fff;color:#262626;text-align:left;cursor:pointer;transition:border-color .15s,box-shadow .15s,background-color .15s}
button.fund-summary-item:hover{border-color:#91caff}
.fund-summary-item.active{border-color:#1677ff;background:#f5f9ff;box-shadow:0 0 0 1px #1677ff}
.fund-summary-item.negative{border-color:#ffccc7}
.fund-summary-item.negative.active{border-color:#1677ff}
.fund-label,.fund-balance,.fund-detail{display:block}
.fund-label{color:#8c8c8c;font-size:12px;line-height:18px}
.fund-balance{margin-top:3px;font-size:21px;line-height:28px;font-weight:650;font-variant-numeric:tabular-nums}
.negative .fund-balance{color:#cf1322}
.fund-balance small{font-size:12px;font-weight:400}
.fund-detail{margin-top:4px;color:#8c8c8c;font-size:11px;white-space:nowrap}
.empty-account{cursor:default}
.empty-account .fund-balance{color:#8c8c8c;font-size:18px}
.account-detail-tabs{margin-top:10px}
.ledger-toolbar{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}
.ledger-note{color:#8c8c8c;font-size:12px}
.in{color:#389e0d}.out{color:#cf1322}
</style>
