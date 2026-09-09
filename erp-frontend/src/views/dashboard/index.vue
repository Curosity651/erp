<template>
  <div class="dashboard-container">
    <a-spin :spinning="loading" :tip="t('dashboard.loading')">
      <!-- 筛选栏 -->
      <FilterBar
        :quick-time-range="quickTimeRange"
        :date-range="dateRange"
        :platform="platform"
        :sku-codes="skuCodes"
        :shop-ids="shopIds"
        :category-id="categoryId"
        :last-update-time="lastUpdateTime"
        @update:quick-time-range="handleQuickTimeChange"
        @update:custom-date="handleCustomDateChange"
        @update:platform="handlePlatformChange"
        @update:sku-codes="handleSkuCodesChange"
        @update:shop-ids="handleShopIdsChange"
        @update:category-id="handleCategoryIdChange"
        @refresh="handleRefresh"
      />

      <!-- Dashboard内容区域 -->
      <div class="dashboard-content">
        <!-- KPI卡片行 - 3个核心指标 -->
        <a-row :gutter="[24, 24]" class="overview-row">
          <a-col :xs="24" :lg="8">
            <SalesOverviewCard
              :data="dashboardData?.salesOverview"
              :server-time="serverTime"
              :quick-time-range="quickTimeRange"
              :date-range="{ start: dateRange.start, end: dateRange.end }"
            />
          </a-col>
          <a-col :xs="24" :lg="8">
            <TargetProgressCard :data="dashboardData?.targetProgress?.monthly" type="monthly" />
          </a-col>
          <a-col :xs="24" :lg="8">
            <TargetProgressCard :data="dashboardData?.targetProgress?.yearly" type="yearly" />
          </a-col>
        </a-row>

        <!-- 汇率信息行 -->
        <a-row :gutter="[24, 24]" class="section-row">
          <a-col :span="24">
            <ExchangeRateCard :data="dashboardData?.exchangeRates" />
          </a-col>
        </a-row>

        <!-- 销售趋势图（优化高度） -->
        <a-row :gutter="[24, 24]" class="section-row">
          <a-col :span="24">
            <SalesTrendCard :data="dashboardData?.salesTrend" />
          </a-col>
        </a-row>

        <!-- 平台履约分布 + 订单状态分布 -->
        <a-row :gutter="[24, 24]" class="section-row">
          <a-col :xs="24" :lg="12">
            <PlatformFulfillmentCard :data="dashboardData?.platformFulfillment" />
          </a-col>
          <a-col :xs="24" :lg="12">
            <OrderStatusCard :data="dashboardData?.orderStatus" />
          </a-col>
        </a-row>

        <!-- SKU排名 -->
        <a-row :gutter="[24, 24]" class="section-row">
          <a-col :xs="24" :lg="12">
            <SkuRankingCard :data="dashboardData?.skuRanking?.top5" type="top" :filters="filters" />
          </a-col>
          <a-col :xs="24" :lg="12">
            <SkuRankingCard
              :data="dashboardData?.skuRanking?.bottom5"
              type="bottom"
              :filters="filters"
            />
          </a-col>
        </a-row>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { getDashboardData } from '@/api/dashboard'
import type { DashboardDataVO, DashboardFilters } from '@/api/dashboard/types'
import {
  getMoscowToday,
  getMoscowYesterday,
  getMoscowLastNDays,
  getMoscowThisMonth,
  getMoscowLastMonth
} from './utils/timezone'
import FilterBar from './components/FilterBar.vue'
import SalesOverviewCard from './components/SalesOverviewCard.vue'
import TargetProgressCard from './components/TargetProgressCard.vue'
import ExchangeRateCard from './components/ExchangeRateCard.vue'
import PlatformFulfillmentCard from './components/PlatformFulfillmentCard.vue'
import SalesTrendCard from './components/SalesTrendCard.vue'
import OrderStatusCard from './components/OrderStatusCard.vue'
import SkuRankingCard from './components/SkuRankingCard.vue'

// 状态
const loading = ref(false)
const dashboardData = ref<DashboardDataVO | null>(null)
const { t, locale } = useI18n()
const lastUpdateTime = ref('')
const serverTime = ref<string>('')

// 快捷时间范围 - 唯一数据源
const quickTimeRange = ref<string>('today')

