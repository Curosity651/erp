<template>
  <a-drawer
    v-model:open="open"
    :title="drawerTitle"
    :width="720"
    destroy-on-close
    placement="right"
  >
    <template v-if="current">
      <!-- ① 结构设计 -->
      <section class="drawer-section">
        <div class="section-title">
          <span>结构设计</span>
          <a-tag v-if="currentGenerated" color="green">已生成 · {{ locations.length }}</a-tag>
        </div>
        <a-alert
          v-if="structureLocked"
          type="warning"
          show-icon
          :message="lockReason"
          style="margin-bottom: 12px"
        />
        <a-form :model="form" :label-col="{ style: { width: '76px' } }" class="structure-form">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="排数">
                <a-input-number
                  v-model:value="form.rackRows"
                  :min="0"
                  :max="999"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="每排列数">
                <a-input-number
                  v-model:value="form.rackColumns"
                  :min="0"
                  :max="999"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="排号前缀">
                <a-input v-model:value="form.rackNoPrefix" placeholder="如 A" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="列号补零">
                <a-input-number
                  v-model:value="form.codePadWidth"
                  :min="1"
                  :max="4"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-space v-if="canEdit" class="struct-btns">
                <a-button
                  type="primary"
                  :loading="savingStructure"
                  :disabled="structureLocked"
                  @click="saveStructure"
                >
                  保存结构
                </a-button>
                <a-popconfirm
                  :title="generateConfirmText"
                  ok-text="确定"
                  cancel-text="取消"
                  :disabled="structureLocked"
                  @confirm="doGenerate"
                >
                  <a-button :loading="generating" :disabled="structureLocked">
                    {{ currentGenerated ? '重新生成' : '生成库位' }}
                  </a-button>
                </a-popconfirm>
              </a-space>
            </a-col>
          </a-row>
          <a-divider orientation="left" plain>托盘规则</a-divider>
          <a-row :gutter="16">
            <a-col :span="6"
              ><a-form-item label="层数"
                ><a-input-number
                  v-model:value="form.palletLevels"
                  :min="1"
                  :max="12"
                  style="width: 100%" /></a-form-item
            ></a-col>
            <a-col :span="6"
              ><a-form-item label="每层托位"
                ><a-input-number
                  v-model:value="form.palletPositionsPerLevel"
                  :min="1"
                  :max="9"
                  style="width: 100%" /></a-form-item
            ></a-col>
            <a-col :span="6"
              ><a-form-item label="最多品种"
                ><a-input-number
                  v-model:value="form.maxSkuKindsPerPallet"
                  :min="1"
                  :max="4"
                  style="width: 100%" /></a-form-item
            ></a-col>
            <a-col :span="6"
              ><a-form-item label="跨货主混托"
                ><a-switch v-model:checked="allowCrossOwnerMix" disabled /></a-form-item
            ></a-col>
            <a-col :span="6"
              ><a-form-item label="利用率"
                ><a-input-number
                  v-model:value="form.defaultPalletUtilization"
                  :min="0.1"
                  :max="1"
                  :step="0.05"
                  :precision="2"
                  style="width: 100%" /></a-form-item
            ></a-col>
            <a-col :span="6"
              ><a-form-item label="长(mm)"
                ><a-input-number
                  v-model:value="form.defaultPalletLengthMm"
                  :min="1"
                  style="width: 100%" /></a-form-item
            ></a-col>
            <a-col :span="6"
              ><a-form-item label="宽(mm)"
                ><a-input-number
                  v-model:value="form.defaultPalletWidthMm"
                  :min="1"
                  style="width: 100%" /></a-form-item
            ></a-col>
            <a-col :span="6"
              ><a-form-item label="高(mm)"
                ><a-input-number
                  v-model:value="form.defaultPalletHeightMm"
                  :min="1"
                  style="width: 100%" /></a-form-item
            ></a-col>
            <a-col :span="6"
              ><a-form-item label="承重(kg)"
                ><a-input-number
                  v-model:value="form.defaultPalletMaxWeightKg"
                  :min="1"
                  style="width: 100%" /></a-form-item
            ></a-col>
          </a-row>
        </a-form>
        <div class="hint">
          二维库位：<b>{{ expectedCount }}</b> 个；托盘位：
          <b>{{ expectedSlotCount }}</b> 个（{{ form.palletLevels }} 层 ×
          {{ form.palletPositionsPerLevel }} 托）
        </div>
      </section>

      <!-- ② 品质分区（固定四类，同时作为下方网格的配色图例） -->
      <section class="drawer-section">
        <div class="section-title">品质分区</div>
        <a-space wrap>
          <a-tag v-for="z in zones" :key="z.id" :color="zoneTagColor(z.zoneType)">
            {{ z.zoneName }}
          </a-tag>
        </a-space>
      </section>

      <!-- ③ 库位 -->
      <section class="drawer-section">
        <div class="section-title">
          <span>库位（共 {{ locations.length }} 个）</span>
          <a-space>
            <a-button v-if="locations.length > 0" size="small" @click="printLocationLabels">
              打印库位标签
            </a-button>
            <a-button v-if="palletSlots.length > 0" size="small" @click="openSlotPrintDialog">
              打印托位标签
            </a-button>
            <a-radio-group v-model:value="viewMode" size="small" button-style="solid">
              <a-radio-button value="grid">网格视图</a-radio-button>
              <a-radio-button value="list">列表视图</a-radio-button>
            </a-radio-group>
            <a-button
              v-if="canEdit && viewMode === 'grid' && locations.length > 0"
              size="small"
              :type="settingMode ? 'default' : 'primary'"
              @click="toggleSetting"
            >
              {{ settingMode ? '退出设置' : '库位设置' }}
            </a-button>
          </a-space>
        </div>

        <a-spin :spinning="loadingLocations">
          <template v-if="locations.length === 0">
            <a-empty description="暂无库位，请先设计结构并生成" />
          </template>

          <!-- 网格视图：排 × 列，按分区上色；库位设置模式下可框选/整排整列选后设分区 -->
          <template v-else-if="viewMode === 'grid'">
            <div v-if="settingMode" class="assign-bar">
              <span class="sel-hint"
                >已选 <b>{{ selectedIds.size }}</b> 个 · 设为：</span
              >
              <a-button
                v-for="z in zones"
                :key="z.id"
                size="small"
                :disabled="selectedIds.size === 0"
                :loading="assigning"
                @click="assignZone(z.id)"
              >
                {{ z.zoneName }}
              </a-button>
              <a-divider type="vertical" />
              <a-button size="small" @click="selectAll">全选</a-button>
              <a-button size="small" :disabled="selectedIds.size === 0" @click="clearSelection">
                清除
              </a-button>
            </div>
            <div v-if="settingMode" class="sel-tip">
              拖拽框选一片区域、或单击切换；点「排号 / 列号」表头可整排 / 整列选
            </div>

            <div class="grid-wrap">
              <table class="rack-grid" :class="{ selectable: settingMode }">
                <thead>
                  <tr>
                    <th class="corner"></th>
                    <th
                      v-for="c in maxColumn"
                      :key="c"
                      :class="{ clickable: settingMode }"
                      @click="settingMode && selectColumn(c)"
                    >
                      {{ c }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(rn, ri) in rackNos" :key="rn">
                    <th
                      class="rack-head"
                      :class="{ clickable: settingMode }"
                      @click="settingMode && selectRow(rn)"
                    >
                      {{ rn }}
                    </th>
                    <td v-for="c in maxColumn" :key="c">
                      <a-tooltip
                        v-if="cellMap[`${rn}|${c}`]"
                        :title="cellTip(cellMap[`${rn}|${c}`])"
                      >
                        <div
                          class="cell filled"
                          :class="{
                            selected: selectedIds.has(cellMap[`${rn}|${c}`].id),
                            preview: settingMode && inDragRect(ri, c),
                            locked:
                              settingMode && occupiedCodes.has(cellMap[`${rn}|${c}`].locationCode)
                          }"
                          :style="{ background: zoneCellColor(zoneTypeOf(cellMap[`${rn}|${c}`])) }"
                          @mousedown="onCellDown(ri, c, $event)"
                          @mouseenter="onCellEnter(ri, c)"
                        >
                          <strong>{{ cellMap[`${rn}|${c}`].locationCode }}</strong>
                          <span
                            v-for="level in slotLevelsFor(cellMap[`${rn}|${c}`].id)"
                            :key="level.levelNo"
                            :class="{ occupied: level.occupiedCount > 0 }"
                            :title="level.detail"
                          >
                            L{{ level.levelNo }} {{ level.occupiedCount }}/{{ level.totalCount }}
                          </span>
                        </div>
                      </a-tooltip>
                      <div v-else class="cell empty" />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>

          <!-- 列表视图 -->
          <a-table
            v-else
            :columns="locationColumns"
            :data-source="locations"
            row-key="id"
            size="small"
            :pagination="{
              defaultPageSize: 10,
              showSizeChanger: true,
              pageSizeOptions: ['10', '20', '50', '100'],
              showTotal: (t: number) => `共 ${t} 个`
            }"
          />
        </a-spin>
      </section>

      <!-- ④ 虚拟库位（收纳积压货：服务商不可见、货主数量不变、不参与自动发货；搬运走「库位调整」） -->
      <section class="drawer-section">
        <div class="section-title">
          <span>虚拟库位（{{ virtualLocs.length }} 个）</span>
        </div>
        <div class="virtual-hint">
          虚拟库位不占物理货架、服务商看不到。用于把积压货经「库位调整」收纳存放；货主库存数量不变。可自定义增删、留空即不启用。
        </div>
        <div v-if="canEdit" class="virtual-add">
          <a-input
            v-model:value="newVirtualName"
            placeholder="虚拟库位名称，如 集装箱A / 海关暂存"
            style="width: 240px"
            :maxlength="32"
            @press-enter="addVirtual"
          />
          <a-button type="primary" :loading="addingVirtual" @click="addVirtual"
            >新增虚拟库位</a-button
          >
        </div>
        <a-table
          size="small"
          row-key="id"
          :data-source="virtualLocs"
          :pagination="false"
          :columns="[
            { title: '名称/编码', dataIndex: 'locationCode', key: 'locationCode' },
            { title: '操作', key: 'op', width: 80 }
          ]"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'op'">
              <a-popconfirm
                title="确认删除该虚拟库位？（其上有货则不可删）"
                @confirm="removeVirtual(record.id)"
              >
                <a-button type="link" danger size="small" :disabled="!canEdit">删除</a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </section>
    </template>
  </a-drawer>

  <a-modal
    v-model:open="slotPrintOpen"
    title="打印托位标签"
    ok-text="打印"
    cancel-text="取消"
    :confirm-loading="slotPrinting"
    :ok-button-props="{ disabled: selectedPrintSlots.length === 0 }"
    @ok="printPalletSlotLabels"
  >
    <a-form layout="vertical">
      <a-form-item label="选择基础库位">
        <a-select
          v-model:value="slotPrintLocationIds"
          mode="multiple"
          show-search
          :max-tag-count="3"
          :options="slotPrintLocationOptions"
          placeholder="请选择需要打印托位标签的库位"
        />
      </a-form-item>
    </a-form>
    <a-space>
      <a-button size="small" @click="selectAllPrintLocations">选择全部</a-button>
      <a-button size="small" @click="slotPrintLocationIds = []">清空</a-button>
      <span class="hint">
        已选 {{ slotPrintLocationIds.length }} 个库位，共 {{ selectedPrintSlots.length }} 张托位标签
      </span>
    </a-space>
    <a-alert
      v-if="selectedPrintSlots.length > 500"
      type="warning"
      show-icon
      message="本次标签较多，建议按排或按部分库位分批打印。"
      style="margin-top: 12px"
    />
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import {
  updateWarehouseStructure,
  listZones,
  initDefaultZones,
  generateLocations,
  listLocations,
  listOccupiedLocations,
  moveLocationZone,
  listVirtualLocations,
  createVirtualLocation,
  deleteVirtualLocation
} from '@/api/wms/location-mgmt'
import type { WarehouseStructure, WmsZone, WmsLocation } from '@/api/wms/location-mgmt/types'
import { useAuthorize } from '@/hooks/permission'
import { listPalletSlots } from '@/api/wms/pallet'
import type { PalletSlotVO } from '@/api/wms/inbound-execution'
import QRCode from 'qrcode'

