<template>
  <a-modal
    :title="title"
    :open="visible"
    :mask-closable="false"
    :body-style="{ paddingBottom: '8px' }"
    :confirm-loading="submitLoading"
    :width="400"
    @ok="handleSubmit"
    @cancel="handleClose"
  >
    <a-form :model="formModel" :label-col="labelCol" :wrapper-col="wrapperCol">
      <a-form-item v-if="isUpdateForm" style="display: none">
        <a-input v-model:value="formModel.id" />
      </a-form-item>
      <a-form-item :label="t('system.projectGroup.name')" v-bind="validateInfos.name">
        <a-input
          v-model:value="formModel.name"
          :placeholder="t('system.projectGroup.namePlaceholder')"
        />
      </a-form-item>
      <a-form-item :label="t('system.projectGroup.code')" v-bind="validateInfos.code">
        <a-input
          v-model:value="formModel.code"
          :placeholder="t('system.projectGroup.codePlaceholder')"
          :disabled="isUpdateForm"
        />
      </a-form-item>
      <a-form-item :label="t('system.projectGroup.description')">
        <a-textarea
          v-model:value="formModel.description"
          :placeholder="t('system.projectGroup.descriptionPlaceholder')"
        />
      </a-form-item>
      <a-form-item :label="t('system.projectGroup.status')">
        <dict-radio-group v-model:value="formModel.status" dict-code="enable_status" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { useModal } from '@/hooks/modal'
import { FormAction, useAdminForm, useFormAction } from '@/hooks/form'
import type { FormRequestMapping } from '@/hooks/form'
import type { ProjectGroupDTO, ProjectGroupPageVO } from '@/api/system/project-group/types'
import { createProjectGroup, updateProjectGroup } from '@/api/system/project-group'
import { overrideProperties } from '@/utils/bean-utils'
import type { ColProps } from 'ant-design-vue'
import { DictRadioGroup } from '@/components/Dict'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const labelCol: ColProps = {
  sm: { span: 24 },
  md: { span: 5 }
}

const wrapperCol: ColProps = {
  sm: { span: 24 },
  md: { span: 19 }
}

const emits = defineEmits<{
  (e: 'submit-success'): void
}>()

const { title, visible, openModal, closeModal } = useModal()

const { formAction, isUpdateForm } = useFormAction()

// 表单模型
const formModel = reactive<ProjectGroupDTO>({
  // 项目组ID
  id: undefined,
  // 项目组名称
  name: undefined,
  // 项目组编码，唯一
  code: undefined,
  // 项目组描述
  description: undefined,
  // 状态（1-启用，0-停用）
  status: 1
})

// 表单的校验规则
const formRule = computed(() => ({
  name: [
    { required: true, message: t('system.projectGroup.validation.name'), trigger: 'blur' },
    { max: 50, message: t('system.projectGroup.validation.nameLength'), trigger: 'blur' }
  ],
  code: [
    { required: true, message: t('system.projectGroup.validation.code'), trigger: 'blur' },
    { max: 30, message: t('system.projectGroup.validation.codeLength'), trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_-]+$/,
      message: t('system.projectGroup.validation.codePattern'),
      trigger: 'blur'
    }
  ]
}))

// 表单的提交请求
const formRequestMapping: FormRequestMapping<ProjectGroupDTO> = {
  [FormAction.CREATE]: createProjectGroup,
  [FormAction.UPDATE]: updateProjectGroup
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
  open(newFormAction: FormAction, record?: ProjectGroupPageVO) {
    openModal()
    resetFields()
    if (newFormAction === FormAction.CREATE) {
      title.value = t('system.projectGroup.createTitle')
    } else {
      title.value = t('system.projectGroup.editTitle')
      overrideProperties(formModel, record)
    }
    formAction.value = newFormAction
  }
})
</script>
