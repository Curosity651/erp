<template>
  <a-drawer
    v-model:open="open"
    :title="drawerTitle"
    :width="820"
    destroy-on-close
    placement="right"
  >
    <a-alert
      type="info"
      show-icon
      style="margin-bottom: 12px"
      message="为每个 SKU 分配库位：仅可选「该货主的服务商在本仓租用货架」上的空闲库位；库位独占，良品入标准区、次品入不良品区；每个 SKU 的分配数量之和须等于实收数量，可拆分到多个库位。若下拉无库位，多为该服务商在本仓未租货架或对应分区无空位。"
    />
    <a-spin :spinning="loading">
      <div v-for="group in groups" :key="group.skuCode" class="sku-block">
        <div class="sku-head">
          <div class="sku-title">
            <span class="sku-code">{{ group.skuCode }}</span>
            <span class="sku-recv">实收 {{ group.actualQuantity }}</span>
            <a-tag :color="groupOk(group) ? 'success' : 'error'">
              已分配 {{ allocated(group) }} / {{ group.actualQuantity }}
            </a-tag>
          </div>
          <a-button size="small" type="link" @click="addRow(group)">+ 添加库位</a-button>
        </div>

        <div v-for="(row, idx) in group.rows" :key="idx" class="alloc-row">
          <a-select v-model:value="row.quality" class="col-quality" @change="onQualityChange(row)">
            <a-select-option value="GOOD">良品</a-select-option>
            <a-select-option value="DAMAGED">次品</a-select-option>
          </a-select>

          <a-select
            v-model:value="row.locationCode"
            class="col-location"
            show-search
            allow-clear
            :placeholder="locLoading[row.quality] ? '加载库位中…' : '选择库位（空闲）'"
            :options="optionsFor(row)"
            :filter-option="filterOption"
            :not-found-content="
              locLoading[row.quality] ? undefined : '该服务商租用货架上暂无匹配空闲库位'
            "
            @change="(v: any) => onLocationChange(row, v)"
          />

          <a-input-number
            v-model:value="row.quantity"
            class="col-qty"
            :min="1"
            :max="rowMax(group, row)"
            :precision="0"
            placeholder="数量"
          />

          <a-button
            type="text"
            danger
            class="col-del"
            :disabled="group.rows.length <= 1"
            @click="removeRow(group, idx)"
          >
            删除
          </a-button>
        </div>
      </div>

      <a-empty v-if="!loading && groups.length === 0" description="无可上架明细" />
    </a-spin>

    <template #footer>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span :style="{ color: allOk ? '#52c41a' : '#ff4d4f' }">
          {{ allOk ? '分配已就绪' : '请完成全部 SKU 的库位分配（数量需与实收一致）' }}
        </span>
        <div style="display: flex; gap: 8px">
          <a-button @click="open = false">取消</a-button>
          <a-button type="primary" :loading="submitting" :disabled="!allOk" @click="submit">
            确认上架
          </a-button>
        </div>
      </div>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { doRequest } from '@/utils/axios/request'
import {
  getInboundOpsDetail,
  putawayInbound,
  listPutawayLocations
} from '@/api/wms/inbound-execution'
import type { AvailableLocationVO } from '@/api/wms/inbound-execution'
import type { PurchaseInboundPageVO } from '@/api/wms/purchase-inbound/types'

const emits = defineEmits<{ (e: 'success'): void }>()

type Quality = 'GOOD' | 'DAMAGED'

interface AllocRow {
  quality: Quality
  locationCode?: string
  zoneId?: number
  quantity: number
}
interface SkuGroup {
  skuCode: string
  actualQuantity: number
  rows: AllocRow[]
}

const open = ref(false)
const loading = ref(false)
const submitting = ref(false)
const currentId = ref<number>()
const currentNo = ref('')
const warehouseId = ref<number>()
const groups = ref<SkuGroup[]>([])

// 按品质缓存该仓可用（空闲）库位
const locCache = reactive<Record<Quality, AvailableLocationVO[]>>({ GOOD: [], DAMAGED: [] })
const locLoaded = reactive<Record<Quality, boolean>>({ GOOD: false, DAMAGED: false })
const locLoading = reactive<Record<Quality, boolean>>({ GOOD: false, DAMAGED: false })

const drawerTitle = computed(() => `上架作业 · ${currentNo.value}`)

async function loadLocations(quality: Quality) {
  if (!currentId.value || locLoaded[quality] || locLoading[quality]) return
  locLoading[quality] = true
  try {
    const res = await listPutawayLocations(currentId.value, quality)
    if (isSuccess(res) && res.data) {
      locCache[quality] = res.data
      locLoaded[quality] = true
    }
  } finally {
    locLoading[quality] = false
  }
}

