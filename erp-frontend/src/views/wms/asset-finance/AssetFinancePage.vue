<template>
  <div class="asset-finance-page">
    <a-card :bordered="false">
      <div class="page-toolbar">
        <a-segmented
          :value="activeSection"
          :options="sectionOptions"
          size="large"
          @change="onSectionChange"
        />
        <a-space>
          <span class="updated-at">更新于 {{ updatedAt || '--' }}</span>
          <a-button v-if="activeSection !== 'wms-account'" :loading="summaryLoading" @click="refreshAll">刷新</a-button>
        </a-space>
      </div>

      <a-spin v-if="activeSection === 'assets'" :spinning="summaryLoading">
        <div class="compact-summary-grid">
          <div v-for="a in assets" :key="a.currency" class="summary-metric asset-metric">
            <div class="metric-label">资产总额 · {{ a.currency }}</div>
            <div class="metric-value">{{ money(a.total) }}</div>
            <div class="metric-detail">采购 {{ money(a.procurement) }} · 物流 {{ money(a.logistics) }}</div>
          </div>
          <div class="summary-metric">
            <div class="metric-label">库存总量</div>
            <div class="metric-value">{{ assetOverview?.totalHeldQuantity ?? 0 }} <small>件</small></div>
            <div class="metric-detail">包含海外仓、在途及 FBO</div>
          </div>
          <div class="summary-metric">
            <div class="metric-label">海外仓 / FBO</div>
            <div class="metric-value compact-value">
              {{ positionQuantity('OWN_AVAILABLE') + positionQuantity('OWN_RESERVED') + positionQuantity('OWN_DAMAGED') }}
              <span>/</span>
              {{ positionQuantity('FBO') }}
            </div>
            <div class="metric-detail">海外仓库存 / FBO 平台库存</div>
          </div>
          <div class="summary-metric" :class="{ 'warning-metric': assetOverview?.unvaluedQuantity }">
            <div class="metric-label">未估值库存</div>
            <div class="metric-value">{{ assetOverview?.unvaluedQuantity ?? 0 }} <small>件</small></div>
            <div class="metric-detail">{{ assetOverview?.unvaluedSkuCount ?? 0 }} 个 SKU 未维护采购成本</div>
          </div>
          <div v-if="!assets.length" class="summary-metric empty-metric">
            <div class="metric-label">资产估值</div>
            <div class="metric-value muted-value">暂无金额</div>
            <div class="metric-detail">库存数量仍正常统计</div>
          </div>
        </div>

        <div class="position-strip">
          <span class="strip-title">库存位置</span>
          <span v-for="position in assetOverview?.holdingPositions ?? []" :key="position.code" class="position-chip">
            {{ position.name }} <strong>{{ position.quantity.toLocaleString() }}</strong>
          </span>
          <span v-if="assetOverview?.fboLastSyncedAt" class="sync-note">
            FBO 同步 {{ assetOverview.fboLastSyncedAt }}
            <a-tag v-if="assetOverview.fboStale" color="orange">可能过期</a-tag>
          </span>
        </div>

        <div v-if="assetOverview?.unvaluedQuantity" class="compact-warning">
          未估值库存不会计入资产金额，请补充对应 SKU 的采购成本。
        </div>

      </a-spin>

      <a-spin v-else-if="activeSection === 'payables'" :spinning="summaryLoading">
        <div class="payable-summary-grid">
          <div
            v-for="item in payablesOverview?.supplierPayable ?? []"
            :key="`supplier-${item.currency}`"
            class="summary-metric"
          >
            <div class="metric-label">采购应付 · {{ item.currency }}</div>
            <div class="metric-value danger-value">{{ money(item.outstanding) }}</div>
            <div class="metric-detail">合同 {{ money(item.contract) }} · 已付 {{ money(item.paid) }}</div>
          </div>
          <div v-if="payablesOverview?.providerPayable" class="summary-metric">
            <div class="metric-label">物流应付 · {{ payablesOverview.providerPayable.currency }}</div>
            <div class="metric-value danger-value">{{ money(payablesOverview.providerPayable.outstanding) }}</div>
            <div class="metric-detail">
              合同 {{ money(payablesOverview.providerPayable.contract) }} · 已付 {{ money(payablesOverview.providerPayable.paid) }}
            </div>
          </div>
          <div v-if="!hasPayables" class="summary-metric empty-metric">
            <div class="metric-label">应付余额</div>
            <div class="metric-value muted-value">0.00</div>
            <div class="metric-detail">当前没有待付款项</div>
          </div>
        </div>
      </a-spin>

      <WmsFundAccountPanel v-else />

      <!-- 明细 -->
      <a-tabs v-if="activeSection === 'assets'" v-model:activeKey="activeTab" class="detail-tabs" @change="onTabChange">
        <!-- 采购成本明细 -->
        <a-tab-pane key="procurement" tab="采购成本明细">
          <a-table
            :columns="procCols"
            :data-source="procRows"
            :loading="procLoading"
            row-key="rowKey"
            size="small"
            :scroll="{ x: 720 }"
            :pagination="{ pageSize: 20, showSizeChanger: true }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'skuBrief'">
                <SkuBriefCell :brief="record.skuBrief" />
              </template>
              <template v-else-if="column.key === 'avgUnitPrice'">
                {{ money(record.avgUnitPrice) }} {{ record.currency }}
              </template>
              <template v-else-if="column.key === 'procurementCost'">
                {{ money(record.procurementCost) }} {{ record.currency }}
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 物流附加明细 -->
        <a-tab-pane key="logistics" tab="物流附加明细">
          <a-table
            :columns="logiCols"
            :data-source="logiRows"
            :loading="logiLoading"
            row-key="skuCode"
            size="small"
            :scroll="{ x: 640 }"
            :pagination="{ pageSize: 20, showSizeChanger: true }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'skuBrief'">
                <SkuBriefCell :brief="record.skuBrief" />
              </template>
              <template v-else-if="column.key === 'unitCostUsd'">${{ money(record.unitCostUsd) }}</template>
              <template v-else-if="column.key === 'logisticsCostUsd'">
                ${{ money(record.logisticsCostUsd) }}
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>

      <a-tabs v-else-if="activeSection === 'payables'" v-model:activeKey="activeTab" class="detail-tabs" @change="onTabChange">
        <!-- 应付供应商（下钻采购单） -->
        <a-tab-pane key="supplier" tab="应付供应商">
          <a-table
            :columns="supCols"
            :data-source="supRows"
            :loading="supLoading"
            row-key="rowKey"
            size="small"
            :scroll="{ x: 720 }"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'contract'">{{ money(record.contract) }} {{ record.currency }}</template>
              <template v-else-if="column.key === 'paid'">{{ money(record.paid) }}</template>
              <template v-else-if="column.key === 'outstanding'">
                <span class="danger-text">{{ money(record.outstanding) }}</span>
              </template>
            </template>
            <template #expandedRowRender="{ record }">
              <a-table
                :columns="supOrderCols"
                :data-source="record.orders"
                row-key="orderNo"
                size="small"
                :pagination="false"
              >
                <template #bodyCell="{ column, record: o }">
                  <template v-if="column.key === 'orderStatus'">
                    <a-tag>{{ poStatusText(o.orderStatus) }}</a-tag>
                  </template>
                  <template v-else-if="column.key === 'totalAmount'">{{ money(o.totalAmount) }}</template>
                  <template v-else-if="column.key === 'prepay'">
                    {{ money(o.prepayAmount) }}
                    <a-tag :color="o.prepayPaid ? 'green' : 'default'" size="small">
                      {{ o.prepayPaid ? '已付' : '未付' }}
                    </a-tag>
                  </template>
                  <template v-else-if="column.key === 'balance'">
                    {{ money(o.balanceAmount) }}
                    <a-tag :color="o.balancePaid ? 'green' : 'default'" size="small">
                      {{ o.balancePaid ? '已付' : '未付' }}
                    </a-tag>
                  </template>
                  <template v-else-if="column.key === 'outstanding'">
                    <span class="danger-text">{{ money(o.outstanding) }}</span>
                  </template>
                </template>
              </a-table>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 应付物流商（下钻物流单） -->
        <a-tab-pane key="provider" tab="应付物流商">
          <a-table
            :columns="provCols"
            :data-source="provRows"
            :loading="provLoading"
            row-key="providerId"
            size="small"
            :scroll="{ x: 720 }"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'contract'">${{ money(record.contract) }}</template>
              <template v-else-if="column.key === 'contractCny'">¥{{ money(record.contractCny) }}</template>
              <template v-else-if="column.key === 'paid'">${{ money(record.paid) }}</template>
              <template v-else-if="column.key === 'outstanding'">
                <span class="danger-text">${{ money(record.outstanding) }}</span>
              </template>
            </template>
            <template #expandedRowRender="{ record }">
              <a-table
                :columns="provOrderCols"
                :data-source="record.orders"
                row-key="shippingNo"
                size="small"
                :pagination="false"
              >
                <template #bodyCell="{ column, record: o }">
                  <template v-if="column.key === 'shippingStatus'">
                    <a-tag>{{ shipStatusText(o.shippingStatus) }}</a-tag>
                  </template>
                  <template v-else-if="column.key === 'totalAmount'">${{ money(o.totalAmount) }}</template>
                  <template v-else-if="column.key === 'totalAmountCny'">¥{{ money(o.totalAmountCny) }}</template>
                  <template v-else-if="column.key === 'paid'">
                    <a-tag :color="o.paid ? 'green' : 'default'" size="small">
                      {{ o.paid ? '已付' : '未付' }}
                    </a-tag>
                  </template>
                  <template v-else-if="column.key === 'outstanding'">
                    <span class="danger-text">${{ money(o.outstanding) }}</span>
                  </template>
                </template>
              </a-table>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onActivated } from 'vue'
