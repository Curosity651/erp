<template>
  <a-drawer
    :open="open"
    title="应收账单详情"
    width="860"
    :body-style="{ paddingBottom: '72px' }"
    @close="emit('update:open', false)"
  >
    <a-spin :spinning="loading">
      <template v-if="bill">
        <div class="bill-head">
          <div>
            <div class="bill-month">{{ bill.billMonth }} 账期</div>
            <div class="bill-operator">{{ bill.wmsTenantName }}</div>
          </div>
          <div class="head-right">
            <div class="total-amount">{{ formatMoney(bill.totalAmount) }}</div>
            <a-tag :color="BILL_STATUS_COLOR[bill.status]">
              {{ BILL_STATUS_TEXT[bill.status] }}
            </a-tag>
          </div>
        </div>

        <a-alert
          v-if="bill.status === 'DISPUTED' && bill.remark"
          type="error"
          show-icon
          :message="`争议原因：${bill.remark}`"
          class="block"
        />

        <div class="section-title">费用汇总</div>
        <div class="summary-grid">
          <div v-for="item in summaryItems" :key="item.label" class="summary-item">
            <span>{{ item.label }}</span>
            <b>{{ formatMoney(item.value) }}</b>
          </div>
        </div>

        <div class="section-head">
          <div>
            <div class="section-title no-margin">操作费明细</div>
            <div class="section-note">按业务动作保存计费数量、单价和原始凭证</div>
          </div>
          <span class="record-count">共 {{ bill.billingRecords?.length || 0 }} 笔</span>
        </div>
        <a-table
          row-key="id"
          size="small"
          :columns="recordColumns"
          :data-source="bill.billingRecords || []"
          :pagination="false"
          :scroll="{ x: 1050 }"
        />

        <template v-if="bill.paymentVoucherFileId">
          <div class="section-title">付款凭证</div>
          <sys-file-upload
            :model-value="bill.paymentVoucherFileId"
            :disabled="true"
            :hide-upload-button="true"
          />
        </template>
      </template>
      <a-empty v-else-if="!loading" description="未找到账单" />
    </a-spin>

    <div v-if="bill" class="drawer-footer">
      <span class="times">
        确认：{{ bill.confirmedTime || '-' }}　付款：{{ bill.paidTime || '-' }}
      </span>
      <a-space>
        <a-button
          v-if="bill.status === 'DRAFT' || bill.status === 'DISPUTED'"
          type="primary"
          :loading="acting"
          @click="transition('CONFIRMED')"
        >
          确认账单
        </a-button>
        <template v-if="bill.status === 'CONFIRMED'">
          <a-button type="primary" :loading="acting" @click="openPayment">
            标记已付款
          </a-button>
          <a-button danger :loading="acting" @click="disputeVisible = true">
            标记争议
          </a-button>
        </template>
      </a-space>
    </div>

    <a-modal
      v-model:open="disputeVisible"
      title="标记账单争议"
      :confirm-loading="acting"
      @ok="submitDispute"
    >
      <a-textarea
        v-model:value="disputeRemark"
        :rows="3"
        placeholder="填写金额、收费项目或业务凭证方面的争议原因"
      />
    </a-modal>

    <a-modal
      v-model:open="paymentVisible"
      title="确认账单付款"
      :confirm-loading="acting"
      :mask-closable="false"
      ok-text="确认已付款"
      @ok="submitPayment"
      @cancel="paymentVoucherFileId = undefined"
    >
      <a-alert
        type="info"
        show-icon
        message="上传付款凭证后，账单才会标记为已付款。"
        class="payment-alert"
      />
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="应付金额">
          <b>{{ formatMoney(bill?.totalAmount) }}</b>
        </a-form-item>
        <a-form-item label="付款凭证" required>
          <sys-file-upload
            v-model="paymentVoucherFileId"
            bucket-key="private-files"
            button-text="上传付款凭证"
            :allowed-types="['application/pdf', 'image/jpeg', 'image/png']"
          />
          <div class="upload-tip">支持 PDF、JPG、PNG 格式，文件最大 10MB</div>
        </a-form-item>
      </a-form>
    </a-modal>
  </a-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import SysFileUpload from '@/components/Upload/SysFileUpload.vue'
import { changeBillStatus, getMonthlyBillDetail } from '@/api/platform-finance/receivable'
import type {
  BillStatus,
  MonthlyBillVO
} from '@/api/platform-finance/receivable/types'
import {
  BILL_STATUS_COLOR,
  BILL_STATUS_TEXT,
  formatMoney,
  operationSubtotal
} from '../constants'

const props = defineProps<{ open: boolean; billId?: number }>()
const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'changed'): void
}>()

