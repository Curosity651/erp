<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="16">
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item :label="t('product.supplier.code')">
            <a-input
              v-model:value="formModel.supplierCode"
              :placeholder="t('product.supplier.codePlaceholder')"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item :label="t('product.supplier.name')">
            <a-input
              v-model:value="formModel.name"
              :placeholder="t('product.supplier.namePlaceholder')"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item :label="t('product.supplier.city')">
            <a-input
              v-model:value="formModel.city"
              :placeholder="t('product.supplier.cityPlaceholder')"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item :label="t('product.supplier.status')">
            <dict-select
              v-model:value="formModel.status"
              dict-code="enable_status"
              :placeholder="t('product.supplier.statusPlaceholder')"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item :label="t('product.supplier.businessContact')">
            <a-input
              v-model:value="formModel.businessContactName"
              :placeholder="t('product.supplier.contactPlaceholder')"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item :label="t('product.supplier.phone')">
            <a-input
              v-model:value="formModel.businessContactPhone"
              :placeholder="t('product.supplier.phonePlaceholder')"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24" class="search-actions-col">
          <div class="search-actions-inline">
            <search-actions :loading="props.loading" @search="search" @reset="reset" />
          </div>
        </a-col>
      </a-row>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { Form } from 'ant-design-vue'
import type { SupplierQO } from '@/api/product/supplier/types'
import { DictSelect } from '@/components/Dict'
import { SearchActions } from '@/components/Search'
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

const formModel = reactive<SupplierQO>({
  supplierCode: undefined,
  name: undefined,
  city: undefined,
  businessContactName: undefined,
  businessContactPhone: undefined,
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