const emits = defineEmits<{ (e: 'success'): void }>()

// 破坏性库位/货架操作权限门（后端 WmsLocationManageController 全部要求 wms:warehouse:edit）
const { hasPermission } = useAuthorize()
const canEdit = computed(() => hasPermission('wms:warehouse:edit'))

const locationColumns = [
  { title: '库位编码', dataIndex: 'locationCode', key: 'locationCode' },
  { title: '排号', dataIndex: 'rackNo', key: 'rackNo' },
  { title: '列', dataIndex: 'columnNo', key: 'columnNo' },
  {
    title: '分区',
    key: 'zone',
    customRender: ({ record }: { record: WmsLocation }) => zoneNameOf(record)
  }
]

// 品质分区配色（同时用于分区 tag 图例与网格格子）
const ZONE_TAG: Record<string, string> = {
  STANDARD: 'blue',
  DEFECTIVE: 'red',
  RETURN: 'green',
  TEMP: 'default'
}
const ZONE_CELL: Record<string, string> = {
  STANDARD: '#bae0ff',
  DEFECTIVE: '#ffccc7',
  RETURN: '#d9f7be',
  TEMP: '#f0f0f0'
}
function zoneTagColor(t?: string) {
  return (t && ZONE_TAG[t]) || 'default'
}
function zoneCellColor(t?: string) {
  return (t && ZONE_CELL[t]) || '#e6e6e6'
}

