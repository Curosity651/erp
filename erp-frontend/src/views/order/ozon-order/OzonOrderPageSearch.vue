<template>
  <div class="order-search-container">
    <a-card class="quick-search-card" :bordered="false">
      <div class="search-header">
        <div class="search-title">
          <search-outlined class="search-icon" />
          <span>订单搜索</span>
        </div>
        <div class="search-actions-inline">
          <a-button type="primary" :loading="props.loading" @click="search">
            <template #icon>
              <search-outlined />
            </template>
            搜索
          </a-button>
          <a-button @click="reset">
            <template #icon>
              <reload-outlined />
            </template>
            重置
          </a-button>
        </div>
      </div>

      <a-form :model="formModel" class="quick-search-form">
        <a-row :gutter="16">
          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">店铺</label>
              <shop-select-input v-model="formModel.shopId" placeholder="请选择店铺" />
            </div>
          </a-col>

          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">订单状态</label>
              <a-select v-model:value="formModel.businessStatus" allow-clear placeholder="选择订单状态">
                <a-select-option v-for="(value, key) in OWNER_ORDER_STATUS_MAP" :key="key" :value="key"
                  >{{ value.label }}
                </a-select-option>
              </a-select>
            </div>
          </a-col>

          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">配送方式</label>
              <a-select
                v-model:value="formModel.fulfillmentType"
                allow-clear
                placeholder="配送方式"
              >
                <a-select-option value="FBS">FBS</a-select-option>
                <a-select-option value="FBO">FBO</a-select-option>
              </a-select>
            </div>
          </a-col>

          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">仓型</label>
              <a-select v-model:value="formModel.warehouseType" allow-clear placeholder="选择仓型">
                <a-select-option value="BIG">大仓（大件）</a-select-option>
                <a-select-option value="SMALL">小仓</a-select-option>
              </a-select>
            </div>
          </a-col>

          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">锁定状态</label>
              <a-select v-model:value="formModel.locked" allow-clear placeholder="选择锁定状态">
                <a-select-option :value="1">已锁定</a-select-option>
                <a-select-option :value="0">未锁定</a-select-option>
              </a-select>
            </div>
          </a-col>

          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">订单号</label>
              <a-input
                v-model:value="formModel.keyword"
                placeholder="平台单号或 ERP 编号"
                allow-clear
              />
            </div>
          </a-col>

          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">商品SKU</label>
              <sku-select-input v-model="formModel.skuCode" placeholder="请选择SKU" />
            </div>
          </a-col>

          <a-col v-bind="wideColConfig">
            <div class="search-group">
              <label class="search-label">订单生成时间</label>
              <a-range-picker
                v-model:value="createdAtRange"
                :show-time="{ format: 'HH:mm:ss' }"
                format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
                allow-clear
              />
            </div>
          </a-col>
        </a-row>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { Form } from 'ant-design-vue'
import { OWNER_ORDER_STATUS_MAP, type OzonOrderQO } from '@/api/order/ozon-order/types'
import dayjs from 'dayjs'
import ShopSelectInput from '@/components/ShopSelectInput.vue'
import SkuSelectInput from '@/components/Sku/SkuSelectInput.vue'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue'

const useForm = Form.useForm

// 栅格配置（参考 SkuPageSearch）
const colConfig = { xs: 24, sm: 12, md: 8, lg: 8, xl: 6, xxl: 6 }
const wideColConfig = { xs: 24, sm: 12, md: 16, lg: 16, xl: 12, xxl: 12 }

const props = withDefaults(
  defineProps<{
    loading?: boolean
  }>(),
  { loading: false }
)

const emits = defineEmits<{
  (e: 'search', params: Record<string, any>): void
}>()

const formModel = reactive<OzonOrderQO>({
  shopId: undefined,
  businessStatus: undefined,
  fulfillmentType: undefined,
  warehouseType: undefined,
  locked: undefined,
  keyword: undefined,
  skuCode: undefined,
  createdAtStart: undefined,
  createdAtEnd: undefined
})

// 平台创建时间范围(订单生成时间)
const createdAtRange = ref<[any, any] | null>(null)

const { resetFields } = useForm(formModel)

const normalizeRange = (range: [any, any] | null) => {
  if (!range || !range[0] || !range[1]) return [undefined, undefined]
  return [
    dayjs(range[0]).format('YYYY-MM-DD HH:mm:ss'),
    dayjs(range[1]).format('YYYY-MM-DD HH:mm:ss')
  ]
}

const search = () => {
  const [start, end] = normalizeRange(createdAtRange.value)
  formModel.createdAtStart = start
  formModel.createdAtEnd = end
  emits('search', toRaw(formModel))
}

const reset = () => {
  resetFields()
  createdAtRange.value = null
  formModel.skuCode = undefined
  search()
}
</script>

<style scoped>
.order-search-container {
  margin-bottom: 16px;
}

.quick-search-card {
  margin-bottom: 12px;
  border-radius: 8px;
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
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
