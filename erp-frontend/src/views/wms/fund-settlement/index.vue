<template>
  <div class="settlement-page">
    <a-card :bordered="false" title="资金结算">
      <template #extra><a-button :loading="loading" @click="loadAll">刷新</a-button></template>
      <a-tabs v-model:activeKey="scopeTab">
        <a-tab-pane key="owner" tab="货主结算">
          <a-alert type="info" show-icon message="货主充值由服务商审核；余额按已通过充值减物流产品费用计算。" style="margin-bottom:12px" />
          <a-tabs>
            <a-tab-pane key="accounts" tab="账户总览">
              <a-table :data-source="ownerAccounts" :columns="accountColumns" row-key="rowKey" size="small" :scroll="{x:900}">
                <template #bodyCell="{column,record}"><template v-if="column.key==='money'"><span :class="{danger:record.negative}">{{ money(record.balance) }} {{ record.currency }}</span></template><template v-else-if="column.key==='recharge'">{{ money(record.rechargeAmount) }}</template><template v-else-if="column.key==='charge'">{{ money(record.chargeAmount) }}</template></template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="reviews" :tab="`充值审核${ownerPending ? ` (${ownerPending})` : ''}`">
              <RechargeTable :rows="ownerRecharges" receiver @approve="approveOwner" @reject="openReject('owner',$event)" @reverse="openReverse('owner',$event)" />
            </a-tab-pane>
            <a-tab-pane key="income" tab="物流费用流水"><OperatorIncomePage /></a-tab-pane>
          </a-tabs>
        </a-tab-pane>
        <a-tab-pane key="platform" tab="平台结算">
          <div class="toolbar"><a-button type="primary" @click="rechargeOpen=true">向平台登记充值</a-button></div>
          <a-alert v-if="platformAccounts.some(a=>a.negative)" type="warning" show-icon message="平台账户余额为负，仅提示，不阻断仓库业务。" style="margin-bottom:12px" />
          <a-row :gutter="[12,12]" style="margin-bottom:16px"><a-col v-for="a in platformAccounts" :key="a.currency" :xs="24" :md="8"><a-card size="small"><a-statistic :title="`平台账户 · ${a.currency}`" :value="a.balance" :precision="2" :value-style="a.negative?{color:'#cf1322'}:{}"/><div class="sub">充值 {{ money(a.rechargeAmount) }} · 费用 {{ money(a.chargeAmount) }}</div></a-card></a-col></a-row>
          <a-tabs>
            <a-tab-pane key="recharges" tab="充值记录"><RechargeTable :rows="platformRecharges" applicant @cancel="cancelPlatform" /></a-tab-pane>
            <a-tab-pane key="expense" tab="服务账单"><OperatorExpensePage /></a-tab-pane>
          </a-tabs>
        </a-tab-pane>
      </a-tabs>
    </a-card>
    <RechargeCreateModal v-model:open="rechargeOpen" target="platform" @saved="loadAll" />
    <a-modal v-model:open="reasonOpen" :title="reasonMode==='reject'?'驳回充值':'冲正充值'" :confirm-loading="reasonLoading" @ok="submitReason">
      <a-form-item :label="reasonMode==='reject'?'驳回原因':'冲正原因'" required><a-textarea v-model:value="reason" :rows="4" :maxlength="500" /></a-form-item>
    </a-modal>
  </div>