const open = ref(false)
const current = ref<WarehouseStructure | undefined>(undefined)
const zones = ref<WmsZone[]>([])
const locations = ref<WmsLocation[]>([])
const palletSlots = ref<PalletSlotVO[]>([])
const viewMode = ref<'grid' | 'list'>('grid')
const slotPrintOpen = ref(false)
const slotPrinting = ref(false)
const slotPrintLocationIds = ref<number[]>([])

const savingStructure = ref(false)
const generating = ref(false)
const loadingLocations = ref(false)
const assigning = ref(false)
const selectedIds = ref<Set<number>>(new Set())
/** 有货占用的库位编码（改分区时锁定这些格子） */
const occupiedCodes = ref<Set<string>>(new Set())
const virtualLocs = ref<WmsLocation[]>([])
const newVirtualName = ref('')
const addingVirtual = ref(false)

const form = reactive({
  rackRows: 0,
  rackColumns: 0,
  rackNoPrefix: '',
  codePadWidth: 2,
  palletLevels: 6,
  palletPositionsPerLevel: 3,
  maxSkuKindsPerPallet: 4,
  allowCrossOwnerMix: 0,
  defaultPalletLengthMm: 1200,
  defaultPalletWidthMm: 1000,
  defaultPalletHeightMm: 1600,
  defaultPalletMaxWeightKg: 1000,
  defaultPalletUtilization: 0.85
})
const allowCrossOwnerMix = computed({
  get: () => form.allowCrossOwnerMix === 1,
  set: value => {
    form.allowCrossOwnerMix = value ? 1 : 0
  }
})

