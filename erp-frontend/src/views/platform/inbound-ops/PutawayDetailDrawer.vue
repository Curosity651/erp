<template>
  <a-drawer v-model:open="visible" :title="`上架详情 · ${record?.inboundNo || ''}`" :width="860">
    <a-spin :spinning="loading">
      <a-descriptions v-if="record" :column="2" bordered size="small">
        <a-descriptions-item label="入库单号">{{ record.inboundNo }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ record.warehouseName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="货主">{{ record.ownerName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="服务商">{{ record.operatorName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="操作员">{{ record.putawayByName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="上架时间">{{ record.putawayTime || '-' }}</a-descriptions-item>
      </a-descriptions>

      <div class="section-header">
        <div class="section-title">本次上架明细</div>
        <a-button type="primary" :disabled="receiptLines.length === 0" @click="printReceipt">打印上架单</a-button>
      </div>

      <a-table row-key="locationCode" size="small" :columns="columns" :data-source="receiptLines" :pagination="false">
        <template #bodyCell="{ column, record: line }">
          <template v-if="column.key === 'quality'">{{ line.quality === 'DAMAGED' ? '不良品' : '良品' }}</template>
          <template v-else-if="column.key === 'reason'">{{ line.overrideReason || '-' }}</template>
        </template>
      </a-table>

      <a-empty v-if="!loading && receiptLines.length === 0" description="未查询到本次上架明细" class="empty" />
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { getPutawayReceiptLines } from '@/api/wms/inbound-execution'
import type { PutawayReceiptLineVO } from '@/api/wms/inbound-execution'
import type { PurchaseInboundPageVO } from '@/api/wms/purchase-inbound/types'
import { printPutawayReceipt } from './putaway-receipt-print'

const visible = ref(false)
const loading = ref(false)
const record = ref<PurchaseInboundPageVO>()
const receiptLines = ref<PutawayReceiptLineVO[]>([])
const columns = [
  { title: '逻辑库位', dataIndex: 'locationCode', key: 'locationCode', width: 150 },
  { title: '内部 SKU', dataIndex: 'skuCode', key: 'skuCode', width: 210 },
  { title: '品质', key: 'quality', width: 90 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 90, align: 'right' },
  { title: '人工覆盖原因', key: 'reason' }
]

function printReceipt() {
  if (record.value && !printPutawayReceipt(record.value, receiptLines.value)) {
    message.warning('浏览器阻止了打印窗口，请允许本站弹出窗口后重试')
  }
}

async function openDetail(row: PurchaseInboundPageVO) {
  record.value = row
  receiptLines.value = []
  visible.value = true
  loading.value = true
  try {
    const response = await getPutawayReceiptLines(row.id)
    if (isSuccess(response)) receiptLines.value = response.data || []
  } finally {
    loading.value = false
  }
}

defineExpose({ open: openDetail })
</script>

<style scoped>
.section-header { display: flex; align-items: center; justify-content: space-between; margin: 20px 0 12px; }
.section-title { font-size: 15px; font-weight: 600; }
.empty { margin-top: 24px; }
</style>
