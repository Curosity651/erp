<template>
  <a-modal
    :open="open"
    :title="
      readonly ? t('platform.return.process.resultTitle') : t('platform.return.process.title')
    "
    :width="1120"
    :footer="readonly ? null : undefined"
    :confirm-loading="submitting"
    :ok-button-props="{ disabled: hasMissingGlobalSku }"
    :ok-text="t('platform.return.process.confirm')"
    :cancel-text="t('platform.common.cancel')"
    @ok="submit"
    @cancel="close"
  >
    <a-spin :spinning="loading">
      <a-alert
        v-if="!readonly"
        type="info"
        show-icon
        :message="t('platform.return.process.description')"
        style="margin-bottom: 16px"
      />
      <a-descriptions v-if="order" size="small" :column="4" bordered style="margin-bottom: 16px">
        <a-descriptions-item :label="t('platform.return.process.order')">{{
          order.returnNo
        }}</a-descriptions-item>
        <a-descriptions-item :label="t('platform.return.process.batch')">{{
          order.returnBatchNo || '-'
        }}</a-descriptions-item>
        <a-descriptions-item :label="t('platform.common.owner')">{{
          order.ownerName
        }}</a-descriptions-item>
        <a-descriptions-item :label="t('platform.common.warehouse')">{{
          order.warehouseName
        }}</a-descriptions-item>
      </a-descriptions>
      <a-table
        :data-source="lines"
        :pagination="false"
        row-key="id"
        size="small"
        :scroll="{ x: 1040 }"
      >
        <a-table-column :title="t('platform.return.receipt.globalSku')" :width="220"
          ><template #default="{ record }"
            ><strong v-if="record.warehouseSkuCode">{{ record.warehouseSkuCode }}</strong>
            <a-tag v-else color="error">{{ t('platform.return.process.globalSkuMissing') }}</a-tag>
            <div class="muted">{{ record.skuName }}</div></template
          ></a-table-column
        >
        <a-table-column :title="t('platform.return.process.ownerDecision')" :width="210"
          ><template #default="{ record }"
            >{{
              t('platform.return.process.decisionSummary', {
                restock: record.restockQty,
                rework: record.reworkQty,
                scrap: record.scrapQty
              })
            }}
            <div class="muted">{{ record.dispositionRemark || '-' }}</div></template
          ></a-table-column
        >
        <a-table-column :title="t('platform.return.process.reworkPass')" :width="110"
          ><template #default="{ record }"
            ><span v-if="readonly">{{ record.reworkPassQty || 0 }}</span
            ><a-input-number
              v-else
              v-model:value="record.reworkPassQty"
              :min="0"
              :max="record.reworkQty"
              style="width: 90px"
              @change="syncRework(record)" /></template
        ></a-table-column>
        <a-table-column :title="t('platform.return.process.reworkScrap')" :width="100"
          ><template #default="{ record }">{{
            readonly ? record.reworkScrapQty || 0 : record.reworkScrapQty
          }}</template></a-table-column
        >
        <a-table-column :title="t('platform.return.process.finalRestock')" :width="100"
          ><template #default="{ record }">{{ stockQty(record) }}</template></a-table-column
        >
        <a-table-column :title="t('platform.return.process.zone')" :width="130"
          ><template #default="{ record }"
            ><span v-if="readonly">{{ zoneText(record.targetZone) }}</span
            ><a-select
              v-else
              v-model:value="record.targetZone"
              :disabled="stockQty(record) === 0"
              :options="zoneOptions"
              style="width: 110px"
              @change="record.targetLocationCode = undefined" /></template
        ></a-table-column>
        <a-table-column :title="t('platform.return.process.location')" :width="160"
          ><template #default="{ record }"
            ><span v-if="readonly">{{ record.processedLocationCode || '-' }}</span
            ><a-select
              v-else
              v-model:value="record.targetLocationCode"
              :disabled="stockQty(record) === 0"
              show-search
              :options="locationOptions(record.targetZone)"
              style="width: 140px" /></template
        ></a-table-column>
      </a-table>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  getAvailableLocations,
  getReturnDetail,
  processReturnDisposition
} from '@/api/wms/return-qc'
import type { ReturnOrderVO } from '@/api/wms/return-qc/types'
import { isSuccess } from '@/api'

