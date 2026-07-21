<template>
  <a-modal
    v-model:open="open"
    title="选择待出库订单"
    width="1000px"
    :footer="null"
    :destroy-on-close="true"
  >
    <!-- 顶部操作栏 -->
    <div class="top-bar">
      <div class="platform-info">
        <PlatformTag :platform="platform" />
        <span class="warehouse-name">{{ warehouseName }}</span>
      </div>
      <div class="search-area">
        <a-input
          v-model:value="searchForm.keyword"
          placeholder="搜索平台订单号"
          style="width: 180px"
          allow-clear
          @pressEnter="handleSearch"
        />
        <SkuSelectInput
          v-model="searchForm.skuCodes"
          :multiple="true"
          placeholder="选择SKU"
          style="width: 300px"
        />
        <a-button type="primary" :loading="tableRef?.loading" @click="handleSearch">
          查询
        </a-button>
        <a-button @click="handleReset">重置</a-button>
      </div>
    </div>

    <!-- 已选池 -->
    <SelectionPool
      :items="selectedList"
      row-key="id"
      :label-key="(item: PendingOrderVO) => item.platformOrderId"
      placeholder="请在下方列表中选择订单"
      :max-display="8"
      @remove="deselect"
      @clear="clear"
    />

    <!-- 订单列表 -->
    <pro-table
      ref="tableRef"
      row-key="id"
      :request="tableRequest"
      :columns="columns"
      :tool-bar-render="false"
      :row-selection="rowSelection"
      :table-alert-render="false"
      :card-props="{ bodyStyle: { padding: 0 } }"
      :scroll="{ y: 300 }"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <!-- 订单信息 -->
        <template v-if="column.key === 'orderInfo'">
          <div class="order-info-cell">
            <div class="platform-order-id">{{ record.platformOrderId }}</div>
            <div class="order-meta">
              <span class="shop-name">{{ record.shopName }}</span>
            </div>
          </div>
        </template>

        <!-- SKU信息 -->
        <template v-else-if="column.key === 'skuInfo'">
          <div v-if="record.items?.length" class="items-stack">
            <div v-for="(item, idx) in record.items" :key="idx" class="item-row">
              <SkuBriefCell :brief="item.skuBrief" />
              <span v-if="item.quantity > 1" class="item-qty">×{{ item.quantity }}</span>
            </div>
          </div>
          <span v-else class="no-sku-info">-</span>
        </template>

        <!-- 数量 -->
        <template v-else-if="column.key === 'quantity'">
          <span class="quantity">{{ record.totalQuantity }}</span>
        </template>

        <!-- 可用库存 -->
        <template v-else-if="column.key === 'stock'">
          <a-tooltip title="当前仓库的可用库存数量">
            <span :class="getStockClass(record)">
              {{ record.availableStock ?? '-' }}
              <CheckCircleOutlined v-if="record.stockStatus === 'sufficient'" />
              <ExclamationCircleOutlined v-else-if="record.stockStatus === 'insufficient'" />
              <CloseCircleOutlined v-else-if="record.stockStatus === 'zero'" />
            </span>
          </a-tooltip>
        </template>
      </template>
    </pro-table>

    <!-- 底部操作 -->
    <div class="modal-footer">
      <div class="selected-info">
        已选择 <span class="count">{{ selectedCount }}</span> 条记录
      </div>
      <a-space>
        <a-button @click="handleCancel">取消</a-button>
        <a-button type="primary" :disabled="isEmpty" @click="handleConfirm">
          确认添加
        </a-button>
      </a-space>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, h } from 'vue'
