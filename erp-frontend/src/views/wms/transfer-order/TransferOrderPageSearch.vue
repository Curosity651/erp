<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 16 }">
    <a-form layout="inline" :model="formModel" class="transfer-filter">
      <a-form-item label="调拨单号">
        <a-input
          v-model:value="formModel.transferNo"
          placeholder="请输入调拨单号"
          allow-clear
          style="width: 130px"
        />
      </a-form-item>
      <a-form-item label="源仓库">
        <WarehouseSelect
          v-model:value="formModel.fromWarehouseId"
          placeholder="请选择源仓库"
          allow-clear
          warehouse-type="OWN"
          width="130px"
        />
      </a-form-item>
      <a-form-item label="目标仓库">
        <WarehouseSelect
          v-model:value="formModel.toWarehouseId"
          placeholder="请选择目标仓库"
          allow-clear
          width="130px"
        />
      </a-form-item>
      <a-form-item label="单据状态">
        <a-select
          v-model:value="formModel.orderStatus"
          placeholder="请选择单据状态"
          allow-clear
          style="width: 120px"
        >
          <a-select-option v-for="(label, value) in TransferOrderStatusMap" :key="value" :value="value">
            {{ label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="创建时间">
        <a-range-picker
          v-model:value="dateRange"
          style="width: 200px"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
        />
      </a-form-item>
      <div class="filter-actions">
        <a-button type="primary" :loading="props.loading" @click="search">查询</a-button>
        <a-button @click="reset">重置</a-button>
      </div>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, toRaw } from 'vue'
import { Form } from 'ant-design-vue'
import type { TransferOrderQO } from '@/api/wms/transfer-order/types'
import { TransferOrderStatusMap } from '@/api/wms/transfer-order/types'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'

const useForm = Form.useForm

const props = withDefaults(
  defineProps<{
    loading?: boolean
  }>(),
  { loading: false }
)

const emits = defineEmits<{
  (e: 'search', params: TransferOrderQO): void
}>()

const formModel = reactive<TransferOrderQO>({
  transferNo: undefined,
  fromWarehouseId: undefined,
  toWarehouseId: undefined,
  orderStatus: undefined,
  createTimeStart: undefined,
  createTimeEnd: undefined
})

// 日期范围
const dateRange = ref<[string, string] | null>(null)

const { resetFields } = useForm(formModel)

// 日期范围变化
const handleDateChange = (dates: [string, string] | null) => {
  if (dates) {
    formModel.createTimeStart = dates[0] + ' 00:00:00'
    formModel.createTimeEnd = dates[1] + ' 23:59:59'
  } else {
    formModel.createTimeStart = undefined
    formModel.createTimeEnd = undefined
  }
}

const search = () => {
  emits('search', toRaw(formModel))
}

const reset = () => {
  resetFields()
  dateRange.value = null
  formModel.createTimeStart = undefined
  formModel.createTimeEnd = undefined
  search()
}
</script>

<style scoped>
/* 筛选条件：宽屏一行显示；窗口变窄时自动折行，绝不溢出浏览器 */
.transfer-filter {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  row-gap: 12px;
}
.transfer-filter :deep(.ant-form-item) {
  margin-right: 10px;
  margin-bottom: 0;
  flex: none;
}
.transfer-filter :deep(.ant-form-item-label) {
  padding-right: 4px;
}
:deep(.ant-form-item-label) {
  font-weight: 500;
}
/* 查询/重置靠右 */
.filter-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
  flex: none;
  padding-left: 8px;
  white-space: nowrap;
}
</style>
