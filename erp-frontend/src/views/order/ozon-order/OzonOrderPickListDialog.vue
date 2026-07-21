<template>
  <a-modal
    v-model:open="visibleProxy"
    :confirm-loading="loading"
    width="680px"
    @cancel="onCancel"
  >
    <template #title>打印拣货单</template>

    <!-- ========== 阶段一：预览 ========== -->
    <div v-if="step === 'preview'">
      <div class="summary">
        <span>已选 {{ rows.length }} 单</span>
        <span class="ok">可拣货 {{ eligible.length }}</span>
        <span class="bad">不可拣货 {{ ineligible.length }}</span>
      </div>

      <div v-if="groups.length" class="section">
        <div class="section-title">将按店铺生成 {{ groups.length }} 份拣货单</div>
        <ul class="list">
          <li v-for="g in groups" :key="g.shopId">
            <span class="badge">{{ g.shopName }}</span>
            <span class="minor">{{ g.orderCount }} 单 / {{ g.skuCount }} SKU</span>
          </li>
        </ul>
      </div>

      <div v-if="ineligible.length" class="section warn">
        <div class="section-title">不可拣货（{{ ineligible.length }}）</div>
        <ul class="list">
          <li v-for="o in ineligible.slice(0, 10)" :key="o.id">
            <span class="oid">{{ o.platformOrderId || '-' }}</span>
            <span class="reason">{{ o.reason }}</span>
          </li>
        </ul>
        <div v-if="ineligible.length > 10" class="minor">仅显示前 10 条…</div>
      </div>
    </div>

    <!-- ========== 阶段二：结果 ========== -->
    <div v-else class="result">
      <div class="summary">
        <span class="ok">{{ readyFiles.length }} 份拣货单</span>
        <span v-if="batch?.failed?.length" class="bad">{{ batch.failed.length }} 单未纳入</span>
      </div>
      <ul class="list">
        <li v-for="f in batch?.files || []" :key="f.shopId">
          <span class="badge">{{ f.shopName || '店铺 ' + f.shopId }}</span>
          <span class="fname">{{ f.fileName || f.objectKey }}</span>
          <span class="minor">{{ f.orderCount }} 单 / {{ f.skuCount }} SKU</span>
          <a v-if="canDownload(f)" @click="onDownload(f)">下载</a>
          <span v-else-if="f.errorMsg" class="reason">{{ f.errorMsg }}</span>
        </li>
      </ul>

      <div v-if="batch?.failed?.length" class="section warn">
        <div class="section-title">未纳入的订单</div>
        <ul class="list">
          <li v-for="o in batch.failed.slice(0, 10)" :key="o.id">
            <span class="oid">{{ o.platformOrderId || '-' }}</span>
            <span class="reason">{{ o.reason }}</span>
          </li>
        </ul>
      </div>
    </div>

    <template #footer>
      <template v-if="step === 'preview'">
        <a-button @click="onCancel">取 消</a-button>
        <a-button type="primary" :disabled="!eligible.length" :loading="loading" @click="onGenerate">
          生 成
        </a-button>
      </template>
      <template v-else>
        <a-button v-if="readyFiles.length > 1" @click="onDownloadAll">全部下载</a-button>
        <a-button type="primary" @click="onCancel">完 成</a-button>
      </template>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, toRefs, watch } from 'vue'
import { printOzonPickList } from '@/api/order/ozon-order'
import type { OzonPickListBatchVO, OzonPickListFileVO } from '@/api/order/ozon-order/types'
import { useLabelFileDownload } from '@/hooks/use-label-file-download'

const props = defineProps<{ open: boolean; rows: any[] }>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'generated'): void
}>()

const fileDownloader = useLabelFileDownload()

const state = reactive({
  visibleProxy: false,
  loading: false,
  step: 'preview' as 'preview' | 'result',
  eligible: [] as any[],
  ineligible: [] as any[],
  batch: null as OzonPickListBatchVO | null
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

/** 拣货单只要求 Ozon + FBS + 未锁定 + 已发货；不区分大小仓 */
function canPick(row: any): boolean {
  return (
    (row?.platform || '').toLowerCase() === 'ozon' &&
    row?.fulfillmentType === 'FBS' &&
    row?.locked !== 1 &&
    row?.erpStatus === 'SHIPPED'
  )
}

function reasonOf(row: any): string {
  if ((row?.platform || '').toLowerCase() !== 'ozon') return '非 Ozon 平台'
  if (row?.fulfillmentType !== 'FBS') return 'FBO 订单不支持拣货单'
  if (row?.locked === 1) return '订单已锁定'
  if (row?.erpStatus !== 'SHIPPED') return `状态不支持：${row?.erpStatus || '-'}`
  return '未知原因'
}

function init() {
  state.step = 'preview'
  state.batch = null
  const rows = Array.isArray(props.rows) ? props.rows : []
  const eligible: any[] = []
  const ineligible: any[] = []
  for (const r of rows) {
    if (canPick(r)) eligible.push(r)
    else ineligible.push({ ...r, reason: reasonOf(r) })
  }
  state.eligible = eligible
  state.ineligible = ineligible
}

/** 预览分组：按店铺一份 */
const groups = computed(() => {
  const map = new Map<number, { shopId: number; shopName: string; orderCount: number; skus: Set<string> }>()
  for (const r of state.eligible) {
    if (!map.has(r.shopId)) {
      map.set(r.shopId, {
        shopId: r.shopId,
        shopName: r.erpShopName || `店铺 ${r.shopId}`,
        orderCount: 0,
        skus: new Set<string>()
      })
    }
    const g = map.get(r.shopId)!
    g.orderCount += 1
    for (const it of r.items || []) {
      if (it?.skuCode) g.skus.add(it.skuCode)
    }
  }
  return [...map.values()].map(g => ({ ...g, skuCount: g.skus.size }))
})

const readyFiles = computed(() => (state.batch?.files || []).filter(f => canDownload(f)))

async function onGenerate() {
  if (!state.eligible.length) return
  state.loading = true
  try {
    const ids = state.eligible.map(x => x.id)
    const resp = await printOzonPickList(ids)
    state.batch = resp?.data as OzonPickListBatchVO
    state.step = 'result'
    emit('generated')
  } finally {
    state.loading = false
  }
}

function canDownload(f: OzonPickListFileVO) {
  return fileDownloader.canDownload(f as any)
}
function onDownload(f: OzonPickListFileVO) {
  return fileDownloader.downloadSingle(f as any)
}
async function onDownloadAll() {
  for (const f of readyFiles.value) {
    await fileDownloader.downloadSingle(f as any)
  }
}

function onCancel() {
  state.visibleProxy = false
}

const { visibleProxy, loading, step, eligible, ineligible, batch } = toRefs(state)
const rows = computed(() => props.rows || [])
</script>

<style scoped>
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
.fname {
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
</style>
