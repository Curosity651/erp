<template>
  <a-drawer v-model:open="open" :title="`入库上架 · ${currentNo}`" :width="1040" destroy-on-close>
    <a-steps :current="loading ? 0 : 1" size="small" class="steps">
      <a-step title="读取实收商品" />
      <a-step title="分配逻辑库位" />
      <a-step title="生成上架单" />
    </a-steps>

    <a-spin :spinning="loading">
      <a-result v-if="loadError && !loading" status="error" title="上架数据加载失败" :sub-title="loadError">
        <template #extra><a-button type="primary" @click="retryLoad">重新加载</a-button></template>
      </a-result>

      <template v-else-if="plan">
        <a-alert :type="totalsMatch ? 'success' : 'error'" show-icon class="quantity-alert">
          <template #message>
            数量核对：实收 {{ receivedGrandTotal }} 件，上架分配 {{ allocatedGrandTotal }} 件
          </template>
          <template #description>
            {{ totalsMatch ? '各 SKU 数量一致，可以提交上架。' : differenceText }}
          </template>
        </a-alert>

        <section v-for="item in plan.items" :key="item.skuCode" class="sku-card">
          <div class="sku-head">
            <div>
              <a class="sku-link" @click="showSkuDetail(item)">{{ item.skuCode }}</a>
              <span class="sku-name">{{ item.skuName || '未命名商品' }}</span>
            </div>
            <div class="sku-meta">
              <span>实收 {{ item.receivedQuantity }} 件</span>
              <span>{{ dimensionsText(item) }}</span>
              <span>{{ weightText(item) }}</span>
            </div>
          </div>

          <div class="allocation-head">
            <span>品质</span><span>目标库位</span><span>本次数量</span>
            <span>推荐与容量</span><span>人工覆盖原因</span><span></span>
          </div>
          <div v-for="(row, index) in rowsFor(item.skuCode)" :key="row.key" class="allocation-row">
            <a-select v-model:value="row.quality" :options="qualityOptions" @change="resetLocation(row)" />
            <a-select
              v-model:value="row.locationId"
              show-search
              placeholder="选择库位"
              :options="locationOptions(item, row)"
              :filter-option="filterLocation"
            />
            <a-input-number v-model:value="row.quantity" :min="1" :precision="0" addon-after="件" />
            <div class="capacity-cell" :class="{ warning: needsOverride(item, row) }">
              {{ recommendationText(item, row) }}
            </div>
            <a-input
              v-model:value="row.overrideReason"
              :disabled="!needsOverride(item, row)"
              :placeholder="needsOverride(item, row) ? '必填：说明现场判断' : '无需填写'"
            />
            <a-space :size="4">
              <a-button size="small" title="拆分到另一个库位" @click="splitRow(item.skuCode, index)">拆分</a-button>
              <a-button size="small" danger :disabled="rowsFor(item.skuCode).length === 1" @click="removeRow(item.skuCode, index)">
                删除
              </a-button>
            </a-space>
          </div>

          <div class="sku-summary" :class="skuAllocated(item) === item.receivedQuantity ? 'ok' : 'bad'">
            已分配 {{ skuAllocated(item) }} / {{ item.receivedQuantity }} 件
          </div>
        </section>

        <section class="billing-section">
          <div class="section-title">入库计费依据</div>
          <a-form layout="inline">
            <a-form-item label="确认体积">
              <a-input-number v-model:value="confirmedVolumeCbm" :min="0" :precision="4" addon-after="m³" />
            </a-form-item>
            <a-form-item label="客户原因加班"><a-switch v-model:checked="afterHours" /></a-form-item>
            <a-form-item v-if="afterHours" label="加班原因">
              <a-input v-model:value="afterHoursReason" placeholder="请填写原因" />
            </a-form-item>
          </a-form>
        </section>
      </template>
    </a-spin>

    <template #footer>
      <div class="footer-row">
        <span :class="allValid ? 'ready' : 'not-ready'">{{ validationText }}</span>
        <a-space>
          <a-button @click="open = false">取消</a-button>
          <a-button type="primary" :loading="submitting" :disabled="!allValid" @click="submit">确认上架</a-button>
        </a-space>
      </div>
    </template>
  </a-drawer>

  <a-modal v-model:open="detailOpen" title="SKU 详情" :footer="null" width="680">
    <a-spin :spinning="detailLoading">
      <div v-if="detailItem" class="detail-layout">
        <a-image v-if="detailImage" :src="detailImage" :width="220" :height="180" class="product-image" />
        <div v-else class="image-placeholder">暂无商品图片</div>
        <a-descriptions :column="1" size="small" bordered class="detail-info">
          <a-descriptions-item label="内部 SKU">{{ detailItem.skuCode }}</a-descriptions-item>
          <a-descriptions-item label="商品名称">{{ detailItem.skuName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="外箱尺寸">{{ dimensionsText(detailItem) }}</a-descriptions-item>
          <a-descriptions-item label="单箱毛重">{{ weightText(detailItem) }}</a-descriptions-item>
          <a-descriptions-item label="本次实收">{{ detailItem.receivedQuantity }} 件</a-descriptions-item>
        </a-descriptions>
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { getSkuByCode } from '@/api/product/sku'
import { getPutawayPlan, putawayInbound } from '@/api/wms/inbound-execution'
import type {
  InboundPutawayDTO,
  LocationRecommendationVO,
  LogicalInboundPutawayPlanVO,
  LogicalPutawaySkuPlanVO,
  PutawayLine
} from '@/api/wms/inbound-execution'
import type { PurchaseInboundPageVO } from '@/api/wms/purchase-inbound/types'
import { printPutawayReceipt } from './putaway-receipt-print'

interface AllocationRow {
  key: string
  skuCode: string
  quality: 'GOOD' | 'DAMAGED'
  locationId?: number
  quantity: number
  overrideReason?: string
}

const emits = defineEmits<{ (e: 'success'): void }>()
const open = ref(false)
const loading = ref(false)
const loadError = ref('')
const submitting = ref(false)
const currentNo = ref('')
const currentRecord = ref<PurchaseInboundPageVO>()
const plan = ref<LogicalInboundPutawayPlanVO>()
const allocations = ref<Record<string, AllocationRow[]>>({})
const confirmedVolumeCbm = ref<number>()
const afterHours = ref(false)
const afterHoursReason = ref('')
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailItem = ref<LogicalPutawaySkuPlanVO>()
const detailImage = ref('')
let rowSequence = 1

const qualityOptions = [
  { value: 'GOOD', label: '良品' },
  { value: 'DAMAGED', label: '不良品' }
]

const receivedGrandTotal = computed(() =>
  (plan.value?.items || []).reduce((sum, item) => sum + item.receivedQuantity, 0)
)
const allocatedGrandTotal = computed(() =>
  Object.values(allocations.value).flat().reduce((sum, row) => sum + Number(row.quantity || 0), 0)
)
const totalsMatch = computed(() =>
  (plan.value?.items || []).every(item => skuAllocated(item) === item.receivedQuantity)
)
const differenceText = computed(() =>
  (plan.value?.items || [])
    .filter(item => skuAllocated(item) !== item.receivedQuantity)
    .map(item => `${item.skuCode}：实收 ${item.receivedQuantity}，已分配 ${skuAllocated(item)}`)
    .join('；')
)
const allValid = computed(() =>
  !!plan.value &&
  plan.value.items.length > 0 &&
  totalsMatch.value &&
  (!afterHours.value || !!afterHoursReason.value.trim()) &&
  plan.value.items.every(item =>
    rowsFor(item.skuCode).every(row =>
      !!row.locationId && row.quantity > 0 && (!needsOverride(item, row) || !!row.overrideReason?.trim())
    )
  )
)
const validationText = computed(() => {
  if (!totalsMatch.value) return '各 SKU 分配数量必须与实收数量一致'
  if (!allValid.value) return '请补齐目标库位和必要的人工覆盖原因'
  return '数量与库位已核对，可以完成上架'
})

function rowsFor(skuCode: string) {
  return allocations.value[skuCode] || []
}

function skuAllocated(item: LogicalPutawaySkuPlanVO) {
  return rowsFor(item.skuCode).reduce((sum, row) => sum + Number(row.quantity || 0), 0)
}

function candidateFor(item: LogicalPutawaySkuPlanVO, row: AllocationRow) {
  return item.recommendations.find(value => value.locationId === row.locationId)
}

function isDefective(candidate: LocationRecommendationVO) {
  return candidate.zoneType === 'DEFECTIVE' || candidate.locationType === 'DEFECTIVE'
}

function locationOptions(item: LogicalPutawaySkuPlanVO, row: AllocationRow) {
  return item.recommendations
    .filter(candidate => row.quality === 'DAMAGED' ? isDefective(candidate) : !isDefective(candidate))
    .map(candidate => ({
      value: candidate.locationId,
      label: `${candidate.locationCode} · 推荐 ${candidate.recommendedQuantity} 件${candidate.publicShared === 1 ? ' · 公共暂存' : ''}`,
      disabled: !candidate.weightAllowed || !candidate.skuKindsAllowed
    }))
}

function recommendationText(item: LogicalPutawaySkuPlanVO, row: AllocationRow) {
  const candidate = candidateFor(item, row)
  if (!candidate) return '请选择库位'
  return `推荐 ${candidate.recommendedQuantity} 件 · 剩余 ${(candidate.remainingVolumeMm3 / 1_000_000_000).toFixed(3)} m³`
}

function needsOverride(item: LogicalPutawaySkuPlanVO, row: AllocationRow) {
  const candidate = candidateFor(item, row)
  return !!candidate && row.quantity > candidate.recommendedQuantity
}

function resetLocation(row: AllocationRow) {
  row.locationId = undefined
  row.overrideReason = undefined
}

function splitRow(skuCode: string, index: number) {
  const rows = rowsFor(skuCode)
  const source = rows[index]
  if (!source || source.quantity < 2) {
    message.warning('当前分配至少需要 2 件才能拆分')
    return
  }
  const moved = Math.floor(source.quantity / 2)
  source.quantity -= moved
  rows.splice(index + 1, 0, {
    key: `row-${rowSequence++}`,
    skuCode,
    quality: source.quality,
    quantity: moved
  })
}

function removeRow(skuCode: string, index: number) {
  rowsFor(skuCode).splice(index, 1)
}

function initialRows(item: LogicalPutawaySkuPlanVO) {
  const rows: AllocationRow[] = []
  let remaining = item.receivedQuantity
  for (const candidate of item.recommendations.filter(value => !isDefective(value) && value.recommendedQuantity > 0)) {
    if (remaining <= 0) break
    const quantity = Math.min(remaining, candidate.recommendedQuantity)
    rows.push({
      key: `row-${rowSequence++}`,
      skuCode: item.skuCode,
      quality: 'GOOD',
      locationId: candidate.locationId,
      quantity
    })
    remaining -= quantity
  }
  if (remaining > 0 || rows.length === 0) {
    rows.push({
      key: `row-${rowSequence++}`,
      skuCode: item.skuCode,
      quality: 'GOOD',
      quantity: remaining || item.receivedQuantity
    })
  }
  return rows
}

async function openPutaway(record: PurchaseInboundPageVO) {
  currentRecord.value = record
  currentNo.value = record.inboundNo
  plan.value = undefined
  allocations.value = {}
  confirmedVolumeCbm.value = undefined
  afterHours.value = false
  afterHoursReason.value = ''
  loadError.value = ''
  open.value = true
  loading.value = true
  try {
    const response = await getPutawayPlan(record.id)
    if (!isSuccess(response) || !response.data) {
      loadError.value = response.message || '没有取得上架计划'
      return
    }
    plan.value = response.data
    response.data.items.forEach(item => { allocations.value[item.skuCode] = initialRows(item) })
    confirmedVolumeCbm.value = response.data.items.reduce(
      (sum, item) => sum + item.outerLengthMm * item.outerWidthMm * item.outerHeightMm * item.receivedQuantity / 1_000_000_000,
      0
    )
  } catch (error: any) {
    loadError.value = error?.message || '网络或服务器异常，请重新加载'
  } finally {
    loading.value = false
  }
}

function retryLoad() {
  if (currentRecord.value) void openPutaway(currentRecord.value)
}

function buildLines(): PutawayLine[] {
  return Object.values(allocations.value).flat().map(row => ({
    skuCode: row.skuCode,
    locationId: row.locationId as number,
    quantity: row.quantity,
    quality: row.quality,
    overrideReason: row.overrideReason?.trim() || undefined
  }))
}

function submit() {
  if (!currentRecord.value || !allValid.value) return
  Modal.confirm({
    title: '确认完成上架',
    content: `请确认现场共 ${receivedGrandTotal.value} 件货物已经按页面所列库位放置完成。提交后将写入库存并生成上架单。`,
    okText: '确认上架',
    cancelText: '返回核对',
    onOk: executePutaway
  })
}

async function executePutaway() {
  if (!currentRecord.value) return
  submitting.value = true
  try {
    const dto: InboundPutawayDTO = {
      inboundOrderId: currentRecord.value.id,
      lines: buildLines(),
      confirmedVolumeCbm: confirmedVolumeCbm.value,
      afterHours: afterHours.value,
      afterHoursReason: afterHoursReason.value.trim() || undefined
    }
    const response = await putawayInbound(dto)
    if (!isSuccess(response)) {
      message.error(response.message || '上架失败')
      throw new Error(response.message || '上架失败')
    }
    message.success('上架成功，库存与上架单已生成')
    open.value = false
    emits('success')
    if (response.data?.length) {
      Modal.confirm({
        title: '上架完成',
        content: '是否立即打印本次上架单？',
        okText: '打印上架单',
        cancelText: '稍后打印',
        onOk: () => {
          if (currentRecord.value && !printPutawayReceipt(currentRecord.value, response.data || [])) {
            message.warning('浏览器阻止了打印窗口，请允许本站弹出窗口后重试')
          }
        }
      })
    }
  } finally {
    submitting.value = false
  }
}

async function showSkuDetail(item: LogicalPutawaySkuPlanVO) {
  detailItem.value = item
  detailImage.value = ''
  detailOpen.value = true
  detailLoading.value = true
  try {
    const response = await getSkuByCode(item.skuCode)
    if (isSuccess(response) && response.data?.files) {
      const files = [
        ...(response.data.files.actual_image || []),
        ...(response.data.files.platform_image || [])
      ]
      detailImage.value = files[0]?.fileUrl || ''
    }
  } finally {
    detailLoading.value = false
  }
}

function dimensionsText(item: LogicalPutawaySkuPlanVO) {
  return `${(item.outerLengthMm / 10).toFixed(1)} × ${(item.outerWidthMm / 10).toFixed(1)} × ${(item.outerHeightMm / 10).toFixed(1)} cm`
}

function weightText(item: LogicalPutawaySkuPlanVO) {
  return `${(item.outerGrossWeightG / 1000).toFixed(2)} kg/箱`
}

const filterLocation = (input: string, option: { label?: string }) =>
  String(option.label || '').toLowerCase().includes(input.toLowerCase())

defineExpose({ open: openPutaway })
</script>

<script lang="ts">
export default { name: 'PutawayDrawer' }
</script>

<style scoped>
.steps { margin-bottom: 20px; }
.quantity-alert { margin-bottom: 14px; }
.sku-card { border: 1px solid #e5e7eb; border-radius: 6px; margin-bottom: 12px; padding: 14px; }
.sku-head { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 12px; }
.sku-link { font-size: 15px; font-weight: 600; }
.sku-name { margin-left: 10px; color: rgba(0, 0, 0, .65); }
.sku-meta { display: flex; gap: 16px; color: rgba(0, 0, 0, .55); font-size: 12px; white-space: nowrap; }
.allocation-head, .allocation-row { display: grid; grid-template-columns: 92px minmax(220px, 1.3fr) 120px minmax(180px, 1fr) minmax(180px, 1fr) 116px; gap: 8px; align-items: center; }
.allocation-head { color: rgba(0, 0, 0, .55); font-size: 12px; padding-bottom: 5px; }
.allocation-row + .allocation-row { margin-top: 8px; }
.allocation-row :deep(.ant-input-number), .allocation-row :deep(.ant-input-number-group-wrapper) { width: 100%; }
.capacity-cell { font-size: 12px; color: rgba(0, 0, 0, .55); }
.capacity-cell.warning { color: #d46b08; }
.sku-summary { text-align: right; margin-top: 8px; font-size: 12px; }
.sku-summary.ok, .ready { color: #389e0d; }
.sku-summary.bad, .not-ready { color: #cf1322; }
.billing-section { border-top: 1px solid #f0f0f0; padding-top: 16px; margin-top: 18px; }
.section-title { font-size: 15px; font-weight: 600; margin-bottom: 10px; }
.footer-row { display: flex; align-items: center; justify-content: space-between; }
.detail-layout { display: grid; grid-template-columns: 220px 1fr; gap: 18px; align-items: start; }
.product-image { object-fit: contain; }
.image-placeholder { height: 180px; display: flex; align-items: center; justify-content: center; background: #f5f5f5; color: rgba(0, 0, 0, .35); }
.detail-info { min-width: 0; }
</style>
