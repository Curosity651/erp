<template>
  <a-modal
    v-model:open="open"
    title="设置安全库存"
    :width="480"
    :confirm-loading="submitLoading"
    @ok="handleSubmit"
  >
    <!-- 复用 SkuBriefCell 展示 SKU 信息 -->
    <div class="sku-info">
      <SkuBriefCell :brief="skuBrief" />
      <div class="warehouse-name">{{ regionName }}</div>
    </div>

    <a-form ref="formRef" :model="formState" :rules="rules" layout="vertical" class="edit-form">
      <a-form-item label="最低安全库存" name="safetyStock">
        <a-input-number
          v-model:value="formState.safetyStock"
          :min="0"
          :precision="0"
          style="width: 100%"
          placeholder="不设置则使用全局配置"
        />
        <div class="mt-1 text-xs" style="color: var(--ant-color-text-secondary)">
          仅当「日均销量 × 预警天数」低于此值时生效，通常无需设置
        </div>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { saveSkuConfig } from '@/api/wms/inventory-forecast'
import type { SkuBriefVO } from '@/api/common/sku-types'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'

defineOptions({ name: 'SafetyStockEditModal' })

const props = defineProps<{
  regionId: number
  regionName: string
  skuBrief: SkuBriefVO
  currentSafetyStock: number
  // 现有通知配置，快捷改安全库存时原样带回，避免覆盖
  currentNotifyEnabled: boolean
  currentNotifyThresholdDays: number | null
}>()

const emit = defineEmits<{
  (e: 'success'): void
}>()

const open = defineModel<boolean>('open', { required: true })

const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const formState = reactive({
  safetyStock: 0
})

const rules: Record<string, Rule[]> = {
  safetyStock: [{ required: true, message: '请输入安全库存', trigger: 'blur' }]
}

watch(
  () => open.value,
  isOpen => {
    if (isOpen) {
      formState.safetyStock = props.currentSafetyStock
    }
  },
  { immediate: true }
)

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    const result = await saveSkuConfig({
      regionId: props.regionId,
      skuCode: props.skuBrief.skuCode,
      safetyStock: formState.safetyStock,
      // 保留原有通知配置，避免快捷改安全库存时覆盖
      notifyEnabled: props.currentNotifyEnabled,
      notifyThresholdDays: props.currentNotifyThresholdDays
    })
    if (isSuccess(result)) {
      message.success('保存成功')
      open.value = false
      emit('success')
    }
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.sku-info {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--ant-color-fill-tertiary);
  border-radius: var(--ant-border-radius);
}

.warehouse-name {
  margin-top: 8px;
  font-size: 13px;
  color: var(--ant-color-text-secondary);
}

.edit-form {
  margin-top: 16px;
}

.unit {
  margin-left: 8px;
  color: var(--ant-color-text-secondary);
}
</style>
