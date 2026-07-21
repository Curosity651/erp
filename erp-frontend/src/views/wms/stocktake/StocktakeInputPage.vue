<template>
  <page-container :page-header-render="false" ghost :loading="loading">
    <div class="page-head">
      <div class="head-main">
        <a-button type="text" @click="goBack"><arrow-left-outlined /></a-button>
        <div>
          <div class="title-row">
            <strong>库位盘点工作台</strong>
            <a-tag color="processing">盘点中</a-tag>
          </div>
          <span>{{ detail?.stocktakeNo }} · {{ detail?.warehouseName }}</span>
        </div>
      </div>
      <div class="head-actions">
        <span>库位进度 {{ completedCount }}/{{ tasks.length }}</span>
        <a-progress :percent="taskProgress" :show-info="false" style="width: 150px" />
        <a-button :disabled="!allCompleted" type="primary" @click="handleSubmitReview">
          提交复核
        </a-button>
      </div>
    </div>

    <div class="workbench">
      <aside class="task-panel">
        <div class="task-filter">
          <a-input v-model:value="taskKeyword" allow-clear placeholder="搜索库位">
            <template #prefix><search-outlined /></template>
          </a-input>
        </div>
        <div class="task-list">
          <button
            v-for="task in filteredTasks"
            :key="task.id"
            type="button"
            :class="['task-row', { active: currentTask?.id === task.id }]"
            @click="selectTask(task)"
          >
            <span :class="['task-state', task.taskStatus.toLowerCase()]">
              <check-outlined v-if="task.taskStatus === 'COMPLETED'" />
            </span>
            <span class="task-info">
              <strong>{{ task.locationCode }}</strong>
              <small>{{ task.countedCount || 0 }}/{{ task.itemCount || 0 }} 项</small>
            </span>
            <a-tag v-if="task.diffCount" color="error">差异 {{ task.diffCount }}</a-tag>
          </button>
        </div>
      </aside>

      <main class="count-panel">
        <div v-if="currentTask" class="location-head">
          <div>
            <span class="eyebrow">当前库位</span>
            <h2>{{ currentTask.locationCode }}</h2>
            <a-segmented v-model:value="selectedLevel" :options="levelOptions" size="small" />
          </div>
          <div class="location-actions">
            <a-input
              v-model:value="itemKeyword"
              allow-clear
              placeholder="扫描或搜索 SKU"
              style="width: 220px"
            >
              <template #prefix><scan-outlined /></template>
            </a-input>
            <a-button
              :disabled="currentTask.taskStatus === 'COMPLETED'"
              @click="extraVisible = true"
            >
              <plus-outlined />账外货物
            </a-button>
            <a-button
              type="primary"
              :loading="completing"
              :disabled="currentTask.taskStatus === 'COMPLETED'"
              @click="handleCompleteTask"
            >
              完成此库位
            </a-button>
          </div>
        </div>

        <a-alert
          v-if="detail?.blindCount === 1"
          type="info"
          show-icon
          message="当前为盲盘模式，盘点期间不显示账面数量和差异。"
        />

        <a-table
          :data-source="filteredItems"
          :columns="columns"
          :pagination="false"
          :loading="itemsLoading"
          row-key="id"
          size="middle"
          :scroll="{ x: 880 }"
        >
          <template #emptyText>
            <a-empty description="该库位账面为空，请核对现场后直接完成，或登记账外货物" />
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'sku'">
              <div class="sku-cell">
                <strong>{{ record.skuCode }}</strong>
                <small>{{ record.skuBrief?.skuName || record.skuBrief?.skuChineseName || '-' }}</small>
              </div>
            </template>
            <template v-else-if="column.key === 'owner'">
              <a-tag>{{ record.ownerName || record.erpTenantId }}</a-tag>
            </template>
            <template v-else-if="column.key === 'batch'">
              <span>{{ record.inboundDate || '-' }}</span>
              <small class="muted">{{ record.slotCode || `${record.locationCode}-L1` }} · {{ qualityText(record.quality) }}</small>
            </template>
            <template v-else-if="column.key === 'systemQuantity'">
              <span v-if="detail?.blindCount !== 1">{{ record.systemQuantity }}</span>
              <span v-else class="muted">隐藏</span>
            </template>
            <template v-else-if="column.key === 'actualQuantity'">
              <a-input-number
                v-model:value="record.actualQuantity"
                :min="0"
                :precision="0"
                :disabled="currentTask?.taskStatus === 'COMPLETED'"
                placeholder="实盘数"
                style="width: 110px"
                @change="markDirty(record)"
              />
            </template>
            <template v-else-if="column.key === 'diff'">
              <template v-if="detail?.blindCount !== 1 && record.actualQuantity != null">
                <span :class="diffClass(record.actualQuantity - record.systemQuantity)">
                  {{ signed(record.actualQuantity - record.systemQuantity) }}
                </span>
              </template>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="record.actualQuantity != null ? 'success' : 'default'">
                {{ record.actualQuantity != null ? '已录入' : '待盘' }}
              </a-tag>
              <a-tag v-if="record.sourceType === 'ADDED'" color="orange">账外</a-tag>
            </template>
          </template>
        </a-table>

        <div class="save-bar">
          <span v-if="dirty" class="unsaved">存在未保存的盘点数量</span>
          <span v-else>数据已保存</span>
          <a-button :loading="saving" :disabled="!dirty" @click="saveCurrentItems">
            <save-outlined />暂存
          </a-button>
        </div>
      </main>
    </div>

    <a-modal
      v-model:open="extraVisible"
      title="登记账外货物"
      ok-text="登记"
      :confirm-loading="addingExtra"
      @ok="handleAddExtra"
    >
      <a-form layout="vertical">
        <a-form-item label="货主" required>
          <PlatformOwnerSelect
            v-model:value="extra.erpTenantId"
            :allowed-ids="eligibleOwnerIds"
            width="100%"
            :placeholder="eligibleOwnerIds.length ? '请选择货主' : '当前库位没有可用货主'"
            :disabled="eligibleOwnersLoading || eligibleOwnerIds.length === 0"
          />
          <div v-if="!eligibleOwnersLoading && eligibleOwnerIds.length === 0" class="owner-warning">
            当前货架未分配给服务商，或承租服务商名下没有启用货主，不能登记账外库存。
          </div>
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="SKU" required>
              <a-input v-model:value="extra.skuCode" placeholder="输入或扫描 SKU" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="实盘数量" required>
              <a-input-number v-model:value="extra.actualQuantity" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="托盘层位" required>
              <a-select
                v-model:value="extra.slotCode"
                :options="extraSlotOptions"
                placeholder="选择 L1/L2/L3"
                @change="onExtraSlotChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="品质">
              <a-select v-model:value="extra.quality">
                <a-select-option value="GOOD">良品</a-select-option>
                <a-select-option value="DAMAGED">不良品</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="入库日期">
              <a-date-picker v-model:value="extra.inboundDate" value-format="YYYY-MM-DD" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </page-container>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  CheckOutlined,
  PlusOutlined,
  SaveOutlined,
  ScanOutlined,
  SearchOutlined
} from '@ant-design/icons-vue'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import {
  addStocktakeExtraItem,
  completeStocktakeTask,
  getStocktakeDetail,
  getStocktakeEligibleOwnerIds,
  getStocktakeTaskItems,
  getStocktakeTasks,
  saveStocktakeItems,
  submitStocktakeReview
} from '@/api/wms/stocktake'
import type {
  StocktakeDetailVO,
  StocktakeExtraItemDTO,
  StocktakeItemVO,
  StocktakeLocationTaskVO
} from '@/api/wms/stocktake/types'
import { isSuccess } from '@/api'
import { listPalletSlots } from '@/api/wms/pallet'
import type { PalletSlotVO } from '@/api/wms/inbound-execution'

