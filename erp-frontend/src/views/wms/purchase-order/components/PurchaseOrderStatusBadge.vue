<template>
  <a-badge :status="badgeStatus" :color="badgeColor" :text="statusText" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PurchaseOrderStatus } from '@/api/wms/purchase-order/types.ts'

defineOptions({ name: 'PurchaseOrderStatusBadge' })

const props = defineProps<{
  status: PurchaseOrderStatus
}>()

/**
 * 状态文本映射
 */
const STATUS_TEXT_MAP: Record<PurchaseOrderStatus, string> = {
  DRAFT: '草稿',
  CONFIRMED: '已确认',
  IN_PRODUCTION: '生产中',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

/**
 * 状态配置
 * - 使用 status 时有预设样式（processing 有动画）
 * - 使用 color 时为自定义颜色（静态圆点）
 */
interface StatusConfig {
  status?: 'success' | 'processing' | 'default' | 'error' | 'warning'
  color?: string
}

const STATUS_CONFIG_MAP: Record<PurchaseOrderStatus, StatusConfig> = {
  DRAFT: { status: 'default' }, // 灰色 - 草稿
  CONFIRMED: { color: '#1890ff' }, // 蓝色(静态) - 已确认，等待生产
  IN_PRODUCTION: { status: 'processing' }, // 蓝色(动态) - 生产中
  COMPLETED: { status: 'success' }, // 绿色 - 已完成
  CANCELLED: { status: 'error' } // 红色 - 已取消
}

const statusConfig = computed(() => STATUS_CONFIG_MAP[props.status] || { status: 'default' })
const badgeStatus = computed(() => statusConfig.value.status)
const badgeColor = computed(() => statusConfig.value.color)
const statusText = computed(() => STATUS_TEXT_MAP[props.status] || props.status)
</script>
