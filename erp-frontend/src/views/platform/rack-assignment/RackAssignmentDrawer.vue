<template>
  <a-drawer
    v-model:open="open"
    :title="drawerTitle"
    :width="760"
    destroy-on-close
    placement="right"
    :body-style="{ scrollbarGutter: 'stable' }"
  >
    <template #extra>
      <a-button type="primary" :disabled="racks.length === 0" @click="openAssign"
        >分配货架</a-button
      >
    </template>

    <template v-if="current">
      <!-- 图例 -->
      <div class="legend-bar">
        <a-space wrap>
          <span><span class="legend" style="background: #f0f0f0" />空闲</span>
          <span><span class="legend" style="border: 2px solid #faad14" />临期(≤30天)</span>
          <span class="hint">有色块=已分配给对应服务商，点击排查看库位</span>
        </a-space>
      </div>

      <!-- 二维预览：按排色块 -->
      <a-spin :spinning="loadingPreview">
        <div class="rack-grid">
          <div
            v-for="r in racks"
            :key="r.rackNo"
            class="rack-block"
            :class="{ active: currentRack?.rackNo === r.rackNo }"
            :style="blockStyle(r)"
            @click="selectRack(r)"
          >
            <div class="rack-no">{{ r.rackNo }}</div>
            <div class="rack-owner">
              {{ r.status === 'OCCUPIED' ? r.assignedWmsTenantName : '空闲' }}
            </div>
            <div class="rack-count">{{ r.locationCount }} 库位</div>
          </div>
          <a-empty
            v-if="racks.length === 0"
            :style="{ gridColumn: '1 / -1' }"
            description="该仓库暂无库位，请先在『库位管理』生成库位"
          />
        </div>
      </a-spin>

      <!-- 选中排详情 + 库位 -->
      <template v-if="currentRack">
        <a-divider style="margin: 16px 0 12px" />
        <div class="section-title">排 {{ currentRack.rackNo }} 明细</div>
        <div v-if="currentRack.status === 'OCCUPIED'" style="margin-bottom: 12px">
          <a-descriptions size="small" :column="2" bordered>
            <a-descriptions-item label="归属服务商">
              {{ currentRack.assignedWmsTenantName }}
            </a-descriptions-item>
            <a-descriptions-item label="月租金">{{ currentRack.monthlyFee }}</a-descriptions-item>
            <a-descriptions-item label="有效期" :span="2">
              {{ currentRack.effectiveFrom }} ~ {{ currentRack.effectiveTo || '长期' }}
              <a-tag v-if="currentRack.expiringSoon" color="orange" style="margin-left: 8px"
                >临期</a-tag
              >
            </a-descriptions-item>
          </a-descriptions>
          <a-popconfirm title="确定解除该排分配？" @confirm="doUnassign">
            <a-button danger size="small" style="margin-top: 8px">解除分配</a-button>
          </a-popconfirm>
        </div>
        <a-table
          :columns="locationColumns"
          :data-source="drawerLocations"
          :loading="loadingLocations"
          row-key="id"
          size="small"
          :pagination="{
            defaultPageSize: 5,
            showSizeChanger: true,
            pageSizeOptions: ['5', '10', '20', '50'],
            showTotal: (t: number) => `共 ${t} 个`
          }"
        />
      </template>
    </template>

    <!-- 分配弹窗 -->
    <a-modal
      v-model:open="assignOpen"
      title="分配货架"
      :confirm-loading="submitting"
      @ok="submitAssign"
      @cancel="assignOpen = false"
    >
      <a-form :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="WMS服务商" required>
          <a-select
            v-model:value="form.wmsTenantId"
            placeholder="选择被分配的服务商"
            :options="operatorOptions"
          />
        </a-form-item>
        <a-form-item label="排号(可多选)" required>
          <a-select
            v-model:value="form.rackNos"
            mode="multiple"
            placeholder="仅可选空闲排"
            :options="idleRackOptions"
          />
        </a-form-item>
        <a-form-item label="月租金(元/排)" required>
          <a-input-number v-model:value="form.monthlyFee" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="生效日期" required>
          <a-date-picker
            v-model:value="form.effectiveFrom"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="结束日期">
          <a-date-picker
            v-model:value="form.effectiveTo"
            value-format="YYYY-MM-DD"
            placeholder="留空=长期有效"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import {
  previewRacks,
  assignRacks,
  unassignRack,
  rackLocations,
  listRackOperators
} from '@/api/wms/rack'
import type { RackVO, WmsOperatorOption, RackLocation } from '@/api/wms/rack/types'
import { listZones } from '@/api/wms/location-mgmt'
import type { WmsZone } from '@/api/wms/location-mgmt/types'

const emits = defineEmits<{ (e: 'success'): void }>()

const PALETTE = [
  '#52c41a',
  '#1677ff',
  '#722ed1',
  '#fa8c16',
  '#13c2c2',
  '#eb2f96',
  '#a0d911',
  '#2f54eb'
]

const locationColumns = [
  { title: '库位编码', dataIndex: 'locationCode', key: 'locationCode' },
  { title: '列', dataIndex: 'columnNo', key: 'columnNo' },
  {
    title: '分区',
    key: 'zone',
    customRender: ({ record }: { record: RackLocation }) => zoneNameOf(record)
  }
]

interface WarehouseBrief {
  id: number
  warehouseName: string
  warehouseCode: string
}

const open = ref(false)
const current = ref<WarehouseBrief | undefined>(undefined)
const racks = ref<RackVO[]>([])
const operators = ref<WmsOperatorOption[]>([])
const currentRack = ref<RackVO | undefined>(undefined)
const drawerLocations = ref<RackLocation[]>([])
const zones = ref<WmsZone[]>([])

