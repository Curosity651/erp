<template>
  <a-drawer :open="open" title="月度服务对账单详情" width="920" :body-style="{ paddingBottom: '72px' }" @close="emit('update:open', false)">
    <a-spin :spinning="loading">
      <template v-if="bill">
        <div class="bill-head">
          <div><div class="bill-month">{{ bill.billMonth }} 账期</div><div class="bill-operator">{{ bill.wmsTenantName }}</div></div>
          <div class="head-right"><div class="total-amount">{{ formatMoney(bill.totalAmount) }}</div><a-tag :color="BILL_STATUS_COLOR[bill.status]">{{ BILL_STATUS_TEXT[bill.status] }}</a-tag></div>
        </div>
        <a-alert type="info" show-icon message="本账单只汇总已经正式入账的服务费，查看或复核不会再次扣减服务商资金余额。" class="block" />
        <a-alert v-if="bill.status === 'DISPUTED' && bill.remark" type="error" show-icon :message="`异议说明：${bill.remark}`" class="block" />

        <div class="section-title">费用汇总</div>
        <div class="summary-grid"><div v-for="item in summaryItems" :key="item.label" class="summary-item"><span>{{ item.label }}</span><b>{{ formatMoney(item.value) }}</b></div></div>
        <div class="amount-strip"><span>原始费用：<b>{{ formatMoney(originalTotal) }}</b></span><span>调整净额：<b :class="adjustmentTotal < 0 ? 'deduction' : 'supplement'">{{ signedMoney(adjustmentTotal) }}</b></span><span>最终应收：<strong>{{ formatMoney(bill.totalAmount) }}</strong></span></div>

        <div class="section-head"><div><div class="section-title no-margin">原始费用明细</div><div class="section-note">系统根据仓库业务动作自动生成的收费记录</div></div><span class="record-count">共 {{ originalRecords.length }} 笔</span></div>
        <a-table row-key="id" size="small" :columns="recordColumns" :data-source="originalRecords" :pagination="false" :scroll="{ x: 1080 }" />

        <div class="section-head"><div><div class="section-title no-margin">账单调整明细</div><div class="section-note">保留补收和冲减记录，不覆盖原始费用</div></div><span class="record-count">共 {{ adjustmentRecords.length }} 笔</span></div>
        <a-table row-key="id" size="small" :columns="adjustmentColumns" :data-source="adjustmentRecords" :pagination="false" :scroll="{ x: 900 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'type'"><a-tag :color="Number(record.amount) < 0 ? 'red' : 'blue'">{{ Number(record.amount) < 0 ? '冲减' : '补收' }}</a-tag></template>
            <template v-else-if="column.key === 'amount'"><span :class="Number(record.amount) < 0 ? 'deduction' : 'supplement'">{{ signedMoney(Number(record.amount)) }}</span></template>
          </template>
        </a-table>
      </template>
      <a-empty v-else-if="!loading" description="未找到账单" />
    </a-spin>
    <div v-if="bill" class="drawer-footer"><span class="times">复核人：{{ bill.reviewerName || '-' }} 复核时间：{{ bill.confirmedTime || '-' }}</span><a-button v-if="bill.status === 'DRAFT' || bill.status === 'DISPUTED'" type="primary" :loading="acting" @click="review">确认复核</a-button></div>
  </a-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { changeBillStatus, getMonthlyBillDetail } from '@/api/platform-finance/receivable'
import type { MonthlyBillVO } from '@/api/platform-finance/receivable/types'
import { BILL_STATUS_COLOR, BILL_STATUS_TEXT, formatMoney } from '../constants'

