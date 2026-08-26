<template>
  <div class="fund-panel">
    <div class="panel-actions"><a-button type="primary" @click="rechargeOpen=true">登记充值</a-button><a-button :loading="loading" @click="load">刷新</a-button></div>
    <div v-if="accounts.some(a=>a.negative)" class="negative-notice">账户存在负余额，仅作财务预警，不影响订单和仓库作业。</div>
    <div class="fund-summary-strip">
      <div v-for="a in accounts" :key="a.currency" class="fund-summary-item" :class="{ negative:a.negative }">
        <div class="fund-label">{{ a.wmsTenantName || 'WMS服务商' }} · {{ a.currency }}</div>
        <div class="fund-balance">{{ money(a.balance) }} <small>{{ a.currency }}</small></div>
        <div class="fund-detail">充值 {{ money(a.rechargeAmount) }} · 费用 {{ money(a.chargeAmount) }}</div>
      </div>
      <div v-if="!accounts.length" class="fund-summary-item empty-account">
        <div class="fund-label">WMS资金账户</div>
        <div class="fund-balance">暂无余额</div>
        <div class="fund-detail">登记充值后显示账户信息</div>
      </div>
    </div>
    <a-tabs class="account-detail-tabs">
      <a-tab-pane key="recharge" tab="充值记录">
        <a-table :data-source="recharges" :columns="rechargeColumns" row-key="id" size="small" :scroll="{x:1000}" :pagination="{pageSize:10}">
          <template #bodyCell="{column,record}">
            <template v-if="column.key==='amount'">{{ money(record.amount) }} {{ record.currency }}</template>
            <template v-else-if="column.key==='status'"><a-tag :color="statusColor[record.status]">{{ statusText[record.status] }}</a-tag></template>
            <template v-else-if="column.key==='voucher'"><a v-if="record.voucherUrl" :href="record.voucherUrl" target="_blank">查看凭证</a><span v-else>--</span></template>
            <template v-else-if="column.key==='operate'"><a-popconfirm v-if="record.status==='PENDING'" title="确认取消这条充值申请？" @confirm="cancel(record.id)"><a>取消</a></a-popconfirm></template>
          </template>
        </a-table>
      </a-tab-pane>
      <a-tab-pane key="ledger" tab="资金流水">
        <a-table :data-source="ledger" :columns="ledgerColumns" row-key="businessNo" size="small" :pagination="{pageSize:10}">
          <template #bodyCell="{column,record}"><template v-if="column.key==='amount'"><span :class="record.amount<0?'out':'in'">{{ record.amount>0?'+':'' }}{{ money(record.amount) }} {{ record.currency }}</span></template></template>
        </a-table>
      </a-tab-pane>
    </a-tabs>
    <RechargeCreateModal v-model:open="rechargeOpen" target="owner" @saved="load" />
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { cancelOwnerRecharge, getOwnerFundAccounts, getOwnerLedger, getOwnerRecharges } from '@/api/fund-settlement'
import type { FundAccount, FundLedger, RechargeOrder, RechargeStatus } from '@/api/fund-settlement/types'
import RechargeCreateModal from './RechargeCreateModal.vue'
const accounts=ref<FundAccount[]>([]), recharges=ref<RechargeOrder[]>([]), ledger=ref<FundLedger[]>([]), loading=ref(false), rechargeOpen=ref(false)
const rechargeColumns=[{title:'充值单号',dataIndex:'rechargeNo',width:180,fixed:'left' as const},{title:'金额',key:'amount',width:130},{title:'付款时间',dataIndex:'paymentTime',width:170},{title:'凭证',key:'voucher',width:90},{title:'状态',key:'status',width:100},{title:'审核人',dataIndex:'reviewerName',width:120},{title:'备注',dataIndex:'remark',width:180},{title:'操作',key:'operate',width:80,fixed:'right' as const}]
const ledgerColumns=[{title:'时间',dataIndex:'occurredTime',width:170},{title:'类型',dataIndex:'entryType',width:100},{title:'业务单号',dataIndex:'businessNo',width:190},{title:'说明',dataIndex:'description'},{title:'金额',key:'amount',width:150}]
const statusText:Record<RechargeStatus,string>={PENDING:'待审核',APPROVED:'已通过',REJECTED:'已驳回',CANCELLED:'已取消',REVERSED:'已冲正'}
const statusColor:Record<RechargeStatus,string>={PENDING:'orange',APPROVED:'green',REJECTED:'red',CANCELLED:'default',REVERSED:'purple'}
const money=(v:number)=>Number(v||0).toFixed(2)
async function load(){loading.value=true;try{const [a,r,l]=await Promise.all([getOwnerFundAccounts(),getOwnerRecharges(),getOwnerLedger()]);if(isSuccess(a))accounts.value=a.data||[];if(isSuccess(r))recharges.value=r.data||[];if(isSuccess(l))ledger.value=l.data||[]}finally{loading.value=false}}
async function cancel(id:number){const res=await cancelOwnerRecharge(id);if(isSuccess(res)){message.success('已取消');load()}}
onMounted(load)
</script>
<style scoped>
.panel-actions{display:flex;justify-content:flex-end;gap:8px;margin-bottom:10px}
.negative-notice{margin-bottom:8px;padding:6px 10px;border:1px solid #ffd591;border-radius:6px;background:#fffaf0;color:#ad6800;font-size:12px}
.fund-summary-strip{display:flex;gap:10px;overflow-x:auto;padding-bottom:2px}
.fund-summary-item{flex:1 0 190px;min-height:82px;padding:10px 13px;border:1px solid #e8e8e8;border-radius:6px;background:#fff}
.fund-summary-item.negative{border-color:#ffccc7;background:#fff7f6}
.fund-label{color:#8c8c8c;font-size:12px;line-height:18px}
.fund-balance{margin-top:2px;font-size:21px;line-height:28px;font-weight:650;font-variant-numeric:tabular-nums}
.negative .fund-balance{color:#cf1322}
.fund-balance small{font-size:12px;font-weight:400}
.fund-detail{margin-top:3px;color:#8c8c8c;font-size:11px;white-space:nowrap}
.empty-account .fund-balance{color:#8c8c8c;font-size:18px}
.account-detail-tabs{margin-top:10px}
.in{color:#389e0d}.out{color:#cf1322}
</style>
