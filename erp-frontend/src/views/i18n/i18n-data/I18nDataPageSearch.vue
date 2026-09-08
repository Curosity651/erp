<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="16">
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item :label="t('i18nAdmin.code')">
            <a-input v-model:value="formModel.code" :placeholder="t('message.pleaseEnter')" />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item :label="t('i18nAdmin.textValue')">
            <a-input v-model:value="formModel.message" :placeholder="t('message.pleaseEnter')" />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item :label="t('i18nAdmin.languageTag')">
            <a-input
              v-model:value="formModel.languageTag"
              :placeholder="t('message.pleaseEnter')"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <SearchActions :loading="props.loading" @search="search" @reset="reset" />
        </a-col>
      </a-row>
    </a-form>
  </a-card>
</template>

<script lang="ts" setup>
import type { I18nDataQO } from '@/api/i18n/types'
import { Form } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
const { useForm } = Form
const { t } = useI18n()

const labelCol = { md: { span: 6 } }

const props = withDefaults(
  defineProps<{
    loading?: boolean
  }>(),
  { loading: false }
)

const emits = defineEmits<{
  (e: 'search', params: Record<string, any>): void
}>()

const formModel = reactive<I18nDataQO>({
  code: '',
  message: '',
  languageTag: ''
})

const { resetFields } = useForm(formModel)

const search = () => {
  emits('search', toRaw(formModel))
}

const reset = () => {
  resetFields()
  search()
}
</script>
