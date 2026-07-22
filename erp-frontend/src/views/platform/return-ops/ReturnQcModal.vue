<template>
  <a-modal
    :open="open"
    :title="readonly ? '退货质检结果' : '退货质检 - 良品与残次品分配'"
    :width="1320"
    :confirm-loading="submitting"
    :footer="readonly ? null : undefined"
    ok-text="确认质检并上架"
    @ok="handleConfirm"
    @cancel="handleClose"
  >
    <a-spin :spinning="loading">
      <div v-if="order" class="order-head">
        <div>
          <span class="order-no">{{ order.returnNo }}</span>
          <span class="order-meta">{{ order.ownerName }}</span>
        </div>
        <div class="warehouse-field">
          <span>退货仓库</span>
          <span v-if="readonly">{{ order.warehouseName }}</span>
          <a-select
            v-else
            v-model:value="selectedWarehouseId"
            :options="warehouseOptions"
            :loading="warehouseLoading"
            placeholder="选择实际退货仓库"
            style="width: 230px"
            @change="handleWarehouseChange"
          />
        </div>
      </div>

      <a-alert
        v-if="!readonly"
        type="info"
        show-icon
        class="qc-alert"
        message="选择实际退货仓库后，将良品和残次品分配到具体托盘层位；电子类商品存在残次品时必须上传照片。"
      />

      <a-table :data-source="lines" :pagination="false" row-key="skuCode" size="small">
        <a-table-column title="SKU" :width="145">
          <template #default="{ record }">
            <div>{{ record.skuCode }}</div>
            <div class="secondary">
              {{ record.skuName }}
              <a-tag v-if="record.electronic" color="geekblue">电子类</a-tag>
            </div>
          </template>
        </a-table-column>
        <a-table-column title="实收" data-index="receivedQty" :width="58" align="right" />
        <a-table-column title="良品数" :width="90">
          <template #default="{ record }">
            <span v-if="readonly">{{ record.qualifiedQty || 0 }}</span>
            <a-input-number
              v-else
              v-model:value="record.qualifiedQty"
              :min="0"
              :max="record.receivedQty"
              class="qty-input"
              @change="syncDamagedQty(record)"
            />
          </template>
        </a-table-column>
        <a-table-column title="良品分区" :width="115">
          <template #default="{ record }">
            <span v-if="readonly">{{ zoneText(record.qualifiedZone) }}</span>
            <a-select
              v-else
              v-model:value="record.qualifiedZone"
              :options="qualifiedZoneOptions"
              :disabled="record.qualifiedQty <= 0"
              class="full-width"
              @change="resetPlacement(record, 'qualified')"
            />
          </template>
        </a-table-column>
        <a-table-column title="良品层位 / 托盘容量" :width="240">
          <template #default="{ record }">
            <span v-if="readonly">{{ record.qualifiedSlotCode || record.qualifiedLocationCode || '-' }}</span>
            <div v-else class="placement-cell">
              <a-select
                v-model:value="record.qualifiedSlotCode"
                :options="placementOptions(record.qualifiedZone)"
                :disabled="record.qualifiedQty <= 0 || !selectedWarehouseId"
                :loading="slotLoading"
                show-search
                allow-clear
                placeholder="选择层位"
                @change="value => handleSlotChange(record, 'qualified', value as string)"
              />
              <a-input-number
                v-model:value="record.qualifiedCapacityPercent"
                :disabled="record.qualifiedQty <= 0"
                :min="1"
                :max="100"
                :precision="0"
                addon-after="%"
                placeholder="入库后容量"
              />
            </div>
          </template>
        </a-table-column>
        <a-table-column title="残次数" :width="90">
          <template #default="{ record }">
            <span v-if="readonly">{{ record.damagedQty || 0 }}</span>
            <a-input-number
              v-else
              v-model:value="record.damagedQty"
              :min="0"
              :max="record.receivedQty"
              class="qty-input"
              @change="syncQualifiedQty(record)"
            />
          </template>
        </a-table-column>
        <a-table-column title="残次品层位 / 托盘容量" :width="240">
          <template #default="{ record }">
            <span v-if="readonly">{{ record.damagedSlotCode || record.damagedLocationCode || '-' }}</span>
            <div v-else class="placement-cell">
              <a-select
                v-model:value="record.damagedSlotCode"
                :options="placementOptions('DEFECTIVE')"
                :disabled="record.damagedQty <= 0 || !selectedWarehouseId"
                :loading="slotLoading"
                show-search
                allow-clear
                placeholder="选择不良品层位"
                @change="value => handleSlotChange(record, 'damaged', value as string)"
              />
              <a-input-number
                v-model:value="record.damagedCapacityPercent"
                :disabled="record.damagedQty <= 0"
                :min="1"
                :max="100"
                :precision="0"
                addon-after="%"
                placeholder="入库后容量"
              />
            </div>
          </template>
        </a-table-column>
        <a-table-column title="照片 / 备注" :width="300">
          <template #default="{ record }">
            <template v-if="readonly">
              <div v-if="record.qcPhotoFileIds?.length" class="photos">
                <a-image
                  v-for="fileId in record.qcPhotoFileIds"
                  :key="fileId"
                  :width="40"
                  :height="40"
                  :src="photoUrlMap[fileId]"
                />
              </div>
              <div class="secondary">{{ record.qcRemark || '-' }}</div>
            </template>
            <div v-else class="qc-cell">
              <div class="photos">
                <div v-for="fileId in record.photoFileIds" :key="fileId" class="photo-wrap">
                  <a-image :width="40" :height="40" :src="photoUrlMap[fileId]" />
                  <close-circle-filled class="photo-remove" @click="handleRemovePhoto(record, fileId)" />
                </div>
                <a-upload
                  :custom-request="(options: any) => handleUpload(options, record)"
                  :show-upload-list="false"
                  accept="image/jpeg,image/png"
                  :disabled="record.photoFileIds.length >= MAX_PHOTOS"
                >
                  <a-button size="small" :loading="record.uploading">
                    <upload-outlined />
                    上传照片 {{ record.photoFileIds.length }}/{{ MAX_PHOTOS }}
                  </a-button>
                </a-upload>
              </div>
              <a-input v-model:value="record.qcRemark" placeholder="备注（选填）" size="small" />
            </div>
          </template>
        </a-table-column>
      </a-table>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { CloseCircleFilled, UploadOutlined } from '@ant-design/icons-vue'
