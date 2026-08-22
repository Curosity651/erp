<template>
  <a-modal
    :open="open"
    title="平台订单签出"
    :width="560"
    ok-text="确认签出"
    :confirm-loading="submitting"
    @ok="handleConfirm"
    @cancel="handleClose"
  >
    <a-alert
      type="warning"
      show-icon
      message="签出后将扣减当前平台订单包裹对应的库存，操作不可重复。"
      style="margin-bottom: 16px"
    />

    <a-descriptions v-if="record" :column="2" size="small" bordered>
      <a-descriptions-item label="平台订单" :span="2">
        {{ record.platformOrderId }}
      </a-descriptions-item>
      <a-descriptions-item label="平台">
        {{ platformText(record.platform) }}
      </a-descriptions-item>
      <a-descriptions-item label="店铺">
        {{ record.shopName || '-' }}
      </a-descriptions-item>
      <a-descriptions-item label="货主">
        {{ record.ownerName }}
      </a-descriptions-item>
      <a-descriptions-item label="商品">
        {{ record.skuKinds }} 种 · {{ record.totalQty }} 件
      </a-descriptions-item>
    </a-descriptions>

    <a-form layout="vertical" style="margin-top: 16px">
      <a-form-item label="签出方式" required>
        <a-select
          v-model:value="channel"
          :options="channelOptions"
          :loading="channelLoading"
          placeholder="请选择签出方式"
        />
        <div class="field-hint">系统已自动带出默认值，特殊情况可以人工修改。</div>
      </a-form-item>
      <a-form-item label="跟踪号">
        <a-input
          v-model:value="trackingNo"
          placeholder="选填，可扫描或录入面单跟踪号"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="重量（kg）" required>
        <a-input-number
          v-model:value="weight"
          :min="0.01"
          :step="0.1"
          :precision="3"
          style="width: 100%"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { confirmShipPackage, listChannels } from '@/api/wms/outbound-shipping'
import type { PackShipPackagePageVO } from '@/api/wms/outbound-shipping/types'

const props = defineProps<{ open: boolean; record?: PackShipPackagePageVO }>()
const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const submitting = ref(false)
const channelLoading = ref(false)
const channel = ref('AUTO')
const trackingNo = ref<string>()
const weight = ref<number>()
const channelOptions = ref<Array<{ label: string; value: string }>>([])

function platformText(platform?: string) {
  if (!platform) return '-'
  const normalized = platform.toLowerCase()
  if (normalized === 'ozon') return 'Ozon'
  if (normalized === 'wb') return 'WB'
  if (normalized === 'yandex') return 'Yandex'
  return platform
}

async function loadChannels() {
  channelLoading.value = true
  try {
    const res = await listChannels()
    if (isSuccess(res) && res.data) {
      channelOptions.value = res.data.map(item => ({ label: item.name, value: item.code }))
    }
  } finally {
    channelLoading.value = false
  }
}

function applyDefaults(record?: PackShipPackagePageVO) {
  trackingNo.value = record?.trackingNo
  weight.value = record?.weight
  const candidate = record?.channelCode || record?.defaultChannelCode || 'AUTO'
  channel.value = channelOptions.value.some(item => item.value === candidate) ? candidate : 'AUTO'
}

watch(
  () => [props.open, props.record?.id] as const,
  async ([open]) => {
    if (!open) return
    if (!channelOptions.value.length) await loadChannels()
    applyDefaults(props.record)
  },
  { immediate: true }
)

function handleClose() {
  emit('update:open', false)
}

async function handleConfirm() {
  if (!props.record) return
  if (!channel.value) {
    message.warning('请选择签出方式')
    return
  }
  if (!weight.value || weight.value <= 0) {
    message.warning('请填写包裹重量')
    return
  }
  submitting.value = true
  try {
    const res = await confirmShipPackage({
      outboundOrderId: props.record.outboundOrderId,
      packageId: props.record.id,
      channel: channel.value,
      trackingNo: trackingNo.value?.trim() || undefined,
      weight: weight.value
    })
    if (isSuccess(res)) {
      message.success('平台订单包裹已签出')
      emit('success')
      emit('update:open', false)
    } else {
      message.error(res.message || '签出失败')
    }
  } catch (error: any) {
    message.error(error?.message || '签出失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.field-hint {
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 12px;
}
</style>