import { isSuccess } from '@/api'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'
import WmsFundAccountPanel from './components/WmsFundAccountPanel.vue'
import {
  getAssetOverview,
  getPayablesOverview,
  getProcurementDetail,
  getLogisticsDetail,
  getPayableSupplier,
  getPayableProvider
} from '@/api/wms/asset-finance'
import type {
  AssetOverviewVO,
  AssetCurrencyVO,
  AssetProcurementRowVO,
  AssetLogisticsRowVO,
  PayablesOverviewVO,
  PayableSupplierVO,
  PayableProviderVO
} from '@/api/wms/asset-finance/types'

defineOptions({ name: 'AssetFinancePage' })

// ---------------- 页面视图 ----------------
type SectionKey = 'assets' | 'payables' | 'wms-account'
const activeSection = ref<SectionKey>('assets')
const sectionOptions = [
  { label: '资产总览', value: 'assets' },
  { label: '应付账务', value: 'payables' },
  { label: 'WMS资金账户', value: 'wms-account' }
]

const assetOverview = ref<AssetOverviewVO>()
const payablesOverview = ref<PayablesOverviewVO>()
const assets = ref<AssetCurrencyVO[]>([])
const summaryLoading = ref(false)
const updatedAt = ref('')
const hasPayables = computed(() => {
  const suppliers = payablesOverview.value?.supplierPayable ?? []
  const provider = payablesOverview.value?.providerPayable
  return suppliers.length > 0 || Number(provider?.contract ?? 0) !== 0 || Number(provider?.outstanding ?? 0) !== 0
})

