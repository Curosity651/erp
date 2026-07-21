<template>
  <a-modal
    :open="open"
    title="打包 - 配货核验并打印标签"
    :width="720"
    :confirm-loading="submitting"
    ok-text="确认打包"
    @ok="handleConfirm"
    @cancel="handleClose"
  >
    <a-spin :spinning="loading">
      <div v-if="order" class="order-head">
        <span class="ob-no">{{ order.outboundNo }}</span>
        <span class="ob-meta">{{ order.ownerName }} · {{ order.warehouseName }}</span>
      </div>

      <div class="section-title">配货明细</div>
      <a-table :data-source="order?.items || []" :pagination="false" row-key="skuCode" size="small">
        <a-table-column title="SKU" :width="160">
          <template #default="{ record }">
            <div>{{ record.skuCode }}</div>
            <div class="sku-name">{{ record.skuName }}</div>
          </template>
        </a-table-column>
        <a-table-column title="数量" data-index="qty" :width="80" align="right" />
        <a-table-column title="品质" :width="90" align="center">
          <template #default="{ record }">
            <a-tag :color="record.quality === 'GOOD' ? 'green' : 'red'">
              {{ record.quality === 'GOOD' ? '良品' : '次品' }}
            </a-tag>
          </template>
        </a-table-column>
      </a-table>

      <a-form :label-col="{ style: { width: '80px' } }" style="margin-top: 16px">
        <a-form-item label="打包模式" required>
          <a-radio-group v-model:value="packMode" button-style="solid">
            <a-radio-button v-for="m in PACK_MODE_OPTIONS" :key="m.value" :value="m.value">
              {{ m.label }}
            </a-radio-button>
          </a-radio-group>
          <div class="mode-desc">{{ PACK_MODE_DESC[packMode] }}</div>
        </a-form-item>
        <a-form-item label="打包员">
          <a-input v-model:value="packerName" placeholder="选填" style="width: 240px" />
        </a-form-item>
      </a-form>

      <a-alert
        type="info"
        show-icon
        message="确认打包后系统将打印物流标签，订单转为「已打包」，进入待签出。"
      />
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { getPackShipDetail, confirmPack } from '@/api/wms/outbound-shipping'
import type { PackShipOrderVO, PackMode } from '@/api/wms/outbound-shipping/types'
import { PACK_MODE_OPTIONS, PACK_MODE_DESC } from './packship-constants'

const props = defineProps<{ open: boolean; orderId?: number }>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'success'): void
}>()

const loading = ref(false)
const submitting = ref(false)
const order = ref<PackShipOrderVO | null>(null)
const packMode = ref<PackMode>('BY_ORDER')
const packerName = ref<string>()

async function loadData(id: number) {
  loading.value = true
  order.value = null
  packMode.value = 'BY_ORDER'
  packerName.value = undefined
  try {
    const res = await getPackShipDetail(id)
    if (isSuccess(res) && res.data) order.value = res.data
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
  submitting.value = true
  try {
    const res = await confirmPack({
      outboundOrderId: order.value.id,
      packMode: packMode.value,
      packerName: packerName.value
    })
    if (isSuccess(res)) {
      message.success('打包完成，已打印物流标签')
      emit('success')
      emit('update:open', false)
    } else {
      message.error(res.message || '打包失败')
    }
  } catch (e: any) {
    message.error(e?.message || '打包失败')
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
  margin-bottom: 12px;
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
  margin: 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}
.sku-name {
  font-size: 12px;
  color: #8c8c8c;
}
.mode-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #8c8c8c;
}
</style>
