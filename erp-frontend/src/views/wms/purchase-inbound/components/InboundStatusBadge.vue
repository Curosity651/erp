<template>
  <a-badge :status="badgeStatus" :text="statusText" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { InboundStatus } from '@/api/wms/purchase-inbound/types.ts'

defineOptions({ name: 'InboundStatusBadge' })

const props = defineProps<{
  status: string
}>()
const { t } = useI18n()

type BadgeStatus = 'success' | 'processing' | 'default' | 'error' | 'warning'

const STATUS_CONFIG_MAP: Record<string, BadgeStatus> = {
  [InboundStatus.DRAFT]: 'default',
  [InboundStatus.SUBMITTED]: 'processing',
  [InboundStatus.RECEIVED]: 'warning',
  [InboundStatus.COMPLETED]: 'success',
  [InboundStatus.CANCELLED]: 'error'
}

const STATUS_I18N_KEYS: Record<string, string> = {
  [InboundStatus.DRAFT]: 'platform.inbound.status.draft',
  [InboundStatus.SUBMITTED]: 'platform.inbound.status.submitted',
  [InboundStatus.RECEIVED]: 'platform.inbound.status.received',
  [InboundStatus.COMPLETED]: 'platform.inbound.status.completed',
  [InboundStatus.CANCELLED]: 'platform.inbound.status.cancelled'
}

const badgeStatus = computed(() => STATUS_CONFIG_MAP[props.status] || 'default')
const statusText = computed(() => {
  const key = STATUS_I18N_KEYS[props.status]
  return key ? t(key) : props.status
})
</script>
