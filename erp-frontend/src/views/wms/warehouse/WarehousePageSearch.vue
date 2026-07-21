<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="[16, 8]">
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="仓库编码">
            <a-input
              v-model:value="formModel.warehouseCode"
              placeholder="请输入仓库编码"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="仓库名称">
            <a-input
              v-model:value="formModel.warehouseName"
              placeholder="请输入仓库名称"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="仓库类型">
            <a-select
              v-model:value="formModel.warehouseType"
              placeholder="请选择仓库类型"
              allow-clear
            >
              <a-select-option value="OWN">自有仓</a-select-option>
              <a-select-option value="FBO">FBO仓</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="状态">
            <dict-select
              v-model:value="formModel.status"
              dict-code="enable_status"
              placeholder="请选择状态"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="所属区域">
            <region-select v-model:value="formModel.regionId" placeholder="全部区域" />
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
import { Form } from 'ant-design-vue'
import type { WarehouseQO } from '@/api/wms/warehouse/types'
import { DictSelect } from '@/components/Dict'
import RegionSelect from '@/components/Lov/RegionSelect.vue'
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

const formModel = reactive<WarehouseQO>({
  warehouseCode: undefined,
  warehouseName: undefined,
  warehouseType: undefined,
  status: undefined,
  regionId: undefined
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
:deep(.ant-form-item) {
  margin-bottom: 16px;
}

:deep(.ant-form-item-label) {
  font-weight: 500;
}
</style>
