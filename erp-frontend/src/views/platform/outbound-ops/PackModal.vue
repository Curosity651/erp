<template>
  <a-modal :open="open" title="订单打包" :width="920" :footer="null" @cancel="handleClose">
    <a-spin :spinning="loading">
      <div v-if="order" class="order-head">
        <span class="ob-no">{{ order.outboundNo }}</span>
        <span class="ob-meta">
          {{ order.ownerName }} · {{ order.warehouseName }}
          <template v-if="order.sourceType === 'SALES'">
            · {{ order.salesOrderCount }} 个平台订单</template
          >
        </span>
      </div>

      <template v-if="order?.sourceType === 'SALES'">
        <div class="toolbar">
          <a-space>
            <a-tag :color="order.documentMode === 'OWNER_PROVIDED' ? 'gold' : 'blue'">
              {{ order.documentMode === 'OWNER_PROVIDED' ? '货主提供资料' : '仓库打印资料' }}
            </a-tag>
            <a-button
              v-if="order.documentMode === 'WAREHOUSE_PRINT'"
              type="primary"
              :loading="preparingLabels"
              @click="handlePrepareLabels"
              >生成平台面单</a-button
            >
            <a-button
              v-if="order.documentMode === 'WAREHOUSE_PRINT' && order.platform === 'ozon'"
              :disabled="!departureDate"
              :loading="preparingAct"
              @click="handlePrepareAct"
              >生成 Ozon 交接单</a-button
            >
            <a-date-picker
              v-if="order.documentMode === 'WAREHOUSE_PRINT' && order.platform === 'ozon'"
              v-model:value="departureDate"
              value-format="YYYY-MM-DD"
              :allow-clear="false"
              style="width: 132px"
            />
          </a-space>
          <a-input v-model:value="packerName" placeholder="打包员（选填）" style="width: 180px" />
        </div>

        <a-alert
          v-if="order.documentMode === 'OWNER_PROVIDED'"
          type="info"
          show-icon
          message="请按平台订单分别核对面单和交接单；需要交接单的订单，两项都确认后才能最终签出。"
          style="margin-bottom: 12px"
        />

        <a-space
          v-if="labelFiles.length || actBatch?.acts?.length"
          wrap
          style="margin-bottom: 12px"
        >
          <a-button
            v-for="file in labelFiles"
            :key="file.fileId"
            size="small"
            @click="openUrl(file.downloadUrl)"
          >
            下载面单：{{ file.fileName }}
          </a-button>
          <a-button
            v-for="act in actBatch?.acts || []"
            :key="act.actId"
            size="small"
            :disabled="act.status !== 'READY'"
            @click="openUrl(act.downloadUrl)"
          >
            {{
              act.status === 'READY'
                ? `下载交接单：${act.fileName}`
                : `交接单${act.status === 'FAILED' ? '失败' : '生成中'}`
            }}
          </a-button>
        </a-space>

        <a-table :data-source="order.packages || []" :pagination="false" row-key="id" size="small">
          <a-table-column title="格口" data-index="sortCode" :width="90">
            <template #default="{ record }">{{ record.sortCode || '—' }}</template>
          </a-table-column>
          <a-table-column title="平台订单" data-index="platformOrderId" :width="190" />
          <a-table-column title="商品" :width="280">
            <template #default="{ record }">
              <div v-for="item in record.items" :key="item.skuCode">
                {{ item.skuCode }}：已复核 {{ item.packedQty || 0 }} / {{ item.qty }}
              </div>
            </template>
          </a-table-column>
          <a-table-column title="面单" :width="110" align="center">
            <template #default="{ record }">
              <a-tag v-if="record.labelStatus === 'READY'" color="green">已生成</a-tag>
              <a-tag v-else-if="record.labelStatus === 'EXTERNAL_CONFIRMED'" color="blue"
                >已核对</a-tag
              >
              <a-button
                v-else-if="order.documentMode === 'OWNER_PROVIDED'"
                type="link"
                size="small"
                @click="handleExternalDocument(record.id)"
                >核对资料</a-button
              >
              <a-tag v-else>待生成</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="交接单" :width="110" align="center">
            <template #default="{ record }">
              <span v-if="!record.handoverRequired">无需</span>
              <a-tag v-else-if="record.handoverStatus === 'READY'" color="green">已生成</a-tag>
              <a-tag v-else-if="record.handoverStatus === 'EXTERNAL_CONFIRMED'" color="blue"
                >已核对</a-tag
              >
              <a-button
                v-else-if="order.documentMode === 'OWNER_PROVIDED'"
                type="link"
                size="small"
                @click="handleExternalHandover(record.id)"
                >核对交接单</a-button
              >
              <a-tag v-else color="orange">待生成</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="打包" :width="150" align="center">
            <template #default="{ record }">
              <a-tag v-if="record.packStatus === 'PACKED'" color="green">已完成</a-tag>
              <a-space v-else size="small">
                <a-button type="link" size="small" @click="openPackScan(record)">扫码</a-button>
                <a-button
                  type="link"
                  size="small"
                  :loading="packingId === record.id"
                  :disabled="!packagePackVerified(record)"
                  @click="handlePackagePack(record.id)"
                  >完成</a-button
                >
              </a-space>
            </template>
          </a-table-column>
        </a-table>
      </template>

      <template v-else-if="order">
        <div class="section-title">配货明细</div>
        <a-table
          :data-source="order.items || []"
          :pagination="false"
          row-key="skuCode"
          size="small"
        >
          <a-table-column title="SKU" data-index="skuCode" />
          <a-table-column title="数量" data-index="qty" :width="90" align="right" />
          <a-table-column title="品质" :width="90" align="center">
            <template #default="{ record }">{{
              record.quality === 'GOOD' ? '良品' : '次品'
            }}</template>
          </a-table-column>
        </a-table>
        <a-form :label-col="{ style: { width: '80px' } }" style="margin-top: 16px">
          <a-form-item label="打包模式" required>
            <a-radio-group v-model:value="packMode" button-style="solid">
              <a-radio-button
                v-for="mode in PACK_MODE_OPTIONS"
                :key="mode.value"
                :value="mode.value"
              >
                {{ mode.label }}
              </a-radio-button>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="打包员"
            ><a-input v-model:value="packerName" style="width: 240px"
          /></a-form-item>
        </a-form>
      </template>
    </a-spin>

    <div class="modal-footer">
      <a-button @click="handleClose">关闭</a-button>
      <a-button
        v-if="order?.sourceType !== 'SALES'"
        type="primary"
        :loading="submitting"
        @click="handleCustomConfirm"
        >确认打包</a-button
      >
    </div>

    <a-modal
      v-model:open="packScanOpen"
      title="包裹商品复核"
      :confirm-loading="packScanSubmitting"
      @ok="submitPackScan"
    >
      <a-form layout="vertical">
        <a-form-item label="平台订单">
          <a-input :value="packScanPackage?.platformOrderId" disabled />
        </a-form-item>
        <a-form-item label="商品条码或ERP SKU" required>
          <a-input v-model:value="packScanForm.scanCode" autofocus />
        </a-form-item>
        <a-form-item label="本次数量" required>
          <a-input-number v-model:value="packScanForm.quantity" :min="1" style="width: 100%" />
        </a-form-item>
        <a-checkbox v-model:checked="packScanForm.manual">手工输入ERP SKU</a-checkbox>
      </a-form>
    </a-modal>
  </a-modal>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { isSuccess } from '@/api'