import {
  InfoCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  CloseCircleOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons-vue'
import { Tooltip } from 'ant-design-vue'
import ProTable from '#/table'
import type { ProTableInstanceExpose, TableRequest } from '#/table'
import { mergePageParam } from '@/utils/page-utils'
import { getPendingOrders } from '@/api/wms/sales-outbound'
import type { PendingOrderVO, PendingOrderQO } from '@/api/wms/sales-outbound/types'
import { PlatformTag, type PlatformType } from '@/components/Platform'
import SkuSelectInput from '@/components/Sku/SkuSelectInput.vue'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'
import SelectionPool from '@/components/SelectionPool/index.vue'
import { useTableSelection } from '@/hooks/useTableSelection'

defineOptions({ name: 'SelectOrderModal' })

const emits = defineEmits<{
  (e: 'confirm', orders: PendingOrderVO[]): void
}>()

// 弹窗状态
const open = ref(false)

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()

// 平台和仓库信息（在 open 时设置，作为固定参数）
const platform = ref<PlatformType>('wildberries')
const warehouseId = ref<number>(0)
const warehouseName = ref<string>('')

// 已添加的订单ID列表（用于过滤）
const existingOrderIds = ref<number[]>([])

// 搜索表单（UI 绑定）
const searchForm = reactive({
  keyword: '',
  skuCodes: [] as string[]
})

// 搜索参数（传递给 tableRequest）
let searchParams: Pick<PendingOrderQO, 'keyword' | 'skuCodes'> = {}

// 使用跨页选择 hook
const {
  selectedList,
  selectedCount,
  isEmpty,
  deselect,
  clear,
  rowSelection
} = useTableSelection<PendingOrderVO>({
  rowKey: 'id',
  disabledKeys: existingOrderIds
})

// 表格列定义
const columns = [
  { title: '订单信息', key: 'orderInfo', width: 200 },
  { title: 'SKU信息', key: 'skuInfo', width: 280 },
  { title: '数量', key: 'quantity', width: 70, align: 'center' as const },
  {
    title: () =>
      h('span', [
        '可用库存 ',
        h(Tooltip, { title: '当前仓库的可用库存数量' }, () =>
          h(QuestionCircleOutlined, { style: { color: '#8c8c8c', fontSize: '12px' } })
        )
      ]),
    key: 'stock',
    width: 110,
    align: 'center' as const
  },
  { title: '订单时间', dataIndex: 'orderTime', width: 160 }
]

/**
 * 刷新表格
 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

/**
 * 表格请求
 */
const tableRequest: TableRequest = async (params, sorter, filter) => {
  if (!platform.value || !warehouseId.value) {
    return { data: { records: [], total: 0 } }
  }

  const pageParam = mergePageParam(params, sorter, filter)
  return getPendingOrders({
    ...pageParam,
    ...searchParams,
    platform: platform.value,
    warehouseId: warehouseId.value
  })
}

/**
 * 获取库存状态样式类
 */
const getStockClass = (record: PendingOrderVO): string => {
  if (record.stockStatus === 'sufficient') return 'stock-sufficient'
  if (record.stockStatus === 'insufficient') return 'stock-insufficient'
  return 'stock-zero'
}

/**
 * 查询
 */
const handleSearch = () => {
  searchParams = {
    keyword: searchForm.keyword || undefined,
    skuCodes: searchForm.skuCodes.length > 0 ? searchForm.skuCodes : undefined
  }
  reloadTable(true)
}

/**
 * 重置
 */
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.skuCodes = []
  searchParams = {}
  reloadTable(true)
}

/**
 * 取消
 */
const handleCancel = () => {
  open.value = false
}

/**
 * 确认
 */
const handleConfirm = () => {
  emits('confirm', selectedList.value)
  open.value = false
}

/**
 * 打开弹窗
 */
const openModal = async (p: PlatformType, wId: number, wName: string, existingIds: number[] = []) => {
  platform.value = p
  warehouseId.value = wId
  warehouseName.value = wName
  existingOrderIds.value = existingIds
  searchForm.keyword = ''
  searchForm.skuCodes = []
  searchParams = {}
  clear()
  open.value = true

  // 等待弹窗渲染后刷新表格
  await nextTick()
  reloadTable(true)
}

defineExpose({ open: openModal })
</script>

<style scoped>
/* ==================== 顶部操作栏 ==================== */
.top-bar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.platform-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.warehouse-name {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.stock-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #8c8c8c;
}

.search-area {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

/* ==================== 订单信息列 ==================== */
.order-info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.platform-order-id {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  color: #262626;
  font-weight: 600;
}

.order-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.shop-name {
  font-size: 12px;
  color: #8c8c8c;
}

/* ==================== 数量和库存 ==================== */
.quantity {
  font-size: 14px;
  color: #1890ff;
  font-weight: 600;
}

.stock-sufficient {
  color: #52c41a;
}

.stock-insufficient {
  color: #faad14;
}

.stock-zero {
  color: #ff4d4f;
}

/* ==================== 底部操作区 ==================== */
.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.selected-info {
  color: #8c8c8c;
}

.selected-info .count {
  color: #1890ff;
  font-weight: 600;
}

/* ==================== 多商品堆叠 ==================== */
.items-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.item-qty {
  font-size: 12px;
  color: #1890ff;
  font-weight: 600;
  flex-shrink: 0;
}

.no-sku-info {
  color: #bfbfbf;
}

/* ==================== 响应式适配 ==================== */
@media (max-width: 900px) {
  .top-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-area {
    width: 100%;
  }

  .search-area > * {
    flex: 1;
    min-width: 120px;
  }

  .search-area > .ant-btn {
    flex: 0 0 auto;
  }
}
</style>
