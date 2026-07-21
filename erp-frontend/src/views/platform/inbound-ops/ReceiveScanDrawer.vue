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
        <div class="scan-row">
          <span class="scan-icon">🔫</span>
          <a-input
            ref="scanInputRef"
            v-model:value="scanValue"
            placeholder="扫描 SKU 条码，或手输后回车"
            allow-clear
            @press-enter="onScan"
          />
          <span class="step-label">每次 +</span>
          <a-input-number v-model:value="step" :min="1" :max="9999" style="width: 88px" />
        </div>
        <div class="scan-feedback">
          <template v-if="lastScan">
            <span v-if="lastScan.ok" class="ok">
              最近：{{ lastScan.code }} ✓ +{{ lastScan.delta }} → 实收 {{ lastScan.actual }}
            </span>
            <span v-else class="err">最近：{{ lastScan.code }} ✗ 不在本单明细</span>
          </template>
          <span v-else class="tip">进入自动聚焦，扫码枪连扫即可；漏扫可在下方手改</span>
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

      <!-- 明细 -->
      <a-table :data-source="rows" :pagination="false" row-key="skuCode" size="small">
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
import { isSuccess } from '@/api'
import { doRequest } from '@/utils/axios/request'
import { getInboundOpsDetail, receiveInbound } from '@/api/wms/inbound-execution'
import type { PurchaseInboundPageVO } from '@/api/wms/purchase-inbound/types'

const emits = defineEmits<{ (e: 'success'): void }>()

interface Row {
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
const lastScan = ref<{ code: string; ok: boolean; delta?: number; actual?: number } | null>(null)
const scanInputRef = ref<{ focus?: () => void } | null>(null)

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
  const row = rows.find(r => r.skuCode.toLowerCase() === code.toLowerCase())
  if (!row) {
    lastScan.value = { code, ok: false }
    beep(false)
    refocus()
    return
  }
  // 允许超收：直接累加（超出预期时状态列标红提示）
  row.actual = (row.actual || 0) + step.value
  lastScan.value = { code: row.skuCode, ok: true, delta: step.value, actual: row.actual }
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
  const items = rows
    .filter(r => (r.actual || 0) > 0)
    .map(r => ({ skuCode: r.skuCode, actualQuantity: r.actual }))
  if (items.length === 0) {
    message.warning('请至少录入一条实收数量大于0的明细')
    return
  }
  const doSubmit = () => {
    submitting.value = true
    doRequest(receiveInbound({ inboundOrderId: currentId.value!, items }), {
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
  const mismatch = rows.some(r => (r.actual || 0) !== r.expected)
  if (mismatch) {
    Modal.confirm({
      title: '确认收货',
      content: '存在缺收或超收，确定按当前实收数量提交？',
      okText: '确定',
      cancelText: '取消',
      onOk: doSubmit
    })
  } else {
    doSubmit()
  }
}

async function openReceive(record: PurchaseInboundPageVO) {
  currentId.value = record.id
  currentNo.value = record.inboundNo
  currentWarehouse.value = record.warehouseName || ''
  rows.splice(0, rows.length)
  scanValue.value = ''
  step.value = 1
  lastScan.value = null
  flashSku.value = ''
  open.value = true
  loading.value = true
  try {
    const res = await getInboundOpsDetail(record.id)
    if (isSuccess(res) && res.data) {
      ;(res.data.items || []).forEach(i => {
        rows.push({
          skuCode: i.skuCode,
          skuName: i.skuBrief?.skuName || '',
          expected: i.expectedQuantity,
          actual: 0
        })
      })
    }
  } finally {
    loading.value = false
    refocus()
  }
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
