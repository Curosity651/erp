<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="[16, 8]">
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="物流单号">
            <a-input
              v-model:value="formModel.shippingNo"
              placeholder="请输入物流单号"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="物流商">
            <logistics-provider-select v-model:value="formModel.providerId" />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="目标区域">
            <region-select v-model:value="formModel.targetRegionId" allow-clear style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="SKU编码">
            <a-input
              v-model:value="formModel.skuCode"
              placeholder="请输入SKU编码"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="物流状态">
            <a-select
              v-model:value="formModel.shippingStatus"
              placeholder="请选择物流状态"
              allow-clear
            >
              <a-select-option
                v-for="(label, value) in ShippingStatusMap"
                :key="value"
                :value="value"
              >
                {{ label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="付款状态">
            <a-select
              v-model:value="formModel.paymentStatus"
              placeholder="请选择付款状态"
              allow-clear
            >
              <a-select-option :value="0">未付</a-select-option>
              <a-select-option :value="1">已付</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="物流方式">
            <a-select
              v-model:value="formModel.shippingMethod"
              placeholder="请选择物流方式"
              allow-clear
            >
              <a-select-option
                v-for="(label, value) in ShippingMethodMap"
                :key="value"
                :value="value"
              >
                {{ label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="物流线路">
            <a-select
              v-model:value="formModel.shippingRoute"
              placeholder="请选择物流线路"
              allow-clear
            >
              <a-select-option
                v-for="(label, value) in ShippingRouteMap"
                :key="value"
                :value="value"
              >
                {{ label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="发货日期">
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
import type { ShippingOrderQO } from '@/api/wms/shipping-order/types'
import {
  ShippingStatusMap,
  ShippingMethodMap,
  ShippingRouteMap
} from '@/api/wms/shipping-order/types'
import LogisticsProviderSelect from '../../../components/Lov/LogisticsProviderSelect.vue'
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
  (e: 'search', params: ShippingOrderQO): void
}>()

const formModel = reactive<ShippingOrderQO>({
  shippingNo: undefined,
  providerId: undefined,
  targetRegionId: undefined,
  skuCode: undefined,
  shippingStatus: undefined,
  paymentStatus: undefined,
  shippingMethod: undefined,
  shippingRoute: undefined,
  shippingDateStart: undefined,
  shippingDateEnd: undefined
})

// 日期范围
const dateRange = ref<[string, string] | null>(null)

const { resetFields } = useForm(formModel)

// 日期范围变化
const handleDateChange = (dates: [string, string] | null) => {
  if (dates) {
    formModel.shippingDateStart = dates[0]
    formModel.shippingDateEnd = dates[1]
  } else {
    formModel.shippingDateStart = undefined
    formModel.shippingDateEnd = undefined
  }
}

const search = () => {
  emits('search', toRaw(formModel))
}

const reset = () => {
  resetFields()
  dateRange.value = null
  formModel.shippingDateStart = undefined
  formModel.shippingDateEnd = undefined
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
