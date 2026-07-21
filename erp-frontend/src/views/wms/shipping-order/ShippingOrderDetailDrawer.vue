<template>
  <a-drawer
    v-model:open="drawerOpen"
    title="物流单详情"
    :width="800"
    :body-style="{ paddingBottom: '80px' }"
  >
    <a-spin :spinning="loading">
      <template v-if="detail">
        <!-- 基本信息 -->
        <a-descriptions title="基本信息" :column="2" bordered size="small">
          <a-descriptions-item label="物流单号">{{ detail.shippingNo }}</a-descriptions-item>
          <a-descriptions-item label="物流状态">
            <shipping-order-status-badge :status="detail.shippingStatus" />
          </a-descriptions-item>
          <a-descriptions-item label="物流商">{{ detail.providerName }}</a-descriptions-item>
          <a-descriptions-item label="目标区域">{{
            detail.targetRegion?.regionName || '-'
          }}</a-descriptions-item>
          <a-descriptions-item label="物流方式">
            <a-tag :color="detail.shippingMethod === 'GRAY' ? 'blue' : 'green'">
              {{ ShippingMethodMap[detail.shippingMethod] }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="物流线路">{{
            ShippingRouteMap[detail.shippingRoute]
          }}</a-descriptions-item>
          <a-descriptions-item label="发货日期">{{ detail.shippingDate }}</a-descriptions-item>
          <a-descriptions-item label="预计到货">{{
            detail.estimatedArrivalDate || '-'
          }}</a-descriptions-item>
          <a-descriptions-item label="预计时效">{{
            detail.estimatedDays ? `${detail.estimatedDays} 天` : '-'
          }}</a-descriptions-item>
          <a-descriptions-item label="发货件数">{{ detail.packageCount }} 件</a-descriptions-item>
          <a-descriptions-item label="总重量">{{
            detail.totalWeight ? `${detail.totalWeight} KG` : '-'
          }}</a-descriptions-item>
        </a-descriptions>

        <!-- 费用信息 -->
        <a-descriptions title="费用信息" :column="2" bordered size="small" style="margin-top: 16px">
          <template v-if="detail.shippingMethod === 'GRAY'">
            <a-descriptions-item label="物流单价"
              >{{ detail.unitPrice }} USD/KG</a-descriptions-item
            >
          </template>
          <template v-else>
            <a-descriptions-item label="运输费用">{{ detail.shippingFee }} USD</a-descriptions-item>
            <a-descriptions-item label="杂费">{{ detail.miscFee }} USD</a-descriptions-item>
          </template>
          <a-descriptions-item label="物流总额(USD)">
            <span class="amount">{{ detail.totalAmount }} USD</span>
          </a-descriptions-item>
          <a-descriptions-item label="物流总额(CNY)">
            <span class="amount">{{ detail.totalAmountCny }} CNY</span>
          </a-descriptions-item>
          <a-descriptions-item label="付款状态">
            <a-badge
              :status="detail.paymentStatus === 1 ? 'success' : 'default'"
              :text="detail.paymentStatus === 1 ? '已付' : '未付'"
            />
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.paymentVoucherFileId" label="付款凭证">
            <div class="voucher-actions">
              <a-button type="link" size="small" @click="handlePreviewVoucher">
                <eye-outlined />
                {{ detail.paymentVoucherFileName || '查看凭证' }}
              </a-button>
              <a-button type="link" size="small" @click="handleDownloadVoucher">
                <download-outlined />
                下载
              </a-button>
            </div>
          </a-descriptions-item>
        </a-descriptions>

        <!-- 货物明细 -->
        <div style="margin-top: 16px">
          <h4>货物明细</h4>
          <a-table
            :data-source="detail.items"
            :columns="itemColumns"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'sku'">
                <sku-brief-cell :brief="record.skuBrief" />
              </template>
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

        <!-- 关联入库单 -->
        <div v-if="relatedInbounds.length > 0" style="margin-top: 16px">
          <h4>关联入库单</h4>
          <a-table
            :data-source="relatedInbounds"
            :columns="inboundColumns"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.orderStatus === 'CONFIRMED' ? 'green' : 'default'">
                  {{ record.orderStatus === 'CONFIRMED' ? '已确认' : '草稿' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'operate'">
                <a @click="handleViewInbound(record)">查看</a>
              </template>
            </template>
          </a-table>
        </div>
      </template>
    </a-spin>

    <!-- 底部操作按钮 -->
    <template #footer>
      <div class="drawer-footer">
        <a-space>
          <a-button @click="handleClose">关闭</a-button>
          <a-button v-if="detail && canEdit" type="primary" @click="handleEdit"> 编辑 </a-button>
          <a-button v-if="detail && canConfirmPayment" type="primary" @click="handleConfirmPayment">
            确认付款
          </a-button>
          <a-button v-if="detail && canCreateInbound" type="primary" @click="handleCreateInbound">
            创建入库单
          </a-button>
        </a-space>
      </div>
    </template>
  </a-drawer>

  <!-- 确认付款弹窗 -->
  <payment-confirm-modal ref="paymentModalRef" @success="handlePaymentSuccess" />

  <!-- 文件预览弹窗 -->
  <private-file-preview-modal
    v-bind="previewState"
    @update:open="
      (val: boolean) => {
        if (!val) closePreview()
      }
    "
    @close="closePreview"
    @download="downloadFile"
  />
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { EyeOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import PaymentConfirmModal from './components/PaymentConfirmModal.vue'
import ShippingOrderStatusBadge from './components/ShippingOrderStatusBadge.vue'
import { SkuBriefCell } from '@/components/Sku'
import { useFilePreview } from '@/hooks/use-file-preview'
import { PrivateFilePreviewModal } from '@/components/File'
import { isSuccess } from '@/api'
import { getShippingOrderDetail, getRelatedInbounds } from '@/api/wms/shipping-order'
import { getFileDownloadUrl } from '@/api/system/file'
import { ShippingMethodMap, ShippingRouteMap, ShippingStatus } from '@/api/wms/shipping-order/types'
import type { ShippingOrderDetailVO, RelatedInboundVO } from '@/api/wms/shipping-order/types'
import { useAuthorize } from '@/hooks/permission'

const emits = defineEmits<{
  (e: 'update-success'): void
}>()

const router = useRouter()
const { hasPermission } = useAuthorize()

const drawerOpen = ref(false)
const loading = ref(false)
const detail = ref<ShippingOrderDetailVO | null>(null)
const relatedInbounds = ref<RelatedInboundVO[]>([])
const paymentModalRef = ref<InstanceType<typeof PaymentConfirmModal>>()

// 文件预览
const { state: previewState, openPreview, closePreview, downloadFile } = useFilePreview()

// 货物明细表格列
const itemColumns = [
  { title: '采购单号', dataIndex: 'purchaseOrderNo', width: 140 },
  { title: 'SKU', key: 'sku', width: 240 },
  { title: '发货数量', dataIndex: 'quantity', width: 100, align: 'center' },
  { title: '已到货', dataIndex: 'receivedQuantity', width: 100, align: 'center' }
]

// 关联入库单表格列
const inboundColumns = [
  { title: '入库单号', dataIndex: 'inboundNo', width: 140 },
  { title: '入库仓库', dataIndex: 'warehouseName', width: 120 },
  { title: '入库日期', dataIndex: 'inboundDate', width: 100 },
  { title: 'SKU数', dataIndex: 'skuCount', width: 80, align: 'center' },
  { title: '入库数量', dataIndex: 'totalQuantity', width: 100, align: 'center' },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'operate', width: 80 }
]

// 是否可编辑
const canEdit = computed(() => {
  if (!detail.value) return false
  return (
    detail.value.shippingStatus !== ShippingStatus.COMPLETED &&
    hasPermission('wms:shipping-order:edit')
  )
})

// 是否可确认付款
const canConfirmPayment = computed(() => {
  if (!detail.value) return false
  return detail.value.paymentStatus === 0 && hasPermission('wms:shipping-order:edit')
})

// 是否可创建入库单
const canCreateInbound = computed(() => {
  if (!detail.value) return false
  // 已发货状态及之后（非待发货、非已完成）可以创建入库单
  const allowedStatuses = [
    ShippingStatus.SHIPPED,
    ShippingStatus.PARTIAL_ARRIVED,
    ShippingStatus.ALL_ARRIVED
  ]
  return (
    allowedStatuses.includes(detail.value.shippingStatus as ShippingStatus) &&
    hasPermission('wms:purchase-inbound:add')
  )
})

/**
 * 打开抽屉
 */
const open = async (id: number) => {
  drawerOpen.value = true
  loading.value = true
  relatedInbounds.value = []
  try {
    const result = await getShippingOrderDetail(id)
    if (isSuccess(result)) {
      detail.value = result.data

      // 加载关联入库单
      const inboundsResult = await getRelatedInbounds(id)
      if (isSuccess(inboundsResult)) {
        relatedInbounds.value = inboundsResult.data || []
      }
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
 * 关闭抽屉
 */
const handleClose = () => {
  drawerOpen.value = false
  detail.value = null
}

/**
 * 编辑
 */
const handleEdit = () => {
  if (detail.value) {
    router.push(`/wms/shipping-order/form/edit/${detail.value.id}`)
  }
}

/**
 * 确认付款
 */
const handleConfirmPayment = () => {
  if (detail.value) {
    paymentModalRef.value?.open(detail.value.id, detail.value.shippingNo)
  }
}

/**
 * 付款成功回调
 */
const handlePaymentSuccess = () => {
  if (detail.value) {
    open(detail.value.id)
  }
  emits('update-success')
}

/**
 * 预览付款凭证
 */
const handlePreviewVoucher = () => {
  if (detail.value?.paymentVoucherFileId) {
    openPreview(
      detail.value.paymentVoucherFileId,
      detail.value.paymentVoucherFileName || '付款凭证'
    )
  }
}

/**
 * 下载付款凭证
 */
const handleDownloadVoucher = async () => {
  if (!detail.value?.paymentVoucherFileId) return

  try {
    const res = await getFileDownloadUrl(detail.value.paymentVoucherFileId)
    if (isSuccess(res) && res.data) {
      window.open(res.data, '_blank')
    }
  } catch (error) {
    message.error('获取下载链接失败')
  }
}

/**
 * 创建入库单
 */
const handleCreateInbound = () => {
  if (detail.value) {
    drawerOpen.value = false
    // 跳转到入库单新建页面，通过 query 传递物流单ID
    router.push({
      path: '/wms/purchase-inbound/form/create',
      query: { shippingOrderId: detail.value.id.toString() }
    })
  }
}

/**
 * 查看入库单详情
 */
const handleViewInbound = (record: RelatedInboundVO) => {
  drawerOpen.value = false
  router.push(`/wms/purchase-inbound/detail/${record.id}`)
}

defineExpose({ open })
</script>

<style scoped>
.amount {
  font-weight: 600;
  color: #f5222d;
}

.drawer-footer {
  text-align: right;
}
</style>
