<template>
  <a-drawer
    v-model:open="open"
    :title="drawerTitle"
    :width="720"
    destroy-on-close
    placement="right"
  >
    <a-spin :spinning="loading">
      <!-- 扫码区 -->
      <div class="scan-box">
        <div class="scan-modes">
          <span><b>整单收货：</b>扫描当前入库单二维码，一次填满全部应收数量</span>
          <span><b>逐件收货：</b>扫描 SKU 条码，按右侧“每次 +”数量累计</span>
        </div>
        <div class="scan-row">
          <a-input
            ref="scanInputRef"
            v-model:value="scanValue"
            placeholder="扫描入库单号或 SKU 条码，手工输入后按回车也可以"
            allow-clear
            @press-enter="onScan"
          />
          <span class="step-label">每次 +</span>
          <a-input-number v-model:value="step" :min="1" :max="9999" style="width: 88px" />
        </div>
        <div class="scan-feedback">
          <template v-if="lastScan">
            <span v-if="lastScan.ok && lastScan.fullOrder" class="ok">
              入库单 {{ lastScan.code }} 已识别，全部应收数量已填入，请核对后确认收货
            </span>
            <span v-else-if="lastScan.ok" class="ok">
              最近：{{ lastScan.code }} ✓ +{{ lastScan.delta }} → 实收 {{ lastScan.actual }}
            </span>
            <span v-else class="err">最近：{{ lastScan.code }} ✗ 不是当前入库单号，也不在本单 SKU 明细中</span>
          </template>
          <span v-else class="tip">当前入库单：{{ currentNo }}；扫码框会自动保持焦点</span>
        </div>
      </div>

      <!-- 进度 -->
      <div class="progress-bar">
        <span>
          已收齐 <b>{{ collectedCount }}</b
          >/{{ rows.length }} SKU · 总实收 <b>{{ totalActual }}</b> / {{ totalExpected }}
        </span>
        <a-button size="small" @click="fillAll">一键实收 = 预期</a-button>
      </div>

      <section class="evidence-section">
        <div class="evidence-head">
          <div>
            <b><span class="required">*</span> 收货现场照片</b>
            <span>请拍摄外包装、到货数量或异常情况，至少上传 1 张</span>
          </div>
          <a-upload
            :custom-request="handleEvidenceUpload"
            :show-upload-list="false"
            accept="image/jpeg,image/png,image/webp"
            :disabled="photoFileIds.length >= MAX_PHOTOS"
          >
            <a-button size="small" :loading="uploadingEvidence">
              <upload-outlined />上传照片 {{ photoFileIds.length }}/{{ MAX_PHOTOS }}
            </a-button>
          </a-upload>
        </div>
        <div v-if="photoFileIds.length" class="photo-list">
          <div v-for="fileId in photoFileIds" :key="fileId" class="photo-item">
            <a-image :src="photoUrls[fileId]" :width="84" :height="64" />
            <close-circle-filled class="photo-remove" @click="removeEvidence(fileId)" />
          </div>
        </div>
      </section>

      <!-- 明细 -->
      <a-table :data-source="rows" :pagination="false" row-key="id" size="small">
        <a-table-column title="SKU编码" data-index="skuCode" />
        <a-table-column title="商品" :width="150">
          <template #default="{ record }">{{ record.skuName || '-' }}</template>
        </a-table-column>
        <a-table-column title="预期" data-index="expected" :width="70" align="center" />
        <a-table-column title="实收" :width="130">
          <template #default="{ record }">
            <a-input-number
              v-model:value="record.actual"
              :min="0"
              :max="record.expected"
              :class="{ 'flash-cell': flashSku === record.skuCode }"
              style="width: 100%"
            />
          </template>
        </a-table-column>
        <a-table-column title="状态" :width="110">
          <template #default="{ record }">
            <a-tag :color="statusColor(record)">{{ statusText(record) }}</a-tag>
          </template>
        </a-table-column>
      </a-table>
    </a-spin>

    <template #footer>
      <div style="display: flex; justify-content: flex-end; gap: 8px">
        <a-button @click="open = false">取消</a-button>
        <a-button type="primary" :loading="submitting" @click="submit">确认收货</a-button>
      </div>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { CloseCircleFilled, UploadOutlined } from '@ant-design/icons-vue'
import { isSuccess } from '@/api'
import { doRequest } from '@/utils/axios/request'
import { getFileDownloadUrl } from '@/api/system/file'
import { useFileUpload } from '@/hooks/use-file-upload'
import { getInboundOpsDetail, receiveInbound } from '@/api/wms/inbound-execution'
import type {
  PurchaseInboundDetailVO,
  PurchaseInboundPageVO
} from '@/api/wms/purchase-inbound/types'

