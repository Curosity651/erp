<template>
  <div>
    <a-alert
      v-if="summary?.stale"
      class="mb-3"
      type="warning"
      show-icon
      message="FBO 库存快照已超过 24 小时，请同步后再用于资产核对"
    />
    <div class="wms-table-toolbar">
      <a-space>
        <a-input-search
          v-model:value="skuCode"
          placeholder="搜索 SKU"
          allow-clear
          style="width: 220px"
          @search="search"
        />
        <a-input-search
          v-model:value="warehouseName"
          placeholder="搜索平台仓库"
          allow-clear
          style="width: 220px"
          @search="search"
        />
      </a-space>
      <a-space>
        <span class="summary">{{ summary?.skuCount ?? 0 }} 个 SKU · {{ summary?.totalQuantity ?? 0 }} 件</span>
        <a-button :loading="loading" @click="reload"><reload-outlined />刷新</a-button>
      </a-space>
    </div>
    <a-table
      row-key="id"
      size="middle"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      @change="changePage"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'sku'"><SkuBriefCell :brief="record.skuBrief" /></template>
        <template v-else-if="column.key === 'source'"><a-tag color="blue">FBO</a-tag></template>
        <template v-else-if="column.key === 'quantity'">
          <span class="quantity">{{ record.quantity?.toLocaleString() ?? 0 }}</span>
        </template>
        <template v-else-if="column.key === 'syncedAt'">
          <a-tooltip :title="record.stale ? '数据已超过 24 小时' : '最新快照'">
            <span :class="{ stale: record.stale }">{{ record.syncedAt || '--' }}</span>
          </a-tooltip>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { isSuccess } from '@/api'
import { getFboInventoryPage, getFboInventorySummary } from '@/api/wms/fbo'
import type { FboInventoryPageVO, FboInventorySummaryVO } from '@/api/wms/fbo/types'
import { SkuBriefCell } from '@/components/Sku'

const rows = ref<FboInventoryPageVO[]>([])
const summary = ref<FboInventorySummaryVO>()
const loading = ref(false)
const skuCode = ref('')
const warehouseName = ref('')
const pagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const columns: TableColumnsType = [
  { title: '来源', key: 'source', width: 80 },
  { title: 'SKU/商品名称', key: 'sku', width: 260 },
  { title: '平台', dataIndex: 'platform', width: 100 },
  { title: '店铺', dataIndex: 'shopName', width: 160 },
  { title: 'FBO 仓库', dataIndex: 'platformWarehouseName', width: 200 },
  { title: '数量', key: 'quantity', width: 110, align: 'right' },
  { title: '同步时间', key: 'syncedAt', width: 180 }
]

async function reload() {
  loading.value = true
  try {
    const [pageResult, summaryResult] = await Promise.all([
      getFboInventoryPage({ current: pagination.current, size: pagination.pageSize, skuCode: skuCode.value || undefined, platformWarehouseName: warehouseName.value || undefined }),
      getFboInventorySummary()
    ])
    if (isSuccess(pageResult)) {
      rows.value = pageResult.data.records
      pagination.total = pageResult.data.total
    }
    if (isSuccess(summaryResult)) summary.value = summaryResult.data
  } finally {
    loading.value = false
  }
}
function search() { pagination.current = 1; reload() }
function changePage(page: TablePaginationConfig) { pagination.current = page.current; pagination.pageSize = page.pageSize; reload() }
onMounted(reload)
defineExpose({ reload })
</script>

<style scoped>
.wms-table-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.summary { color: var(--ant-color-text-secondary); }
.quantity { font-weight: 600; font-variant-numeric: tabular-nums; }
.stale { color: #d46b08; }
</style>
