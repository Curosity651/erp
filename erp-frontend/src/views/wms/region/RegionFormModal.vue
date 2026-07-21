<template>
  <a-modal
    :title="title"
    :open="visible"
    :mask-closable="false"
    :confirm-loading="submitLoading"
    :width="640"
    @ok="handleSubmit"
    @cancel="handleClose"
  >
    <a-form :model="formModel" :label-col="{ span: 24 }" :wrapper-col="{ span: 24 }">
      <a-form-item v-if="isUpdateForm" style="display: none">
        <a-input v-model:value="formModel.id" />
      </a-form-item>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="区域编码" v-bind="validateInfos.regionCode">
            <a-input
              v-model:value="formModel.regionCode"
              placeholder="请输入区域编码（如 RU、KZ）"
              :disabled="isUpdateForm"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="区域名称" v-bind="validateInfos.regionName">
            <a-input v-model:value="formModel.regionName" placeholder="请输入区域名称" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="启用状态" v-bind="validateInfos.status">
            <a-radio-group v-model:value="formModel.status">
              <a-radio :value="1">启用</a-radio>
              <a-radio :value="0">停用</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="备注说明">
        <a-textarea
          v-model:value="formModel.remark"
          placeholder="请输入备注（可选，如描述区域地理范围）"
          :rows="3"
          :maxlength="500"
          show-count
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { useModal } from '@/hooks/modal'
import { FormAction, useAdminForm, useFormAction } from '@/hooks/form'
import type { FormRequestMapping } from '@/hooks/form'
import type { RegionDTO, RegionPageVO } from '@/api/wms/region/types'
import { createRegion, updateRegion } from '@/api/wms/region'
import { overrideProperties } from '@/utils/bean-utils'

const emits = defineEmits<{
  (e: 'submit-success'): void
}>()

const { title, visible, openModal, closeModal } = useModal()

const { formAction, isUpdateForm } = useFormAction()

// 表单模型
const formModel = reactive<RegionDTO>({
  id: undefined,
  regionCode: '',
  regionName: '',
  status: 1,
  remark: undefined
})

// 表单的校验规则
const formRule = reactive({
  regionCode: [
    { required: true, message: '请输入区域编码', trigger: 'blur' },
    { max: 50, message: '区域编码长度不能超过50个字符', trigger: 'blur' }
  ],
  regionName: [
    { required: true, message: '请输入区域名称', trigger: 'blur' },
    { max: 100, message: '区域名称长度不能超过100个字符', trigger: 'blur' }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})

// 表单的提交请求
const formRequestMapping: FormRequestMapping<RegionDTO> = {
  [FormAction.CREATE]: createRegion,
  [FormAction.UPDATE]: updateRegion
}

const { submitLoading, validateAndSubmit, resetFields, validateInfos } = useAdminForm(
  formAction,
  formRequestMapping,
  formModel,
  formRule
)

/* 表单提交处理 */
const handleSubmit = () => {
  const model = { ...formModel }
  validateAndSubmit(model, {
    onSuccess: () => {
      closeModal()
      emits('submit-success')
    }
  })
}

/* 弹窗关闭方法 */
const handleClose = () => {
  closeModal()
  submitLoading.value = false
}

defineExpose({
  open(newFormAction: FormAction, record?: RegionPageVO) {
    openModal()
    resetFields()

    if (newFormAction === FormAction.CREATE) {
      title.value = '新建区域'
      formModel.regionCode = ''
      formModel.regionName = ''
      formModel.status = 1
      formModel.remark = undefined
    } else {
      title.value = '编辑区域'
      overrideProperties(formModel, record)
    }
    formAction.value = newFormAction
  }
})
</script>

<style scoped>
:deep(.ant-form-item) {
  margin-bottom: 16px;
}

:deep(.ant-form-item-label > label) {
  font-size: 13px;
  color: #595959;
}
</style>
