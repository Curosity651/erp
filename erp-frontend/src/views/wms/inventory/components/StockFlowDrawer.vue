<template>
  <a-drawer
    v-model:open="visible"
    title="库存详情"
    width="720"
    :destroy-on-close="true"
  >
    <!-- 上下文卡片 -->
    <div class="context-card">
      <div class="context-top">
        <a-tag :color="getWarehouseTypeColor(inventorySnapshot.warehouseType)">
          {{ WarehouseTypeMap[inventorySnapshot.warehouseType] || '仓库' }}
        </a-tag>
        <span class="context-warehouse-name">{{ warehouseName }}</span>
      </div>
      <div class="context-body">
        <sku-brief-cell :brief="inventorySnapshot.skuBrief" />
        <div class="context-quantities">
          <div class="qty-item">
            <span class="qty-label">可用</span>
            <span class="qty-value available">{{ inventorySnapshot.available }}</span>
          </div>
          <div class="qty-item">
            <span class="qty-label">在途</span>
            <span class="qty-value in-transit">{{ inventorySnapshot.inTransit }}</span>
          </div>
          <div class="qty-item">
            <span class="qty-label">残品</span>
            <span :class="['qty-value', 'damaged', { 'has-value': inventorySnapshot.damaged > 0 }]">
              {{ inventorySnapshot.damaged }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <a-segmented v-model:value="directionFilter" :options="directionOptions" @change="handleFilterChange" />
      <a-select
        v-model:value="postingTypeFilter"
        placeholder="过账类型"
        allow-clear
        mode="multiple"
        :max-tag-count="1"
        style="width: 200px"
        :options="postingTypeOptions"
        @change="handleFilterChange"
      />
    </div>

    <!-- 流水表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :loading="loading"
      :pagination="pagination"
      size="small"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <!-- 时间列 -->
        <template v-if="column.key === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>

        <!-- 业务类型列 -->
        <template v-else-if="column.key === 'postingType'">
          <a-tag :color="getPostingTypeColor(record.postingType)" class="posting-tag">
            {{ PostingTypeMap[record.postingType] || record.postingType }}
          </a-tag>
        </template>

        <!-- 库存变动列 -->
        <template v-else-if="column.key === 'bucketChange'">
          <div class="bucket-change">
            <div class="bucket-change-main">
              <span class="bucket-name" :style="{ color: getBucketColor(record.bucket) }">
                {{ BucketMap[record.bucket] || record.bucket }}
              </span>
              <span :class="record.direction === 'IN' ? 'delta-positive' : 'delta-negative'">
                {{ record.direction === 'IN' ? '+' : '-' }}{{ record.quantity }}
              </span>
            </div>
            <div class="bucket-change-detail">
              {{ record.beforeQuantity }} → {{ record.afterQuantity }}
            </div>
          </div>
        </template>

        <!-- 来源单号列 -->
        <template v-else-if="column.key === 'sourceNo'">
          <span v-if="record.sourceNo" class="source-no">{{ record.sourceNo }}</span>
          <span v-else class="text-tertiary">-</span>
        </template>
      </template>
    </a-table>

    <!-- 底部 -->
    <template #footer>
      <div class="drawer-footer">
        <a class="view-all-link" @click="goToAllFlows">
          查看完整流水
          <right-outlined />
        </a>
        <a-button @click="visible = false">关闭</a-button>
      </div>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { RightOutlined } from '@ant-design/icons-vue'
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { pageStockFlow, listStockFlowPostingTypes } from '@/api/wms/stock-flow'
import { getInventoryDetail } from '@/api/wms/inventory'
import type { StockFlowPageVO } from '@/api/wms/stock-flow/types'
import type { SkuBriefVO } from '@/api/common/sku-types'
import { SkuBriefCell } from '@/components/Sku'
import {
  PostingTypeMap,
  getPostingTypeColor,
  BucketMap,
  getBucketColor,
  WarehouseTypeMap,
  getWarehouseTypeColor
} from '../../shared/constants'

defineOptions({ name: 'StockFlowDrawer' })

const router = useRouter()

const visible = ref(false)
const loading = ref(false)
const data = ref<StockFlowPageVO[]>([])
const warehouseId = ref<number>()
const warehouseName = ref('')
const skuCode = ref('')

// 库存快照
const inventorySnapshot = reactive<{
  warehouseType: string
  skuBrief?: SkuBriefVO
  available: number
  inTransit: number
  damaged: number
}>({
  warehouseType: 'OWN',
  skuBrief: undefined,
  available: 0,
  inTransit: 0,
  damaged: 0
})

// 筛选
const directionFilter = ref<string>('all')
const postingTypeFilter = ref<string[]>([])

// 过账类型下拉：动态选项，仅展示"本仓+本SKU实际出现过的类型"
const postingTypeOptions = ref<{ value: string; label: string }[]>([])

const directionOptions = [
  { label: '全部', value: 'all' },
  { label: '入库', value: 'IN' },
  { label: '出库', value: 'OUT' }
]

async function loadPostingTypeOptions() {
  postingTypeOptions.value = []
  try {
    const res = await listStockFlowPostingTypes(warehouseId.value, skuCode.value)
    if (isSuccess(res) && res.data) {
      postingTypeOptions.value = res.data.map(code => ({
        value: code,
        label: PostingTypeMap[code] || code
      }))
    }
  } catch {
    // 选项加载失败不阻断流水展示
  }
}

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: false,
  size: 'small'
})

