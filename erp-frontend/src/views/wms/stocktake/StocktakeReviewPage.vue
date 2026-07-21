<template>
  <page-container :page-header-render="false" ghost :loading="loading">
    <div class="review-head">
      <div class="head-main">
        <a-button type="text" @click="goBack"><arrow-left-outlined /></a-button>
        <div>
          <div class="title-row">
            <strong>盘点差异复核</strong>
            <a-tag color="warning">待复核</a-tag>
          </div>
          <span>{{ detail?.stocktakeNo }} · {{ detail?.warehouseName }}</span>
        </div>
      </div>
      <a-button type="primary" :loading="confirming" @click="handleConfirm">
        确认调整库存
      </a-button>
    </div>

    <div class="summary-band">
      <div><span>盘点批次</span><strong>{{ items.length }}</strong></div>
      <div><span>差异批次</span><strong>{{ diffItems.length }}</strong></div>
      <div><span>盘盈数量</span><strong class="profit">+{{ profitQuantity }}</strong></div>
      <div><span>盘亏数量</span><strong class="loss">-{{ lossQuantity }}</strong></div>
      <div><span>净差异</span><strong>{{ signed(netDiff) }}</strong></div>
    </div>

    <div class="review-table">
      <div class="table-tools">
        <a-input v-model:value="keyword" allow-clear placeholder="搜索 SKU、货主或库位" style="width: 280px">
          <template #prefix><search-outlined /></template>
        </a-input>
        <a-radio-group v-model:value="filter" button-style="solid">
          <a-radio-button value="DIFF">只看差异</a-radio-button>
          <a-radio-button value="ALL">全部明细</a-radio-button>
        </a-radio-group>
      </div>
      <a-table :columns="columns" :data-source="visibleItems" :pagination="false" row-key="id" :scroll="{ x: 900 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'sku'">
            <div class="stack"><strong>{{ record.skuCode }}</strong><small>{{ record.skuBrief?.skuName || '-' }}</small></div>
          </template>
          <template v-else-if="column.key === 'owner'">{{ record.ownerName || record.erpTenantId }}</template>
          <template v-else-if="column.key === 'batch'">
            <div class="stack"><span>{{ record.inboundDate || '-' }}</span><small>{{ record.quality === 'DAMAGED' ? '不良品' : '良品' }}</small></div>
          </template>
          <template v-else-if="column.key === 'diff'">
            <strong :class="diffClass(record.diffQuantity || 0)">{{ signed(record.diffQuantity || 0) }}</strong>
          </template>
          <template v-else-if="column.key === 'source'">
            <a-tag v-if="record.sourceType === 'ADDED'" color="orange">账外新增</a-tag>
            <span v-else>-</span>
          </template>
        </template>
      </a-table>
    </div>
  </page-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { ArrowLeftOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { confirmStocktake, getStocktakeDetail } from '@/api/wms/stocktake'
import type { StocktakeDetailVO, StocktakeItemVO } from '@/api/wms/stocktake/types'
import { isSuccess } from '@/api'

defineOptions({ name: 'StocktakeReview' })
const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const loading = ref(false)
const confirming = ref(false)
const detail = ref<StocktakeDetailVO>()
const items = ref<StocktakeItemVO[]>([])
const keyword = ref('')
const filter = ref<'DIFF' | 'ALL'>('DIFF')

const diffItems = computed(() => items.value.filter(item => (item.diffQuantity || 0) !== 0))
const profitQuantity = computed(() => diffItems.value.reduce((sum, item) => sum + Math.max(item.diffQuantity || 0, 0), 0))
const lossQuantity = computed(() => diffItems.value.reduce((sum, item) => sum + Math.abs(Math.min(item.diffQuantity || 0, 0)), 0))
const netDiff = computed(() => profitQuantity.value - lossQuantity.value)
const visibleItems = computed(() => {
  const base = filter.value === 'DIFF' ? diffItems.value : items.value
  const value = keyword.value.trim().toLowerCase()
  if (!value) return base
  return base.filter(item => [item.skuCode, item.ownerName, item.locationCode].some(text => (text || '').toLowerCase().includes(value)))
})
const columns = [
  { title: '库位', dataIndex: 'locationCode', width: 110 },
  { title: 'SKU', key: 'sku', width: 200 },
  { title: '货主', key: 'owner', width: 130 },
  { title: '批次/品质', key: 'batch', width: 130 },
  { title: '账面数', dataIndex: 'systemQuantity', align: 'right', width: 90 },
  { title: '实盘数', dataIndex: 'actualQuantity', align: 'right', width: 90 },
  { title: '差异', key: 'diff', align: 'right', width: 90 },
  { title: '来源', key: 'source', width: 110 }
]

function signed(value: number) { return value > 0 ? `+${value}` : String(value) }
function diffClass(value: number) { return value > 0 ? 'profit' : value < 0 ? 'loss' : '' }
function goBack() { router.push('/ops/stocktake') }

function handleConfirm() {
  Modal.confirm({
    title: '确认库存调整',
    content: `将按 ${diffItems.value.length} 项差异调整物理库存并记录库存流水。该操作不可撤销，确认继续吗？`,
    okText: '确认过账',
    onOk: async () => {
      confirming.value = true
      try {
        const result = await confirmStocktake(id)
        if (isSuccess(result)) {
          message.success('盘点确认完成，库存已更新')
          goBack()
        }
      } finally {
        confirming.value = false
      }
    }
  })
}

onMounted(async () => {
  loading.value = true
  try {
    const result = await getStocktakeDetail(id)
    if (isSuccess(result) && result.data) {
      detail.value = result.data
      items.value = result.data.items || []
      if (detail.value.orderStatus !== 'REVIEWING') {
        message.warning('该盘点单当前不在复核状态')
        goBack()
      }
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="less">
.review-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; padding: 14px 18px; border: 1px solid #f0f0f0; background: #fff; }
.head-main, .title-row { display: flex; align-items: center; gap: 12px; }
.title-row strong { font-size: 17px; }
.head-main span { color: #8c8c8c; font-size: 13px; }
.summary-band { display: grid; grid-template-columns: repeat(5, 1fr); margin-bottom: 12px; border: 1px solid #f0f0f0; background: #fff; }
.summary-band > div { display: flex; flex-direction: column; padding: 16px 20px; border-right: 1px solid #f0f0f0; }
.summary-band > div:last-child { border-right: 0; }
.summary-band span { color: #8c8c8c; font-size: 12px; }
.summary-band strong { margin-top: 4px; font-size: 22px; }
.profit { color: #389e0d; }
.loss { color: #cf1322; }
.review-table { padding: 16px; border: 1px solid #f0f0f0; background: #fff; }
.table-tools { display: flex; justify-content: space-between; margin-bottom: 14px; }
.stack { display: flex; flex-direction: column; }
.stack small { color: #8c8c8c; }
@media (max-width: 800px) { .summary-band { grid-template-columns: repeat(2, 1fr); } }
</style>
