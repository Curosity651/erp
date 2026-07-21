<template>
  <a-modal
    v-model:open="visible"
    title="新建销售出库单"
    width="480px"
    :mask-closable="false"
    @cancel="handleCancel"
  >
    <a-form ref="formRef" :model="formModel" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item
        label="平台"
        name="platform"
        :rules="[{ required: true, message: '请选择平台' }]"
      >
        <PlatformSelect v-model:value="formModel.platform" placeholder="请选择平台" width="100%" />
      </a-form-item>

      <a-form-item
        label="出库仓库"
        name="warehouseId"
        :rules="[{ required: true, message: '请选择仓库' }]"
      >
        <WarehouseSelect
          v-model:value="formModel.warehouseId"
          placeholder="请选择出库仓库"
          width="100%"
          @change="handleWarehouseChange"
        />
      </a-form-item>
    </a-form>

    <a-alert type="warning" message="平台和仓库确定后不可修改" show-icon class="mt-4" />

    <template #footer>
      <a-button @click="handleCancel">取消</a-button>
      <a-button type="primary" @click="handleConfirm">下一步</a-button>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import type { WarehouseOptionVO } from '@/api/wms/warehouse/types'
import { PlatformSelect, type PlatformType } from '@/components/Platform'

export interface CreateOutboundForm {
  platform: PlatformType | undefined
  warehouseId: number | undefined
  warehouseName: string
}

const emits = defineEmits<{
  (e: 'confirm', form: CreateOutboundForm): void
}>()

const visible = ref(false)
const formRef = ref<FormInstance>()
const warehouseName = ref('')

const formModel = reactive<Omit<CreateOutboundForm, 'warehouseName'>>({
  platform: undefined,
  warehouseId: undefined
})

const open = () => {
  visible.value = true
  formModel.platform = 'wildberries'
  formModel.warehouseId = undefined
  warehouseName.value = ''
}

const handleCancel = () => {
  visible.value = false
}

const handleWarehouseChange = (_value?: number, option?: WarehouseOptionVO) => {
  warehouseName.value = option?.warehouseName || ''
}

const handleConfirm = async () => {
  try {
    await formRef.value?.validate()
    emits('confirm', {
      platform: formModel.platform,
      warehouseId: formModel.warehouseId,
      warehouseName: warehouseName.value
    })
    visible.value = false
  } catch {
    // 校验失败
  }
}

defineExpose({ open })
</script>
