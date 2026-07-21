<template>
  <a-drawer
    v-model:open="visible"
    title="销售出库单详情"
    :width="700"
    :body-style="{ paddingBottom: '80px' }"
  >
    <a-spin :spinning="loading">
      <template v-if="detail">
        <!-- 基本信息 -->
        <a-descriptions title="基本信息" :column="2" bordered size="small">
          <a-descriptions-item label="出库单号">{{ detail.outboundNo }}</a-descriptions-item>
          <a-descriptions-item label="单据状态">
            <sales-outbound-status-badge :status="detail.orderStatus" />
          </a-descriptions-item>
          <a-descriptions-item label="平台">
            <platform-tag :platform="detail.platform" />
          </a-descriptions-item>
          <a-descriptions-item label="出库仓库">{{
            detail.warehouseName || '-'
          }}</a-descriptions-item>
          <a-descriptions-item label="出库日期">{{ detail.outboundDate }}</a-descriptions-item>
          <a-descriptions-item label="订单数量">{{ detail.orderCount }}</a-descriptions-item>
          <a-descriptions-item label="SKU数量">{{ detail.skuCount }}</a-descriptions-item>
          <a-descriptions-item label="出库总数量">{{ detail.totalQuantity }}</a-descriptions-item>
          <a-descriptions-item label="创建人">{{ detail.createByName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detail.createTime }}</a-descriptions-item>
        </a-descriptions>

        <a-descriptions
          v-if="hasLogisticsInfo"
          title="物流与签出"
          :column="2"
          bordered
          size="small"
          style="margin-top: 16px"
        >
          <a-descriptions-item label="物流产品">
            {{ detail.logisticsProductName || '未选择' }}
          </a-descriptions-item>
          <a-descriptions-item label="物流渠道">{{ detail.channelName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="计费重量">
            {{ detail.weight != null ? `${detail.weight} kg` : '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="物流费用">
            {{ detail.shippingFee != null ? `₽${detail.shippingFee}` : '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="运单号" :span="2">
            {{ detail.trackingNo || '未填写' }}
          </a-descriptions-item>
        </a-descriptions>

        <!-- 库存不足预警 -->
        <a-alert
          v-if="detail.orderStatus === OutboundOrderStatus.DRAFT && detail.hasStockShortage"
          type="warning"
          show-icon
          style="margin-top: 16px"
        >
          <template #message>
            <span>
              存在 <strong>{{ detail.shortageSkuCount }}</strong> 个SKU库存不足，请确认后再出库
            </span>
          </template>
        </a-alert>

        <!-- 视图切换 -->
        <div style="margin-top: 16px">
          <div class="view-header">
            <h4>出库明细</h4>
            <a-radio-group v-model:value="viewMode" size="small">
              <a-radio-button value="detail">订单+SKU</a-radio-button>
              <a-radio-button value="summary">SKU汇总</a-radio-button>
            </a-radio-group>
          </div>

          <!-- 订单+SKU 视图 -->
          <a-table
            v-if="viewMode === 'detail'"
            :data-source="detail.items"
            :columns="detailColumns"
            :pagination="false"
            size="small"
            row-key="id"
            :scroll="{ x: 600 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'orderInfo'">
                <div class="order-info-cell">
                  <div class="platform-order-id">{{ record.platformOrderId }}</div>
                </div>
              </template>
              <template v-else-if="column.key === 'skuInfo'">
                <sku-brief-cell :brief="record.skuBrief" />
              </template>
              <template v-else-if="column.key === 'stockStatus'">
                <StockStatusCell
                  v-if="record.stockStatus"
                  :status="record.stockStatus"
                  :available-stock="record.availableStock"
                  :shortage="record.shortage"
                />
                <span v-else class="no-stock-info">-</span>
              </template>
            </template>
            <template #summary>
              <a-table-summary fixed>
                <a-table-summary-row>
                  <a-table-summary-cell :index="0" :col-span="2" align="right">
                    <strong>合计</strong>
                  </a-table-summary-cell>
                  <a-table-summary-cell :index="2" align="center">
                    <strong>{{ totalQuantity }}</strong>
                  </a-table-summary-cell>
                </a-table-summary-row>
              </a-table-summary>
            </template>
          </a-table>

          <!-- SKU汇总 视图 -->
          <a-table
            v-else
            :data-source="skuSummaryData"
            :columns="summaryColumns"
            :pagination="false"
            size="small"
            row-key="skuCode"
            :scroll="{ x: 500 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'skuInfo'">
                <sku-brief-cell :brief="record.skuBrief" />
              </template>
            </template>
            <template #summary>
              <a-table-summary fixed>
                <a-table-summary-row>
                  <a-table-summary-cell :index="0" align="right">
                    <strong>合计</strong>
                  </a-table-summary-cell>
                  <a-table-summary-cell :index="1" align="center">
                    <strong>{{ totalQuantity }}</strong>
                  </a-table-summary-cell>
                  <a-table-summary-cell :index="2" align="center">
                    <strong>{{ totalOrderCount }}</strong>
                  </a-table-summary-cell>
                </a-table-summary-row>
              </a-table-summary>
            </template>
          </a-table>
        </div>

        <!-- 备注 -->
        <a-descriptions
          v-if="detail.remark"
          title="备注"
          :column="1"
          bordered
          size="small"
          style="margin-top: 16px"
        >
          <a-descriptions-item>{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </a-spin>

    <!-- 底部操作按钮 -->
    <template #footer>
      <div class="drawer-footer">
        <!-- 左侧：危险操作 -->
        <div class="footer-left">
          <!-- 草稿：删除（不要的草稿直接删） -->
          <a-popconfirm
            v-if="canEdit"
            title="确定要删除该草稿出库单吗？"
            ok-text="确定"
            cancel-text="取消"
            @confirm="handleDelete"
          >
            <a-button danger :loading="actionLoading">删除</a-button>
          </a-popconfirm>
          <!-- 已确认（下架前）：取消出库，释放已占用库存 -->
          <a-popconfirm
            v-if="canCancel"
            title="确定要取消出库吗？取消后将释放已占用的库存。"
            ok-text="确定"
            cancel-text="取消"
            @confirm="handleCancelOutbound"
          >
            <a-button danger :loading="actionLoading">取消出库</a-button>
          </a-popconfirm>
        </div>
        <!-- 右侧：常规操作（草稿） -->
        <a-space>
          <a-button v-if="canEdit" @click="handleEdit">编辑</a-button>
          <a-popconfirm
            v-if="canEdit"
            title="确定要提交出库单吗？提交后将占用库存并进入待下架。"
            ok-text="确定"
            cancel-text="取消"
            @confirm="handleConfirmOutbound"
          >
            <a-button type="primary" :loading="actionLoading">提交</a-button>
          </a-popconfirm>
        </a-space>
      </div>
    </template>
  </a-drawer>

  <!-- 库存不足弹窗 -->
  <stock-shortage-modal ref="stockShortageModalRef" />
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import {
  getSalesOutboundDetail,
  confirmOutbound,
  cancelOutbound,
  deleteSalesOutbound
} from '@/api/wms/sales-outbound'
import { OutboundOrderStatus } from '@/api/wms/sales-outbound/types'
import type { SalesOutboundDetailVO, SalesOutboundItemVO } from '@/api/wms/sales-outbound/types'
import type { SkuBriefVO } from '@/api/wms/inventory/types'
import { useAuthorize } from '@/hooks/permission'
import SalesOutboundStatusBadge from './components/SalesOutboundStatusBadge.vue'
import StockStatusCell from './components/StockStatusCell.vue'
import StockShortageModal from './components/StockShortageModal.vue'
import { PlatformTag } from '@/components/Platform'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'

const emits = defineEmits<{
  (e: 'update-success'): void
}>()

const router = useRouter()
const { hasPermission } = useAuthorize()

const visible = ref(false)
const loading = ref(false)
const actionLoading = ref(false)
const detail = ref<SalesOutboundDetailVO | null>(null)
const viewMode = ref<'detail' | 'summary'>('detail')
const stockShortageModalRef = ref<InstanceType<typeof StockShortageModal>>()

const hasLogisticsInfo = computed(() => {
  const d = detail.value
  return !!(d && (d.logisticsProductId || d.channelName || d.weight != null || d.shippingFee != null))
})

// 订单+SKU 视图列（根据状态动态显示库存列）
const detailColumns = computed(() => {
  const cols = [
    { title: '订单信息', key: 'orderInfo', width: 160 },
    { title: 'SKU信息', key: 'skuInfo', width: 200 },
    { title: '出库数量', dataIndex: 'quantity', width: 80, align: 'center' as const }
  ]
  // 草稿状态显示库存状态列
  if (detail.value?.orderStatus === OutboundOrderStatus.DRAFT) {
    cols.push({ title: '库存状态', key: 'stockStatus', width: 140 })
  }
  return cols
})

// SKU汇总 视图列
const summaryColumns = [
  { title: 'SKU信息', key: 'skuInfo', width: 250 },
  { title: '出库数量', dataIndex: 'totalQuantity', width: 100, align: 'center' as const },
  { title: '订单数', dataIndex: 'orderCount', width: 100, align: 'center' as const }
]

// SKU汇总数据
interface SkuSummary {
  skuCode: string
  skuBrief?: SkuBriefVO
  totalQuantity: number
  orderCount: number
}

const skuSummaryData = computed<SkuSummary[]>(() => {
  if (!detail.value?.items) return []

  const summaryMap = new Map<string, SkuSummary>()
  const orderSet = new Map<string, Set<number>>()

  for (const item of detail.value.items) {
    const existing = summaryMap.get(item.skuCode)
    if (existing) {
      existing.totalQuantity += item.quantity
      orderSet.get(item.skuCode)?.add(item.erpOrderId)
    } else {
      summaryMap.set(item.skuCode, {
        skuCode: item.skuCode,
        skuBrief: item.skuBrief,
        totalQuantity: item.quantity,
        orderCount: 1
      })
      orderSet.set(item.skuCode, new Set([item.erpOrderId]))
    }
  }

  // 更新订单数
  for (const [skuCode, summary] of summaryMap) {
    summary.orderCount = orderSet.get(skuCode)?.size || 0
  }

  return Array.from(summaryMap.values())
})

// 合计数量
const totalQuantity = computed(() => {
  if (!detail.value?.items) return 0
  return detail.value.items.reduce((sum, item) => sum + item.quantity, 0)
})

// 合计订单数（SKU汇总视图）
const totalOrderCount = computed(() => {
  if (!detail.value?.items) return 0
  const orderIds = new Set(detail.value.items.map(item => item.erpOrderId))
  return orderIds.size
})

// 是否可编辑/提交/删除（草稿态）
const canEdit = computed(() => {
  if (!detail.value) return false
  return (
    detail.value.orderStatus === OutboundOrderStatus.DRAFT &&
    hasPermission('wms:sales-outbound:edit')
  )
})

// 是否可取消出库（已确认、平台下架前）—— 取消将释放已占用库存
const canCancel = computed(() => {
  if (!detail.value) return false
  return (
    detail.value.orderStatus === OutboundOrderStatus.CONFIRMED &&
    hasPermission('wms:sales-outbound:edit')
  )
})

/**
 * 打开抽屉
 */
const open = async (id: number) => {
  visible.value = true
  loading.value = true
  viewMode.value = 'detail'
  try {
    const result = await getSalesOutboundDetail(id)
    if (isSuccess(result)) {
      detail.value = result.data
    } else {
      message.error(result.message || '获取详情失败')
    }
  } catch (e) {
    message.error('获取详情失败')
  } finally {
    loading.value = false
  }
}

/**
 * 编辑
 */
const handleEdit = () => {
  if (detail.value) {
    visible.value = false
    router.push(`/wms/sales-outbound/form/edit/${detail.value.id}`)
  }
}

/**
 * 提交出库单
 */
const handleConfirmOutbound = async () => {
  if (!detail.value) return
  actionLoading.value = true
  try {
    const result = await confirmOutbound(detail.value.id)
    if (isSuccess(result)) {
      message.success('提交成功，已进入待下架')
      emits('update-success')
      // 刷新详情
      await open(detail.value.id)
    } else {
      // 检查是否有库存不足明细
      const shortages = result.data
      if (shortages && shortages.length > 0) {
        stockShortageModalRef.value?.open(shortages)
      } else {
        message.error(result.message || '提交失败')
      }
    }
  } catch (e) {
    message.error('提交失败')
  } finally {
    actionLoading.value = false
  }
}

/**
 * 取消出库单（已确认，下架前）——释放已占用库存
 */
const handleCancelOutbound = async () => {
  if (!detail.value) return
  actionLoading.value = true
  try {
    const result = await cancelOutbound(detail.value.id)
    if (isSuccess(result)) {
      message.success('取消成功，已释放占用库存')
      emits('update-success')
      // 刷新详情
      await open(detail.value.id)
    } else {
      message.error(result.message || '取消失败')
    }
  } catch (e) {
    message.error('取消失败')
  } finally {
    actionLoading.value = false
  }
}

/**
 * 删除出库单（草稿）
 */
const handleDelete = async () => {
  if (!detail.value) return
  actionLoading.value = true
  try {
    const result = await deleteSalesOutbound([detail.value.id])
    if (isSuccess(result)) {
      message.success('删除成功')
      emits('update-success')
      visible.value = false
    } else {
      message.error(result.message || '删除失败')
    }
  } catch (e) {
    message.error('删除失败')
  } finally {
    actionLoading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.drawer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-left {
  flex-shrink: 0;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.view-header h4 {
  margin: 0;
}

.order-info-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.platform-order-id {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  color: #262626;
  font-weight: 600;
}

.no-stock-info {
  color: #bfbfbf;
}
</style>
