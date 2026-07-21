<template>
  <a-badge :status="badgeStatus" :text="statusText" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { InboundStatus, InboundStatusMap } from '@/api/wms/purchase-inbound/types.ts'

defineOptions({ name: 'InboundStatusBadge' })

const props = defineProps<{
  status: string
}>()

type BadgeStatus = 'success' | 'processing' | 'default' | 'error' | 'warning'

const STATUS_CONFIG_MAP: Record<string, BadgeStatus> = {
  [InboundStatus.DRAFT]: 'default',
  [InboundStatus.SUBMITTED]: 'processing',
  [InboundStatus.RECEIVED]: 'warning',
  [InboundStatus.COMPLETED]: 'success',
  [InboundStatus.CANCELLED]: 'error'
}

const badgeStatus = computed(() => STATUS_CONFIG_MAP[props.status] || 'default')
const statusText = computed(() => InboundStatusMap[props.status] || props.status)
</script>
