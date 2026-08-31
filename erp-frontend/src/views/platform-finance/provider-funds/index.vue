<template>
  <div class="provider-settlement-page">
    <div class="view-switcher">
      <button
        type="button"
        class="view-card"
        :class="{ active: activeView === 'account' }"
        @click="activeView = 'account'"
      >
        <span class="view-icon"><WalletCards :size="22" /></span>
        <span class="view-copy">
          <strong>服务商资金账户</strong>
          <small>查看充值、实时服务费、余额和资金流水</small>
        </span>
        <span class="view-stat"><b>{{ accounts.length }}</b><small>个账户</small></span>
      </button>
      <button
        type="button"
        class="view-card"
        :class="{ active: activeView === 'bill' }"
        @click="activeView = 'bill'"
      >
        <span class="view-icon"><FileCheck2 :size="22" /></span>
        <span class="view-copy">
          <strong>月度服务对账单</strong>
          <small>按月汇总服务费，调整差异并完成复核</small>
        </span>
        <span class="view-stat"><b>{{ billTotal }}</b><small>笔账单</small></span>
      </button>
    </div>

    <a-card :bordered="false" class="filters">
      <a-form layout="inline" class="filter-form">
        <a-form-item label="WMS服务商"><a-select v-model:value="query.wmsTenantId" allow-clear show-search option-filter-prop="label" :options="operators" style="width:180px" /></a-form-item>
        <template v-if="viewPolicy.account">
          <a-form-item label="币种"><a-select v-model:value="query.currency" allow-clear :options="currencies" style="width:110px" /></a-form-item>
          <a-form-item label="余额状态"><a-select v-model:value="query.negative" allow-clear :options="balanceOptions" style="width:130px" /></a-form-item>
        </template>
        <template v-if="viewPolicy.bill">
          <a-form-item label="账期"><a-range-picker v-model:value="monthRange" picker="month" :placeholder="['开始账期','结束账期']" style="width:220px" /></a-form-item>
          <a-form-item label="账单状态"><a-select v-model:value="billStatuses" mode="multiple" allow-clear :max-tag-count="1" :options="BILL_STATUS_OPTIONS" style="width:170px" /></a-form-item>
        </template>
        <a-form-item class="filter-actions"><a-button type="primary" :loading="loading || billLoading" @click="search">查询</a-button><a-button @click="reset">重置</a-button></a-form-item>
      </a-form>
    </a-card>

    <a-card v-if="viewPolicy.account" :bordered="false" title="服务商资金账户" class="section-card">
      <template #extra><span class="section-note">账面余额 = 已审核充值 - 已入账服务费；不同币种独立核算</span></template>
      <a-table :loading="loading" :data-source="accounts" :columns="accountColumns" row-key="rowKey" :scroll="{x:1050}" size="small" :pagination="false">
        <template #bodyCell="{column,record}"><template v-if="column.key==='recharge'">{{ formatCurrency(record.rechargeAmount, record.currency) }}</template><template v-else-if="column.key==='charge'">{{ formatCurrency(record.chargeAmount, record.currency) }}</template><template v-else-if="column.key==='balance'"><span :class="{danger:record.negative}">{{ formatCurrency(record.balance, record.currency) }}</span></template><template v-else-if="column.key==='pending'"><a-tag v-if="record.pendingCount" color="orange">{{ record.pendingCount }} 笔</a-tag><span v-else>0</span></template><template v-else-if="column.key==='lastChange'">{{ formatAccountTime(record.lastChangeTime) }}</template><template v-else-if="column.key==='operate'"><a @click="openDetail(record)">查看账户</a></template></template>
      </a-table>
    </a-card>

    <a-card v-if="viewPolicy.bill" :bordered="false" title="月度服务对账单" class="section-card">
      <template #extra><span class="section-note">月底自动生成，逐条复核，不重复扣减余额</span></template>
      <a-table
        :loading="billLoading"
        :data-source="bills"
        :columns="billColumns"
        row-key="id"
        size="small"
        :scroll="{x:1460}"
        :pagination="billPagination"
        @change="handleBillTableChange"
      >
        <template #bodyCell="{column,record}">
          <template v-if="column.key==='money'">{{ money(record[column.dataIndex]) }}</template>
          <template v-else-if="column.key==='total'"><strong>{{ money(record.totalAmount) }} {{ record.currency }}</strong></template>
          <template v-else-if="column.key==='status'"><a-tag :color="BILL_STATUS_COLOR[record.status]">{{ BILL_STATUS_TEXT[record.status] }}</a-tag></template>
          <template v-else-if="column.key==='reviewer'"><template v-if="record.reviewerName">{{ record.reviewerName }}<br><span class="section-note">{{ record.confirmedTime }}</span></template><span v-else class="section-note">尚未复核</span></template>
          <template v-else-if="column.key==='operate'"><a-space><a @click="viewBill(record)">查看</a><a v-if="billOperations(record.status).adjust" @click="adjustBill(record)">调整费用</a><a v-if="billOperations(record.status).review" @click="viewBill(record)">复核</a></a-space></template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="drawerOpen" :width="1040" :title="`服务商资金 · ${current?.wmsTenantName||''}`">
      <div v-if="current" class="account-strip"><div><span>币种</span><b>{{ current.currency }}</b></div><div><span>已审核充值</span><b>{{ formatCurrency(current.rechargeAmount, current.currency) }}</b></div><div><span>已入账服务费</span><b>{{ formatCurrency(current.chargeAmount, current.currency) }}</b></div><div><span>账面余额</span><b :class="{danger:current.negative}">{{ formatCurrency(current.balance, current.currency) }}</b></div></div>
      <a-tabs>
        <a-tab-pane key="recharges" tab="充值审核"><a-table :data-source="recharges" :columns="rechargeColumns" row-key="id" size="small" :scroll="{x:1050}"><template #bodyCell="{column,record}"><template v-if="column.key==='amount'">{{ money(record.amount) }} {{ record.currency }}</template><template v-else-if="column.key==='status'"><a-tag :color="record.status==='APPROVED'?'green':record.status==='PENDING'?'orange':record.status==='REJECTED'?'red':'default'">{{ rechargeStatusText(record.status) }}</a-tag></template><template v-else-if="column.key==='voucher'"><a v-if="record.voucherUrl" :href="record.voucherUrl" target="_blank">查看凭证</a></template><template v-else-if="column.key==='operate'"><a-space><a-popconfirm v-if="record.status==='PENDING'" title="确认通过？" @confirm="approve(record.id)"><a>通过</a></a-popconfirm><a v-if="record.status==='PENDING'" @click="reasonAction('reject',record.id)">驳回</a><a v-if="record.status==='APPROVED'" @click="reasonAction('reverse',record.id)">冲正</a></a-space></template></template></a-table></a-tab-pane>
        <a-tab-pane key="ledger" tab="资金流水">
          <div class="ledger-toolbar"><span class="section-note">按月汇总；展开月份查看单笔流水</span><a-button @click="exportOpen=true"><template #icon><Download :size="16" /></template>导出流水</a-button></div>
          <a-table :loading="ledgerLoading" :data-source="ledgerMonths" :columns="ledgerMonthColumns" row-key="month" size="small" :scroll="{x:920}" :pagination="false">
            <template #bodyCell="{column,record}"><template v-if="column.key==='money'">{{ money(record[column.dataIndex]) }} {{ record.currency }}</template><template v-else-if="column.key==='net'"><span :class="record.netChange<0?'danger':'income'">{{ record.netChange>0?'+':'' }}{{ money(record.netChange) }} {{ record.currency }}</span></template><template v-else-if="column.key==='statement'"><a-tag :color="ledgerStatementMeta(record.statementStatus).color">{{ ledgerStatementMeta(record.statementStatus).text }}</a-tag></template></template>
            <template #expandedRowRender="{record}"><a-table :data-source="record.details" :columns="ledgerDetailColumns" :row-key="(row:any)=>`${row.businessNo}-${row.occurredTime}`" size="small" :pagination="false"><template #bodyCell="{column,record:detail}"><template v-if="column.key==='time'">{{ formatAccountTime(detail.occurredTime) }}</template><template v-else-if="column.key==='amount'"><span :class="detail.amount<0?'danger':'income'">{{ detail.amount>0?'+':'' }}{{ money(detail.amount) }} {{ detail.currency }}</span></template></template></a-table></template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-drawer>
    <a-modal v-model:open="exportOpen" title="导出资金流水" :confirm-loading="exporting" @ok="submitExport"><a-form layout="vertical"><a-form-item label="资金账户"><a-input :value="`${current?.wmsTenantName||'-'} / ${current?.currency||'-'}`" disabled /></a-form-item><a-form-item label="导出日期范围" required><a-range-picker v-model:value="exportRange" style="width:100%" /></a-form-item></a-form><a-alert type="info" show-icon message="Excel 包含“月度汇总”和“流水明细”两个工作表。" /></a-modal>
    <a-modal v-model:open="reasonOpen" :title="mode==='reject'?'驳回充值':'冲正充值'" @ok="submitReason"><a-textarea v-model:value="reason" :rows="4" placeholder="必须填写原因" /></a-modal>
    <ReceivableBillDrawer v-model:open="billDrawerOpen" :bill-id="currentBill?.id" @changed="refreshBills" />
    <BillAdjustmentModal v-model:open="adjustmentOpen" :bill="currentBill" @saved="refreshBills" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { Download, FileCheck2, WalletCards } from 'lucide-vue-next'
