<template>
  <div class="order-info" :title="titleText">
    <!-- 第一行：平台 + 订单号 -->
    <div class="order-info__row order-info__row--primary">
      <PlatformTag v-if="platform" :platform="platformLower" />
      <span class="order-info__order-id">{{ platformOrderId || '-' }}</span>
    </div>

    <!-- 第二行：店铺名称（弱化、单行省略 + tooltip） -->
    <div class="order-info__row order-info__row--shop">
      <span class="order-info__shop-icon">🏬</span>
      <span class="order-info__shop-name" :title="shopName || '-'">
        {{ shopName || '-' }}
      </span>
    </div>

    <!-- 第三行：ERP编号 + 配送方式 -->
    <div v-if="erpOrderId || fulfillmentType" class="order-info__row order-info__row--secondary">
      <span v-if="erpOrderId" class="order-info__erp-id">ERP: {{ erpOrderId }}</span>
      <span v-if="erpOrderId && fulfillmentType" class="order-info__divider">·</span>
      <span v-if="fulfillmentType" class="order-info__delivery">{{ fulfillmentType.toUpperCase() }}</span>
    </div>

    <!-- 第四行：时间（独立一行，显示莫斯科时间） -->
    <div v-if="orderTime" class="order-info__row order-info__row--time">
      <span class="order-info__time-icon">📅</span>
      <span class="order-info__time-value">
        {{ formattedTime }}
        <span v-if="showTimezone" class="order-info__tz-badge">{{ timezone }}</span>
      </span>
    </div>

    <!-- 扩展插槽：用于展示额外信息（如 RID 等） -->
    <div v-if="$slots.extra" class="order-info__row order-info__row--extra">
      <slot name="extra" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatDateTime } from '@/utils/financial-utils'
import { PlatformTag } from '@/components/Platform'
import type { PlatformType } from '@/constants/platform'

interface Props {
  /** 平台名称 (Wildberries, Ozon 等) */
  platform?: string
  /** 平台订单号 */
  platformOrderId?: string
  /** ERP 订单 ID */
  erpOrderId?: number | string
  /** 店铺名称 */
  shopName?: string
  /** 履约类型 (FBS/FBO) */
  fulfillmentType?: string
  /** 订单时间 */
  orderTime?: string
  /** 是否显示时区标签 */
  showTimezone?: boolean
  /** 时区标签文字 */
  timezone?: string
  /** 自定义标题 */
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  showTimezone: true,
  timezone: 'MSK'
})

const platformLower = computed(() => (props.platform || '').toLowerCase() as PlatformType)

const formattedTime = computed(() => {
  return formatDateTime(props.orderTime)
})

const titleText = computed(() => {
  if (props.title) return props.title
  const parts = [
    props.platform,
    props.platformOrderId,
    props.erpOrderId ? `ERP:${props.erpOrderId}` : null,
    props.fulfillmentType,
    props.orderTime ? `${formattedTime.value} (${props.timezone})` : null
  ].filter(Boolean)
  return parts.join(' | ')
})
</script>

<style scoped>
/* ========== Block: order-info ========== */
.order-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #1f1f1f;
  font-size: 14px;
  line-height: 22px;
}

/* ========== Element: row (基础) ========== */
.order-info__row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

/* ========== Modifier: row variants ========== */
.order-info__row--primary {
  margin-bottom: 2px;
}

.order-info__row--secondary {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 2px;
}

.order-info__row--shop {
  font-size: 13px;
  color: #595959;
  min-width: 0;
  gap: 6px;
}

.order-info__row--time {
  font-size: 12px;
  color: #8c8c8c;
  gap: 6px;
}

.order-info__row--extra {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

/* ========== Elements ========== */
.order-info__order-id {
  font-weight: 700;
  color: #262626;
  font-size: 15px;
}

.order-info__shop-icon {
  font-size: 13px;
  opacity: 0.8;
}

.order-info__shop-name {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.order-info__erp-id {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  color: #8c8c8c;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.order-info__divider {
  color: #bfbfbf;
  margin: 0 4px;
}

.order-info__delivery {
  color: #8c8c8c;
  font-size: 13px;
}

.order-info__time-icon {
  font-size: 12px;
  opacity: 0.8;
}

.order-info__time-value {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: flex;
  align-items: center;
  gap: 4px;
  font-variant-numeric: tabular-nums;
}

.order-info__tz-badge {
  display: inline-block;
  padding: 0 4px;
  font-size: 10px;
  line-height: 16px;
  color: #1890ff;
  background: #e6f7ff;
  border-radius: 2px;
  font-weight: 600;
  letter-spacing: 0.3px;
  flex-shrink: 0;
}

/* ========== 响应式 ========== */
@media (max-width: 1400px) {
  .order-info__time-value {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style>
