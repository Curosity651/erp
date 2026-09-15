<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="[16, 8]">
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="退货单号">
            <a-input v-model:value="formModel.returnNo" placeholder="请输入退货单号" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="平台订单号">
            <a-input
              v-model:value="formModel.platformOrderId"
              placeholder="请输入平台订单号"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="状态">
            <a-select
              v-model:value="formModel.returnStatus"
              :options="statusOptions"
              allow-clear
              placeholder="全部状态"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="SKU">
            <SkuSelectInput v-model:value="formModel.skuCode" />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="入库仓库">
            <WarehouseSelect
              v-model:value="formModel.warehouseId"
              placeholder="请选择入库仓库"
              allow-clear
              width="100%"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="退货日期">
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
import type { ReturnInboundQO } from '@/api/wms/return-inbound/types'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import SkuSelectInput from '@/components/Sku/SkuSelectInput.vue'

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
  (e: 'search', params: ReturnInboundQO): void
}>()

const formModel = reactive<ReturnInboundQO>({
  returnNo: undefined,
  platformOrderId: undefined,
  platform: undefined,
  skuCode: undefined,
  warehouseId: undefined,
  returnDateStart: undefined,
  returnDateEnd: undefined,
  returnStatus: undefined
})

const statusOptions = [
  { label: '待货主处置', value: 'PENDING_OWNER' },
  { label: '待仓库处理', value: 'PENDING_OPERATION' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已关闭', value: 'CLOSED' }
]

// 日期范围
const dateRange = ref<[string, string] | null>(null)

const { resetFields } = useForm(formModel)

// 日期范围变化
const handleDateChange = (dates: [string, string] | null) => {
  if (dates) {
    formModel.returnDateStart = dates[0]
    formModel.returnDateEnd = dates[1]
  } else {
    formModel.returnDateStart = undefined
    formModel.returnDateEnd = undefined
  }
}

const search = () => {
  emits('search', toRaw(formModel))
}

const reset = () => {
  resetFields()
  dateRange.value = null
  formModel.returnDateStart = undefined
  formModel.returnDateEnd = undefined
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
