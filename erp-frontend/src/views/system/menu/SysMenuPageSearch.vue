<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="16">
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item :label="t('system.menu.id')">
            <a-input v-model:value="formModel.id" :placeholder="t('message.pleaseEnter')" />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item :label="t('system.menu.name')">
            <a-input v-model:value="searchTitle" :placeholder="t('message.pleaseEnter')" />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item :label="t('system.menu.path')">
            <a-input v-model:value="formModel.path" :placeholder="t('message.pleaseEnter')" />
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
import type { SysMenuQO } from '@/api/system/menu/types'
import { useVModel } from '@vueuse/core'
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
const useForm = Form.useForm

// 表单 label 全局配置
const labelCol = { md: { span: 6 } }

const props = withDefaults(
  defineProps<{
    loading?: boolean
    searchTitle?: string
  }>(),
  { loading: false, searchTitle: '' }
)

const emits = defineEmits<{
  (e: 'search', params: Record<string, any>): void
  (e: 'update:searchTitle', searchTitle: string): void
}>()

// searchTitle 变动的同时，会自动调用 emits 事件 update:searchTitle
const searchTitle = useVModel(props, 'searchTitle')

const formModel = reactive<SysMenuQO>({
  id: undefined,
  path: ''
})

const { resetFields } = useForm(formModel)

const search = () => {
  emits('search', toRaw(formModel))
}

const reset = () => {
  searchTitle.value = ''
  resetFields()
  search()
}
</script>
