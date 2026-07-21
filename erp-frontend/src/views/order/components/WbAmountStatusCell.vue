<template>
  <div class="order-cell amount-status" :title="titleText">
    <!-- 金额信息（支持多币种）：主=转换后金额，副=原始金额 -->
    <div class="amount-section">
      <div v-if="hasConvertedAmount" class="amount-primary">
        {{ formattedConvertedAmount }}
      </div>
      <div class="amount-secondary">
        {{ formattedTotalAmount }}
      </div>
    </div>

    <!-- 主要状态 -->
    <div class="status-section">
      <div class="main-status">
        <span :class="['status-dot', erpStatusDotClass]"></span>
        <span class="kv-value" :title="erpStatusTip">{{ erpStatusLabel }}</span>
      </div>
      <div v-if="platformStatus || platformSubstatus" class="sub-status">
        <div v-if="platformSubstatus" class="kv">
          <span class="kv-key">商家处理状态</span>
          <span class="kv-value" :class="supplierStatusCls" :title="supplierStatusTip">{{
            supplierStatusLabel
          }}</span>
        </div>
        <div v-if="platformStatus" class="kv">
          <span class="kv-key">平台履约状态</span>
          <span class="kv-value" :class="platformStatusCls" :title="platformStatusTip">{{
            platformStatusLabel
          }}</span>
        </div>
      </div>
    </div>

    <!-- 额外内容插槽（如异常标签） -->
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
  /** 转换后金额（如卢布） */
  convertedAmount?: number | null
  /** 转换后货币代码 */
  convertedCurrencyCode?: string
  /** ERP 状态码 */
  erpStatus?: string
  /** ERP 状态映射结果 */
  erpStatusMapping?: StatusMapping
  /** 平台履约状态码 */
  platformStatus?: string
  /** 平台履约状态映射结果 */
  platformStatusMapping?: StatusMapping
  /** 商家处理状态码 */
  platformSubstatus?: string
  /** 商家处理状态映射结果 */
  platformSubstatusMapping?: StatusMapping
}

const props = defineProps<Props>()

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

// ERP 状态
const erpStatusLabel = computed(() => props.erpStatusMapping?.label || props.erpStatus || '-')
const erpStatusTip = computed(() => props.erpStatusMapping?.tip || props.erpStatus || '-')
const erpStatusDotClass = computed(() => props.erpStatusMapping?.dotClass || 'status-unknown')

// 商家处理状态
const supplierStatusLabel = computed(
  () => props.platformSubstatusMapping?.label || props.platformSubstatus || '-'
)
const supplierStatusTip = computed(
  () => props.platformSubstatusMapping?.tip || props.platformSubstatus || '-'
)
const supplierStatusCls = computed(() => props.platformSubstatusMapping?.cls || '')

// 平台履约状态
const platformStatusLabel = computed(
  () => props.platformStatusMapping?.label || props.platformStatus || '-'
)
const platformStatusTip = computed(
  () => props.platformStatusMapping?.tip || props.platformStatus || '-'
)
const platformStatusCls = computed(() => props.platformStatusMapping?.cls || '')

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

<style scoped lang="less">
.order-cell.amount-status {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0;
  color: #1f1f1f;
  font-size: 14px;
  line-height: 22px;

  .amount-section {
    display: flex;
    flex-direction: column;
    gap: 2px;
    align-items: flex-start;
    text-align: left;

    .amount-primary {
      font-weight: 800;
      color: #1f1f1f;
      font-size: 16px;
      letter-spacing: 0.2px;
      font-variant-numeric: tabular-nums;
      font-feature-settings:
        'tnum' 1,
        'lnum' 1;
    }

    .amount-secondary {
      font-size: 12px;
      color: #8c8c8c;
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-variant-numeric: tabular-nums;
      font-feature-settings:
        'tnum' 1,
        'lnum' 1;
    }
  }

  .status-section {
    .main-status {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 4px;

      .status-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        display: inline-block;
        vertical-align: middle;
        flex-shrink: 0;

        &.status-pending {
          background: #b5b5ba;
        }
        &.status-readytoship {
          background: #faad14;
        }
        &.status-shipped {
          background: #1890ff;
        }
        &.status-arrivedatplatformwarehouse {
          background: #52c41a;
        }
        &.status-delivered {
          background: #13c2c2;
        }
        &.status-canceled {
          background: #ff4d4f;
        }
        &.status-returned {
          background: #722ed1;
        }
        &.status-sold {
          background: #52c41a;
        }
        &.status-unknown {
          background: #d9d9d9;
        }
      }

      .kv-value {
        font-weight: 700;
        font-size: 12px;
        color: #262626;
      }
    }

    .sub-status {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .kv {
        display: flex;
        align-items: center;
        gap: 6px;
      }

      .kv-key {
        font-size: 11px;
        color: #8c8c8c;
      }

      .kv-value {
        font-size: 11px;
        color: #595959;
        font-weight: 600;
      }

      // 履约状态色系
      .wb-s-neutral {
        color: #8c8c8c;
      }
      .wb-s-process {
        color: #1677ff;
      }
      .wb-s-transit {
        color: #1890ff;
      }
      .wb-s-success {
        color: #52c41a;
      }
      .wb-s-warning {
        color: #faad14;
      }
      .wb-s-error {
        color: #ff4d4f;
      }
    }
  }
}
</style>
