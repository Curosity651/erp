<template>
  <a-drawer :open="open" title="拣货单" width="620" @close="handleClose">
    <template #extra>
      <a-button type="primary" :disabled="!pickList" @click="handlePrint">
        <printer-outlined />
        打印拣货单
      </a-button>
    </template>
    <a-spin :spinning="loading">
      <template v-if="pickList">
        <a-descriptions :column="2" size="small" bordered style="margin-bottom: 16px">
          <a-descriptions-item label="出库单号">{{ pickList.outboundNo }}</a-descriptions-item>
          <a-descriptions-item label="货主">{{ pickList.ownerName }}</a-descriptions-item>
          <a-descriptions-item label="仓库">{{ pickList.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="下架模式">
            {{ PICK_MODE_TEXT[pickList.pickMode] }}
          </a-descriptions-item>
          <a-descriptions-item label="拣货员">{{ pickList.pickerName }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">
          取货清单
          <span class="hint">（按库位排序，依次取货）</span>
        </div>
        <a-table
          :data-source="pickList.allocations"
          :pagination="false"
          row-key="batchNo"
          size="small"
        >
          <a-table-column title="序" :width="48" align="center">
            <template #default="{ index }">{{ index + 1 }}</template>
          </a-table-column>
          <a-table-column title="库位" data-index="locationCode" :width="110">
            <template #default="{ record }">
              <a-tag color="blue">{{ record.locationCode }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="SKU" :width="150">
            <template #default="{ record }">
              <div>{{ record.skuCode }}</div>
              <div class="sku-name">{{ record.skuName }}</div>
            </template>
          </a-table-column>
          <a-table-column title="批次" data-index="batchNo" :width="130" />
          <a-table-column title="取货数" data-index="takeQty" :width="70" align="right" />
        </a-table>

        <div class="total-bar">
          共 {{ pickList.allocations.length }} 个库位点 · 合计取货 {{ totalTake }} 件
        </div>
      </template>
      <a-empty v-else-if="!loading" description="暂无拣货单" />
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PrinterOutlined } from '@ant-design/icons-vue'
import { isSuccess } from '@/api'
import { getPickList } from '@/api/wms/outbound-picking'
import type { PickListVO } from '@/api/wms/outbound-picking/types'
import { PICK_MODE_TEXT } from './constants'

const props = defineProps<{ open: boolean; orderId?: number }>()
const emit = defineEmits<{ (e: 'update:open', v: boolean): void }>()

const loading = ref(false)
const pickList = ref<PickListVO | null>(null)

const totalTake = computed(() =>
  (pickList.value?.allocations || []).reduce((s, a) => s + a.takeQty, 0)
)

async function load(id: number) {
  loading.value = true
  pickList.value = null
  try {
    const res = await getPickList(id)
    if (isSuccess(res) && res.data) pickList.value = res.data
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.open, props.orderId] as const,
  ([open, id]) => {
    if (open && id) load(id)
  },
  { immediate: true }
)

function esc(v: unknown): string {
  return String(v ?? '').replace(
    /[&<>"]/g,
    c => (({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }) as Record<string, string>)[c]
  )
}

function handlePrint() {
  const pl = pickList.value
  if (!pl) return
  const now = new Date().toLocaleString('zh-CN')
  const rows = (pl.allocations || [])
    .map(
      (a, i) => `<tr>
        <td class="c">${i + 1}</td>
        <td class="loc">${esc(a.locationCode)}</td>
        <td>${esc(a.skuCode)}<div class="muted">${esc(a.skuName)}</div></td>
        <td>${esc(a.batchNo)}</td>
        <td class="r">${esc(a.takeQty)}</td>
      </tr>`
    )
    .join('')
  const html = `<!doctype html><html lang="zh"><head><meta charset="utf-8"><title>拣货单-${esc(pl.outboundNo)}</title>
<style>
  * { box-sizing: border-box; }
  body { font-family: -apple-system, "Microsoft YaHei", sans-serif; color: #000; margin: 24px; }
  h1 { font-size: 20px; text-align: center; margin: 0 0 4px; }
  .sub { text-align: center; color: #666; font-size: 12px; margin-bottom: 16px; }
  .meta { width: 100%; border-collapse: collapse; margin-bottom: 12px; font-size: 13px; }
  .meta td { padding: 4px 8px; border: 1px solid #999; }
  .meta .k { background: #f2f2f2; width: 90px; font-weight: 600; }
  table.items { width: 100%; border-collapse: collapse; font-size: 13px; }
  table.items th, table.items td { border: 1px solid #999; padding: 6px 8px; }
  table.items th { background: #f2f2f2; }
  .c { text-align: center; width: 36px; }
  .r { text-align: right; width: 64px; }
  .loc { font-weight: 600; white-space: nowrap; }
  .muted { color: #666; font-size: 11px; }
  .total { margin-top: 10px; font-size: 13px; text-align: right; }
  .sign { margin-top: 36px; display: flex; justify-content: space-between; font-size: 13px; }
  @media print { body { margin: 12mm; } button { display: none; } }
</style></head><body>
  <h1>拣货单</h1>
  <div class="sub">打印时间：${esc(now)}</div>
  <table class="meta">
    <tr><td class="k">出库单号</td><td>${esc(pl.outboundNo)}</td><td class="k">下架模式</td><td>${esc(PICK_MODE_TEXT[pl.pickMode] || pl.pickMode)}</td></tr>
    <tr><td class="k">货主</td><td>${esc(pl.ownerName)}</td><td class="k">仓库</td><td>${esc(pl.warehouseName)}</td></tr>
    <tr><td class="k">拣货员</td><td colspan="3">${esc(pl.pickerName)}</td></tr>
  </table>
  <table class="items">
    <thead><tr><th class="c">序</th><th>库位</th><th>SKU</th><th>批次</th><th class="r">取货数</th></tr></thead>
    <tbody>${rows}</tbody>
  </table>
  <div class="total">共 ${(pl.allocations || []).length} 个库位点 · 合计取货 ${totalTake.value} 件</div>
  <div class="sign"><span>拣货员签字：____________</span><span>复核签字：____________</span></div>
</body></html>`
  const w = window.open('', '_blank', 'width=900,height=1000')
  if (!w) {
    message.warning('打印窗口被浏览器拦截，请允许弹出窗口后重试')
    return
  }
  w.document.write(html)
  w.document.close()
  w.focus()
  w.onload = () => {
    w.print()
  }
}

function handleClose() {
  emit('update:open', false)
}
</script>

<style scoped>
.section-title {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}
.section-title .hint {
  font-size: 12px;
  font-weight: 400;
  color: #8c8c8c;
}
.sku-name {
  font-size: 12px;
  color: #8c8c8c;
}
.total-bar {
  margin-top: 12px;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  text-align: right;
}
</style>
