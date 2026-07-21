<template>
  <div class="financial-search-container">
    <a-card class="quick-search-card" :bordered="false">
      <div class="search-header">
        <div class="search-title">
          <search-outlined class="search-icon" />
          <span>财务报表搜索</span>
        </div>
        <div class="search-actions-inline">
          <a-button type="primary" :loading="props.loading" @click="search">
            <template #icon><search-outlined /></template>
            搜索
          </a-button>
          <a-button @click="reset">
            <template #icon><reload-outlined /></template>
            重置
          </a-button>
        </div>
      </div>

      <a-form :model="formModel" class="quick-search-form">
        <a-row :gutter="16">
          <!-- 店铺选择 -->
          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">店铺</label>
              <shop-select-input v-model="formModel.shopId" placeholder="请选择店铺" />
            </div>
          </a-col>

          <!-- 供应商操作名称 -->
          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">供应商操作名称</label>
              <a-input
                v-model:value="formModel.supplierOperName"
                placeholder="请输入供应商操作名称"
                allow-clear
              />
            </div>
          </a-col>

          <!-- 报表ID -->
          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">报表ID</label>
              <a-input-number
                v-model:value="formModel.realizationreportId"
                placeholder="请输入报表ID"
                :controls="false"
                style="width: 100%"
                allow-clear
              />
            </div>
          </a-col>

          <!-- 装配单ID（assemblyId） -->
          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">装配单ID</label>
              <a-input
                v-model:value="formModel.assemblyId"
                placeholder="请输入装配单ID"
                allow-clear
              />
            </div>
          </a-col>

          <!-- 报表日期范围 -->
          <a-col v-bind="wideColConfig">
            <div class="search-group">
              <label class="search-label">报表日期范围</label>
              <a-range-picker
                v-model:value="rrDtRange"
                format="YYYY-MM-DD"
                style="width: 100%"
                allow-clear
                :placeholder="['开始日期', '结束日期']"
              />
            </div>
          </a-col>

          <!-- 仅显示未关联订单 -->
          <a-col v-bind="colConfig">
            <div class="search-group checkbox-group">
              <a-checkbox v-model:checked="formModel.unmatchedOrderOnly">
                仅显示未关联订单
              </a-checkbox>
              <span class="checkbox-hint">assemblyId 为空/0 或订单不存在</span>
            </div>
          </a-col>

          <!-- 订单时间范围 -->
          <a-col v-bind="wideColConfig">
            <div class="search-group">
              <label class="search-label">订单时间范围</label>
              <a-range-picker
                v-model:value="orderDtRange"
                :show-time="{ format: 'HH:mm:ss' }"
                format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
                allow-clear
                :placeholder="['开始时间', '结束时间']"
              />
            </div>
          </a-col>

          <!-- 销售时间范围 -->
          <a-col v-bind="wideColConfig">
            <div class="search-group">
              <label class="search-label">销售时间范围</label>
              <a-range-picker
                v-model:value="saleDtRange"
                :show-time="{ format: 'HH:mm:ss' }"
                format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
                allow-clear
                :placeholder="['开始时间', '结束时间']"
              />
            </div>
          </a-col>
        </a-row>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { Form, message } from 'ant-design-vue'
import type { WbReportDetailQO } from '@/api/financial/wb-report/types'
import dayjs from 'dayjs'
import ShopSelectInput from '@/components/ShopSelectInput.vue'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue'

const useForm = Form.useForm

// 栅格配置
const colConfig = { xs: 24, sm: 12, md: 8, lg: 8, xl: 6, xxl: 6 }
const wideColConfig = { xs: 24, sm: 12, md: 16, lg: 16, xl: 12, xxl: 12 }

const props = withDefaults(
  defineProps<{
    loading?: boolean
  }>(),
  { loading: false }
)

const emits = defineEmits<{
  (e: 'search', params: WbReportDetailQO): void
}>()

const formModel = reactive<WbReportDetailQO>({
  shopId: undefined,
  rrDtStart: undefined,
  rrDtEnd: undefined,
  orderDtStart: undefined,
  orderDtEnd: undefined,
  saleDtStart: undefined,
  saleDtEnd: undefined,
  supplierOperName: undefined,
  realizationreportId: undefined,
  assemblyId: undefined,
  unmatchedOrderOnly: undefined
})

// 日期范围
const rrDtRange = ref<[any, any] | null>(null)
const orderDtRange = ref<[any, any] | null>(null)
const saleDtRange = ref<[any, any] | null>(null)

const { resetFields } = useForm(formModel)

/**
 * 格式化日期范围
 */
const normalizeDateRange = (range: [any, any] | null, withTime = false) => {
  if (!range || !range[0] || !range[1]) return [undefined, undefined]

  const format = withTime ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD'
  return [dayjs(range[0]).format(format), dayjs(range[1]).format(format)]
}

/**
 * 日期范围验证
 */
const validateDateRange = (start?: string, end?: string): boolean => {
  if (!start || !end) return true

  const startDate = dayjs(start)
  const endDate = dayjs(end)

  if (endDate.isBefore(startDate)) {
    return false
  }

  return true
}

/**
 * 搜索
 */
const search = () => {
  // 格式化报表日期范围
  const [rrStart, rrEnd] = normalizeDateRange(rrDtRange.value, false)
  formModel.rrDtStart = rrStart
  formModel.rrDtEnd = rrEnd

  // 格式化订单时间范围
  const [orderStart, orderEnd] = normalizeDateRange(orderDtRange.value, true)
  formModel.orderDtStart = orderStart
  formModel.orderDtEnd = orderEnd

  // 格式化销售时间范围
  const [saleStart, saleEnd] = normalizeDateRange(saleDtRange.value, true)
  formModel.saleDtStart = saleStart
  formModel.saleDtEnd = saleEnd

  // 验证日期范围
  if (!validateDateRange(formModel.rrDtStart, formModel.rrDtEnd)) {
    message.error('报表日期范围：结束日期不能早于开始日期')
    return
  }

  if (!validateDateRange(formModel.orderDtStart, formModel.orderDtEnd)) {
    message.error('订单时间范围：结束时间不能早于开始时间')
    return
  }

  if (!validateDateRange(formModel.saleDtStart, formModel.saleDtEnd)) {
    message.error('销售时间范围：结束时间不能早于开始时间')
    return
  }

  emits('search', toRaw(formModel))
}

/**
 * 重置
 */
const reset = () => {
  resetFields()
  rrDtRange.value = null
  orderDtRange.value = null
  saleDtRange.value = null
  search()
}
</script>

<style scoped>
.financial-search-container {
  margin-bottom: 12px;
}

.quick-search-card {
  margin-bottom: 0;
  border-radius: 8px;
  padding: 12px 16px;
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.search-title {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.search-icon {
  margin-right: 8px;
  color: #1890ff;
}

.search-actions-inline {
  display: flex;
  gap: 8px;
}

.quick-search-form {
  margin: 0;
  padding-top: 4px;
}

.search-group {
  display: flex;
  flex-direction: column;
}

.search-label {
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #595959;
  white-space: nowrap;
}

.checkbox-group {
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100%;
  padding-top: 20px;
}

.checkbox-hint {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 4px;
}

@media (max-width: 768px) {
  .search-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  .search-actions-inline {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
