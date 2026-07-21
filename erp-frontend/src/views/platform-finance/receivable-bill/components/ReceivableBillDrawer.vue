<template>
  <a-drawer
    :open="open"
    title="应收账单详情"
    width="560"
    :body-style="{ paddingBottom: '80px' }"
    @close="handleClose"
  >
    <a-spin :spinning="loading">
      <template v-if="bill">
        <!-- 头部 -->
        <div class="bill-head">
          <div class="head-top">
            <div>
              <div class="bill-month">{{ bill.billMonth }} 账期</div>
              <div class="bill-operator">{{ bill.wmsTenantName }}</div>
            </div>
            <a-tag :color="BILL_STATUS_COLOR[bill.status]" class="status-tag">
              {{ BILL_STATUS_TEXT[bill.status] }}
            </a-tag>
          </div>
          <div class="total-amount">{{ formatMoney(bill.totalAmount) }}</div>
          <div class="head-times">
            <span>确认时间：{{ bill.confirmedTime || '—' }}</span>
            <span>付款时间：{{ bill.paidTime || '—' }}</span>
          </div>
          <a-alert
            v-if="bill.status === 'DISPUTED' && bill.remark"
            type="error"
            show-icon
            :message="`争议原因：${bill.remark}`"
            style="margin-top: 12px"
          />
        </div>

        <!-- 费用构成 -->
        <div class="fee-section">
          <div class="section-title">费用构成</div>
          <div class="fee-row">
            <span class="fee-label">货架租金</span>
            <span class="fee-value">{{ formatMoney(bill.rackFee) }}</span>
          </div>

          <div class="fee-group-title">操作费</div>
          <div v-for="f in opFields" :key="f.key" class="fee-row indent">
            <span class="fee-label">{{ f.label }}</span>
            <span class="fee-value">{{ formatMoney(bill[f.key] as number) }}</span>
          </div>
          <div class="fee-row subtotal">
            <span class="fee-label">操作费小计</span>
            <span class="fee-value">{{ formatMoney(operationSubtotal(bill)) }}</span>
          </div>

          <div class="fee-row total">
            <span class="fee-label">合计应收</span>
            <span class="fee-value">{{ formatMoney(bill.totalAmount) }}</span>
          </div>
        </div>
      </template>
      <a-empty v-else-if="!loading" description="未找到账单" />
    </a-spin>

    <!-- 底部状态操作（跟随状态机） -->
    <div v-if="bill" class="drawer-footer">
      <a-space>
        <template v-if="bill.status === 'DRAFT'">
          <a-button type="primary" :loading="acting" @click="doConfirm">确认账单</a-button>
        </template>
        <template v-else-if="bill.status === 'CONFIRMED'">
          <a-button type="primary" :loading="acting" @click="doPay">标记已付款</a-button>
          <a-button danger :loading="acting" @click="openDispute">标记争议</a-button>
        </template>
        <template v-else-if="bill.status === 'DISPUTED'">
          <a-button type="primary" :loading="acting" @click="doConfirm">重新确认</a-button>
        </template>
        <span v-else class="readonly-hint">账单已付款，不可再操作</span>
      </a-space>
    </div>

    <!-- 争议原因弹窗 -->
    <a-modal
      v-model:open="disputeVisible"
      title="标记争议"
      :confirm-loading="acting"
      @ok="doDispute"
    >
      <a-textarea
        v-model:value="disputeRemark"
        :rows="3"
        placeholder="请填写争议原因（将展示在账单上）"
      />
    </a-modal>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { getMonthlyBillDetail, changeBillStatus } from '@/api/platform-finance/receivable'
import type { MonthlyBillVO } from '@/api/platform-finance/receivable/types'
import { isSuccess } from '@/api'
import {
  BILL_STATUS_TEXT,
  BILL_STATUS_COLOR,
  FEE_FIELDS,
  formatMoney,
  operationSubtotal
} from '../constants'

const props = defineProps<{ open: boolean; billId?: number }>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'changed'): void
}>()

const opFields = FEE_FIELDS.filter(f => f.group === 'op')

const loading = ref(false)
const acting = ref(false)
const bill = ref<MonthlyBillVO | null>(null)

const disputeVisible = ref(false)
const disputeRemark = ref('')

async function loadDetail(id: number) {
  loading.value = true
  bill.value = null
  try {
    const res = await getMonthlyBillDetail(id)
    if (isSuccess(res) && res.data) bill.value = res.data
  } catch (e) {
    console.error('加载账单详情失败', e)
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.open, props.billId] as const,
  ([open, id]) => {
    if (open && id) loadDetail(id)
  },
  { immediate: true }
)

function handleClose() {
  emit('update:open', false)
}

async function transition(target: MonthlyBillVO['status'], remark?: string) {
  if (!bill.value) return
  acting.value = true
  try {
    const res = await changeBillStatus(bill.value.id, target, remark)
    if (isSuccess(res) && res.data) {
      bill.value = res.data
      message.success('操作成功')
      emit('changed')
    } else {
      message.error(res.message || '操作失败')
    }
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  } finally {
    acting.value = false
  }
}

function doConfirm() {
  transition('CONFIRMED')
}
function doPay() {
  transition('PAID')
}
function openDispute() {
  disputeRemark.value = ''
  disputeVisible.value = true
}
async function doDispute() {
  if (!disputeRemark.value.trim()) {
    message.warning('请填写争议原因')
    return
  }
  await transition('DISPUTED', disputeRemark.value.trim())
  disputeVisible.value = false
}
</script>

<style scoped>
.bill-head {
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.head-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.bill-month {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.bill-operator {
  margin-top: 2px;
  font-size: 13px;
  color: #8c8c8c;
}

.status-tag {
  margin: 0;
  font-weight: 600;
}

.total-amount {
  margin-top: 14px;
  font-size: 30px;
  font-weight: 700;
  color: #1890ff;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
}

.head-times {
  margin-top: 8px;
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #8c8c8c;
}

.fee-section {
  margin-top: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 12px;
}

.fee-group-title {
  margin: 12px 0 6px;
  font-size: 12px;
  color: #8c8c8c;
}

.fee-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 14px;
  border-bottom: 1px dashed #f0f0f0;
}

.fee-row.indent .fee-label {
  padding-left: 12px;
  color: #595959;
}

.fee-label {
  color: #262626;
}

.fee-value {
  font-weight: 500;
  color: #262626;
  font-variant-numeric: tabular-nums;
}

.fee-row.subtotal {
  border-bottom: none;
}

.fee-row.subtotal .fee-label,
.fee-row.subtotal .fee-value {
  color: #8c8c8c;
}

.fee-row.total {
  margin-top: 8px;
  border-top: 2px solid #f0f0f0;
  border-bottom: none;
}

.fee-row.total .fee-label {
  font-weight: 600;
}

.fee-row.total .fee-value {
  font-size: 18px;
  font-weight: 700;
  color: #1890ff;
}

.drawer-footer {
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  padding: 12px 24px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  text-align: right;
}

.readonly-hint {
  font-size: 13px;
  color: #8c8c8c;
}
</style>