const columns: TableColumnsType = [
  { title: '时间', key: 'createTime', width: 100 },
  { title: '业务类型', key: 'postingType', width: 110 },
  { title: '库存变动', key: 'bucketChange', width: 140 },
  { title: '来源单号', key: 'sourceNo', width: 150, ellipsis: true }
]

function formatTime(time: string) {
  const d = dayjs(time)
  if (d.year() === dayjs().year()) {
    return d.format('MM-DD HH:mm')
  }
  return d.format('YYYY-MM-DD HH:mm')
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      current: pagination.current,
      size: pagination.pageSize,
      warehouseId: warehouseId.value,
      skuCode: skuCode.value
    }
    if (directionFilter.value !== 'all') {
      params.direction = directionFilter.value
    }
    if (postingTypeFilter.value.length > 0) {
      params.postingTypes = postingTypeFilter.value
    }
    const result = await pageStockFlow(params as any)
    if (isSuccess(result) && result.data) {
      data.value = result.data.records
      pagination.total = result.data.total
    }
  } catch (e) {
    console.error('加载流水失败', e)
  } finally {
    loading.value = false
  }
}

async function loadSnapshot() {
  try {
    const result = await getInventoryDetail(warehouseId.value!, skuCode.value)
    if (isSuccess(result) && result.data) {
      const d = result.data
      inventorySnapshot.warehouseType = d.warehouseDisplay?.warehouseType || 'OWN'
      inventorySnapshot.skuBrief = d.skuBrief
      inventorySnapshot.available = d.availableQuantity ?? 0
      inventorySnapshot.inTransit = d.inTransitQuantity ?? 0
      inventorySnapshot.damaged = d.damagedQuantity ?? 0
    }
  } catch {
    // 快照加载失败不阻断流水展示
  }
}

function handleFilterChange() {
  pagination.current = 1
  loadData()
}

function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current
  loadData()
}

function goToAllFlows() {
  router.push({
    path: '/inventory/stock-record',
    query: { view: 'flow', warehouseId: warehouseId.value, skuCode: skuCode.value }
  })
  visible.value = false
}

async function open(wId: number, code: string, wName?: string) {
  warehouseId.value = wId
  skuCode.value = code
  warehouseName.value = wName || `仓库${wId}`
  pagination.current = 1
  directionFilter.value = 'all'
  postingTypeFilter.value = []
  // 重置快照
  inventorySnapshot.warehouseType = 'OWN'
  inventorySnapshot.skuBrief = { skuCode: code } as SkuBriefVO
  inventorySnapshot.available = 0
  inventorySnapshot.inTransit = 0
  inventorySnapshot.damaged = 0

  visible.value = true
  // 并行加载流水、快照、过账类型选项
  loadData()
  loadSnapshot()
  loadPostingTypeOptions()
}

defineExpose({ open })
</script>

<style scoped>
/* 上下文卡片 */
.context-card {
  background: var(--ant-color-fill-quaternary, #fafafa);
  border: 1px solid var(--ant-color-border-secondary, #f0f0f0);
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
}

.context-top {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

.context-warehouse-name {
  font-weight: 600;
  font-size: 14px;
}

.context-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.context-quantities {
  display: flex;
  gap: 28px;
  flex-shrink: 0;
}

.qty-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 48px;
}

.qty-label {
  font-size: 12px;
  color: var(--ant-color-text-tertiary, #999);
  margin-bottom: 4px;
}

.qty-value {
  font-size: 20px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.qty-value.available {
  color: var(--ant-color-success, #52c41a);
}

.qty-value.in-transit {
  color: #722ed1;
}

.qty-value.damaged {
  color: var(--ant-color-text-tertiary, #999);
}

.qty-value.damaged.has-value {
  color: var(--ant-color-error, #ff4d4f);
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

/* 表格内库存变动 */
.bucket-change .bucket-change-main {
  display: flex;
  align-items: center;
  gap: 6px;
}

.bucket-change .bucket-name {
  font-weight: 500;
}

.bucket-change .bucket-change-detail {
  margin-top: 2px;
  font-size: 12px;
  color: var(--ant-color-text-tertiary, #8c8c8c);
}

.bucket-change .delta-positive {
  color: var(--ant-color-success, #52c41a);
  font-weight: 600;
}

.bucket-change .delta-negative {
  color: var(--ant-color-error, #ff4d4f);
  font-weight: 600;
}

.posting-tag {
  margin: 0;
}

.source-no {
  color: var(--ant-color-text-secondary, #666);
}

.text-tertiary {
  color: var(--ant-color-text-tertiary, #999);
}

/* 底部 */
.drawer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.view-all-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}
</style>
