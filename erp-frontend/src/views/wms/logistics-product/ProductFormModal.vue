<template>
  <a-modal
    v-model:open="open"
    :title="isEdit ? '编辑物流产品' : '新建物流产品'"
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
      <a-form-item label="产品编码" name="productCode">
        <a-input
          v-model:value="formModel.productCode"
          placeholder="选填，如 SMALL-ECO"
          :maxlength="50"
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
        label="单价(₽)"
        name="unitPrice"
        :rules="[{ required: true, message: '请输入单价' }]"
      >
        <a-input-number
          v-model:value="formModel.unitPrice"
          :min="0"
          :precision="2"
          style="width: 200px"
          placeholder="每次使用收费"
        />
        <span class="price-hint">名下货主每使用一次收取该金额</span>
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
import { saveLogisticsProduct } from '@/api/wms/logistics-product'
import type { LogisticsProductDTO, LogisticsProductVO } from '@/api/wms/logistics-product/types'
import { PRESET_TAGS } from '@/api/wms/logistics-product/types'

const emits = defineEmits<{ (e: 'success'): void }>()

const open = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formModel = reactive<LogisticsProductDTO>({
  id: undefined,
  productName: '',
  productCode: undefined,
  tags: [],
  unitPrice: undefined as unknown as number,
  remark: undefined
})

const isEdit = computed(() => formModel.id != null)
const presetOptions = PRESET_TAGS.map(t => ({ value: t, label: t }))

function openModal(record?: LogisticsProductVO) {
  formModel.id = record?.id
  formModel.productName = record?.productName ?? ''
  formModel.productCode = record?.productCode
  formModel.tags = record?.tags ? [...record.tags] : []
  formModel.unitPrice = record?.unitPrice as unknown as number
  formModel.remark = record?.remark
  open.value = true
}

async function submit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  doRequest(saveLogisticsProduct({ ...formModel }), {
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

defineExpose({ open: openModal })
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
