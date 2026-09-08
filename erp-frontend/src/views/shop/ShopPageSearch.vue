<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="16">
        <a-col :xl="6" :md="8" :sm="24">
          <a-form-item :label="t('shop.platform')">
            <PlatformSelect
              v-model:value="formModel.platform"
              style="width: 100%"
              :placeholder="t('shop.all')"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="8" :sm="24">
          <a-form-item :label="t('shop.statusLabel')">
            <a-select
              v-model:value="formModel.status"
              allow-clear
              :placeholder="t('shop.all')"
              style="width: 100%"
            >
              <a-select-option :value="1">{{ t('shop.enable') }}</a-select-option>
              <a-select-option :value="0">{{ t('shop.disable') }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :xl="8" :md="8" :sm="24">
          <a-form-item :label="t('shop.keyword')">
            <a-input
              v-model:value="formModel.keyword"
              allow-clear
              :placeholder="t('shop.keywordPlaceholder')"
              @press-enter="search"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="4" :md="24" :sm="24">
          <search-actions :loading="props.loading" @search="search" @reset="reset" />
        </a-col>
      </a-row>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { Form } from 'ant-design-vue'
import { PlatformSelect } from '@/components/Platform'
import { useI18n } from 'vue-i18n'
// 已不使用原 scaffold 的 ShopQO，这里手动声明查询模型类型
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

const formModel = reactive<{ platform?: string; status?: number; keyword?: string }>({
  platform: undefined,
  status: undefined,
  keyword: undefined
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
