<template>
  <a-modal
    :title="title"
    :open="visible"
    :mask-closable="false"
    :body-style="{ paddingBottom: '8px' }"
    :confirm-loading="submitLoading"
    :width="650"
    @ok="handleSubmit"
    @cancel="handleClose"
  >
    <a-form :model="formModel" :label-col="labelCol" :wrapper-col="wrapperCol">
      <a-row>
        <a-col :xs="24" :sm="24" :md="12">
          <!-- userId 由 formModel 承载（open() 时 overrideProperties 写入、提交时随 formModel 一起发送），
               无需隐藏输入框占位。此前的 <a-form-item style="display:none"> 因 a-form-item 不透传 style，
               实际以 block 渲染并带 24px 下边距，导致更新态左列比右列整体下移 24px。 -->
          <a-form-item :label="t('system.user.username')" v-bind="validateInfos.username">
            <a-input v-model:value="formModel.username" :placeholder="t('message.pleaseEnter')" />
          </a-form-item>

          <a-form-item v-if="isCreateForm" :label="t('system.user.password')" v-bind="validateInfos.pass">
            <a-input-password v-model:value="formModel.pass" :placeholder="t('message.pleaseEnter')" />
          </a-form-item>

          <a-form-item :label="t('system.user.nickname')" v-bind="validateInfos.nickname">
            <a-input v-model:value="formModel.nickname" :placeholder="t('message.pleaseEnter')" />
          </a-form-item>

          <a-form-item :label="t('system.user.organization')">
            <sys-organization-tree-select
              v-model:value="formModel.organizationId"
              :placeholder="t('common.select')"
            />
          </a-form-item>

          <a-form-item :label="t('system.user.status')">
            <dict-radio-group v-model:value="formModel.status" dict-code="user_status" />
          </a-form-item>
        </a-col>

        <a-col :xs="24" :sm="24" :md="12">
          <a-form-item :label="t('system.user.gender')">
            <dict-select v-model:value="formModel.gender" dict-code="gender" />
          </a-form-item>

          <a-form-item :label="t('system.user.phone')">
            <a-input v-model:value="formModel.phoneNumber" :placeholder="t('message.pleaseEnter')" />
          </a-form-item>

          <a-form-item :label="t('system.user.email')">
            <a-input v-model:value="formModel.email" :placeholder="t('message.pleaseEnter')" />
          </a-form-item>

          <a-form-item v-if="isCreateForm" :label="t('system.user.role')">
            <sys-role-select
              v-model:value="formModel.roleCodes"
              mode="multiple"
              allow-clear
              :placeholder="t('common.select')"
            />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import SysRoleSelect from '../role/SysRoleSelect.vue'
import SysOrganizationTreeSelect from '../organization/SysOrganizationTreeSelect.vue'
import type { SysUserDTO, SysUserPageVO } from '@/api/system/user/types'
import { overrideProperties } from '@/utils/bean-utils'
import { createUser, updateUser } from '@/api/system/user'
import { passEncrypt } from '@/utils/password-utils'
import { useAdminForm, useFormAction, FormAction, labelCol, wrapperCol } from '@/hooks/form'
import type { FormRequestMapping } from '@/hooks/form'
import { useModal } from '@/hooks/modal'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const emits = defineEmits<{
  (e: 'submit-success'): void
}>()

const { title, visible, openModal, closeModal } = useModal()

const { formAction, isCreateForm, isUpdateForm } = useFormAction()

// 表单模型
const formModel = reactive<SysUserDTO>({
  userId: undefined,
  username: '',
  pass: '',
  nickname: '',
  organizationId: 0,
  status: 1,
  gender: 1,
  phoneNumber: '',
  email: '',
  roleCodes: []
})

// 表单校验规则
const formRule = reactive({
  username: [{ required: true, message: t('system.user.usernameRequired') }],
  pass: [{ required: isCreateForm, message: t('system.user.passwordRequired') }],
  nickname: [{ required: true, message: t('system.user.nicknameRequired') }]
})

// 表单的提交请求
const formRequestMapping: FormRequestMapping<SysUserDTO> = {
  [FormAction.CREATE]: createUser,
  [FormAction.UPDATE]: updateUser
}

const { submitLoading, validateAndSubmit, resetFields, validateInfos } = useAdminForm(
  formAction,
  formRequestMapping,
  formModel,
  formRule
)

/* 表单提交处理 */
const handleSubmit = () => {
  validateAndSubmit(
    {
      ...formModel,
      pass: passEncrypt(formModel.pass)
    },
    {
      onSuccess: () => {
        closeModal()
        emits('submit-success')
      }
    }
  )
}

/* 弹窗关闭方法 */
const handleClose = () => {
  closeModal()
  submitLoading.value = false
}

defineExpose({
  open(newFormAction: FormAction, record?: SysUserPageVO) {
    openModal()
    resetFields()
    if (newFormAction === FormAction.CREATE) {
      title.value = t('system.user.newUser')
    } else {
      title.value = t('system.user.editUser')
      overrideProperties(formModel, record)
    }
    formAction.value = newFormAction
  }
})
</script>