defineOptions({ name: 'StocktakeInput' })
const route = useRoute()
const router = useRouter()
const stocktakeId = Number(route.params.id)
const detail = ref<StocktakeDetailVO>()
const tasks = ref<StocktakeLocationTaskVO[]>([])
const currentTask = ref<StocktakeLocationTaskVO>()
const items = ref<StocktakeItemVO[]>([])
const loading = ref(false)
const itemsLoading = ref(false)
const saving = ref(false)
const completing = ref(false)
const dirty = ref(false)
const taskKeyword = ref('')
const itemKeyword = ref('')
const extraVisible = ref(false)
const addingExtra = ref(false)
const eligibleOwnersLoading = ref(false)
const eligibleOwnerIds = ref<number[]>([])
const currentSlots = ref<PalletSlotVO[]>([])
const selectedLevel = ref(1)
const extra = reactive<Partial<StocktakeExtraItemDTO>>({ quality: 'GOOD', actualQuantity: 1 })

const completedCount = computed(() => tasks.value.filter(t => t.taskStatus === 'COMPLETED').length)
const allCompleted = computed(() => tasks.value.length > 0 && completedCount.value === tasks.value.length)
const taskProgress = computed(() => tasks.value.length ? Math.round(completedCount.value * 100 / tasks.value.length) : 0)
const filteredTasks = computed(() => tasks.value.filter(t => t.locationCode.toLowerCase().includes(taskKeyword.value.toLowerCase())))
const filteredItems = computed(() => {
  const keyword = itemKeyword.value.trim().toLowerCase()
  const levelItems = items.value.filter(item => slotLevel(item.slotCode) === selectedLevel.value)
  if (!keyword) return levelItems
  return levelItems.filter(item =>
    item.skuCode.toLowerCase().includes(keyword) ||
    (item.ownerName || '').toLowerCase().includes(keyword)
  )
})
const levelOptions = computed(() => [1, 2, 3].map(level => ({
  label: `L${level} (${items.value.filter(item => slotLevel(item.slotCode) === level).length})`,
  value: level
})))
const extraSlotOptions = computed(() => currentSlots.value.map(slot => ({
  value: slot.slotCode,
  label: `${slot.slotCode} · ${slot.palletId ? `${slot.palletNo || '已有托盘'} ${Math.round(slot.capacityPercent || 0)}%` : '空层位'}`
})))

