<template>
  <a-modal
    :open="open"
    :title="readonly ? '退货质检结果' : '退货质检 - 良品与残次品分配'"
    :width="1100"
    :confirm-loading="submitting"
    :footer="readonly ? null : undefined"
    ok-text="确认质检并上架"
    @ok="handleConfirm"
    @cancel="handleClose"
  >
    <a-spin :spinning="loading">
      <div v-if="order" class="order-head">
        <span class="ob-no">{{ order.returnNo }}</span>
        <span class="ob-meta">{{ order.ownerName }} · {{ order.warehouseName }}</span>
      </div>

      <a-alert
        v-if="!readonly"
        type="info"
        show-icon
        style="margin: 12px 0"
        message="良品进入退货区并可再次分配，残次品进入不良品区且不可分配；两类数量之和必须等于实收数量。"
      />

      <a-table :data-source="lines" :pagination="false" row-key="skuCode" size="small">
        <a-table-column title="SKU" :width="150">
          <template #default="{ record }">
            <div>{{ record.skuCode }}</div>
            <div class="sku-name">
              {{ record.skuName }}
              <a-tag v-if="record.electronic" color="geekblue" size="small">电子类</a-tag>
            </div>
          </template>
        </a-table-column>
        <a-table-column title="实收" data-index="receivedQty" :width="64" align="right" />

        <a-table-column title="良品数" :width="100">
          <template #default="{ record }">
            <span v-if="readonly">{{ record.qualifiedQty || 0 }}</span>
            <a-input-number v-else v-model:value="record.qualifiedQty" :min="0" :max="record.receivedQty" style="width: 80px" @change="() => syncDamagedQty(record)" />
          </template>
        </a-table-column>

        <a-table-column title="良品库位" :width="160">
          <template #default="{ record }">
            <template v-if="readonly">{{ record.qualifiedLocationCode || '—' }}</template>
            <a-select v-else v-model:value="record.qualifiedLocationCode" :options="locationOptions.RETURN" :disabled="record.qualifiedQty <= 0" show-search allow-clear placeholder="选择退货区库位" style="width: 140px" />
          </template>
        </a-table-column>

        <a-table-column title="残次数" :width="100">
          <template #default="{ record }">
            <span v-if="readonly">{{ record.damagedQty || 0 }}</span>
            <a-input-number v-else v-model:value="record.damagedQty" :min="0" :max="record.receivedQty" style="width: 80px" @change="() => syncQualifiedQty(record)" />
          </template>
        </a-table-column>

        <a-table-column title="残次品库位" :width="160">
          <template #default="{ record }">
            <template v-if="readonly">{{ record.damagedLocationCode || '—' }}</template>
            <a-select v-else v-model:value="record.damagedLocationCode" :options="locationOptions.DEFECTIVE" :disabled="record.damagedQty <= 0" show-search allow-clear placeholder="选择不良品区库位" style="width: 140px" />
          </template>
        </a-table-column>

        <a-table-column title="照片 / 备注" :width="320">
          <template #default="{ record }">
            <template v-if="readonly">
              <div v-if="(record.qcPhotoFileIds || []).length" class="qc-photos">
                <a-image
                  v-for="fid in record.qcPhotoFileIds"
                  :key="fid"
                  :width="40"
                  :height="40"
                  :src="photoUrlMap[fid]"
                  class="qc-thumb"
                />
              </div>
              <div class="sku-name">{{ record.qcRemark || '—' }}</div>
            </template>
            <template v-else>
              <div class="qc-cell">
                <div class="qc-photos">
                  <div v-for="fid in record.photoFileIds" :key="fid" class="qc-thumb-wrap">
                    <a-image :width="40" :height="40" :src="photoUrlMap[fid]" />
                    <close-circle-filled
                      class="qc-thumb-del"
                      @click="handleRemovePhoto(record, fid)"
                    />
                  </div>
                  <a-upload
                    :custom-request="(opts: any) => handleUpload(opts, record)"
                    :show-upload-list="false"
                    accept="image/*"
                  >
                    <a-button
                      size="small"
                      :loading="record.uploading"
                    >
                      <upload-outlined />
                      上传照片
                    </a-button>
                  </a-upload>
                </div>
                <a-input
                  v-model:value="record.qcRemark"
                  placeholder="备注(选填)"
                  size="small"
                  class="qc-remark"
                />
              </div>
            </template>
          </template>
        </a-table-column>
      </a-table>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined, CloseCircleFilled } from '@ant-design/icons-vue'
