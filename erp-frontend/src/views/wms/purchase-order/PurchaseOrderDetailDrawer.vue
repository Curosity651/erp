<template>
  <a-drawer
    v-model:open="visible"
    title="采购单详情"
    :width="900"
    :body-style="{ padding: '24px' }"
  >
    <a-spin :spinning="loading">
      <template v-if="detail">
        <!-- 状态进度卡片 -->
        <div v-if="detail.orderStatus != 'DRAFT'" class="status-progress-card">
          <div class="card-header">
            <span class="order-no">{{ detail.orderNo }}</span>
            <purchase-order-status-badge :status="detail.orderStatus" />
          </div>

          <!-- 进度指示器（仅生产中状态显示） -->
          <div v-if="detail.orderStatus === 'IN_PRODUCTION'" class="progress-indicators">
            <div class="progress-item">
              <div class="progress-header">
                <send-outlined />
                <span class="progress-label">发货进度</span>
                <span class="progress-percent">{{ shippingPercent }}%</span>
              </div>
              <a-progress
                :percent="shippingPercent"
                :stroke-color="shippingPercent >= 100 ? '#13c2c2' : '#faad14'"
                :show-info="false"
                size="small"
              />
              <div class="progress-detail">
                {{ detail.totalShippedQuantity }} / {{ detail.totalQuantity }} 件
              </div>
            </div>

            <div class="progress-item">
              <div class="progress-header">
                <home-outlined />
                <span class="progress-label">入库进度</span>
                <span class="progress-percent">{{ receivingPercent }}%</span>
              </div>
              <a-progress
                :percent="receivingPercent"
                :stroke-color="receivingPercent >= 100 ? '#52c41a' : '#fa8c16'"
                :show-info="false"
                size="small"
              />
              <div class="progress-detail">
                {{ detail.totalReceivedQuantity }} / {{ detail.totalQuantity }} 件
              </div>
            </div>
          </div>

          <!-- 已完成状态展示 -->
          <div v-else-if="detail.orderStatus === 'COMPLETED'" class="completed-indicators">
            <a-tag color="cyan"><check-outlined /> 全部发货</a-tag>
            <a-tag color="green"><check-outlined /> 全部入库</a-tag>
          </div>
        </div>

        <a-divider v-if="detail.orderStatus != 'DRAFT'" />

        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-header">
            <span class="section-title">基本信息</span>
          </div>
          <a-descriptions :column="3" size="small">
            <a-descriptions-item label="采购单号">{{ detail.orderNo }}</a-descriptions-item>
            <a-descriptions-item label="订单状态">
              <purchase-order-status-badge :status="detail.orderStatus" />
            </a-descriptions-item>
            <a-descriptions-item label="供应商">{{
              detail.supplierName || '-'
            }}</a-descriptions-item>
            <a-descriptions-item label="币种"
              >{{ detail.currencyCode }} ({{ currencySymbol }})</a-descriptions-item
            >
            <a-descriptions-item label="下单日期">{{
              detail.orderDate || '-'
            }}</a-descriptions-item>
            <a-descriptions-item label="预计交货日期">{{
              detail.expectedDeliveryDate || '-'
            }}</a-descriptions-item>
            <a-descriptions-item label="实际交货日期">{{
              detail.actualDeliveryDate || '-'
            }}</a-descriptions-item>
            <a-descriptions-item label="是否含税">{{
              detail.taxIncluded === 1 ? '是' : '否'
            }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{
              detail.createTime || '-'
            }}</a-descriptions-item>
          </a-descriptions>
        </div>

        <a-divider />

        <!-- 金额与付款 -->
        <div class="detail-section">
          <div class="section-header">
            <span class="section-title">金额与付款</span>
          </div>
          <div class="amount-summary">
            <div class="amount-main">
              <span class="amount-label">合同总金额</span>
              <span class="amount-value">{{ formatAmountWithSymbol(detail.totalAmount) }}</span>
            </div>
            <div class="amount-extra">
              <span>首付比例 {{ detail.prepayRatio }}%</span>
              <a-divider type="vertical" />
              <span>尾款账期 {{ detail.balancePaymentDays || 0 }} 天</span>
            </div>
          </div>
          <div class="payment-status-row">
            <div class="payment-item">
              <div class="payment-item-header">
                <span class="payment-item-title">首付款 ({{ detail.prepayRatio }}%)</span>
                <a-badge
                  :status="detail.prepayStatus === 1 ? 'success' : 'default'"
                  :text="detail.prepayStatus === 1 ? '已付款' : '待付款'"
                />
              </div>
              <div class="payment-item-amount">
                {{ formatAmountWithSymbol(detail.prepayAmount) }}
              </div>
              <div v-if="detail.prepayStatus === 1" class="payment-item-info">
                <span>{{ detail.prepayTime }}</span>
                <a-button
                  v-if="detail.prepayVoucherFile"
                  type="link"
                  size="small"
                  @click="handleViewVoucher('PREPAY')"
                >
                  查看凭证
                </a-button>
              </div>
            </div>
            <a-divider type="vertical" class="payment-divider" />
            <div class="payment-item">
              <div class="payment-item-header">
                <span class="payment-item-title">尾款 ({{ 100 - detail.prepayRatio }}%)</span>
                <a-badge
                  :status="detail.balanceStatus === 1 ? 'success' : 'default'"
                  :text="detail.balanceStatus === 1 ? '已付款' : '待付款'"
                />
              </div>
              <div class="payment-item-amount">{{ formatAmountWithSymbol(balanceAmount) }}</div>
              <div v-if="detail.balanceStatus === 1" class="payment-item-info">
                <span>{{ detail.balancePayTime }}</span>
                <a-button
                  v-if="detail.balanceVoucherFile"
                  type="link"
                  size="small"
                  @click="handleViewVoucher('BALANCE')"
                >
                  查看凭证
                </a-button>
              </div>
            </div>
          </div>
        </div>

        <a-divider />

        <!-- 采购明细 -->
        <div class="detail-section">
          <div class="section-header">
            <span class="section-title">采购明细</span>
            <span class="section-extra">
              {{ detail.items?.length || 0 }} 个SKU，共 {{ detail.totalQuantity }} 件
              <a-divider type="vertical" />
              发货: {{ detail.totalShippedQuantity }}/{{ detail.totalQuantity }}
              <a-divider type="vertical" />
              入库: {{ detail.totalReceivedQuantity }}/{{ detail.totalQuantity }}
            </span>
          </div>
          <a-table
            :data-source="detail.items"
            :columns="itemColumns"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'skuInfo'">
                <sku-brief-cell :brief="getSkuBrief(record.skuCode)" />
              </template>
            </template>
          </a-table>
        </div>

        <a-divider />

        <!-- 附件资料 -->
        <div class="detail-section">
          <div class="section-header">
            <span class="section-title">附件资料</span>
          </div>
          <template v-if="hasAnyFiles">
            <!-- 合同附件 - 突出显示 -->
            <div v-if="detail.contractFile" class="attachment-group contract-group">
              <div class="attachment-group-header">
                <file-protect-outlined class="group-icon" />
                <span class="group-title">采购合同</span>
              </div>
              <div class="file-card contract-card" @click="handlePreviewFile(detail.contractFile)">
                <div class="file-card-icon">
                  <file-pdf-outlined
                    v-if="detail.contractFile.contentType?.includes('pdf')"
                    class="icon-pdf"
                  />
                  <file-word-outlined
                    v-else-if="
                      detail.contractFile.contentType?.includes('word') ||
                      detail.contractFile.contentType?.includes('document')
                    "
                    class="icon-word"
                  />
                  <file-image-outlined
                    v-else-if="detail.contractFile.contentType?.startsWith('image/')"
                    class="icon-image"
                  />
                  <file-outlined v-else class="icon-default" />
                </div>
                <div class="file-card-info">
                  <div class="file-card-name" :title="detail.contractFile.fileName">
                    {{ detail.contractFile.fileName }}
                  </div>
                  <div class="file-card-meta">
                    {{ formatFileSize(detail.contractFile.fileSize) }}
                  </div>
                </div>
                <div class="file-card-actions" @click.stop>
                  <a-tooltip title="预览">
                    <a-button
                      type="text"
                      size="small"
                      @click="handlePreviewFile(detail.contractFile)"
                    >
                      <eye-outlined />
                    </a-button>
                  </a-tooltip>
                  <a-tooltip title="下载">
                    <a-button
                      type="text"
                      size="small"
                      @click="handleDownloadFile(detail.contractFile)"
                    >
                      <download-outlined />
                    </a-button>
                  </a-tooltip>
                </div>
              </div>
            </div>

            <!-- 其他附件 - 网格布局 -->
            <div
              v-if="detail.otherFiles && detail.otherFiles.length > 0"
              class="attachment-group other-group"
            >
              <div class="attachment-group-header">
                <paper-clip-outlined class="group-icon" />
                <span class="group-title">其他附件</span>
                <span class="group-count">{{ detail.otherFiles.length }}</span>
              </div>
              <div class="file-grid">
                <div
                  v-for="file in detail.otherFiles"
                  :key="file.id"
                  class="file-card"
                  @click="handlePreviewFile(file)"
                >
                  <div class="file-card-icon">
                    <file-pdf-outlined v-if="file.contentType?.includes('pdf')" class="icon-pdf" />
                    <file-word-outlined
                      v-else-if="
                        file.contentType?.includes('word') || file.contentType?.includes('document')
                      "
                      class="icon-word"
                    />
                    <file-excel-outlined
                      v-else-if="
                        file.contentType?.includes('sheet') || file.contentType?.includes('excel')
                      "
                      class="icon-excel"
                    />
                    <file-image-outlined
                      v-else-if="file.contentType?.startsWith('image/')"
                      class="icon-image"
                    />
                    <file-outlined v-else class="icon-default" />
                  </div>
                  <div class="file-card-info">
                    <div class="file-card-name" :title="file.fileName">
                      {{ file.fileName }}
                    </div>
                    <div class="file-card-meta">
                      {{ formatFileSize(file.fileSize) }}
                    </div>
                  </div>
                  <div class="file-card-actions" @click.stop>
                    <a-tooltip title="预览">
                      <a-button type="text" size="small" @click="handlePreviewFile(file)">
                        <eye-outlined />
                      </a-button>
                    </a-tooltip>
                    <a-tooltip title="下载">
                      <a-button type="text" size="small" @click="handleDownloadFile(file)">
                        <download-outlined />
                      </a-button>
                    </a-tooltip>
                  </div>
                </div>
              </div>
            </div>
          </template>
          <a-empty v-else description="暂无附件" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
        </div>

        <a-divider />

        <!-- 质检数据 -->
        <div class="detail-section">
          <div class="section-header">
            <span class="section-title">质检数据</span>
          </div>
          <a-table
            v-if="detail.qcItems && detail.qcItems.length > 0"
            :data-source="detail.qcItems"
            :columns="qcColumns"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'skuInfo'">
                <sku-brief-cell :brief="getSkuBrief(record.skuCode)" />
              </template>
            </template>
          </a-table>
          <a-empty v-else description="暂无质检数据" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
        </div>

        <!-- 备注 -->
        <template v-if="detail.remark">
          <a-divider />
          <div class="detail-section">
            <div class="section-header">
              <span class="section-title">备注</span>
            </div>
            <div class="remark-content">{{ detail.remark }}</div>
          </div>
        </template>

        <a-divider />

        <!-- 关联物流单 -->
        <div class="detail-section">
          <div class="section-header">
            <span class="section-title">关联物流单</span>
            <span v-if="shippingOrders.length > 0" class="section-extra">
              共 {{ shippingOrders.length }} 个物流单
            </span>
          </div>
          <a-spin :spinning="shippingLoading">
            <a-table
              v-if="shippingOrders.length > 0"
              :data-source="shippingOrders"
              :columns="shippingColumns"
              :pagination="false"
              size="small"
              row-key="id"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'shippingNo'">
                  <a @click="handleViewShippingOrder(record)">{{ record.shippingNo }}</a>
                </template>
                <template v-else-if="column.key === 'shippingInfo'">
                  <span>{{
                    ShippingMethodMap[record.shippingMethod] || record.shippingMethod
                  }}</span>
                  <span> / </span>
                  <span>{{ ShippingRouteMap[record.shippingRoute] || record.shippingRoute }}</span>
                </template>
                <template v-else-if="column.key === 'status'">
                  <shipping-order-status-badge :status="record.shippingStatus" />
                </template>
                <template v-else-if="column.key === 'progress'">
                  <span>{{ record.receivedQuantity }}/{{ record.shippedQuantity }}</span>
                </template>
                <template v-else-if="column.key === 'payment'">
                  <a-badge
                    :status="record.paymentStatus === 1 ? 'success' : 'default'"
                    :text="record.paymentStatus === 1 ? '已付' : '未付'"
                  />
                </template>
              </template>
            </a-table>
            <a-empty v-else description="暂无关联物流单" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
          </a-spin>
        </div>
      </template>
    </a-spin>

    <!-- 底部操作按钮 -->
    <template #footer>
      <div class="drawer-footer">
        <a-space>
          <a-button @click="handleClose">关闭</a-button>
          <a-button v-if="actions.confirm" type="primary" @click="handleConfirm">
            确认采购单
          </a-button>
          <a-button v-if="actions.startProduction" type="primary" @click="handleStartProduction">
            开始生产
          </a-button>
          <a-button v-if="actions.cancel" danger @click="handleCancel"> 取消采购单 </a-button>
        </a-space>
      </div>
    </template>

    <!-- 凭证查看弹窗 -->
    <voucher-view-modal ref="voucherModalRef" :currency-code="detail?.currencyCode" />

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
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message, Empty } from 'ant-design-vue'
import {
  FileOutlined,
  SendOutlined,
  HomeOutlined,
  CheckOutlined,
  EyeOutlined,
  DownloadOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  FileExcelOutlined,
  FileImageOutlined,
  FileProtectOutlined,
  PaperClipOutlined
} from '@ant-design/icons-vue'
import type {
  PurchaseOrderDetailVO,
  PaymentType,
  FileInfoVO
} from '@/api/wms/purchase-order/types'
import {
  getPurchaseOrderDetail,
  confirmPurchaseOrder,
  startProduction,
  cancelPurchaseOrder
} from '@/api/wms/purchase-order'
import { getDetailPageActions } from '@/api/wms/purchase-order/status-utils'
import PurchaseOrderStatusBadge from './components/PurchaseOrderStatusBadge.vue'
import ShippingOrderStatusBadge from '@/views/wms/shipping-order/components/ShippingOrderStatusBadge.vue'
import { getShippingOrdersByPurchaseOrderId } from '@/api/wms/shipping-order'
import type { ShippingOrderSimpleVO } from '@/api/wms/shipping-order/types'
import { ShippingMethodMap, ShippingRouteMap } from '@/api/wms/shipping-order/types'
import { getFileDownloadUrl } from '@/api/system/file'
import { doRequest } from '@/utils/axios/request'
import { isSuccess } from '@/api'
import { useAuthorize } from '@/hooks/permission'
import { getCurrencySymbol } from '@/utils/currency-utils'
import VoucherViewModal from './components/VoucherViewModal.vue'
import { SkuBriefCell } from '@/components/Sku'
import { PrivateFilePreviewModal } from '@/components/File'
import { useFilePreview } from '@/hooks/use-file-preview'

