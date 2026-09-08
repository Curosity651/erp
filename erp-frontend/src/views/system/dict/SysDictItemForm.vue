<template>
  <a-form :model="formModel" :label-col="labelCol" :wrapper-col="wrapperCol">
    <a-form-item v-if="isUpdateForm" style="display: none">
      <a-input v-model:value="formModel.id" />
    </a-form-item>

    <a-form-item :label="t('system.dict.code')" v-bind="validateInfos.dictCode">
      <a-input v-model:value="formModel.dictCode" :disabled="true" />
    </a-form-item>

    <a-form-item :label="t('system.dict.textValue')" v-bind="validateInfos.name">
      <a-input v-model:value="formModel.name" :placeholder="t('message.pleaseEnter')" />
    </a-form-item>

    <a-form-item :label="t('system.dict.dataValue')" v-bind="validateInfos.value">
      <a-input
        v-model:value="formModel.value"
        :placeholder="t('system.dict.dataValuePlaceholder')"
        :disabled="isUpdateForm"
      />
    </a-form-item>

    <a-form-item :label="t('system.dict.attributes')">
      <dic-item-attributes-editor v-model:value="formModel.attributes" />
    </a-form-item>

    <a-form-item v-bind="validateInfos.sort">
      <template #label>
        <span>
          {{ t('system.dict.sort') }}
          <a-tooltip :title="t('system.dict.sortTip')"> <exclamation-circle-outlined /> </a-tooltip>
        </span>
      </template>
      <a-input-number
        v-model:value="formModel.sort"
        :placeholder="t('system.dict.sortPlaceholder')"
        :min="0"
        style="width: 70%"
      />
    </a-form-item>

    <a-form-item :label="t('common.remarks')">
      <a-textarea v-model:value="formModel.remarks" :auto-size="{ minRows: 3, maxRows: 5 }" />
    </a-form-item>

    <a-form-item :wrapper-col="{ offset: 10 }">
      <a-button type="primary" :loading="submitLoading" @click="handleSubmit">{{
        t('system.dict.submit')
      }}</a-button>
      <a-button style="margin-left: 8px" @click="showTable">{{ t('action.cancel') }}</a-button>
    </a-form-item>
  </a-form>
</template>

<script setup lang="ts">
import { useAdminForm, useFormAction, FormAction } from '@/hooks/form'
import type { FormRequestMapping } from '@/hooks/form'
import { createDictItem, updateDictItem } from '@/api/system/dict'
import type { DictItemAttributes, SysDictItemDTO, SysDictItemPageVO } from '@/api/system/dict/types'
import { DictItemStatus } from '@/api/system/dict/types'
import { overrideProperties } from '@/utils/bean-utils'
import type { ColProps } from 'ant-design-vue'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import DicItemAttributesEditor from '@/views/system/dict/DicItemAttributesEditor.vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const labelCol: ColProps = {
  sm: { span: 24 },
  md: { span: 6 }
}
const wrapperCol: ColProps = {
  sm: { span: 24 },
  md: { span: 12 }
}

const emits = defineEmits<{
  (e: 'submit-success'): void
  (e: 'show-table'): void
}>()

const { formAction, isUpdateForm } = useFormAction()

// 表单模型
const formModel = reactive<SysDictItemDTO & { attributes: DictItemAttributes }>({
  id: undefined,
  dictCode: '',
  value: '',
  name: '',
  status: DictItemStatus.ENABLED,
  attributes: {},
  sort: 1,
  remarks: ''
})

// 表单的校验规则
const formRule = computed(() => ({
  dictCode: [{ required: true, message: t('system.dict.validation.codeRequired') }],
  name: [{ required: true, message: t('system.dict.validation.textValue') }],
  value: [{ required: true, message: t('system.dict.validation.dataValue') }],
  sort: [
    { required: true, message: t('system.dict.validation.sort') },
    { type: 'number', min: 0, message: t('system.dict.validation.sortMin') }
  ]
}))

// 表单的提交请求
const formRequestMapping: FormRequestMapping<SysDictItemDTO> = {
  [FormAction.CREATE]: createDictItem,
  [FormAction.UPDATE]: updateDictItem
}

const { submitLoading, validateAndSubmit, resetFields, validateInfos } = useAdminForm(
  formAction,
  formRequestMapping,
  formModel,
  formRule
)

/* 表单提交处理 */
const handleSubmit = () => {
  validateAndSubmit(toRaw(formModel), {
    onSuccess: () => {
      emits('submit-success')
      showTable()
    }
  })
}

const showTable = () => {
  emits('show-table')
}

defineExpose({
  create(dictCode: string) {
    formAction.value = FormAction.CREATE
    resetFields()
    formModel.dictCode = dictCode
  },
  update(record?: SysDictItemPageVO) {
    formAction.value = FormAction.UPDATE
    resetFields()
    overrideProperties(formModel, record)
  }
})
</script>
