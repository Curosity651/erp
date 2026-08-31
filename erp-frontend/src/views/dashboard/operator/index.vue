<template>
  <div class="operator-dashboard">
    <a-card :bordered="false" class="filter-card" :body-style="{ padding: '12px 16px' }">
      <div class="filter-bar">
        <a-space :size="12" wrap>
          <a-range-picker
            v-model:value="monthRange"
            picker="month"
            value-format="YYYY-MM"
            :allow-clear="false"
            style="width: 240px"
          />
          <a-select
            v-model:value="selectedOwners"
            mode="multiple"
            placeholder="货主（全部）"
            allow-clear
            style="min-width: 220px"
            :options="ownerOptions"
            :max-tag-count="2"
          />
          <a-button type="primary" :loading="loading" @click="loadData">查询</a-button>
        </a-space>
        <span class="updated-at">更新于 {{ updatedAt || '—' }}</span>
      </div>
    </a-card>

    <a-spin :spinning="loading">
      <a-row :gutter="[16, 16]" class="overview-row">
        <a-col :xs="24" :md="12" :xl="6">
          <a-card :bordered="false" title="本期收入">
            <MoneyList :items="data?.overview.incomeByCurrency" value-class="positive" />
            <div class="kpi-foot">物流产品计费 {{ data?.overview.incomeCount ?? 0 }} 次</div>
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12" :xl="6">
          <a-card :bordered="false" title="本期平台费用">
            <div v-if="data?.overview.expenseByCurrency?.length" class="money-list">
              <div v-for="item in data.overview.expenseByCurrency" :key="item.currency">
                <div class="money-line">
                  <span>{{ item.currency }} 已确认应付</span>
                  <strong class="warning">{{ formatCurrency(item.confirmedPayable, item.currency) }}</strong>
                </div>
                <div class="expense-detail">
                  已付款 {{ formatCurrency(item.paidAmount, item.currency) }} · 争议
                  {{ formatCurrency(item.disputedAmount, item.currency) }}
                </div>
              </div>
            </div>
            <a-empty v-else :image="simpleImage" description="暂无费用" />
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12" :xl="6">
          <a-card :bordered="false" title="经营余额">
            <MoneyList :items="data?.overview.balanceByCurrency" value-class="balance" />
            <div class="kpi-foot">收入减同币种已确认应付，不做汇率折算</div>
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12" :xl="6">
          <a-card :bordered="false" title="服务范围">
            <div class="scope-value">{{ data?.overview.ownerEnabled ?? 0 }} / {{ data?.overview.ownerTotal ?? 0 }}</div>
            <div class="scope-label">启用货主 / 全部货主</div>
            <div class="kpi-foot">
              物流产品 {{ data?.overview.productEnabled ?? 0 }} / {{ data?.overview.productTotal ?? 0 }}
            </div>
          </a-card>
        </a-col>
      </a-row>

      <a-card :bordered="false" title="收支趋势（按月、按币种）" class="section-card">
        <v-chart :option="trendOption" autoresize style="height: 320px" />
      </a-card>

      <div class="section-toolbar">
        <strong>物流产品与货主收入</strong>
        <a-select v-model:value="analysisCurrency" style="width: 120px" :options="currencyOptions" />
      </div>
      <a-row :gutter="[16, 16]" class="section-card">
        <a-col :xs="24" :lg="14">
          <a-card :bordered="false" title="物流产品使用 TOP10">
            <v-chart :option="productBarOption" autoresize style="height: 320px" />
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="10">
          <a-card :bordered="false" title="收入构成（按产品）">
            <v-chart :option="incomePieOption" autoresize style="height: 320px" />
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="[16, 16]" class="section-card">
        <a-col :xs="24" :lg="12">
          <a-card :bordered="false" :title="`货主收入贡献 TOP10（${analysisCurrency}）`">
            <v-chart :option="ownerIncomeOption" autoresize style="height: 300px" />
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-card :bordered="false" title="货主出库吞吐 TOP10（单）">
            <v-chart :option="ownerOutboundOption" autoresize style="height: 300px" />
          </a-card>
        </a-col>
      </a-row>

      <a-card :bordered="false">
        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :md="8">
            <a-statistic title="名下货主在库总件数" :value="data?.scale.onHandQty ?? 0" suffix="件" />
          </a-col>
          <a-col :xs="24" :md="8">
            <a-statistic title="当前有效排数" :value="data?.scale.rackCount ?? 0" suffix="排" />
          </a-col>
          <a-col :xs="24" :md="8">
            <a-statistic
              title="货架月租成本合计"
              :value="formatCurrency(data?.scale.rackMonthlyFee, data?.scale.rackMonthlyFeeCurrency)"
            />
          </a-col>
        </a-row>
      </a-card>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import type { EChartsOption } from 'echarts'
import { Empty } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { getOperatorDashboardData } from '@/api/wms/operator-dashboard'
import type { CurrencyAmountVO, OperatorDashboardVO } from '@/api/wms/operator-dashboard/types'
import { listErpTenants } from '@/api/tenant'
import { formatCurrency } from '@/views/wms/finance-income/currency'
import { buildTrendSeries } from './dashboard-series'

