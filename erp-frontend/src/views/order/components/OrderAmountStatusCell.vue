<template>
  <div class="order-cell amount-status" :title="titleText">
    <!-- 金额信息（支持多币种，显示顺序由 amountOrder 控制） -->
    <div class="amount-section">
      <div v-if="hasTwoAmounts" class="amount-primary">
        {{ primaryAmountText }}
      </div>
      <div class="amount-secondary">
        {{ secondaryAmountText }}
      </div>
    </div>

    <!-- 主要状态 -->
    <div class="status-section">
      <div class="main-status">
        <span :class="['status-dot', erpStatusDotClass]"></span>
        <span class="kv-value" :title="erpStatusTip">{{ erpStatusLabel }}</span>
      </div>
      <div v-if="platformStatus || platformSubstatus" class="sub-status">
        <div v-if="platformStatus" class="kv">
          <span class="kv-key">{{ platformStatusLabel }}</span>
          <span class="kv-value" :class="platformStatusCls" :title="platformStatusTip">{{
            platformStatusText
          }}</span>
        </div>
        <div v-if="platformSubstatus" class="kv">
          <span class="kv-key">{{ platformSubstatusLabel }}</span>
          <span class="kv-value" :class="supplierStatusCls" :title="supplierStatusTip">{{
            supplierStatusText
          }}</span>
        </div>
      </div>
    </div>

    <!-- 额外内容插槽 -->
    <slot name="extra"></slot>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatAmount } from '@/utils/currency-utils'

interface StatusMapping {
  label: string
  tip: string
  cls?: string
  dotClass?: string
}

interface Props {
  /** 订单总金额（原始币种） */
  totalAmount?: number | null
  /** 原始货币代码 */
  currencyCode?: string
  /** 转换后金额 */
  convertedAmount?: number | null
  /** 转换后货币代码 */
  convertedCurrencyCode?: string
  /** ERP 状态码 */
  erpStatus?: string
  /** ERP 状态映射结果 */
  erpStatusMapping?: StatusMapping
  /** 平台状态码 */
  platformStatus?: string
  /** 平台状态映射结果 */
  platformStatusMapping?: StatusMapping
  /** 平台子状态码 */
  platformSubstatus?: string
  /** 平台子状态映射结果 */
  platformSubstatusMapping?: StatusMapping
  /** 平台状态显示标签（WB: "平台履约状态"，Ozon/Yandex: "平台状态"） */
  platformStatusLabel?: string
  /** 子状态显示标签（WB: "商家处理状态"，Ozon/Yandex: "子状态"） */
  platformSubstatusLabel?: string
  /**
   * 金额显示顺序
   * - 'total-first'：原始金额大字 + 转换金额小字（Ozon/Yandex 默认）
   * - 'converted-first'：转换金额大字 + 原始金额小字（WB）
   */
  amountOrder?: 'total-first' | 'converted-first'
}

const props = withDefaults(defineProps<Props>(), {
  platformStatusLabel: '平台状态',
  platformSubstatusLabel: '子状态',
  amountOrder: 'total-first'
})

// 金额格式化
const hasConvertedAmount = computed(() => {
  return props.convertedAmount !== undefined && props.convertedAmount !== null
})

const formattedTotalAmount = computed(() => {
  return formatAmount(props.totalAmount, props.currencyCode)
})

const formattedConvertedAmount = computed(() => {
  return formatAmount(props.convertedAmount, props.convertedCurrencyCode)
})

// 金额显示顺序
const isConvertedFirst = computed(() => props.amountOrder === 'converted-first')
const hasTwoAmounts = computed(() => hasConvertedAmount.value)

const primaryAmountText = computed(() =>
  isConvertedFirst.value ? formattedConvertedAmount.value : formattedTotalAmount.value
)

const secondaryAmountText = computed(() => {
  if (!hasTwoAmounts.value) return formattedTotalAmount.value
  return isConvertedFirst.value ? formattedTotalAmount.value : formattedConvertedAmount.value
})