</template>
<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref } from 'vue'
import { Button, Popconfirm, Space, Table, Tag, message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { cancelPlatformRecharge, getOperatorOwnerAccounts, getOperatorOwnerRecharges, getOperatorPlatformAccounts, getOperatorPlatformRecharges, reviewOwnerRecharge, reverseOwnerRecharge } from '@/api/fund-settlement'
import type { FundAccount, RechargeOrder } from '@/api/fund-settlement/types'
import OperatorIncomePage from '@/views/wms/finance-income/index.vue'
import OperatorExpensePage from '@/views/wms/finance-expense/index.vue'
import RechargeCreateModal from '@/views/wms/asset-finance/components/RechargeCreateModal.vue'

const RechargeTable=defineComponent({props:{rows:{type:Array as ()=>RechargeOrder[],required:true},receiver:Boolean,applicant:Boolean},emits:['approve','reject','reverse','cancel'],setup(props,{emit}){const cols=[{title:'充值单号',dataIndex:'rechargeNo',width:180,fixed:'left'},{title:'货主',dataIndex:'erpTenantName',width:130},{title:'金额',key:'amount',width:120},{title:'付款时间',dataIndex:'paymentTime',width:170},{title:'提交人',dataIndex:'applicantName',width:110},{title:'状态',key:'status',width:90},{title:'凭证',key:'voucher',width:80},{title:'原因',key:'reason',width:180},{title:'操作',key:'operate',width:180,fixed:'right'}];return()=>h(Table,{dataSource:props.rows,columns:cols,rowKey:'id',size:'small',scroll:{x:1200},pagination:{pageSize:10}},{bodyCell:({column,record}:{column:any;record:RechargeOrder})=>{if(column.key==='amount')return `${Number(record.amount).toFixed(2)} ${record.currency}`;if(column.key==='status')return h(Tag,{color:record.status==='APPROVED'?'green':record.status==='PENDING'?'orange':record.status==='REJECTED'?'red':'default'},()=>record.status);if(column.key==='voucher')return record.voucherUrl?h('a',{href:record.voucherUrl,target:'_blank'},'查看'): '--';if(column.key==='reason')return record.rejectReason||record.reverseReason||'--';if(column.key==='operate'){const actions:any[]=[];if(props.receiver&&record.status==='PENDING'){actions.push(h(Popconfirm,{title:'确认通过该充值？',onConfirm:()=>emit('approve',record.id)},{default:()=>h('a','通过')}),h('a',{onClick:()=>emit('reject',record.id)},'驳回'))}if(props.receiver&&record.status==='APPROVED')actions.push(h('a',{onClick:()=>emit('reverse',record.id)},'冲正'));if(props.applicant&&record.status==='PENDING')actions.push(h(Popconfirm,{title:'确认取消？',onConfirm:()=>emit('cancel',record.id)},{default:()=>h('a','取消')}));return h(Space,{},()=>actions)}return undefined}})}})
const scopeTab=ref('owner'),loading=ref(false),rechargeOpen=ref(false),ownerAccounts=ref<(FundAccount&{rowKey:string})[]>([]),platformAccounts=ref<FundAccount[]>([]),ownerRecharges=ref<RechargeOrder[]>([]),platformRecharges=ref<RechargeOrder[]>([])
const accountColumns=[{title:'货主',dataIndex:'erpTenantName',width:160,fixed:'left' as const},{title:'币种',dataIndex:'currency',width:80},{title:'累计充值',key:'recharge',width:130},{title:'物流费用',key:'charge',width:130},{title:'记录余额',key:'money',width:140},{title:'待审核',dataIndex:'pendingCount',width:90},{title:'最近变动',dataIndex:'lastChangeTime',width:170}]
const ownerPending=computed(()=>ownerRecharges.value.filter(x=>x.status==='PENDING').length),money=(v:number)=>Number(v||0).toFixed(2)
async function loadAll(){loading.value=true;try{const [oa,or,pa,pr]=await Promise.all([getOperatorOwnerAccounts(),getOperatorOwnerRecharges(),getOperatorPlatformAccounts(),getOperatorPlatformRecharges()]);if(isSuccess(oa))ownerAccounts.value=(oa.data||[]).map(x=>({...x,rowKey:`${x.erpTenantId}-${x.currency}`}));if(isSuccess(or))ownerRecharges.value=or.data||[];if(isSuccess(pa))platformAccounts.value=pa.data||[];if(isSuccess(pr))platformRecharges.value=pr.data||[]}finally{loading.value=false}}
async function approveOwner(id:number){const res=await reviewOwnerRecharge(id,true);if(isSuccess(res)){message.success('审核通过');loadAll()}}
async function cancelPlatform(id:number){const res=await cancelPlatformRecharge(id);if(isSuccess(res)){message.success('已取消');loadAll()}}
const reasonOpen=ref(false),reasonLoading=ref(false),reason=ref(''),reasonScope=ref<'owner'>('owner'),reasonMode=ref<'reject'|'reverse'>('reject'),reasonId=ref<number>()
function openReject(scope:'owner',id:number){reasonScope.value=scope;reasonMode.value='reject';reasonId.value=id;reason.value='';reasonOpen.value=true}
function openReverse(scope:'owner',id:number){reasonScope.value=scope;reasonMode.value='reverse';reasonId.value=id;reason.value='';reasonOpen.value=true}
async function submitReason(){if(!reason.value.trim()){message.warning('请填写原因');return}reasonLoading.value=true;try{const res=reasonMode.value==='reject'?await reviewOwnerRecharge(reasonId.value!,false,reason.value):await reverseOwnerRecharge(reasonId.value!,reason.value);if(isSuccess(res)){message.success('操作成功');reasonOpen.value=false;loadAll()}}finally{reasonLoading.value=false}}
onMounted(loadAll)
</script>
<style scoped>.toolbar{display:flex;justify-content:flex-end;margin-bottom:12px}.sub{margin-top:8px;color:#8c8c8c}.danger{color:#cf1322;font-weight:600}</style>
