<template>
  <a-drawer v-model:open="open" :title="`入库上架 · ${currentNo}`" :width="980" destroy-on-close>
    <a-steps :current="loading ? 0 : 1" size="small" class="steps">
      <a-step title="读取实收商品" />
      <a-step title="分托与层位确认" />
      <a-step title="写入库存" />
    </a-steps>

    <a-spin :spinning="loading">
      <a-alert
        v-for="warning in plan?.warnings || []"
        :key="warning"
        type="warning"
        show-icon
        :message="warning"
        class="warning"
      />

      <section v-if="sourceItems.length" class="source-band">
        <div class="section-title">待上架商品</div>
        <div class="source-list">
          <span v-for="item in sourceItems" :key="item.skuCode" class="source-item">
            <strong>{{ item.skuCode }}</strong>
            <span>{{ item.quantity }} 件</span>
          </span>
        </div>
      </section>

      <section v-if="pallets.length" class="plan-section">
        <div class="plan-header">
          <div>
            <div class="section-title">人工分托与位置确认</div>
            <div class="subtle">共 {{ pallets.length }} 托，仓库人员按现场情况拆托、混托并选择实际托位</div>
          </div>
          <a-tag color="blue">位置由员工确认</a-tag>
        </div>

        <div v-for="(pallet, index) in pallets" :key="pallet.palletKey" class="pallet-card">
          <div class="pallet-head">
            <div class="pallet-name">
              <span class="sequence">{{ index + 1 }}</span>
              <strong>{{ pallet.palletNo || `新托盘 ${index + 1}` }}</strong>
              <a-tag :color="typeColor(pallet.palletType)">{{ typeText(pallet.palletType) }}</a-tag>
              <a-tag v-if="pallet.existingPallet" color="cyan">合并现有托盘</a-tag>
              <a-button v-if="!pallet.existingPallet" size="small" @click="splitPallet(index)">
                拆出一托
              </a-button>
              <a-button
                v-if="index > 0 && !pallet.existingPallet"
                size="small"
                @click="mergePrevious(index)"
              >
                合并到上一托
              </a-button>
            </div>
            <span class="capacity-text">{{ capacityText(pallet) }}</span>
          </div>

          <div class="pallet-body">
            <div class="goods-column">
              <a-select
                :value="pallet.palletId || 0"
                :options="existingPalletOptions(pallet)"
                style="width: 100%; margin-bottom: 10px"
                @change="value => selectPallet(pallet, Number(value))"
              />
              <div v-if="pallet.existingPallet" class="existing-goods">
                原托已有：
                {{
                  selectedExisting(pallet)
                    ?.items.map(item => `${item.skuCode} × ${item.quantity}`)
                    .join('，')
                }}
              </div>
              <div v-for="item in pallet.items" :key="item.skuCode" class="goods-row">
                <span class="sku">{{ item.skuCode }}</span>
                <a-input-number v-model:value="item.quantity" :min="1" :precision="0" addon-after="件" />
                <span v-if="item.quantityPerPallet" class="subtle">标准 {{ item.quantityPerPallet }} 件/托</span>
              </div>
            </div>

            <div class="control-grid">
              <label>
                <span>品质</span>
                <a-select
                  v-model:value="pallet.quality"
                  :disabled="pallet.existingPallet"
                  :options="qualityOptions"
                  @change="changeQuality(pallet)"
                />
              </label>
              <label>
                <span>层位</span>
                <a-select
                  v-model:value="pallet.slotCode"
                  show-search
                  :disabled="pallet.existingPallet"
                  :options="slotOptions(pallet)"
                  :filter-option="filterOption"
                  @change="value => applySlot(pallet, value as string)"
                />
              </label>
              <label>
                <span>容量</span>
                <a-input-number
                  v-model:value="pallet.capacityPercent"
                  :min="1"
                  :max="100"
                  :precision="0"
                  addon-after="%"
                />
              </label>
              <label>
                <span>实际重量</span>
                <a-input-number
                  v-model:value="pallet.actualWeightKg"
                  :min="0"
                  :precision="2"
                  addon-after="kg"
                  placeholder="选填"
                />
              </label>
              <label class="full-toggle">
                <span>人工满托</span>
                <a-switch v-model:checked="pallet.manualFull" />
              </label>
            </div>
          </div>

          <a-progress
            :percent="Math.min(pallet.capacityPercent || 0, 100)"
            :status="pallet.capacityPercent ? 'normal' : 'exception'"
            size="small"
            :show-info="false"
          />
          <div v-if="!pallet.capacityPercent" class="capacity-required">
            当前 SKU 缺少可计算容量的数据，请仓库人员按现场托盘占用情况填写。
          </div>
        </div>
      </section>

      <a-empty v-if="!loading && !pallets.length" description="没有可上架的实收商品" />

      <section v-if="pallets.length" class="billing-section">
        <div class="section-title">入库计费依据</div>
        <a-form layout="inline">
          <a-form-item label="确认体积">
            <a-input-number
              v-model:value="confirmedVolumeCbm"
              :disabled="!volumeNeedsConfirmation"
              :min="0.0001"
              :precision="4"
              addon-after="m³"
              style="width: 190px"
            />
          </a-form-item>
          <a-form-item label="客户原因加班">
            <a-switch v-model:checked="afterHours" />
          </a-form-item>
          <a-form-item v-if="afterHours" label="加班原因">
            <a-input v-model:value="afterHoursReason" placeholder="请填写客户或服务商原因" />
          </a-form-item>
        </a-form>
        <div class="subtle">
          费率：50元/m³；只有勾选客户原因加班时才加收基础操作费的50%。
        </div>
      </section>
    </a-spin>

    <template #footer>
      <div class="footer-row">
        <span :class="allValid ? 'ready' : 'not-ready'">
          {{ allValid ? '数量、分托和托位已确认' : validationText }}
        </span>
        <a-space>
          <a-button @click="open = false">取消</a-button>
          <a-button type="primary" :loading="submitting" :disabled="!allValid" @click="submit">
            确认上架
          </a-button>
        </a-space>
      </div>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { doRequest } from '@/utils/axios/request'
