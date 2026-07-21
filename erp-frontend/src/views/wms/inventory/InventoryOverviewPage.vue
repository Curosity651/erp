<template>
  <div class="inventory-overview-page">
    <!-- 统计面板 -->
    <statistic-panel :summary="summary" :loading="summaryLoading" />

    <!-- Tabs 布局 -->
    <div class="mt-4">
      <a-card :bordered="false" :body-style="{ padding: '12px 24px' }">
        <a-tabs v-model:activeKey="activeTab">
          <template #rightExtra>
            <a-button type="primary" @click="handleSyncFbo">
              <sync-outlined />
              同步FBO库存
            </a-button>
          </template>

          <a-tab-pane key="region">
            <template #tab>
              <span class="tab-label">
                区域维度
                <span class="tab-count">{{ regionData.length }}</span>
              </span>
            </template>
            <region-overview-table
              :data="regionData"
              :loading="regionLoading"
              @view-detail="handleRegionDetail"
            />
          </a-tab-pane>

          <a-tab-pane key="warehouse">
            <template #tab>
              <span class="tab-label">
                仓库维度
                <span class="tab-count">{{ summary?.warehouseCount ?? 0 }}</span>
              </span>
            </template>
            <warehouse-overview-table
              :data="warehouseData"
              :loading="warehouseLoading"
              @view-detail="handleWarehouseDetail"
              @refresh="loadWarehouseData"
            />
          </a-tab-pane>

          <a-tab-pane key="sku">
            <template #tab>
              <span class="tab-label">
                SKU维度
                <span class="tab-count">{{ summary?.totalSkuCount ?? 0 }}</span>
              </span>
            </template>
            <sku-overview-table
              ref="skuTableRef"
              @view-detail="handleSkuDetail"
              @refresh="handleSkuRefresh"
            />
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </div>

    <!-- FBO 同步弹窗和日志抽屉 -->
    <FboStockSyncModal ref="syncModalRef" @success="refreshAll" @view-log="handleViewSyncLog" />
    <FboSyncLogDrawer ref="logDrawerRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { SyncOutlined } from '@ant-design/icons-vue'
import { isSuccess } from '@/api'
import { getInventorySummary, getInventorySummaryByWarehouse, getInventorySummaryByRegion } from '@/api/wms/inventory'
import type { InventorySummaryVO, WarehouseSummaryVO, RegionSummaryVO } from '@/api/wms/inventory/types'
import StatisticPanel from './components/StatisticPanel.vue'
import WarehouseOverviewTable from './components/WarehouseOverviewTable.vue'
import SkuOverviewTable from './components/SkuOverviewTable.vue'
import RegionOverviewTable from './components/RegionOverviewTable.vue'
import FboStockSyncModal from './components/FboStockSyncModal.vue'
import FboSyncLogDrawer from './components/FboSyncLogDrawer.vue'

defineOptions({ name: 'InventoryOverviewPage' })

const route = useRoute()
const router = useRouter()

// 有效的 tab key 列表
const validTabs = ['region', 'warehouse', 'sku'] as const
type TabKey = typeof validTabs[number]

// 从 URL 读取初始 tab，默认 region
const activeTab = ref<TabKey>(
  validTabs.includes(route.query.view as TabKey)
    ? (route.query.view as TabKey)
    : 'region'
)

// 监听 URL 参数变化，同步 tab 状态
watch(
  () => route.query.view,
  (view) => {
    if (view && validTabs.includes(view as TabKey)) {
      activeTab.value = view as TabKey
    }
  }
)

// 汇总数据
const summary = ref<InventorySummaryVO | null>(null)
const summaryLoading = ref(false)

// 区域数据
const regionData = ref<RegionSummaryVO[]>([])
const regionLoading = ref(false)

// 仓库数据
const warehouseData = ref<WarehouseSummaryVO[]>([])
const warehouseLoading = ref(false)

// SKU 表格引用
const skuTableRef = ref<InstanceType<typeof SkuOverviewTable>>()

// FBO 同步相关
const syncModalRef = ref<InstanceType<typeof FboStockSyncModal>>()
const logDrawerRef = ref<InstanceType<typeof FboSyncLogDrawer>>()

function handleSyncFbo() {
  syncModalRef.value?.show()
}

function handleViewSyncLog() {
  logDrawerRef.value?.show()
}

// 加载汇总数据
async function loadSummary() {
  summaryLoading.value = true
  try {
    const result = await getInventorySummary()
    if (isSuccess(result) && result.data) {
      summary.value = result.data
    }
  } catch (e) {
    console.error('加载库存汇总失败', e)
  } finally {
    summaryLoading.value = false
  }
}

// 加载仓库数据
async function loadWarehouseData() {
  warehouseLoading.value = true
  try {
    const result = await getInventorySummaryByWarehouse({})
    if (isSuccess(result) && result.data) {
      warehouseData.value = result.data
    }
  } catch (e) {
    console.error('加载仓库汇总失败', e)
  } finally {
    warehouseLoading.value = false
  }
}

// 加载区域数据
async function loadRegionData() {
  regionLoading.value = true
  try {
    const result = await getInventorySummaryByRegion()
    if (isSuccess(result) && result.data) {
      regionData.value = result.data
    }
  } catch (e) {
    console.error('加载区域汇总失败', e)
  } finally {
    regionLoading.value = false
  }
}

// 刷新 SKU 表格
function handleSkuRefresh() {
  skuTableRef.value?.reload()
}

// 跳转仓库明细
function handleWarehouseDetail(warehouseId: number) {
  router.push({
    path: '/inventory/inventory-detail',
    query: { warehouseId: String(warehouseId) }
  })
}

// 跳转 SKU 明细
function handleSkuDetail(skuCode: string) {
  router.push({
    path: '/inventory/inventory-detail',
    query: { skuCode }
  })
}

// 跳转区域明细
function handleRegionDetail(regionId: number) {
  router.push({
    path: '/inventory/inventory-detail',
    query: { regionId: String(regionId) }
  })
}

// 刷新全部
async function refreshAll() {
  await Promise.all([loadSummary(), loadWarehouseData(), loadRegionData()])
  skuTableRef.value?.reload()
}

onMounted(() => {
  refreshAll()
})

defineExpose({ refreshAll })
</script>

<style scoped>
.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--ant-color-text-secondary);
  background: var(--ant-color-fill-secondary);
  border-radius: 10px;
}
</style>