function positionQuantity(code: string) {
  return assetOverview.value?.holdingPositions?.find(item => item.code === code)?.quantity ?? 0
}

async function loadAssetOverview() {
  summaryLoading.value = true
  try {
    const res = await getAssetOverview()
    if (isSuccess(res)) {
      assetOverview.value = res.data
      assets.value = res.data.assets
      updatedAt.value = new Date().toLocaleString('zh-CN', { hour12: false })
    }
  } finally {
    summaryLoading.value = false
  }
}

async function loadPayablesOverview() {
  summaryLoading.value = true
  try {
    const res = await getPayablesOverview()
    if (isSuccess(res)) {
      payablesOverview.value = res.data
      updatedAt.value = new Date().toLocaleString('zh-CN', { hour12: false })
    }
  } finally {
    summaryLoading.value = false
  }
}

// ---------------- 明细 tabs ----------------
const activeTab = ref('procurement')
const loaded = ref<Record<string, boolean>>({})

// 采购成本
const procRows = ref<(AssetProcurementRowVO & { rowKey: string })[]>([])
const procLoading = ref(false)
const procCols = [
  { title: 'SKU', key: 'skuBrief', width: 240, fixed: 'left' as const },
  { title: '币种', dataIndex: 'currency', key: 'currency', width: 80 },
  { title: '持有量', dataIndex: 'heldQty', key: 'heldQty', width: 100 },
  { title: '加权单价', key: 'avgUnitPrice', width: 140 },
  { title: '采购成本', key: 'procurementCost', width: 160 }
]
async function loadProcurement() {
  procLoading.value = true
  try {
    const res = await getProcurementDetail()
    if (isSuccess(res)) {
      procRows.value = res.data.map((r, i) => ({ ...r, rowKey: `${r.skuCode}-${r.currency}-${i}` }))
    }
  } finally {
    procLoading.value = false
  }
}

