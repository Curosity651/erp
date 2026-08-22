<template>
  <a-modal
    title="新建盘点任务"
    :open="visible"
    :width="860"
    :confirm-loading="submitting"
    :mask-closable="false"
    ok-text="创建任务"
    @ok="handleSubmit"
    @cancel="handleClose"
  >
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-form-item label="盘点方式" name="stocktakeMode" required>
        <div class="mode-grid">
          <button
            v-for="mode in modes"
            :key="mode.value"
            type="button"
            :class="['mode-option', { active: form.stocktakeMode === mode.value }]"
            @click="selectMode(mode.value)"
          >
            <component :is="mode.icon" class="mode-icon" />
            <span class="mode-copy">
              <strong>{{ mode.label }}</strong>
              <small>{{ mode.description }}</small>
            </span>
            <check-circle-filled v-if="form.stocktakeMode === mode.value" class="checked" />
          </button>
        </div>
      </a-form-item>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            label="盘点仓库"
            name="warehouseId"
            :rules="[{ required: true, message: '请选择盘点仓库' }]"
          >
            <WarehouseSelect
              v-model:value="form.warehouseId"
              width="100%"
              placeholder="请选择盘点仓库"
              @change="handleWarehouseChange"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="盘点日期"
            name="stocktakeDate"
            :rules="[{ required: true, message: '请选择盘点日期' }]"
          >
            <a-date-picker
              v-model:value="form.stocktakeDate"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-alert v-if="form.stocktakeMode === 'FULL'" type="info" show-icon>
        <template #message>全仓盘点将为仓库内每个物理库位生成任务，包括当前账面为空的库位。</template>
      </a-alert>

      <a-form-item
        v-if="form.stocktakeMode === 'CYCLE'"
        label="盘点库位"
        name="locationIds"
        :rules="[{ required: true, type: 'array', min: 1, message: '请至少选择一个库位' }]"
      >
        <a-select
          v-model:value="form.locationIds"
          mode="multiple"
          show-search
          allow-clear
          :loading="locationsLoading"
          placeholder="选择本次循环盘点的库位"
          :filter-option="filterLocation"
          :options="locationOptions"
        />
        <div class="field-hint">已选择 {{ form.locationIds?.length || 0 }} 个库位</div>
      </a-form-item>

      <template v-if="form.stocktakeMode === 'SPECIAL'">
        <a-form-item label="专项对象">
          <a-radio-group v-model:value="form.virtualLocationOnly" @change="handleSpecialTypeChange">
            <a-radio :value="false">物理库存</a-radio>
            <a-radio :value="true">虚拟库位专项</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item
          v-if="form.virtualLocationOnly"
          label="虚拟库位"
          required
        >
          <a-select
            v-model:value="form.locationIds"
            mode="multiple"
            show-search
            allow-clear
            :loading="locationsLoading"
            placeholder="请选择需要盘点的虚拟库位"
            :filter-option="filterLocation"
            :options="virtualLocationOptions"
          />
          <div class="field-hint">已选择 {{ form.locationIds?.length || 0 }} 个虚拟库位</div>
        </a-form-item>
        <template v-else>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="指定货主">
              <PlatformOwnerSelect
                v-model:value="form.specialOwnerId"
                width="100%"
                placeholder="可选，不选则包含全部货主"
                @change="handleSpecialOwnerChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="指定 SKU">
              <a-select
                v-model:value="form.specialSkuCodes"
                mode="multiple"
                show-search
                allow-clear
                :loading="skuLoading"
                :options="skuOptions"
                :filter-option="filterSku"
                :placeholder="form.warehouseId ? '请选择 SKU，可输入编码或名称搜索' : '请先选择仓库'"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="寻货范围">
          <a-radio-group v-model:value="form.specialSearchAll">
            <a-radio :value="false">只盘账面所在库位</a-radio>
            <a-radio :value="true">全仓寻货（包含空库位）</a-radio>
          </a-radio-group>
        </a-form-item>
        </template>
      </template>

      <a-divider />
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="盘点显示">
            <a-switch v-model:checked="form.blindCount" />
            <span class="switch-label">盲盘，不向盘点人员显示账面数量</span>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="备注">
            <a-input v-model:value="form.remark" :maxlength="500" placeholder="选填" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { message, type FormInstance } from 'ant-design-vue'