import { isSuccess } from '@/api'
import { listWmsOperators } from '@/api/tenant'
import { exportPlatformLedger,getPlatformFundAccounts,getPlatformLedgerMonths,getPlatformRecharges,reviewPlatformRecharge,reversePlatformRecharge } from '@/api/fund-settlement'
import type { FundAccount,FundLedgerMonth,RechargeOrder,RechargeStatus } from '@/api/fund-settlement/types'
import { pageMonthlyBill } from '@/api/platform-finance/receivable'
import type { BillStatus, MonthlyBillVO } from '@/api/platform-finance/receivable/types'
import { BILL_STATUS_COLOR, BILL_STATUS_OPTIONS, BILL_STATUS_TEXT } from '../receivable-bill/constants'
import ReceivableBillDrawer from '../receivable-bill/components/ReceivableBillDrawer.vue'
import BillAdjustmentModal from '../receivable-bill/components/BillAdjustmentModal.vue'
import { billOperations } from './bill-operation-policy'
import { formatAccountTime } from './account-display'
import { formatCurrency } from '@/views/wms/finance-income/currency'
import { settlementViewPolicy, type SettlementView } from './settlement-view'
import { ledgerStatementMeta } from './ledger-display'
import { remoteFileDownload } from '@/utils/file-utils'

