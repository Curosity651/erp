<template>
  <div class="form-section">
    <div class="section-title">
      <dollar-outlined class="section-icon section-icon--cost" />
      费用信息
    </div>

    <!-- 灰关模式 -->
    <template v-if="shippingMethod === 'GRAY'">
      <a-row :gutter="24">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item label="物流单价(USD/KG)">
            <a-input-number
              :value="unitPrice"
              :min="0"
              :max="99999999.9999"
              :precision="4"
              style="width: 100%"
              placeholder="请输入"
              :disabled="disabled"
              @change="handleUnitPriceChange"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <!-- 费用统计 -->
      <div class="cost-statistics">
        <a-row :gutter="24">
          <a-col :xs="12" :sm="8" :lg="6">
            <a-statistic
              title="物流总额 (USD)"
              :value="calculatedAmountUsd"
              :precision="2"
              prefix="$"
              :value-style="{ color: '#cf1322', fontWeight: 600 }"
            />
          </a-col>
          <a-col :xs="12" :sm="8" :lg="6">
            <a-statistic
              title="物流总额 (CNY)"
              :value="calculatedAmountCny"
              :precision="2"
              prefix="¥"
              :value-style="{ color: '#fa8c16', fontWeight: 600 }"
            />
          </a-col>
        </a-row>
      </div>
    </template>

    <!-- 白关模式 -->
    <template v-else-if="shippingMethod === 'WHITE'">
      <a-row :gutter="24">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item label="运输费用(USD)">
            <a-input-number
              :value="shippingFee"
              :min="0"
              :precision="2"
              style="width: 100%"
              placeholder="请输入"
              :disabled="disabled"
              @change="handleShippingFeeChange"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item label="杂费(USD)">
            <a-input-number
              :value="miscFee"
              :min="0"
              :precision="2"
              style="width: 100%"
              placeholder="请输入"
              :disabled="disabled"
              @change="handleMiscFeeChange"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <!-- 费用统计 -->
      <div class="cost-statistics">
        <a-row :gutter="24">
          <a-col :xs="12" :sm="8" :lg="6">
            <a-statistic
              title="物流总额 (USD)"
              :value="calculatedAmountUsd"
              :precision="2"
              prefix="$"
              :value-style="{ color: '#cf1322', fontWeight: 600 }"
            />
          </a-col>
          <a-col :xs="12" :sm="8" :lg="6">
            <a-statistic
              title="物流总额 (CNY)"
              :value="calculatedAmountCny"
              :precision="2"
              prefix="¥"
              :value-style="{ color: '#fa8c16', fontWeight: 600 }"
            />
          </a-col>
        </a-row>
      </div>
    </template>

    <!-- 未选择物流方式 -->
    <template v-else>
      <a-empty description="请先选择物流方式" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
    </template>

    <!-- 汇率信息 -->
    <div v-if="shippingMethod" class="exchange-rate-info">
      <a-spin v-if="loadingRate" size="small" />
      <template v-else>
        <span>当前汇率：1 USD = {{ exchangeRate }} CNY</span>
        <a-button type="link" size="small" :loading="loadingRate" @click="refreshExchangeRate">
          刷新汇率
        </a-button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { Empty, message } from 'ant-design-vue'
import { DollarOutlined } from '@ant-design/icons-vue'
import { getLatestExchangeRate } from '@/api/system/exchange-rate'
import { isSuccess } from '@/api'

const props = defineProps<{
  shippingMethod?: string
  totalWeight?: number
  unitPrice?: number
  shippingFee?: number
  miscFee?: number
  disabled: boolean
}>()

const emits = defineEmits<{
  (e: 'update:unitPrice', value: number | undefined): void
  (e: 'update:shippingFee', value: number | undefined): void
  (e: 'update:miscFee', value: number | undefined): void
  (e: 'amountChange', usd: number, cny: number): void
}>()

// 汇率（默认值）
const exchangeRate = ref(7.2)
const loadingRate = ref(false)

// 计算 USD 金额（直接使用 props，无需内部状态）
const calculatedAmountUsd = computed(() => {
  if (props.shippingMethod === 'GRAY') {
    return (props.unitPrice || 0) * (props.totalWeight || 0)
  } else if (props.shippingMethod === 'WHITE') {
    return (props.shippingFee || 0) + (props.miscFee || 0)
  }
  return 0
})

// 计算 CNY 金额
const calculatedAmountCny = computed(() => {
  return calculatedAmountUsd.value * exchangeRate.value
})

// 处理单价变化
const handleUnitPriceChange = (value: number | null) => {
  emits('update:unitPrice', value ?? undefined)
}

// 处理运费变化
const handleShippingFeeChange = (value: number | null) => {
  emits('update:shippingFee', value ?? undefined)
}

// 处理杂费变化
const handleMiscFeeChange = (value: number | null) => {
  emits('update:miscFee', value ?? undefined)
}

// 加载汇率
const loadExchangeRate = async () => {
  loadingRate.value = true
  try {
    const result = await getLatestExchangeRate('USD', 'CNY')
    if (isSuccess(result) && result.data) {
      exchangeRate.value = result.data
    }
  } catch (e) {
    console.error('获取汇率失败', e)
  } finally {
    loadingRate.value = false
  }
}

// 刷新汇率
const refreshExchangeRate = async () => {
  await loadExchangeRate()
  message.success('汇率已刷新')
}

// 计算并通知父组件
const calculate = () => {
  emits('amountChange', calculatedAmountUsd.value, calculatedAmountCny.value)
}

// 监听金额变化
watch([calculatedAmountUsd, calculatedAmountCny], () => {
  calculate()
})

onMounted(() => {
  loadExchangeRate()
})

defineExpose({ calculate })
</script>

<style scoped>
.form-section {
  padding: 16px 20px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.section-icon {
  margin-right: 8px;
  font-size: 16px;
}

.section-icon--cost {
  color: #fa8c16;
}

/* 费用统计区域 */
.cost-statistics {
  padding: 16px;
  margin-top: 8px;
  background: #fafafa;
  border-radius: 6px;
}

.exchange-rate-info {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #f0f0f0;
  font-size: 12px;
  color: #8c8c8c;
}
</style>
