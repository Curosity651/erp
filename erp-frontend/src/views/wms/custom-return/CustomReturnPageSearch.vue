<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="[16, 8]">
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="退货单号">
            <a-input v-model:value="formModel.inboundNo" placeholder="请输入退货单号" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="退货类型">
            <a-select
              v-model:value="formModel.returnType"
              placeholder="请选择退货类型"
              allow-clear
            >
              <a-select-option
                v-for="(label, value) in CUSTOM_RETURN_TYPE_MAP"
                :key="value"
                :value="value"
              >
                {{ label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="SKU编码">
            <a-input v-model:value="formModel.skuCode" placeholder="请输入SKU编码" allow-clear />
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
          <a-form-item label="单据状态">
            <a-select
              v-model:value="formModel.orderStatus"
              placeholder="请选择单据状态"
              allow-clear
            >
              <a-select-option
                v-for="(label, value) in InboundStatusMap"
                :key="value"
                :value="value"
              >
                {{ label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="入库日期">
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
import { SearchActions } from '@/components/Search'
import type { CustomReturnQO } from '@/api/wms/custom-return/types'
import { CUSTOM_RETURN_TYPE_MAP } from '@/api/wms/custom-return/types'
import { InboundStatusMap } from '@/api/wms/purchase-inbound/types'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'

const useForm = Form.useForm

const labelCol = { md: { span: 6 } }

const props = withDefaults(
  defineProps<{
    loading?: boolean
  }>(),
  { loading: false }
)

const emits = defineEmits<{
  (e: 'search', params: CustomReturnQO): void
}>()

const formModel = reactive<CustomReturnQO>({
  inboundNo: undefined,
  returnType: undefined,
  skuCode: undefined,
  warehouseId: undefined,
  orderStatus: undefined,
  inboundDateStart: undefined,
  inboundDateEnd: undefined
})

const dateRange = ref<[string, string] | null>(null)

const { resetFields } = useForm(formModel)

const handleDateChange = (dates: [string, string] | null) => {
  if (dates) {
    formModel.inboundDateStart = dates[0]
    formModel.inboundDateEnd = dates[1]
  } else {
    formModel.inboundDateStart = undefined
    formModel.inboundDateEnd = undefined
  }
}

const search = () => {
  emits('search', toRaw(formModel))
}

const reset = () => {
  resetFields()
  dateRange.value = null
  formModel.inboundDateStart = undefined
  formModel.inboundDateEnd = undefined
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