import { isSuccess } from '@/api'
import {
  getAuthorizedWarehouses,
  getAvailableSlots,
  getReturnDetail,
  submitQc
} from '@/api/wms/return-qc'
import { batchGetDownloadUrls, getFileDownloadUrl } from '@/api/system/file'
import { useFileUpload } from '@/hooks/use-file-upload'
import type { PalletSlotVO } from '@/api/wms/inbound-execution'
import type { ReturnOrderVO, ReturnQcLineDTO, ReturnZone } from '@/api/wms/return-qc/types'

type PlacementKind = 'qualified' | 'damaged'

interface QcLine {
  skuCode: string
  skuName?: string
  electronic: boolean
  quantityPerPallet?: number
  receivedQty: number
  qualifiedQty: number
  damagedQty: number
  qualifiedZone: ReturnZone
  qualifiedLocationCode?: string
  qualifiedSlotCode?: string
  qualifiedPalletId?: number
  qualifiedCapacityPercent?: number
  damagedLocationCode?: string
  damagedSlotCode?: string
  damagedPalletId?: number
  damagedCapacityPercent?: number
  qcRemark?: string
  photoFileIds: number[]
  qcPhotoFileIds?: number[]
  uploading?: boolean
}

const PHOTO_TYPES = ['image/jpeg', 'image/jpg', 'image/png']
const MAX_PHOTOS = 6
const MAX_PHOTO_SIZE = 10 * 1024 * 1024
const ZONES: ReturnZone[] = ['RETURN', 'STANDARD', 'DEFECTIVE']
const qualifiedZoneOptions = [
  { label: '退货区', value: 'RETURN' },
  { label: '标准区', value: 'STANDARD' }
]

