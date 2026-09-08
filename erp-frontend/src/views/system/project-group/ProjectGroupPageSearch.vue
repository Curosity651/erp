<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="16">
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item :label="t('system.projectGroup.name')">
            <a-input
              v-model:value="formModel.name"
              :placeholder="t('system.projectGroup.namePlaceholder')"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item :label="t('system.projectGroup.code')">
            <a-input
              v-model:value="formModel.code"
              :placeholder="t('system.projectGroup.codePlaceholder')"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item :label="t('system.projectGroup.status')">
            <dict-select
              v-model:value="formModel.status"
              dict-code="enable_status"
              :placeholder="t('system.projectGroup.statusPlaceholder')"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <search-actions :loading="props.loading" @search="search" @reset="reset" />
        </a-col>
      </a-row>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { Form } from 'ant-design-vue'
import type { ProjectGroupQO } from '@/api/system/project-group/types'
import { DictSelect } from '@/components/Dict'
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

const formModel = reactive<ProjectGroupQO>({
  name: undefined,
  code: undefined,
  status: undefined
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
