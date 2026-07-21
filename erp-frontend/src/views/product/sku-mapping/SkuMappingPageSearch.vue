<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="16">
        <a-col :xxl="6" :xl="7" :lg="9" :md="12" :sm="24">
          <a-form-item label="平台商品ID">
            <a-input v-model:value="formModel.platformItemId" placeholder="精确匹配" />
          </a-form-item>
        </a-col>
        <a-col :xxl="6" :xl="8" :lg="9" :md="12" :sm="24">
          <a-form-item label="ERP SKU编码">
            <a-input v-model:value="formModel.skuCode" placeholder="精确匹配" />
          </a-form-item>
        </a-col>
        <a-col
          :xxl="2"
          :xl="24"
          :lg="24"
          :md="24"
          :sm="24"
          style="display: flex; align-items: flex-start"
        >
          <div class="search-actions-wrapper">
            <search-actions :loading="props.loading" @search="search" @reset="reset" />
          </div>
        </a-col>
      </a-row>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { Form } from 'ant-design-vue'
import type { SkuMappingQO } from '@/api/product/sku-mapping/types'

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

const formModel = reactive<SkuMappingQO>({
  platformItemId: undefined,
  skuCode: undefined
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

<style scoped>
.search-actions-wrapper {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
@media (max-width: 1599px) {
  .search-actions-wrapper {
    margin-top: 4px;
  }
}
</style>
