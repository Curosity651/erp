<template>
  <a-modal v-model:open="visible" title="库存不足，无法出库" :footer="null" width="520px">
    <div class="shortage-content">
      <p class="shortage-summary">
        共 <strong>{{ shortages.length }}</strong> 个SKU库存不足：
      </p>
      <a-table
        :data-source="shortages"
        :columns="columns"
        :pagination="false"
        size="small"
        row-key="skuCode"
        :scroll="{ y: 280 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'sku'">
            <div class="sku-cell">
              <span class="sku-code">{{ record.skuCode }}</span>
              <span v-if="record.skuName" class="sku-name">{{ record.skuName }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'shortage'">
            <span class="shortage-value">{{ record.shortage }}</span>
          </template>
        </template>
      </a-table>
    </div>
    <div class="modal-footer">
      <a-button type="primary" @click="handleClose">知道了</a-button>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { StockShortageVO } from '@/api/wms/sales-outbound/types'

const visible = ref(false)
const shortages = ref<StockShortageVO[]>([])

const columns = [
  { title: 'SKU', key: 'sku', width: 200 },
  { title: '需求', dataIndex: 'requiredQty', width: 70, align: 'center' as const },
  { title: '可用', dataIndex: 'availableQty', width: 70, align: 'center' as const },
  { title: '缺口', key: 'shortage', width: 70, align: 'center' as const }
]

const open = (data: StockShortageVO[]) => {
  shortages.value = data
  visible.value = true
}

const handleClose = () => {
  visible.value = false
}

defineExpose({ open })
</script>

<style scoped>
.shortage-content {
  padding: 0 0 16px;
}

.shortage-summary {
  margin-bottom: 12px;
  color: #8c8c8c;
}

.sku-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sku-code {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  font-weight: 500;
  color: #262626;
}

.sku-name {
  font-size: 12px;
  color: #8c8c8c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}

.shortage-value {
  color: #ff4d4f;
  font-weight: 500;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
</style>