const props = defineProps<{ open: boolean; orderId?: number; readonly?: boolean }>()
const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const { uploadFile } = useFileUpload()
const loading = ref(false)
const submitting = ref(false)
const warehouseLoading = ref(false)
const slotLoading = ref(false)
const selectedWarehouseId = ref<number>()
const warehouseOptions = ref<{ label: string; value: number }[]>([])
const order = ref<ReturnOrderVO | null>(null)
const lines = ref<QcLine[]>([])
const photoUrlMap = reactive<Record<number, string>>({})
const slotsByZone = reactive<Record<string, PalletSlotVO[]>>({ RETURN: [], STANDARD: [], DEFECTIVE: [] })

watch(
  () => [props.open, props.orderId] as const,
  ([open, id]) => {
    if (open && id) void loadData(id)
  },
  { immediate: true }
)

async function loadData(id: number) {
  loading.value = true
  order.value = null
  lines.value = []
  selectedWarehouseId.value = undefined
  try {
    const res = await getReturnDetail(id)
    if (!isSuccess(res) || !res.data) return
    order.value = res.data
    lines.value = (res.data.items || []).map(item => ({
      skuCode: item.skuCode,
      skuName: item.skuName,
      electronic: Boolean(item.electronic),
      quantityPerPallet: item.quantityPerPallet,
      receivedQty: item.receivedQty ?? item.expectedQty,
      qualifiedQty: item.qualifiedQty ?? item.receivedQty ?? item.expectedQty,
      damagedQty: item.damagedQty ?? 0,
      qualifiedZone: item.qualifiedZone ?? 'RETURN',
      qualifiedLocationCode: item.qualifiedLocationCode,
      qualifiedSlotCode: item.qualifiedSlotCode,
      qualifiedPalletId: item.qualifiedPalletId,
      damagedLocationCode: item.damagedLocationCode,
      damagedSlotCode: item.damagedSlotCode,
      damagedPalletId: item.damagedPalletId,
      qcRemark: item.qcRemark,
      photoFileIds: [],
      qcPhotoFileIds: item.qcPhotoFileIds || []
    }))
    if (!props.readonly) await loadWarehouses(res.data)
    await loadPhotoUrls(lines.value.flatMap(line => line.qcPhotoFileIds || []))
  } finally {
    loading.value = false
  }
}

async function loadWarehouses(detail: ReturnOrderVO) {
  warehouseLoading.value = true
  warehouseOptions.value = []
  try {
    const res = await getAuthorizedWarehouses(detail.id)
    if (!isSuccess(res) || !res.data) return
    warehouseOptions.value = res.data.map(item => ({
      label: `${item.warehouseName} (${item.warehouseCode})`,
      value: item.id
    }))
    selectedWarehouseId.value = warehouseOptions.value.some(item => item.value === detail.warehouseId)
      ? detail.warehouseId
      : warehouseOptions.value[0]?.value
    if (selectedWarehouseId.value) await loadSlots()
    else message.warning('当前货主没有可用于退货质检的有效仓库')
  } finally {
    warehouseLoading.value = false
  }
}

async function loadSlots() {
  ZONES.forEach(zone => (slotsByZone[zone] = []))
  if (!order.value || !selectedWarehouseId.value) return
  slotLoading.value = true
  try {
    const results = await Promise.all(
      ZONES.map(zone => getAvailableSlots(order.value!.id, selectedWarehouseId.value!, zone))
    )
    results.forEach((res, index) => {
      if (isSuccess(res) && res.data) slotsByZone[ZONES[index]] = res.data
    })
  } finally {
    slotLoading.value = false
  }
}

async function handleWarehouseChange() {
  lines.value.forEach(line => {
    resetPlacement(line, 'qualified')
    resetPlacement(line, 'damaged')
  })
  await loadSlots()
}

function placementOptions(zone: ReturnZone) {
  return (slotsByZone[zone] || []).map(slot => ({
    label: slot.palletId
      ? `${slot.slotCode} · 合并 ${slot.palletNo} · 当前 ${Math.round(slot.capacityPercent || 0)}%`
      : `${slot.slotCode} · 空层位`,
    value: slot.slotCode
  }))
}