const drawerTitle = computed(() =>
  current.value
    ? `库位管理 · ${current.value.warehouseName}（${current.value.warehouseCode}）`
    : '库位管理'
)
const currentGenerated = computed(() => current.value?.locationGenerated === 1)
const structureLocked = computed(() => !!current.value?.structureLocked)
const lockReason = computed(() => {
  const w = current.value
  if (!w) return ''
  const parts: string[] = []
  if (w.occupied) parts.push(`该仓有货物占用（${w.occupiedLocationCount || 0} 个库位）`)
  if (w.assigned) {
    const names = (w.assignedOperatorNames || []).join('、')
    const racks = w.assignedRackCount ? `，共 ${w.assignedRackCount} 排` : ''
    parts.push(`货架已分配给服务商${names ? `「${names}」` : ''}${racks}`)
  }
  return `${parts.join('；')}。请先清空货物 / 到「货架分配」页解除分配后，再调整结构。`
})
/** 有货占用的库位ID（改分区锁定用） */
const lockedIds = computed(
  () => new Set(locations.value.filter(l => occupiedCodes.value.has(l.locationCode)).map(l => l.id))
)
const expectedCount = computed(() => (form.rackRows || 0) * (form.rackColumns || 0))
const expectedSlotCount = computed(
  () => expectedCount.value * (form.palletLevels || 0) * (form.palletPositionsPerLevel || 0)
)
const slotPrintLocationOptions = computed(() =>
  locations.value.map(location => ({
    label: `${location.locationCode} · ${zoneNameOf(location)}`,
    value: location.id
  }))
)
const selectedPrintSlots = computed(() => {
  return slotPrintLocationIds.value
    .flatMap(locationId => slotsByLocationId.value.get(locationId) || [])
    .sort(
      (a, b) =>
        a.locationCode.localeCompare(b.locationCode, undefined, { numeric: true }) ||
        a.levelNo - b.levelNo ||
        (a.positionNo || 0) - (b.positionNo || 0)
    )
})
const generateConfirmText = computed(() =>
  currentGenerated.value
    ? '重新生成将清空原有库位并按当前结构重建，确定？'
    : '生成后可在无货物占用时重新生成，确定生成？'
)

/** 排号自然排序（A1,A2,...,A10） */
function rackNum(rackNo: string): number {
  const m = rackNo.match(/(\d+)\s*$/)
  return m ? parseInt(m[1]!, 10) : 0
}
const rackNos = computed(() => {
  const set = Array.from(new Set(locations.value.map(l => l.rackNo)))
  return set.sort((a, b) => rackNum(a) - rackNum(b) || a.localeCompare(b))
})
const maxColumn = computed(() => locations.value.reduce((m, l) => Math.max(m, l.columnNo || 0), 0))
const cellMap = computed<Record<string, WmsLocation>>(() => {
  const map: Record<string, WmsLocation> = {}
  for (const l of locations.value) {
    map[`${l.rackNo}|${l.columnNo}`] = l
  }
  return map
})

