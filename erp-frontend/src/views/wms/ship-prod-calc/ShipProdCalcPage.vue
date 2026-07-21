<template>
  <div class="ship-prod-calc-page">
    <a-card title="发货生产测算" :bordered="false">
      <!-- 状态卡（点击筛选决策） -->
      <a-row :gutter="16" class="status-cards">
        <a-col :xs="12" :lg="6">
          <div
            class="status-card ship"
            :class="{ active: searchParams.decision === 'SHIP' }"
            @click="toggleDecision('SHIP')"
          >
            <div class="sc-title">🔴 需发货</div>
            <div class="sc-value">{{ counts.needShip }}</div>
            <div class="sc-desc">全链路支撑 &lt; {{ thresholds.ship }} 天</div>
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div
            class="status-card produce"
            :class="{ active: searchParams.decision === 'PRODUCE' }"
            @click="toggleDecision('PRODUCE')"
          >
            <div class="sc-title">🟠 需订货</div>
            <div class="sc-value">{{ counts.needProduce }}</div>
            <div class="sc-desc">生产支撑 &lt; {{ thresholds.prod }} 天</div>
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div
            class="status-card shortage"
            :class="{ active: searchParams.decision === 'SHORTAGE' }"
            @click="toggleDecision('SHORTAGE')"
          >
            <div class="sc-title">⚠️ 断档预警</div>
            <div class="sc-value">{{ counts.shortage }}</div>
            <div class="sc-desc">现货撑不到最早到货</div>
          </div>
        </a-col>
        <a-col :xs="12" :lg="6">
          <div
            class="status-card nosales"
            :class="{ active: searchParams.decision === 'NO_SALES' }"
            @click="toggleDecision('NO_SALES')"
          >
            <div class="sc-title">⚪ 待观察</div>
            <div class="sc-value">{{ counts.noSales }}</div>
            <div class="sc-desc">零销量 / 新品</div>
          </div>
        </a-col>
      </a-row>

      <!-- 工具栏 -->
      <div class="table-toolbar">
        <a-space>
          <a-input-search
            v-model:value="searchParams.skuKeyword"
            placeholder="搜索 SKU 编码/名称"
            style="width: 220px"
            allow-clear
            @search="handleSearch"
          />
          <a-select
            v-model:value="searchParams.decision"
            placeholder="决策筛选"
            allow-clear
            style="width: 130px"
            @change="handleSearch"
          >
            <a-select-option value="SHIP">需发货</a-select-option>
            <a-select-option value="PRODUCE">需订货</a-select-option>
            <a-select-option value="SHORTAGE">断档</a-select-option>
            <a-select-option value="NO_SALES">待观察</a-select-option>
          </a-select>
          <a-date-picker
            v-model:value="searchParams.baseDate"
            value-format="YYYY-MM-DD"
            placeholder="测算基准日"
            @change="handleSearch"
          />
        </a-space>
        <div class="threshold-hint">
          阈值：发 {{ thresholds.ship }} · 产 {{ thresholds.prod }} · 备 {{ thresholds.safety }} 天
        </div>
      </div>

      <!-- 测算总表 -->
      <pro-table
        ref="tableRef"
        row-key="skuCode"
        :request="tableRequest"
        :columns="columns"
        :tool-bar-render="false"
        :card-props="false"
        :scroll="{ x: 1180 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'skuBrief'">
            <SkuBriefCell :brief="record.skuBrief" />
          </template>
          <template v-else-if="column.key === 'daily'">
            <div>估算 {{ record.zt }}</div>
            <div class="text-secondary">常 {{ record.xt }} / 峰 {{ record.yt }}</div>
          </template>
          <template v-else-if="column.key === 'shipSupport'">
            {{ fmtDays(record.shipSupportDays) }}
          </template>
          <template v-else-if="column.key === 'totalSupport'">
            <a-tooltip :title="record.shipPath">
              <span :class="{ 'danger-text': record.needShip }">
                {{ fmtDays(record.totalSupportDays) }}
              </span>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'shipDecision'">
            <a-tag v-if="record.needShip" color="red">发货 F={{ record.shipPlanQty }}</a-tag>
            <span v-else class="text-tertiary">暂不</span>
          </template>
          <template v-else-if="column.key === 'prodDecision'">
            <a-tooltip :title="record.prodPath">
              <a-tag v-if="record.needProduce" color="orange">订货 Q={{ record.prodPlanQty }}</a-tag>
              <span v-else class="text-tertiary">暂不</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'operate'">
            <a @click="showDetail(record)">详情</a>
          </template>
        </template>
      </pro-table>
    </a-card>

    <ShipProdDetailDrawer
      v-model:open="detailOpen"
      :sku-code="selectedSku"
      :base-date="searchParams.baseDate"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import ProTable from '#/table'