const zoneById = computed<Record<number, WmsZone>>(() => {
  const m: Record<number, WmsZone> = {}
  for (const z of zones.value) m[z.id] = z
  return m
})
function zoneNameOf(loc: RackLocation): string {
  return (loc.zoneId != null && zoneById.value[loc.zoneId]?.zoneName) || '-'
}

const loadingPreview = ref(false)
const loadingLocations = ref(false)
const assignOpen = ref(false)
const submitting = ref(false)

const form = reactive<{
  wmsTenantId?: number
  rackNos: string[]
  monthlyFee?: number
  effectiveFrom?: string
  effectiveTo?: string
  remark?: string
}>({ rackNos: [] })

const drawerTitle = computed(() =>
  current.value
    ? `货架分配 · ${current.value.warehouseName}（${current.value.warehouseCode}）`
    : '货架分配'
)
const operatorOptions = computed(() => operators.value.map(o => ({ label: o.name, value: o.id })))
const idleRackOptions = computed(() =>
  racks.value.filter(r => r.status === 'IDLE').map(r => ({ label: r.rackNo, value: r.rackNo }))
)

// 服务商 → 固定颜色
const colorMap = computed(() => {
  const map: Record<number, string> = {}
  let i = 0
  racks.value.forEach(r => {
    if (r.assignedWmsTenantId != null && map[r.assignedWmsTenantId] === undefined) {
      map[r.assignedWmsTenantId] = PALETTE[i % PALETTE.length]!
      i++
    }
  })
  return map
})

function blockStyle(r: RackVO) {
  const occupied = r.status === 'OCCUPIED' && r.assignedWmsTenantId != null
  const bg = occupied ? colorMap.value[r.assignedWmsTenantId as number] : '#f0f0f0'
  return {
    background: bg,
    color: occupied ? '#fff' : 'rgba(0,0,0,0.55)',
    border: r.expiringSoon ? '2px solid #faad14' : '1px solid #d9d9d9'
  }
}

async function loadPreview() {
  if (!current.value) return
  loadingPreview.value = true
  try {
    const res = await previewRacks(current.value.id)
    if (isSuccess(res)) racks.value = res.data || []
  } finally {
    loadingPreview.value = false
  }
}

async function loadOperators() {
  const res = await listRackOperators()
  if (isSuccess(res)) operators.value = res.data || []
}

async function loadZones() {
  if (!current.value) return
  await initDefaultZones(current.value.id)
  const res = await listZones(current.value.id)
  if (isSuccess(res)) zones.value = res.data || []
}

function openAssign() {
  form.wmsTenantId = undefined
  form.rackNos = []
  form.monthlyFee = undefined
  form.effectiveFrom = undefined
  form.effectiveTo = undefined
  form.remark = undefined
  assignOpen.value = true
}

async function submitAssign() {
  if (
    !form.wmsTenantId ||
    form.rackNos.length === 0 ||
    form.monthlyFee == null ||
    !form.effectiveFrom
  ) {
    message.warning('服务商、排号、月租金、生效日期为必填')
    return
  }
  submitting.value = true
  try {
    const res = await assignRacks({
      wmsTenantId: form.wmsTenantId,
      warehouseId: current.value!.id,
      rackNos: form.rackNos,
      monthlyFee: form.monthlyFee,
      effectiveFrom: form.effectiveFrom,
      effectiveTo: form.effectiveTo,
      remark: form.remark
    })
    if (isSuccess(res)) {
      message.success(`已分配 ${res.data} 个排`)
      assignOpen.value = false
      await loadPreview()
      emits('success')
    } else {
      message.error(res.message || '分配失败')
    }
  } finally {
    submitting.value = false
  }
}

async function selectRack(r: RackVO) {
  currentRack.value = r
  loadingLocations.value = true
  try {
    const res = await rackLocations(current.value!.id, r.rackNo)
    if (isSuccess(res)) drawerLocations.value = res.data || []
  } finally {
    loadingLocations.value = false
  }
}

async function doUnassign() {
  if (!currentRack.value?.assignmentId) return
  const res = await unassignRack(currentRack.value.assignmentId)
  if (isSuccess(res)) {
    message.success('已解除分配')
    currentRack.value = undefined
    drawerLocations.value = []
    await loadPreview()
    emits('success')
  } else {
    message.error(res.message || '解除失败')
  }
}

function openDrawer(warehouse: WarehouseBrief) {
  current.value = { ...warehouse }
  racks.value = []
  operators.value = []
  zones.value = []
  currentRack.value = undefined
  drawerLocations.value = []
  open.value = true
  loadPreview()
  loadOperators()
  loadZones()
}

defineExpose({ open: openDrawer })
</script>

<script lang="ts">
export default {
  name: 'RackAssignmentDrawer'
}
</script>

<style scoped>
.legend-bar {
  margin-bottom: 12px;
  color: #666;
}

.legend-bar .hint {
  color: #999;
}

.legend {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 3px;
  margin-right: 4px;
  vertical-align: -2px;
}

.section-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 12px;
  border-left: 3px solid #1677ff;
  padding-left: 8px;
}

.rack-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(104px, 1fr));
  gap: 10px;
  min-height: 40px;
}

.rack-block {
  height: 72px;
  border-radius: 6px;
  padding: 6px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 2px;
  transition: transform 0.08s;
}

.rack-block:hover {
  transform: translateY(-2px);
}

.rack-block.active {
  box-shadow: 0 0 0 2px #1677ff;
}

.rack-no {
  font-weight: 600;
  font-size: 15px;
}

.rack-owner {
  font-size: 12px;
}

.rack-count {
  font-size: 11px;
  opacity: 0.85;
}
</style>