import {
  CheckCircleFilled,
  EnvironmentOutlined,
  RetweetOutlined,
  SearchOutlined
} from '@ant-design/icons-vue'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import { createStocktake, getSelectableSkus } from '@/api/wms/stocktake'
import type {
  AvailableSkuVO,
  StocktakeDTO,
  StocktakeMode
} from '@/api/wms/stocktake/types'
import { listLocations, listVirtualLocations } from '@/api/wms/location-mgmt'
import type { WmsLocation } from '@/api/wms/location-mgmt/types'
import { isSuccess } from '@/api'

const emit = defineEmits<{ (e: 'submit-success', id: number): void }>()
const visible = ref(false)
const submitting = ref(false)
const locationsLoading = ref(false)
const skuLoading = ref(false)
const locations = ref<WmsLocation[]>([])
const virtualLocations = ref<WmsLocation[]>([])
const selectableSkus = ref<AvailableSkuVO[]>([])
const formRef = ref<FormInstance>()

const today = () => {
  const value = new Date()
  const offset = value.getTimezoneOffset() * 60000
  return new Date(value.getTime() - offset).toISOString().slice(0, 10)
}

const newForm = (): StocktakeDTO => ({
  warehouseId: undefined as unknown as number,
  stocktakeDate: today(),
  stocktakeScope: 'ALL',
  stocktakeMode: 'FULL',
  locationIds: [],
  specialSkuCodes: [],
  specialSearchAll: false,
  virtualLocationOnly: false,
  blindCount: true
})

const form = reactive<StocktakeDTO>(newForm())
const modes = [
  { value: 'FULL' as const, label: '全仓盘点', description: '逐库位盘完整个仓库', icon: EnvironmentOutlined },
  { value: 'CYCLE' as const, label: '循环盘点', description: '选择部分库位执行', icon: RetweetOutlined },
  { value: 'SPECIAL' as const, label: '专项盘点', description: '按 SKU 或货主核查', icon: SearchOutlined }
]

const locationOptions = computed(() =>
  locations.value
    .filter(item => item.isVirtual !== 1)
    .sort((a, b) => naturalCompare(a.locationCode, b.locationCode))
    .map(item => ({ label: item.locationCode, value: item.id }))
)
const virtualLocationOptions = computed(() =>
  [...virtualLocations.value]
    .sort((a, b) => naturalCompare(a.locationCode, b.locationCode))
    .map(item => ({ label: item.locationCode, value: item.id }))
)
const skuOptions = computed(() => {
  const merged = new Map<string, AvailableSkuVO>()
  for (const item of selectableSkus.value) {
    const current = merged.get(item.skuCode)
    if (current) {
      current.stockQuantity += item.stockQuantity || 0
      current.ownerName = '多个货主'
    } else {
      merged.set(item.skuCode, { ...item })
    }
  }
  return [...merged.values()]
    .sort((a, b) => naturalCompare(a.skuCode, b.skuCode))
    .map(item => ({
      value: item.skuCode,
      label: `${item.warehouseSkuCode || item.skuCode} · ${
        item.skuBrief?.skuName || item.skuBrief?.skuChineseName || '未命名商品'
      } · 库存 ${item.stockQuantity || 0}`,
      searchText: [
        item.skuCode,
        item.warehouseSkuCode,
        item.skuBrief?.skuName,
        item.skuBrief?.skuChineseName,
        item.ownerName
      ].filter(Boolean).join(' ').toLowerCase()
    }))
})

function naturalCompare(left?: string, right?: string) {
  return (left || '').localeCompare(right || '', 'zh-CN', {
    numeric: true,
    sensitivity: 'base'
  })
}

function filterLocation(input: string, option: any) {
  return String(option.label).toLowerCase().includes(input.toLowerCase())
}