import type { ProColumns, TableRequest, ProTableInstanceExpose } from '#/table'
import { isSuccess } from '@/api'
import { mergePageParam } from '@/utils/page-utils'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'
import { getShipProdSummary } from '@/api/wms/ship-prod-calc'
import type {
  ShipProdCalcRowVO,
  ShipProdCounts,
  ShipProdDecision
} from '@/api/wms/ship-prod-calc/types'
import ShipProdDetailDrawer from './components/ShipProdDetailDrawer.vue'

defineOptions({ name: 'ShipProdCalcPage' })

const tableRef = ref<ProTableInstanceExpose>()

const columns: ProColumns[] = [
  { title: 'SKU', key: 'skuBrief', width: 240, fixed: 'left' },
  { title: '现货', dataIndex: 'onHand', key: 'onHand', width: 80 },
  { title: '在途', dataIndex: 'inTransit', key: 'inTransit', width: 80 },
  { title: '在制', dataIndex: 'producing', key: 'producing', width: 80 },
  { title: '日均(估/常/峰)', key: 'daily', width: 130 },
  { title: '现货支撑', key: 'shipSupport', width: 90 },
  { title: '全链路支撑', key: 'totalSupport', width: 100 },
  { title: '发货决策', key: 'shipDecision', width: 130 },
  { title: '生产决策', key: 'prodDecision', width: 130 },
  { title: '操作', key: 'operate', width: 70, fixed: 'right' }
]

const searchParams = reactive({
  skuKeyword: '',
  decision: undefined as ShipProdDecision | undefined,
  baseDate: undefined as string | undefined
})

const counts = ref<ShipProdCounts>({ needShip: 0, needProduce: 0, shortage: 0, noSales: 0 })
const thresholds = reactive({ ship: 35, prod: 80, safety: 45 })

const detailOpen = ref(false)
const selectedSku = ref<string>()

const tableRequest: TableRequest = async (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  const res = await getShipProdSummary({ ...pageParam, ...searchParams })
  if (isSuccess(res)) {
    counts.value = res.data.counts
    thresholds.ship = res.data.shipThresholdDays
    thresholds.prod = res.data.prodThresholdDays
    thresholds.safety = res.data.safetyStockDays
    return { ...res, data: { records: res.data.list, total: res.data.total } }
  }
  return res
}

const reloadTable = (resetPageIndex?: boolean) => tableRef.value?.actionRef?.reload(resetPageIndex)

function handleSearch() {
  reloadTable(true)
}

function toggleDecision(d: ShipProdDecision) {
  searchParams.decision = searchParams.decision === d ? undefined : d
  handleSearch()
}

function fmtDays(v: number | null): string {
  return v === null || v === undefined ? '∞' : String(v)
}

function showDetail(record: ShipProdCalcRowVO) {
  selectedSku.value = record.skuCode
  detailOpen.value = true
}
</script>

<style scoped>
.ship-prod-calc-page {
  padding: 16px;
}
.status-cards {
  margin-bottom: 16px;
}
.status-card {
  border: 1px solid var(--ant-color-border-secondary, #f0f0f0);
  border-radius: 8px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s;
}
.status-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.status-card.active {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
}
.sc-title {
  font-size: 13px;
  color: var(--ant-color-text-secondary, rgba(0, 0, 0, 0.65));
}
.sc-value {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.3;
}
.sc-desc {
  font-size: 12px;
  color: var(--ant-color-text-tertiary, rgba(0, 0, 0, 0.45));
}
.status-card.ship .sc-value {
  color: #ff4d4f;
}
.status-card.produce .sc-value {
  color: #fa8c16;
}
.status-card.shortage .sc-value {
  color: #d48806;
}
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.threshold-hint {
  color: var(--ant-color-text-secondary);
  font-size: 13px;
}
.text-secondary {
  color: var(--ant-color-text-secondary);
  font-size: 12px;
}
.text-tertiary {
  color: var(--ant-color-text-tertiary);
  font-size: 12px;
}
.danger-text {
  color: #ff4d4f;
  font-weight: 600;
}
</style>
