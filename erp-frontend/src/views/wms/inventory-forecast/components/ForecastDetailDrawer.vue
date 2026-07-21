<template>
  <a-drawer v-model:open="open" title="库存预测详情" :width="720" @close="handleClose">
    <template v-if="loading">
      <a-skeleton active />
    </template>
    <a-flex v-else-if="detail" vertical :gap="32">
      <!-- SKU + 区域头部 -->
      <a-flex align="center" :gap="24" class="detail-header">
        <SkuBriefCell :brief="detail.skuBrief" />
        <a-flex vertical class="region-info">
          <span class="region-label">区域</span>
          <span class="region-name">{{ detail.regionName }}</span>
        </a-flex>
      </a-flex>

      <!-- 当前库存 -->
      <a-flex vertical :gap="12">
        <span class="section-title">当前库存</span>
        <a-descriptions :column="4" bordered size="small">
          <a-descriptions-item label="可售库存">
            {{ detail.currentStock.sellable }}
          </a-descriptions-item>
          <a-descriptions-item label="预占库存">
            {{ detail.currentStock.reserved }}
          </a-descriptions-item>
          <a-descriptions-item label="在途库存">
            {{ detail.currentStock.inTransit }}
          </a-descriptions-item>
          <a-descriptions-item label="待发货">
            <span class="pending-shipment-value">{{ detail.currentStock.pendingShipment }}</span>
            <span class="pending-shipment-hint">(预计)</span>
          </a-descriptions-item>
          <a-descriptions-item label="日均销量">{{ detail.dailySales }}</a-descriptions-item>
          <a-descriptions-item label="可售天数">
            <a-tag :color="getStatusTagColor(detail.status)">
              {{
                formatSellableDays(
                  detail.sellableDays,
                  detail.status
                )
              }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="有效安全库存">
            <span style="color: var(--ant-color-error)">
              {{ detail.effectiveSafetyStock ?? '-' }}
            </span>
            <a-tooltip v-if="detail.effectiveSafetyStock && detail.safetyStock">
              <template #title>
                = max(最低{{ detail.safetyStock }}, 日销{{ detail.dailySales }} × {{ detail.thresholdDays }}天)
              </template>
              <span class="ml-1 cursor-help" style="color: var(--ant-color-text-tertiary)">ⓘ</span>
            </a-tooltip>
          </a-descriptions-item>
        </a-descriptions>
      </a-flex>

      <!-- 预测区域 -->
      <a-flex vertical :gap="12">
        <a-flex justify="space-between" align="center">
          <span class="section-title">未来 {{ detail.forecastList.length }} 天预测</span>
          <a-segmented v-model:value="viewMode" :options="viewOptions" size="small" />
        </a-flex>

        <!-- 模拟日均销量 -->
        <a-flex justify="space-between" align="center" class="simulation-section">
          <a-space>
            <span>模拟日均销量:</span>
            <a-input-number v-model:value="simulatedDailySales" :min="0" style="width: 120px" />
            <a-button type="primary" size="small" @click="handleSimulate">重新计算</a-button>
            <a-button size="small" @click="resetSimulation">重置</a-button>
          </a-space>
          <span class="safety-threshold">
            安全水位 {{ detail.effectiveSafetyStock ?? detail.safetyStock }}
            <EditOutlined class="edit-icon" @click="safetyStockModalOpen = true" />
          </span>
        </a-flex>

        <!-- 图表视图 -->
        <ForecastChart
          v-if="viewMode === 'chart'"
          :forecast-list="detail.forecastList"
          :safety-stock="detail.safetyStock"
          :effective-safety-stock="detail.effectiveSafetyStock"
          :daily-sales="detail.dailySales"
          :pending-shipment-total="detail.pendingShipmentTotal"
        />

        <!-- 表格视图 -->
        <a-flex v-else vertical :gap="12" class="forecast-table-wrapper">
          <a-table
            :columns="forecastColumns"
            :data-source="detail.forecastList"
            :pagination="false"
            :scroll="{ y: 360 }"
            size="small"
            row-key="date"
            :row-class-name="(record, index) => getRowClassName(record, index)"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'date'">
                {{ formatDateChinese(record.date) }}
              </template>
              <template v-else-if="column.key === 'incoming'">
                <div class="incoming-cell">
                  <span v-if="record.incoming > 0" class="incoming-value">
                    +{{ record.incoming }}
                  </span>
                  <span v-else class="no-incoming">—</span>
                  <div
                    v-for="d in record.incomingDetails"
                    :key="d.sourceNo"
                    :class="[
                      'incoming-detail',
                      d.type === 'PENDING_SHIPMENT' ? 'incoming-detail--pending' : ''
                    ]"
                  >
                    └ {{ d.type === 'PENDING_SHIPMENT' ? '待发货' : '在途' }} {{ d.sourceNo }}
                    <template v-if="d.status">（{{ d.status }}）</template>
                  </div>
                </div>
              </template>
              <template v-else-if="column.key === 'sales'">
                <span class="sales-value">{{ record.sales }}</span>
              </template>
              <template v-else-if="column.key === 'closingStock'">
                <span :class="getStockClassByStatus(record.status)">
                  {{ record.closingStock }}
                </span>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="getStatusTagColor(record.status)" size="small">
                  <component :is="getStatusIcon(record.status)" :size="12" class="status-icon" />
                  {{ getStatusLabel(record.status) }}
                </a-tag>
              </template>
            </template>
          </a-table>

          <!-- 底部汇总 -->
          <a-flex align="center" :gap="24" class="forecast-summary">
            <span>汇总</span>
            <span class="summary-item">
              入库合计: <span class="incoming-value">+{{ totalIncoming }}</span>
            </span>
            <span class="summary-item">
              预计断货日:
              <span v-if="firstStockoutDate" class="stockout-date">
                {{ formatDateChinese(firstStockoutDate) }}
              </span>
              <span v-else class="no-stockout">—</span>
            </span>
          </a-flex>
        </a-flex>
      </a-flex>
    </a-flex>

    <!-- 安全库存编辑弹窗 -->
    <SafetyStockEditModal
      v-if="detail && props.regionId"
      v-model:open="safetyStockModalOpen"
      :region-id="props.regionId"
      :region-name="detail.regionName"
      :sku-brief="detail.skuBrief"
      :current-safety-stock="detail.safetyStock"
      :current-notify-enabled="detail.notifyEnabled"
      :current-notify-threshold-days="detail.notifyThresholdDays"
      @success="loadDetail()"
    />
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { EditOutlined } from '@ant-design/icons-vue'
import {
  ShieldCheckIcon,
  TrendingDownIcon,
  AlertCircleIcon,
  PackageXIcon,
  BarChart2Icon
} from '@/components/Icon'
import { isSuccess } from '@/api'
import { getForecastDetail } from '@/api/wms/inventory-forecast'
import type {
  ForecastDetailVO,
  ForecastDayVO,
  ForecastStatus
} from '@/api/wms/inventory-forecast/types'
import { formatDateChinese } from '@/utils/date'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'
import SafetyStockEditModal from './SafetyStockEditModal.vue'
import ForecastChart from './ForecastChart.vue'
import { useForecastStatus } from '../composables/use-forecast-status'

