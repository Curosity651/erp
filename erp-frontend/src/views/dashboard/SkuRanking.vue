<template>
  <!-- 搜索区域 -->
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="filters" :label-col="labelCol">
      <a-row :gutter="16">
        <a-col :xl="8" :lg="8" :md="8" :sm="12">
          <a-form-item label="平台">
            <PlatformSelect v-model:value="filters.platform" allow-clear style="width: 100%" />
          </a-form-item>
        </a-col>

        <a-col :xl="8" :lg="8" :md="8" :sm="12">
          <a-form-item label="店铺">
            <ShopSelectInput v-model="filters.shopIds" :multiple="true" placeholder="请选择店铺" />
          </a-form-item>
        </a-col>

        <a-col :xl="8" :lg="8" :md="8" :sm="12">
          <a-form-item label="品类">
            <CategoryTreeSelect v-model:value="filters.categoryId" placeholder="请选择品类" />
          </a-form-item>
        </a-col>

        <a-col :xl="8" :lg="8" :md="8" :sm="12">
          <a-form-item label="SKU">
            <SkuSelectInput
              :model-value="filters.skuCodes"
              :multiple="true"
              placeholder="请选择SKU"
              @update:model-value="handleSkuCodesChange"
            />
          </a-form-item>
        </a-col>

        <a-col :xl="8" :lg="12" :md="12" :sm="24">
          <a-form-item label="时间范围">
            <a-range-picker
              v-model:value="dateRangeValue"
              :allow-clear="false"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>

        <a-col :xl="8" :lg="8" :md="8" :sm="12">
          <search-actions
            :loading="tableRef?.loading"
            @search="handleSearch"
            @reset="handleReset"
          />
        </a-col>
      </a-row>
    </a-form>
  </a-card>

  <!-- 数据表格 -->
  <pro-table
    ref="tableRef"
    header-title="SKU销量排名"
    row-key="sku"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 800 }"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <a-button type="primary" :loading="exporting" @click="handleExport">
        <template #icon>
          <DownloadOutlined />
        </template>
        导出Excel
      </a-button>
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'rank'">
        <div class="rank-cell">
          <span v-if="record.rank === 1" class="rank-badge">🥇</span>
          <span v-else-if="record.rank === 2" class="rank-badge">🥈</span>
          <span v-else-if="record.rank === 3" class="rank-badge">🥉</span>
          <span v-else class="rank-number">{{ record.rank }}</span>
        </div>
      </template>

      <template v-if="column.key === 'product'">
        <div class="product-cell">
          <a-image
            v-if="record.imageUrl"
            :src="record.imageUrl"
            :width="40"
            :height="40"
            class="product-image"
          />
          <div class="product-info">
            <div class="sku-code">
              {{ record.sku }}
              <a-tag v-if="!record.isMapped" color="orange" size="small">未映射</a-tag>
            </div>
            <div v-if="record.skuNameCn" class="product-name">{{ record.skuNameCn }}</div>
          </div>
        </div>
      </template>

      <template v-if="column.key === 'category'">
        <span>{{ record.categoryName || '-' }}</span>
      </template>

      <template v-if="column.key === 'quantity'">
        <span class="quantity-value">{{ formatNumber(record.quantity) }}</span>
      </template>

      <template v-if="column.key === 'amount'">
        <span class="amount-value">₽{{ formatAmount(record.amount) }}</span>
      </template>
    </template>
  </pro-table>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import dayjs, { Dayjs } from 'dayjs'
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import type { ProTableInstanceExpose } from '#/table/Table'
import type { TableRequest } from '#/table/typing'
import { mergePageParam } from '@/utils/page-utils'
import { PlatformSelect } from '@/components/Platform'
import ShopSelectInput from '@/components/ShopSelectInput.vue'
import CategoryTreeSelect from '@/components/Lov/CategoryTreeSelect.vue'
import SkuSelectInput from '@/components/Sku/SkuSelectInput.vue'
import { getSkuRankingPage, exportSkuRanking } from '@/api/dashboard'
import type { SkuRankingItemVO, SkuRankingQueryParams } from '@/api/dashboard/types'
import { useDashboardFilterStore } from '@/stores/dashboard-filter-store'
import { remoteFileDownload } from '@/utils/file-utils'

// 如果需要被多页签缓存，必须要设置组件名称
defineOptions({ name: 'SkuRankingPage' })

const route = useRoute()

// 表单 label 全局配置
const labelCol = { md: { span: 6 } }

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()

// 状态
const exporting = ref(false)

// Dashboard 筛选条件 Store
const dashboardFilterStore = useDashboardFilterStore()