import { getPutawayPlan, putawayInbound } from '@/api/wms/inbound-execution'
import { listPallets, listPalletSlots } from '@/api/wms/pallet'
import type {
  InboundPutawayPlanVO,
  PalletPlan,
  PalletSlotVO,
  PutawayLine
} from '@/api/wms/inbound-execution'
import type { PurchaseInboundPageVO } from '@/api/wms/purchase-inbound/types'
import type { PalletSummaryVO } from '@/api/wms/pallet'

const emits = defineEmits<{ (e: 'success'): void }>()

const open = ref(false)
const loading = ref(false)
const submitting = ref(false)
const currentId = ref<number>()
const currentNo = ref('')
const plan = ref<InboundPutawayPlanVO>()
const receivedTotals = ref(new Map<string, number>())
const confirmedVolumeCbm = ref<number>()
const afterHours = ref(false)
const afterHoursReason = ref('')
const existingPallets = ref<PalletSummaryVO[]>([])
const allSlots = ref<PalletSlotVO[]>([])
let manualSequence = 1

const pallets = computed(() => plan.value?.pallets || [])
const sourceItems = computed(() =>
  Array.from(receivedTotals.value.entries()).map(([skuCode, quantity]) => ({ skuCode, quantity }))
)

const chosenSlots = computed(() => {
  const values = new Set<string>()
  pallets.value.forEach(pallet => pallet.slotCode && values.add(pallet.slotCode))
  return values
})

const qualityOptions = [
  { value: 'GOOD', label: '良品' },
  { value: 'DAMAGED', label: '残次品' }
]

const allocatedTotals = computed(() => {
  const totals = new Map<string, number>()
  pallets.value.forEach(pallet =>
    pallet.items.forEach(item => totals.set(item.skuCode, (totals.get(item.skuCode) || 0) + Number(item.quantity || 0)))
  )
  return totals
})

const totalsMatch = computed(() =>
  Array.from(receivedTotals.value.entries()).every(
    ([sku, quantity]) => allocatedTotals.value.get(sku) === quantity
  )
)

const volumeNeedsConfirmation = computed(() => (plan.value?.volumeMissingSkuCodes || []).length > 0)

