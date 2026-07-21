<template>
  <a-modal
    v-model:open="open"
    :title="isEdit ? '编辑配置' : '新增配置'"
    :width="520"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-form
      ref="formRef"
      :model="formState"
      :rules="rules"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
    >
      <a-form-item label="区域" name="regionId">
        <RegionSelect
          v-model:value="formState.regionId"
          :disabled="isEdit"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="SKU" name="skuCode">
        <SkuSelectInput
          v-model="formState.skuCode"
          :disabled="isEdit"
          @update:model-value="() => formRef?.validateFields(['skuCode'])"
        />
      </a-form-item>
      <a-form-item label="安全库存" name="safetyStock">
        <a-input-number
          v-model:value="formState.safetyStock"
          :min="0"
          :placeholder="`留空使用全局(${defaultSafetyStock}件)`"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="启用通知" name="notifyEnabled">
        <a-switch v-model:checked="formState.notifyEnabled" />
      </a-form-item>
      <a-form-item label="预警阈值">
        <a-input-number
          v-model:value="formState.notifyThresholdDays"
          :min="1"
          :placeholder="`留空使用全局(${thresholdDays}天)`"
          style="width: 100%"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { Modal } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import { doRequest } from '@/utils/axios/request'
import { saveSkuConfig, deleteSkuConfig } from '@/api/wms/inventory-forecast'
import RegionSelect from '@/components/Lov/RegionSelect.vue'
import SkuSelectInput from '@/components/Sku/SkuSelectInput.vue'
import type { InventoryConfigVO, InventoryConfigDTO } from '@/api/wms/inventory-forecast/types'

defineOptions({ name: 'SkuConfigFormModal' })

const props = defineProps<{
  editData?: InventoryConfigVO
  thresholdDays: number
  defaultSafetyStock: number
}>()

const open = defineModel<boolean>('open', { required: true })

const emit = defineEmits<{
  success: []
}>()

const formRef = ref<FormInstance>()
const isEdit = computed(() => !!props.editData)

const formState = reactive<InventoryConfigDTO>({
  id: undefined,
  regionId: undefined as unknown as number,
  skuCode: '',
  safetyStock: undefined,
  notifyEnabled: true,
  notifyThresholdDays: undefined
})

const rules: Record<string, Rule[]> = {
  regionId: [{ required: true, message: '请选择区域' }],
  skuCode: [{ required: true, message: '请选择SKU' }]
}

watch(
  () => props.editData,
  data => {
    if (data) {
      Object.assign(formState, {
        id: data.id,
        regionId: data.regionId,
        skuCode: data.skuCode,
        safetyStock: data.safetyStock,
        notifyEnabled: data.notifyEnabled,
        notifyThresholdDays: data.notifyThresholdDays
      })
    } else {
      Object.assign(formState, {
        id: undefined,
        regionId: undefined,
        skuCode: '',
        safetyStock: undefined,
        notifyEnabled: true,
        notifyThresholdDays: undefined
      })
    }
  },
  { immediate: true }
)

async function handleSubmit() {
  try {
    await formRef.value?.validate()

    // 校验：安全库存和预警阈值不能同时为空
    if (formState.safetyStock == null && formState.notifyThresholdDays == null) {
      Modal.confirm({
        title: '配置无效',
        content: '安全库存和预警阈值都未设置，该配置与全局配置一致，建议删除。',
        okText: '删除配置',
        okType: 'danger',
        cancelText: '继续编辑',
        onOk: () => {
          if (isEdit.value && formState.id) {
            doRequest(deleteSkuConfig(formState.id), {
              successMessage: '删除成功',
              onSuccess: () => {
                open.value = false
                emit('success')
              }
            })
          } else {
            open.value = false
          }
        }
      })
      return
    }

    doRequest(saveSkuConfig(formState), {
      successMessage: isEdit.value ? '更新成功' : '创建成功',
      onSuccess: () => {
        open.value = false
        emit('success')
      }
    })
  } catch {
    // 校验失败
  }
}

function handleCancel() {
  formRef.value?.resetFields()
}
</script>
