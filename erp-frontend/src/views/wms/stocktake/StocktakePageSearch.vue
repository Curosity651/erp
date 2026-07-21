<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="16">
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="盘点单号">
            <a-input
              v-model:value="formModel.stocktakeNo"
              placeholder="请输入盘点单号"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="盘点仓库">
            <WarehouseSelect
              v-model:value="formModel.warehouseId"
              placeholder="请选择盘点仓库"
              allow-clear
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="盘点日期">
            <a-range-picker
              v-model:value="dateRange"
              style="width: 100%"
              value-format="YYYY-MM-DD"
              @change="handleDateChange"
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
import { ref, reactive, toRaw } from 'vue'
import { Form } from 'ant-design-vue'
import type { StocktakeQO } from '@/api/wms/stocktake/types'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'

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
  (e: 'search', params: StocktakeQO): void
}>()

const formModel = reactive<StocktakeQO>({
  stocktakeNo: undefined,
  warehouseId: undefined,
  orderStatus: undefined,
  stocktakeDateStart: undefined,
  stocktakeDateEnd: undefined
})

// 日期范围
const dateRange = ref<[string, string] | null>(null)

const { resetFields } = useForm(formModel)

// 日期范围变化
const handleDateChange = (dates: [string, string] | null) => {
  if (dates) {
    formModel.stocktakeDateStart = dates[0]
    formModel.stocktakeDateEnd = dates[1]
  } else {
    formModel.stocktakeDateStart = undefined
    formModel.stocktakeDateEnd = undefined
  }
}

const search = () => {
  emits('search', toRaw(formModel))
}

const reset = () => {
  resetFields()
  dateRange.value = null
  formModel.stocktakeDateStart = undefined
  formModel.stocktakeDateEnd = undefined
  search()
}
</script>
