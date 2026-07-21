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
            <div class="section-title">系统分托结果</div>
            <div class="subtle">共 {{ pallets.length }} 托，满托优先 L3，半托与混托优先 L1</div>
          </div>
          <a-tag color="blue">每层 1 托 · 每库位 3 层</a-tag>
        </div>

        <div v-for="(pallet, index) in pallets" :key="pallet.palletKey" class="pallet-card">
          <div class="pallet-head">
            <div class="pallet-name">
              <span class="sequence">{{ index + 1 }}</span>
              <strong>{{ pallet.palletNo || `新托盘 ${index + 1}` }}</strong>
              <a-tag :color="typeColor(pallet.palletType)">{{ typeText(pallet.palletType) }}</a-tag>
              <a-tag v-if="pallet.existingPallet" color="cyan">合并现有托盘</a-tag>
            </div>
            <span class="capacity-text">{{ capacityText(pallet) }}</span>
          </div>

          <div class="pallet-body">
            <div class="goods-column">
              <div v-for="item in pallet.items" :key="item.skuCode" class="goods-row">
                <span class="sku">{{ item.skuCode }}</span>
                <span>{{ item.quantity }} 件</span>
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
    </a-spin>

    <template #footer>
      <div class="footer-row">
        <span :class="allValid ? 'ready' : 'not-ready'">
          {{ allValid ? '分托和层位已确认' : '请补齐层位及托盘容量' }}
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

const pallets = computed(() => plan.value?.pallets || [])
const sourceItems = computed(() => {
  const totals = new Map<string, number>()
  pallets.value.forEach(pallet =>
    pallet.items.forEach(item => totals.set(item.skuCode, (totals.get(item.skuCode) || 0) + item.quantity))
  )
  return Array.from(totals.entries()).map(([skuCode, quantity]) => ({ skuCode, quantity }))
})

const chosenSlots = computed(() => {
  const values = new Set<string>()
  pallets.value.forEach(pallet => pallet.slotCode && values.add(pallet.slotCode))
  return values
})

const qualityOptions = [
  { value: 'GOOD', label: '良品' },
  { value: 'DAMAGED', label: '残次品' }
]

const allValid = computed(
  () =>
    pallets.value.length > 0 &&
    pallets.value.every(
      pallet => !!pallet.slotCode && !!pallet.capacityPercent && pallet.capacityPercent > 0 && pallet.capacityPercent <= 100
    )
)

async function openPutaway(record: PurchaseInboundPageVO) {
  currentId.value = record.id
  currentNo.value = record.inboundNo
  plan.value = undefined
  open.value = true
  loading.value = true
  try {
    const response = await getPutawayPlan(record.id)
    if (isSuccess(response) && response.data) {
      plan.value = response.data
      plan.value.pallets.forEach(pallet => {
        pallet.actualWeightKg = pallet.estimatedWeightKg
        pallet.manualFull = pallet.palletType === 'SINGLE_FULL'
      })
    }
  } finally {
    loading.value = false
  }
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
  doRequest(putawayInbound({ inboundOrderId: currentId.value, lines: buildLines() }), {
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
    body{font-family:Arial,"Microsoft YaHei",sans-serif;margin:24px}.label{width:420px;height:250px;border:2px solid #111;padding:20px;margin:0 0 24px;page-break-after:always;box-sizing:border-box}
    h1{font-size:30px;margin:0 0 18px;letter-spacing:0}.slot{font-size:26px;font-weight:700}.meta{margin-top:16px;font-size:16px;line-height:1.8}.barcode{font-family:monospace;font-size:24px;letter-spacing:2px;margin-top:12px}
  </style></head><body>${created
    .map(
      pallet => `<div class="label"><h1>${pallet.palletNo}</h1><div class="slot">${pallet.slotCode || ''}</div><div class="meta">${pallet.items
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
.ready { color: #389e0d; }
.not-ready { color: #cf1322; }
</style>