// 平台筛选
const platform = ref<string>()

// SKU 编码列表（多选）
const skuCodes = ref<string[]>([])

// 店铺 ID 列表（多选）
const shopIds = ref<number[]>([])

// 品类 ID
const categoryId = ref<number | undefined>(undefined)

// 自定义日期范围（仅在 custom 模式下使用）
const customDateRange = ref<{ start: string; end: string }>({
  start: '',
  end: ''
})

// 从 quickTimeRange 派生计算日期范围
const dateRange = computed(() => {
  switch (quickTimeRange.value) {
    case 'today':
      return getMoscowToday()
    case 'yesterday':
      return getMoscowYesterday()
    case 'last7days':
      return getMoscowLastNDays(7)
    case 'thisMonth':
      return getMoscowThisMonth()
    case 'lastMonth':
      return getMoscowLastMonth()
    case 'custom':
      return customDateRange.value
    default:
      return getMoscowLastNDays(7)
  }
})

// 从 dateRange 和 platform 派生筛选条件
const filters = computed<DashboardFilters>(() => ({
  startDate: dateRange.value.start,
  endDate: dateRange.value.end,
  platform: platform.value,
  skuCodes: skuCodes.value.length > 0 ? skuCodes.value : undefined,
  shopIds: shopIds.value.length > 0 ? shopIds.value : undefined,
  categoryId: categoryId.value
}))

// 加载Dashboard数据
async function loadDashboardData() {
  loading.value = true
  try {
    const response = await getDashboardData({
      startDate: filters.value.startDate,
      endDate: filters.value.endDate,
      platform: filters.value.platform,
      skuCodes: filters.value.skuCodes,
      shopIds: filters.value.shopIds,
      categoryId: filters.value.categoryId
    })

    // response 是完整的 AxiosResponse 对象
    // response.data 是 ApiResult<DashboardDataVO>
    if (response.data?.data) {
      dashboardData.value = response.data.data
      lastUpdateTime.value = new Date().toLocaleString(locale.value)

      // 从响应头中提取服务器时间
      const dateHeader = response.headers?.['date'] || response.headers?.['Date']
      if (dateHeader) {
        serverTime.value = dateHeader
      }
    }
  } catch (error: any) {
    message.error(error.message || t('dashboard.loadFailed'))
    console.error(t('dashboard.loadFailed'), error)
  } finally {
    loading.value = false
  }
}

// 处理快捷时间范围变化
function handleQuickTimeChange(value: string) {
  quickTimeRange.value = value
  loadDashboardData()
}

// 处理自定义日期变化
function handleCustomDateChange(start: string, end: string) {
  quickTimeRange.value = 'custom'
  customDateRange.value = { start, end }
  loadDashboardData()
}

// 处理平台变化
function handlePlatformChange(value: string) {
  platform.value = value
  loadDashboardData()
}

// 处理 SKU 编码变化
function handleSkuCodesChange(value: string[]) {
  skuCodes.value = value
  loadDashboardData()
}

// 处理店铺 ID 变化
function handleShopIdsChange(value: number[]) {
  shopIds.value = value
  loadDashboardData()
}

// 处理品类变化
function handleCategoryIdChange(value: number | undefined) {
  categoryId.value = value
  loadDashboardData()
}

// 手动刷新
function handleRefresh() {
  loadDashboardData()
}

// 页面加载时初始化
onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.dashboard-container {
  min-height: calc(100vh - 64px);
  max-width: 1800px;
  margin: 0 auto;
}

.dashboard-content {
  margin-top: 24px;
}

.overview-row {
  margin-bottom: 24px;
}

.overview-row :deep(.ant-col) {
  display: flex;
}

.overview-row :deep(.ant-col > *) {
  width: 100%;
  min-height: 200px;
}

.section-row {
  margin-bottom: 24px;
}

.section-row :deep(.ant-col) {
  display: flex;
}

.section-row :deep(.ant-col > *) {
  width: 100%;
  height: 100%;
}

.section-row:last-child {
  margin-bottom: 0;
}

/* 响应式布局 */
@media (max-width: 767px) {
  .dashboard-container {
    padding: 12px;
  }

  .dashboard-content {
    margin-top: 16px;
  }

  .overview-row,
  .section-row {
    margin-bottom: 16px;
  }
}
</style>