defineOptions({ name: 'ForecastDetailDrawer' })

const { getStatusTagColor, getStatusLabel, formatSellableDays } =
  useForecastStatus()

const props = defineProps<{
  regionId?: number
  skuCode?: string
}>()

const open = defineModel<boolean>('open', { required: true })

const loading = ref(false)
const detail = ref<ForecastDetailVO>()
const simulatedDailySales = ref<number>()
const safetyStockModalOpen = ref(false)
const viewMode = ref<'chart' | 'table'>('chart')

const viewOptions = [
  { value: 'chart', label: '图表' },
  { value: 'table', label: '表格' }
]

const forecastColumns = [
  { title: '日期', key: 'date', width: 70 },
  { title: '预计入库', key: 'incoming', width: 140 },
  { title: '预计销量', key: 'sales', width: 75 },
  { title: '期末库存', key: 'closingStock', width: 75 },
  { title: '状态', key: 'status', width: 60 }
]

// 计算首个预警日索引（LOW 或 CRITICAL）
const firstWarningIndex = computed(() => {
  if (!detail.value) return -1
  return detail.value.forecastList.findIndex(d => d.status === 'LOW' || d.status === 'CRITICAL')
})

// 计算首个断货日索引
const firstStockoutIndex = computed(() => {
  if (!detail.value) return -1
  return detail.value.forecastList.findIndex(d => d.status === 'STOCKOUT')
})

// 计算入库合计
const totalIncoming = computed(() => {
  if (!detail.value) return 0
  return detail.value.forecastList.reduce((sum, d) => sum + d.incoming, 0)
})

// 计算首个断货日期
const firstStockoutDate = computed(() => {
  if (!detail.value || firstStockoutIndex.value < 0) return null
  return detail.value.forecastList[firstStockoutIndex.value].date
})

watch(
  () => [open.value, props.regionId, props.skuCode],
  ([isOpen, regionId, sku]) => {
    if (isOpen && regionId && sku) {
      loadDetail()
    }
  },
  { immediate: true }
)

async function loadDetail(dailySales?: number) {
  if (!props.regionId || !props.skuCode) return

  loading.value = true
  try {
    const result = await getForecastDetail({
      regionId: props.regionId,
      skuCode: props.skuCode,
      dailySales
    })
    if (isSuccess(result)) {
      detail.value = result.data
      if (!dailySales) {
        simulatedDailySales.value = result.data.dailySales
      }
    }
  } finally {
    loading.value = false
  }
}