import {
  confirmExternalDocument,
  confirmExternalHandover,
  confirmPack,
  confirmPackPackage,
  getPackShipDetail,
  pollOzonAct,
  prepareOutboundLabels,
  prepareOzonAct,
  scanPackPackage
} from '@/api/wms/outbound-shipping'
import type { OzonActBatchVO, PackMode, PackShipOrderVO } from '@/api/wms/outbound-shipping/types'
import type { OutboundPackageVO } from '@/api/wms/outbound-picking/types'
import { PACK_MODE_OPTIONS } from './packship-constants'

const props = defineProps<{ open: boolean; orderId?: number }>()
const emit = defineEmits<{ (e: 'update:open', value: boolean): void; (e: 'success'): void }>()

const loading = ref(false)
const submitting = ref(false)
const preparingLabels = ref(false)
const preparingAct = ref(false)
const packingId = ref<number>()
const order = ref<PackShipOrderVO | null>(null)
const packMode = ref<PackMode>('BY_ORDER')
const packerName = ref<string>()
const labelFiles = ref<any[]>([])
const actBatch = ref<OzonActBatchVO>()
const departureDate = ref(dayjs().format('YYYY-MM-DD'))
const packScanOpen = ref(false)
const packScanSubmitting = ref(false)
const packScanPackage = ref<OutboundPackageVO>()
const packScanForm = ref({ scanCode: '', quantity: 1, manual: false })
let pollTimer: number | undefined