const columns = [
  { title: 'SKU', key: 'sku', width: 210 },
  { title: '货主', key: 'owner', width: 120 },
  { title: '批次/品质', key: 'batch', width: 130 },
  { title: '账面数', key: 'systemQuantity', align: 'right', width: 90 },
  { title: '实盘数', key: 'actualQuantity', width: 130 },
  { title: '差异', key: 'diff', align: 'right', width: 80 },
  { title: '状态', key: 'status', width: 130 }
]

function qualityText(value?: string) { return value === 'DAMAGED' ? '不良品' : '良品' }
function signed(value: number) { return value > 0 ? `+${value}` : String(value) }
function diffClass(value: number) { return value > 0 ? 'diff-profit' : value < 0 ? 'diff-loss' : '' }
function markDirty(record: StocktakeItemVO) {
  record.stocktakeStatus = record.actualQuantity == null ? 'PENDING' : 'COUNTED'
  dirty.value = true
}

async function loadTasks(selectId?: number) {
  const result = await getStocktakeTasks(stocktakeId)
  if (!isSuccess(result)) return
  tasks.value = result.data || []
  const target = tasks.value.find(t => t.id === selectId)
    || tasks.value.find(t => t.taskStatus !== 'COMPLETED')
    || tasks.value[0]
  if (target) await selectTask(target, true)
}

async function selectTask(task: StocktakeLocationTaskVO, force = false) {
  if (!force && currentTask.value?.id === task.id) return
  if (dirty.value) {
    const saved = await saveCurrentItems()
    if (!saved) return
  }
  currentTask.value = task
  itemsLoading.value = true
  eligibleOwnersLoading.value = true
  eligibleOwnerIds.value = []
  extra.erpTenantId = undefined
  try {
    const [itemResult, ownerResult, slotResult] = await Promise.all([
      getStocktakeTaskItems(task.id),
      getStocktakeEligibleOwnerIds(task.id),
      listPalletSlots(task.warehouseId)
    ])
    if (isSuccess(itemResult)) items.value = itemResult.data || []
    if (isSuccess(ownerResult)) eligibleOwnerIds.value = ownerResult.data || []
    currentSlots.value = isSuccess(slotResult)
      ? (slotResult.data || []).filter(slot => slot.locationId === task.locationId)
      : []
    selectedLevel.value = 1
    const defaultSlot = currentSlots.value.find(slot => slot.levelNo === selectedLevel.value)
    extra.slotCode = defaultSlot?.slotCode
    extra.palletId = defaultSlot?.palletId
    dirty.value = false
  } finally {
    itemsLoading.value = false
    eligibleOwnersLoading.value = false
  }
}

async function saveCurrentItems() {
  if (!currentTask.value) return false
  saving.value = true
  try {
    const result = await saveStocktakeItems({
      stocktakeId,
      items: items.value.map(item => ({ itemId: item.id, actualQuantity: item.actualQuantity ?? null }))
    })
    if (isSuccess(result)) {
      dirty.value = false
      message.success('已暂存')
      return true
    }
    return false
  } finally {
    saving.value = false
  }
}

async function handleCompleteTask() {
  if (!currentTask.value) return
  if (items.value.some(item => item.actualQuantity == null)) {
    message.warning('当前库位仍有商品未录入实盘数量')
    return
  }
  completing.value = true
  try {
    if (dirty.value && !(await saveCurrentItems())) return
    const result = await completeStocktakeTask(currentTask.value.id)
    if (isSuccess(result)) {
      message.success('当前库位已完成')
      await loadTasks()
    }
  } finally {
    completing.value = false
  }
}

