<template>
  <div class="flow-list-view">
    <flow-list-search
      ref="searchRef"
      :loading="tableRef?.loading"
      :initial-warehouse-id="initialWarehouseId"
      :initial-sku-code="initialSkuCode"
      :initial-posting-no="initialPostingNo"
      @search="searchTable"
    />

    <pro-table
      ref="tableRef"
      header-title="流水明细"
      row-key="id"
      :request="tableRequest"
      :columns="columns"
      :scroll="{ x: 900 }"
    >
      <template #toolBarRender>
        <export-confirm-button :on-export="handleExport" />
      </template>

      <template #bodyCell="{ column, record }">
        <!-- 时间 -->
        <template v-if="column.key === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>

        <!-- 类型 -->
        <template v-else-if="column.key === 'postingType'">
          <a-tag :color="getPostingTypeColor(record.postingType)">
            {{ PostingTypeMap[record.postingType] || record.postingType }}
          </a-tag>
        </template>

        <!-- 仓库/区域 -->
        <template v-else-if="column.key === 'location'">
          <span v-if="record.warehouseId > 0">
            {{ record.warehouseDisplay?.warehouseName || '-' }}
          </span>
          <a-tag v-else-if="record.regionId > 0" color="purple" class="region-tag">
            <global-outlined />
            {{ record.regionDisplay?.regionName || '-' }}
          </a-tag>
          <span v-else>-</span>
        </template>

        <!-- SKU 信息 -->
        <template v-else-if="column.key === 'skuInfo'">
          <sku-brief-cell :brief="record.skuBrief" />
        </template>

        <!-- 库存桶变动 -->
        <template v-else-if="column.key === 'bucketChange'">
          <div class="bucket-change">
            <div class="bucket-change-main">
              <span class="bucket-name" :style="{ color: getBucketColor(record.bucket) }">
                {{ BucketMap[record.bucket] || record.bucket }}
              </span>
              <span :class="eventDelta(record) > 0 ? 'delta-positive' : 'delta-negative'">
                {{ eventDelta(record) > 0 ? '+' : '' }}{{ eventDelta(record) }}
              </span>
            </div>
            <div class="bucket-change-detail">
              {{ record.beforeQuantity }} → {{ record.afterQuantity }}
            </div>
			<div class="bucket-change-detail" v-if="record.reservedDelta">
			  预占 {{ signed(record.reservedDelta) }}（{{ record.beforeReserved }} → {{ record.afterReserved }}）
			</div>
          </div>
        </template>

		<template v-else-if="column.key === 'locationCode'">
		  <span>{{ record.locationCode || '-' }}</span>
		  <span v-if="record.counterpartLocationCode" class="text-tertiary">
			→ {{ record.counterpartLocationCode }}
		  </span>
		</template>

        <!-- 来源单号 -->
        <template v-else-if="column.key === 'sourceNo'">
          <a v-if="record.sourceNo" @click="handleViewSource(record)">
            {{ record.sourceNo }}
          </a>
          <span v-else class="text-tertiary">-</span>
        </template>

        <!-- 操作 -->
        <template v-else-if="column.key === 'operate'">
          <a @click="handleViewInventory(record)">查看库存</a>
        </template>
      </template>
    </pro-table>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { GlobalOutlined } from '@ant-design/icons-vue'
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import type { ProTableInstanceExpose } from '#/table/Table'
import type { TableRequest } from '#/table/typing'
import { mergePageParam } from '@/utils/page-utils'
import { pageStockFlow } from '@/api/wms/stock-flow'
import type { StockFlowPageVO } from '@/api/wms/stock-flow/types'
import { SkuBriefCell } from '@/components/Sku'
import { ExportConfirmButton } from '@/components/Button'
import FlowListSearch from '../components/FlowListSearch.vue'
import {
  PostingTypeMap,
  getPostingTypeColor,
  BucketMap,
  getBucketColor
} from '../../shared/constants'

defineOptions({ name: 'FlowListView' })

const props = defineProps<{
  initialWarehouseId?: number
  initialSkuCode?: string
  initialPostingNo?: string
}>()