// 物流附加
const logiRows = ref<AssetLogisticsRowVO[]>([])
const logiLoading = ref(false)
const logiCols = [
  { title: 'SKU', key: 'skuBrief', width: 240, fixed: 'left' as const },
  { title: '已发运在库量', dataIndex: 'shippedHeldQty', key: 'shippedHeldQty', width: 120 },
  { title: '单位物流成本', key: 'unitCostUsd', width: 140 },
  { title: '物流附加成本', key: 'logisticsCostUsd', width: 160 }
]
async function loadLogistics() {
  logiLoading.value = true
  try {
    const res = await getLogisticsDetail()
    if (isSuccess(res)) {
      logiRows.value = res.data
    }
  } finally {
    logiLoading.value = false
  }
}

// 应付供应商
const supRows = ref<(PayableSupplierVO & { rowKey: string })[]>([])
const supLoading = ref(false)
const supCols = [
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 200, fixed: 'left' as const },
  { title: '币种', dataIndex: 'currency', key: 'currency', width: 80 },
  { title: '合同总额', key: 'contract', width: 160 },
  { title: '已付', key: 'paid', width: 140 },
  { title: '应付余额', key: 'outstanding', width: 140 }
]
const supOrderCols = [
  { title: '采购单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '状态', key: 'orderStatus', width: 100 },
  { title: '合同总额', key: 'totalAmount', width: 120 },
  { title: '首付款', key: 'prepay', width: 150 },
  { title: '尾款', key: 'balance', width: 150 },
  { title: '应付余额', key: 'outstanding', width: 120 }
]
async function loadSupplier() {
  supLoading.value = true
  try {
    const res = await getPayableSupplier()
    if (isSuccess(res)) {
      supRows.value = res.data.map((r) => ({ ...r, rowKey: `${r.supplierId}-${r.currency}` }))
    }
  } finally {
    supLoading.value = false
  }
}

// 应付物流商
const provRows = ref<PayableProviderVO[]>([])
const provLoading = ref(false)
const provCols = [
  { title: '物流商', dataIndex: 'providerName', key: 'providerName', width: 200, fixed: 'left' as const },
  { title: '合同总额(USD)', key: 'contract', width: 150 },
  { title: '折合(CNY)', key: 'contractCny', width: 150 },
  { title: '已付', key: 'paid', width: 140 },
  { title: '应付余额', key: 'outstanding', width: 140 }
]
const provOrderCols = [
  { title: '物流单号', dataIndex: 'shippingNo', key: 'shippingNo' },
  { title: '状态', key: 'shippingStatus', width: 110 },
  { title: '总额(USD)', key: 'totalAmount', width: 120 },
  { title: '总额(CNY)', key: 'totalAmountCny', width: 120 },
  { title: '付款', key: 'paid', width: 90 },
  { title: '应付余额', key: 'outstanding', width: 120 }
]
async function loadProvider() {
  provLoading.value = true
  try {
    const res = await getPayableProvider()
    if (isSuccess(res)) {
      provRows.value = res.data
    }
  } finally {
    provLoading.value = false
  }
}

function onTabChange(key: string) {
  loaded.value[key] = true
  if (key === 'procurement') loadProcurement()
  else if (key === 'logistics') loadLogistics()
  else if (key === 'supplier') loadSupplier()
  else if (key === 'provider') loadProvider()
}

async function onSectionChange(value: string | number) {
  activeSection.value = value as SectionKey
  if (activeSection.value === 'assets') {
    activeTab.value = 'procurement'
    await loadAssetOverview()
    await loadProcurement()
  } else if (activeSection.value === 'payables') {
    activeTab.value = 'supplier'
    await loadPayablesOverview()
    await loadSupplier()
  }
}

async function refreshAll() {
  if (activeSection.value === 'assets') {
    await loadAssetOverview()
    if (activeTab.value === 'procurement') await loadProcurement()
    else if (activeTab.value === 'logistics') await loadLogistics()
  } else if (activeSection.value === 'payables') {
    await loadPayablesOverview()
    if (activeTab.value === 'supplier') await loadSupplier()
    else if (activeTab.value === 'provider') await loadProvider()
  }
}

// ---------------- 工具 ----------------
function money(v: number | null | undefined): string {
  const n = Number(v ?? 0)
  return n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const PO_STATUS: Record<string, string> = {
  DRAFT: '草稿',
  CONFIRMED: '已确认',
  IN_PRODUCTION: '生产中',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}
function poStatusText(s: string): string {
  return PO_STATUS[s] ?? s
}

const SHIP_STATUS: Record<string, string> = {
  PENDING: '待发货',
  SHIPPED: '已发货',
  PARTIAL_ARRIVED: '部分到货',
  ALL_ARRIVED: '全部到货',
  COMPLETED: '已完成'
}
function shipStatusText(s: string): string {
  return SHIP_STATUS[s] ?? s
}

onMounted(() => {
  loadAssetOverview()
  loaded.value['procurement'] = true
  loadProcurement()
})
onActivated(refreshAll)
</script>

<style scoped>
.asset-finance-page {
  padding: 12px;
}
.asset-finance-page :deep(.ant-card-body) {
  padding: 16px;
}
.page-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.compact-summary-grid,
.payable-summary-grid {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 2px;
}
.summary-metric {
  flex: 1 0 176px;
  min-width: 0;
  min-height: 88px;
  padding: 11px 13px;
  border: 1px solid var(--ant-color-border-secondary, #e8e8e8);
  border-radius: 6px;
  background: #fff;
}
.asset-metric {
  border-color: #b7d5ff;
}
.metric-label {
  color: var(--ant-color-text-secondary, rgba(0, 0, 0, 0.65));
  font-size: 12px;
  line-height: 18px;
}
.metric-value {
  margin-top: 3px;
  font-size: 21px;
  line-height: 28px;
  font-weight: 650;
  color: var(--ant-color-text, rgba(0, 0, 0, 0.88));
  font-variant-numeric: tabular-nums;
}
.metric-value small {
  font-size: 12px;
  font-weight: 400;
}
.compact-value span {
  margin: 0 4px;
  color: var(--ant-color-text-tertiary, rgba(0, 0, 0, 0.45));
}
.metric-detail {
  margin-top: 3px;
  overflow: hidden;
  color: var(--ant-color-text-tertiary, rgba(0, 0, 0, 0.45));
  font-size: 11px;
  line-height: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.warning-metric {
  border-color: #ffd591;
  background: #fffaf0;
}
.danger-value {
  color: #cf1322;
}
.muted-value {
  color: var(--ant-color-text-tertiary, rgba(0, 0, 0, 0.45));
  font-size: 18px;
}
.position-strip {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  margin-top: 10px;
  padding: 7px 10px;
  overflow-x: auto;
  border: 1px solid var(--ant-color-border-secondary, #f0f0f0);
  border-radius: 6px;
  white-space: nowrap;
}
.strip-title {
  padding-right: 8px;
  border-right: 1px solid var(--ant-color-border-secondary, #f0f0f0);
  font-weight: 600;
}
.position-chip {
  color: var(--ant-color-text-secondary, rgba(0, 0, 0, 0.65));
  font-size: 12px;
}
.position-chip strong {
  margin-left: 3px;
  color: var(--ant-color-text, rgba(0, 0, 0, 0.88));
  font-variant-numeric: tabular-nums;
}
.sync-note {
  margin-left: auto;
  color: var(--ant-color-text-tertiary, rgba(0, 0, 0, 0.45));
  font-size: 12px;
}
.compact-warning {
  margin-top: 8px;
  color: #ad6800;
  font-size: 12px;
}
.detail-tabs {
  margin-top: 10px;
}
.danger-text {
  color: #ff4d4f;
  font-weight: 600;
}
.updated-at { font-size: 12px; color: var(--ant-color-text-tertiary); }
@media (max-width: 768px) {
  .page-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
  .page-toolbar :deep(.ant-segmented) {
    max-width: 100%;
  }
  .summary-metric {
    flex-basis: 158px;
  }
}
</style>
