<template>
  <div class="asset-finance-page">
    <a-card title="资产与账务" :bordered="false">
      <template #extra>
        <a-space>
          <span class="updated-at">更新于 {{ updatedAt || '--' }}</span>
          <a-button :loading="overviewLoading" @click="refreshAll">刷新</a-button>
        </a-space>
      </template>
      <a-spin :spinning="overviewLoading">
        <!-- 资产总览：分币种，不折算 -->
        <div class="section-title">📦 资产估值（采购成本 + 物流附加，加权平均、分币种不折算）</div>
        <a-row :gutter="16" class="stat-row">
          <a-col v-for="a in assets" :key="a.currency" :xs="24" :sm="12" :lg="8">
            <div class="stat-card asset">
              <div class="sc-head">
                <span class="sc-cur">{{ a.currency }}</span>
                <span class="sc-total">{{ money(a.total) }}</span>
              </div>
              <div class="sc-split">
                <span>采购 {{ money(a.procurement) }}</span>
                <span>物流 {{ money(a.logistics) }}</span>
              </div>
            </div>
          </a-col>
          <a-col v-if="!assets.length" :span="24">
            <a-empty description="暂无持有资产" />
          </a-col>
        </a-row>

        <a-alert
          v-if="overview?.unvaluedQuantity"
          class="unvalued-alert"
          type="warning"
          show-icon
          :message="`有 ${overview.unvaluedSkuCount} 个 SKU、${overview.unvaluedQuantity} 件库存尚未维护采购成本，数量已计入资产，金额暂未计入`"
        />

        <div class="section-title">库存位置分布 · 共 {{ overview?.totalHeldQuantity ?? 0 }} 件</div>
        <a-row :gutter="12" class="position-row">
          <a-col v-for="position in overview?.holdingPositions ?? []" :key="position.code" :xs="12" :md="8" :xl="6">
            <div class="position-item">
              <span>{{ position.name }}</span>
              <strong>{{ position.quantity.toLocaleString() }}</strong>
            </div>
          </a-col>
        </a-row>

        <!-- 账务总览 -->
        <a-row :gutter="16">
          <a-col :xs="24" :lg="12">
            <div class="section-title">🏭 应付生产采购商（分币种）</div>
            <a-table
              :columns="supPayCols"
              :data-source="overview?.supplierPayable ?? []"
              row-key="currency"
              size="small"
              :pagination="false"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'contract'">{{ money(record.contract) }}</template>
                <template v-else-if="column.key === 'paid'">{{ money(record.paid) }}</template>
                <template v-else-if="column.key === 'outstanding'">
                  <span class="danger-text">{{ money(record.outstanding) }}</span>
                </template>
              </template>
            </a-table>
          </a-col>
          <a-col :xs="24" :lg="12">
            <div class="section-title">🚚 应付物流商（USD）</div>
            <a-table
              :columns="provPayCols"
              :data-source="overview ? [overview.providerPayable] : []"
              row-key="currency"
              size="small"
              :pagination="false"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'contract'">{{ money(record.contract) }}</template>
                <template v-else-if="column.key === 'paid'">{{ money(record.paid) }}</template>
                <template v-else-if="column.key === 'outstanding'">
                  <span class="danger-text">{{ money(record.outstanding) }}</span>
                </template>
              </template>
            </a-table>
          </a-col>
        </a-row>
      </a-spin>

      <!-- 明细 -->
      <a-tabs v-model:activeKey="activeTab" class="detail-tabs" @change="onTabChange">
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
import { ref, onMounted, onActivated } from 'vue'
import { isSuccess } from '@/api'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'
import {
  getAssetFinanceOverview,
  getProcurementDetail,
  getLogisticsDetail,
  getPayableSupplier,
  getPayableProvider
} from '@/api/wms/asset-finance'
import type {
  AssetFinanceOverviewVO,
  AssetCurrencyVO,
  AssetProcurementRowVO,
  AssetLogisticsRowVO,
  PayableSupplierVO,
  PayableProviderVO
} from '@/api/wms/asset-finance/types'

defineOptions({ name: 'AssetFinancePage' })

// ---------------- 总览 ----------------
const overview = ref<AssetFinanceOverviewVO>()
const assets = ref<AssetCurrencyVO[]>([])
const overviewLoading = ref(false)
const updatedAt = ref('')

const supPayCols = [
  { title: '币种', dataIndex: 'currency', key: 'currency', width: 80 },
  { title: '合同总额', key: 'contract' },
  { title: '已付', key: 'paid' },
  { title: '应付余额', key: 'outstanding' }
]
const provPayCols = [
  { title: '币种', dataIndex: 'currency', key: 'currency', width: 80 },
  { title: '合同总额', key: 'contract' },
  { title: '已付', key: 'paid' },
  { title: '应付余额', key: 'outstanding' }
]

async function loadOverview() {
  overviewLoading.value = true
  try {
    const res = await getAssetFinanceOverview()
    if (isSuccess(res)) {
      overview.value = res.data
      assets.value = res.data.assets
      updatedAt.value = new Date().toLocaleString('zh-CN', { hour12: false })
    }
  } finally {
    overviewLoading.value = false
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

async function refreshAll() {
  await loadOverview()
  if (activeTab.value === 'procurement') await loadProcurement()
  else if (activeTab.value === 'logistics') await loadLogistics()
  else if (activeTab.value === 'supplier') await loadSupplier()
  else if (activeTab.value === 'provider') await loadProvider()
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
  loadOverview()
  loaded.value['procurement'] = true
  loadProcurement()
})
onActivated(refreshAll)
</script>

<style scoped>
.asset-finance-page {
  padding: 16px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  margin: 8px 0 12px;
  color: var(--ant-color-text, rgba(0, 0, 0, 0.88));
}
.stat-row {
  margin-bottom: 16px;
}
.stat-card {
  border: 1px solid var(--ant-color-border-secondary, #f0f0f0);
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 16px;
}
.stat-card.asset {
  background: linear-gradient(135deg, rgba(22, 119, 255, 0.06), rgba(22, 119, 255, 0.01));
}
.sc-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.sc-cur {
  font-size: 13px;
  color: var(--ant-color-text-secondary, rgba(0, 0, 0, 0.65));
  font-weight: 600;
}
.sc-total {
  font-size: 24px;
  font-weight: 700;
  color: #1677ff;
}
.sc-split {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  color: var(--ant-color-text-tertiary, rgba(0, 0, 0, 0.45));
}
.detail-tabs {
  margin-top: 16px;
}
.danger-text {
  color: #ff4d4f;
  font-weight: 600;
}
.updated-at { font-size: 12px; color: var(--ant-color-text-tertiary); }
.unvalued-alert { margin-bottom: 16px; }
.position-row { margin-bottom: 18px; }
.position-item { display: flex; justify-content: space-between; align-items: center; min-height: 48px; padding: 10px 12px; border: 1px solid var(--ant-color-border-secondary, #f0f0f0); border-radius: 6px; }
.position-item span { color: var(--ant-color-text-secondary); }
.position-item strong { font-size: 16px; font-variant-numeric: tabular-nums; }
</style>