async function openPutaway(record: PurchaseInboundPageVO) {
  currentId.value = record.id
  currentNo.value = record.inboundNo
  warehouseId.value = record.warehouseId
  open.value = true
  loading.value = true
  groups.value = []
  locCache.GOOD = []
  locCache.DAMAGED = []
  locLoaded.GOOD = false
  locLoaded.DAMAGED = false
  try {
    const res = await getInboundOpsDetail(record.id)
    if (isSuccess(res) && res.data) {
      groups.value = (res.data.items || [])
        .filter(i => (i.actualQuantity || 0) > 0)
        .map(i => ({
          skuCode: i.skuCode,
          actualQuantity: i.actualQuantity,
          rows: [{ quality: 'GOOD' as Quality, quantity: i.actualQuantity }]
        }))
    }
    await loadLocations('GOOD')
  } finally {
    loading.value = false
  }
}

// 全表已选库位码（用于库位独占过滤）
const chosenCodes = computed(() => {
  const s = new Set<string>()
  groups.value.forEach(g => g.rows.forEach(r => r.locationCode && s.add(r.locationCode)))
  return s
})

// 某行的可选库位：该品质分区内的空库位，排除其它行已选（本行当前值保留）
function optionsFor(row: AllocRow) {
  const list = locCache[row.quality] || []
  return list
    .filter(l => l.locationCode === row.locationCode || !chosenCodes.value.has(l.locationCode))
    .map(l => ({
      value: l.locationCode,
      label: l.zoneName ? `${l.locationCode} · ${l.zoneName}` : l.locationCode,
      zoneId: l.zoneId
    }))
}

const filterOption = (input: string, option: { value: string }) =>
  option.value.toLowerCase().includes(input.toLowerCase())

function onQualityChange(row: AllocRow) {
  row.locationCode = undefined
  row.zoneId = undefined
  loadLocations(row.quality)
}

function onLocationChange(row: AllocRow, code?: string) {
  const hit = (locCache[row.quality] || []).find(l => l.locationCode === code)
  row.zoneId = hit?.zoneId
}

function addRow(group: SkuGroup) {
  const remaining = group.actualQuantity - allocated(group)
  if (remaining <= 0) {
    message.warning('该 SKU 已按实收数量分配完，无法再加库位')
    return
  }
  group.rows.push({ quality: 'GOOD', quantity: remaining })
  loadLocations('GOOD')
}
function removeRow(group: SkuGroup, idx: number) {
  if (group.rows.length > 1) group.rows.splice(idx, 1)
}

const allocated = (group: SkuGroup) => group.rows.reduce((sum, r) => sum + (r.quantity || 0), 0)
const groupOk = (group: SkuGroup) => allocated(group) === group.actualQuantity

// 该行数量上限 = 实收数量 - 本组其它行已分配，保证各 SKU 上架总量不超过实收数量
function rowMax(group: SkuGroup, row: AllocRow): number {
  const others = group.rows.reduce((sum, r) => sum + (r === row ? 0 : r.quantity || 0), 0)
  return Math.max(group.actualQuantity - others, 0)
}

const allOk = computed(
  () =>
    groups.value.length > 0 &&
    groups.value.every(g => groupOk(g) && g.rows.every(r => !!r.locationCode && r.quantity > 0))
)

function submit() {
  if (!allOk.value) {
    message.warning('请完成全部 SKU 的库位分配，且分配数量与实收一致')
    return
  }
  const lines = groups.value.flatMap(g =>
    g.rows.map(r => ({
      skuCode: g.skuCode,
      locationCode: r.locationCode as string,
      quantity: r.quantity,
      quality: r.quality,
      zoneId: r.zoneId
    }))
  )
  submitting.value = true
  doRequest(putawayInbound({ inboundOrderId: currentId.value!, lines }), {
    successMessage: '上架成功，库存已增加',
    onSuccess: () => {
      open.value = false
      emits('success')
    },
    onFinally: () => {
      submitting.value = false
    }
  })
}

defineExpose({ open: openPutaway })
</script>

<script lang="ts">
export default {
  name: 'PutawayDrawer'
}
</script>

<style scoped>
.sku-block {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
}
.sku-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.sku-title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.sku-code {
  font-weight: 600;
}
.sku-recv {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
.alloc-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.col-quality {
  width: 96px;
  flex: none;
}
.col-location {
  flex: 1 1 auto;
}
.col-qty {
  width: 120px;
  flex: none;
}
.col-del {
  flex: none;
}
</style>
