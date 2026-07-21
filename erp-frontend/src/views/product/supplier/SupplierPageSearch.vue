<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="16">
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item label="供应商编码">
            <a-input v-model:value="formModel.supplierCode" placeholder="请输入供应商编码" />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item label="供应商名称">
            <a-input v-model:value="formModel.name" placeholder="请输入供应商名称" />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item label="所在城市">
            <a-input v-model:value="formModel.city" placeholder="请输入城市" />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item label="状态">
            <dict-select
              v-model:value="formModel.status"
              dict-code="enable_status"
              placeholder="请选择状态"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item label="业务联系人">
            <a-input v-model:value="formModel.businessContactName" placeholder="请输入业务联系人" />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="12" :sm="24">
          <a-form-item label="联系电话">
            <a-input v-model:value="formModel.businessContactPhone" placeholder="请输入联系电话" />
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