import { isSuccess } from '@/api'
import { getReturnDetail, submitQc, getAvailableLocations } from '@/api/wms/return-qc'
import { useSysFileUpload } from '@/hooks/use-sys-file-upload'
import { getFileDownloadUrl, batchGetDownloadUrls } from '@/api/system/file'
import type {
  ReturnOrderVO,
  ReturnZone,
  ReturnQcLineDTO
} from '@/api/wms/return-qc/types'

interface QcLine {
  skuCode: string
  skuName?: string
  electronic: boolean
  receivedQty: number
  qualifiedQty: number
  damagedQty: number
  qualifiedZone: ReturnZone
  qualifiedLocationCode?: string
  damagedLocationCode?: string
  qcRemark?: string
  // 编辑态：本次上传的照片 OSS 文件ID
  photoFileIds: number[]
  // 只读态：后端回显的照片 OSS 文件ID
  qcPhotoFileIds?: number[]
  // 单行上传中标记
  uploading?: boolean
}

// 质检照片允许的图片类型（复用 OSS 私有桶）
const PHOTO_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif']
// fileId → 签名下载URL，供缩略图/预览展示
const photoUrlMap = reactive<Record<number, string>>({})

const { uploadFile } = useSysFileUpload()

const props = defineProps<{ open: boolean; orderId?: number; readonly?: boolean }>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'success'): void
}>()

const loading = ref(false)
const submitting = ref(false)
const order = ref<ReturnOrderVO | null>(null)
const lines = ref<QcLine[]>([])

// 各分区可用(未占用)库位选项：PASS→RETURN(退货区)、FAIL→DEFECTIVE(次品区)
const locationOptions = reactive<Record<string, { label: string; value: string }[]>>({
  RETURN: [],
  DEFECTIVE: []
})

// 按「退货单所属仓库」加载退货区/次品区的可用库位（仓库由后端从退货单取，前端只传单号）
async function loadLocations(returnOrderId?: number) {
  locationOptions.RETURN = []
  locationOptions.DEFECTIVE = []
  if (!returnOrderId) return
  for (const zone of ['RETURN', 'DEFECTIVE'] as const) {
    try {
      const res = await getAvailableLocations(returnOrderId, zone)
      if (isSuccess(res) && res.data) {
        locationOptions[zone] = res.data.map(code => ({ label: code, value: code }))
      }
    } catch (e) {
      console.error('加载可用库位失败', zone, e)
    }
  }
}

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
        electronic: i.electronic,
        receivedQty: i.receivedQty ?? i.expectedQty,
        qualifiedQty: i.qualifiedQty ?? i.receivedQty ?? i.expectedQty,
        damagedQty: i.damagedQty ?? 0,
        qualifiedZone: i.qualifiedZone ?? 'RETURN',
        qualifiedLocationCode: i.qualifiedLocationCode ?? (i.quality === 'GOOD' ? i.locationCode : undefined),
        damagedLocationCode: i.damagedLocationCode ?? (i.quality === 'DAMAGED' ? i.locationCode : undefined),
        qcRemark: i.qcRemark,
        photoFileIds: [],
        qcPhotoFileIds: i.qcPhotoFileIds || []
      }))
      // 编辑态：加载本退货单所属仓库的退货区/次品区可用库位供下拉
      if (!props.readonly) {
        await loadLocations(res.data.id)
      }
      // 只读态：批量取已存照片的签名URL用于缩略图展示
      await loadPhotoUrls(lines.value.flatMap(l => l.qcPhotoFileIds || []))
    }
  } finally {
    loading.value = false
  }
}

// 批量拉取照片签名URL填入 photoUrlMap
async function loadPhotoUrls(fileIds: number[]) {
  const missing = Array.from(new Set(fileIds)).filter(id => !photoUrlMap[id])
  if (missing.length === 0) return
  try {
    const res = await batchGetDownloadUrls(missing)
    if (isSuccess(res) && res.data) {
      Object.entries(res.data).forEach(([id, url]) => {
        photoUrlMap[Number(id)] = url as string
      })
    }
  } catch (e) {
    console.error('加载质检照片失败', e)
  }
}

