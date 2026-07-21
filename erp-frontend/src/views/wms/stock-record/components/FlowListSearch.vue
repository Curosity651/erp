<template>
  <a-card :bordered="false" class="search-card" :body-style="{ paddingBottom: 0 }">
    <a-form :model="formModel" class="search-form">
      <a-row :gutter="16">
        <!-- 日期 -->
        <a-col :xs="24" :sm="12" :md="12" :lg="8" :xl="8">
          <a-form-item label="日期">
            <a-range-picker
              v-model:value="formModel.timeRange"
              :placeholder="['开始时间', '结束时间']"
              :allow-clear="false"
              :presets="rangePresets"
              :show-time="{ format: 'HH:mm' }"
              format="YYYY-MM-DD HH:mm"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <!-- 区域 -->
        <a-col :xs="24" :sm="12" :md="12" :lg="8" :xl="8">
          <a-form-item label="区域">
            <region-select
              v-model:value="formModel.regionId"
              placeholder="全部区域"
              allow-clear
              style="width: 100%"
              @change="handleRegionChange"
            />
          </a-form-item>
        </a-col>
        <!-- 仓库 -->
        <a-col :xs="24" :sm="12" :md="12" :lg="8" :xl="8">
          <a-form-item label="仓库">
            <warehouse-select
              v-model:value="formModel.warehouseId"
              :region-id="formModel.regionId"
              placeholder="全部仓库"
              allow-clear
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <!-- SKU -->
        <a-col :xs="24" :sm="12" :md="12" :lg="8" :xl="8">
          <a-form-item label="SKU">
            <sku-select-input v-model="formModel.skuCode" placeholder="选择SKU" />
          </a-form-item>
        </a-col>
        <!-- 类型 -->
        <a-col :xs="24" :sm="12" :md="12" :lg="8" :xl="8">
          <a-form-item label="类型">
            <a-select
              v-model:value="formModel.postingType"
              placeholder="全部类型"
              allow-clear
              :options="postingTypeOptions"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <!-- 方向 -->
        <a-col :xs="24" :sm="12" :md="12" :lg="8" :xl="8">
          <a-form-item label="方向">
            <a-select
              v-model:value="formModel.flowDirection"
              placeholder="全部方向"
              allow-clear
              :options="flowDirectionOptions"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <!-- 过账单号 -->
        <a-col :xs="24" :sm="12" :md="12" :lg="8" :xl="8">
          <a-form-item label="过账单号">
            <a-input v-model:value="formModel.postingNo" placeholder="输入过账单号" allow-clear />
          </a-form-item>
        </a-col>
        <!-- 操作按钮 -->
        <a-col :xs="24" :sm="12" :md="12" :lg="8" :xl="8">
          <search-actions :loading="props.loading" @search="search" @reset="reset" />
        </a-col>
      </a-row>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { reactive, watch, toRaw, computed } from 'vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import RegionSelect from '@/components/Lov/RegionSelect.vue'
import SkuSelectInput from '@/components/Sku/SkuSelectInput.vue'
import { postingTypeOptions } from '../../shared/constants'

const props = withDefaults(
  defineProps<{
    loading?: boolean
    initialWarehouseId?: number
    initialSkuCode?: string
    initialPostingNo?: string
  }>(),
  { loading: false }
)

const emits = defineEmits<{
  (e: 'search', params: Record<string, any>): void
}>()

const flowDirectionOptions = [
  { label: '入库', value: 'IN' },
  { label: '出库', value: 'OUT' }
]

// 预设时间范围
const rangePresets = computed(() => [
  { label: '今日', value: [dayjs().startOf('day'), dayjs().endOf('day')] as [Dayjs, Dayjs] },
  {
    label: '近7天',
    value: [dayjs().subtract(6, 'day').startOf('day'), dayjs().endOf('day')] as [Dayjs, Dayjs]
  },
  {
    label: '近30天',
    value: [dayjs().subtract(29, 'day').startOf('day'), dayjs().endOf('day')] as [Dayjs, Dayjs]
  },
  { label: '本月', value: [dayjs().startOf('month'), dayjs().endOf('day')] as [Dayjs, Dayjs] }
])

// 默认近7天
const defaultTimeRange: [Dayjs, Dayjs] = [
  dayjs().subtract(6, 'day').startOf('day'),
  dayjs().endOf('day')
]

const formModel = reactive({
  regionId: undefined as number | undefined,
  warehouseId: props.initialWarehouseId,
  skuCode: props.initialSkuCode || '',
  postingNo: props.initialPostingNo || '',
  postingType: undefined as string | undefined,
  flowDirection: undefined as string | undefined,
  timeRange: defaultTimeRange as [Dayjs, Dayjs]
})

// 区域变化时清空仓库选择
function handleRegionChange() {
  formModel.warehouseId = undefined
}

const search = () => {
  const { timeRange, ...rest } = toRaw(formModel)
  const params = {
    ...rest,
    startTime: formModel.timeRange[0].format('YYYY-MM-DD HH:mm:ss'),
    endTime: formModel.timeRange[1].format('YYYY-MM-DD HH:mm:ss')
  }
  emits('search', params)
}

const reset = () => {
  formModel.regionId = undefined
  formModel.warehouseId = undefined
  formModel.skuCode = ''
  formModel.postingNo = ''
  formModel.postingType = undefined
  formModel.flowDirection = undefined
  formModel.timeRange = defaultTimeRange
  search()
}

// 初始参数变化时更新表单并触发查询
watch(
  () => [props.initialWarehouseId, props.initialSkuCode, props.initialPostingNo],
  ([warehouseId, skuCode, postingNo]) => {
    let changed = false
    if (warehouseId && warehouseId !== formModel.warehouseId) {
      formModel.warehouseId = warehouseId
      changed = true
    }
    if (skuCode && skuCode !== formModel.skuCode) {
      formModel.skuCode = skuCode as string
      changed = true
    }
    if (postingNo && postingNo !== formModel.postingNo) {
      formModel.postingNo = postingNo as string
      changed = true
    }
    if (changed) {
      search()
    }
  }
)

defineExpose({
  search,
  getFormModel: () => toRaw(formModel)
})
</script>

<style scoped>
.search-card {
  margin-bottom: 16px;
}

.search-card :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.search-card :deep(.ant-form-item-label > label) {
  color: var(--text-secondary, #595959);
}
</style>