// ERP 状态
const erpStatusLabel = computed(() => props.erpStatusMapping?.label || props.erpStatus || '-')
const erpStatusTip = computed(() => props.erpStatusMapping?.tip || props.erpStatus || '-')
const erpStatusDotClass = computed(() => props.erpStatusMapping?.dotClass || 'status-unknown')

// 平台状态
const platformStatusText = computed(
  () => props.platformStatusMapping?.label || props.platformStatus || '-'
)
const platformStatusTip = computed(
  () => props.platformStatusMapping?.tip || props.platformStatus || '-'
)
const platformStatusCls = computed(() => props.platformStatusMapping?.cls || '')

// 子状态
const supplierStatusText = computed(
  () => props.platformSubstatusMapping?.label || props.platformSubstatus || '-'
)
const supplierStatusTip = computed(
  () => props.platformSubstatusMapping?.tip || props.platformSubstatus || '-'
)
const supplierStatusCls = computed(() => props.platformSubstatusMapping?.cls || '')

// 标题文本
const titleText = computed(() => {
  const parts = [
    formattedTotalAmount.value,
    hasConvertedAmount.value ? formattedConvertedAmount.value : null,
    props.erpStatus
  ].filter(Boolean)
  return parts.join(' | ')
})
</script>

<style scoped>
.order-cell.amount-status {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0;
  color: #1f1f1f;
  font-size: 14px;
  line-height: 22px;
}

.amount-status .amount-section {
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: flex-start;
  text-align: left;
}

.amount-status .amount-primary {
  font-weight: 800;
  color: #1f1f1f;
  font-size: 16px;
  letter-spacing: 0.2px;
  font-variant-numeric: tabular-nums;
}

.amount-status .amount-secondary {
  font-size: 12px;
  color: #8c8c8c;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-variant-numeric: tabular-nums;
}

.amount-status .main-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.amount-status .status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  vertical-align: middle;
  flex-shrink: 0;
}

.status-dot.status-pending { background: #b5b5ba; }
.status-dot.status-readytoship { background: #faad14; }
.status-dot.status-shipped { background: #1890ff; }
.status-dot.status-arrivedatplatformwarehouse { background: #52c41a; }
.status-dot.status-delivered { background: #13c2c2; }
.status-dot.status-canceled { background: #ff4d4f; }
.status-dot.status-returned { background: #722ed1; }
.status-dot.status-sold { background: #52c41a; }
.status-dot.status-unknown { background: #d9d9d9; }

.amount-status .main-status .kv-value {
  font-weight: 700;
  font-size: 12px;
  color: #262626;
}

.amount-status .sub-status {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.amount-status .sub-status .kv {
  display: flex;
  align-items: center;
  gap: 6px;
}

.amount-status .sub-status .kv-key {
  font-size: 11px;
  color: #8c8c8c;
}

.amount-status .sub-status .kv-value {
  font-size: 11px;
  color: #595959;
  font-weight: 600;
}

/* WB 向后兼容 + 通用状态颜色 */
.amount-status .sub-status .wb-s-neutral,
.amount-status .sub-status .neutral { color: #8c8c8c; }
.amount-status .sub-status .wb-s-process,
.amount-status .sub-status .processing { color: #1677ff; }
.amount-status .sub-status .wb-s-transit { color: #1890ff; }
.amount-status .sub-status .wb-s-success,
.amount-status .sub-status .success { color: #52c41a; }
.amount-status .sub-status .wb-s-warning,
.amount-status .sub-status .warning { color: #faad14; }
.amount-status .sub-status .wb-s-error,
.amount-status .sub-status .error { color: #ff4d4f; }
.amount-status .sub-status .orange { color: #fa8c16; }
.amount-status .sub-status .blue { color: #1890ff; }
.amount-status .sub-status .purple { color: #722ed1; }
.amount-status .sub-status .green { color: #52c41a; }
.amount-status .sub-status .gray { color: #8c8c8c; }
.amount-status .sub-status .default { color: #595959; }
</style>