const allValid = computed(
  () =>
    pallets.value.length > 0 &&
    totalsMatch.value &&
    (!volumeNeedsConfirmation.value || Number(confirmedVolumeCbm.value || 0) > 0) &&
    (!afterHours.value || !!afterHoursReason.value.trim()) &&
    pallets.value.every(
      pallet =>
        pallet.items.length > 0 &&
        new Set(pallet.items.map(item => item.skuCode)).size <= 4 &&
        !!pallet.slotCode &&
        !!pallet.capacityPercent &&
        pallet.capacityPercent > 0 &&
        pallet.capacityPercent <= 100
    )
)

const validationText = computed(() =>
  totalsMatch.value ? '请补齐托位及托盘容量' : '各SKU分配数量必须与实收数量一致'
)

async function openPutaway(record: PurchaseInboundPageVO) {
  currentId.value = record.id
  currentNo.value = record.inboundNo
  plan.value = undefined
  confirmedVolumeCbm.value = undefined
  afterHours.value = false
  afterHoursReason.value = ''
  open.value = true
  loading.value = true
  try {
    const response = await getPutawayPlan(record.id)
    if (isSuccess(response) && response.data) {
      plan.value = response.data
      receivedTotals.value = new Map()
      confirmedVolumeCbm.value = response.data.calculatedVolumeCbm
      plan.value.pallets.forEach(pallet => {
        pallet.items.forEach(item =>
          receivedTotals.value.set(
            item.skuCode,
            (receivedTotals.value.get(item.skuCode) || 0) + item.quantity
          )
        )
        pallet.actualWeightKg = pallet.estimatedWeightKg
        pallet.manualFull = false
      })
      const ownerId = plan.value.pallets[0]?.items[0]?.erpTenantId
      const [palletResponse, slotResponse] = await Promise.all([
        listPallets({
          warehouseId: plan.value.warehouseId,
          erpTenantId: ownerId,
          status: 'PARTIAL'
        }),
        listPalletSlots(plan.value.warehouseId)
      ])
      existingPallets.value =
        isSuccess(palletResponse) && palletResponse.data
          ? palletResponse.data.filter(item => item.erpTenantId === ownerId)
          : []
      allSlots.value = isSuccess(slotResponse) && slotResponse.data ? slotResponse.data : []
    }
  } finally {
    loading.value = false
  }
}

function selectedExisting(pallet: PalletPlan) {
  return existingPallets.value.find(item => item.id === pallet.palletId)
}

function existingPalletOptions(pallet: PalletPlan) {
  const incomingKinds = new Set(pallet.items.map(item => item.skuCode))
  const options = existingPallets.value
    .filter(item => {
      const qualities = new Set(item.items.map(value => value.quality || 'GOOD'))
      const kinds = new Set([...item.items.map(value => value.skuCode), ...incomingKinds])
      return qualities.size === 1 && qualities.has(pallet.quality) && kinds.size <= 4
    })
    .map(item => ({
      value: item.id,
      label: `并入 ${item.palletNo} · ${item.slotCode} · 已占 ${item.capacityPercent || '?'}%`
    }))
  return [{ value: 0, label: '使用新托盘（人工选择空托位）' }, ...options]
}

function selectPallet(pallet: PalletPlan, palletId: number) {
  if (!palletId) {
    pallet.palletId = undefined
    pallet.palletNo = undefined
    pallet.existingPallet = false
    pallet.slotCode = ''
    pallet.locationCode = ''
    pallet.levelNo = 0
    pallet.capacityPercent = undefined
    pallet.actualWeightKg = undefined
    pallet.manualFull = false
    return
  }
  const existing = existingPallets.value.find(item => item.id === palletId)
  const slot = allSlots.value.find(item => item.palletId === palletId)
  if (!existing || !slot) {
    message.warning('现有托盘位置已变化，请重新打开上架页面')
    return
  }
  pallet.palletId = existing.id
  pallet.palletNo = existing.palletNo
  pallet.existingPallet = true
  pallet.slotCode = slot.slotCode
  pallet.locationCode = slot.locationCode
  pallet.levelNo = slot.levelNo
  pallet.capacityPercent = existing.capacityPercent
  pallet.actualWeightKg = existing.actualWeightKg
  pallet.manualFull = false
}

