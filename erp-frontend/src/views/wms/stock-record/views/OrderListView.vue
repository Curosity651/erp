<template>
  <div class="order-list-view">
    <order-list-search
      ref="searchRef"
      :loading="tableRef?.loading"
      :initial-warehouse-id="initialWarehouseId"
      :initial-source-no="initialSourceNo"
      @search="searchTable"
    />

    <pro-table
      ref="tableRef"
      header-title="按操作单"
      row-key="id"
      :request="tableRequest"
      :columns="orderColumns"
      :scroll="{ x: 1000 }"
    >
      <template #toolBarRender>
        <export-confirm-button :on-export="handleExport" />
      </template>

      <template #bodyCell="{ column, record }">
        <!-- 时间 -->
        <template v-if="column.key === 'postTime'">
          {{ formatTime(record.postTime || record.createTime) }}
        </template>

        <!-- 单号 -->
        <template v-else-if="column.key === 'postingNo'">
          <span class="posting-no">{{ record.postingNo }}</span>
        </template>

        <!-- 类型 -->
        <template v-else-if="column.key === 'postingType'">
          <a-tag :color="getPostingTypeColor(record.postingType)">
            {{ record.postingTypeDesc || PostingTypeMap[record.postingType] || record.postingType }}
          </a-tag>
        </template>

        <!-- 仓库/区域 -->
        <template v-else-if="column.key === 'location'">
          <span v-if="record.warehouseId > 0">
            {{ record.warehouseDisplay?.warehouseName || record.warehouseName || '-' }}
          </span>
          <a-tag v-else-if="record.regionId > 0" color="purple" class="region-tag">
            <global-outlined />
            {{ record.regionDisplay?.regionName || '-' }}
          </a-tag>
          <span v-else>-</span>
        </template>

        <!-- 来源单号 -->
        <template v-else-if="column.key === 'sourceNo'">
          <a v-if="record.sourceNo" @click="handleViewSource(record)">
            {{ record.sourceNo }}
          </a>
          <span v-else class="text-tertiary">-</span>
        </template>

        <!-- 操作列 -->
        <template v-else-if="column.key === 'operate'">
          <operation-group>
            <a @click="handleViewDetail(record)">详情</a>
            <a @click="handleViewFlow(record)">流水</a>
          </operation-group>
        </template>
      </template>
    </pro-table>

    <!-- 详情抽屉 -->
    <stock-posting-detail-drawer
      v-model:open="drawerVisible"
      :posting-id="currentPostingId"
      @view-source="handleViewSource"
    />
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
import { pageStockPosting } from '@/api/wms/stock-posting'
import type { StockPostingPageVO } from '@/api/wms/stock-posting/types'
import { ExportConfirmButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import OrderListSearch from '../components/OrderListSearch.vue'
import StockPostingDetailDrawer from '../components/StockPostingDetailDrawer.vue'
import {
  PostingTypeMap,
  getPostingTypeColor
} from '../../shared/constants'

defineOptions({ name: 'OrderListView' })

const props = defineProps<{
  initialWarehouseId?: number
  initialSourceNo?: string
}>()

const emit = defineEmits<{
  (e: 'view-source', record: StockPostingPageVO): void
}>()

const router = useRouter()

// 详情抽屉
const drawerVisible = ref(false)
const currentPostingId = ref<number>()

const tableRef = ref<ProTableInstanceExpose>()
const searchRef = ref<InstanceType<typeof OrderListSearch>>()

let searchParams: Record<string, any> = {}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageStockPosting({
    ...pageParam,
    ...searchParams
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

function handleViewSource(record: StockPostingPageVO) {
  emit('view-source', record)
}

// 查看详情
function handleViewDetail(record: StockPostingPageVO) {
  currentPostingId.value = record.id
  drawerVisible.value = true
}

// 查看流水
function handleViewFlow(record: StockPostingPageVO) {
  // 通过 URL 参数切换到流水 Tab 并过滤
  router.replace({
    query: {
      view: 'flow',
      postingNo: record.postingNo
    }
  })
}

async function handleExport() {
  // TODO: 实现导出逻辑
  console.log('导出操作单数据', searchParams)
}

const orderColumns: ProColumns[] = [
  { title: '时间', key: 'postTime', width: 120 },
  { title: '单号', key: 'postingNo', width: 160 },
  { title: '类型', key: 'postingType', width: 100 },
  { title: '仓库/区域', key: 'location', width: 100 },
  { title: 'SKU 数', dataIndex: 'skuCount', width: 80, align: 'center' },
  { title: '总数量', dataIndex: 'totalQuantity', width: 80, align: 'right' },
  { title: '来源单号', key: 'sourceNo', width: 140 },
  { title: '操作', key: 'operate', width: 120, fixed: 'right' }
]

defineExpose({
  reload: reloadTable
})
</script>

<style scoped>
.order-list-view .posting-no {
  font-weight: 600;
  color: var(--text-primary, #262626);
}

.order-list-view .text-tertiary {
  color: var(--text-tertiary, #999);
}

.order-list-view .region-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>
