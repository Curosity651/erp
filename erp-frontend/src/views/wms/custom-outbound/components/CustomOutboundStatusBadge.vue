<template>
  <a-badge :status="badgeStatus" :text="statusText" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CustomOutboundStatus, CustomOutboundStatusMap } from '@/api/wms/custom-outbound/types'

defineOptions({ name: 'CustomOutboundStatusBadge' })

const props = defineProps<{
  status: string
}>()

type BadgeStatus = 'success' | 'processing' | 'default' | 'error' | 'warning'

const STATUS_CONFIG_MAP: Record<string, BadgeStatus> = {
  [CustomOutboundStatus.DRAFT]: 'default',
  [CustomOutboundStatus.CONFIRMED]: 'processing',
  [CustomOutboundStatus.CANCELLED]: 'error',
  [CustomOutboundStatus.PICKING]: 'processing',
  [CustomOutboundStatus.BACKORDER]: 'warning',
  [CustomOutboundStatus.PACKED]: 'processing',
  [CustomOutboundStatus.SHIPPED]: 'success'
}

const badgeStatus = computed(() => STATUS_CONFIG_MAP[props.status] || 'default')
const statusText = computed(() => CustomOutboundStatusMap[props.status] || props.status)
</script>
