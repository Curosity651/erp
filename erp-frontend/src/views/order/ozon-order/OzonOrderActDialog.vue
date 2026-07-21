<template>
  <a-modal
    v-model:open="visibleProxy"
    :confirm-loading="loading"
    :mask-closable="step !== 'generating'"
    :closable="step !== 'generating'"
    :keyboard="step !== 'generating'"
    width="760px"
    @cancel="onCancel"
  >
    <template #title>准备发运</template>

    <!-- ========== 阶段一：预览 ========== -->
    <div v-if="step === 'preview'">
      <div class="date-row">
        <label class="date-label">发货日期</label>
        <a-date-picker v-model:value="departureDate" :allow-clear="false" value-format="YYYY-MM-DD" />
      </div>

      <div class="summary">
        <span>已选 {{ rows.length }} 单</span>
        <span class="ok">可发运 {{ eligible.length }}</span>
        <span class="bad">不可发运 {{ ineligible.length }}</span>
      </div>

      <div v-if="groups.length" class="section">
        <div class="section-title">将生成 {{ groups.length }} 份运单</div>
        <ul class="list">
          <li v-for="g in groups" :key="g.key">
            <span class="badge">{{ g.shopName }}</span>
            <span class="wh">{{ g.warehouseName }}</span>
            <span class="minor">{{ g.orders.length }} 单</span>
          </li>
        </ul>
      </div>

      <div v-if="ineligible.length" class="section warn">
        <div class="section-title">不可发运（{{ ineligible.length }}）</div>
        <ul class="list">
          <li v-for="o in ineligible.slice(0, 10)" :key="o.id">
            <span class="oid">{{ o.platformOrderId || '-' }}</span>
            <span class="reason">{{ o.reason }}</span>
          </li>
        </ul>
        <div v-if="ineligible.length > 10" class="minor">仅显示前 10 条…</div>
      </div>

      <a-alert type="info" show-icon class="notice">
        <template #message>
          运单由 Ozon 按「物流方式 + 发货日期」汇总当日货件生成，内容不等于所选订单集合。
        </template>
      </a-alert>
    </div>

    <!-- ========== 阶段二：生成中 ========== -->
    <div v-else-if="step === 'generating'" class="generating">
      <p class="generating__tip">正在生成运单，Ozon 侧约需 1–2 分钟，请勿关闭本窗口…</p>
      <ul class="list">
        <li v-for="a in acts" :key="a.actId">
          <span class="badge">{{ a.shopName || '店铺 ' + a.shopId }}</span>
          <span class="wh">{{ a.warehouseName || a.deliveryMethodName }}</span>
          <span :class="['status', statusCls(a.status)]">{{ statusText(a.status) }}</span>
        </li>
      </ul>
      <a-progress :percent="progressPct" :show-info="false" status="active" />
      <div class="minor">已等待 {{ elapsed }}s</div>
    </div>

    <!-- ========== 阶段三：结果 ========== -->
    <div v-else class="result">
      <div class="summary">
        <span class="ok">{{ readyActs.length }} 份运单已就绪</span>
        <span v-if="failedActs.length" class="bad">{{ failedActs.length }} 份失败</span>
      </div>
      <ul class="list">
        <li v-for="a in acts" :key="a.actId">
          <span :class="['status', statusCls(a.status)]">{{ a.status === 'READY' ? '✓' : '✗' }}</span>
          <span class="badge">{{ a.shopName || '店铺 ' + a.shopId }}</span>
          <span class="wh">{{ a.warehouseName || a.deliveryMethodName }}</span>
          <span class="minor">{{ a.departureDate }}</span>
          <a v-if="a.status === 'READY' && canDownload(a)" @click="onDownload(a)">下载</a>
          <span v-else-if="a.errorMsg" class="reason">{{ a.errorMsg }}</span>
        </li>
      </ul>
      <a-alert v-if="timedOut" type="warning" show-icon class="notice">
        <template #message>
          轮询超时（{{ POLL_TIMEOUT_MS / 1000 }}s）。运单可能仍在 Ozon 侧生成，可稍后重新点击「准备发运」，
          系统会复用已创建的运单而不会重复生成。
        </template>
      </a-alert>
    </div>

    <template #footer>
      <template v-if="step === 'preview'">
        <a-button @click="onCancel">取 消</a-button>
        <a-button type="primary" :disabled="!eligible.length" :loading="loading" @click="onGenerate">
          确认生成
        </a-button>
      </template>
      <template v-else-if="step === 'generating'">
        <a-button danger @click="onStopPolling">停止等待</a-button>
      </template>
      <template v-else>
        <a-button type="primary" @click="onCancel">完 成</a-button>
      </template>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, onUnmounted, reactive, toRefs, watch } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { createOzonActs, getOzonActBatch } from '@/api/order/ozon-order'