async function handleAddExtra() {
  if (!currentTask.value || !extra.erpTenantId || !extra.skuCode || !extra.actualQuantity || !extra.slotCode) {
    message.warning('请填写货主、SKU、实盘数量和托盘层位')
    return
  }
  addingExtra.value = true
  try {
    const result = await addStocktakeExtraItem({
      taskId: currentTask.value.id,
      erpTenantId: extra.erpTenantId,
      skuCode: extra.skuCode.trim(),
      actualQuantity: extra.actualQuantity,
      slotCode: extra.slotCode,
      palletId: extra.palletId,
      quality: extra.quality,
      inboundDate: extra.inboundDate
    })
    if (isSuccess(result) && result.data) {
      items.value.push(result.data)
      extraVisible.value = false
      Object.assign(extra, { erpTenantId: undefined, skuCode: '', actualQuantity: 1, quality: 'GOOD', inboundDate: undefined, slotCode: undefined, palletId: undefined })
      message.success('账外货物已登记到当前库位')
    }
  } finally {
    addingExtra.value = false
  }
}

function slotLevel(slotCode?: string) {
  const match = slotCode?.match(/-L([123])$/)
  return match ? Number(match[1]) : 1
}

function onExtraSlotChange(slotCode: string) {
  const slot = currentSlots.value.find(item => item.slotCode === slotCode)
  extra.palletId = slot?.palletId
  if (slot) selectedLevel.value = slot.levelNo
}

function handleSubmitReview() {
  Modal.confirm({
    title: '提交差异复核',
    content: '提交后盘点数量将不可继续修改，确认提交吗？',
    onOk: async () => {
      const result = await submitStocktakeReview(stocktakeId)
      if (isSuccess(result)) {
        message.success('已提交复核')
        router.push(`/wms/stocktake/review/${stocktakeId}`)
      }
    }
  })
}

function goBack() { router.push('/ops/stocktake') }

onMounted(async () => {
  loading.value = true
  try {
    const result = await getStocktakeDetail(stocktakeId)
    if (isSuccess(result) && result.data) {
      detail.value = result.data
      if (detail.value.orderStatus !== 'COUNTING') {
        message.warning('该盘点单当前不能继续录入')
        goBack()
        return
      }
      await loadTasks()
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="less">
.page-head { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 12px; padding: 14px 18px; border: 1px solid #f0f0f0; background: #fff; }
.head-main, .head-actions, .title-row, .location-actions { display: flex; align-items: center; gap: 12px; }
.head-main span, .head-actions { color: #8c8c8c; font-size: 13px; }
.title-row strong { color: #262626; font-size: 17px; }
.workbench { display: grid; grid-template-columns: 250px minmax(0, 1fr); min-height: 620px; border: 1px solid #f0f0f0; background: #fff; }
.task-panel { border-right: 1px solid #f0f0f0; }
.task-filter { padding: 12px; border-bottom: 1px solid #f0f0f0; }
.task-list { max-height: 660px; overflow: auto; }
.task-row { display: flex; width: 100%; align-items: center; gap: 10px; min-height: 58px; padding: 10px 12px; border: 0; border-bottom: 1px solid #f5f5f5; background: #fff; text-align: left; cursor: pointer; }
.task-row:hover { background: #fafafa; }
.task-row.active { background: #e6f4ff; box-shadow: inset 3px 0 #1677ff; }
.task-state { width: 18px; height: 18px; flex: 0 0 auto; border: 2px solid #bfbfbf; border-radius: 50%; color: #fff; font-size: 10px; text-align: center; line-height: 14px; }
.task-state.completed { border-color: #52c41a; background: #52c41a; }
.task-info { display: flex; min-width: 0; flex: 1; flex-direction: column; }
.task-info strong { color: #262626; }
.task-info small, .muted { color: #8c8c8c; }
.count-panel { min-width: 0; padding: 18px; }
.location-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 14px; }
.location-head h2 { margin: 2px 0 0; font-size: 22px; letter-spacing: 0; }
.eyebrow { color: #8c8c8c; font-size: 12px; }
.count-panel :deep(.ant-alert) { margin-bottom: 12px; }
.sku-cell, td .batch-cell { display: flex; flex-direction: column; }
.sku-cell small { color: #8c8c8c; }
.diff-profit { color: #389e0d; font-weight: 600; }
.diff-loss { color: #cf1322; font-weight: 600; }
.save-bar { display: flex; align-items: center; justify-content: flex-end; gap: 16px; padding-top: 14px; color: #8c8c8c; }
.unsaved { color: #d46b08; }
.owner-warning { margin-top: 6px; color: #d46b08; font-size: 12px; line-height: 1.5; }
@media (max-width: 900px) {
  .page-head, .location-head { align-items: flex-start; flex-direction: column; }
  .workbench { grid-template-columns: 190px minmax(0, 1fr); }
  .location-actions { flex-wrap: wrap; }
}
</style>
