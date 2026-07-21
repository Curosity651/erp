<template>
  <a-drawer v-model:open="visible" title="库位调整单详情" :width="760">
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="2" size="small" bordered style="margin-bottom: 16px">
          <a-descriptions-item label="调整单号">{{ detail.transferNo }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-badge :status="badge(detail.orderStatus)" :text="statusText(detail.orderStatus)" />
          </a-descriptions-item>
          <a-descriptions-item label="所属服务商">{{ detail.operatorName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="货主">{{ detail.ownerName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="仓库">{{ detail.warehouseName || '-' }}</a-descriptions-item>
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
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'quality'">
              <a-tag :color="record.sourceQuality === 'DAMAGED' ? 'red' : 'green'">
                {{ record.sourceQuality === 'DAMAGED' ? '次品' : '良品' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'move'">
              {{ record.sourceLocationCode }} → {{ record.targetLocationCode }}
              <a-tag v-if="record.toGood === 1" color="green" style="margin-left: 4px">置良品</a-tag>
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
import { LocationTransferStatusList } from '@/api/wms/location-transfer/types'
import type { LocationTransferDetailVO, LocationTransferStatus } from '@/api/wms/location-transfer/types'

defineOptions({ name: 'LocationTransferDetailDrawer' })

const visible = ref(false)
const loading = ref(false)
const detail = ref<LocationTransferDetailVO | null>(null)

const columns = [
  { title: 'SKU', dataIndex: 'skuCode', width: 150, ellipsis: true },
  { title: '品质', key: 'quality', width: 70, align: 'center' as const },
  { title: '移库（源→目标）', key: 'move', ellipsis: true },
  { title: '数量', dataIndex: 'quantity', width: 80, align: 'right' as const }
]

const statusText = (s: LocationTransferStatus) =>
  LocationTransferStatusList.find(x => x.value === s)?.label || s
const badge = (s: LocationTransferStatus) =>
  (LocationTransferStatusList.find(x => x.value === s)?.badge as any) || 'default'

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
