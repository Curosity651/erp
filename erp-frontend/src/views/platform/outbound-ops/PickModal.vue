<template>
  <a-modal
    :open="open"
    title="下架 - FIFO 分配拣货"
    :width="820"
    :confirm-loading="submitting"
    :ok-button-props="{ disabled: hasShortage }"
    ok-text="确认下架"
    @ok="handleConfirm"
    @cancel="handleClose"
  >
    <a-spin :spinning="loading">
      <!-- 订单头 -->
      <div v-if="order" class="order-head">
        <span class="ob-no">{{ order.outboundNo }}</span>
        <span class="ob-meta">{{ order.ownerName }} · {{ order.warehouseName }}</span>
      </div>

      <!-- 缺货告警（整单挂起） -->
      <a-alert
        v-if="hasShortage"
        type="error"
        show-icon
        style="margin: 12px 0"
        message="存在缺货 SKU：按规则整单挂起（backorder），不可部分下架。请补货后再下架。"
      />

      <!-- 需求 vs 可用 -->
      <div class="section-title">出库明细</div>
      <a-table
        :data-source="order?.items || []"
        :pagination="false"
        row-key="skuCode"
        size="small"
        :row-class-name="rowClass"
      >
        <a-table-column title="SKU" data-index="skuCode" :width="140">
          <template #default="{ record }">
            <div>{{ record.skuCode }}</div>
            <div class="sku-name">{{ record.skuName }}</div>
          </template>
        </a-table-column>
        <a-table-column title="需求数" data-index="requiredQty" :width="80" align="right" />
        <a-table-column title="可用良品" data-index="availableQty" :width="90" align="right" />
        <a-table-column title="缺口" :width="80" align="right">
          <template #default="{ record }">
            <span v-if="record.shortage" class="shortage">
              -{{ record.requiredQty - record.availableQty }}
            </span>
            <span v-else style="color: #52c41a">充足</span>
          </template>
        </a-table-column>
      </a-table>

      <!-- FIFO 分配预览 -->
      <template v-if="!hasShortage">
        <div class="section-title" style="margin-top: 16px">
          FIFO 分配预览
          <span class="hint">（按 入库日期→批次 升序锁定良品批次）</span>
        </div>
        <a-table :data-source="allocations" :pagination="false" row-key="batchNo" size="small">
          <a-table-column title="库位" data-index="locationCode" :width="110" />
          <a-table-column title="SKU" data-index="skuCode" :width="140" />
          <a-table-column title="批次" data-index="batchNo" :width="140" />
          <a-table-column title="入库日" data-index="inboundDate" :width="110" />
          <a-table-column title="取货数" data-index="takeQty" :width="80" align="right" />
        </a-table>

        <!-- 下架模式 + 拣货员 -->
        <a-form :label-col="{ style: { width: '80px' } }" style="margin-top: 16px">
          <a-form-item label="下架模式" required>
            <a-radio-group v-model:value="pickMode" button-style="solid">
              <a-radio-button v-for="m in PICK_MODE_OPTIONS" :key="m.value" :value="m.value">
                {{ m.label }}
              </a-radio-button>
            </a-radio-group>
            <div class="mode-desc">{{ PICK_MODE_DESC[pickMode] }}</div>
          </a-form-item>
          <a-form-item label="拣货员" required>
            <a-select
              v-model:value="pickerId"
              :options="pickerOptions"
              :loading="pickerLoading"
              placeholder="请指定拣货员"
              style="width: 240px"
            />
          </a-form-item>
        </a-form>
      </template>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import {
  getOutboundDetail,
  previewAllocation,
  confirmPick,
  listPickers
} from '@/api/wms/outbound-picking'
import type { OutboundOrderVO, PickAllocationVO, PickMode } from '@/api/wms/outbound-picking/types'
import { PICK_MODE_OPTIONS, PICK_MODE_DESC } from './constants'

const props = defineProps<{ open: boolean; orderId?: number }>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'success'): void
}>()

const loading = ref(false)
const submitting = ref(false)
const order = ref<OutboundOrderVO | null>(null)
const allocations = ref<PickAllocationVO[]>([])

const pickMode = ref<PickMode>('BY_ORDER')
const pickerId = ref<number>()

const pickerLoading = ref(false)
const pickerOptions = ref<{ label: string; value: number }[]>([])

const hasShortage = computed(() => (order.value?.items || []).some(i => i.shortage))

function rowClass(record: { shortage: boolean }) {
  return record.shortage ? 'shortage-row' : ''
}

async function loadPickers() {
  if (pickerOptions.value.length) return
  pickerLoading.value = true
  try {
    const res = await listPickers()
    if (isSuccess(res) && res.data) {
      pickerOptions.value = res.data.map(p => ({ label: p.name, value: p.id }))
    }
  } finally {
    pickerLoading.value = false
  }
}

async function loadData(id: number) {
  loading.value = true
  order.value = null
  allocations.value = []
  pickerId.value = undefined
  pickMode.value = 'BY_ORDER'
  try {
    const res = await getOutboundDetail(id)
    if (isSuccess(res) && res.data) order.value = res.data
    if (!hasShortage.value) {
      const pre = await previewAllocation(id)
      if (isSuccess(pre) && pre.data) allocations.value = pre.data
    }
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.open, props.orderId] as const,
  ([open, id]) => {
    if (open && id) {
      loadPickers()
      loadData(id)
    }
  },
  { immediate: true }
)

function handleClose() {
  emit('update:open', false)
}

async function handleConfirm() {
  if (hasShortage.value || !order.value) return
  if (!pickerId.value) {
    message.warning('请指定拣货员')
    return
  }
  submitting.value = true
  try {
    const res = await confirmPick({
      outboundOrderId: order.value.id,
      pickMode: pickMode.value,
      pickerId: pickerId.value
    })
    if (isSuccess(res)) {
      message.success('下架成功，已生成拣货单')
      emit('success')
      emit('update:open', false)
    } else {
      message.error(res.message || '下架失败')
    }
  } catch (e: any) {
    message.error(e?.message || '下架失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.order-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.ob-no {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}
.ob-meta {
  font-size: 13px;
  color: #8c8c8c;
}
.section-title {
  margin: 12px 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}
.section-title .hint {
  font-size: 12px;
  font-weight: 400;
  color: #8c8c8c;
}
.sku-name {
  font-size: 12px;
  color: #8c8c8c;
}
.shortage {
  color: #ff4d4f;
  font-weight: 600;
}
.mode-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #8c8c8c;
}
:deep(.shortage-row) {
  background: #fff1f0;
}
</style>