const zoneById = computed<Record<number, WmsZone>>(() => {
  const m: Record<number, WmsZone> = {}
  for (const z of zones.value) m[z.id] = z
  return m
})
function zoneTypeOf(loc: WmsLocation): string | undefined {
  return loc.zoneId != null ? zoneById.value[loc.zoneId]?.zoneType : undefined
}
function zoneNameOf(loc: WmsLocation): string {
  return (loc.zoneId != null && zoneById.value[loc.zoneId]?.zoneName) || '-'
}
function cellTip(loc: WmsLocation): string {
  const base = `${loc.locationCode} · ${zoneNameOf(loc)}`
  return settingMode.value && occupiedCodes.value.has(loc.locationCode)
    ? `${base}（该库位有货，不能改分区）`
    : base
}

const slotsByLocationId = computed(() => {
  const grouped = new Map<number, PalletSlotVO[]>()
  for (const slot of palletSlots.value) {
    const slots = grouped.get(slot.locationId)
    if (slots) slots.push(slot)
    else grouped.set(slot.locationId, [slot])
  }
  for (const slots of grouped.values()) {
    slots.sort(
      (a, b) => b.levelNo - a.levelNo || (a.positionNo || 0) - (b.positionNo || 0)
    )
  }
  return grouped
})

interface SlotLevelSummary {
  levelNo: number
  occupiedCount: number
  totalCount: number
  detail: string
}

const slotLevelsByLocationId = computed(() => {
  const result = new Map<number, SlotLevelSummary[]>()
  for (const [locationId, slots] of slotsByLocationId.value) {
    const levels = new Map<number, PalletSlotVO[]>()
    for (const slot of slots) {
      const levelSlots = levels.get(slot.levelNo)
      if (levelSlots) levelSlots.push(slot)
      else levels.set(slot.levelNo, [slot])
    }
    result.set(
      locationId,
      Array.from(levels.entries())
        .sort(([a], [b]) => b - a)
        .map(([levelNo, levelSlots]) => ({
          levelNo,
          occupiedCount: levelSlots.filter(slot => !!slot.palletId).length,
          totalCount: levelSlots.length,
          detail: levelSlots
            .map(
              slot =>
                `P${slot.positionNo || '-'}：${
                  slot.palletId
                    ? `${slot.palletNo || '已占用'}（${Math.round(slot.capacityPercent || 0)}%）`
                    : '空'
                }`
            )
            .join('，')
        }))
    )
  }
  return result
})

function slotLevelsFor(locationId: number) {
  return slotLevelsByLocationId.value.get(locationId) || []
}

async function printLocationLabels() {
  if (!current.value || locations.value.length === 0) return
  const cards = await Promise.all(
    locations.value.map(async location => ({
      location,
      qr: await QRCode.toDataURL(location.locationCode, { margin: 1, width: 220 })
    }))
  )
  const html = cards
    .map(
      ({ location, qr }) => `<section class="label">
    <img src="${qr}" alt="${location.locationCode}">
    <div><div class="kind">库位</div><div class="code">${location.locationCode}</div>
    <div class="sub">${current.value?.warehouseName || ''} · ${zoneNameOf(location)}</div></div>
  </section>`
    )
    .join('')
  const page = window.open('', '_blank', 'width=700,height=800')
  if (!page) {
    message.warning('打印窗口被浏览器拦截，请允许弹出窗口后重试')
    return
  }
  page.document
    .write(`<!doctype html><html lang="zh"><head><meta charset="utf-8"><title>库位标签</title>
  <style>@page{size:80mm 50mm;margin:0}*{box-sizing:border-box}body{margin:0;font-family:"Microsoft YaHei",sans-serif}
  .label{width:80mm;height:50mm;padding:4mm;page-break-after:always;display:grid;grid-template-columns:32mm 1fr;gap:4mm;align-items:center}
  img{width:30mm;height:30mm}.kind{font-size:10pt;color:#555}.code{font-size:19pt;font-weight:700;overflow-wrap:anywhere}.sub{font-size:9pt;color:#555;margin-top:3mm}</style>
  </head><body>${html}</body></html>`)
  page.document.close()
  page.focus()
  page.onload = () => page.print()
}

function openSlotPrintDialog() {
  slotPrintLocationIds.value = locations.value[0] ? [locations.value[0].id] : []
  slotPrintOpen.value = true
}

function selectAllPrintLocations() {
  slotPrintLocationIds.value = locations.value.map(location => location.id)
}

