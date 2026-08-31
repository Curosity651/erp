<template>
  <a-modal
    v-model:open="open"
    :title="modalTitle"
    :confirm-loading="submitting"
    :width="560"
    @ok="submit"
  >
    <a-form
      ref="formRef"
      :model="formModel"
      :label-col="{ style: { width: '90px' } }"
      style="margin-top: 16px"
    >
      <a-alert
        v-if="pricingLocked"
        type="info"
        show-icon
        message="该产品已产生业务数据，产品编码、默认费用和币种已锁定；如需调价请使用“复制调价”。"
        style="margin-bottom: 16px"
      />
      <a-form-item
        label="产品名称"
        name="productName"
        :rules="[{ required: true, message: '请输入产品名称' }]"
      >
        <a-input
          v-model:value="formModel.productName"
          placeholder="如：小件经济渠道"
          :maxlength="100"
        />
      </a-form-item>
      <a-form-item
        label="产品编码"
        name="productCode"
        :rules="[{ required: true, whitespace: true, message: '请输入产品编码' }]"
      >
        <a-input
          v-model:value="formModel.productCode"
          placeholder="如 STANDARD"
          :maxlength="50"
          :disabled="pricingLocked"
          @blur="normalizeProductCode"
        />
      </a-form-item>
      <a-form-item label="特性词条" name="tags">
        <a-select
          v-model:value="formModel.tags"
          mode="tags"
          placeholder="选择或输入自定义词条后回车"
          :options="presetOptions"
          :token-separators="[',']"
        />
        <div class="tag-hint">预置：大件/小件/自提等，可直接输入自定义词条</div>
      </a-form-item>
      <a-form-item
        label="默认费用"
        name="unitPrice"
        :rules="[{ required: true, message: '请输入单价' }]"
      >
        <a-input-number
          v-model:value="formModel.unitPrice"
          :min="0"
          :precision="2"
          style="width: 200px"
          placeholder="每次使用收费"
          :disabled="pricingLocked"
        />
        <a-select
          v-model:value="formModel.currency"
          :disabled="pricingLocked"
          style="width: 100px; margin-left: 8px"
        >
          <a-select-option value="RUB">RUB</a-select-option>
          <a-select-option value="CNY">CNY</a-select-option>
          <a-select-option value="USD">USD</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="产品说明" name="productDescription">
        <a-textarea
          v-model:value="formModel.productDescription"
          :rows="4"
          :maxlength="2000"
          show-count
          placeholder="说明适用场景、预计时效和仓库处理方式"
        />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formModel.remark" :rows="2" :maxlength="500" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import { doRequest } from '@/utils/axios/request'
import { createLogisticsProduct, updateLogisticsProduct } from '@/api/wms/logistics-product'
import type { LogisticsProductDTO, LogisticsProductVO } from '@/api/wms/logistics-product/types'
import { PRESET_TAGS } from '@/api/wms/logistics-product/types'
import { isPricingLocked, suggestVersionCode } from './product-lifecycle'

const emits = defineEmits<{ (e: 'success'): void }>()

const open = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const sourceRecord = ref<LogisticsProductVO>()
const copyMode = ref(false)

const formModel = reactive<LogisticsProductDTO>({
  id: undefined,
  productName: '',
  productCode: '',
  tags: [],
  unitPrice: undefined as unknown as number,
  currency: 'RUB',
  productDescription: undefined,
  remark: undefined
})

const isEdit = computed(() => formModel.id != null)
const pricingLocked = computed(() => isEdit.value && isPricingLocked(sourceRecord.value))
const modalTitle = computed(() => {
  if (copyMode.value) return '复制物流产品调价'
  return isEdit.value ? '编辑物流产品' : '新建物流产品'
})
const presetOptions = PRESET_TAGS.map(t => ({ value: t, label: t }))

function openModal(record?: LogisticsProductVO) {
  copyMode.value = false
  sourceRecord.value = record
  formModel.id = record?.id
  formModel.productName = record?.productName ?? ''
  formModel.productCode = record?.productCode ?? ''
  formModel.tags = record?.tags ? [...record.tags] : []
  formModel.unitPrice = record?.unitPrice as unknown as number
  formModel.currency = record?.currency || 'RUB'
  formModel.productDescription = record?.productDescription
  formModel.remark = record?.remark
  open.value = true
}

function openCopy(record: LogisticsProductVO) {
  copyMode.value = true
  sourceRecord.value = undefined
  formModel.id = undefined
  formModel.productName = record.productName
  formModel.productCode = suggestVersionCode(record.productCode)
  formModel.tags = record.tags ? [...record.tags] : []
  formModel.unitPrice = record.unitPrice
  formModel.currency = record.currency || 'RUB'
  formModel.productDescription = record.productDescription
  formModel.remark = record.remark
  open.value = true
}

async function submit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  normalizeProductCode()
  const request = isEdit.value
    ? updateLogisticsProduct(formModel.id!, { ...formModel })
    : createLogisticsProduct({ ...formModel })
  doRequest(request, {
    successMessage: '保存成功',
    onSuccess: () => {
      open.value = false
      emits('success')
    },
    onFinally: () => {
      submitting.value = false
    }
  })
}

function normalizeProductCode() {
  formModel.productCode = formModel.productCode.trim().toUpperCase()
}

defineExpose({ open: openModal, openCopy })
</script>

<script lang="ts">
export default {
  name: 'ProductFormModal'
}
</script>

<style scoped>
.tag-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 4px;
}
.price-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-left: 12px;
}
</style>