const props = defineProps<{ open: boolean; billId?: number }>()
const emit = defineEmits<{ (e: 'update:open', value: boolean): void; (e: 'changed'): void }>()
const loading = ref(false), acting = ref(false), bill = ref<MonthlyBillVO>()
const adjustmentRecords = computed(() => (bill.value?.billingRecords || []).filter(row => row.sourceType === 'BILL_ADJUSTMENT'))
const originalRecords = computed(() => (bill.value?.billingRecords || []).filter(row => row.sourceType !== 'BILL_ADJUSTMENT'))
const adjustmentTotal = computed(() => adjustmentRecords.value.reduce((sum, row) => sum + Number(row.amount || 0), 0))
const originalTotal = computed(() => Number(bill.value?.totalAmount || 0) - adjustmentTotal.value)
const summaryItems = computed(() => bill.value ? [
  { label: '入库费', value: bill.value.inboundFee }, { label: '出库费', value: bill.value.outboundFee },
  { label: '配送费', value: bill.value.deliveryFee }, { label: '退货费', value: bill.value.returnFee },
  { label: '验货费', value: bill.value.inspectionFee }, { label: '其他服务', value: bill.value.driverFee }
] : [])
const unitText: Record<string, string> = { CBM: '立方米', PALLET: '托', BOX: '箱', ITEM: '件', RATE: '比例', ACTUAL: '笔' }
const recordColumns = [
  { title: '收费项目', dataIndex: 'feeName', width: 150, fixed: 'left' as const }, { title: '货主', dataIndex: 'ownerName', width: 110, customRender: ({ value }: any) => value || '-' },
  { title: '业务凭证', dataIndex: 'sourceRef', width: 170 }, { title: '数量', width: 105, align: 'right' as const, customRender: ({ record }: any) => `${Number(record.billingQuantity || 0)} ${unitText[record.billingUnit] || record.billingUnit || ''}` },
  { title: '单价', dataIndex: 'unitPrice', width: 105, align: 'right' as const, customRender: ({ value }: any) => formatMoney(value) }, { title: '金额', dataIndex: 'amount', width: 115, align: 'right' as const, customRender: ({ value }: any) => formatMoney(value) },
  { title: '计费说明', dataIndex: 'remark', width: 220 }, { title: '入账时间', dataIndex: 'createTime', width: 165 }
]
const adjustmentColumns = [
  { title: '类型', key: 'type', width: 80, fixed: 'left' as const }, { title: '收费项目', dataIndex: 'feeName', width: 150 }, { title: '业务凭证', dataIndex: 'sourceRef', width: 170 },
  { title: '调整原因', dataIndex: 'remark', width: 230 }, { title: '操作人', dataIndex: 'operatorName', width: 120 }, { title: '调整时间', dataIndex: 'createTime', width: 165 },
  { title: '金额', key: 'amount', width: 120, align: 'right' as const, fixed: 'right' as const }
]

watch(() => [props.open, props.billId] as const, ([visible, id]) => { if (visible && id) load(id) })
async function load(id: number) { loading.value = true; bill.value = undefined; try { const response = await getMonthlyBillDetail(id); if (isSuccess(response)) bill.value = response.data } finally { loading.value = false } }
function signedMoney(value: number) { return value ? `${value > 0 ? '+' : '-'}${formatMoney(Math.abs(value))}` : formatMoney(0) }
function review() {
  if (!bill.value) return
  Modal.confirm({ title: '确认复核这张月度服务对账单？', content: '复核后账单将锁定，不能继续调整；本操作不会再次扣减服务商资金余额。', okText: '确认复核', cancelText: '取消', async onOk() {
    if (!bill.value) return; acting.value = true
    try { const response = await changeBillStatus(bill.value.id, 'CONFIRMED'); if (isSuccess(response)) { await load(bill.value.id); message.success('账单已复核'); emit('changed') } } finally { acting.value = false }
  } })
}
</script>

<style scoped>
.bill-head,.head-right,.section-head,.amount-strip,.drawer-footer{display:flex;align-items:center}.bill-head{justify-content:space-between;padding-bottom:16px;border-bottom:1px solid #f0f0f0}.bill-month{font-size:17px;font-weight:600}.bill-operator,.section-note,.record-count,.times{color:#8c8c8c;font-size:12px}.bill-operator{margin-top:4px}.head-right{gap:12px}.total-amount{color:#1677ff;font-size:26px;font-weight:700}.block{margin-top:16px}.section-title{margin:20px 0 10px;font-size:14px;font-weight:600}.section-title.no-margin{margin:0}.summary-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));border-top:1px solid #f0f0f0;border-left:1px solid #f0f0f0}.summary-item{padding:10px 12px;border-right:1px solid #f0f0f0;border-bottom:1px solid #f0f0f0}.summary-item span{display:block;color:#8c8c8c;font-size:12px}.summary-item b{display:block;margin-top:4px}.amount-strip{justify-content:flex-end;gap:28px;margin-top:14px;padding:12px 14px;background:#fafafa}.amount-strip strong{font-size:17px}.section-head{justify-content:space-between;align-items:flex-end;margin:22px 0 10px}.drawer-footer{position:absolute;right:0;bottom:0;left:0;justify-content:space-between;padding:12px 24px;background:#fff;border-top:1px solid #f0f0f0}.supplement{color:#1677ff}.deduction{color:#cf1322}
</style>
