<template>
  <a-modal v-model:open="open" title="登记充值" :confirm-loading="submitting" @ok="submit" @cancel="reset">
    <a-alert type="info" show-icon message="本功能仅登记线下付款记录，不会发起真实支付。" style="margin-bottom: 16px" />
    <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 6 }">
      <a-form-item label="金额" name="amount"><a-input-number v-model:value="form.amount" :min="0.01" :precision="2" style="width:100%" /></a-form-item>
      <a-form-item label="币种" name="currency"><a-select v-model:value="form.currency" :options="currencyOptions" /></a-form-item>
      <a-form-item label="付款时间" name="paymentTime"><a-date-picker v-model:value="form.paymentTime" show-time style="width:100%" /></a-form-item>
      <a-form-item label="付款凭证" name="voucherFileId">
        <SysFileUpload v-model="form.voucherFileId" button-text="上传付款凭证" :allowed-types="['application/pdf','image/jpeg','image/jpg','image/png']" />
      </a-form-item>
      <a-form-item label="备注"><a-textarea v-model:value="form.remark" :maxlength="500" :rows="3" /></a-form-item>
    </a-form>
  </a-modal>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message, type FormInstance } from 'ant-design-vue'
import SysFileUpload from '@/components/Upload/SysFileUpload.vue'
import { isSuccess } from '@/api'
import { createOwnerRecharge, createPlatformRecharge } from '@/api/fund-settlement'
import { defaultRechargeCurrency, rechargeCurrencyOptions } from './recharge-currency-policy'

const props = defineProps<{ open: boolean; target: 'owner' | 'platform' }>()
const emit = defineEmits<{ (e:'update:open', value:boolean):void; (e:'saved'):void }>()
const open = ref(props.open)
watch(() => props.open, v => { open.value = v; if (v) reset() })
watch(open, v => emit('update:open', v))
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive<{ amount?:number; currency:string; paymentTime:Dayjs; voucherFileId?:number; remark?:string }>({ currency:defaultRechargeCurrency(props.target), paymentTime:dayjs() })
const currencyOptions = computed(() => rechargeCurrencyOptions(props.target))
const rules = { amount:[{ required:true, message:'请输入金额' }], currency:[{ required:true }], paymentTime:[{ required:true }], voucherFileId:[{ required:true, message:'请上传付款凭证' }] }
function reset() { form.amount=undefined; form.currency=defaultRechargeCurrency(props.target); form.paymentTime=dayjs(); form.voucherFileId=undefined; form.remark=undefined; formRef.value?.clearValidate() }
async function submit() {
  await formRef.value?.validate()
  if (!form.amount || !form.voucherFileId) return
  submitting.value=true
  try {
    const api = props.target === 'owner' ? createOwnerRecharge : createPlatformRecharge
    const res = await api({ amount:form.amount, currency:form.currency, paymentTime:form.paymentTime.toISOString(), voucherFileId:form.voucherFileId, remark:form.remark })
    if (isSuccess(res)) { message.success('充值记录已提交，等待收款方审核'); open.value=false; reset(); emit('saved') }
  } finally { submitting.value=false }
}
</script>