defineOptions({ name: 'PurchaseOrderDetailDrawer' })

const emits = defineEmits<{
  (e: 'update-success'): void
}>()

const router = useRouter()
const { hasPermission } = useAuthorize()

const visible = ref(false)
const loading = ref(false)
const detail = ref<PurchaseOrderDetailVO>()

// 关联物流单
const shippingOrders = ref<ShippingOrderSimpleVO[]>([])
const shippingLoading = ref(false)

const voucherModalRef = ref<InstanceType<typeof VoucherViewModal>>()

// 文件预览
const { state: previewState, openPreview, closePreview, downloadFile } = useFilePreview()

// 币种符号
const currencySymbol = computed(() => getCurrencySymbol(detail.value?.currencyCode))

// 尾款金额
const balanceAmount = computed(() => {
  if (!detail.value) return 0
  return detail.value.totalAmount - detail.value.prepayAmount
})

// 发货进度百分比
const shippingPercent = computed(() => {
  if (!detail.value || detail.value.totalQuantity <= 0) return 0
  return Math.round((detail.value.totalShippedQuantity / detail.value.totalQuantity) * 100)
})

// 入库进度百分比
const receivingPercent = computed(() => {
  if (!detail.value || detail.value.totalQuantity <= 0) return 0
  return Math.round((detail.value.totalReceivedQuantity / detail.value.totalQuantity) * 100)
})

