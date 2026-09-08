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

      <a-form-item :label="t('system.organization.parent')" v-bind="validateInfos.parentId">
        <sys-organization-tree-select
          v-model:value="formModel.parentId"
          :placeholder="t('system.organization.parent')"
          :tree-data="hasRootOrganizationTree"
          tree-default-expand-all
          allow-clear
        />
      </a-form-item>

      <a-form-item :label="t('system.organization.name')" v-bind="validateInfos.name">
        <a-input v-model:value="formModel.name" :placeholder="t('system.organization.name')" />
      </a-form-item>

      <a-form-item :label="t('system.organization.sort')">
        <a-input-number
          v-model:value="formModel.sort"
          style="width: 60%"
          :placeholder="t('system.organization.sortHint')"
        />
      </a-form-item>

      <a-form-item :label="t('system.organization.remarks')" v-bind="validateInfos.remarks">
        <a-textarea
          v-model:value="formModel.remarks"
          :placeholder="t('system.organization.remarks')"
          :rows="3"
          :max-length="512"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import SysOrganizationTreeSelect from './SysOrganizationTreeSelect.vue'
import { useModal } from '@/hooks/modal'
import type { FormRequestMapping } from '@/hooks/form'
import { FormAction, useAdminForm, useFormAction, labelCol, wrapperCol } from '@/hooks/form'
import { overrideProperties } from '@/utils/bean-utils'
import type {
  SysOrganizationTree,
  SysOrganizationVO,
  SysOrganizationDTO
} from '@/api/system/organization/types'
import { createOrganization, updateOrganization } from '@/api/system/organization'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const emits = defineEmits<{
  (e: 'submit-success'): void
}>()

const props = defineProps<{
  organizationTree: SysOrganizationTree[]
}>()

const hasRootOrganizationTree = computed<SysOrganizationTree[]>(() => [
  {
    id: 0,
    key: 0,
    name: t('system.organization.root'),
    children: props.organizationTree
  } as SysOrganizationTree
])

const { title, visible, openModal, closeModal } = useModal()

const { formAction, isUpdateForm } = useFormAction()

// 表单的提交请求
const formRequestMapping: FormRequestMapping<SysOrganizationDTO> = {
  [FormAction.CREATE]: createOrganization,
  [FormAction.UPDATE]: updateOrganization
}

// 表单模型
const formModel = reactive<SysOrganizationDTO>({
  id: undefined,
  parentId: 0,
  name: '',
  sort: 1,
  remarks: ''
})

// 表单的校验规则
const formRule = reactive({
  parentId: [
    {
      required: true,
      type: 'number',
      message: t('system.organization.parentRequired')
    }
  ],
  name: [{ required: true, message: t('system.organization.nameRequired') }],
  remarks: [{ max: 512 }]
})

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
  open(newFormAction: FormAction, record?: SysOrganizationVO) {
    openModal()
    resetFields()
    formAction.value = newFormAction
    if (newFormAction === FormAction.CREATE) {
      title.value = t('system.organization.new')
    } else {
      title.value = t('system.organization.editTitle')
      overrideProperties(formModel, record)
    }
  }
})
</script>
