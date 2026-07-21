<template>
  <div class="stock-record-page">
    <!-- 今日统计卡片 -->
    <today-summary-cards :summary="todaySummary" :loading="summaryLoading" />

    <!-- 主内容区 -->
    <a-card :bordered="false" class="main-content">
      <a-tabs v-model:active-key="activeView" @change="handleViewChange">
        <!-- 视图切换 -->
        <a-tab-pane key="flow">
          <template #tab>
            <span>
              <bars-outlined />
              流水明细
            </span>
          </template>
          <flow-list-view
            ref="flowViewRef"
            :initial-warehouse-id="initialParams.warehouseId"
            :initial-sku-code="initialParams.skuCode"
            :initial-posting-no="initialParams.postingNo"
            @view-source="handleViewSource"
          />
        </a-tab-pane>

        <a-tab-pane key="order">
          <template #tab>
            <span>
              <file-text-outlined />
              按操作单
            </span>
          </template>
          <order-list-view
            ref="orderViewRef"
            :initial-warehouse-id="initialParams.warehouseId"
            :initial-source-no="initialParams.sourceNo"
            @view-source="handleViewSource"
          />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BarsOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { getStockFlowTodaySummary } from '@/api/wms/stock-flow'
import type { StockFlowTodaySummaryVO } from '@/api/wms/stock-flow/types'
import TodaySummaryCards from './components/TodaySummaryCards.vue'
import FlowListView from './views/FlowListView.vue'
import OrderListView from './views/OrderListView.vue'

defineOptions({ name: 'StockRecordPage' })

type ViewType = 'flow' | 'order'

const route = useRoute()
const router = useRouter()

// 今日统计
const todaySummary = ref<StockFlowTodaySummaryVO | null>(null)
const summaryLoading = ref(false)

// 视图切换
const activeView = ref<ViewType>((route.query.view as ViewType) || 'flow')

// 子视图引用
const flowViewRef = ref<InstanceType<typeof FlowListView>>()
const orderViewRef = ref<InstanceType<typeof OrderListView>>()

// 初始参数（从其他页面跳转时带入）
const initialParams = reactive({
  warehouseId: route.query.warehouseId ? Number(route.query.warehouseId) : undefined,
  skuCode: route.query.skuCode as string | undefined,
  sourceNo: route.query.sourceNo as string | undefined,
  postingNo: route.query.postingNo as string | undefined
})

// 加载今日统计
async function loadTodaySummary() {
  summaryLoading.value = true
  try {
    const result = await getStockFlowTodaySummary()
    if (isSuccess(result) && result.data) {
      todaySummary.value = result.data
    }
  } catch (e) {
    console.error('加载今日统计失败', e)
  } finally {
    summaryLoading.value = false
  }
}

// 视图切换
function handleViewChange(key: string) {
  router.replace({ query: { ...route.query, view: key } })
}

// 查看来源单据
function handleViewSource(record: any) {
  // 根据来源类型跳转
  const { sourceType, sourceNo, sourceId } = record
  if (!sourceNo) return

  const routeMap: Record<string, string> = {
    PURCHASE_INBOUND: '/wms/purchase-inbound',
    SALES_OUTBOUND: '/wms/sales-outbound',
    RETURN_INBOUND: '/wms/return-inbound',
    // 盘点/调拨/库存调整 已迁至「海外仓作业」(/ops) 下
    TRANSFER: '/ops/transfer-order',
    ADJUSTMENT: '/ops/adjustment',
    STOCKTAKE: '/ops/stocktake'
  }

  const path = routeMap[sourceType]
  if (path) {
    router.push({ path, query: { id: sourceId } })
  } else {
    message.info(`来源单号: ${sourceNo}`)
  }
}

// 监听 URL 参数变化
watch(
  () => route.query,
  query => {
    // 只处理当前页面的路由，避免 keep-alive 时污染其他路由
    if (route.path !== '/inventory/stock-record') return

    if (!query.view) {
      activeView.value = 'flow' // 同步更新 Tab 选中状态
      router.replace({ query: { ...query, view: 'flow' } })
      return
    }
    if (query.view !== activeView.value) {
      activeView.value = query.view as ViewType
    }
    if (query.warehouseId) {
      initialParams.warehouseId = Number(query.warehouseId)
    }
    if (query.skuCode) {
      initialParams.skuCode = query.skuCode as string
    }
    if (query.sourceNo) {
      initialParams.sourceNo = query.sourceNo as string
    }
    if (query.postingNo) {
      initialParams.postingNo = query.postingNo as string
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadTodaySummary()
})
</script>

<style scoped>
.stock-record-page .main-content {
  margin-top: 16px;
}
</style>
