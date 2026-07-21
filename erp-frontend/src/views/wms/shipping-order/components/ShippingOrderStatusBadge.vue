<template>
  <a-badge :status="badgeStatus" :color="badgeColor" :text="statusText" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ShippingStatus, ShippingStatusMap } from '@/api/wms/shipping-order/types.ts'

defineOptions({ name: 'ShippingOrderStatusBadge' })

const props = defineProps<{
  status: string
}>()

/**
 * 状态配置
 */
interface StatusConfig {
  status?: 'success' | 'processing' | 'default' | 'error' | 'warning'
  color?: string
}

const STATUS_CONFIG_MAP: Record<string, StatusConfig> = {
  [ShippingStatus.PENDING]: { status: 'default' }, // 灰色 - 待发货
  [ShippingStatus.SHIPPED]: { status: 'processing' }, // 蓝色(动态) - 已发货，运输中
  [ShippingStatus.PARTIAL_ARRIVED]: { color: '#fa8c16' }, // 橙色 - 部分到货
  [ShippingStatus.ALL_ARRIVED]: { color: '#13c2c2' }, // 青色 - 全部到货，待完成
  [ShippingStatus.COMPLETED]: { status: 'success' } // 绿色 - 已完成
}

const statusConfig = computed(() => STATUS_CONFIG_MAP[props.status] || { status: 'default' })
const badgeStatus = computed(() => statusConfig.value.status)
const badgeColor = computed(() => statusConfig.value.color)
const statusText = computed(() => ShippingStatusMap[props.status] || props.status)
</script>