async function printPalletSlotLabels() {
  const slots = selectedPrintSlots.value
  if (!current.value || slots.length === 0) {
    message.warning('请至少选择一个基础库位')
    return
  }
  const page = window.open('', '_blank', 'width=700,height=800')
  if (!page) {
    message.warning('打印窗口被浏览器拦截，请允许弹出窗口后重试')
    return
  }
  slotPrinting.value = true
  try {
    const cards = await Promise.all(
      slots.map(async slot => ({
        slot,
        qr: await QRCode.toDataURL(slot.slotCode, { margin: 1, width: 220 })
      }))
    )
    const locationById = new Map(locations.value.map(location => [location.id, location]))
    const html = cards
      .map(({ slot, qr }) => {
        const location = locationById.get(slot.locationId)
        return `<section class="label">
    <img src="${qr}" alt="${slot.slotCode}">
    <div>
      <div class="kind">托位 · ${slot.locationCode}</div>
      <div class="code">${slot.slotCode}</div>
      <div class="sub">第 ${slot.levelNo} 层 · 第 ${slot.positionNo || '-'} 托位 · ${
        location ? zoneNameOf(location) : slot.zoneName || ''
      }</div>
    </div>
  </section>`
      })
      .join('')
    page.document.write(`<!doctype html><html lang="zh"><head><meta charset="utf-8"><title>托位标签</title>
  <style>@page{size:80mm 50mm;margin:0}*{box-sizing:border-box}body{margin:0;font-family:"Microsoft YaHei",sans-serif}
  .label{width:80mm;height:50mm;padding:4mm;page-break-after:always;display:grid;grid-template-columns:32mm 1fr;gap:4mm;align-items:center}
  img{width:30mm;height:30mm}.kind{font-size:9pt;color:#555}.code{font-size:15pt;font-weight:700;overflow-wrap:anywhere;line-height:1.25}.sub{font-size:9pt;color:#555;margin-top:3mm}</style>
  </head><body>${html}</body></html>`)
    page.document.close()
    page.focus()
    page.onload = () => page.print()
    slotPrintOpen.value = false
  } catch (error) {
    page.close()
    message.error('生成托位标签失败')
  } finally {
    slotPrinting.value = false
  }
}

/** 选中一个库位（有货占用的库位锁定，不可选） */
function addSel(id: number) {
  if (!lockedIds.value.has(id)) selectedIds.value.add(id)
}
function toggleSelect(id: number) {
  if (selectedIds.value.has(id)) selectedIds.value.delete(id)
  else addSel(id)
}
function clearSelection() {
  selectedIds.value.clear()
}

// 库位设置模式 + 拖拽框选
const settingMode = ref(false)
function toggleSetting() {
  settingMode.value = !settingMode.value
  if (!settingMode.value) clearSelection()
}

const dragging = ref(false)
const dragStart = ref<{ ri: number; c: number } | null>(null)
const dragEnd = ref<{ ri: number; c: number } | null>(null)
let dragMoved = false

function locAt(ri: number, c: number): WmsLocation | undefined {
  const rn = rackNos.value[ri]
  return rn ? cellMap.value[`${rn}|${c}`] : undefined
}
function onCellDown(ri: number, c: number, e: MouseEvent) {
  if (!settingMode.value) return
  e.preventDefault()
  dragging.value = true
  dragMoved = false
  dragStart.value = { ri, c }
  dragEnd.value = { ri, c }
  document.addEventListener('mouseup', onDocMouseUp, { once: true })
}
function onCellEnter(ri: number, c: number) {
  if (!dragging.value) return
  dragEnd.value = { ri, c }
  if (dragStart.value && (ri !== dragStart.value.ri || c !== dragStart.value.c)) dragMoved = true
}
function onDocMouseUp() {
  if (dragging.value && dragStart.value && dragEnd.value) {
    if (!dragMoved) {
      const loc = locAt(dragStart.value.ri, dragStart.value.c)
      if (loc) toggleSelect(loc.id)
    } else {
      const r1 = Math.min(dragStart.value.ri, dragEnd.value.ri)
      const r2 = Math.max(dragStart.value.ri, dragEnd.value.ri)
      const c1 = Math.min(dragStart.value.c, dragEnd.value.c)
      const c2 = Math.max(dragStart.value.c, dragEnd.value.c)
      for (let ri = r1; ri <= r2; ri++) {
        for (let c = c1; c <= c2; c++) {
          const loc = locAt(ri, c)
          if (loc) addSel(loc.id)
        }
      }
    }
  }
  dragging.value = false
  dragStart.value = null
  dragEnd.value = null
}
function inDragRect(ri: number, c: number): boolean {
  if (!dragging.value || !dragStart.value || !dragEnd.value) return false
  const r1 = Math.min(dragStart.value.ri, dragEnd.value.ri)
  const r2 = Math.max(dragStart.value.ri, dragEnd.value.ri)
  const c1 = Math.min(dragStart.value.c, dragEnd.value.c)
  const c2 = Math.max(dragStart.value.c, dragEnd.value.c)
  return ri >= r1 && ri <= r2 && c >= c1 && c <= c2
}
function selectRow(rn: string) {
  for (let c = 1; c <= maxColumn.value; c++) {
    const loc = cellMap.value[`${rn}|${c}`]
    if (loc) addSel(loc.id)
  }
}
function selectColumn(c: number) {
  for (const rn of rackNos.value) {
    const loc = cellMap.value[`${rn}|${c}`]
    if (loc) addSel(loc.id)
  }
}
function selectAll() {
  for (const l of locations.value) addSel(l.id)
}
async function assignZone(zoneId: number) {
  if (!current.value || selectedIds.value.size === 0) return
  assigning.value = true
  try {
    const res = await moveLocationZone({
      warehouseId: current.value.id,
      locationIds: Array.from(selectedIds.value),
      zoneId
    })
    if (isSuccess(res)) {
      message.success(`已设置 ${res.data} 个库位的分区`)
      clearSelection()
      await loadLocations()
      emits('success')
    } else {
      message.error(res.message || '设置失败')
    }
  } finally {
    assigning.value = false
  }
}

