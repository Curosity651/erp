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
      <a-form-item label="名称" v-bind="validateInfos.name">
        <a-input v-model:value="formModel.name" placeholder="请输入项目组名称" />
      </a-form-item>
      <a-form-item label="编码" v-bind="validateInfos.code">
        <a-input
          v-model:value="formModel.code"
          placeholder="请输入项目组编码"
          :disabled="isUpdateForm"
        />
      </a-form-item>
      <a-form-item label="描述">
        <a-textarea v-model:value="formModel.description" placeholder="请输入项目组描述" />
      </a-form-item>
      <a-form-item label="状态">
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
const formRule = reactive({
  name: [
    { required: true, message: '请输入项目组名称', trigger: 'blur' },
    { max: 50, message: '项目组名称长度不能超过50个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入项目组编码', trigger: 'blur' },
    { max: 30, message: '项目组编码长度不能超过30个字符', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_-]+$/,
      message: '项目组编码只能包含字母、数字、下划线和横线',
      trigger: 'blur'
    }
  ]
})

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
      title.value = '新建项目组'
    } else {
      title.value = '编辑项目组'
      overrideProperties(formModel, record)
    }
    formAction.value = newFormAction
  }
})
</script>
