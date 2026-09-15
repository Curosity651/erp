<template>
  <a-modal
    :open="open"
    :title="t('platform.ship.title')"
    :width="560"
    :ok-text="t('platform.ship.confirm')"
    :cancel-text="t('platform.common.cancel')"
    :confirm-loading="submitting"
    @ok="handleConfirm"
    @cancel="handleClose"
  >
    <a-alert
      type="warning"
      show-icon
      :message="t('platform.ship.warning')"
      style="margin-bottom: 16px"
    />

    <a-descriptions v-if="record" :column="2" size="small" bordered>
      <a-descriptions-item :label="t('platform.picking.simple.platformOrder')" :span="2">
        {{ record.platformOrderId }}
      </a-descriptions-item>
      <a-descriptions-item :label="t('dashboard.platform')">
        {{ platformText(record.platform) }}
      </a-descriptions-item>
      <a-descriptions-item :label="t('platform.ship.shop')">
        {{ record.shopName || '-' }}
      </a-descriptions-item>
      <a-descriptions-item :label="t('platform.common.owner')">
        {{ record.ownerName }}
      </a-descriptions-item>
      <a-descriptions-item :label="t('platform.picking.simple.goods')">
        {{ t('platform.return.skuKindsAndPieces', { kinds: record.skuKinds, pieces: record.totalQty }) }}
      </a-descriptions-item>
    </a-descriptions>

    <a-form layout="vertical" style="margin-top: 16px">
      <a-form-item :label="t('platform.ship.method')" required>
        <a-select
          v-model:value="channel"
          :options="channelOptions"
          :loading="channelLoading"
          :placeholder="t('platform.ship.selectMethod')"
        />
        <div class="field-hint">{{ t('platform.ship.methodHint') }}</div>
      </a-form-item>
      <a-form-item :label="t('platform.ship.trackingNo')">
        <a-input
          v-model:value="trackingNo"
          :placeholder="t('platform.ship.trackingPlaceholder')"
          allow-clear
        />
      </a-form-item>
      <a-form-item :label="t('platform.ship.weight')" required>
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
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { confirmShipPackage, listChannels } from '@/api/wms/outbound-shipping'
import type { PackShipPackagePageVO } from '@/api/wms/outbound-shipping/types'

const props = defineProps<{ open: boolean; record?: PackShipPackagePageVO }>()
const { t } = useI18n()
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
    message.warning(t('platform.ship.selectMethod'))
    return
  }
  if (!weight.value || weight.value <= 0) {
    message.warning(t('platform.ship.enterWeight'))
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
      message.success(t('platform.ship.success'))
      emit('success')
      emit('update:open', false)
    } else {
      message.error(res.message || t('platform.ship.failed'))
    }
  } catch (error: any) {
    message.error(error?.message || t('platform.ship.failed'))
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
