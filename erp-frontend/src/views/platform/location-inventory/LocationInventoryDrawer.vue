<template>
  <a-drawer v-model:open="open" title="库位库存详情" :width="760">
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="3" bordered size="small">
          <a-descriptions-item label="库位">{{ detail.location.locationCode }}</a-descriptions-item>
          <a-descriptions-item label="类型">{{ detail.location.locationType || '-' }}</a-descriptions-item>
          <a-descriptions-item label="容量">{{ detail.location.utilizationPercent }}%</a-descriptions-item>
          <a-descriptions-item label="总件数">{{ detail.location.totalQuantity }}</a-descriptions-item>
          <a-descriptions-item label="可用">{{ detail.location.availableQuantity }}</a-descriptions-item>
          <a-descriptions-item label="预占">{{ detail.location.reservedQuantity }}</a-descriptions-item>
          <a-descriptions-item label="已用体积">{{ formatVolume(detail.location.usedVolumeMm3) }}</a-descriptions-item>
          <a-descriptions-item label="库位体积">{{ formatVolume(detail.location.capacityVolumeMm3) }}</a-descriptions-item>
          <a-descriptions-item label="SKU 种类">{{ detail.location.skuKindCount }}</a-descriptions-item>
        </a-descriptions>

        <a-table
          class="detail-table"
          :data-source="detail.items"
          :columns="columns"
          :pagination="false"
          :scroll="{ x: 900 }"
          row-key="inventoryId"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'owner'">
              <div>{{ record.ownerName || record.erpTenantId }}</div>
              <small>{{ record.wmsTenantName || record.wmsTenantId }}</small>
            </template>
            <template v-else-if="column.key === 'sku'">
              <div>{{ record.skuCode }}</div>
              <small>{{ record.skuName || '-' }}</small>
            </template>
            <template v-else-if="column.key === 'outer'">
              <span v-if="record.outerLengthMm">
                {{ record.outerLengthMm }}×{{ record.outerWidthMm }}×{{ record.outerHeightMm }} mm
              </span>
              <span v-else>-</span>
            </template>
          </template>
        </a-table>
      </template>
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { getLocationInventoryDetail } from '@/api/wms/location-inventory'
import type { LocationInventoryDetail } from '@/api/wms/location-inventory/types'
import { formatVolume } from './location-utilization'

const open = ref(false)
const loading = ref(false)
const detail = ref<LocationInventoryDetail>()

const columns = [
  { title: '货主 / 服务商', key: 'owner', width: 160 },
  { title: 'SKU', key: 'sku', width: 190 },
  { title: '品质', dataIndex: 'quality', key: 'quality', width: 90 },
  { title: '总数', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '预占', dataIndex: 'reservedQuantity', key: 'reservedQuantity', width: 80 },
  { title: '可用', dataIndex: 'availableQuantity', key: 'availableQuantity', width: 80 },
  { title: '外箱尺寸', key: 'outer', width: 210 }
]

const show = async (locationId: number) => {
  open.value = true
  loading.value = true
  detail.value = undefined
  try {
    const response = await getLocationInventoryDetail(locationId)
    if (!isSuccess(response)) {
      message.error(response.message || '库位详情加载失败')
      return
    }
    detail.value = response.data
  } finally {
    loading.value = false
  }
}

defineExpose({ show })
</script>

<style scoped>
.detail-table {
  margin-top: 18px;
}

small {
  color: #8c8c8c;
}
</style>
