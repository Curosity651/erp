<template>
  <a-modal
    :open="open"
    title="退货收货 - 清点退回货物"
    :width="640"
    :confirm-loading="submitting"
    ok-text="确认收货"
    @ok="handleConfirm"
    @cancel="handleClose"
  >
    <a-spin :spinning="loading">
      <div v-if="order" class="order-head">
        <span class="ob-no">{{ order.returnNo }}</span>
        <span class="ob-meta">{{ order.ownerName }} · {{ order.warehouseName }}</span>
      </div>

      <a-alert
        type="info"
        show-icon
        style="margin: 12px 0"
        message="录入实际退回数量；确认后转「待质检」，由质检员逐 SKU 判定良品/次品。"
      />

      <a-table :data-source="lines" :pagination="false" row-key="skuCode" size="small">
        <a-table-column title="SKU" :width="200">
          <template #default="{ record }">
            <div>{{ record.warehouseSkuCode || record.skuCode }}</div>
            <div class="sku-name">{{ record.skuName }}</div>
          </template>
        </a-table-column>
        <a-table-column title="应退" data-index="expectedQty" :width="90" align="right" />
        <a-table-column title="实收数" :width="140">
          <template #default="{ record }">
            <a-input-number v-model:value="record.receivedQty" :min="1" :max="record.expectedQty" style="width: 100%" />
          </template>
        </a-table-column>
      </a-table>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { getReturnDetail, receiveReturn } from '@/api/wms/return-qc'
import type { ReturnOrderVO } from '@/api/wms/return-qc/types'

const props = defineProps<{ open: boolean; orderId?: number }>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'success'): void
}>()

const loading = ref(false)
const submitting = ref(false)
const order = ref<ReturnOrderVO | null>(null)
const lines = ref<
  { skuCode: string; skuName?: string; expectedQty: number; receivedQty: number }[]
>([])

async function loadData(id: number) {
  loading.value = true
  order.value = null
  lines.value = []
  try {
    const res = await getReturnDetail(id)
    if (isSuccess(res) && res.data) {
      order.value = res.data
      lines.value = (res.data.items || []).map(i => ({
        skuCode: i.skuCode,
        skuName: i.skuName,
        expectedQty: i.expectedQty,
        receivedQty: i.receivedQty ?? i.expectedQty
      }))
    }
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.open, props.orderId] as const,
  ([open, id]) => {
    if (open && id) loadData(id)
  },
  { immediate: true }
)

function handleClose() {
  emit('update:open', false)
}

async function handleConfirm() {
  if (!order.value) return
  const items = lines.value
    .filter(l => l.receivedQty > 0)
    .map(l => ({ skuCode: l.skuCode, receivedQty: l.receivedQty }))
  if (!items.length) {
    message.warning('请至少录入一条实收数量大于 0 的明细')
    return
  }
  submitting.value = true
  try {
    const res = await receiveReturn({ returnOrderId: order.value.id, items })
    if (isSuccess(res)) {
      message.success('退货收货完成，转待质检')
      emit('success')
      emit('update:open', false)
    } else {
      message.error(res.message || '收货失败')
    }
  } catch (e: any) {
    message.error(e?.message || '收货失败')
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
.sku-name {
  font-size: 12px;
  color: #8c8c8c;
}
</style>