function slotOptions(pallet: PalletPlan) {
  const candidates = plan.value?.slotCandidates || []
  const zoneType = pallet.quality === 'DAMAGED' ? 'DEFECTIVE' : 'STANDARD'
  return candidates
    .filter(slot => slot.zoneType === zoneType)
    .filter(slot => slot.slotCode === pallet.slotCode || !chosenSlots.value.has(slot.slotCode))
    .map(slot => ({
      value: slot.slotCode,
      label: `${slot.slotCode} · ${slot.zoneName || '标准区'}`
    }))
}

function changeQuality(pallet: PalletPlan) {
  const options = slotOptions(pallet)
  const currentValid = options.some(option => option.value === pallet.slotCode)
  if (!currentValid) {
    pallet.slotCode = options[0]?.value || ''
  }
  if (pallet.slotCode) applySlot(pallet, pallet.slotCode)
}

function applySlot(pallet: PalletPlan, slotCode: string) {
  const slot = (plan.value?.slotCandidates || []).find(candidate => candidate.slotCode === slotCode)
  if (!slot) return
  pallet.locationCode = slot.locationCode
  pallet.levelNo = slot.levelNo
}

function splitPallet(index: number) {
  const source = pallets.value[index]
  if (!source || source.existingPallet || source.items.length !== 1 || source.items[0].quantity < 2) {
    message.warning('单品托盘数量至少为2件时才能拆托')
    return
  }
  const item = source.items[0]
  const moved = Math.max(1, Math.floor(item.quantity / 2))
  item.quantity -= moved
  const copy: PalletPlan = {
    ...source,
    palletKey: `MANUAL-UI-${Date.now()}-${manualSequence++}`,
    palletId: undefined,
    palletNo: undefined,
    existingPallet: false,
    palletType: 'SINGLE_PARTIAL',
    slotCode: '',
    locationCode: '',
    levelNo: 0,
    capacityPercent: undefined,
    actualWeightKg: undefined,
    manualFull: false,
    wholePalletEligible: false,
    items: [{ ...item, quantity: moved }]
  }
  plan.value?.pallets.splice(index + 1, 0, copy)
}

function mergePrevious(index: number) {
  const current = pallets.value[index]
  const target = pallets.value[index - 1]
  if (!current || !target || target.existingPallet || current.existingPallet) return
  const kinds = new Set([...target.items, ...current.items].map(item => item.skuCode))
  if (kinds.size > 4) {
    message.warning('同一托盘最多允许4种SKU')
    return
  }
  current.items.forEach(item => {
    const existing = target.items.find(value => value.skuCode === item.skuCode)
    if (existing) existing.quantity += item.quantity
    else target.items.push({ ...item })
  })
  target.palletType = target.items.length > 1 ? 'MIXED' : 'SINGLE_PARTIAL'
  target.capacityPercent = undefined
  target.manualFull = false
  plan.value?.pallets.splice(index, 1)
}

const filterOption = (input: string, option: { value: string }) =>
  option.value.toLowerCase().includes(input.toLowerCase())

function typeText(type: PalletPlan['palletType']) {
  return { SINGLE_FULL: '单品满托', SINGLE_PARTIAL: '单品半托', MIXED: '混托' }[type]
}

function typeColor(type: PalletPlan['palletType']) {
  return { SINGLE_FULL: 'green', SINGLE_PARTIAL: 'blue', MIXED: 'orange' }[type]
}

function capacityText(pallet: PalletPlan) {
  if (!pallet.capacityPercent) return '待人工确认容量'
  return `预计占用 ${Math.round(pallet.capacityPercent)}%`
}

function buildLines(): PutawayLine[] {
  return pallets.value.flatMap(pallet =>
    pallet.items.map(item => ({
      skuCode: item.skuCode,
      locationCode: pallet.locationCode,
      palletKey: pallet.palletKey,
      palletId: pallet.palletId,
      slotCode: pallet.slotCode,
      quantity: item.quantity,
      quality: pallet.quality,
      capacityPercent: pallet.capacityPercent,
      capacitySource: pallet.capacitySource,
      actualWeightKg: pallet.actualWeightKg,
      manualFull: pallet.manualFull
    }))
  )
}

