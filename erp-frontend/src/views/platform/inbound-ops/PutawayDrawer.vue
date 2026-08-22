<template>
  <a-drawer v-model:open="open" :title="`登记上架 · ${currentNo}`" :width="1180" destroy-on-close>
    <a-steps :current="loading ? 0 : 1" size="small" class="steps">
      <a-step title="读取实收商品" />
      <a-step title="记录实际库位" />
      <a-step title="生成上架单" />
    </a-steps>
    <a-alert message="请按现场已经完成的上架结果登记" description="系统不会推荐库位或数量。每种 SKU 可以拆分记录到多个库位，但登记合计必须与实收数量一致。" type="info" show-icon class="record-alert" />

    <a-spin :spinning="loading">
      <a-result v-if="loadError && !loading" status="error" title="上架数据加载失败" :sub-title="loadError">
        <template #extra><a-button type="primary" @click="retryLoad">重新加载</a-button></template>
      </a-result>
      <template v-else-if="context">
        <a-alert :type="totalsMatch ? 'success' : 'error'" show-icon class="quantity-alert">
          <template #message>数量核对：实收 {{ receivedGrandTotal }} 件，已登记 {{ allocatedGrandTotal }} 件</template>
          <template #description>{{ totalsMatch ? '各 SKU 数量一致。' : differenceText }}</template>
        </a-alert>

        <section v-for="item in context.items" :key="item.skuCode" class="sku-card">
          <div class="sku-head">
            <div>
              <a class="sku-link" @click="showSkuDetail(item)">{{ item.warehouseSkuCode || item.skuCode }}</a>
              <span class="sku-name">{{ item.skuName || '未命名商品' }}</span>
            </div>
            <div class="sku-meta">
              <span>实收 {{ item.receivedQuantity }} 件</span><span>{{ dimensionsText(item) }}</span><span>{{ weightText(item) }}</span>
            </div>
          </div>

          <div class="allocation-head">
            <span>品质</span><span>实际库位</span><span>当前容量</span><span>登记后容量</span><span>实际数量</span><span>超限说明</span><span></span>
          </div>
          <div v-for="(row, index) in rowsFor(item.skuCode)" :key="row.key" class="allocation-row">
            <a-select v-model:value="row.quality" :options="qualityOptions" @change="resetLocation(row)" />
            <a-select v-model:value="row.locationId" show-search allow-clear placeholder="选择现场库位" :options="locationOptions(row)" :filter-option="filterLocation" />
            <div class="capacity-cell">{{ currentCapacityText(row) }}</div>
            <div class="capacity-cell" :class="capacityClass(row)">{{ projectedCapacityText(row) }}</div>
            <a-input-number v-model:value="row.quantity" :min="1" :precision="0" addon-after="件" />
            <a-input v-model:value="row.capacityOverrideReason" :disabled="!needsReason(row)" :placeholder="needsReason(row) ? '必填：说明现场情况' : '无需填写'" />
            <a-space :size="4">
              <a-button size="small" @click="splitRow(item.skuCode, index)">拆分</a-button>
              <a-button size="small" danger :disabled="rowsFor(item.skuCode).length === 1" @click="removeRow(item.skuCode, index)">删除</a-button>
            </a-space>
          </div>
          <div class="sku-summary" :class="skuAllocated(item) === item.receivedQuantity ? 'ok' : 'bad'">
            已登记 {{ skuAllocated(item) }} / {{ item.receivedQuantity }} 件，剩余 {{ item.receivedQuantity - skuAllocated(item) }} 件
          </div>
        </section>

        <section class="billing-section">
          <div class="section-title">入库计费依据</div>
          <a-form layout="inline">
            <a-form-item label="确认体积"><a-input-number v-model:value="confirmedVolumeCbm" :min="0" :precision="4" addon-after="m³" /></a-form-item>
            <a-form-item label="客户原因加班"><a-switch v-model:checked="afterHours" /></a-form-item>
            <a-form-item v-if="afterHours" label="加班原因"><a-input v-model:value="afterHoursReason" placeholder="请填写原因" /></a-form-item>
          </a-form>
        </section>
      </template>
    </a-spin>

    <template #footer>
      <div class="footer-row">
        <span :class="allValid ? 'ready' : 'not-ready'">{{ validationText }}</span>
        <a-space><a-button @click="open = false">取消</a-button><a-button type="primary" :loading="submitting" :disabled="!allValid" @click="submit">确认上架记录</a-button></a-space>
      </div>
    </template>
  </a-drawer>

  <a-modal v-model:open="detailOpen" title="SKU 详情" :footer="null" width="680">
    <div v-if="detailItem" class="detail-layout">
      <a-image v-if="detailItem.imageUrl" :src="detailItem.imageUrl" :width="220" :height="180" class="product-image" />
      <div v-else class="image-placeholder">暂无商品图片</div>
      <a-descriptions :column="1" size="small" bordered class="detail-info">
        <a-descriptions-item label="内部 SKU">{{ detailItem.warehouseSkuCode || detailItem.skuCode }}</a-descriptions-item>
        <a-descriptions-item label="原始 SKU">{{ detailItem.skuCode }}</a-descriptions-item>
        <a-descriptions-item label="商品名称">{{ detailItem.skuName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="外箱尺寸">{{ dimensionsText(detailItem) }}</a-descriptions-item>
        <a-descriptions-item label="单箱毛重">{{ weightText(detailItem) }}</a-descriptions-item>
        <a-descriptions-item label="本次实收">{{ detailItem.receivedQuantity }} 件</a-descriptions-item>
      </a-descriptions>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { getPutawayRecordContext, recordPutaway } from '@/api/wms/inbound-execution'
import type { PutawayRecordContextVO, PutawayRecordDTO, PutawayRecordLocationVO, PutawayRecordSkuVO } from '@/api/wms/inbound-execution'
import type { PurchaseInboundPageVO } from '@/api/wms/purchase-inbound/types'
import { allocatedQuantity, capacityState, createInitialRow, mergeRecordLines, projectLocationCapacity, requiresCapacityReason } from './putaway-record'
import type { CapacityProjection, PutawayRecordFormLine } from './putaway-record'
import { printPutawayReceipt } from './putaway-receipt-print'

const emits = defineEmits<{ (e: 'success'): void }>()
const open = ref(false)
const loading = ref(false)
const loadError = ref('')
const submitting = ref(false)
const currentNo = ref('')
const currentRecord = ref<PurchaseInboundPageVO>()
const context = ref<PutawayRecordContextVO>()
const allocations = ref<Record<string, PutawayRecordFormLine[]>>({})
const confirmedVolumeCbm = ref<number>()
const afterHours = ref(false)
const afterHoursReason = ref('')
const detailOpen = ref(false)
const detailItem = ref<PutawayRecordSkuVO>()

const qualityOptions = [{ value: 'GOOD', label: '良品' }, { value: 'DAMAGED', label: '不良品' }]
const allRows = computed(() => Object.values(allocations.value).flat())
const skuByCode = computed(() => new Map((context.value?.items || []).map(item => [item.skuCode, item])))
const locationById = computed(() => new Map((context.value?.locations || []).map(item => [item.locationId, item])))
const receivedGrandTotal = computed(() => (context.value?.items || []).reduce((sum, item) => sum + item.receivedQuantity, 0))
const allocatedGrandTotal = computed(() => allRows.value.reduce((sum, row) => sum + Number(row.quantity || 0), 0))
const totalsMatch = computed(() => (context.value?.items || []).every(item => skuAllocated(item) === item.receivedQuantity))
const differenceText = computed(() => (context.value?.items || []).filter(item => skuAllocated(item) !== item.receivedQuantity)
  .map(item => `${item.warehouseSkuCode || item.skuCode}：实收 ${item.receivedQuantity}，已登记 ${skuAllocated(item)}`).join('；'))
const allValid = computed(() => !!context.value && context.value.items.length > 0 && totalsMatch.value &&
  (!afterHours.value || !!afterHoursReason.value.trim()) && allRows.value.every(row =>
    !!row.locationId && Number(row.quantity) > 0 && (!needsReason(row) || !!row.capacityOverrideReason?.trim())))
const validationText = computed(() => {
  if (!totalsMatch.value) return '各 SKU 登记数量必须与实收数量一致'
  if (afterHours.value && !afterHoursReason.value.trim()) return '请填写加班原因'
  if (!allValid.value) return '请补齐实际库位、数量和必要的超限说明'
  return '上架结果已核对，可以登记入账'
})

function rowsFor(skuCode: string) { return allocations.value[skuCode] || [] }
function skuAllocated(item: PutawayRecordSkuVO) { return allocatedQuantity(rowsFor(item.skuCode), item.skuCode) }
function locationFor(row: PutawayRecordFormLine) { return row.locationId ? locationById.value.get(row.locationId) : undefined }
function projectionFor(row: PutawayRecordFormLine): CapacityProjection {
  const location = locationFor(row)
  return location ? projectLocationCapacity(location, allRows.value, skuByCode.value) : { calculable: false }
}
function isDefective(location: PutawayRecordLocationVO) { return location.zoneType === 'DEFECTIVE' }
function locationOptions(row: PutawayRecordFormLine) {
  return (context.value?.locations || []).filter(location => row.quality === 'DAMAGED' ? isDefective(location) : !isDefective(location)).map(location => ({
    value: location.locationId,
    label: `${location.locationCode} · ${location.zoneName || location.zoneType || '未分区'}${location.publicShared === 1 ? ' · 公共' : ''} · ${location.capacityCalculable ? `${Number(location.utilizationPercent || 0).toFixed(1)}%` : '容量未知'}`
  }))
}
function currentCapacityText(row: PutawayRecordFormLine) {
  const location = locationFor(row)
  if (!location) return '请选择库位'
  return location.capacityCalculable ? `${Number(location.utilizationPercent || 0).toFixed(1)}%` : '资料不全，无法计算'
}
function projectedCapacityText(row: PutawayRecordFormLine) {
  if (!row.locationId) return '请选择库位'
  const projection = projectionFor(row)
  if (!projection.calculable) return '资料不全，允许登记'
  const warnings: string[] = [`${Number(projection.utilizationPercent || 0).toFixed(1)}%`]
  if (projection.volumeAllowed === false) warnings.push('体积超限')
  if (projection.weightAllowed === false) warnings.push('承重超限')
  if (projection.skuKindsAllowed === false) warnings.push('SKU种类偏多')
  return warnings.join(' · ')
}
function capacityClass(row: PutawayRecordFormLine) { return row.locationId ? capacityState(projectionFor(row)) : 'unknown' }
function needsReason(row: PutawayRecordFormLine) { return !!row.locationId && requiresCapacityReason(projectionFor(row)) }
function resetLocation(row: PutawayRecordFormLine) { row.locationId = undefined; row.capacityOverrideReason = undefined }
function splitRow(skuCode: string, index: number) {
  const source = rowsFor(skuCode)[index]
  const row = createInitialRow(skuCode)
  row.quality = source?.quality || 'GOOD'
  rowsFor(skuCode).splice(index + 1, 0, row)
}
function removeRow(skuCode: string, index: number) { rowsFor(skuCode).splice(index, 1) }

async function openPutaway(record: PurchaseInboundPageVO) {
  currentRecord.value = record
  currentNo.value = record.inboundNo
  context.value = undefined
  allocations.value = {}
  confirmedVolumeCbm.value = undefined
  afterHours.value = false
  afterHoursReason.value = ''
  loadError.value = ''
  open.value = true
  loading.value = true
  try {
    const response = await getPutawayRecordContext(record.id)
    if (!isSuccess(response) || !response.data) { loadError.value = response.message || '没有取得上架记录数据'; return }
    context.value = response.data
    response.data.items.forEach(item => { allocations.value[item.skuCode] = [createInitialRow(item.skuCode)] })
    if (response.data.items.every(item => item.outerLengthMm && item.outerWidthMm && item.outerHeightMm)) {
      confirmedVolumeCbm.value = response.data.items.reduce((sum, item) => sum + Number(item.outerLengthMm) * Number(item.outerWidthMm) * Number(item.outerHeightMm) * item.receivedQuantity / 1_000_000_000, 0)
    }
  } catch (error: any) {
    loadError.value = error?.message || '网络或服务器异常，请重新加载'
  } finally { loading.value = false }
}

function retryLoad() { if (currentRecord.value) void openPutaway(currentRecord.value) }
function submit() {
  if (!currentRecord.value || !allValid.value) return
  Modal.confirm({ title: '确认登记上架结果', content: `请确认现场 ${receivedGrandTotal.value} 件货物已经按页面记录放置完成。提交后将写入库存并生成上架单。`, okText: '确认登记', cancelText: '返回核对', onOk: executePutaway })
}
async function executePutaway() {
  if (!currentRecord.value) return
  submitting.value = true
  try {
    const lines = mergeRecordLines(allRows.value).map(row => ({ skuCode: row.skuCode, locationId: row.locationId as number, quantity: row.quantity as number, quality: row.quality, capacityOverrideReason: row.capacityOverrideReason?.trim() || undefined }))
    const dto: PutawayRecordDTO = { inboundOrderId: currentRecord.value.id, lines, confirmedVolumeCbm: confirmedVolumeCbm.value, afterHours: afterHours.value, afterHoursReason: afterHoursReason.value.trim() || undefined }
    const response = await recordPutaway(dto)
    if (!isSuccess(response)) { message.error(response.message || '上架记录失败'); throw new Error(response.message || '上架记录失败') }
    message.success('上架结果已记录，库存与上架单已生成')
    open.value = false
    emits('success')
    if (response.data?.length) {
      Modal.confirm({ title: '上架记录完成', content: '是否立即打印本次上架单？', okText: '打印上架单', cancelText: '稍后打印', onOk: () => {
        if (currentRecord.value && !printPutawayReceipt(currentRecord.value, response.data || [])) message.warning('浏览器阻止了打印窗口，请允许本站弹出窗口后重试')
      } })
    }
  } finally { submitting.value = false }
}
function showSkuDetail(item: PutawayRecordSkuVO) { detailItem.value = item; detailOpen.value = true }
function dimensionsText(item: PutawayRecordSkuVO) {
  if (!item.outerLengthMm || !item.outerWidthMm || !item.outerHeightMm) return '外箱尺寸未维护'
  return `${(item.outerLengthMm / 10).toFixed(1)} × ${(item.outerWidthMm / 10).toFixed(1)} × ${(item.outerHeightMm / 10).toFixed(1)} cm`
}
function weightText(item: PutawayRecordSkuVO) { return item.outerGrossWeightG ? `${(item.outerGrossWeightG / 1000).toFixed(2)} kg/箱` : '单箱毛重未维护' }
const filterLocation = (input: string, option: { label?: string }) => String(option.label || '').toLowerCase().includes(input.toLowerCase())

defineExpose({ open: openPutaway })
</script>

<script lang="ts">export default { name: 'PutawayDrawer' }</script>

<style scoped>
.steps { margin-bottom: 16px; }
.record-alert, .quantity-alert { margin-bottom: 14px; }
.sku-card { border: 1px solid #e5e7eb; border-radius: 6px; margin-bottom: 12px; padding: 14px; }
.sku-head { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 12px; }
.sku-link { font-size: 15px; font-weight: 600; }
.sku-name { margin-left: 10px; color: rgba(0, 0, 0, .65); }
.sku-meta { display: flex; gap: 16px; color: rgba(0, 0, 0, .55); font-size: 12px; white-space: nowrap; }
.allocation-head, .allocation-row { display: grid; grid-template-columns: 82px minmax(210px, 1.35fr) 130px 170px 120px minmax(180px, 1fr) 116px; gap: 8px; align-items: center; }
.allocation-head { color: rgba(0, 0, 0, .55); font-size: 12px; padding-bottom: 5px; }
.allocation-row + .allocation-row { margin-top: 8px; }
.allocation-row :deep(.ant-input-number), .allocation-row :deep(.ant-input-number-group-wrapper) { width: 100%; }
.capacity-cell { min-height: 32px; display: flex; align-items: center; font-size: 12px; color: rgba(0, 0, 0, .55); }
.capacity-cell.warning { color: #d46b08; }
.capacity-cell.overflow { color: #cf1322; font-weight: 600; }
.capacity-cell.unknown { color: rgba(0, 0, 0, .45); }
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