const activeView = ref<SettlementView>('account')
const viewPolicy = computed(() => settlementViewPolicy(activeView.value))
const query=reactive<{wmsTenantId?:number;currency?:string;negative?:boolean}>({}),loading=ref(false),accounts=ref<(FundAccount&{rowKey:string})[]>([]),operators=ref<{label:string;value:number}[]>([])
const currencies=['RUB','CNY','USD','EUR'].map(value=>({label:value,value})),balanceOptions=[{label:'余额正常',value:false},{label:'余额为负',value:true}]
const monthRange=ref<[Dayjs,Dayjs]|null>(null),billStatuses=ref<BillStatus[]>([]),billLoading=ref(false),bills=ref<MonthlyBillVO[]>([]),billPage=ref(1),billSize=ref(10),billTotal=ref(0)
const accountColumns=[{title:'WMS服务商',dataIndex:'wmsTenantName',width:180,fixed:'left' as const},{title:'币种',dataIndex:'currency',width:80},{title:'已审核充值',key:'recharge',width:140,align:'right' as const},{title:'已入账服务费',key:'charge',width:150,align:'right' as const},{title:'账面余额',key:'balance',width:150,align:'right' as const},{title:'待审核充值',key:'pending',width:110},{title:'最近账务时间',key:'lastChange',width:170},{title:'操作',key:'operate',width:100,fixed:'right' as const}]
const billColumns=[{title:'账期',dataIndex:'billMonth',width:90,fixed:'left' as const},{title:'WMS服务商',dataIndex:'wmsTenantName',width:150},{title:'入库费',dataIndex:'inboundFee',key:'money',width:105,align:'right' as const},{title:'出库费',dataIndex:'outboundFee',key:'money',width:105,align:'right' as const},{title:'配送费',dataIndex:'deliveryFee',key:'money',width:105,align:'right' as const},{title:'退货费',dataIndex:'returnFee',key:'money',width:105,align:'right' as const},{title:'验货费',dataIndex:'inspectionFee',key:'money',width:105,align:'right' as const},{title:'其他',dataIndex:'driverFee',key:'money',width:105,align:'right' as const},{title:'合计',key:'total',width:150,align:'right' as const},{title:'状态',key:'status',width:90},{title:'复核信息',key:'reviewer',width:180},{title:'操作',key:'operate',width:190,fixed:'right' as const}]
const rechargeColumns=[{title:'充值单号',dataIndex:'rechargeNo',width:180,fixed:'left' as const},{title:'金额',key:'amount',width:130},{title:'付款时间',dataIndex:'paymentTime',width:170},{title:'提交人',dataIndex:'applicantName',width:120},{title:'凭证',key:'voucher',width:90},{title:'状态',key:'status',width:90},{title:'备注',dataIndex:'remark',width:160},{title:'操作',key:'operate',width:150,fixed:'right' as const}]
const ledgerMonthColumns=[{title:'月份',dataIndex:'month',width:100,fixed:'left' as const},{title:'充值合计',dataIndex:'rechargeAmount',key:'money',width:135,align:'right' as const},{title:'服务费合计',dataIndex:'chargeAmount',key:'money',width:135,align:'right' as const},{title:'冲正金额',dataIndex:'reversalAmount',key:'money',width:130,align:'right' as const},{title:'净变动',key:'net',width:150,align:'right' as const},{title:'期初余额',dataIndex:'openingBalance',key:'money',width:140,align:'right' as const},{title:'期末余额',dataIndex:'closingBalance',key:'money',width:140,align:'right' as const},{title:'状态',key:'statement',width:125}]
const ledgerDetailColumns=[{title:'时间',key:'time',width:155},{title:'类型',dataIndex:'entryTypeLabel',width:100},{title:'业务单据',dataIndex:'businessLabel',width:170},{title:'费用项目',dataIndex:'descriptionLabel'},{title:'操作人',dataIndex:'operatorName',width:110},{title:'金额',key:'amount',width:145,align:'right' as const}]
const billPagination=computed(()=>({current:billPage.value,pageSize:billSize.value,total:billTotal.value,showSizeChanger:true,pageSizeOptions:['10','20','50'],showTotal:(total:number)=>`共 ${total} 条`}))
const current=ref<FundAccount>(),drawerOpen=ref(false),recharges=ref<RechargeOrder[]>([]),ledgerMonths=ref<FundLedgerMonth[]>([]),ledgerLoading=ref(false),money=(v:number)=>Number(v||0).toLocaleString('zh-CN',{minimumFractionDigits:2,maximumFractionDigits:2})
const exportOpen=ref(false),exporting=ref(false),exportRange=ref<[Dayjs,Dayjs]>([dayjs().startOf('month'),dayjs()])
const currentBill=ref<MonthlyBillVO>(),billDrawerOpen=ref(false),adjustmentOpen=ref(false)