function submit() {
  if (!allValid.value || !currentId.value) {
    message.warning('请先确认全部托盘的层位和容量')
    return
  }
  submitting.value = true
  doRequest(putawayInbound({
    inboundOrderId: currentId.value,
    lines: buildLines(),
    confirmedVolumeCbm: confirmedVolumeCbm.value,
    afterHours: afterHours.value,
    afterHoursReason: afterHoursReason.value.trim() || undefined
  }), {
    successMessage: '上架成功，托盘与库存已同步',
    onSuccess: response => {
      const created = (response.data || []) as PalletSummaryVO[]
      if (created.length) printLabels(created)
      open.value = false
      emits('success')
    },
    onFinally: () => {
      submitting.value = false
    }
  })
}

function printLabels(created: PalletSummaryVO[]) {
  const printable = window.open('', '_blank', 'width=760,height=680')
  if (!printable) return
  printable.document.write(`<!doctype html><html><head><title>托盘标签</title><style>
    body{font-family:Arial,"Microsoft YaHei",sans-serif;margin:24px}.label{width:420px;min-height:250px;border:2px solid #111;padding:20px;margin:0 0 24px;page-break-after:always;box-sizing:border-box}
    h1{font-size:28px;margin:0 0 10px;letter-spacing:0}.owner{font-size:18px;font-weight:700}.meta{margin-top:12px;font-size:15px;line-height:1.7}.barcode{font-family:monospace;font-size:22px;letter-spacing:1px;margin-top:12px}
  </style></head><body>${created
    .map(
      pallet => `<div class="label"><h1>${pallet.palletNo}</h1><div class="owner">货主：${pallet.ownerName || pallet.erpTenantId || '-'}</div><div>服务商：${pallet.wmsTenantName || pallet.wmsTenantId || '-'}</div><div class="meta">${pallet.items
        .map(item => `${item.skuCode} × ${item.quantity}`)
        .join('<br>')}</div><div class="barcode">*${pallet.palletNo}*</div></div>`
    )
    .join('')}</body></html>`)
  printable.document.close()
  printable.focus()
  printable.print()
}

defineExpose({ open: openPutaway })
</script>

<script lang="ts">
export default { name: 'PutawayDrawer' }
</script>

<style scoped>
.steps { margin-bottom: 20px; }
.warning { margin-bottom: 10px; }
.source-band { padding: 14px 0 18px; border-bottom: 1px solid #f0f0f0; }
.section-title { font-size: 15px; font-weight: 600; margin-bottom: 8px; }
.source-list { display: flex; flex-wrap: wrap; gap: 10px; }
.source-item { display: inline-flex; gap: 12px; padding: 6px 10px; background: #f5f5f5; border-radius: 4px; }
.existing-goods { margin: -2px 0 10px; color: #1677ff; font-size: 12px; }
.plan-section { padding-top: 18px; }
.plan-header, .pallet-head, .pallet-body, .footer-row { display: flex; justify-content: space-between; align-items: center; }
.plan-header { margin-bottom: 12px; }
.subtle { color: rgba(0,0,0,.45); font-size: 12px; }
.pallet-card { border: 1px solid #e5e7eb; border-radius: 6px; padding: 14px; margin-bottom: 10px; }
.pallet-head { margin-bottom: 12px; }
.pallet-name { display: flex; align-items: center; gap: 8px; }
.sequence { display: inline-flex; width: 24px; height: 24px; align-items: center; justify-content: center; border-radius: 50%; background: #1677ff; color: #fff; }
.capacity-text { color: rgba(0,0,0,.65); }
.pallet-body { align-items: flex-start; gap: 20px; }
.goods-column { flex: 1; min-width: 240px; }
.goods-row { display: grid; grid-template-columns: minmax(110px,1fr) 70px 120px; gap: 8px; min-height: 30px; align-items: center; }
.sku { font-weight: 600; overflow-wrap: anywhere; }
.control-grid { flex: 0 0 480px; display: grid; grid-template-columns: 1.45fr 1fr 1fr 76px; gap: 10px; }
.control-grid label { display: grid; gap: 5px; font-size: 12px; color: rgba(0,0,0,.65); }
.control-grid :deep(.ant-input-number), .control-grid :deep(.ant-select) { width: 100%; }
.full-toggle { justify-items: center; }
.capacity-required { color: #d46b08; font-size: 12px; margin-top: 4px; }
.billing-section { border-top: 1px solid #f0f0f0; margin-top: 16px; padding-top: 16px; }
.ready { color: #389e0d; }
.not-ready { color: #cf1322; }
</style>
