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
        <!-- 仓库 -->
        <a-col :xs="24" :sm="12" :md="12" :lg="8" :xl="8">
          <a-form-item label="仓库">
            <warehouse-select
              v-model:value="formModel.warehouseId"
              placeholder="全部仓库"
              allow-clear
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <!-- 操作类型 -->
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
        <!-- 来源单号 -->
        <a-col :xs="24" :sm="12" :md="12" :lg="8" :xl="8">
          <a-form-item label="来源单号">
            <a-input v-model:value="formModel.sourceNo" placeholder="输入来源单号" allow-clear />
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
import { postingTypeOptions } from '../../shared/constants'

const props = withDefaults(
  defineProps<{
    loading?: boolean
    initialWarehouseId?: number
    initialSourceNo?: string
  }>(),
  { loading: false }
)

const emits = defineEmits<{
  (e: 'search', params: Record<string, any>): void
}>()

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
  warehouseId: props.initialWarehouseId,
  postingType: undefined as string | undefined,
  sourceNo: props.initialSourceNo || '',
  timeRange: defaultTimeRange as [Dayjs, Dayjs]
})

const search = () => {
  const { timeRange, ...rest } = toRaw(formModel)
  const params = {
    ...rest,
    postTimeStart: formModel.timeRange[0].format('YYYY-MM-DD HH:mm:ss'),
    postTimeEnd: formModel.timeRange[1].format('YYYY-MM-DD HH:mm:ss')
  }
  emits('search', params)
}

const reset = () => {
  formModel.warehouseId = undefined
  formModel.postingType = undefined
  formModel.sourceNo = ''
  formModel.timeRange = defaultTimeRange
  search()
}

watch(
  () => [props.initialWarehouseId, props.initialSourceNo],
  ([warehouseId, sourceNo]) => {
    if (warehouseId) formModel.warehouseId = warehouseId
    if (sourceNo) formModel.sourceNo = sourceNo as string
  }
)

defineExpose({
  search,
  getFormModel: () => toRaw(formModel)
})
</script>

<style lang="less" scoped>
.search-card {
  margin-bottom: 16px;

  .search-form {
    :deep(.ant-form-item) {
      margin-bottom: 16px;
    }

    :deep(.ant-form-item-label) {
      > label {
        color: #595959;
      }
    }
  }
}
</style>