import type { OzonActVO, OzonActStatus, OzonOrderRejectVO } from '@/api/order/ozon-order/types'
import { isBigWarehouse } from './warehouse-type'
import { useLabelFileDownload } from '@/hooks/use-label-file-download'

/** 轮询间隔与总超时 */
const POLL_INTERVAL_MS = 3000
const POLL_TIMEOUT_MS = 180_000

const props = defineProps<{ open: boolean; rows: any[] }>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'generated'): void
}>()

const fileDownloader = useLabelFileDownload()

const state = reactive({
  visibleProxy: false,
  loading: false,
  step: 'preview' as 'preview' | 'generating' | 'result',
  departureDate: dayjs().format('YYYY-MM-DD'),
  eligible: [] as any[],
  ineligible: [] as (OzonOrderRejectVO & Record<string, any>)[],
  batchNo: '',
  acts: [] as OzonActVO[],
  elapsed: 0,
  timedOut: false
})

let pollTimer: ReturnType<typeof setTimeout> | null = null
let startedAt = 0

watch(
  () => props.open,
  v => {
    state.visibleProxy = v
    if (v) init()
    else stopPolling()
  }
)
watch(
  () => state.visibleProxy,
  v => emit('update:open', v)
)
onUnmounted(stopPolling)

/**
 * 可发运判定，与后端 OzonActService 的校验保持一致：
 * Ozon + FBS + 未锁定 + 已发货(SHIPPED) + 已打面单 + 大仓
 */
function canShip(row: any): boolean {
  return (
    (row?.platform || '').toLowerCase() === 'ozon' &&
    row?.fulfillmentType === 'FBS' &&
    row?.locked !== 1 &&
    row?.erpStatus === 'SHIPPED' &&
    !!row?.hasLabel &&
    isBigWarehouse(row?.warehouseName)
  )
}

function reasonOf(row: any): string {
  if ((row?.platform || '').toLowerCase() !== 'ozon') return '非 Ozon 平台'
  if (row?.fulfillmentType !== 'FBS') return 'FBO 订单不支持发运'
  if (row?.locked === 1) return '订单已锁定'
  if (row?.erpStatus !== 'SHIPPED') return `状态不支持：${row?.erpStatus || '-'}`
  if (!row?.warehouseName) return '缺少仓库信息'
  if (!isBigWarehouse(row?.warehouseName)) return '小仓订单无需运单'
  if (!row?.hasLabel) return '未打印面单'
  return '未知原因'
}

function init() {
  stopPolling()
  state.step = 'preview'
  state.departureDate = dayjs().format('YYYY-MM-DD')
  state.batchNo = ''
  state.acts = []
  state.elapsed = 0
  state.timedOut = false

  const rows = Array.isArray(props.rows) ? props.rows : []
  const eligible: any[] = []
  const ineligible: any[] = []
  for (const r of rows) {
    if (canShip(r)) eligible.push(r)
    else ineligible.push({ ...r, reason: reasonOf(r) })
  }
  state.eligible = eligible
  state.ineligible = ineligible
}

