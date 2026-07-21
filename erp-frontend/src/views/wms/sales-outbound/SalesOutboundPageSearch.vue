<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="[16, 8]">
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="出库单号">
            <a-input
              v-model:value="formModel.outboundNo"
              placeholder="请输入出库单号"
              allow-clear
            />
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
          <a-form-item label="出库仓库">
            <WarehouseSelect
              v-model:value="formModel.warehouseId"
              placeholder="请选择出库仓库"
              allow-clear
              width="100%"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="单据状态">
            <a-select
              v-model:value="formModel.orderStatus"
              placeholder="请选择单据状态"
              allow-clear
            >
              <a-select-option
                v-for="(label, value) in OutboundOrderStatusMap"
                :key="value"
                :value="value"
              >
                {{ label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="出库日期">
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
import type { SalesOutboundQO } from '@/api/wms/sales-outbound/types'
import { OutboundOrderStatusMap } from '@/api/wms/sales-outbound/types'
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
  (e: 'search', params: SalesOutboundQO): void
}>()

const formModel = reactive<SalesOutboundQO>({
  outboundNo: undefined,
  platformOrderId: undefined,
  warehouseId: undefined,
  orderStatus: undefined,
  outboundDateStart: undefined,
  outboundDateEnd: undefined
})

// 日期范围
const dateRange = ref<[string, string] | null>(null)

const { resetFields } = useForm(formModel)

// 日期范围变化
const handleDateChange = (dates: [string, string] | null) => {
  if (dates) {
    formModel.outboundDateStart = dates[0]
    formModel.outboundDateEnd = dates[1]
  } else {
    formModel.outboundDateStart = undefined
    formModel.outboundDateEnd = undefined
  }
}

const search = () => {
  emits('search', toRaw(formModel))
}

const reset = () => {
  resetFields()
  dateRange.value = null
  formModel.outboundDateStart = undefined
  formModel.outboundDateEnd = undefined
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
