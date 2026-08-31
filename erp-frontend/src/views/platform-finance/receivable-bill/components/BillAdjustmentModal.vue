<template>
  <a-modal
    :open="open"
    title="调整账单费用"
    :width="680"
    :confirm-loading="saving"
    ok-text="保存调整"
    :mask-closable="false"
    @ok="submit"
    @cancel="close"
  >
    <a-alert
      type="info"
      show-icon
      message="原始费用不会被覆盖。补收费以正数计入，冲减费以负数计入。"
      class="notice"
    />
    <a-descriptions v-if="bill" :column="3" size="small" bordered class="bill-summary">
      <a-descriptions-item label="账期">{{ bill.billMonth }}</a-descriptions-item>
      <a-descriptions-item label="WMS服务商">{{ bill.wmsTenantName }}</a-descriptions-item>
      <a-descriptions-item label="当前应收">{{ formatMoney(bill.totalAmount) }}</a-descriptions-item>
    </a-descriptions>
    <a-form :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
      <a-form-item label="调整类型" required>
        <a-radio-group v-model:value="form.adjustmentType">
          <a-radio-button value="SUPPLEMENT">补收费</a-radio-button>
          <a-radio-button value="DEDUCTION">冲减费</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="收费项目" required>
        <a-select
          v-model:value="form.feeCode"
          :options="rateOptions"
          :loading="rateLoading"
          show-search
          option-filter-prop="label"
          placeholder="请选择需要调整的收费项目"
        />
      </a-form-item>
      <a-form-item label="调整金额" required>
        <a-input-number
          v-model:value="form.amount"
          :min="0.01"
          :precision="2"
          addon-after="CNY"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="业务凭证" required>
        <a-input v-model:value="form.sourceRef" placeholder="请输入业务单号或费用凭证号" />
      </a-form-item>
      <a-form-item label="调整原因" required>
        <a-textarea
          v-model:value="form.remark"
          :rows="3"
          placeholder="说明遗漏收费或冲减原因"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { addBillAdjustment, listEffectiveRates } from '@/api/platform-finance/receivable'
import type {
  BillAdjustmentType,
  FeeRate,
  MonthlyBillVO
} from '@/api/platform-finance/receivable/types'
import { formatMoney } from '../constants'

const props = defineProps<{ open: boolean; bill?: MonthlyBillVO }>()
const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'saved'): void
}>()

const saving = ref(false)
const rateLoading = ref(false)
const rates = ref<FeeRate[]>([])
const form = reactive({
  adjustmentType: 'SUPPLEMENT' as BillAdjustmentType,
  feeCode: undefined as string | undefined,
  amount: undefined as number | undefined,
  sourceRef: '',
  remark: ''
})
const rateOptions = computed(() =>
  rates.value.map(rate => ({ value: rate.feeCode, label: `${rate.feeName}（${rate.feeCode}）` }))
)

watch(
  () => [props.open, props.bill?.wmsTenantId] as const,
  async ([visible, tenantId]) => {
    if (!visible || !tenantId) return
    reset()
    rateLoading.value = true
    try {
      const response = await listEffectiveRates(tenantId)
      if (isSuccess(response)) rates.value = response.data || []
    } finally {
      rateLoading.value = false
    }
  }
)

function reset() {
  rates.value = []
  Object.assign(form, {
    adjustmentType: 'SUPPLEMENT',
    feeCode: undefined,
    amount: undefined,
    sourceRef: '',
    remark: ''
  })
}

function close() {
  emit('update:open', false)
}

async function submit() {
  if (!props.bill || !form.feeCode || !form.amount || !form.sourceRef.trim() || !form.remark.trim()) {
    message.warning('请完整填写调整类型、收费项目、金额、业务凭证和调整原因')
    return
  }
  saving.value = true
  try {
    const response = await addBillAdjustment(props.bill.id, {
      adjustmentType: form.adjustmentType,
      feeCode: form.feeCode,
      amount: form.amount,
      sourceRef: form.sourceRef.trim(),
      remark: form.remark.trim()
    })
    if (isSuccess(response)) {
      message.success(form.adjustmentType === 'SUPPLEMENT' ? '补收费已登记' : '冲减费已登记')
      close()
      emit('saved')
    }
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.notice { margin-bottom: 16px; }
.bill-summary { margin-bottom: 18px; }
</style>