function handleSlotChange(record: QcLine, kind: PlacementKind, slotCode?: string) {
  if (!slotCode) {
    resetPlacement(record, kind)
    return
  }
  const zone = kind === 'qualified' ? record.qualifiedZone : 'DEFECTIVE'
  const slot = (slotsByZone[zone] || []).find(item => item.slotCode === slotCode)
  if (!slot) return
  const quantity = kind === 'qualified' ? record.qualifiedQty : record.damagedQty
  const estimatedIncrement = record.quantityPerPallet
    ? (quantity / record.quantityPerPallet) * 100
    : undefined
  if (kind === 'qualified') {
    record.qualifiedSlotCode = slot.slotCode
    record.qualifiedLocationCode = slot.locationCode
    record.qualifiedPalletId = slot.palletId
    record.qualifiedCapacityPercent = recommendedCapacity(slot, estimatedIncrement)
  } else {
    record.damagedSlotCode = slot.slotCode
    record.damagedLocationCode = slot.locationCode
    record.damagedPalletId = slot.palletId
    record.damagedCapacityPercent = recommendedCapacity(slot, estimatedIncrement)
  }
}

function recommendedCapacity(slot: PalletSlotVO, increment?: number) {
  if (increment === undefined) return undefined
  return Math.min(100, Math.max(1, Math.ceil((slot.capacityPercent || 0) + increment)))
}

function resetPlacement(record: QcLine, kind: PlacementKind) {
  if (kind === 'qualified') {
    record.qualifiedLocationCode = undefined
    record.qualifiedSlotCode = undefined
    record.qualifiedPalletId = undefined
    record.qualifiedCapacityPercent = undefined
  } else {
    record.damagedLocationCode = undefined
    record.damagedSlotCode = undefined
    record.damagedPalletId = undefined
    record.damagedCapacityPercent = undefined
  }
}

function syncDamagedQty(record: QcLine) {
  record.qualifiedQty = Number(record.qualifiedQty || 0)
  record.damagedQty = Math.max(0, record.receivedQty - record.qualifiedQty)
  if (!record.damagedQty) resetPlacement(record, 'damaged')
  if (record.qualifiedSlotCode) handleSlotChange(record, 'qualified', record.qualifiedSlotCode)
}

function syncQualifiedQty(record: QcLine) {
  record.damagedQty = Number(record.damagedQty || 0)
  record.qualifiedQty = Math.max(0, record.receivedQty - record.damagedQty)
  if (!record.qualifiedQty) resetPlacement(record, 'qualified')
  if (record.damagedSlotCode) handleSlotChange(record, 'damaged', record.damagedSlotCode)
}

async function handleUpload(options: any, record: QcLine) {
  if (record.photoFileIds.length >= MAX_PHOTOS) {
    message.warning(`每个 SKU 最多上传 ${MAX_PHOTOS} 张照片`)
    options.onError?.(new Error('照片数量超过限制'))
    return
  }
  record.uploading = true
  try {
    const result = await uploadFile(options.file, {
      bucketKey: 'private-files',
      folder: `return-qc/${order.value?.id || 'unknown'}/${record.skuCode}`,
      allowedTypes: PHOTO_TYPES,
      maxSize: MAX_PHOTO_SIZE
    })
    if (!result) {
      options.onError?.(new Error('上传失败'))
      return
    }
    record.photoFileIds.push(result.fileId)
    const urlRes = await getFileDownloadUrl(result.fileId)
    if (isSuccess(urlRes) && urlRes.data) photoUrlMap[result.fileId] = urlRes.data
    options.onSuccess?.(result)
  } catch (error: any) {
    options.onError?.(error)
  } finally {
    record.uploading = false
  }
}

function handleRemovePhoto(record: QcLine, fileId: number) {
  record.photoFileIds = record.photoFileIds.filter(id => id !== fileId)
}

async function loadPhotoUrls(fileIds: number[]) {
  const missing = Array.from(new Set(fileIds)).filter(id => !photoUrlMap[id])
  if (!missing.length) return
  try {
    const res = await batchGetDownloadUrls(missing)
    if (isSuccess(res) && res.data) {
      Object.entries(res.data).forEach(([id, url]) => (photoUrlMap[Number(id)] = url as string))
    }
  } catch (error) {
    console.error('加载质检照片失败', error)
  }
}

