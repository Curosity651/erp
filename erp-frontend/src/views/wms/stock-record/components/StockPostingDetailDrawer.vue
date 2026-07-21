<template>
  <a-drawer v-model:open="visible" title="操作单详情" :width="560" :destroy-on-close="true">
    <!-- 概况信息区 -->
    <a-spin :spinning="detailLoading">
      <a-descriptions :column="2" bordered size="small" class="detail-descriptions">
        <a-descriptions-item label="单号">
          {{ detail?.postingNo || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="类型">
          <a-tag v-if="detail?.postingType" :color="getPostingTypeColor(detail.postingType)">
            {{ detail.postingTypeDesc || PostingTypeMap[detail.postingType] || detail.postingType }}
          </a-tag>
          <span v-else>-</span>
        </a-descriptions-item>
        <a-descriptions-item label="仓库/区域">
          <span v-if="detail?.warehouseId && detail.warehouseId > 0">
            {{ detail?.warehouseDisplay?.warehouseName || '-' }}
          </span>
          <a-tag v-else-if="detail?.regionId && detail.regionId > 0" color="purple">
            <global-outlined />
            {{ detail?.regionDisplay?.regionName || '-' }}
          </a-tag>
          <span v-else>-</span>
        </a-descriptions-item>
        <a-descriptions-item label="时间">
          {{ detail?.postTime ? formatTime(detail.postTime) : '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="来源单号">
          <a v-if="detail?.sourceNo" @click="handleViewSource">{{ detail.sourceNo }}</a>
          <span v-else>-</span>
        </a-descriptions-item>
        <a-descriptions-item label="SKU 数">
          {{ detail?.skuCount ?? '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="总数量">
          {{ detail?.totalQuantity ?? '-' }}
        </a-descriptions-item>
      </a-descriptions>
    </a-spin>

    <!-- SKU 搜索框 -->
    <div class="search-box">
      <a-input-search
        v-model:value="searchSkuCode"
        placeholder="请输入 SKU 编码搜索"
        allow-clear
        @search="handleSearch"
        @change="handleSearchChange"
      />
    </div>

    <!-- SKU 明细列表 -->
    <a-table
      :data-source="itemList"
      :columns="itemColumns"
      :loading="itemLoading"
      :pagination="pagination"
      row-key="id"
      size="small"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <!-- 仓库/区域 -->
        <template v-if="column.key === 'location'">
          <span v-if="record.warehouseId > 0">
            {{ record.warehouseDisplay?.warehouseName || '-' }}
          </span>
          <a-tag v-else-if="record.regionId > 0" color="purple" size="small">
            <global-outlined />
            {{ record.regionDisplay?.regionName || '-' }}
          </a-tag>
          <span v-else>-</span>
        </template>
        <!-- SKU 信息 -->
        <template v-else-if="column.key === 'skuInfo'">
          <sku-brief-cell :brief="record.skuBrief" />
        </template>
        <!-- 库存桶 -->
        <template v-else-if="column.key === 'bucket'">
          <span :style="{ color: getBucketColor(record.bucket) }">
            {{ BucketMap[record.bucket] || record.bucket }}
          </span>
        </template>
        <!-- 变动 -->
        <template v-else-if="column.key === 'change'">
          <span :class="record.direction === 'IN' ? 'delta-positive' : 'delta-negative'">
            {{ record.direction === 'IN' ? '+' : '-' }}{{ record.quantity }}
          </span>
        </template>
      </template>
    </a-table>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, watch, reactive } from 'vue'
import dayjs from 'dayjs'
import { GlobalOutlined } from '@ant-design/icons-vue'
import { isSuccess } from '@/api'
import { getStockPostingDetail, pageStockPostingItem } from '@/api/wms/stock-posting'
import type { StockPostingDetailVO, StockPostingItemPageVO } from '@/api/wms/stock-posting/types'
import { SkuBriefCell } from '@/components/Sku'
import {
  PostingTypeMap,
  getPostingTypeColor,
  BucketMap,
  getBucketColor
} from '../../shared/constants'

const props = defineProps<{
  open: boolean
  postingId?: number
}>()

const emit = defineEmits<{
  (e: 'update:open', open: boolean): void
  (e: 'view-source', record: StockPostingDetailVO): void
}>()

const visible = ref(false)

// 详情数据
const detail = ref<StockPostingDetailVO | null>(null)
const detailLoading = ref(false)

// 明细列表
const itemList = ref<StockPostingItemPageVO[]>([])
const itemLoading = ref(false)
const searchSkuCode = ref('')

// 分页
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: false,
  showTotal: (total: number) => `共 ${total} 条`
})

// 表格列
const itemColumns = [
  { title: '仓库/区域', key: 'location', width: '25%' },
  { title: 'SKU 信息', key: 'skuInfo', width: '35%' },
  { title: '库存桶', key: 'bucket', width: '20%' },
  { title: '变动', key: 'change', width: '20%', align: 'right' as const }
]

// 监听 visible 变化
watch(
  () => props.open,
  val => {
    visible.value = val
    if (val && props.postingId) {
      // 重置状态
      searchSkuCode.value = ''
      pagination.current = 1
      loadDetail()
      loadItems()
    }
  }
)

watch(visible, val => {
  emit('update:open', val)
})

// 加载详情
async function loadDetail() {
  if (!props.postingId) return
  detailLoading.value = true
  try {
    const result = await getStockPostingDetail(props.postingId)
    if (isSuccess(result) && result.data) {
      detail.value = result.data
    }
  } finally {
    detailLoading.value = false
  }
}

// 加载明细列表
async function loadItems() {
  if (!props.postingId) return
  itemLoading.value = true
  try {
    const result = await pageStockPostingItem({
      postingId: props.postingId,
      skuCode: searchSkuCode.value || undefined,
      current: pagination.current,
      size: pagination.pageSize
    })
    if (isSuccess(result) && result.data) {
      itemList.value = result.data.records
      pagination.total = result.data.total
    }
  } finally {
    itemLoading.value = false
  }
}

// 搜索
let searchTimer: ReturnType<typeof setTimeout> | null = null
function handleSearchChange() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    handleSearch()
  }, 300)
}

function handleSearch() {
  pagination.current = 1
  loadItems()
}

// 分页变化
function handleTableChange(pag: any) {
  pagination.current = pag.current
  loadItems()
}

// 格式化时间
function formatTime(time: string) {
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

// 查看来源单据
function handleViewSource() {
  if (detail.value) {
    emit('view-source', detail.value)
  }
}
</script>

<style scoped>
.detail-descriptions {
  margin-bottom: 16px;
}

.search-box {
  margin-bottom: 12px;
}

.delta-positive {
  color: var(--success-color, #52c41a);
  font-weight: 600;
}

.delta-negative {
  color: var(--error-color, #ff4d4f);
  font-weight: 600;
}
</style>
