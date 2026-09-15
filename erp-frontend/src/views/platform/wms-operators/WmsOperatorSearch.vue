<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="[16, 8]">
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item :label="t('platform.operator.code')">
            <a-input v-model:value="formModel.tenantCode" :placeholder="t('platform.operator.codePlaceholder')" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item :label="t('platform.operator.name')">
            <a-input v-model:value="formModel.tenantName" :placeholder="t('platform.operator.namePlaceholder')" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item :label="t('platform.common.status')">
            <a-select
              v-model:value="formModel.status"
              :placeholder="t('platform.operator.statusPlaceholder')"
              allow-clear
              :options="statusOptions"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <search-actions :loading="props.loading" @search="search" @reset="reset" />
        </a-col>
      </a-row>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { computed, reactive, toRaw } from 'vue'
import { useI18n } from 'vue-i18n'
import { Form } from 'ant-design-vue'
import type { TenantPageParam } from '@/api/tenant/types'

const useForm = Form.useForm
const { t } = useI18n()
const labelCol = { md: { span: 6 } }

const props = withDefaults(defineProps<{ loading?: boolean }>(), { loading: false })

const emits = defineEmits<{
  (e: 'search', params: TenantPageParam): void
}>()

const statusOptions = computed(() => [
  { label: t('platform.operator.enabled'), value: 1 },
  { label: t('platform.operator.disabled'), value: 0 }
])

const formModel = reactive<TenantPageParam>({
  tenantCode: undefined,
  tenantName: undefined,
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

<script lang="ts">
export default {
  name: 'WmsOperatorSearch'
}
</script>

<style scoped>
:deep(.ant-form-item) {
  margin-bottom: 16px;
}
:deep(.ant-form-item-label) {
  font-weight: 500;
}
</style>