function fillForm(w?: WarehouseStructure) {
  form.rackRows = w?.rackRows ?? 0
  form.rackColumns = w?.rackColumns ?? 0
  form.rackNoPrefix = w?.rackNoPrefix ?? ''
  form.codePadWidth = w?.codePadWidth ?? 2
  form.palletLevels = w?.palletLevels ?? 6
  form.palletPositionsPerLevel = w?.palletPositionsPerLevel ?? 3
  form.maxSkuKindsPerPallet = w?.maxSkuKindsPerPallet ?? 4
  form.allowCrossOwnerMix = 0
  form.defaultPalletLengthMm = w?.defaultPalletLengthMm ?? 1200
  form.defaultPalletWidthMm = w?.defaultPalletWidthMm ?? 1000
  form.defaultPalletHeightMm = w?.defaultPalletHeightMm ?? 1600
  form.defaultPalletMaxWeightKg = w?.defaultPalletMaxWeightKg ?? 1000
  form.defaultPalletUtilization = w?.defaultPalletUtilization ?? 0.85
}

async function loadZones() {
  if (!current.value) return
  let res = await listZones(current.value.id)
  let list = isSuccess(res) ? res.data || [] : []
  // 固定四类分区若未初始化则静默创建，保证分区图例与库位设分区可用
  if (list.length === 0) {
    await initDefaultZones(current.value.id)
    res = await listZones(current.value.id)
    list = isSuccess(res) ? res.data || [] : []
  }
  zones.value = list
}

async function loadLocations() {
  if (!current.value) return
  loadingLocations.value = true
  try {
    const [res, slotRes] = await Promise.all([
      listLocations(current.value.id),
      listPalletSlots(current.value.id)
    ])
    if (isSuccess(res)) locations.value = res.data || []
    if (isSuccess(slotRes)) palletSlots.value = slotRes.data || []
  } finally {
    loadingLocations.value = false
  }
}

async function loadOccupied() {
  if (!current.value) return
  const res = await listOccupiedLocations(current.value.id)
  occupiedCodes.value = new Set(isSuccess(res) ? res.data || [] : [])
}

async function loadVirtual() {
  if (!current.value) return
  const res = await listVirtualLocations(current.value.id)
  virtualLocs.value = isSuccess(res) ? res.data || [] : []
}

async function addVirtual() {
  if (!current.value) return
  const name = (newVirtualName.value || '').trim()
  if (!name) {
    message.warning('请输入虚拟库位名称')
    return
  }
  addingVirtual.value = true
  try {
    const res = await createVirtualLocation(current.value.id, name)
    if (isSuccess(res)) {
      message.success('虚拟库位已新增')
      newVirtualName.value = ''
      await loadVirtual()
    } else {
      message.error(res.message || '新增失败')
    }
  } finally {
    addingVirtual.value = false
  }
}

async function removeVirtual(id: number) {
  const res = await deleteVirtualLocation(id)
  if (isSuccess(res)) {
    message.success('已删除')
    await loadVirtual()
  } else {
    message.error(res.message || '删除失败（库位上可能仍有货）')
  }
}