const emits = defineEmits<{ (e: 'success'): void }>()

interface Row {
  id: number
  skuCode: string
  skuName: string
  expected: number
  actual: number
}

const open = ref(false)
const loading = ref(false)
const submitting = ref(false)
const currentId = ref<number>()
const currentNo = ref('')
const currentWarehouse = ref('')
const rows = reactive<Row[]>([])
const scanValue = ref('')
const step = ref(1)
const flashSku = ref('')
const lastScan = ref<{
  code: string
  ok: boolean
  fullOrder?: boolean
  delta?: number
  actual?: number
} | null>(null)
const scanInputRef = ref<{ focus?: () => void } | null>(null)
const photoFileIds = ref<number[]>([])
const photoUrls = reactive<Record<number, string>>({})
const uploadingEvidence = ref(false)
const { uploadFile } = useFileUpload()
const MAX_PHOTOS = 6

const drawerTitle = computed(
  () =>
    `收货作业 · ${currentNo.value}${currentWarehouse.value ? '（' + currentWarehouse.value + '）' : ''}`
)
const totalActual = computed(() => rows.reduce((s, r) => s + (r.actual || 0), 0))
const totalExpected = computed(() => rows.reduce((s, r) => s + (r.expected || 0), 0))
const collectedCount = computed(() => rows.filter(r => (r.actual || 0) >= r.expected).length)

function statusText(r: Row): string {
  if (r.actual > r.expected) return `超${r.actual - r.expected}`
  if (r.actual < r.expected) return `缺${r.expected - r.actual}`
  return '齐'
}
function statusColor(r: Row): string {
  if (r.actual > r.expected) return 'red'
  if (r.actual < r.expected) return 'default'
  return 'green'
}

function refocus() {
  nextTick(() => scanInputRef.value?.focus?.())
}