// 是否有任何附件
const hasAnyFiles = computed(() => {
  if (!detail.value) return false
  return (
    detail.value.contractFile || (detail.value.otherFiles && detail.value.otherFiles.length > 0)
  )
})

// 获取 SKU 展示信息
const getSkuBrief = (skuCode: string): SkuBriefVO | undefined => {
  return detail.value?.skuBriefMap?.[skuCode]
}

// 操作按钮配置
const actions = computed(() => {
  if (!detail.value || !hasPermission('wms:purchase-order:edit')) {
    return { edit: false, confirm: false, startProduction: false, cancel: false }
  }
  return getDetailPageActions(detail.value.orderStatus)
})

// 明细表格列
const itemColumns = [
  { title: 'SKU信息', key: 'skuInfo', width: 240 },
  { title: '数量', dataIndex: 'quantity', width: 80, align: 'center' as const },
  {
    title: '单价',
    dataIndex: 'unitPrice',
    width: 100,
    align: 'right' as const,
    customRender: ({ text }: { text: number }) => formatAmountValue(text)
  },
  {
    title: '金额',
    dataIndex: 'amount',
    width: 120,
    align: 'right' as const,
    customRender: ({ text }: { text: number }) => formatAmountValue(text)
  },
  { title: '已发货', dataIndex: 'shippedQuantity', width: 80, align: 'center' as const },
  { title: '已入库', dataIndex: 'receivedQuantity', width: 80, align: 'center' as const },
  { title: '备注', dataIndex: 'remark', ellipsis: true }
]