function filterSku(input: string, option: any) {
  return String(option.searchText || option.label).includes(input.trim().toLowerCase())
}

function selectMode(mode: StocktakeMode) {
  form.stocktakeMode = mode
  form.stocktakeScope = mode === 'FULL' ? 'ALL' : 'PARTIAL'
  if (mode !== 'SPECIAL') form.virtualLocationOnly = false
  form.locationIds = []
  if (mode === 'SPECIAL') void loadSelectableSkus()
}

function handleSpecialTypeChange() {
  form.locationIds = []
  form.specialOwnerId = undefined
  form.specialSkuCodes = []
  form.specialSearchAll = false
}

async function handleSpecialOwnerChange() {
  form.specialSkuCodes = []
  await loadSelectableSkus()
}

async function loadSelectableSkus() {
  selectableSkus.value = []
  if (!form.warehouseId || form.virtualLocationOnly) return
  skuLoading.value = true
  try {
    const result = await getSelectableSkus(
      form.warehouseId,
      undefined,
      form.specialOwnerId
    )
    if (isSuccess(result)) selectableSkus.value = result.data || []
  } finally {
    skuLoading.value = false
  }
}

async function handleWarehouseChange() {
  form.locationIds = []
  locations.value = []
  virtualLocations.value = []
  selectableSkus.value = []
  form.specialSkuCodes = []
  if (!form.warehouseId) return
  locationsLoading.value = true
  try {
    const [physicalResult, virtualResult, skuResult] = await Promise.all([
      listLocations(form.warehouseId),
      listVirtualLocations(form.warehouseId),
      getSelectableSkus(form.warehouseId, undefined, form.specialOwnerId)
    ])
    if (isSuccess(physicalResult)) locations.value = physicalResult.data || []
    if (isSuccess(virtualResult)) virtualLocations.value = virtualResult.data || []
    if (isSuccess(skuResult)) selectableSkus.value = skuResult.data || []
  } finally {
    locationsLoading.value = false
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (form.stocktakeMode === 'SPECIAL' && form.virtualLocationOnly && !form.locationIds?.length) {
    message.warning('虚拟库位专项盘点请至少选择一个虚拟库位')
    return
  }
  if (
    form.stocktakeMode === 'SPECIAL' &&
    !form.virtualLocationOnly &&
    !form.specialOwnerId &&
    !form.specialSkuCodes?.length
  ) {
    message.warning('专项盘点请至少指定一个货主或 SKU')
    return
  }
  submitting.value = true
  try {
    const result = await createStocktake({ ...form })
    if (isSuccess(result) && result.data) {
      message.success('盘点任务创建成功')
      visible.value = false
      emit('submit-success', result.data)
    }
  } finally {
    submitting.value = false
  }
}

function handleClose() {
  visible.value = false
}

defineExpose({
  open() {
    Object.assign(form, newForm())
    locations.value = []
    virtualLocations.value = []
    selectableSkus.value = []
    visible.value = true
  }
})
</script>

<style scoped lang="less">
.mode-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.mode-option { position: relative; display: flex; align-items: center; gap: 12px; min-height: 76px; padding: 14px; border: 1px solid #d9d9d9; border-radius: 6px; background: #fff; color: #595959; text-align: left; cursor: pointer; }
.mode-option:hover { border-color: #69b1ff; }
.mode-option.active { border-color: #1677ff; background: #e6f4ff; color: #1677ff; }
.mode-icon { flex: 0 0 auto; font-size: 24px; }
.mode-copy { display: flex; min-width: 0; flex-direction: column; }
.mode-copy strong { font-size: 14px; color: #262626; }
.mode-copy small { margin-top: 3px; color: #8c8c8c; }
.checked { position: absolute; top: 8px; right: 8px; }
.field-hint { margin-top: 6px; color: #8c8c8c; font-size: 12px; }
.switch-label { margin-left: 10px; color: #595959; }
:deep(.ant-alert) { margin-bottom: 18px; }
</style>
