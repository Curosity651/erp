<template>
  <a-drawer v-model:open="open" :title="t('platform.location.detailTitle')" :width="760">
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="3" bordered size="small">
          <a-descriptions-item :label="t('platform.location.location')">{{ detail.location.locationCode }}</a-descriptions-item>
          <a-descriptions-item :label="t('platform.location.type')">{{ detail.location.locationType || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="t('platform.location.capacity')">{{ detail.location.utilizationPercent }}%</a-descriptions-item>
          <a-descriptions-item :label="t('platform.location.totalPieces')">{{ detail.location.totalQuantity }}</a-descriptions-item>
          <a-descriptions-item :label="t('platform.location.available')">{{ detail.location.availableQuantity }}</a-descriptions-item>
          <a-descriptions-item :label="t('platform.location.reservedShort')">{{ detail.location.reservedQuantity }}</a-descriptions-item>
          <a-descriptions-item :label="t('platform.location.usedVolume')">{{ formatVolume(detail.location.usedVolumeMm3) }}</a-descriptions-item>
          <a-descriptions-item :label="t('platform.location.locationVolume')">{{ formatVolume(detail.location.capacityVolumeMm3) }}</a-descriptions-item>
          <a-descriptions-item :label="t('platform.location.skuKinds')">{{ detail.location.skuKindCount }}</a-descriptions-item>
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
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { getLocationInventoryDetail } from '@/api/wms/location-inventory'
import type { LocationInventoryDetail } from '@/api/wms/location-inventory/types'
import { formatVolume } from './location-utilization'

const open = ref(false)
const { t } = useI18n()
const loading = ref(false)
const detail = ref<LocationInventoryDetail>()

const columns = computed(() => [
  { title: t('platform.location.ownerProvider'), key: 'owner', width: 160 },
  { title: t('platform.common.sku'), key: 'sku', width: 190 },
  { title: t('platform.location.quality'), dataIndex: 'quality', key: 'quality', width: 90 },
  { title: t('platform.location.total'), dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: t('platform.location.reservedShort'), dataIndex: 'reservedQuantity', key: 'reservedQuantity', width: 80 },
  { title: t('platform.location.available'), dataIndex: 'availableQuantity', key: 'availableQuantity', width: 80 },
  { title: t('platform.location.outerSize'), key: 'outer', width: 210 }
])

const show = async (locationId: number) => {
  open.value = true
  loading.value = true
  detail.value = undefined
  try {
    const response = await getLocationInventoryDetail(locationId)
    if (!isSuccess(response)) {
      message.error(response.message || t('platform.location.detailLoadFailed'))
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
