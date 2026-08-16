<template>
  <a-drawer v-model:open="visible" title="库位调整单详情" :width="980">
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="2" size="small" bordered style="margin-bottom: 16px">
          <a-descriptions-item label="调整单号">{{ detail.transferNo }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-badge :status="badge(detail.orderStatus)" :text="statusText(detail.orderStatus)" />
          </a-descriptions-item>
          <a-descriptions-item label="所属服务商">
            {{ detail.operatorName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="货主">{{ detail.ownerName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="仓库">{{ detail.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="操作员">
            {{ detail.operatorUserName || detail.createByName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="调整原因">
            {{ reasonText(detail.reasonCode, detail.reason) }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.completeTime" label="完成时间">
            {{ detail.completeTime }}
          </a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</a-descriptions-item>
        </a-descriptions>

        <a-table
          :data-source="detail.items"
          :columns="columns"
          :pagination="false"
          row-key="id"
          size="small"
          :scroll="{ x: 900 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'sku'">
              <div>{{ record.warehouseSkuCode || record.skuCode }}</div>
              <div class="subtle">ERP SKU：{{ record.skuCode }}</div>
            </template>
            <template v-else-if="column.key === 'source'">
              <div>{{ record.sourceLocationCode || '-' }}</div>
              <div class="subtle">{{ record.sourceZoneName || '未设置分区' }}</div>
            </template>
            <template v-else-if="column.key === 'target'">
              <div>{{ record.targetLocationCode || '-' }}</div>
              <div class="subtle">{{ record.targetZoneName || '未设置分区' }}</div>
            </template>
            <template v-else-if="column.key === 'quality'">
              <a-tag color="green">良品</a-tag>
            </template>
          </template>
        </a-table>
      </template>
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { isSuccess } from '@/api'
import { getLocationTransferDetail } from '@/api/wms/location-transfer'
import {
  LocationTransferReasonList,
  LocationTransferStatusList
} from '@/api/wms/location-transfer/types'
import type {
  LocationTransferDetailVO,
  LocationTransferStatus
} from '@/api/wms/location-transfer/types'

defineOptions({ name: 'LocationTransferDetailDrawer' })

const visible = ref(false)
const loading = ref(false)
const detail = ref<LocationTransferDetailVO | null>(null)

const columns = [
  { title: 'SKU', key: 'sku', width: 240, fixed: 'left' as const },
  { title: '品质', key: 'quality', width: 90 },
  { title: '源库位', key: 'source', width: 190 },
  { title: '目标库位', key: 'target', width: 190 },
  { title: '数量', dataIndex: 'quantity', width: 100, align: 'right' as const },
  { title: '备注', dataIndex: 'remark', width: 160, ellipsis: true }
]

const statusText = (status: LocationTransferStatus) =>
  LocationTransferStatusList.find(item => item.value === status)?.label || status
const badge = (status: LocationTransferStatus) =>
  (LocationTransferStatusList.find(item => item.value === status)?.badge as any) || 'default'

function reasonText(code?: string, reason?: string) {
  if (reason) return reason
  return LocationTransferReasonList.find(item => item.value === code)?.label || code || '-'
}

async function open(id: number) {
  visible.value = true
  loading.value = true
  detail.value = null
  try {
    const res = await getLocationTransferDetail(id)
    if (isSuccess(res) && res.data) detail.value = res.data
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.subtle {
  color: rgb(0 0 0 / 45%);
  font-size: 12px;
}
</style>
