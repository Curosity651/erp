<template>
  <div class="platform-dashboard-container">
    <a-spin :spinning="loading" tip="加载中...">
      <!-- 筛选栏 -->
      <PlatformFilterBar
        :quick-time-range="quickTimeRange"
        :date-range="dateRange"
        :warehouse-ids="warehouseIds"
        :wms-tenant-ids="wmsTenantIds"
        :last-update-time="lastUpdateTime"
        @update:quick-time-range="handleQuickTimeChange"
        @update:custom-date="handleCustomDateChange"
        @update:warehouse-ids="handleWarehouseChange"
        @update:wms-tenant-ids="handleOperatorChange"
        @refresh="handleRefresh"
      />

      <div class="dashboard-content">
        <!-- A 运营总览 KPI -->
        <div class="section-row">
          <PlatformKpiCards :data="data?.opsOverview" />
        </div>

        <!-- B 仓容利用率 -->
        <a-row :gutter="[24, 24]" class="section-row">
          <a-col :xs="24" :lg="14">
            <CapacityUtilizationCard :data="data?.capacity?.byWarehouse" />
          </a-col>
          <a-col :xs="24" :lg="10">
            <ZoneOccupancyCard :data="data?.capacity?.byZone" />
          </a-col>
        </a-row>

        <!-- C 吞吐趋势 -->
        <a-row :gutter="[24, 24]" class="section-row">
          <a-col :span="24">
            <ThroughputTrendCard :data="data?.throughput" />
          </a-col>
        </a-row>

        <!-- D 货主排名 -->
        <a-row :gutter="[24, 24]" class="section-row">
          <a-col :xs="24" :lg="12">
            <OwnerRankingCard
              title="服务商在库占用 TOP10"
              unit="件"
              color="#1890ff"
              :data="data?.operatorRanking?.byStock"
            />
          </a-col>
          <a-col :xs="24" :lg="12">
            <OwnerRankingCard
              title="服务商吞吐 TOP10"
              unit="单"
              color="#52c41a"
              :data="data?.operatorRanking?.byThroughput"
            />
          </a-col>
        </a-row>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getPlatformDashboardData } from '@/api/platform-dashboard'
import type { PlatformDashboardDataVO } from '@/api/platform-dashboard/types'
import {
  getMoscowToday,
  getMoscowYesterday,
  getMoscowLastNDays,
  getMoscowThisMonth,
  getMoscowLastMonth
} from '../utils/timezone'
import PlatformFilterBar from './components/PlatformFilterBar.vue'
import PlatformKpiCards from './components/PlatformKpiCards.vue'
import CapacityUtilizationCard from './components/CapacityUtilizationCard.vue'
import ZoneOccupancyCard from './components/ZoneOccupancyCard.vue'
import ThroughputTrendCard from './components/ThroughputTrendCard.vue'
import OwnerRankingCard from './components/OwnerRankingCard.vue'

const loading = ref(false)
const data = ref<PlatformDashboardDataVO | null>(null)
const lastUpdateTime = ref('')

// 筛选状态
const quickTimeRange = ref<string>('last7days')
const warehouseIds = ref<number[]>([])
const wmsTenantIds = ref<number[]>([])
const customDateRange = ref<{ start: string; end: string }>({ start: '', end: '' })

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

async function loadData() {
  loading.value = true
  try {
    data.value = await getPlatformDashboardData({
      startDate: dateRange.value.start,
      endDate: dateRange.value.end,
      warehouseIds: warehouseIds.value.length ? warehouseIds.value : undefined,
      wmsTenantIds: wmsTenantIds.value.length ? wmsTenantIds.value : undefined
    })
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  } catch (error: any) {
    message.error(error?.message || '加载平台数据分析失败')
    console.error('加载平台数据分析失败:', error)
  } finally {
    loading.value = false
  }
}

function handleQuickTimeChange(value: string) {
  quickTimeRange.value = value
  loadData()
}

function handleCustomDateChange(start: string, end: string) {
  quickTimeRange.value = 'custom'
  customDateRange.value = { start, end }
  loadData()
}

function handleWarehouseChange(value: number[]) {
  warehouseIds.value = value
  loadData()
}

function handleOperatorChange(value: number[]) {
  wmsTenantIds.value = value
  loadData()
}

function handleRefresh() {
  loadData()
}

onMounted(loadData)
</script>

<script lang="ts">
export default {
  name: 'PlatformDashboardPage'
}
</script>

<style scoped>
.platform-dashboard-container {
  min-height: calc(100vh - 64px);
  max-width: 1800px;
  margin: 0 auto;
}

.dashboard-content {
  margin-top: 24px;
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

@media (max-width: 767px) {
  .platform-dashboard-container {
    padding: 12px;
  }

  .dashboard-content {
    margin-top: 16px;
  }

  .section-row {
    margin-bottom: 16px;
  }
}
</style>