async function loadAccounts(){loading.value=true;try{const res=await getPlatformFundAccounts(query);if(isSuccess(res))accounts.value=(res.data||[]).map(x=>({...x,rowKey:`${x.wmsTenantId}-${x.currency}`}))}finally{loading.value=false}}
async function loadBills(){billLoading.value=true;try{const res=await pageMonthlyBill({page:billPage.value,size:billSize.value},{billMonthStart:monthRange.value?.[0]?.format('YYYY-MM'),billMonthEnd:monthRange.value?.[1]?.format('YYYY-MM'),wmsTenantId:query.wmsTenantId,statuses:billStatuses.value.length?billStatuses.value:undefined});if(isSuccess(res)){bills.value=res.data?.records||[];billTotal.value=res.data?.total||0}}finally{billLoading.value=false}}
function search(){billPage.value=1;viewPolicy.value.account?loadAccounts():loadBills()}
function reset(){Object.assign(query,{wmsTenantId:undefined,currency:undefined,negative:undefined});monthRange.value=null;billStatuses.value=[];billPage.value=1;search()}
function handleBillTableChange(pagination:any){billPage.value=pagination.current||1;billSize.value=pagination.pageSize||10;loadBills()}
async function refreshBills(){await loadBills();if(currentBill.value){const fresh=bills.value.find(x=>x.id===currentBill.value?.id);if(fresh)currentBill.value=fresh}await loadAccounts()}
function viewBill(row:MonthlyBillVO){currentBill.value=row;billDrawerOpen.value=true}
function adjustBill(row:MonthlyBillVO){currentBill.value=row;adjustmentOpen.value=true}
async function openDetail(row:FundAccount){current.value=row;drawerOpen.value=true;ledgerLoading.value=true;try{const [r,l]=await Promise.all([getPlatformRecharges({wmsTenantId:row.wmsTenantId}),getPlatformLedgerMonths(row.wmsTenantId,{currency:row.currency})]);if(isSuccess(r))recharges.value=r.data||[];if(isSuccess(l))ledgerMonths.value=l.data||[]}finally{ledgerLoading.value=false}}
async function submitExport(){if(!current.value||!exportRange.value){message.warning('请选择导出日期范围');return}exporting.value=true;try{const response=await exportPlatformLedger({wmsTenantId:current.value.wmsTenantId,currency:current.value.currency,startDate:exportRange.value[0].format('YYYY-MM-DD'),endDate:exportRange.value[1].format('YYYY-MM-DD')});remoteFileDownload(response);exportOpen.value=false;message.success('资金流水已导出')}finally{exporting.value=false}}
async function approve(id:number){const res=await reviewPlatformRecharge(id,true);if(isSuccess(res)){message.success('审核通过');await openDetail(current.value!);loadAccounts()}}
const reasonOpen=ref(false),reason=ref(''),mode=ref<'reject'|'reverse'>('reject'),actionId=ref<number>()
function reasonAction(m:'reject'|'reverse',id:number){mode.value=m;actionId.value=id;reason.value='';reasonOpen.value=true}
async function submitReason(){if(!reason.value.trim()){message.warning('请填写原因');return}const res=mode.value==='reject'?await reviewPlatformRecharge(actionId.value!,false,reason.value):await reversePlatformRecharge(actionId.value!,reason.value);if(isSuccess(res)){message.success('操作成功');reasonOpen.value=false;await openDetail(current.value!);loadAccounts()}}
function rechargeStatusText(status:RechargeStatus){return ({PENDING:'待审核',APPROVED:'已通过',REJECTED:'已驳回',CANCELLED:'已取消',REVERSED:'已冲正'} as Record<RechargeStatus,string>)[status]}
onMounted(async()=>{const r=await listWmsOperators();if(isSuccess(r))operators.value=(r.data||[]).map(x=>({label:x.tenantName,value:x.id}));await Promise.all([loadAccounts(),loadBills()])})
</script>

