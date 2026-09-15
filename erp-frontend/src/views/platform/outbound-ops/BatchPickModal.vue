<template>
  <a-modal
    :open="open"
    :title="t('platform.batchPick.title')"
    :width="920"
    :confirm-loading="submitting"
    :ok-button-props="{ disabled: !preview || !pickerId }"
    :ok-text="t('platform.batchPick.confirm')"
    :cancel-text="t('platform.common.cancel')"
    @ok="handleCreate"
    @cancel="handleClose"
  >
    <a-spin :spinning="loading">
      <a-alert
        type="info"
        show-icon
        :message="t('platform.batchPick.description')"
        style="margin-bottom: 16px"
      />

      <a-row v-if="preview" :gutter="12" class="metrics">
        <a-col :span="4"><a-statistic :title="t('platform.batchPick.selected')" :value="preview.selectedOrderCount" /></a-col>
        <a-col :span="4"><a-statistic :title="t('platform.batchPick.salesOrders')" :value="preview.selectedSalesOrderCount" /></a-col>
        <a-col :span="4"><a-statistic :title="t('platform.batchPick.tasks')" :value="preview.taskCount" /></a-col>
        <a-col :span="4"><a-statistic :title="t('platform.picking.totalPieces')" :value="preview.totalQuantity" /></a-col>
        <a-col :span="4"><a-statistic :title="t('platform.batchPick.wholePallet')" :value="preview.wholePalletCount" /></a-col>
        <a-col :span="4"><a-statistic :title="t('platform.batchPick.sortPackages')" :value="preview.secondaryOrderCount" /></a-col>
      </a-row>

      <div v-if="preview" class="section-title">{{ t('platform.batchPick.splitResult') }}</div>
      <a-table
        v-if="preview"
        :data-source="preview.tasks"
        :pagination="false"
        row-key="outboundOrderIds"
        size="small"
        :scroll="{ y: 280 }"
      >
        <a-table-column :title="t('platform.batchPick.warehouseOwner')" :width="220">
          <template #default="{ record }">
            <div>{{ record.warehouseName }}</div>
            <div class="muted">{{ record.ownerName }}</div>
          </template>
        </a-table-column>
        <a-table-column :title="t('platform.batchPick.type')" :width="90">
          <template #default="{ record }">
              <a-tag
                :color="
                  record.taskType === 'WAVE'
                    ? 'blue'
                    : record.taskType === 'PALLET_DIRECT'
                      ? 'green'
                      : 'default'
                "
              >
                {{
                  record.taskType === 'WAVE'
                    ? t('platform.outbound.mode.wave')
                    : record.taskType === 'PALLET_DIRECT'
                      ? t('platform.batchPick.palletDirect')
                      : t('platform.outbound.mode.single')
                }}
            </a-tag>
          </template>
        </a-table-column>
        <a-table-column :title="t('platform.batchPick.outboundOrders')" data-index="orderCount" :width="70" align="right" />
        <a-table-column :title="t('platform.batchPick.salesOrders')" data-index="salesOrderCount" :width="80" align="right" />
        <a-table-column title="SKU" data-index="skuCount" :width="65" align="right" />
        <a-table-column :title="t('platform.batchPick.pieces')" data-index="totalQuantity" :width="65" align="right" />
        <a-table-column :title="t('platform.batchPick.wholePallet')" data-index="wholePalletCount" :width="65" align="right" />
        <a-table-column :title="t('platform.batchPick.sortedPackages')" data-index="secondaryOrderCount" :width="82" align="right" />
      </a-table>

      <a-form layout="vertical" class="task-form">
        <a-row :gutter="16">
          <a-col :span="9">
            <a-form-item :label="t('platform.batchPick.unifiedPicker')" required>
              <a-select
                v-model:value="pickerId"
                :options="pickerOptions"
                :loading="pickerLoading"
                :placeholder="t('platform.batchPick.selectPicker')"
              />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item :label="t('platform.batchPick.maxPackages')">
              <a-input-number v-model:value="maxOrders" :min="1" :max="50" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item :label="t('platform.batchPick.palletPriority')">
              <a-switch v-model:checked="wholePalletPriority" />
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item :label="t('platform.batchPick.sortSlots')">
              <a-switch v-model:checked="useSortSlots" />
              <span class="switch-copy">{{ useSortSlots ? t('platform.batchPick.enabled') : t('platform.batchPick.disabled') }}</span>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { createBatchPick, listPickers, previewBatchPick } from '@/api/wms/outbound-picking'
import type { BatchPickPreviewVO } from '@/api/wms/outbound-picking/types'

const props = defineProps<{ open: boolean; orderIds: number[] }>()
const { t } = useI18n()
const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const loading = ref(false)
const submitting = ref(false)
const pickerLoading = ref(false)
const pickerId = ref<number>()
const pickerOptions = ref<{ label: string; value: number }[]>([])
const maxOrders = ref(20)
const wholePalletPriority = ref(true)
const useSortSlots = ref(false)
const preview = ref<BatchPickPreviewVO | null>(null)

async function loadPreview() {
  if (!props.orderIds.length) return
  loading.value = true
  preview.value = null
  try {
    const res = await previewBatchPick({
      outboundOrderIds: props.orderIds,
      maxOrdersPerTask: maxOrders.value,
      wholePalletPriority: wholePalletPriority.value,
      useSortSlots: useSortSlots.value
    })
    if (isSuccess(res) && res.data) preview.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadPickers() {
  if (pickerOptions.value.length) return
  pickerLoading.value = true
  try {
    const res = await listPickers()
    if (isSuccess(res) && res.data) {
      pickerOptions.value = res.data.map(item => ({ label: item.name, value: item.id }))
    }
  } finally {
    pickerLoading.value = false
  }
}

watch(
  () => [props.open, props.orderIds] as const,
  ([open]) => {
    if (!open) return
    pickerId.value = undefined
    maxOrders.value = 20
    wholePalletPriority.value = true
    useSortSlots.value = false
    loadPickers()
    loadPreview()
  },
  { immediate: true }
)

watch([maxOrders, wholePalletPriority, useSortSlots], () => {
  if (props.open) loadPreview()
})

function handleClose() {
  emit('update:open', false)
}

async function handleCreate() {
  if (!pickerId.value || !preview.value) return
  submitting.value = true
  try {
    const res = await createBatchPick({
      outboundOrderIds: props.orderIds,
      pickerId: pickerId.value,
      maxOrdersPerTask: maxOrders.value,
      wholePalletPriority: wholePalletPriority.value,
      useSortSlots: useSortSlots.value
    })
    if (isSuccess(res) && res.data) {
      const taskNos = res.data.taskNos.join('、')
      message.success(t('platform.batchPick.success', {
        tasks: res.data.taskCount,
        numbers: taskNos,
        outbounds: res.data.orderCount,
        orders: res.data.salesOrderCount
      }))
      emit('success')
      emit('update:open', false)
    } else {
      message.error(res.message || t('platform.batchPick.failed'))
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.metrics {
  padding: 12px 4px;
  border: 1px solid #f0f0f0;
}
.section-title {
  margin: 18px 0 8px;
  font-weight: 600;
}
.muted {
  margin-top: 2px;
  color: #8c8c8c;
  font-size: 12px;
}
.task-form {
  margin-top: 18px;
}
.switch-copy {
  margin-left: 8px;
  color: #8c8c8c;
  font-size: 12px;
}
</style>