use([CanvasRenderer, BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const loading = ref(false)
const data = ref<OperatorDashboardVO | null>(null)
const updatedAt = ref('')
const analysisCurrency = ref('CNY')
const monthRange = ref<[string, string]>([
  dayjs().subtract(5, 'month').format('YYYY-MM'),
  dayjs().format('YYYY-MM')
])
const selectedOwners = ref<number[]>([])
const ownerOptions = ref<{ value: number; label: string }[]>([])

const MoneyList = defineComponent({
  props: {
    items: { type: Array as () => CurrencyAmountVO[], default: () => [] },
    valueClass: { type: String, default: '' }
  },
  setup(props) {
    return () =>
      props.items.length
        ? h(
            'div',
            { class: 'money-list' },
            props.items.map(item =>
              h('div', { class: 'money-line' }, [
                h('span', item.currency),
                h('strong', { class: props.valueClass }, formatCurrency(item.amount, item.currency))
              ])
            )
          )
        : h(Empty, { image: simpleImage, description: '暂无数据' })
  }
})

async function loadOwnerOptions() {
  const res = await listErpTenants()
  if (isSuccess(res) && res.data) {
    ownerOptions.value = res.data.map(t => ({ value: t.id, label: t.tenantName }))
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getOperatorDashboardData({
      monthStart: monthRange.value[0],
      monthEnd: monthRange.value[1],
      erpTenantIds: selectedOwners.value.length ? selectedOwners.value : undefined
    })
    if (isSuccess(res) && res.data) {
      data.value = res.data
      updatedAt.value = dayjs().format('HH:mm:ss')
    }
  } finally {
    loading.value = false
  }
}

const currencyOptions = computed(() => {
  const currencies = new Set<string>()
  data.value?.products.forEach(item => currencies.add(item.currency))
  data.value?.ownerIncomeTop.forEach(item => currencies.add(item.currency))
  data.value?.overview.incomeByCurrency.forEach(item => currencies.add(item.currency))
  return [...currencies].sort().map(currency => ({ label: currency, value: currency }))
})

watch(currencyOptions, options => {
  if (options.length && !options.some(item => item.value === analysisCurrency.value)) {
    analysisCurrency.value = options[0].value
  }
})

const trendOption = computed<EChartsOption>(() => {
  const trend = data.value?.trend
  const series = trend ? buildTrendSeries(trend) : []
  return {
    tooltip: { trigger: 'axis' },
    legend: { type: 'scroll' },
    grid: { left: 64, right: 24, top: 56, bottom: 28 },
    xAxis: { type: 'category', data: trend?.months ?? [] },
    yAxis: { type: 'value', name: '金额' },
    series
  }
})

function hBar(names: string[], values: number[], color: string): EChartsOption {
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 56, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: [...names].reverse() },
    series: [{ type: 'bar', data: [...values].reverse(), itemStyle: { color }, label: { show: true, position: 'right' } }]
  }
}

const products = computed(() =>
  (data.value?.products ?? [])
    .filter(item => item.currency === analysisCurrency.value)
    .sort((a, b) => Number(b.amount) - Number(a.amount))
    .slice(0, 10)
)
const owners = computed(() =>
  (data.value?.ownerIncomeTop ?? [])
    .filter(item => item.currency === analysisCurrency.value)
    .sort((a, b) => Number(b.amount) - Number(a.amount))
    .slice(0, 10)
)

const productBarOption = computed<EChartsOption>(() =>
  hBar(products.value.map(p => p.productName), products.value.map(p => p.usageCount), '#1677ff')
)
const incomePieOption = computed<EChartsOption>(() => ({
  tooltip: {
    trigger: 'item',
    valueFormatter: (value: unknown) => formatCurrency(Number(value), analysisCurrency.value)
  },
  legend: { bottom: 0, type: 'scroll' },
  series: [{
    type: 'pie',
    radius: ['42%', '68%'],
    center: ['50%', '44%'],
    itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
    label: { formatter: '{b}\n{d}%' },
    data: products.value.filter(p => Number(p.amount) > 0).map(p => ({ name: p.productName, value: p.amount }))
  }]
}))
const ownerIncomeOption = computed<EChartsOption>(() =>
  hBar(
    owners.value.map(o => o.ownerName || `货主${o.erpTenantId}`),
    owners.value.map(o => Number(o.amount)),
    '#52c41a'
  )
)
const ownerOutboundOption = computed<EChartsOption>(() =>
  hBar(
    (data.value?.ownerOutboundTop ?? []).map(o => o.ownerName || `货主${o.erpTenantId}`),
    (data.value?.ownerOutboundTop ?? []).map(o => o.orders),
    '#722ed1'
  )
)

onMounted(() => {
  loadOwnerOptions()
  loadData()
})
</script>

<script lang="ts">
export default { name: 'OperatorDashboardPage' }
</script>

<style scoped>
.filter-card,
.section-card,
.overview-row {
  margin-bottom: 16px;
}
.overview-row :deep(.ant-col) {
  display: flex;
}
.overview-row :deep(.ant-card) {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
}
.overview-row :deep(.ant-card-body) {
  display: flex;
  flex: 1;
  flex-direction: column;
}
.overview-row .kpi-foot {
  margin-top: auto;
  padding-top: 8px;
}
.filter-bar,
.section-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.section-toolbar {
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}
.updated-at,
.kpi-foot,
.scope-label,
.expense-detail {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
.kpi-foot,
.expense-detail {
  margin-top: 8px;
}
.money-list {
  display: grid;
  gap: 10px;
  min-height: 52px;
}
.money-line {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}
.money-line strong {
  font-size: 20px;
}
.positive {
  color: #389e0d;
}
.warning {
  color: #d46b08;
}
.balance {
  color: #1677ff;
}
.scope-value {
  font-size: 30px;
  font-weight: 600;
}
</style>
