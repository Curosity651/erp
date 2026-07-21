<template>
  <a-modal
    :title="title"
    :open="visible"
    :mask-closable="false"
    :confirm-loading="submitLoading"
    :width="560"
    @ok="handleSubmit"
    @cancel="handleClose"
  >
    <a-form :model="formModel" :label-col="{ span: 24 }" :wrapper-col="{ span: 24 }">
      <a-form-item v-if="isUpdateForm" style="display: none">
        <a-input v-model:value="formModel.id" />
      </a-form-item>

      <!-- 基本信息 -->
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="物流商编码" v-bind="validateInfos.providerCode">
            <a-input
              v-model:value="formModel.providerCode"
              placeholder="请输入物流商编码"
              :disabled="isUpdateForm"
            >
              <template #prefix><barcode-outlined class="input-icon" /></template>
            </a-input>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="物流商名称" v-bind="validateInfos.providerName">
            <a-input v-model:value="formModel.providerName" placeholder="请输入物流商名称">
              <template #prefix><car-outlined class="input-icon" /></template>
            </a-input>
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 联系方式 -->
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="联系人">
            <a-input v-model:value="formModel.contactName" placeholder="请输入联系人">
              <template #prefix><user-outlined class="input-icon" /></template>
            </a-input>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="联系电话">
            <a-input v-model:value="formModel.contactPhone" placeholder="请输入联系电话">
              <template #prefix><phone-outlined class="input-icon" /></template>
            </a-input>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="联系邮箱">
            <a-input v-model:value="formModel.contactEmail" placeholder="请输入联系邮箱">
              <template #prefix><mail-outlined class="input-icon" /></template>
            </a-input>
          </a-form-item>
        </a-col>
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
          placeholder="请输入备注（可选）"
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
import type {
  LogisticsProviderDTO,
  LogisticsProviderPageVO
} from '@/api/wms/logistics-provider/types'
import { createLogisticsProvider, updateLogisticsProvider } from '@/api/wms/logistics-provider'
import { overrideProperties } from '@/utils/bean-utils'
import {
  BarcodeOutlined,
  CarOutlined,
  UserOutlined,
  PhoneOutlined,
  MailOutlined
} from '@ant-design/icons-vue'

const emits = defineEmits<{
  (e: 'submit-success'): void
}>()

const { title, visible, openModal, closeModal } = useModal()

const { formAction, isUpdateForm } = useFormAction()

// 表单模型
const formModel = reactive<LogisticsProviderDTO>({
  id: undefined,
  providerCode: '',
  providerName: '',
  contactName: undefined,
  contactPhone: undefined,
  contactEmail: undefined,
  status: 1,
  remark: undefined
})

// 表单的校验规则
const formRule = reactive({
  providerCode: [
    { required: true, message: '请输入物流商编码', trigger: 'blur' },
    { max: 50, message: '物流商编码长度不能超过50个字符', trigger: 'blur' }
  ],
  providerName: [
    { required: true, message: '请输入物流商名称', trigger: 'blur' },
    { max: 100, message: '物流商名称长度不能超过100个字符', trigger: 'blur' }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})

// 表单的提交请求
const formRequestMapping: FormRequestMapping<LogisticsProviderDTO> = {
  [FormAction.CREATE]: createLogisticsProvider,
  [FormAction.UPDATE]: updateLogisticsProvider
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
  open(newFormAction: FormAction, record?: LogisticsProviderPageVO) {
    openModal()
    resetFields()

    if (newFormAction === FormAction.CREATE) {
      title.value = '新建物流商'
      formModel.providerCode = ''
      formModel.providerName = ''
      formModel.contactName = undefined
      formModel.contactPhone = undefined
      formModel.contactEmail = undefined
      formModel.status = 1
      formModel.remark = undefined
    } else {
      title.value = '编辑物流商'
      overrideProperties(formModel, record)
    }
    formAction.value = newFormAction
  }
})
</script>

<style scoped>
.input-icon {
  color: #bfbfbf;
}

:deep(.ant-form-item) {
  margin-bottom: 16px;
}

:deep(.ant-form-item-label > label) {
  font-size: 13px;
  color: #595959;
}
</style>
