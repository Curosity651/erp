<template>
  <div class="sku-overview-table">
    <!-- 搜索栏 -->
    <div class="wms-table-toolbar">
      <a-space :size="12">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索 SKU 编码或商品名称"
          style="width: 280px"
          allow-clear
          @search="handleSearch"
          @press-enter="handleSearch"
        />
        <a-select
          v-model:value="stockStatus"
          placeholder="库存状态"
          allow-clear
          style="width: 120px"
          :options="stockStatusOptions"
          @change="handleSearch"
        />
      </a-space>
      <a-flex :gap="12" align="center">
        <span class="wms-table-toolbar__total">共 {{ total }} 个 SKU</span>
        <a-button :loading="loading" @click="emit('refresh')">
          <reload-outlined :spin="loading" />
          刷新
        </a-button>
      </a-flex>
    </div>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :loading="loading"
      :pagination="pagination"
      row-key="skuCode"
      size="middle"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'skuInfo'">
          <sku-brief-cell
            :brief="record.skuBrief"
            class="clickable"
            @click="handleViewDetail(record)"
          />
        </template>
        <template v-else-if="column.key === 'totalHeldQuantity'">
          <span class="wms-quantity-cell">{{ record.totalHeldQuantity?.toLocaleString() ?? 0 }}</span>
        </template>
        <template v-else-if="column.key === 'fboWarehouseQuantity'">
          <span class="wms-quantity-cell">{{ record.fboWarehouseQuantity?.toLocaleString() ?? 0 }}</span>
        </template>
        <template v-else-if="column.key === 'ownWarehouseQuantity'">
          <span class="wms-quantity-cell">{{ record.ownWarehouseQuantity?.toLocaleString() ?? 0 }}</span>
        </template>
        <template v-else-if="column.key === 'availableQuantity'">
          <span :class="['wms-quantity-cell', 'wms-quantity-cell--available', { 'wms-quantity-cell--negative': record.availableQuantity < 0 }]">
            {{ record.availableQuantity?.toLocaleString() ?? 0 }}
          </span>
        </template>
        <template v-else-if="column.key === 'inTransitQuantity'">
          <span class="wms-quantity-cell wms-quantity-cell--in-transit">
            {{ record.inTransitQuantity?.toLocaleString() ?? 0 }}
          </span>
        </template>
        <template v-else-if="column.key === 'damagedQuantity'">
          <span :class="['wms-quantity-cell', 'wms-quantity-cell--damaged', { 'has-value': record.damagedQuantity > 0 }]">
            {{ record.damagedQuantity?.toLocaleString() ?? 0 }}
          </span>
        </template>
        <template v-else-if="column.key === 'unitVolume'">
          <span class="wms-volume-cell">{{ formatUnitVolume(record.unitVolume) }}</span>
        </template>
        <template v-else-if="column.key === 'totalVolume'">
          <span class="wms-volume-cell">{{ formatVolume(record.totalVolume) }}</span>
        </template>
        <template v-else-if="column.key === 'operate'">
          <a @click="handleViewDetail(record)">明细</a>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { pageInventorySummaryBySku } from '@/api/wms/inventory'
import type { SkuSummaryVO, StockStatus } from '@/api/wms/inventory/types'
import { stockStatusOptions } from '../../shared/constants'
import { SkuBriefCell } from '@/components/Sku'

defineOptions({ name: 'SkuOverviewTable' })

const emit = defineEmits<{
  (e: 'view-detail', skuCode: string): void
  (e: 'refresh'): void
}>()

const data = ref<SkuSummaryVO[]>([])
const loading = ref(false)
const total = ref(0)
const searchKeyword = ref('')
const stockStatus = ref<StockStatus>()

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (t: number) => `共 ${t} 条`
})

const columns: TableColumnsType = [
  { title: 'SKU/商品名称', key: 'skuInfo', width: 260 },
  { title: '仓库数', dataIndex: 'warehouseCount', width: 80, align: 'right' },
  { title: '持有总量', key: 'totalHeldQuantity', width: 100, align: 'right' },
  { title: 'FBO仓内', key: 'fboWarehouseQuantity', width: 100, align: 'right' },
  { title: '自有仓仓内', key: 'ownWarehouseQuantity', width: 110, align: 'right' },
  { title: '可用', key: 'availableQuantity', width: 100, align: 'right' },
  { title: '占用', dataIndex: 'reservedQuantity', width: 100, align: 'right' },
  { title: '在途', key: 'inTransitQuantity', width: 100, align: 'right' },
  { title: '残品', key: 'damagedQuantity', width: 100, align: 'right' },
  { title: '单件 (m³)', key: 'unitVolume', width: 100, align: 'right' },
  { title: '总体积 (m³)', key: 'totalVolume', width: 110, align: 'right' },
  { title: '操作', key: 'operate', width: 80, align: 'center' }
]

function formatUnitVolume(value: number | null | undefined): string {
  if (value === null || value === undefined) return '--'
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatVolume(value: number | null | undefined): string {
  if (value === null || value === undefined) return '--'
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.pageSize,
      keyword: searchKeyword.value || undefined,
      stockStatus: stockStatus.value
    }
    const result = await pageInventorySummaryBySku(params)
    if (isSuccess(result) && result.data) {
      data.value = result.data.records
      total.value = result.data.total
      pagination.total = result.data.total
    }
  } catch (e) {
    console.error('加载 SKU 汇总失败', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

function handleViewDetail(record: SkuSummaryVO) {
  emit('view-detail', record.skuCode)
}

async function reload() {
  await loadData()
}

onMounted(() => {
  loadData()
})

defineExpose({ reload })
</script>

<style scoped>
/* 数量单元格样式 */
.wms-quantity-cell {
  font-variant-numeric: tabular-nums;
}

.wms-quantity-cell--available {
  color: var(--wms-color-available);
}

.wms-quantity-cell--in-transit {
  color: var(--wms-color-in-transit);
}

.wms-quantity-cell--damaged {
  color: var(--ant-color-text-tertiary);
}

.wms-quantity-cell--damaged.has-value {
  color: var(--wms-color-damaged);
}

.wms-quantity-cell--negative {
  color: var(--wms-color-damaged);
}

.wms-volume-cell {
  font-variant-numeric: tabular-nums;
  color: var(--ant-color-text-secondary);
}

/* 表格工具栏 */
.wms-table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.wms-table-toolbar__total {
  font-size: 14px;
  color: var(--ant-color-text-tertiary);
}

.clickable {
  cursor: pointer;
}
</style>