// 质检数据表格列
const qcColumns = [
  { title: 'SKU信息', key: 'skuInfo', width: 200 },
  { title: '长(cm)', dataIndex: 'lengthCm', width: 80, align: 'center' as const },
  { title: '宽(cm)', dataIndex: 'widthCm', width: 80, align: 'center' as const },
  { title: '高(cm)', dataIndex: 'heightCm', width: 80, align: 'center' as const },
  { title: '毛重(KG)', dataIndex: 'grossWeightKg', width: 90, align: 'center' as const },
  { title: '净重(KG)', dataIndex: 'netWeightKg', width: 90, align: 'center' as const },
  {
    title: '质检报告',
    dataIndex: 'qcFileName',
    width: 120,
    ellipsis: true,
    customRender: ({ text }: { text: string }) => text || '-'
  }
]

// 关联物流单表格列
const shippingColumns = [
  { title: '物流单号', key: 'shippingNo', width: 160 },
  { title: '物流商', dataIndex: 'providerName', width: 120, ellipsis: true },
  { title: '物流方式/线路', key: 'shippingInfo', width: 100 },
  { title: '发货日期', dataIndex: 'shippingDate', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '到货进度', key: 'progress', width: 80, align: 'center' as const },
  { title: '付款', key: 'payment', width: 70 }
]

