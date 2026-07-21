<template>
  <a-tag :color="tagColor">
    <template #icon>
      <check-outlined v-if="status === 'ALL_SHIPPED'" />
      <send-outlined v-else-if="status === 'PARTIAL_SHIPPED'" />
      <inbox-outlined v-else />
    </template>
    {{ displayText }}
  </a-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CheckOutlined, SendOutlined, InboxOutlined } from '@ant-design/icons-vue'
import type { PurchaseShippingStatus } from '@/api/wms/purchase-order/types'
import { SHIPPING_STATUS_CONFIG, calculatePercent } from '@/api/wms/purchase-order/status-utils'

defineOptions({ name: 'ShippingProgressTag' })

const props = defineProps<{
  status: PurchaseShippingStatus
  shipped: number
  total: number
}>()

const tagColor = computed(() => SHIPPING_STATUS_CONFIG[props.status]?.color || 'default')

const displayText = computed(() => {
  switch (props.status) {
    case 'ALL_SHIPPED':
      return '全部发货'
    case 'PARTIAL_SHIPPED':
      return `发货 ${calculatePercent(props.shipped, props.total)}%`
    default:
      return '未发货'
  }
})
</script>