// 提示音（扫中/扫错）
let audioCtx: AudioContext | null = null
function beep(ok: boolean) {
  try {
    const Ctx =
      window.AudioContext ||
      (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
    audioCtx = audioCtx || new Ctx()
    const osc = audioCtx.createOscillator()
    const gain = audioCtx.createGain()
    osc.connect(gain)
    gain.connect(audioCtx.destination)
    osc.frequency.value = ok ? 880 : 300
    gain.gain.value = 0.05
    osc.start()
    osc.stop(audioCtx.currentTime + 0.08)
  } catch {
    // 忽略：无音频环境不影响功能
  }
}

function onScan() {
  const code = scanValue.value.trim()
  scanValue.value = ''
  if (!code) return
  if (code.toLowerCase() === currentNo.value.trim().toLowerCase()) {
    fillAll()
    lastScan.value = { code: currentNo.value, ok: true, fullOrder: true }
    beep(true)
    refocus()
    return
  }
  const matchingRows = rows.filter(r => r.skuCode.toLowerCase() === code.toLowerCase())
  const row = matchingRows.find(r => (r.actual || 0) < r.expected)
  if (!row) {
    lastScan.value = { code, ok: false }
    if (matchingRows.length > 0) message.warning(`SKU ${code} 已全部收齐`)
    beep(false)
    refocus()
    return
  }
  const previous = row.actual || 0
  const delta = Math.min(step.value, row.expected - previous)
  row.actual = previous + delta
  lastScan.value = { code: row.skuCode, ok: true, delta, actual: row.actual }
  flashSku.value = row.skuCode
  beep(true)
  window.setTimeout(() => {
    if (flashSku.value === row.skuCode) flashSku.value = ''
  }, 600)
  refocus()
}

function fillAll() {
  rows.forEach(r => {
    r.actual = r.expected
  })
}

function submit() {
	if (photoFileIds.value.length === 0) {
		message.warning('请至少上传一张收货现场照片')
		return
	}
  const items = rows
    .filter(r => (r.actual || 0) > 0)
    .map(r => ({
      inboundOrderItemId: r.id,
      skuCode: r.skuCode,
      actualQuantity: r.actual
    }))
  if (items.length === 0) {
    message.warning('请至少录入一条实收数量大于0的明细')
    return
  }
  const doSubmit = () => {
    submitting.value = true
    doRequest(receiveInbound({
      inboundOrderId: currentId.value!,
      items,
      evidenceFileIds: photoFileIds.value
    }), {
      successMessage: '收货成功',
      onSuccess: () => {
        open.value = false
        emits('success')
      },
      onFinally: () => {
        submitting.value = false
      }
    })
  }
  const shortQuantity = rows.reduce((sum, row) => sum + Math.max(row.expected - row.actual, 0), 0)
  Modal.confirm({
    title: shortQuantity > 0 ? '确认按实际数量收货' : '确认货物全部收齐',
    content:
      shortQuantity > 0
        ? `当前少收 ${shortQuantity} 件。确认后本张入库单将结束收货，剩余待收数量会被释放。`
        : `请确认现场货物已经清点并全部收齐。本次共 ${rows.length} 条明细、${totalActual.value} 件，确认后将进入待上架状态。`,
    okText: shortQuantity > 0 ? '按实际数量收货' : '确认全部收齐',
    cancelText: '返回核对',
    onOk: doSubmit
  })
}

async function openReceive(
  record: PurchaseInboundPageVO,
  options?: { detail?: PurchaseInboundDetailVO; fillExpected?: boolean }
) {
  currentId.value = record.id
  currentNo.value = record.inboundNo
  currentWarehouse.value = record.warehouseName || ''
  rows.splice(0, rows.length)
  scanValue.value = ''
  step.value = 1
  lastScan.value = null
  flashSku.value = ''
  photoFileIds.value = []
  Object.keys(photoUrls).forEach(key => delete photoUrls[Number(key)])
  open.value = true
  loading.value = true
  try {
    const detail = options?.detail
    const res = detail ? null : await getInboundOpsDetail(record.id)
    const data = detail || (res && isSuccess(res) ? res.data : undefined)
    if (data) {
      ;(data.items || []).forEach(i => {
        rows.push({
          id: i.id,
          skuCode: i.skuCode,
          skuName: i.skuBrief?.skuName || '',
          expected: i.expectedQuantity,
          actual: options?.fillExpected ? i.expectedQuantity : 0
        })
      })
    }
  } finally {
    loading.value = false
    refocus()
  }
}

async function handleEvidenceUpload(options: any) {
  if (photoFileIds.value.length >= MAX_PHOTOS) {
    message.warning(`最多上传 ${MAX_PHOTOS} 张收货照片`)
    options.onError?.(new Error('照片数量超过限制'))
    return
  }
  uploadingEvidence.value = true
  try {
    const result = await uploadFile(options.file, {
      bucketKey: 'private-files',
      folder: `inbound-receive/${currentId.value || 'unknown'}`,
      allowedTypes: ['image/jpeg', 'image/png', 'image/webp'],
      maxSize: 10 * 1024 * 1024
    })
    if (!result) {
      options.onError?.(new Error('上传失败'))
      return
    }
    photoFileIds.value.push(result.fileId)
    const response = await getFileDownloadUrl(result.fileId)
    if (isSuccess(response) && response.data) photoUrls[result.fileId] = response.data
    options.onSuccess?.(result)
  } catch (error: any) {
    options.onError?.(error)
  } finally {
    uploadingEvidence.value = false
  }
}

function removeEvidence(fileId: number) {
  photoFileIds.value = photoFileIds.value.filter(id => id !== fileId)
  delete photoUrls[fileId]
}

defineExpose({ open: openReceive })
</script>

<script lang="ts">
export default {
  name: 'ReceiveScanDrawer'
}
</script>

<style scoped>
.scan-box {
  background: #f6fbff;
  border: 1px solid #bae0ff;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
}
.scan-modes {
  display: grid;
  gap: 4px;
  margin-bottom: 10px;
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
}
.scan-modes b {
  color: rgba(0, 0, 0, 0.88);
}
.scan-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.scan-icon {
  font-size: 20px;
}
.step-label {
  color: #8c8c8c;
  white-space: nowrap;
}
.scan-feedback {
  margin-top: 8px;
  font-size: 13px;
  min-height: 20px;
}
.scan-feedback .ok {
  color: #52c41a;
}
.scan-feedback .err {
  color: #ff4d4f;
}
.scan-feedback .tip {
  color: #8c8c8c;
}
.progress-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.progress-bar b {
  color: #1677ff;
}
.evidence-section {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
}
.evidence-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.evidence-head > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.evidence-head span {
  color: rgba(0, 0, 0, .45);
  font-size: 12px;
}
.evidence-head .required { color: #ff4d4f; font-size: 14px; }
.photo-list { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 10px; }
.photo-item { position: relative; }
.photo-item :deep(img) { object-fit: cover; border-radius: 4px; }
.photo-remove { position: absolute; top: -6px; right: -6px; color: #ff4d4f; background: #fff; border-radius: 50%; cursor: pointer; }
.flash-cell :deep(.ant-input-number) {
  transition: background 0.2s;
}
.flash-cell {
  animation: flash 0.6s ease;
}
@keyframes flash {
  0% {
    background: #fffbe6;
    box-shadow: 0 0 0 2px #ffe58f;
  }
  100% {
    background: transparent;
    box-shadow: none;
  }
}
</style>