<style scoped>
.provider-settlement-page{min-height:100%}.view-switcher{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px;margin-bottom:16px}.view-card{display:grid;grid-template-columns:44px minmax(0,1fr) auto;align-items:center;gap:14px;min-height:92px;padding:16px 18px;border:1px solid #d9d9d9;border-radius:6px;background:#fff;color:#262626;text-align:left;cursor:pointer;transition:border-color .15s,background-color .15s,box-shadow .15s}.view-card:hover{border-color:#91caff}.view-card.active{border-color:#1677ff;background:#f5f9ff;box-shadow:0 0 0 1px #1677ff}.view-icon{display:grid;place-items:center;width:44px;height:44px;border-radius:6px;background:#f0f2f5;color:#5b6678}.view-card.active .view-icon{background:#1677ff;color:#fff}.view-copy{min-width:0}.view-copy strong,.view-copy small,.view-stat b,.view-stat small{display:block}.view-copy strong{font-size:16px}.view-copy small,.view-stat small{margin-top:6px;color:#8c8c8c;font-size:12px}.view-stat{text-align:right}.view-stat b{font-size:22px}.filters,.section-card{margin-bottom:16px}.filter-form{display:flex;flex-wrap:wrap;align-items:center;gap:14px 18px}.filter-form :deep(.ant-form-item){margin:0}.filter-actions :deep(.ant-form-item-control-input-content){display:flex;gap:8px}.section-note{color:#8c8c8c;font-size:12px}.danger{color:#cf1322;font-weight:600}.income{color:#389e0d}.account-strip{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));margin-bottom:18px;border:1px solid #f0f0f0}.account-strip>div{padding:14px 16px;border-right:1px solid #f0f0f0}.account-strip>div:last-child{border-right:0}.account-strip span{display:block;color:#8c8c8c;font-size:12px}.account-strip b{display:block;margin-top:6px;font-size:18px}.ledger-toolbar{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}@media(max-width:1100px){.view-switcher{grid-template-columns:1fr}.view-card{min-height:82px}}
</style>
