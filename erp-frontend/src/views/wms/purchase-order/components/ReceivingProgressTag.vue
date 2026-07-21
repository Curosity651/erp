<template>
  <a-tag :color="tagColor">
    <template #icon>
      <check-outlined v-if="status === 'ALL_RECEIVED'" />
      <import-outlined v-else-if="status === 'PARTIAL_RECEIVED'" />
      <home-outlined v-else />
    </template>
    {{ displayText }}
  </a-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CheckOutlined, ImportOutlined, HomeOutlined } from '@ant-design/icons-vue'
import type { PurchaseReceivingStatus } from '@/api/wms/purchase-order/types'
import { RECEIVING_STATUS_CONFIG, calculatePercent } from '@/api/wms/purchase-order/status-utils'

defineOptions({ name: 'ReceivingProgressTag' })

const props = defineProps<{
  status: PurchaseReceivingStatus
  received: number
  total: number
}>()

const tagColor = computed(() => RECEIVING_STATUS_CONFIG[props.status]?.color || 'default')

const displayText = computed(() => {
  switch (props.status) {
    case 'ALL_RECEIVED':
      return '全部入库'
    case 'PARTIAL_RECEIVED':
      return `入库 ${calculatePercent(props.received, props.total)}%`
    default:
      return '未入库'
  }
})
</script>
