<template>
  <div class="operator-dashboard">
    <!-- 筛选栏 -->
    <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ padding: '12px 16px' }">
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
      <!-- A 经营总览 -->
      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :xs="12" :lg="6">
          <a-card :bordered="false">
            <a-statistic
              title="本期收入"
              :value="data?.overview.income ?? 0"
              :precision="2"
              prefix="₽"
              :value-style="{ color: '#52c41a' }"
            />
            <div class="kpi-foot">产品使用 {{ data?.overview.incomeCount ?? 0 }} 次</div>
          </a-card>
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-card :bordered="false">
            <a-statistic
              title="本期支出"
              :value="data?.overview.expense ?? 0"
              :precision="2"
              prefix="₽"
              :value-style="{ color: '#fa8c16' }"
            />
            <div class="kpi-foot">应付平台（租金+操作费）</div>
          </a-card>
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-card :bordered="false">
            <a-statistic
              title="净收益"
              :value="data?.overview.netProfit ?? 0"
              :precision="2"
              prefix="₽"
              :value-style="{ color: netColor }"
            />
            <div class="kpi-foot">收入 − 支出</div>
          </a-card>
        </a-col>
        <a-col :xs="12" :lg="6">
          <a-card :bordered="false">
            <a-statistic
              title="名下货主"
              :value="data?.overview.ownerEnabled ?? 0"
              :suffix="`/ ${data?.overview.ownerTotal ?? 0}`"
            />
            <div class="kpi-foot">
              物流产品 启用 {{ data?.overview.productEnabled ?? 0 }} / 共
              {{ data?.overview.productTotal ?? 0 }}
            </div>
          </a-card>
        </a-col>
      </a-row>

      <!-- B 收支趋势 -->
      <a-card :bordered="false" title="收支趋势（按月）" style="margin-bottom: 16px">
        <v-chart :option="trendOption" autoresize style="height: 320px" />
      </a-card>

      <!-- C 产品分析 -->
      <a-row :gutter="16" style="margin-bottom: 16px">
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

      <!-- D 货主分析 -->
      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :xs="24" :lg="12">
          <a-card :bordered="false" title="货主收入贡献 TOP10（₽）">
            <v-chart :option="ownerIncomeOption" autoresize style="height: 300px" />
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-card :bordered="false" title="货主出库吞吐 TOP10（单）">
            <v-chart :option="ownerOutboundOption" autoresize style="height: 300px" />
          </a-card>
        </a-col>
      </a-row>

      <!-- E 服务规模 -->
      <a-card :bordered="false">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-statistic
              title="名下货主在库总件数"
              :value="data?.scale.onHandQty ?? 0"
              suffix="件"
            />
          </a-col>
          <a-col :span="8">
            <a-statistic title="当前有效货架数" :value="data?.scale.rackCount ?? 0" suffix="个" />
          </a-col>
          <a-col :span="8">
            <a-statistic
              title="货架月租成本合计"
              :value="data?.scale.rackMonthlyFee ?? 0"
              :precision="2"
              prefix="₽"
            />
          </a-col>
        </a-row>
      </a-card>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import type { EChartsOption } from 'echarts'
import { isSuccess } from '@/api'
import { getOperatorDashboardData } from '@/api/wms/operator-dashboard'
import type { OperatorDashboardVO } from '@/api/wms/operator-dashboard/types'
import { listErpTenants } from '@/api/tenant'

use([
  CanvasRenderer,
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent
])

const loading = ref(false)
const data = ref<OperatorDashboardVO | null>(null)
const updatedAt = ref('')

// 筛选：默认最近 6 个月
const monthRange = ref<[string, string]>([
  dayjs().subtract(5, 'month').format('YYYY-MM'),
  dayjs().format('YYYY-MM')
])
const selectedOwners = ref<number[]>([])
const ownerOptions = ref<{ value: number; label: string }[]>([])

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
      monthStart: monthRange.value?.[0],
      monthEnd: monthRange.value?.[1],
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

const netColor = computed(() =>
  (data.value?.overview.netProfit ?? 0) >= 0 ? '#1890ff' : '#ff4d4f'
)

// B 收支趋势：收入柱(绿)+支出柱(橙)+净收益折线(蓝)
const trendOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['收入', '支出', '净收益'] },
  grid: { left: 60, right: 24, top: 40, bottom: 28 },
  xAxis: { type: 'category', data: data.value?.trend.months ?? [] },
  yAxis: { type: 'value', name: '₽' },
  series: [
    {
      name: '收入',
      type: 'bar',
      data: data.value?.trend.income ?? [],
      itemStyle: { color: '#52c41a' }
    },
    {
      name: '支出',
      type: 'bar',
      data: data.value?.trend.expense ?? [],
      itemStyle: { color: '#fa8c16' }
    },
    {
      name: '净收益',
      type: 'line',
      data: data.value?.trend.net ?? [],
      itemStyle: { color: '#1890ff' },
      lineStyle: { width: 2 }
    }
  ]
}))

// 横向条形通用构造（TOP 榜倒序显示，最大在上）
function hBar(names: string[], values: number[], color: string): EChartsOption {
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 40, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: [...names].reverse() },
    series: [
      {
        type: 'bar',
        data: [...values].reverse(),
        itemStyle: { color },
        label: { show: true, position: 'right' }
      }
    ]
  }
}

// C 左：产品使用次数 TOP
const productBarOption = computed<EChartsOption>(() =>
  hBar(
    (data.value?.products ?? []).map(p => p.productName),
    (data.value?.products ?? []).map(p => p.usageCount),
    '#1890ff'
  )
)

// C 右：收入构成环形（按产品金额）
const incomePieOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item', valueFormatter: (v: unknown) => `₽ ${Number(v).toFixed(2)}` },
  legend: { bottom: 0, type: 'scroll' },
  series: [
    {
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{d}%' },
      data: (data.value?.products ?? [])
        .filter(p => Number(p.amount) > 0)
        .map(p => ({ name: p.productName, value: p.amount }))
    }
  ]
}))

// D 左：货主收入贡献
const ownerIncomeOption = computed<EChartsOption>(() =>
  hBar(
    (data.value?.ownerIncomeTop ?? []).map(o => o.ownerName || `货主${o.erpTenantId}`),
    (data.value?.ownerIncomeTop ?? []).map(o => Number(o.amount)),
    '#52c41a'
  )
)

// D 右：货主出库吞吐
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
export default {
  name: 'OperatorDashboardPage'
}
</script>

<style scoped>
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.updated-at {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
.kpi-foot {
  margin-top: 8px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
