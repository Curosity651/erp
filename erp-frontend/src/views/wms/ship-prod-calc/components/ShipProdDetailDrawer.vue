<template>
  <a-drawer
    :open="open"
    :width="720"
    title="发货生产测算 · 数据链"
    placement="right"
    @close="handleClose"
  >
    <a-spin :spinning="loading">
      <template v-if="detail">
        <div class="sku-head">
          <SkuBriefCell :brief="detail.skuBrief" />
          <span class="base-date">基准日 {{ detail.baseDate }}</span>
        </div>

        <a-alert
          v-for="(w, i) in detail.warnings"
          :key="i"
          type="warning"
          show-icon
          :message="w"
          style="margin-bottom: 8px"
        />

        <!-- ⓪ 时间轴 -->
        <a-card size="small" title="⓪ 时间轴" :bordered="false" class="sec">
          <div class="timeline">
            <div class="tl-node"><span class="tl-dot" />今天<b>{{ detail.baseDate }}</b></div>
            <div class="tl-node">
              <span class="tl-dot arrive" />最早到货 ΔtC
              <b>{{ fmtDate(detail.earliestArrivalDate) }}</b>
            </div>
            <div class="tl-node">
              <span class="tl-dot done" />最早完工 ΔtE
              <b>{{ fmtDate(detail.earliestCompletionDate) }}</b>
            </div>
            <div class="tl-node">
              <span class="tl-dot out" />断货日
              <b>{{ fmtDate(detail.stockoutDate) }}</b>
            </div>
            <div class="tl-node">
              <span class="tl-dot line" />发货红线(35)
              <b>{{ fmtDate(detail.shipRedLineDate) }}</b>
            </div>
            <div class="tl-node">
              <span class="tl-dot line2" />生产红线(80)
              <b>{{ fmtDate(detail.prodRedLineDate) }}</b>
            </div>
          </div>
        </a-card>

        <!-- ① 销量指标 -->
        <a-card size="small" title="① 销量指标" :bordered="false" class="sec">
          <a-descriptions :column="3" size="small" bordered>
            <a-descriptions-item label="常态 Xt">{{ detail.xt }}</a-descriptions-item>
            <a-descriptions-item label="巅峰 Yt">{{ detail.yt }}</a-descriptions-item>
            <a-descriptions-item label="估算 Zt">{{ detail.zt }}</a-descriptions-item>
            <a-descriptions-item label="Z(t+1)">{{ detail.zt1 }}</a-descriptions-item>
            <a-descriptions-item label="Z(t+2)">{{ detail.zt2 }}</a-descriptions-item>
            <a-descriptions-item label="Z(t+3)">{{ detail.zt3 }}</a-descriptions-item>
            <a-descriptions-item label="Avg7">{{ detail.avg7 }}</a-descriptions-item>
            <a-descriptions-item label="Avg15">{{ detail.avg15 }}</a-descriptions-item>
            <a-descriptions-item label="Avg30">{{ detail.avg30 }}</a-descriptions-item>
            <a-descriptions-item label="k_base">{{ detail.kBase }}</a-descriptions-item>
            <a-descriptions-item label="峰值样本">{{ detail.peakSamples }}</a-descriptions-item>
            <a-descriptions-item label="历史天数">{{ detail.historyDays }}</a-descriptions-item>
          </a-descriptions>
          <div class="formula">
            Xt = 0.5·Avg7 + 0.3·Avg15 + 0.2·Avg30 ｜ Zt = (Xt + Yt) / 2 ｜ Z(t+a) = (Xt·k(t+a)/k_base
            + Yt) / 2
          </div>
        </a-card>

        <!-- ② 发货链 -->
        <a-card size="small" title="② 发货链（35 天规则）" :bordered="false" class="sec">
          <a-descriptions :column="2" size="small" bordered>
            <a-descriptions-item label="现货 A+B">{{ detail.overseas + detail.fbo }}</a-descriptions-item>
            <a-descriptions-item label="在途 C">{{ detail.inTransit }}</a-descriptions-item>
            <a-descriptions-item label="现货支撑 S">{{ fmtDays(detail.shipSupportDays) }} 天</a-descriptions-item>
            <a-descriptions-item label="ΔtC">{{ fmtDays(detail.shipDtC) }} 天</a-descriptions-item>
            <a-descriptions-item label="全链路支撑" :span="2">
              {{ fmtDays(detail.totalSupportDays) }} 天
            </a-descriptions-item>
            <a-descriptions-item label="计算路径" :span="2">{{ detail.shipPath }}</a-descriptions-item>
            <a-descriptions-item label="决策" :span="2">
              <a-tag v-if="detail.needShip" color="red">
                需发货 · F = Z(t+2)×45 = {{ detail.shipPlanQty }}
              </a-tag>
              <a-tag v-else color="default">暂不发货</a-tag>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- ③ 生产链 -->
        <a-card size="small" title="③ 生产链（80 天规则）" :bordered="false" class="sec">
          <a-descriptions :column="2" size="small" bordered>
            <a-descriptions-item label="基础库存 A+B+C+D">
              {{ detail.overseas + detail.fbo + detail.inTransit + detail.factoryDone }}
            </a-descriptions-item>
            <a-descriptions-item label="在制 E">{{ detail.producing }}</a-descriptions-item>
            <a-descriptions-item label="基础支撑">{{ fmtDays(detail.prodSupportDays) }} 天</a-descriptions-item>
            <a-descriptions-item label="ΔtE">{{ fmtDays(detail.prodDtE) }} 天</a-descriptions-item>
            <a-descriptions-item label="全链路支撑" :span="2">
              {{ fmtDays(detail.prodTotalSupportDays) }} 天
            </a-descriptions-item>
            <a-descriptions-item label="计算路径" :span="2">{{ detail.prodPath }}</a-descriptions-item>
            <a-descriptions-item label="决策" :span="2">
              <a-tag v-if="detail.needProduce" color="orange">
                需订货 · Q = Z(t+3)×45 = {{ detail.prodPlanQty }}
              </a-tag>
              <a-tag v-else color="default">暂不订货</a-tag>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- ④ 批次明细 -->
        <a-card size="small" title="④ 批次明细" :bordered="false" class="sec">
          <div class="batch-title">C 在途（物流单）</div>
          <a-table
            :data-source="detail.transitBatches"
            :columns="batchColumns"
            :pagination="false"
            size="small"
            row-key="label"
          />
          <div class="batch-title" style="margin-top: 12px">E 在制（采购单未发货）</div>
          <a-table
            :data-source="detail.producingBatches"
            :columns="batchColumns"
            :pagination="false"
            size="small"
            row-key="label"
          />
        </a-card>
      </template>
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'
import { isSuccess } from '@/api'
import { getShipProdDetail } from '@/api/wms/ship-prod-calc'
import type { ShipProdCalcDetailVO } from '@/api/wms/ship-prod-calc/types'

