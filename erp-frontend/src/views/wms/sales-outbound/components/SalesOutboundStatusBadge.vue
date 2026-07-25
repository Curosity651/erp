<template>
  <a-badge :status="badgeStatus" :text="statusText" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { OutboundOrderStatus, OutboundOrderStatusMap } from '@/api/wms/sales-outbound/types.ts'

defineOptions({ name: 'OutboundStatusBadge' })

const props = defineProps<{
  status: string
}>()

type BadgeStatus = 'success' | 'processing' | 'default' | 'error' | 'warning'

const STATUS_CONFIG_MAP: Record<string, BadgeStatus> = {
  [OutboundOrderStatus.WAITING_TRANSFER]: 'warning',
  [OutboundOrderStatus.DRAFT]: 'default',
  [OutboundOrderStatus.CONFIRMED]: 'processing',
  [OutboundOrderStatus.CANCELLED]: 'error',
  [OutboundOrderStatus.PICKING]: 'processing',
  [OutboundOrderStatus.BACKORDER]: 'warning',
  [OutboundOrderStatus.PACKED]: 'processing',
  [OutboundOrderStatus.SHIPPED]: 'success'
}

const badgeStatus = computed(() => STATUS_CONFIG_MAP[props.status] || 'default')
const statusText = computed(() => OutboundOrderStatusMap[props.status] || props.status)
</script>
