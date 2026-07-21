<template>
  <a-modal
    v-model:open="visibleProxy"
    :confirm-loading="loading"
    width="820px"
    :ok-button-props="{ disabled: step === 'preview' ? eligible.length === 0 : false }"
    @ok="onOk"
    @cancel="onCancel"
  >
    <template #title>打印面单</template>

    <!-- 预览阶段 -->
    <div v-if="step === 'preview'">
      <div class="summary">
        <span>已选择：{{ total }} 单</span>
        <span>可打印：{{ eligible.length }} 单</span>
        <span>不可打印：{{ ineligible.length }} 单</span>
      </div>
      <div v-if="eligible.length" class="section">
        <div class="section-title">可打印订单</div>
        <ul class="list">
          <li v-for="o in eligible.slice(0, 10)" :key="o.id">
            <span class="badge">{{ o.platform }}</span>
            <span class="oid">{{ o.platformOrderId }} (ERP:{{ o.id }})</span>
          </li>
        </ul>
        <div v-if="eligible.length > 10" class="minor">仅显示前 10 条…</div>
      </div>
      <div v-if="ineligible.length" class="section warn">
        <div class="section-title">不可打印订单</div>
        <ul class="list">
          <li v-for="o in ineligible.slice(0, 10)" :key="o.id">
            <span class="badge">{{ o.platform }}</span>
            <span class="oid">{{ o.platformOrderId || '-' }} (ERP:{{ o.id }})</span>
            <span class="reason">{{ o.reason }}</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- 结果阶段：使用共享的 LabelBatchResult 组件 -->
    <LabelBatchResult v-else :batch="result" :loading="loading" />
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, toRefs, watch } from 'vue'
import { printLabels } from '@/api/order/wb-order'
import type { LabelBatchVO } from '@/api/order/wb-order/types'
import LabelBatchResult from '@/views/order/label/LabelBatchResult.vue'

const props = defineProps<{ open: boolean; rows: any[] }>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'printed', v: LabelBatchVO): void
}>()

const state = reactive({
  visibleProxy: false,
  loading: false,
  step: 'preview' as 'preview' | 'result',
  total: 0,
  eligible: [] as any[],
  ineligible: [] as any[],
  result: null as LabelBatchVO | null
})

watch(
  () => props.open,
  v => {
    state.visibleProxy = v
    if (v) init()
  }
)
watch(
  () => state.visibleProxy,
  v => emit('update:open', v)
)

function init() {
  state.step = 'preview'
  state.result = null
  const rows = Array.isArray(props.rows) ? props.rows : []
  const eligible: any[] = []
  const ineligible: any[] = []
  for (const r of rows) {
    if (canPrint(r)) eligible.push(r)
    else ineligible.push({ ...r, reason: reasonOf(r) })
  }
  state.total = rows.length
  state.eligible = eligible
  state.ineligible = ineligible
}

function canPrint(row: any) {
  const isWb = (row?.platform || '').toLowerCase() === 'wildberries'
  if (!isWb) return false
  if (row?.locked === 1) return false
  return row?.erpStatus === 'SHIPPED' && !!row?.platformOrderId
}

function reasonOf(row: any) {
  const isWb = (row?.platform || '').toLowerCase() === 'wildberries'
  if (!isWb) return '非 Wildberries 平台'
  if (!row?.platformOrderId) return '缺少平台订单号'
  if (row?.locked === 1) return '订单已锁定'
  if (row?.erpStatus !== 'SHIPPED') return `状态不支持：${row?.erpStatus || '-'}`
  return '未知原因'
}

async function onOk() {
  if (state.step === 'preview') {
    if (!state.eligible.length) return
    const ids = state.eligible.map((x: any) => x.id)
    state.loading = true
    try {
      const resp = await printLabels(ids)
      state.result = resp?.data as LabelBatchVO
      state.step = 'result'
      emit('printed', state.result)
    } finally {
      state.loading = false
    }
    return
  }
  state.visibleProxy = false
}

function onCancel() {
  state.visibleProxy = false
}

const { visibleProxy, loading, step, total, eligible, ineligible, result } = toRefs(state)
</script>

<style scoped>
.summary {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}
.section {
  margin-top: 12px;
}
.section-title {
  font-weight: 600;
  margin-bottom: 6px;
}
.section.warn {
  background: #fff7f7;
  border: 1px solid #ffcdd2;
  border-radius: 6px;
  padding: 8px;
}
.list {
  padding-left: 16px;
}
.list li {
  display: flex;
  gap: 8px;
  align-items: center;
  margin: 4px 0;
}
.badge {
  background: #f0f0f0;
  padding: 0 6px;
  border-radius: 3px;
}
.oid {
  color: #555;
}
.reason {
  color: #a00;
}
.minor {
  color: #888;
  font-size: 12px;
}
</style>