async function loadData(id: number) {
  loading.value = true
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
    if (open && id) {
      labelFiles.value = []
      actBatch.value = undefined
      loadData(id)
    } else stopPolling()
  },
  { immediate: true }
)

async function handlePrepareLabels() {
  if (!order.value) return
  preparingLabels.value = true
  try {
    const res = await prepareOutboundLabels(order.value.id)
    if (isSuccess(res)) {
      labelFiles.value = res.data?.files || []
      message.success('平台面单已生成，请下载并打印')
      await loadData(order.value.id)
    }
  } finally {
    preparingLabels.value = false
  }
}

async function handleExternalDocument(packageId: number) {
  if (!order.value) return
  const res = await confirmExternalDocument(order.value.id, packageId)
  if (isSuccess(res)) await loadData(order.value.id)
}

async function handleExternalHandover(packageId: number) {
  if (!order.value) return
  const res = await confirmExternalHandover(order.value.id, packageId)
  if (isSuccess(res)) {
    message.success('交接单已核对')
    await loadData(order.value.id)
  }
}

function openPackScan(pack: OutboundPackageVO) {
  packScanPackage.value = pack
  packScanForm.value = { scanCode: '', quantity: 1, manual: false }
  packScanOpen.value = true
}

async function submitPackScan() {
  if (!order.value || !packScanPackage.value || !packScanForm.value.scanCode.trim()) return
  packScanSubmitting.value = true
  try {
    const res = await scanPackPackage({
      outboundOrderId: order.value.id,
      packageId: packScanPackage.value.id,
      scanCode: packScanForm.value.scanCode.trim(),
      quantity: packScanForm.value.quantity,
      manual: packScanForm.value.manual
    })
    if (isSuccess(res)) {
      packScanOpen.value = false
      message.success('包裹复核数量已登记')
      await loadData(order.value.id)
    }
  } finally {
    packScanSubmitting.value = false
  }
}

function packagePackVerified(pack: OutboundPackageVO) {
  return pack.items.every(item => (item.packedQty || 0) === item.qty)
}

async function handlePackagePack(packageId: number) {
  if (!order.value) return
  packingId.value = packageId
  try {
    const res = await confirmPackPackage({
      outboundOrderId: order.value.id,
      packageId,
      packerName: packerName.value
    })
    if (isSuccess(res)) {
      message.success('该平台订单已完成打包')
      await loadData(order.value.id)
      emit('success')
    }
  } finally {
    packingId.value = undefined
  }
}

async function handlePrepareAct() {
  if (!order.value) return
  preparingAct.value = true
  try {
    const res = await prepareOzonAct(order.value.id, departureDate.value)
    if (!isSuccess(res)) return
    actBatch.value = res.data
    if (!res.data?.acts?.length) {
      message.info('当前配送方式无需生成 Ozon 交接单')
      return
    }
    schedulePoll()
  } finally {
    preparingAct.value = false
  }
}

function schedulePoll() {
  stopPolling()
  if (!actBatch.value?.acts.some(act => act.status === 'CREATING' || act.status === 'PENDING'))
    return
  pollTimer = window.setTimeout(async () => {
    if (!order.value || !actBatch.value?.batchNo) return
    const res = await pollOzonAct(order.value.id, actBatch.value.batchNo)
    if (isSuccess(res)) actBatch.value = res.data
    schedulePoll()
  }, 3000)
}

function stopPolling() {
  if (pollTimer) window.clearTimeout(pollTimer)
  pollTimer = undefined
}

async function handleCustomConfirm() {
  if (!order.value) return
  submitting.value = true
  try {
    const res = await confirmPack({
      outboundOrderId: order.value.id,
      packMode: packMode.value,
      packerName: packerName.value
    })
    if (isSuccess(res)) {
      message.success('打包完成')
      emit('success')
      handleClose()
    }
  } finally {
    submitting.value = false
  }
}

function openUrl(url?: string) {
  if (url) window.open(url, '_blank', 'noopener')
}

function handleClose() {
  stopPolling()
  emit('update:open', false)
}

onBeforeUnmount(stopPolling)
</script>

<style scoped>
.order-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
}
.ob-no {
  font-size: 16px;
  font-weight: 600;
}
.ob-meta {
  color: #8c8c8c;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.section-title {
  margin: 8px 0;
  font-weight: 600;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>
