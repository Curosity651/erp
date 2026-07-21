<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="[16, 8]">
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
          <a-form-item label="仓库编码">
            <a-input
              v-model:value="formModel.warehouseCode"
              placeholder="请输入仓库编码"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="库位状态">
            <a-select
              v-model:value="formModel.generatedStatus"
              placeholder="请选择库位状态"
              allow-clear
              :options="generatedOptions"
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
import { reactive, toRaw } from 'vue'
import { Form } from 'ant-design-vue'

export interface LocationMgmtQuery {
  warehouseName?: string
  warehouseCode?: string
  /** 1=已生成 0=未生成 undefined=全部 */
  generatedStatus?: number
}

const useForm = Form.useForm

const labelCol = { md: { span: 6 } }

const props = withDefaults(defineProps<{ loading?: boolean }>(), { loading: false })

const emits = defineEmits<{ (e: 'search', params: LocationMgmtQuery): void }>()

const generatedOptions = [
  { label: '已生成', value: 1 },
  { label: '未生成', value: 0 }
]

const formModel = reactive<LocationMgmtQuery>({
  warehouseName: undefined,
  warehouseCode: undefined,
  generatedStatus: undefined
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
  name: 'LocationMgmtSearch'
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
