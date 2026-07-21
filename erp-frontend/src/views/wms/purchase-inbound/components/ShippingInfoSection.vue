<template>
  <div class="shipping-info-section">
    <div class="section-title">
      <car-outlined class="section-icon" />
      物流单信息
    </div>
    <template v-if="shipping">
      <a-descriptions :column="3" size="small" bordered>
        <a-descriptions-item label="物流单号">{{ shipping.shippingNo }}</a-descriptions-item>
        <a-descriptions-item label="物流商">{{ shipping.providerName }}</a-descriptions-item>
        <a-descriptions-item label="发货日期">{{ shipping.shippingDate }}</a-descriptions-item>
        <a-descriptions-item label="发货总数">{{ shipping.totalQuantity }} 件</a-descriptions-item>
        <a-descriptions-item label="已入库">
          <span class="received-quantity">{{ shipping.receivedQuantity }}</span> 件
        </a-descriptions-item>
        <a-descriptions-item label="待入库">
          <span class="pending-quantity">{{ shipping.pendingQuantity }}</span> 件
        </a-descriptions-item>
      </a-descriptions>
    </template>
    <template v-else>
      <div class="empty-placeholder">
        <inbox-outlined class="empty-icon" />
        <span>请先选择物流单</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { CarOutlined, InboxOutlined } from '@ant-design/icons-vue'
import type { AvailableShippingVO } from '@/api/wms/purchase-inbound/types'

defineProps<{
  shipping: AvailableShippingVO | undefined
}>()
</script>

<style scoped>
.shipping-info-section {
  padding: 16px 20px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.section-icon {
  margin-right: 8px;
  color: #1890ff;
  font-size: 16px;
}

.received-quantity {
  color: #52c41a;
  font-weight: 600;
}

.pending-quantity {
  color: #1890ff;
  font-weight: 600;
}

.empty-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 0;
  color: #8c8c8c;
}

.empty-icon {
  font-size: 32px;
  margin-bottom: 8px;
  color: #bfbfbf;
}
</style>
