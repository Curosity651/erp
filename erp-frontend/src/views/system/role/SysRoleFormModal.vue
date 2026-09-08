<template>
  <a-modal
    :title="title"
    :open="visible"
    :mask-closable="false"
    :body-style="{ paddingBottom: '8px' }"
    :confirm-loading="submitLoading"
    @ok="handleSubmit"
    @cancel="handleClose"
  >
    <a-form :model="formModel" :label-col="labelCol" :wrapper-col="wrapperCol">
      <a-form-item v-if="isUpdateForm" style="display: none">
        <a-input v-model:value="formModel.id" />
      </a-form-item>

      <a-form-item :label="t('system.role.name')" v-bind="validateInfos.name">
        <a-input v-model:value="formModel.name" :placeholder="t('message.pleaseEnter')" />
      </a-form-item>

      <a-form-item :label="t('system.role.code')" v-bind="validateInfos.code">
        <a-input
          v-model:value="formModel.code"
          :disabled="isUpdateForm"
          :placeholder="t('system.role.codeHint')"
        />
      </a-form-item>

      <a-form-item :label="t('system.role.type')" v-bind="validateInfos.type">
        <dict-radio-group
          v-model:value="formModel.type"
          :disabled="isUpdateForm"
          type="button"
          dict-code="role_type"
        />
      </a-form-item>

      <a-form-item :label="t('system.role.dataPermission')" v-bind="validateInfos.scopeType">
        <a-select v-model:value="formModel.scopeType">
          <a-select-option :value="0">{{ t('system.role.all') }}</a-select-option>
          <a-select-option :value="1">{{ t('system.role.personal') }}</a-select-option>
          <a-select-option :value="2">{{ t('system.role.selfAndChildren') }}</a-select-option>
          <a-select-option :value="3">{{ t('system.role.current') }}</a-select-option>
          <a-select-option :value="4">{{ t('system.role.currentAndChildren') }}</a-select-option>
          <a-select-option :value="5">{{ t('system.role.custom') }}</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        v-if="isCustomScopeType"
        :label="t('system.role.dataScope')"
        v-bind="validateInfos.scopeResources"
      >
        <sys-organization-tree-select
          v-model:value="formModel.scopeResourceList"
          :multiple="true"
        />
      </a-form-item>

      <a-form-item :label="t('system.role.remarks')">
        <a-textarea
          v-model:value="formModel.remarks"
          :auto-size="{ minRows: 4, maxRows: 8 }"
          :placeholder="t('system.role.remarksHint')"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import SysOrganizationTreeSelect from '../organization/SysOrganizationTreeSelect.vue'
import { useModal } from '@/hooks/modal'
import { useAdminForm, useFormAction, FormAction, labelCol, wrapperCol } from '@/hooks/form'
import type { FormRequestMapping } from '@/hooks/form'
import { overrideProperties } from '@/utils/bean-utils'
import type { Rule } from 'ant-design-vue/es/form'
import { createRole, updateRole } from '@/api/system/role'
import type { SysRoleDTO, SysRolePageVO } from '@/api/system/role/types'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const emits = defineEmits<{
  (e: 'submit-success'): void
}>()

const { title, visible, openModal, closeModal } = useModal()

const { formAction, isUpdateForm } = useFormAction()

/** 校验密码 */
const validateCode = async (_rule: Rule, value: string) => {
  if (value === '') {
    return Promise.reject(t('system.role.codeRequired'))
  } else {
    if (value.indexOf('ROLE_') !== 0) {
      return Promise.reject(t('system.role.codeHint'))
    }
    return Promise.resolve()
  }
}

// 表单模型
const formModel = reactive<SysRoleDTO & { scopeResourceList?: number[] }>({
  id: undefined,
  name: '',
  code: '',
  type: 1,
  scopeType: 1,
  scopeResources: undefined,
  remarks: '',

  scopeResourceList: []
})

// 是否是自定义的数据权限类型
const isCustomScopeType = computed(() => formModel.scopeType === 5)

// 表单的校验规则
const formRule = reactive({
  name: [{ required: true, message: t('system.role.nameRequired') }],
  code: [{ validator: validateCode }],
  type: [{ required: true, message: t('system.role.typeRequired') }],
  scopeType: [{ required: true, message: t('system.role.permissionRequired') }],
  scopeResources: [{ required: isCustomScopeType, message: t('system.role.scopeRequired') }]
})

// 表单的提交请求
const formRequestMapping: FormRequestMapping<SysRoleDTO> = {
  [FormAction.CREATE]: createRole,
  [FormAction.UPDATE]: updateRole
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
  model.scopeResources = model.scopeResourceList?.join(',')
  delete model.scopeResourceList
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
  open(newFormAction: FormAction, record?: SysRolePageVO) {
    openModal()
    resetFields()
    if (newFormAction === FormAction.CREATE) {
      title.value = t('system.role.newRole')
    } else {
      title.value = t('system.role.editRole')
      overrideProperties(formModel, record)
      formModel.scopeResourceList = record?.scopeResources?.split(',').map(Number)
    }
    formAction.value = newFormAction
  }
})
</script>
