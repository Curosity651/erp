<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" :label-col="labelCol">
      <a-row :gutter="[16, 8]">
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="采购单号">
            <a-input v-model:value="formModel.orderNo" placeholder="请输入采购单号" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="供应商">
            <supplier-select v-model:value="supplierCode" @change="handleSupplierChange" />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="订单状态">
            <a-select
              v-model:value="formModel.orderStatus"
              placeholder="请选择订单状态"
              allow-clear
              :options="orderStatusOptions"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="发货状态">
            <a-select
              v-model:value="formModel.shippingStatus"
              placeholder="请选择发货状态"
              allow-clear
              :options="shippingStatusOptions"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="入库状态">
            <a-select
              v-model:value="formModel.receivingStatus"
              placeholder="请选择入库状态"
              allow-clear
              :options="receivingStatusOptions"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="首付款状态">
            <a-select
              v-model:value="formModel.prepayStatus"
              placeholder="请选择首付款状态"
              allow-clear
              :options="paymentStatusOptions"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="尾款状态">
            <a-select
              v-model:value="formModel.balanceStatus"
              placeholder="请选择尾款状态"
              allow-clear
              :options="paymentStatusOptions"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="下单日期">
            <a-range-picker
              v-model:value="orderDateRange"
              style="width: 100%"
              value-format="YYYY-MM-DD"
              @change="handleDateRangeChange"
            />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="SKU编码">
            <a-input v-model:value="formModel.skuCode" placeholder="请输入SKU编码" allow-clear />
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
import type {
  PurchaseOrderQO,
  PurchaseOrderStatus,
  PurchaseShippingStatus,
  PurchaseReceivingStatus
} from '@/api/wms/purchase-order/types'
import type { SupplierPageVO } from '@/api/product/supplier/types'
import { SupplierSelect } from '@/components/Lov'

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
  (e: 'search', params: PurchaseOrderQO): void
}>()

// 订单状态选项
const orderStatusOptions: { label: string; value: PurchaseOrderStatus }[] = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '生产中', value: 'IN_PRODUCTION' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

// 发货状态选项
const shippingStatusOptions: { label: string; value: PurchaseShippingStatus }[] = [
  { label: '未发货', value: 'NOT_SHIPPED' },
  { label: '部分发货', value: 'PARTIAL_SHIPPED' },
  { label: '全部发货', value: 'ALL_SHIPPED' }
]

// 入库状态选项
const receivingStatusOptions: { label: string; value: PurchaseReceivingStatus }[] = [
  { label: '未入库', value: 'NOT_RECEIVED' },
  { label: '部分入库', value: 'PARTIAL_RECEIVED' },
  { label: '全部入库', value: 'ALL_RECEIVED' }
]

// 付款状态选项
const paymentStatusOptions = [
  { label: '未付款', value: 0 },
  { label: '已付款', value: 1 }
]

const formModel = reactive<PurchaseOrderQO>({
  orderNo: undefined,
  supplierId: undefined,
  orderStatus: undefined,
  shippingStatus: undefined,
  receivingStatus: undefined,
  prepayStatus: undefined,
  balanceStatus: undefined,
  orderDateStart: undefined,
  orderDateEnd: undefined,
  skuCode: undefined
})

// 供应商编码（用于显示）
const supplierCode = ref<string>()

// 日期范围
const orderDateRange = ref<[string, string]>()

const { resetFields } = useForm(formModel)

// 处理供应商选择变化
const handleSupplierChange = (_value?: string, option?: SupplierPageVO) => {
  formModel.supplierId = option?.id
}

// 处理日期范围变化
const handleDateRangeChange = (dates: [string, string] | null) => {
  if (dates) {
    formModel.orderDateStart = dates[0]
    formModel.orderDateEnd = dates[1]
  } else {
    formModel.orderDateStart = undefined
    formModel.orderDateEnd = undefined
  }
}

const search = () => {
  emits('search', toRaw(formModel))
}

const reset = () => {
  resetFields()
  supplierCode.value = undefined
  orderDateRange.value = undefined
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