function handleSimulate() {
  if (simulatedDailySales.value !== undefined) {
    loadDetail(simulatedDailySales.value)
  }
}

function resetSimulation() {
  loadDetail()
}

function handleClose() {
  detail.value = undefined
  simulatedDailySales.value = undefined
}

function getStockClassByStatus(status: ForecastStatus): string {
  if (status === 'STOCKOUT') return 'stock-danger'
  if (status === 'CRITICAL' || status === 'LOW') return 'stock-warning'
  return ''
}

function getStatusIcon(status: ForecastStatus) {
  const map: Record<ForecastStatus, typeof ShieldCheckIcon> = {
    SUFFICIENT: ShieldCheckIcon,
    LOW: TrendingDownIcon,
    CRITICAL: AlertCircleIcon,
    STOCKOUT: PackageXIcon,
    NO_SALES: BarChart2Icon
  }
  return map[status]
}

function getRowClassName(record: ForecastDayVO, index: number): string {
  if (index === firstStockoutIndex.value) return 'row-stockout'
  if (index === firstWarningIndex.value) return 'row-first-warning'
  return ''
}
</script>

<style scoped>
/*
 * 间距系统：使用 a-flex gap 统一控制
 * - 区块间距: 32px (由外层 a-flex 控制)
 * - 区块内元素间距: 12px (由内层 a-flex 控制)
 * - 卡片内边距: 12px 16px
 *
 * 颜色系统：通过 cssVars 注入 CSS 变量，支持主题切换
 */

/* ===== 头部区域 ===== */
.detail-header {
  padding-bottom: 16px;
  border-bottom: 1px solid var(--ant-color-border);
}

.region-info {
  line-height: 1.4;
}

.region-label {
  font-size: 12px;
  color: var(--ant-color-text-tertiary);
}

.region-name {
  font-size: 14px;
  color: var(--ant-color-text);
}

/* ===== 统一标题样式 ===== */
.section-title {
  font-weight: 600;
  font-size: 16px;
  color: var(--ant-color-text);
}

/* ===== 库存描述区域 ===== */
.pending-shipment-value {
  color: var(--ant-color-text-secondary);
}

.pending-shipment-hint {
  margin-left: 4px;
  font-size: 12px;
  color: var(--ant-color-text-tertiary);
}

/* ===== 模拟区域 ===== */
.simulation-section {
  padding: 12px 16px;
  background: var(--ant-color-fill-tertiary);
  border-radius: var(--ant-border-radius);
}

.safety-threshold {
  font-size: 14px;
  color: var(--ant-color-text-secondary);
}

.edit-icon {
  margin-left: 8px;
  color: var(--ant-color-primary);
  cursor: pointer;
  font-size: 14px;
}

.edit-icon:hover {
  color: var(--ant-color-primary-hover);
}

/* ===== 表格视图 ===== */
.incoming-cell {
  line-height: 1.4;
}

.incoming-value {
  color: var(--ant-color-success);
  font-weight: 500;
}

.no-incoming {
  color: var(--ant-color-text-quaternary);
}

.sales-value {
  color: var(--ant-color-text-secondary);
}

.incoming-detail {
  font-size: 12px;
  color: var(--ant-color-text-secondary);
  padding-left: 4px;
  margin-top: 2px;
}

.incoming-detail--pending {
  color: var(--ant-color-text-tertiary);
}

.stock-danger {
  color: var(--color-error);
  font-weight: 600;
}

.stock-warning {
  color: var(--color-warning);
}

.status-icon {
  vertical-align: -0.125em;
  margin-right: 4px;
}

/* 关键日期高亮行 */
.forecast-table-wrapper :deep(.row-stockout) {
  background-color: var(--color-error-bg) !important;
}

.forecast-table-wrapper :deep(.row-first-warning td),
.forecast-table-wrapper :deep(.row-stockout td) {
  position: relative;
}

.forecast-table-wrapper :deep(.row-stockout td:first-child::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background-color: var(--color-error);
}

.forecast-table-wrapper :deep(.row-first-warning td:first-child::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background-color: var(--ant-color-warning);
}

/* ===== 底部汇总 ===== */
.forecast-summary {
  padding: 12px 16px;
  background: var(--ant-color-fill-quaternary);
  border-radius: var(--ant-border-radius);
  font-size: 14px;
  color: var(--ant-color-text);
}

.summary-item {
  color: var(--ant-color-text-secondary);
}

.stockout-date {
  color: var(--ant-color-error);
  font-weight: 600;
}

.no-stockout {
  color: var(--ant-color-text-tertiary);
}
</style>