function zoneText(zone?: ReturnZone) {
  return zone === 'STANDARD' ? '标准区' : zone === 'DEFECTIVE' ? '不良品区' : '退货区'
}

function handleClose() {
  emit('update:open', false)
}

async function handleConfirm() {
  if (!order.value || !selectedWarehouseId.value) {
    message.warning('请选择退货仓库')
    return
  }
  const usedSlots = new Set<string>()
  for (const line of lines.value) {
    if (line.qualifiedQty < 0 || line.damagedQty < 0 || line.qualifiedQty + line.damagedQty !== line.receivedQty) {
      message.warning(`${line.skuCode} 良品数与残次数之和必须等于实收数`)
      return
    }
    for (const kind of ['qualified', 'damaged'] as PlacementKind[]) {
      const quantity = kind === 'qualified' ? line.qualifiedQty : line.damagedQty
      if (quantity <= 0) continue
      const slotCode = kind === 'qualified' ? line.qualifiedSlotCode : line.damagedSlotCode
      const capacity = kind === 'qualified' ? line.qualifiedCapacityPercent : line.damagedCapacityPercent
      if (!slotCode || !capacity) {
        message.warning(`${line.skuCode} 请选择${kind === 'qualified' ? '良品' : '残次品'}层位并填写容量`)
        return
      }
      if (usedSlots.has(slotCode)) {
        message.warning(`层位 ${slotCode} 本次已被另一条质检结果使用，请重新选择`)
        return
      }
      usedSlots.add(slotCode)
    }
    if (line.electronic && line.damagedQty > 0 && !line.photoFileIds.length) {
      message.warning(`${line.skuCode} 属于电子类商品，存在残次品时必须上传照片`)
      return
    }
  }

  const dtoLines: ReturnQcLineDTO[] = lines.value.map(line => ({
    skuCode: line.skuCode,
    qualifiedQty: line.qualifiedQty,
    damagedQty: line.damagedQty,
    qualifiedZone: line.qualifiedQty > 0 ? line.qualifiedZone : undefined,
    qualifiedLocationCode: line.qualifiedQty > 0 ? line.qualifiedLocationCode : undefined,
    qualifiedSlotCode: line.qualifiedQty > 0 ? line.qualifiedSlotCode : undefined,
    qualifiedPalletId: line.qualifiedQty > 0 ? line.qualifiedPalletId : undefined,
    qualifiedCapacityPercent: line.qualifiedQty > 0 ? line.qualifiedCapacityPercent : undefined,
    damagedLocationCode: line.damagedQty > 0 ? line.damagedLocationCode : undefined,
    damagedSlotCode: line.damagedQty > 0 ? line.damagedSlotCode : undefined,
    damagedPalletId: line.damagedQty > 0 ? line.damagedPalletId : undefined,
    damagedCapacityPercent: line.damagedQty > 0 ? line.damagedCapacityPercent : undefined,
    qcRemark: line.qcRemark,
    photoFileIds: line.photoFileIds
  }))
  submitting.value = true
  try {
    const res = await submitQc({
      returnOrderId: order.value.id,
      warehouseId: selectedWarehouseId.value,
      lines: dtoLines
    })
    if (isSuccess(res)) {
      message.success('质检完成，退货已按托盘层位上架')
      emit('success')
      emit('update:open', false)
    } else message.error(res.message || '质检失败')
  } catch (error: any) {
    message.error(error?.message || '质检失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 38px;
}

.order-no {
  margin-right: 12px;
  font-weight: 600;
}

.order-meta,
.secondary {
  color: #8c8c8c;
  font-size: 12px;
}

.warehouse-field,
.photos {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qc-alert {
  margin: 12px 0;
}

.qty-input,
.full-width,
.placement-cell :deep(.ant-select),
.placement-cell :deep(.ant-input-number) {
  width: 100%;
}

.placement-cell,
.qc-cell {
  display: grid;
  gap: 6px;
}

.photos {
  flex-wrap: wrap;
}

.photo-wrap {
  position: relative;
  width: 40px;
  height: 40px;
}

.photo-remove {
  position: absolute;
  top: -6px;
  right: -6px;
  color: #ff4d4f;
  cursor: pointer;
  background: #fff;
  border-radius: 50%;
}
</style>