// 格式化金额（带符号）
const formatAmountWithSymbol = (amount: number): string => {
  const symbol = currencySymbol.value
  const formatted =
    amount?.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) ||
    '0.00'
  return `${symbol} ${formatted}`
}

// 格式化金额（仅数值）
const formatAmountValue = (amount: number): string => {
  return (
    amount?.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) ||
    '0.00'
  )
}

// 格式化文件大小
const formatFileSize = (size: number): string => {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

// 加载详情
const loadDetail = async (id: number) => {
  loading.value = true
  try {
    const res = await getPurchaseOrderDetail(id)
    if (isSuccess(res)) {
      detail.value = res.data
      // 加载关联物流单
      loadShippingOrders(id)
    }
  } catch (error) {
    message.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

// 加载关联物流单
const loadShippingOrders = async (purchaseOrderId: number) => {
  shippingLoading.value = true
  try {
    const res = await getShippingOrdersByPurchaseOrderId(purchaseOrderId)
    if (isSuccess(res)) {
      shippingOrders.value = res.data || []
    }
  } catch (error) {
    console.error('加载关联物流单失败', error)
  } finally {
    shippingLoading.value = false
  }
}

// 查看物流单详情
const handleViewShippingOrder = (record: ShippingOrderSimpleVO) => {
  router.push({ name: 'ShippingOrderDetail', query: { id: record.id } })
  visible.value = false
}

// 查看凭证
const handleViewVoucher = (type: PaymentType) => {
  if (!detail.value) return

  const isPrepay = type === 'PREPAY'
  const fileInfo = isPrepay ? detail.value.prepayVoucherFile : detail.value.balanceVoucherFile
  const amount = isPrepay ? detail.value.prepayAmount : balanceAmount.value
  const paymentTime = isPrepay ? detail.value.prepayTime : detail.value.balancePayTime

  voucherModalRef.value?.open({
    type,
    amount,
    paymentTime,
    fileInfo
  })
}

// 下载文件
const handleDownloadFile = async (file: FileInfoVO) => {
  try {
    const res = await getFileDownloadUrl(file.sysFileId)
    if (isSuccess(res) && res.data) {
      window.open(res.data, '_blank')
    }
  } catch (error) {
    message.error('获取下载链接失败')
  }
}

// 预览文件
const handlePreviewFile = (file: FileInfoVO) => {
  openPreview(file.sysFileId, file.fileName, file.contentType)
}

// 确认采购单
const handleConfirm = () => {
  if (!detail.value) return

  Modal.confirm({
    title: '确认采购单',
    content: `确定要确认采购单 ${detail.value.orderNo} 吗？确认后将无法修改基本信息。`,
    okText: '确定',
    cancelText: '取消',
    onOk: () => {
      doRequest(confirmPurchaseOrder(detail.value!.id), {
        successMessage: '确认成功',
        onSuccess: () => {
          loadDetail(detail.value!.id)
          emits('update-success')
        }
      })
    }
  })
}

// 开始生产
const handleStartProduction = () => {
  if (!detail.value) return

  Modal.confirm({
    title: '开始生产',
    content: `确定要将采购单 ${detail.value.orderNo} 标记为生产中吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: () => {
      doRequest(startProduction(detail.value!.id), {
        successMessage: '操作成功',
        onSuccess: () => {
          loadDetail(detail.value!.id)
          emits('update-success')
        }
      })
    }
  })
}

// 取消采购单
const handleCancel = () => {
  if (!detail.value) return

  Modal.confirm({
    title: '取消采购单',
    content: `确定要取消采购单 ${detail.value.orderNo} 吗？此操作不可恢复。`,
    okText: '确定',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: () => {
      doRequest(cancelPurchaseOrder(detail.value!.id), {
        successMessage: '取消成功',
        onSuccess: () => {
          loadDetail(detail.value!.id)
          emits('update-success')
        }
      })
    }
  })
}

// 关闭抽屉
const handleClose = () => {
  visible.value = false
}

defineExpose({
  open(id: number) {
    visible.value = true
    shippingOrders.value = []
    loadDetail(id)
  }
})
</script>

<style scoped>
/* 状态进度卡片 */
.status-progress-card {
  background: linear-gradient(135deg, #f6f9fc 0%, #f0f5fa 100%);
  border-radius: 8px;
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-header .order-no {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

.progress-indicators {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.progress-indicators .progress-item {
  background: white;
  border-radius: 6px;
  padding: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.progress-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: #595959;
}

.progress-header .progress-label {
  flex: 1;
  font-size: 13px;
}

.progress-header .progress-percent {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.progress-detail {
  margin-top: 8px;
  font-size: 12px;
  color: #8c8c8c;
  text-align: right;
}

.completed-indicators {
  display: flex;
  gap: 12px;
}

/* 区块样式 */
.detail-section {
  padding: 0;
}

/* 区块标题 */
.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: #262626;
}

.section-title::before {
  content: '';
  width: 3px;
  height: 14px;
  background: #1890ff;
  border-radius: 2px;
  margin-right: 8px;
}

.section-extra {
  margin-left: auto;
  font-size: 13px;
  font-weight: normal;
  color: #8c8c8c;
}

/* 金额汇总 */
.amount-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
  margin-bottom: 16px;
}

.amount-main {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.amount-main .amount-label {
  font-size: 14px;
  color: #8c8c8c;
}

.amount-main .amount-value {
  font-size: 24px;
  font-weight: 600;
  color: #f5222d;
}

.amount-extra {
  font-size: 13px;
  color: #8c8c8c;
}

/* 付款状态行 */
.payment-status-row {
  display: flex;
  align-items: stretch;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
}

.payment-item {
  flex: 1;
  padding: 0 16px;
}

.payment-item:first-child {
  padding-left: 0;
}

.payment-item:last-child {
  padding-right: 0;
}

.payment-divider {
  height: auto;
  margin: 0;
}

.payment-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.payment-item-title {
  font-size: 13px;
  color: #8c8c8c;
}

.payment-item-amount {
  font-size: 20px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 4px;
}

.payment-item-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #8c8c8c;
}

/* 文件样式 */
.file-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.file-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.file-item:hover {
  background: #f5f5f5;
}

.file-name {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1890ff;
}

.file-size {
  color: #8c8c8c;
  font-size: 12px;
}

/* 附件资料区域 - 改进样式 */
.attachment-group {
  margin-bottom: 16px;
}

.attachment-group:last-child {
  margin-bottom: 0;
}

.attachment-group-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.attachment-group-header .group-icon {
  font-size: 14px;
  color: #1890ff;
}

.attachment-group-header .group-title {
  font-size: 13px;
  font-weight: 500;
  color: #595959;
}

.attachment-group-header .group-count {
  font-size: 12px;
  color: #8c8c8c;
  background: #f0f0f0;
  padding: 0 6px;
  border-radius: 10px;
}

/* 文件卡片 */
.file-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.file-card:hover {
  background: #f5f5f5;
  border-color: #d9d9d9;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

/* 合同卡片特殊样式 */
.contract-card {
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%);
  border-color: #91d5ff;
}

.contract-card:hover {
  background: linear-gradient(135deg, #d6f0ff 0%, #e6f7ff 100%);
  border-color: #69c0ff;
}

.file-card-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 8px;
  font-size: 20px;
}

.file-card-icon .icon-pdf {
  color: #ff4d4f;
}

.file-card-icon .icon-word {
  color: #1890ff;
}

.file-card-icon .icon-excel {
  color: #52c41a;
}

.file-card-icon .icon-image {
  color: #722ed1;
}

.file-card-icon .icon-default {
  color: #8c8c8c;
}

.file-card-info {
  flex: 1;
  min-width: 0;
}

.file-card-name {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 2px;
}

.file-card-meta {
  font-size: 12px;
  color: #8c8c8c;
}

.file-card-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.file-card-actions .ant-btn {
  color: #8c8c8c;
}

.file-card-actions .ant-btn:hover {
  color: #1890ff;
  background: rgba(24, 144, 255, 0.1);
}

/* 文件网格布局 */
.file-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

@media (max-width: 768px) {
  .file-grid {
    grid-template-columns: 1fr;
  }
}

/* 备注 */
.remark-content {
  font-size: 14px;
  color: #595959;
  line-height: 1.6;
  white-space: pre-wrap;
}

/* 底部按钮 */
.drawer-footer {
  display: flex;
  justify-content: flex-end;
}

/* Descriptions 样式调整 */
.detail-section :deep(.ant-descriptions-item-label) {
  color: #8c8c8c;
  width: 100px;
}

.detail-section :deep(.ant-descriptions-item-content) {
  color: #262626;
}
</style>