const props = defineProps<{
  open: boolean
  skuCode?: string
  baseDate?: string
}>()
const emit = defineEmits<{ (e: 'update:open', v: boolean): void }>()

const loading = ref(false)
const detail = ref<ShipProdCalcDetailVO | null>(null)

const batchColumns = [
  { title: '数量', dataIndex: 'qty', key: 'qty', width: 100 },
  { title: '预计日期', dataIndex: 'eta', key: 'eta', width: 140 },
  { title: '来源', dataIndex: 'label', key: 'label' }
]

watch(
  () => props.open,
  async open => {
    if (open && props.skuCode) {
      await load(props.skuCode)
    }
  }
)

async function load(skuCode: string) {
  loading.value = true
  detail.value = null
  try {
    const res = await getShipProdDetail(skuCode, props.baseDate)
    if (isSuccess(res)) {
      detail.value = res.data
    }
  } finally {
    loading.value = false
  }
}

function handleClose() {
  emit('update:open', false)
}

function fmtDays(v: number | null | undefined): string {
  return v === null || v === undefined ? '∞' : String(v)
}
function fmtDate(v: string | null | undefined): string {
  return v || '—'
}
</script>

<style scoped>
.sku-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.base-date {
  color: var(--ant-color-text-tertiary, rgba(0, 0, 0, 0.45));
  font-size: 12px;
}
.sec {
  margin-bottom: 12px;
  background: var(--ant-color-fill-quaternary, #fafafa);
}
.formula {
  margin-top: 8px;
  font-size: 12px;
  color: var(--ant-color-text-tertiary, rgba(0, 0, 0, 0.45));
}
.timeline {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.tl-node {
  font-size: 12px;
  color: var(--ant-color-text-secondary, rgba(0, 0, 0, 0.65));
}
.tl-node b {
  display: block;
  color: var(--ant-color-text, rgba(0, 0, 0, 0.88));
  font-size: 13px;
}
.tl-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 4px;
  background: #1677ff;
}
.tl-dot.arrive {
  background: #52c41a;
}
.tl-dot.done {
  background: #722ed1;
}
.tl-dot.out {
  background: #ff4d4f;
}
.tl-dot.line {
  background: #fa8c16;
}
.tl-dot.line2 {
  background: #d48806;
}
.batch-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
}
</style>
