<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="16">
        <a-col :xl="6" :md="8" :sm="12">
          <a-form-item label="SKU编码">
            <a-input v-model:value="formModel.skuCode" placeholder="请输入SKU编码" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="8" :sm="12">
          <a-form-item label="仓库">
            <warehouse-select
              v-model:value="formModel.warehouseId"
              placeholder="请选择"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="8" :sm="12">
          <a-form-item label="仓库类型">
            <a-select
              v-model:value="formModel.warehouseType"
              placeholder="全部"
              allow-clear
              :options="warehouseTypeOptions"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="8" :sm="12">
          <a-form-item label="库存状态">
            <a-select
              v-model:value="formModel.stockStatus"
              placeholder="全部"
              allow-clear
              :options="stockStatusOptions"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="8" :sm="12">
          <a-form-item :label-col="{ span: 0 }" :wrapper-col="{ span: 24 }">
            <a-checkbox v-model:checked="formModel.hasDamaged">仅显示有残品</a-checkbox>
          </a-form-item>
        </a-col>
        <a-col :xl="6" :md="8" :sm="12">
          <search-actions :loading="props.loading" @search="handleSearch" @reset="handleReset" />
        </a-col>
      </a-row>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Form } from 'ant-design-vue'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import { warehouseTypeOptions, stockStatusOptions } from '../../shared/constants'
import type { InventoryQO } from '@/api/wms/inventory/types'

defineOptions({ name: 'InventoryDetailSearch' })

const useForm = Form.useForm

const labelCol = { md: { span: 6 } }

const props = withDefaults(
  defineProps<{
    loading?: boolean
  }>(),
  { loading: false }
)

const emit = defineEmits<{
  (e: 'search', params: InventoryQO): void
}>()

const route = useRoute()

const formModel = reactive<InventoryQO>({
  regionId: undefined,
  skuCode: undefined,
  warehouseId: undefined,
  warehouseType: undefined,
  stockStatus: undefined,
  hasDamaged: undefined
})

const { resetFields } = useForm(formModel)

function handleSearch() {
  emit('search', { ...formModel })
}

function handleReset() {
  resetFields()
  handleSearch()
}

// 监听 URL 参数，支持从其他页面跳转时预填
watch(
  () => route.query,
  query => {
    if (route.path !== '/inventory/inventory-detail') return
    if (query.regionId) {
      formModel.regionId = Number(query.regionId)
    }
    if (query.warehouseId) {
      formModel.warehouseId = Number(query.warehouseId)
    }
    if (query.skuCode) {
      formModel.skuCode = query.skuCode as string
    }
    // 有参数时自动触发搜索
    if (query.regionId || query.warehouseId || query.skuCode) {
      handleSearch()
    }
  },
  { immediate: true }
)

defineExpose({ search: handleSearch, reset: handleReset })
</script>
