<template>
  <a-modal
    :open="open"
    title="订单签出"
    :width="640"
    :confirm-loading="submitting"
    ok-text="订单签出"
    @ok="handleConfirm"
    @cancel="handleClose"
  >
    <a-spin :spinning="loading">
      <div v-if="order" class="order-head">
        <span class="ob-no">{{ order.outboundNo }}</span>
        <span class="ob-meta">
          {{ order.ownerName }} · {{ order.warehouseName }} · {{ order.skuKinds }}种{{
            order.totalQty
          }}件
        </span>
      </div>

      <a-alert
        type="warning"
        show-icon
        style="margin: 12px 0"
        message="签出即出库：物理库存正式扣减、释放锁定，并按整托/箱件生成仓储操作费。"
      />

      <a-form :label-col="{ style: { width: '90px' } }">
        <a-form-item label="物流渠道" required>
          <a-select
            v-model:value="channel"
            :options="channelOptions"
            :loading="channelLoading"
            placeholder="选择渠道（可自动）"
            style="width: 260px"
          />
        </a-form-item>
        <a-form-item label="面单跟踪号">
          <a-input
            v-model:value="trackingNo"
            placeholder="选填：扫描或录入面单跟踪号"
            style="width: 260px"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="称重(kg)" required>
          <a-input-number v-model:value="weight" :min="0.01" :step="0.1" style="width: 180px" />
        </a-form-item>
        <a-divider orientation="left" plain>仓储操作计费</a-divider>
        <a-form-item v-if="order.handlingPreview?.wholePallets.length" label="整托">
          <a-checkbox-group v-model:value="fullPalletIds">
            <a-space direction="vertical">
              <a-checkbox
                v-for="pallet in order.handlingPreview.wholePallets"
                :key="pallet.palletId"
                :value="pallet.palletId"
              >
                {{ pallet.palletNo }} · {{ pallet.quantity }}件
              </a-checkbox>
            </a-space>
          </a-checkbox-group>
        </a-form-item>
        <a-form-item label="大箱件数">
          <a-input-number v-model:value="largeBoxCount" :min="0" :precision="0" />
          <span class="fee-hint">10元/箱，一箱按一件</span>
        </a-form-item>
        <a-form-item label="小件数量">
          <a-input-number v-model:value="smallItemCount" :min="0" :precision="0" />
          <span class="fee-hint">1元/件</span>
        </a-form-item>
        <a-alert
          type="info"
          show-icon
          :message="`非整托共 ${looseQuantity} 件，大箱与小件合计必须一致。预计操作费 ${operationFee.toFixed(2)} 元`"
          style="margin-bottom: 16px"
        />
        <a-form-item v-if="order?.needPhoto" label="签出照片" required>
          <a-upload
            v-model:file-list="fileList"
            list-type="picture-card"
            :before-upload="() => false"
            accept="image/*"
          >
            <div v-if="fileList.length < 5">
              <plus-outlined />
              <div style="margin-top: 4px">上传</div>
            </div>
          </a-upload>
          <div class="photo-hint">该订单含次品/电子类，按 BR-04 签出必须拍照上传。</div>
        </a-form-item>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { UploadProps } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { getPackShipDetail, confirmShip, listChannels } from '@/api/wms/outbound-shipping'
import type { PackShipOrderVO } from '@/api/wms/outbound-shipping/types'
import { formatMoney } from './packship-constants'

const props = defineProps<{ open: boolean; orderId?: number }>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'success'): void
}>()

const loading = ref(false)
const submitting = ref(false)
const order = ref<PackShipOrderVO | null>(null)

const channel = ref<string>('AUTO')
const trackingNo = ref<string>()
const weight = ref<number>()
const fileList = ref<UploadProps['fileList']>([])
const fullPalletIds = ref<number[]>([])
const largeBoxCount = ref(0)
const smallItemCount = ref(0)

const selectedWholeQuantity = computed(() => {
  const selected = new Set(fullPalletIds.value)
  return (order.value?.handlingPreview?.wholePallets || [])
    .filter(item => selected.has(item.palletId))
    .reduce((sum, item) => sum + item.quantity, 0)
})

const looseQuantity = computed(() =>
  Math.max(
    Number(order.value?.handlingPreview?.wholePalletQuantity || 0) +
      Number(order.value?.handlingPreview?.looseQuantity || 0) -
      selectedWholeQuantity.value,
    0
  )
)

const operationFee = computed(
  () => fullPalletIds.value.length * 80 + largeBoxCount.value * 10 + smallItemCount.value
)

const channelLoading = ref(false)
const channelOptions = ref<{ label: string; value: string }[]>([])

async function loadChannels() {
  if (channelOptions.value.length) return
  channelLoading.value = true
  try {
    const res = await listChannels()
    if (isSuccess(res) && res.data) {
      channelOptions.value = res.data.map(c => ({ label: c.name, value: c.code }))
    }
  } finally {
    channelLoading.value = false
  }
}

async function loadData(id: number) {
  loading.value = true
  order.value = null
  channel.value = 'AUTO'
  trackingNo.value = undefined
  weight.value = undefined
  fileList.value = []
  fullPalletIds.value = []
  largeBoxCount.value = 0
  smallItemCount.value = 0
  try {
    const res = await getPackShipDetail(id)
    if (isSuccess(res) && res.data) {
      order.value = res.data
      fullPalletIds.value = (res.data.handlingPreview?.wholePallets || []).map(item => item.palletId)
      smallItemCount.value = res.data.handlingPreview?.looseQuantity || 0
    }
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.open, props.orderId] as const,
  ([open, id]) => {
    if (open && id) {
      loadChannels()
      loadData(id)
    }
  },
  { immediate: true }
)

function handleClose() {
  emit('update:open', false)
}

async function handleConfirm() {
  if (!order.value) return
  if (!weight.value || weight.value <= 0) {
    message.warning('请录入称重')
    return
  }
  const photoCount = fileList.value?.length || 0
  if (order.value.needPhoto && photoCount === 0) {
    message.warning('该订单含次品/电子类，签出必须上传照片')
    return
  }
  if (largeBoxCount.value + smallItemCount.value !== looseQuantity.value) {
    message.warning(`大箱与小件合计必须等于非整托数量 ${looseQuantity.value}`)
    return
  }
  submitting.value = true
  try {
    const res = await confirmShip({
      outboundOrderId: order.value.id,
      channel: channel.value,
      trackingNo: trackingNo.value?.trim() || undefined,
      weight: weight.value,
      photoCount,
      fullPalletIds: fullPalletIds.value,
      largeBoxCount: largeBoxCount.value,
      smallItemCount: smallItemCount.value
    })
    if (isSuccess(res) && res.data) {
      const fee = res.data.shippingFee
      const operationFee = res.data.warehouseOperationFee || 0
      message.success(
        `签出成功，库存已扣减；仓储操作费 ¥${operationFee.toFixed(2)}${
          fee > 0 ? `，物流费 ${formatMoney(fee)}` : ''
        }`
      )
      emit('success')
      emit('update:open', false)
    } else {
      message.error(res.message || '签出失败')
    }
  } catch (e: any) {
    message.error(e?.message || '签出失败')
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
.photo-hint {
  font-size: 12px;
  color: #fa8c16;
}
.fee-hint {
  margin-left: 10px;
  color: #8c8c8c;
  font-size: 12px;
}
</style>