// 筛选条件
const filters = reactive<SkuRankingQueryParams>({
  startDate: '',
  endDate: '',
  platform: undefined,
  shopIds: [],
  categoryId: undefined,
  skuCodes: []
})

// 日期选择器绑定值
const dateRangeValue = computed<[Dayjs, Dayjs]>({
  get: () => [dayjs(filters.startDate), dayjs(filters.endDate)] as [Dayjs, Dayjs],
  set: (val: [Dayjs, Dayjs]) => {
    if (val && val.length === 2) {
      filters.startDate = val[0].format('YYYY-MM-DD')
      filters.endDate = val[1].format('YYYY-MM-DD')
    }
  }
})

// 表格列定义
const columns: ProColumns<SkuRankingItemVO>[] = [
  { title: '排名', key: 'rank', dataIndex: 'rank', width: 80, align: 'center' },
  { title: '产品信息', key: 'product', dataIndex: 'sku', width: 300 },
  { title: '品类', key: 'category', dataIndex: 'categoryName', width: 150 },
  {
    title: '销量',
    key: 'quantity',
    dataIndex: 'quantity',
    width: 100,
    align: 'right',
    sorter: true
  },
  {
    title: '销售额',
    key: 'amount',
    dataIndex: 'amount',
    width: 150,
    align: 'right',
    sorter: true
  }
]

// 初始化
onMounted(() => {
  // 从路由参数初始化筛选条件
  initFiltersFromRoute()
})

// 初始化筛选条件（优先从 store 读取，其次从路由参数）
function initFiltersFromRoute() {
  // 先尝试从 store 获取筛选条件（从 Dashboard 跳转过来）
  const sharedFilters = dashboardFilterStore.consumeFilters()

  if (sharedFilters) {
    // 使用 store 中的筛选条件
    filters.startDate = sharedFilters.startDate
    filters.endDate = sharedFilters.endDate
    filters.platform = sharedFilters.platform || undefined
    filters.shopIds = sharedFilters.shopIds || []
    filters.categoryId = sharedFilters.categoryId
    filters.skuCodes = sharedFilters.skuCodes || []
  } else {
    // 否则从路由参数读取（兑容旧的跳转方式）
    const { startDate, endDate, platform } = route.query

    if (startDate && endDate) {
      filters.startDate = startDate as string
      filters.endDate = endDate as string
    } else {
      // 默认最近7天
      filters.startDate = dayjs().subtract(6, 'day').format('YYYY-MM-DD')
      filters.endDate = dayjs().format('YYYY-MM-DD')
    }

    if (platform) {
      filters.platform = platform as string
    }
  }
}

// 处理 SKU 编码变化
function handleSkuCodesChange(value: string | string[] | undefined) {
  filters.skuCodes = Array.isArray(value) ? value : value ? [value] : []
}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return getSkuRankingPage({ ...pageParam }, { ...filters })
}

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

/* 搜索 */
const handleSearch = () => {
  reloadTable(true)
}

/* 重置 */
const handleReset = () => {
  filters.startDate = dayjs().subtract(6, 'day').format('YYYY-MM-DD')
  filters.endDate = dayjs().format('YYYY-MM-DD')
  filters.platform = undefined
  filters.shopIds = []
  filters.categoryId = undefined
  filters.skuCodes = []
  reloadTable(true)
}

// 导出
async function handleExport() {
  exporting.value = true
  try {
    const hide = message.loading('正在导出SKU排行...', 0)
    const response = await exportSkuRanking(filters)
    hide()
    remoteFileDownload(response, `SKU排行导出_${Date.now()}.xlsx`)
    message.success('导出成功')
  } catch (error: any) {
    message.error('导出失败：' + (error.message || '未知错误'))
  } finally {
    exporting.value = false
  }
}

// 格式化数字
function formatNumber(num: number) {
  return num?.toLocaleString('zh-CN') || '0'
}

// 格式化金额
function formatAmount(amount: number) {
  return (
    amount?.toLocaleString('zh-CN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }) || '0.00'
  )
}
</script>

<style scoped>
.rank-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-badge {
  font-size: 20px;
}

.rank-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--ant-color-fill-quaternary);
  color: var(--ant-color-text-secondary);
  font-weight: 600;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-image {
  border-radius: 4px;
  flex-shrink: 0;
}

.product-info {
  flex: 1;
  min-width: 0;
}

.sku-code {
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.product-name {
  font-size: 12px;
  color: var(--ant-color-text-tertiary);
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quantity-value,
.amount-value {
  font-weight: 600;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
}

.amount-value {
  color: var(--ant-color-primary);
}
</style>
