<template>
  <div class="form-section">
    <div class="section-title">
      <dollar-outlined class="section-icon" />
      付款信息
    </div>
    <a-form :label-col="{ span: 24 }" :wrapper-col="{ span: 24 }">
      <a-row :gutter="24">
        <!-- 首付款比例 -->
        <a-col :span="6">
          <a-form-item label="首付款比例">
            <a-input-number
              :value="formModel.prepayRatio"
              :min="0"
              :max="100"
              :precision="2"
              :disabled="disabled || paymentTermsDisabled"
              addon-after="%"
              style="width: 100%"
              @change="(value: number | null) => updatePaymentTerms({ prepayRatio: value ?? undefined })"
            />
          </a-form-item>
        </a-col>
        <!-- 尾款账期 -->
        <a-col :span="6">
          <a-form-item label="尾款账期">
            <a-input-number
              :value="formModel.balancePaymentDays"
              :min="0"
              :max="365"
              :precision="0"
              :disabled="disabled || paymentTermsDisabled"
              addon-after="天"
              style="width: 100%"
              @change="(value: number | null) => updatePaymentTerms({ balancePaymentDays: value ?? undefined })"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="24">
        <!-- 首付款状态 -->
        <a-col :span="12">
          <a-form-item>
            <div class="payment-status-row">
              <a-checkbox v-model:checked="prepayPaid" :disabled="disabled || prepayAlreadyPaid">
                首付款已付
              </a-checkbox>
              <span v-if="prepayAmount" class="payment-amount">
                金额: {{ currencySymbol }}{{ formatAmount(prepayAmount) }}
              </span>
            </div>
            <!-- 首付款凭证上传 -->
            <div v-if="prepayPaid" class="voucher-upload">
              <sys-file-upload
                :model-value="formModel.paymentInfo.prepayVoucherFileId"
                bucket-key="private-files"
                button-text="上传首付款凭证"
                :allowed-types="voucherAllowedTypes"
                :disabled="disabled || prepayAlreadyPaid"
                :allow-delete="!prepayAlreadyPaid"
                @update:model-value="(val: number | undefined) => updatePaymentInfo({ prepayVoucherFileId: val })"
              />
              <div
                v-if="!formModel.paymentInfo.prepayVoucherFileId && prepayPaid"
                class="voucher-hint"
              >
                <exclamation-circle-outlined /> 已付款需上传凭证
              </div>
            </div>
          </a-form-item>
        </a-col>

        <!-- 尾款状态 -->
        <a-col :span="12">
          <a-form-item>
            <div class="payment-status-row">
              <a-checkbox v-model:checked="balancePaid" :disabled="disabled || balanceAlreadyPaid">
                尾款已付
              </a-checkbox>
              <span v-if="balanceAmount" class="payment-amount">
                金额: {{ currencySymbol }}{{ formatAmount(balanceAmount) }}
              </span>
            </div>
            <!-- 尾款凭证上传 -->
            <div v-if="balancePaid" class="voucher-upload">
              <sys-file-upload
                :model-value="formModel.paymentInfo.balanceVoucherFileId"
                bucket-key="private-files"
                button-text="上传尾款凭证"
                :allowed-types="voucherAllowedTypes"
                :disabled="disabled || balanceAlreadyPaid"
                :allow-delete="!balanceAlreadyPaid"
                @update:model-value="(val: number | undefined) => updatePaymentInfo({ balanceVoucherFileId: val })"
              />
              <div
                v-if="!formModel.paymentInfo.balanceVoucherFileId && balancePaid"
                class="voucher-hint"
              >
                <exclamation-circle-outlined /> 已付款需上传凭证
              </div>
            </div>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { ExclamationCircleOutlined, DollarOutlined } from '@ant-design/icons-vue'
import { SysFileUpload } from '@/components/Upload'
import { getCurrencySymbol } from '@/utils/currency-utils'
import type { PaymentInfoDTO } from '@/api/wms/purchase-order/types'

interface FormModel {
  prepayRatio?: number
  balancePaymentDays?: number
  paymentInfo: PaymentInfoDTO
}

interface Props {
  // 表单模型
  formModel: FormModel
  // 合同总金额（用于计算付款金额）
  totalAmount?: number
  // 币种编码（用于展示金额符号）
  currencyCode?: string
  // 是否禁用整个区块
  disabled?: boolean
  // 付款条款是否禁用（首付比例、尾款账期）
  paymentTermsDisabled?: boolean
  // 首付款是否已付（数据库中已确认）
  prepayAlreadyPaid?: boolean
  // 尾款是否已付（数据库中已确认）
  balanceAlreadyPaid?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  paymentTermsDisabled: false,
  prepayAlreadyPaid: false,
  balanceAlreadyPaid: false
})

const emits = defineEmits<{
  (e: 'update:paymentInfo', value: PaymentInfoDTO): void
  (e: 'update:terms', value: { prepayRatio?: number; balancePaymentDays?: number }): void
}>()

// 金额符号（按币种）
const currencySymbol = computed(() => getCurrencySymbol(props.currencyCode))

// 凭证允许的文件类型
const voucherAllowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'application/pdf']

// 更新付款信息的辅助函数
const updatePaymentInfo = (updates: Partial<PaymentInfoDTO>) => {
  emits('update:paymentInfo', {
    ...props.formModel.paymentInfo,
    ...updates
  })
}

const updatePaymentTerms = (updates: { prepayRatio?: number; balancePaymentDays?: number }) => {
  emits('update:terms', {
    prepayRatio: props.formModel.prepayRatio,
    balancePaymentDays: props.formModel.balancePaymentDays,
    ...updates
  })
}

// 首付款已付状态
const prepayPaid = computed({
  get: () => props.formModel.paymentInfo.prepayStatus === 1,
  set: (val: boolean) => {
    updatePaymentInfo({
      prepayStatus: val ? 1 : 0,
      prepayVoucherFileId: val ? props.formModel.paymentInfo.prepayVoucherFileId : undefined
    })
  }
})

// 尾款已付状态
const balancePaid = computed({
  get: () => props.formModel.paymentInfo.balanceStatus === 1,
  set: (val: boolean) => {
    updatePaymentInfo({
      balanceStatus: val ? 1 : 0,
      balanceVoucherFileId: val ? props.formModel.paymentInfo.balanceVoucherFileId : undefined
    })
  }
})

// 首付款金额
const prepayAmount = computed(() => {
  if (!props.totalAmount || !props.formModel.prepayRatio) return 0
  return (props.totalAmount * props.formModel.prepayRatio) / 100
})

// 尾款金额
const balanceAmount = computed(() => {
  if (!props.totalAmount) return 0
  return props.totalAmount - prepayAmount.value
})

// 格式化金额
const formatAmount = (amount: number): string => {
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 校验付款信息
const validate = (): string | null => {
  if (prepayPaid.value && !props.formModel.paymentInfo.prepayVoucherFileId) {
    return '首付款已付时必须上传凭证'
  }
  if (balancePaid.value && !props.formModel.paymentInfo.balanceVoucherFileId) {
    return '尾款已付时必须上传凭证'
  }
  return null
}

defineExpose({ validate })
</script>

<style scoped>
.form-section {
  padding: 16px 20px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.section-icon {
  margin-right: 8px;
  color: #1890ff;
  font-size: 16px;
}

.payment-status-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.payment-amount {
  color: #f5222d;
  font-weight: 500;
}

.voucher-upload {
  margin-top: 12px;
  padding-left: 24px;
}

.voucher-hint {
  margin-top: 8px;
  color: #faad14;
  font-size: 12px;
}
</style>