const loading = ref(false)
const acting = ref(false)
const bill = ref<MonthlyBillVO>()
const disputeVisible = ref(false)
const disputeRemark = ref('')
const paymentVisible = ref(false)
const paymentVoucherFileId = ref<number>()

const summaryItems = computed(() => {
  if (!bill.value) return []
  return [
    { label: '货架租金', value: bill.value.rackFee },
    { label: '入库操作', value: bill.value.inboundFee },
    { label: '出库操作', value: bill.value.outboundFee },
    { label: '配送费用', value: bill.value.deliveryFee },
    { label: '退货取件', value: bill.value.returnFee },
    { label: '验货费用', value: bill.value.inspectionFee },
    { label: '其他服务', value: bill.value.driverFee },
    { label: '操作费小计', value: operationSubtotal(bill.value) }
  ]
})

const unitText: Record<string, string> = {
  CBM: '立方米',
  PALLET: '托',
  BOX: '箱',
  ITEM: '件',
  RATE: '比例',
  ACTUAL: '笔'
}
const recordColumns = [
  { title: '收费项目', dataIndex: 'feeName', width: 150, fixed: 'left' },
  { title: '货主', dataIndex: 'ownerName', width: 120, customRender: ({ value }: any) => value || '-' },
  { title: '业务凭证', dataIndex: 'sourceRef', width: 160 },
  {
    title: '数量',
    width: 100,
    align: 'right',
    customRender: ({ record }: any) =>
      `${Number(record.billingQuantity || 0)} ${unitText[record.billingUnit] || record.billingUnit || ''}`
  },
  {
    title: '单价',
    dataIndex: 'unitPrice',
    width: 100,
    align: 'right',
    customRender: ({ value }: any) => formatMoney(value)
  },
  {
    title: '金额',
    dataIndex: 'amount',
    width: 110,
    align: 'right',
    customRender: ({ value }: any) => formatMoney(value)
  },
  { title: '计费说明', dataIndex: 'remark', width: 220 },
  { title: '入账时间', dataIndex: 'createTime', width: 165 }
]

watch(
  () => [props.open, props.billId] as const,
  ([visible, id]) => {
    if (visible && id) load(id)
  }
)

async function load(id: number) {
  loading.value = true
  bill.value = undefined
  try {
    const response = await getMonthlyBillDetail(id)
    if (isSuccess(response)) bill.value = response.data
  } finally {
    loading.value = false
  }
}

async function transition(status: BillStatus, remark?: string, voucherFileId?: number) {
  if (!bill.value) return
  acting.value = true
  try {
    const response = await changeBillStatus(bill.value.id, status, remark, voucherFileId)
    if (isSuccess(response)) {
      await load(bill.value.id)
      message.success('账单状态已更新')
      emit('changed')
    }
  } finally {
    acting.value = false
  }
}

function openPayment() {
  paymentVoucherFileId.value = undefined
  paymentVisible.value = true
}

async function submitPayment() {
  if (!paymentVoucherFileId.value) {
    message.warning('请先上传付款凭证')
    return
  }
  await transition('PAID', undefined, paymentVoucherFileId.value)
  if (bill.value?.status === 'PAID') {
    paymentVisible.value = false
    paymentVoucherFileId.value = undefined
  }
}

async function submitDispute() {
  if (!disputeRemark.value.trim()) {
    message.warning('请填写争议原因')
    return
  }
  await transition('DISPUTED', disputeRemark.value.trim())
  disputeVisible.value = false
  disputeRemark.value = ''
}
</script>

<style scoped>
.bill-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.bill-month {
  font-size: 17px;
  font-weight: 600;
}
.bill-operator,
.section-note,
.record-count,
.times {
  color: #8c8c8c;
  font-size: 12px;
}
.bill-operator {
  margin-top: 4px;
}
.head-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.total-amount {
  color: #1677ff;
  font-size: 26px;
  font-weight: 700;
}
.block {
  margin-top: 16px;
}
.section-title {
  margin: 20px 0 10px;
  font-size: 14px;
  font-weight: 600;
}
.section-title.no-margin {
  margin: 0;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border-top: 1px solid #f0f0f0;
  border-left: 1px solid #f0f0f0;
}
.summary-item {
  min-width: 0;
  padding: 10px 12px;
  border-right: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}
.summary-item span {
  display: block;
  color: #8c8c8c;
  font-size: 12px;
}
.summary-item b {
  display: block;
  margin-top: 4px;
  font-weight: 600;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin: 22px 0 10px;
}
.drawer-footer {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
.payment-alert {
  margin-bottom: 18px;
}
.upload-tip {
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 12px;
}
</style>