/** 预览分组：与后端一致，按 (店铺, 物流方式) 分组，一组一份运单 */
const groups = computed(() => {
  const map = new Map<string, { key: string; shopName: string; warehouseName: string; orders: any[] }>()
  for (const r of state.eligible) {
    const key = `${r.shopId}__${r.deliveryMethodId ?? r.warehouseName}`
    if (!map.has(key)) {
      map.set(key, {
        key,
        shopName: r.erpShopName || `店铺 ${r.shopId}`,
        warehouseName: r.warehouseName || r.deliveryMethodName || '-',
        orders: []
      })
    }
    map.get(key)!.orders.push(r)
  }
  return [...map.values()]
})

const readyActs = computed(() => state.acts.filter(a => a.status === 'READY'))
const failedActs = computed(() => state.acts.filter(a => a.status === 'FAILED'))
const progressPct = computed(() => {
  if (!state.acts.length) return 0
  const done = state.acts.filter(a => a.status === 'READY' || a.status === 'FAILED').length
  return Math.round((done / state.acts.length) * 100)
})

function isTerminal(s: OzonActStatus) {
  return s === 'READY' || s === 'FAILED'
}
function statusText(s: OzonActStatus) {
  return { CREATING: '创建中', PENDING: '生成中', READY: '已就绪', FAILED: '失败' }[s] || s
}
function statusCls(s: OzonActStatus) {
  return { CREATING: 'pending', PENDING: 'pending', READY: 'ok', FAILED: 'bad' }[s] || ''
}

async function onGenerate() {
  if (!state.eligible.length) return
  state.loading = true
  try {
    const ids = state.eligible.map(x => x.id)
    const resp = await createOzonActs(ids, state.departureDate)
    const batch = resp?.data
    state.batchNo = batch?.batchNo || ''
    state.acts = batch?.acts || []
    emit('generated')

    if (!state.acts.length) {
      state.step = 'result'
      return
    }
    if (state.acts.every(a => isTerminal(a.status))) {
      state.step = 'result'
      return
    }
    state.step = 'generating'
    startedAt = Date.now()
    state.elapsed = 0
    scheduleNextPoll()
  } finally {
    state.loading = false
  }
}

function scheduleNextPoll() {
  pollTimer = setTimeout(poll, POLL_INTERVAL_MS)
}

async function poll() {
  if (!state.batchNo) return
  state.elapsed = Math.round((Date.now() - startedAt) / 1000)

  try {
    const resp = await getOzonActBatch(state.batchNo)
    state.acts = resp?.data?.acts || state.acts
  } catch {
    // 单次轮询失败不中断，等下一轮；超时兜底会收口
  }

  if (state.acts.length && state.acts.every(a => isTerminal(a.status))) {
    stopPolling()
    state.step = 'result'
    const ok = state.acts.filter(a => a.status === 'READY').length
    if (ok) message.success(`${ok} 份运单已就绪`)
    return
  }

  if (Date.now() - startedAt >= POLL_TIMEOUT_MS) {
    stopPolling()
    state.timedOut = true
    state.step = 'result'
    return
  }
  scheduleNextPoll()
}

function stopPolling() {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

function onStopPolling() {
  stopPolling()
  state.timedOut = true
  state.step = 'result'
}

function canDownload(a: OzonActVO) {
  return fileDownloader.canDownload(a as any)
}
function onDownload(a: OzonActVO) {
  return fileDownloader.downloadSingle(a as any)
}

function onCancel() {
  stopPolling()
  state.visibleProxy = false
}

const { visibleProxy, loading, step, departureDate, eligible, ineligible, acts, elapsed, timedOut } =
  toRefs(state)
const rows = computed(() => props.rows || [])
</script>

<style scoped>
.date-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.date-label {
  font-weight: 600;
}
.summary {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}
.summary .ok {
  color: #389e0d;
}
.summary .bad {
  color: #a00;
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
  margin: 0;
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
.wh {
  color: #333;
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
.status.ok {
  color: #389e0d;
}
.status.bad {
  color: #a00;
}
.status.pending {
  color: #1677ff;
}
.generating {
  padding: 8px 0;
}
.generating__tip {
  margin-bottom: 12px;
}
.notice {
  margin-top: 12px;
}
</style>
