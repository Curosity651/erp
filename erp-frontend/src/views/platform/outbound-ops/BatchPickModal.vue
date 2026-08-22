<template>
  <a-modal
    :open="open"
    title="批量创建拣货任务"
    :width="920"
    :confirm-loading="submitting"
    :ok-button-props="{ disabled: !preview || !pickerId }"
    ok-text="确认生成任务"
    @ok="handleCreate"
    @cancel="handleClose"
  >
    <a-spin :spinning="loading">
      <a-alert
        type="info"
        show-icon
        message="默认拣货完成后直接按平台订单复核打包。大型波次需要临时分货时，可在下方主动启用格口。"
        style="margin-bottom: 16px"
      />

      <a-row v-if="preview" :gutter="12" class="metrics">
        <a-col :span="4"><a-statistic title="已选出库单" :value="preview.selectedOrderCount" /></a-col>
        <a-col :span="4"><a-statistic title="销售订单" :value="preview.selectedSalesOrderCount" /></a-col>
        <a-col :span="4"><a-statistic title="生成任务" :value="preview.taskCount" /></a-col>
        <a-col :span="4"><a-statistic title="总件数" :value="preview.totalQuantity" /></a-col>
        <a-col :span="4"><a-statistic title="整托" :value="preview.wholePalletCount" /></a-col>
        <a-col :span="4"><a-statistic title="格口包裹" :value="preview.secondaryOrderCount" /></a-col>
      </a-row>

      <div v-if="preview" class="section-title">自动拆分结果</div>
      <a-table
        v-if="preview"
        :data-source="preview.tasks"
        :pagination="false"
        row-key="outboundOrderIds"
        size="small"
        :scroll="{ y: 280 }"
      >
        <a-table-column title="仓库 / 货主" :width="220">
          <template #default="{ record }">
            <div>{{ record.warehouseName }}</div>
            <div class="muted">{{ record.ownerName }}</div>
          </template>
        </a-table-column>
        <a-table-column title="类型" :width="90">
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
                    ? '波次拣货'
                    : record.taskType === 'PALLET_DIRECT'
                      ? '整托直发'
                      : '按单拣货'
                }}
            </a-tag>
          </template>
        </a-table-column>
        <a-table-column title="出库单" data-index="orderCount" :width="70" align="right" />
        <a-table-column title="销售订单" data-index="salesOrderCount" :width="80" align="right" />
        <a-table-column title="SKU" data-index="skuCount" :width="65" align="right" />
        <a-table-column title="件数" data-index="totalQuantity" :width="65" align="right" />
        <a-table-column title="整托" data-index="wholePalletCount" :width="65" align="right" />
        <a-table-column title="分货包裹" data-index="secondaryOrderCount" :width="82" align="right" />
      </a-table>

      <a-form layout="vertical" class="task-form">
        <a-row :gutter="16">
          <a-col :span="9">
            <a-form-item label="统一指定拣货员" required>
              <a-select
                v-model:value="pickerId"
                :options="pickerOptions"
                :loading="pickerLoading"
                placeholder="请选择当前平台拣货员"
              />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="单任务最大包裹数">
              <a-input-number v-model:value="maxOrders" :min="1" :max="50" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="整托优先">
              <a-switch v-model:checked="wholePalletPriority" />
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item label="格口分货">
              <a-switch v-model:checked="useSortSlots" />
              <span class="switch-copy">{{ useSortSlots ? '启用' : '不启用' }}</span>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { createBatchPick, listPickers, previewBatchPick } from '@/api/wms/outbound-picking'
import type { BatchPickPreviewVO } from '@/api/wms/outbound-picking/types'

const props = defineProps<{ open: boolean; orderIds: number[] }>()
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
      message.success(
        `已生成 ${res.data.taskCount} 个任务（${taskNos}），包含 ${res.data.orderCount} 张出库单、${res.data.salesOrderCount} 个销售订单`
      )
      emit('success')
      emit('update:open', false)
    } else {
      message.error(res.message || '创建拣货任务失败')
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
