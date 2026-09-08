<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="16">
        <a-col :md="8" :sm="24">
          <a-form-item :label="t('system.user.username')">
            <a-input v-model:value="formModel.username" :placeholder="t('message.pleaseEnter')" />
          </a-form-item>
        </a-col>
        <a-col :md="8" :sm="24">
          <a-form-item :label="t('system.user.status')">
            <dict-select
              v-model:value="formModel.status"
              dict-code="user_status"
              allow-clear
              :placeholder="t('message.pleaseEnter')"
            />
          </a-form-item>
        </a-col>
        <template v-if="!searchCollapsed">
          <a-col :md="8" :sm="24">
            <a-form-item :label="t('system.user.nickname')">
              <a-input v-model:value="formModel.nickname" :placeholder="t('message.pleaseEnter')" />
            </a-form-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-item :label="t('system.user.email')">
              <a-input v-model:value="formModel.email" :placeholder="t('message.pleaseEnter')" />
            </a-form-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-item :label="t('system.user.phone')">
              <a-input-number
                v-model:value="formModel.phoneNumber"
                :placeholder="t('message.pleaseEnter')"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </template>
        <a-col :xl="8" :md="12" :sm="24">
          <search-actions
            v-model:collapsed="searchCollapsed"
            :collapsible="true"
            :loading="props.loading"
            @search="search"
            @reset="reset"
          />
        </a-col>
      </a-row>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { DictSelect } from '@/components/Dict'
import { Form } from 'ant-design-vue'
import type { SysUserQO } from '@/api/system/user/types'
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
const useForm = Form.useForm

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

const searchCollapsed = ref(true)

const formModel = reactive<SysUserQO>({
  username: '',
  nickname: '',
  status: undefined,
  email: '',
  phoneNumber: ''
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