async function saveStructure() {
  if (!current.value) return
  savingStructure.value = true
  try {
    const res = await updateWarehouseStructure({ id: current.value.id, ...form })
    if (isSuccess(res)) {
      message.success('结构已保存')
      // 同步本地副本，供预计数量/重新生成判断
      current.value = { ...current.value, ...form }
      emits('success')
    } else {
      message.error(res.message || '保存失败')
    }
  } finally {
    savingStructure.value = false
  }
}

async function doGenerate() {
  if (!current.value) return
  generating.value = true
  try {
    const res = await generateLocations(current.value.id)
    if (isSuccess(res)) {
      message.success(`已生成 ${res.data} 个库位`)
      current.value = { ...current.value, locationGenerated: 1 }
      await loadLocations()
      emits('success')
    } else {
      message.error(res.message || '生成失败')
    }
  } finally {
    generating.value = false
  }
}

function openDrawer(warehouse: WarehouseStructure) {
  current.value = { ...warehouse }
  fillForm(warehouse)
  zones.value = []
  locations.value = []
  selectedIds.value = new Set()
  settingMode.value = false
  viewMode.value = 'grid'
  open.value = true
  occupiedCodes.value = new Set()
  virtualLocs.value = []
  newVirtualName.value = ''
  loadZones()
  loadLocations()
  loadOccupied()
  loadVirtual()
}

defineExpose({ open: openDrawer })
</script>

<script lang="ts">
export default {
  name: 'WarehouseLocationDrawer'
}
</script>

<style scoped>
.drawer-section {
  margin-bottom: 24px;
}
.virtual-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 12px;
  line-height: 1.6;
}
.virtual-add {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.section-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border-left: 3px solid #1677ff;
  padding-left: 8px;
}

.structure-form :deep(.ant-form-item) {
  margin-bottom: 12px;
}

.section-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 4px;
}

.section-actions .hint {
  color: #8c8c8c;
  font-size: 13px;
}

.section-actions .hint b {
  color: #1677ff;
}

.empty-hint {
  color: #999;
}

.grid-wrap {
  overflow: auto;
  max-height: 420px;
}

.rack-grid {
  border-collapse: separate;
  border-spacing: 4px;
}

.rack-grid td {
  text-align: center;
}

.rack-grid .cell {
  margin: 0 auto;
}

.rack-grid th {
  font-size: 12px;
  color: #8c8c8c;
  font-weight: 500;
  text-align: center;
  min-width: 26px;
}

.rack-grid th.rack-head {
  color: #595959;
  font-weight: 600;
  text-align: right;
  padding-right: 4px;
}

.rack-grid th.corner {
  min-width: 32px;
}

.cell {
  width: 108px;
  height: 78px;
  border-radius: 4px;
  box-sizing: border-box;
}

.cell.filled {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: 18px repeat(3, 16px);
  column-gap: 5px;
  padding: 3px 6px;
  text-align: left;
  font-size: 10px;
  overflow: hidden;
}

.cell.filled strong {
  font-size: 11px;
  grid-column: 1 / -1;
}
.cell.filled span {
  color: rgba(0, 0, 0, 0.55);
  line-height: 16px;
}
.cell.filled span.occupied {
  color: #135200;
  font-weight: 600;
}

.cell.filled {
  border: 1px solid rgba(0, 0, 0, 0.12);
  cursor: pointer;
}

.cell.filled:hover {
  filter: brightness(0.94);
}

.cell.filled.selected {
  box-shadow: 0 0 0 2px #1677ff;
  border-color: #1677ff;
}

.cell.filled.preview {
  outline: 2px dashed #1677ff;
  outline-offset: -2px;
}

.cell.filled.locked {
  cursor: not-allowed;
  opacity: 0.5;
  box-shadow: inset 0 0 0 2px rgba(0, 0, 0, 0.28);
}

.cell.filled.locked:hover {
  filter: none;
}

.rack-grid.selectable {
  user-select: none;
}

.rack-grid.selectable th.clickable {
  cursor: pointer;
}

.rack-grid.selectable th.clickable:hover {
  color: #1677ff;
}

.sel-tip {
  color: #8c8c8c;
  font-size: 12px;
  margin-bottom: 8px;
}

.cell.empty {
  background: #fafafa;
  border: 1px dashed #e0e0e0;
}

.struct-btns {
  padding-top: 2px;
}

.assign-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.assign-bar .sel-hint {
  color: #8c8c8c;
  font-size: 13px;
}

.assign-bar .sel-hint b {
  color: #1677ff;
}
</style>
