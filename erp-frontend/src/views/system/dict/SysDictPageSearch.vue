<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="16">
        <a-col :md="8" :sm="24">
          <a-form-item :label="t('system.dict.code')">
            <a-input v-model:value="formModel.code" :placeholder="t('message.pleaseEnter')" />
          </a-form-item>
        </a-col>
        <a-col :md="8" :sm="24">
          <a-form-item :label="t('system.dict.title')">
            <a-input v-model:value="formModel.title" :placeholder="t('message.pleaseEnter')" />
          </a-form-item>
        </a-col>
        <a-col :xl="8" :md="12" :sm="24">
          <search-actions :loading="props.loading" @search="search" @reset="reset" />
        </a-col>
      </a-row>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { Form } from 'ant-design-vue'
import type { SysDictQO } from '@/api/system/dict/types'
import { useI18n } from 'vue-i18n'
const useForm = Form.useForm
const { t } = useI18n()

// 表单 label 全局配置
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

// const [searchCollapsed, toggleSearchCollapsed] = useToggle(true)

const formModel = reactive<SysDictQO>({
  title: '',
  code: ''
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
