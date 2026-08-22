<template>
  <a-drawer v-model:open="visible" title="报废单详情" :width="720">
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="2" size="small" bordered style="margin-bottom: 16px">
          <a-descriptions-item label="报废单号">{{ detail.adjustmentNo }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-badge :status="badge(detail.orderStatus)" :text="statusText(detail.orderStatus)" />
          </a-descriptions-item>
          <a-descriptions-item label="货主">{{ detail.ownerName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="仓库">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="报废日期">{{ detail.adjustmentDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="报废原因" :span="2">
            {{ detail.adjustmentReason || '-' }}
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.rejectReason" label="驳回原因" :span="2">
            <span style="color: #ff4d4f">{{ detail.rejectReason }}</span>
          </a-descriptions-item>
        </a-descriptions>

        <a-table
          :data-source="detail.items"
          :columns="columns"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'quality'">
              <a-tag :color="record.quality === 'DAMAGED' ? 'red' : 'green'">
                {{ record.quality === 'DAMAGED' ? '次品' : '良品' }}
              </a-tag>
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
import { getAdjustmentDetail } from '@/api/wms/adjustment'
import { ScrapStatusList } from '@/api/wms/adjustment/types'
import type { AdjustmentDetailVO, ScrapStatus } from '@/api/wms/adjustment/types'

defineOptions({ name: 'AdjustmentDetailDrawer' })

const visible = ref(false)
const loading = ref(false)
const detail = ref<AdjustmentDetailVO | null>(null)

const columns = [
  { title: '托盘号', dataIndex: 'palletNo', width: 190, ellipsis: true },
  { title: '托位', dataIndex: 'slotCode', width: 140, ellipsis: true },
  { title: '内部 SKU', dataIndex: 'warehouseSkuCode', width: 190, ellipsis: true },
  { title: '品质', key: 'quality', width: 80, align: 'center' as const },
  { title: '报废数量', dataIndex: 'quantity', width: 90, align: 'right' as const },
  { title: '备注', dataIndex: 'remark', ellipsis: true }
]

const statusText = (s: ScrapStatus) => ScrapStatusList.find(x => x.value === s)?.label || s
const badge = (s: ScrapStatus) => (ScrapStatusList.find(x => x.value === s)?.badge as any) || 'default'

async function open(id: number) {
  visible.value = true
  loading.value = true
  detail.value = null
  try {
    const res = await getAdjustmentDetail(id)
    if (isSuccess(res) && res.data) detail.value = res.data
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