const props = defineProps<{ open: boolean; orderId?: number; readonly?: boolean }>()
const emit = defineEmits<{ (e: 'update:open', value: boolean): void; (e: 'success'): void }>()
const { t } = useI18n()
const loading = ref(false)
const submitting = ref(false)
const order = ref<ReturnOrderVO | null>(null)
const lines = ref<any[]>([])
const locations = reactive<Record<string, string[]>>({ RETURN: [], STANDARD: [] })
const hasMissingGlobalSku = computed(() => lines.value.some(line => !line.warehouseSkuCode))
const zoneOptions = computed(() => [
  { label: t('platform.return.process.returnZone'), value: 'RETURN' },
  { label: t('platform.return.process.standardZone'), value: 'STANDARD' }
])
const stockQty = (line: any) => Number(line.restockQty || 0) + Number(line.reworkPassQty || 0)
const zoneText = (zone?: string) =>
  zone === 'STANDARD'
    ? t('platform.return.process.standardZone')
    : zone === 'RETURN'
      ? t('platform.return.process.returnZone')
      : '-'
const locationOptions = (zone?: string) =>
  (locations[zone || ''] || []).map(value => ({ label: value, value }))
function syncRework(line: any) {
  line.reworkPassQty = Number(line.reworkPassQty || 0)
  line.reworkScrapQty = Number(line.reworkQty || 0) - line.reworkPassQty
}
function close() {
  emit('update:open', false)
}

async function load(id: number) {
  loading.value = true
  try {
    const res = await getReturnDetail(id)
    if (!isSuccess(res) || !res.data) return
    order.value = res.data
    lines.value = (res.data.items || []).map(item => ({
      ...item,
      reworkPassQty: item.reworkPassQty || 0,
      reworkScrapQty: props.readonly ? item.reworkScrapQty || 0 : item.reworkQty || 0,
      targetZone: item.qualifiedZone || 'RETURN'
    }))
    if (!props.readonly) {
      const result = await Promise.all(
        ['RETURN', 'STANDARD'].map(zone => getAvailableLocations(id, res.data!.warehouseId, zone))
      )
      result.forEach((value, index) => {
        if (isSuccess(value) && value.data)
          locations[index === 0 ? 'RETURN' : 'STANDARD'] = value.data
      })
    }
  } finally {
    loading.value = false
  }
}
watch(
  () => [props.open, props.orderId] as const,
  ([open, id]) => {
    if (open && id) load(id)
  },
  { immediate: true }
)

async function submit() {
  if (!order.value) return
  if (hasMissingGlobalSku.value) return message.error(t('platform.return.process.globalSkuMissing'))
  for (const line of lines.value) {
    if (
      Number(line.reworkPassQty || 0) + Number(line.reworkScrapQty || 0) !==
      Number(line.reworkQty || 0)
    )
      return message.warning(
        t('platform.return.process.incompleteRework', { sku: line.warehouseSkuCode })
      )
    if (stockQty(line) > 0 && (!line.targetZone || !line.targetLocationCode))
      return message.warning(
        t('platform.return.process.selectLocation', { sku: line.warehouseSkuCode })
      )
  }
  submitting.value = true
  try {
    const res = await processReturnDisposition({
      returnOrderId: order.value.id,
      items: lines.value.map(line => ({
        itemId: line.id,
        reworkPassQty: Number(line.reworkPassQty || 0),
        reworkScrapQty: Number(line.reworkScrapQty || 0),
        targetZone: line.targetZone,
        targetLocationCode: line.targetLocationCode
      }))
    })
    if (!isSuccess(res)) return message.error(res.message || t('platform.return.process.failed'))
    message.success(t('platform.return.process.success'))
    emit('success')
    close()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.muted {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 3px;
}
</style>
