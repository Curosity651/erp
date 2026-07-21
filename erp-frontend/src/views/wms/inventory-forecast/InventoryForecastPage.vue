<template>
  <div class="inventory-forecast-page">
    <a-card title="库存预测" :bordered="false">
      <!-- 状态统计卡片 -->
      <ForecastStatusCards
        v-model="activeStatus"
        :status-counts="statusCounts"
        :threshold-days="thresholdDays"
      />

      <!-- 搜索栏 -->
      <div class="table-toolbar">
        <a-space>
          <RegionSelect
            v-model:value="searchParams.regionId"
            placeholder="选择区域"
            allow-clear
            @change="handleSearch"
          />
          <a-input-search
            v-model:value="searchParams.skuKeyword"
            placeholder="搜索SKU"
            style="width: 200px"
            @search="handleSearch"
          />
          <a-select
            v-model:value="searchParams.status"
            placeholder="库存状态"
            allow-clear
            style="width: 120px"
            @change="handleStatusSelectChange"
          >
            <a-select-option value="SUFFICIENT">充足</a-select-option>
            <a-select-option value="LOW">偏低</a-select-option>
            <a-select-option value="CRITICAL">告急</a-select-option>
            <a-select-option value="STOCKOUT">断货</a-select-option>
            <a-select-option value="NO_SALES">待观察</a-select-option>
          </a-select>
          <a-select v-model:value="searchParams.days" style="width: 120px" @change="handleSearch">
            <a-select-option :value="7">预测 7 天</a-select-option>
            <a-select-option :value="14">预测 14 天</a-select-option>
            <a-select-option :value="30">预测 30 天</a-select-option>
            <a-select-option :value="60">预测 60 天</a-select-option>
            <a-select-option :value="90">预测 90 天</a-select-option>
          </a-select>
        </a-space>
        <div class="threshold-hint">预警阈值: {{ thresholdDays }} 天</div>
      </div>

      <!-- 预测列表 -->
      <pro-table
        ref="tableRef"
        :row-key="(record: ForecastSummaryVO) => record.regionId + ':' + record.skuCode"
        :request="tableRequest"
        :columns="columns"
        :tool-bar-render="false"
        :card-props="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'skuBrief'">
            <SkuBriefCell :brief="record.skuBrief" />
          </template>
          <template v-else-if="column.key === 'stock'">
            <div>可售: {{ record.sellableQuantity }}</div>
            <div class="text-secondary">在途: {{ record.inTransitQuantity }}</div>
            <div v-if="record.pendingShipmentQuantity > 0" class="text-tertiary">
              待发货: {{ record.pendingShipmentQuantity }}
            </div>
          </template>
          <template v-else-if="column.key === 'sellableDays'">
            <a-tag :color="getStatusTagColor(record.status)">
              {{ formatSellableDays(record.sellableDays, record.status, searchParams.days) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'stockoutDate'">
            {{ record.stockoutDate || '—' }}
          </template>
          <template v-else-if="column.key === 'operate'">
            <a @click="showDetail(record)">查看详情</a>
          </template>
        </template>
      </pro-table>
    </a-card>

    <!-- 详情抽屉 -->
    <ForecastDetailDrawer
      v-model:open="detailDrawerOpen"
      :region-id="selectedRegionId"
      :sku-code="selectedSkuCode"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import type { TableRequest, ProTableInstanceExpose } from '#/table'
import { isSuccess } from '@/api'
import { getForecastSummary } from '@/api/wms/inventory-forecast'
import { mergePageParam } from '@/utils/page-utils'
import RegionSelect from '@/components/Lov/RegionSelect.vue'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'
import ForecastDetailDrawer from './components/ForecastDetailDrawer.vue'
import ForecastStatusCards from './components/ForecastStatusCards.vue'
import { useForecastStatus } from './composables/use-forecast-status'
import type {
  ForecastSummaryVO,
  ForecastStatus,
  ForecastStatusCounts
} from '@/api/wms/inventory-forecast/types'

defineOptions({ name: 'InventoryForecastPage' })

const { getStatusTagColor, formatSellableDays } = useForecastStatus()

const tableRef = ref<ProTableInstanceExpose>()

const columns: ProColumns[] = [
  { title: '区域', dataIndex: 'regionName', key: 'regionName', width: 120 },
  { title: 'SKU', key: 'skuBrief', width: 280 },
  { title: '库存', key: 'stock', width: 120 },
  { title: '日均销量', dataIndex: 'dailySales', key: 'dailySales', width: 100 },
  { title: '可售天数', key: 'sellableDays', width: 100 },
  { title: '预计断货', key: 'stockoutDate', width: 120 },
  { title: '操作', key: 'operate', width: 100, fixed: 'right' }
]

const thresholdDays = ref(7)
const activeStatus = ref<ForecastStatus | null>(null)
const statusCounts = ref<ForecastStatusCounts>({
  SUFFICIENT: 0,
  LOW: 0,
  CRITICAL: 0,
  STOCKOUT: 0,
  NO_SALES: 0
})

const searchParams = reactive({
  regionId: undefined as number | undefined,
  skuKeyword: '',
  status: undefined as ForecastStatus | undefined,
  days: 30 as number
})

const detailDrawerOpen = ref(false)
const selectedRegionId = ref<number>()
const selectedSkuCode = ref<string>()

/* 远程加载表格数据 */
const tableRequest: TableRequest = async (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  const result = await getForecastSummary({ ...pageParam, ...searchParams })
  if (isSuccess(result)) {
    thresholdDays.value = result.data.thresholdDays
    statusCounts.value = result.data.statusCounts
    return {
      ...result,
      data: {
        records: result.data.list,
        total: result.data.total
      }
    }
  }
  return result
}

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

function handleSearch() {
  reloadTable(true)
}

function handleStatusSelectChange(status: ForecastStatus | undefined) {
  // 同步更新卡片状态，watch 会自动触发查询
  activeStatus.value = status ?? null
}

// 监听状态变化，触发查询
watch(activeStatus, status => {
  searchParams.status = status ?? undefined
  reloadTable(true)
})

function showDetail(record: ForecastSummaryVO) {
  selectedRegionId.value = record.regionId
  selectedSkuCode.value = record.skuCode
  detailDrawerOpen.value = true
}
</script>

<style scoped>
.inventory-forecast-page {
  padding: 16px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.threshold-hint {
  color: var(--ant-color-text-secondary);
  font-size: 13px;
}

.text-secondary {
  color: var(--ant-color-text-secondary);
  font-size: 12px;
}

.text-tertiary {
  color: var(--ant-color-text-tertiary);
  font-size: 12px;
  font-style: italic;
}
</style>