const emit = defineEmits<{
  (e: 'view-source', record: StockFlowPageVO): void
}>()

const router = useRouter()

const tableRef = ref<ProTableInstanceExpose>()
const searchRef = ref<InstanceType<typeof FlowListSearch>>()

let searchParams: Record<string, any> = {}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  // 搜索栏字段名 → 后端 StockFlowQO 字段名映射
  const { startTime, endTime, flowDirection, postingType, ...rest } = searchParams
  return pageStockFlow({
    ...pageParam,
    ...rest,
    startTime: startTime || undefined,
    endTime: endTime || undefined,
    direction: flowDirection || undefined,
    postingTypes: postingType ? [postingType] : undefined
  })
}

const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

const searchTable = (params: Record<string, any>) => {
  searchParams = params
  reloadTable(true)
}

function formatTime(time: string) {
  return dayjs(time).format('MM-DD HH:mm')
}

function eventDelta(record: StockFlowPageVO) {
  return record.quantityDelta !== 0 ? record.quantityDelta : record.reservedDelta
}

function signed(value: number) {
  return value > 0 ? `+${value}` : String(value)
}

function handleViewSource(record: StockFlowPageVO) {
  emit('view-source', record)
}

function handleViewInventory(record: StockFlowPageVO) {
  if (record.warehouseId && record.warehouseId > 0) {
    // 仓库流水 → 库存明细页
    router.push({
      path: '/inventory/inventory-detail',
      query: {
        warehouseId: String(record.warehouseId),
        skuCode: record.skuCode
      }
    })
  } else {
    // 区域流水 → 库存总览页，选中区域维度 tab
    router.push({
      path: '/inventory/inventory-overview',
      query: {
        view: 'region',
        skuCode: record.skuCode
      }
    })
  }
}

async function handleExport() {
	const result = await pageStockFlow({ page: 1, size: 10000, ...searchParams })
	const rows = result.data?.records || []
	const headers = ['时间', '类型', '仓库', '库位', '目标库位', 'SKU', '品质', '实物变化', '预占变化', '来源单号']
	const csv = [headers, ...rows.map(row => [row.createTime, PostingTypeMap[row.postingType] || row.postingType,
		row.warehouseDisplay?.warehouseName || '', row.locationCode || '', row.counterpartLocationCode || '',
		row.skuCode, row.quality === 'DEFECTIVE' ? '不良品' : '良品', row.quantityDelta,
		row.reservedDelta, row.sourceNo || ''])]
		.map(row => row.map(value => `"${String(value ?? '').replace(/"/g, '""')}"`).join(',')).join('\r\n')
	const link = document.createElement('a')
	link.href = URL.createObjectURL(new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' }))
	link.download = `库存流水-${new Date().toISOString().slice(0, 10)}.csv`
	link.click()
	URL.revokeObjectURL(link.href)
}

const columns: ProColumns[] = [
  { title: '时间', key: 'createTime', width: 100 },
  { title: '类型', key: 'postingType', width: 100 },
  { title: '仓库/区域', key: 'location', width: 100 },
  { title: 'SKU 信息', key: 'skuInfo', width: 220 },
	{ title: '库位', key: 'locationCode', width: 150 },
  { title: '库存桶变动', key: 'bucketChange', width: 180 },
  { title: '来源单号', key: 'sourceNo', width: 140 },
  { title: '操作', key: 'operate', width: 80, align: 'center' }
]

defineExpose({
  reload: reloadTable
})
</script>

<style scoped>
.flow-list-view .bucket-change .bucket-change-main {
  display: flex;
  align-items: center;
  gap: 6px;
}

.flow-list-view .bucket-change .bucket-change-main .bucket-name {
  font-weight: 500;
}

.flow-list-view .bucket-change .bucket-change-detail {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-tertiary, #8c8c8c);
}

.flow-list-view .bucket-change .delta-positive {
  color: var(--success-color, #52c41a);
  font-weight: 600;
}

.flow-list-view .bucket-change .delta-negative {
  color: var(--error-color, #ff4d4f);
  font-weight: 600;
}

.flow-list-view .text-tertiary {
  color: var(--text-tertiary, #999);
}

.flow-list-view .region-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>
