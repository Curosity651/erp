<template>
  <a-modal
    :open="open"
    title="登记补充费用"
    :width="640"
    :confirm-loading="saving"
    @ok="submit"
    @cancel="emit('update:open', false)"
  >
    <a-alert
      type="info"
      show-icon
      message="用于登记配送、退货取件和验货等尚未由业务单自动生成的费用。"
      class="notice"
    />
    <a-form :model="form" :label-col="{ span: 6 }">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="账期" required>
            <a-date-picker v-model:value="billMonth" picker="month" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="WMS服务商" required>
            <a-select
              v-model:value="form.wmsTenantId"
              :options="operatorOptions"
              show-search
              option-filter-prop="label"
              @change="loadRates"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="仓库">
            <a-select
              v-model:value="form.warehouseId"
              :options="warehouseOptions"
              allow-clear
              show-search
              option-filter-prop="label"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="收费项目" required>
            <a-select
              v-model:value="form.feeCode"
              :options="rateOptions"
              :loading="rateLoading"
              @change="handleRateChange"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="quantityLabel" required>
            <a-input-number
              v-model:value="form.quantity"
              :min="0.0001"
              :precision="selectedRate?.billingUnit === 'ACTUAL' ? 2 : 0"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="单价">
            <a-input-number
              :value="selectedRate?.unitPrice || 0"
              disabled
              :precision="2"
              addon-after="元"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col v-if="selectedRate?.billingUnit === 'ACTUAL'" :span="12">
          <a-form-item label="实际金额" required>
            <a-input-number
              v-model:value="form.actualAmount"
              :min="0.01"
              :precision="2"
              addon-after="元"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col :span="selectedRate?.billingUnit === 'ACTUAL' ? 12 : 24">
          <a-form-item
            label="业务凭证"
            required
            :label-col="{ span: selectedRate?.billingUnit === 'ACTUAL' ? 6 : 3 }"
          >
            <a-input v-model:value="form.sourceRef" placeholder="运输单号、退货单号或费用凭证号" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="计费说明" required :label-col="{ span: 3 }">
            <a-textarea
              v-model:value="form.remark"
              :rows="3"
              placeholder="填写距离、箱数、分摊依据或实际费用来源"
            />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <div v-if="selectedRate" class="estimate">
      预计计费：
      <b>{{ money(estimatedAmount) }}</b>
      <span>{{ selectedRate.remark }}</span>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { getWarehouseOptions } from '@/api/wms/warehouse'
import { addManualCharge, listEffectiveRates } from '@/api/platform-finance/receivable'
import type { FeeRate } from '@/api/platform-finance/receivable/types'

const props = defineProps<{
  open: boolean
  operatorOptions: { label: string; value: number }[]
}>()
const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'saved'): void
}>()

const saving = ref(false)
const rateLoading = ref(false)
const billMonth = ref<Dayjs>(dayjs())
const rates = ref<FeeRate[]>([])
const warehouses = ref<any[]>([])
const form = reactive({
  wmsTenantId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  feeCode: undefined as string | undefined,
  quantity: 1,
  actualAmount: undefined as number | undefined,
  sourceRef: '',
  remark: ''
})

const warehouseOptions = computed(() =>
  warehouses.value.map(row => ({ value: row.id, label: row.warehouseName }))
)
const billableRates = computed(() =>
  rates.value.filter(rate =>
    ['DELIVERY', 'RETURN', 'INSPECTION', 'DRIVER'].includes(rate.feeType)
  )
)
const rateOptions = computed(() =>
  billableRates.value.map(rate => ({
    value: rate.feeCode,
    label: `${rate.feeName}（${unitText(rate.billingUnit)}）`
  }))
)
const selectedRate = computed(() => rates.value.find(rate => rate.feeCode === form.feeCode))
const quantityLabel = computed(() =>
  selectedRate.value?.billingUnit === 'ACTUAL'
    ? '费用笔数'
    : `计费数量（${unitText(selectedRate.value?.billingUnit)}）`
)
const estimatedAmount = computed(() =>
  selectedRate.value?.billingUnit === 'ACTUAL'
    ? Number(form.actualAmount || 0)
    : Number(form.quantity || 0) * Number(selectedRate.value?.unitPrice || 0)
)

watch(
  () => props.open,
  async visible => {
    if (!visible) return
    if (!warehouses.value.length) {
      const response = await getWarehouseOptions()
      if (isSuccess(response)) warehouses.value = response.data || []
    }
  }
)

async function loadRates() {
  rates.value = []
  form.feeCode = undefined
  form.actualAmount = undefined
  if (!form.wmsTenantId) return
  rateLoading.value = true
  try {
    const response = await listEffectiveRates(form.wmsTenantId)
    if (isSuccess(response)) rates.value = response.data || []
  } finally {
    rateLoading.value = false
  }
}

function handleRateChange() {
  form.quantity = 1
  form.actualAmount = undefined
}

async function submit() {
  if (
    !billMonth.value ||
    !form.wmsTenantId ||
    !form.feeCode ||
    !form.sourceRef.trim() ||
    !form.remark.trim()
  ) {
    message.warning('请填写账期、服务商、收费项目、业务凭证和计费说明')
    return
  }
  if (selectedRate.value?.billingUnit === 'ACTUAL' && !form.actualAmount) {
    message.warning('实报实销项目必须填写实际金额')
    return
  }
  saving.value = true
  try {
    const response = await addManualCharge({
      wmsTenantId: form.wmsTenantId,
      warehouseId: form.warehouseId,
      billMonth: billMonth.value.format('YYYY-MM'),
      feeCode: form.feeCode,
      quantity: form.quantity,
      actualAmount: form.actualAmount,
      sourceRef: form.sourceRef.trim(),
      remark: form.remark.trim()
    })
    if (isSuccess(response)) {
      message.success('补充费用已登记到对应账期')
      emit('update:open', false)
      emit('saved')
      reset()
    }
  } finally {
    saving.value = false
  }
}

function reset() {
  billMonth.value = dayjs()
  rates.value = []
  Object.assign(form, {
    wmsTenantId: undefined,
    warehouseId: undefined,
    feeCode: undefined,
    quantity: 1,
    actualAmount: undefined,
    sourceRef: '',
    remark: ''
  })
}

function unitText(unit?: string) {
  return (
    {
      ACTUAL: '按实际金额',
      BOX: '箱',
      ITEM: '件',
      PALLET: '托',
      CBM: '立方米',
      RATE: '比例'
    }[unit || ''] || unit || '数量'
  )
}

function money(value: number) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
}
</script>

<style scoped>
.notice {
  margin-bottom: 16px;
}
.estimate {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  color: #595959;
}
.estimate b {
  color: #1677ff;
  font-size: 16px;
}
.estimate span {
  margin-left: auto;
  color: #8c8c8c;
  font-size: 12px;
}
</style>