// 单张上传（复用 OSS 私有桶直传），成功后收集 fileId 并取预览URL
async function handleUpload(options: any, record: QcLine) {
  record.uploading = true
  try {
    const result = await uploadFile(options.file, {
      bucketKey: 'private-files',
      allowedTypes: PHOTO_TYPES
    })
    if (result) {
      record.photoFileIds.push(result.fileId)
      const urlRes = await getFileDownloadUrl(result.fileId)
      if (isSuccess(urlRes) && urlRes.data) {
        photoUrlMap[result.fileId] = urlRes.data
      }
      options.onSuccess?.(result)
    } else {
      options.onError?.(new Error('上传失败'))
    }
  } catch (e: any) {
    options.onError?.(e)
  } finally {
    record.uploading = false
  }
}

// 移除本次上传的照片
function handleRemovePhoto(record: QcLine, fileId: number) {
  record.photoFileIds = record.photoFileIds.filter(id => id !== fileId)
}

watch(
  () => [props.open, props.orderId] as const,
  ([open, id]) => {
    if (open && id) loadData(id)
  },
  { immediate: true }
)

function syncDamagedQty(record: QcLine) {
  record.qualifiedQty = Number(record.qualifiedQty || 0)
  record.damagedQty = Math.max(0, record.receivedQty - record.qualifiedQty)
  if (record.damagedQty === 0) record.damagedLocationCode = undefined
}

function syncQualifiedQty(record: QcLine) {
  record.damagedQty = Number(record.damagedQty || 0)
  record.qualifiedQty = Math.max(0, record.receivedQty - record.damagedQty)
  if (record.qualifiedQty === 0) record.qualifiedLocationCode = undefined
}

function handleClose() {
  emit('update:open', false)
}

async function handleConfirm() {
  if (!order.value) return
  // 校验
  for (const l of lines.value) {
    if (l.qualifiedQty < 0 || l.damagedQty < 0 || l.qualifiedQty + l.damagedQty !== l.receivedQty) {
      message.warning(`${l.skuCode} 良品数与残次数之和必须等于实收数`)
      return
    }
    if (l.qualifiedQty > 0 && !l.qualifiedLocationCode?.trim()) {
      message.warning(`${l.skuCode} 请选择良品库位`)
      return
    }
    if (l.damagedQty > 0 && !l.damagedLocationCode?.trim()) {
      message.warning(`${l.skuCode} 请选择残次品库位`)
      return
    }
  }
  const linesDto: ReturnQcLineDTO[] = lines.value.map(l => ({
    skuCode: l.skuCode,
    qualifiedQty: l.qualifiedQty,
    damagedQty: l.damagedQty,
    qualifiedZone: l.qualifiedQty > 0 ? l.qualifiedZone : undefined,
    qualifiedLocationCode: l.qualifiedQty > 0 ? l.qualifiedLocationCode?.trim() : undefined,
    damagedLocationCode: l.damagedQty > 0 ? l.damagedLocationCode?.trim() : undefined,
    qcRemark: l.qcRemark,
    photoFileIds: l.photoFileIds
  }))
  submitting.value = true
  try {
    const res = await submitQc({ returnOrderId: order.value.id, lines: linesDto })
    if (isSuccess(res)) {
      message.success('质检完成，退货货物已上架回库')
      emit('success')
      emit('update:open', false)
    } else {
      message.error(res.message || '质检失败')
    }
  } catch (e: any) {
    message.error(e?.message || '质检失败')
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
.qc-cell {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.qc-remark {
  flex: 1;
  min-width: 110px;
}
.qc-photos {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}
.qc-thumb,
.qc-thumb-wrap :deep(.ant-image-img) {
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
}
.qc-thumb-wrap {
  position: relative;
  line-height: 0;
}
.qc-thumb-del {
  position: absolute;
  top: -6px;
  right: -6px;
  color: #ff4d4f;
  background: #fff;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
}
</style>
