<template>
  <page-container
    class="sales-outbound-form-page"
    :page-header-render="false"
    ghost
    :loading="loading"
  >
    <!-- 自定义页面头部 -->
    <template #customHeader>
      <div class="custom-page-header">
        <div class="header-left">
          <a-button type="text" @click="handleBack">
            <arrow-left-outlined />
            返回列表
          </a-button>
          <a-divider type="vertical" />
          <span class="page-title">{{ pageTitle }}</span>
          <a-tag
            v-if="isUpdateForm && orderStatus"
            :color="getStatusColor(orderStatus)"
            style="margin-left: 12px"
          >
            {{ OutboundOrderStatusMap[orderStatus] }}
          </a-tag>
        </div>
        <div class="header-right">
          <span v-if="outboundNo" class="outbound-no">出库单号：{{ outboundNo }}</span>
        </div>
      </div>
    </template>

    <!-- 页面主体内容 -->
    <a-form
      ref="formRef"
      :model="formModel"
      :label-col="{ span: 24 }"
      :wrapper-col="{ span: 24 }"
      class="form-content"
    >
      <!-- 基本信息区块 -->
      <div class="form-section">
        <div class="section-title">
          <profile-outlined class="section-icon" />
          基本信息
        </div>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12" :lg="6">
            <a-form-item label="出库单号">
              <a-input :value="outboundNo" placeholder="保存后自动生成" disabled />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <a-form-item label="平台">
              <div class="readonly-field">
                <LockOutlined class="lock-icon" />
                <span>{{ getPlatformLabel(formModel.platform) }}</span>
              </div>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <a-form-item label="出库仓库">
              <div class="readonly-field">
                <LockOutlined class="lock-icon" />
                <span>{{ warehouseName || '-' }}</span>
              </div>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <a-form-item
              label="出库日期"
              name="outboundDate"
              :rules="[{ required: true, message: '请选择出库日期', trigger: 'change' }]"
            >
              <a-date-picker
                v-model:value="outboundDateValue"
                style="width: 100%"
                value-format="YYYY-MM-DD"
                @change="handleOutboundDateChange"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="24" :lg="12">
            <a-form-item label="物流产品" name="logisticsProductId">
              <a-select
                v-model:value="formModel.logisticsProductId"
                placeholder="选填：服务商提供的物流产品（签出时按单价计物流费）"
                allow-clear
                :loading="productLoading"
                @change="hasUnsavedChanges = true"
              >
                <a-select-option v-for="p in productOptions" :key="p.id" :value="p.id">
                  <span>{{ p.productName }}</span>
                  <span style="color: #fa8c16; margin-left: 8px">₽{{ p.unitPrice }}/次</span>
                  <a-tag
                    v-for="t in p.tags.slice(0, 3)"
                    :key="t"
                    style="margin-left: 6px"
                    color="blue"
                  >
                    {{ t }}
                  </a-tag>
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="24" :lg="12">
            <a-form-item label="平台资料" name="documentMode">
              <a-radio-group v-model:value="formModel.documentMode" button-style="solid">
                <a-radio-button value="WAREHOUSE_PRINT">仓库打印</a-radio-button>
                <a-radio-button value="OWNER_PROVIDED">货主已提供</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
      </div>

      <!-- 出库明细区块 -->
      <div class="form-section">
        <div class="section-title">
          <export-outlined class="section-icon" />
          出库明细
          <a-button
            type="primary"
            size="small"
            style="margin-left: auto"
            :disabled="!formModel.platform"
            @click="handleAddOrder"
          >
            <plus-outlined />
            添加订单
          </a-button>
        </div>
        <outbound-item-table
          v-model:items="outboundItems"
          :disabled="false"
          @remove="handleRemoveItem"
        />
      </div>

      <!-- 备注区块 -->
      <div class="form-section">
        <div class="section-title">
          <edit-outlined class="section-icon" />
          备注
        </div>
        <a-form-item>
          <a-textarea
            v-model:value="formModel.remark"
            placeholder="请输入备注(可选)"
            :rows="3"
            :maxlength="500"
            show-count
          />
        </a-form-item>
      </div>
    </a-form>

    <!-- 底部操作按钮区域 -->
    <template #footer>
      <a-space>
        <a-button @click="handleCancel">取消</a-button>
        <a-button :loading="submitLoading" @click="handleSaveDraft"> 保存草稿 </a-button>
        <a-button type="primary" :loading="submitLoading" @click="handleConfirmOutbound">
          提交仓库
        </a-button>
      </a-space>
    </template>

    <!-- 选择订单弹窗 -->
    <select-order-modal ref="selectOrderModalRef" @confirm="handleOrderConfirm" />
    <!-- 库存不足明细弹窗 -->
    <stock-shortage-modal ref="stockShortageModalRef" />
  </page-container>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  ProfileOutlined,
  ExportOutlined,
  EditOutlined,
  PlusOutlined,
  LockOutlined
} from '@ant-design/icons-vue'
import PageContainer from '#/layout/components/PageContainer'
import { emitter } from '@/hooks/mitt'
import { useUnsavedChangesGuard } from '@/hooks/useUnsavedChangesGuard'
import OutboundItemTable from './components/OutboundItemTable.vue'
import type { OutboundItemFormData } from './components/OutboundItemTable.vue'
import SelectOrderModal from './components/SelectOrderModal.vue'
import StockShortageModal from './components/StockShortageModal.vue'
import {
  createSalesOutbound,
  updateSalesOutbound,
  getSalesOutboundDetail,
  confirmOutbound
} from '@/api/wms/sales-outbound'
import { listOwnerLogisticsProducts } from '@/api/wms/logistics-product'
import type { LogisticsProductVO } from '@/api/wms/logistics-product/types'
import { OutboundOrderStatusMap, OutboundOrderStatus } from '@/api/wms/sales-outbound/types'
import { getPlatformLabel } from '@/components/Platform'
import type {
  SalesOutboundDTO,
  SalesOutboundItemDTO,
  PendingOrderVO
} from '@/api/wms/sales-outbound/types'
import { isSuccess } from '@/api'

defineOptions({ name: 'SalesOutboundFormPage' })

const route = useRoute()
const router = useRouter()

// 表单引用
const formRef = ref<FormInstance>()
const selectOrderModalRef = ref<InstanceType<typeof SelectOrderModal>>()
const stockShortageModalRef = ref<InstanceType<typeof StockShortageModal>>()

// 表单模式：create | edit
const formMode = computed(() => route.params.mode as 'create' | 'edit')

// 出库单 ID
const outboundOrderId = computed(() => {
  const id = route.params.id as string
  return id ? Number(id) : undefined
})

// 页面状态
const loading = ref(false)
const submitLoading = ref(false)
const hasUnsavedChanges = ref(false)

// 未保存变更守卫
const { confirmIfDirty, resetDirty, useRouteLeaveGuard } = useUnsavedChangesGuard(hasUnsavedChanges)
useRouteLeaveGuard()

// 出库单状态
const orderStatus = ref<string>('')

// 出库单号
const outboundNo = ref<string>('')

// 仓库名称（用于只读展示）
const warehouseName = ref<string>('')

// 是否编辑模式
const isUpdateForm = computed(() => formMode.value === 'edit')

// 页面标题
const pageTitle = computed(() => {
  return isUpdateForm.value ? '编辑销售出库单' : '新建销售出库单'
})

// 日期值
const outboundDateValue = ref<string>()

// 出库明细
const outboundItems = ref<OutboundItemFormData[]>([])

// 表单模型
const formModel = reactive<SalesOutboundDTO>({
  id: undefined,
  warehouseId: undefined as unknown as number,
  platform: '',
  outboundDate: '',
  remark: undefined,
  logisticsProductId: undefined,
  documentMode: 'WAREHOUSE_PRINT',
  items: []
})

// 物流产品选项（父服务商启用中的产品）
const productOptions = ref<LogisticsProductVO[]>([])
const productLoading = ref(false)
const loadProductOptions = async () => {
  productLoading.value = true
  try {
    const res = await listOwnerLogisticsProducts()
    if (isSuccess(res) && res.data) {
      productOptions.value = res.data
    }
  } finally {
    productLoading.value = false
  }
}

// 获取状态颜色
const getStatusColor = (status: string): string => {
  const colorMap: Record<string, string> = {
    [OutboundOrderStatus.DRAFT]: 'default',
    [OutboundOrderStatus.CONFIRMED]: 'success',
    [OutboundOrderStatus.CANCELLED]: 'error'
  }
  return colorMap[status] || 'default'
}

// 处理出库日期变化
const handleOutboundDateChange = (date: string | null) => {
  formModel.outboundDate = date || ''
  hasUnsavedChanges.value = true
}

// 处理平台变化
const handlePlatformChange = () => {
  // 切换平台时清空已选订单
  if (outboundItems.value.length > 0) {
    Modal.confirm({
      title: '确认切换平台',
      content: `切换平台将清空已选的 ${outboundItems.value.length} 个订单，确定要继续吗？`,
      okText: '确定清空',
      cancelText: '取消',
      onOk: () => {
        outboundItems.value = []
        hasUnsavedChanges.value = true
      },
      onCancel: () => {
        // 恢复原平台 - 需要记录旧值
      }
    })
  } else {
    hasUnsavedChanges.value = true
  }
}

// 打开选择订单弹窗
const handleAddOrder = () => {
  const existingOrderIds = [...new Set(outboundItems.value.map(item => item.erpOrderId))]
  selectOrderModalRef.value?.open(
    formModel.platform,
    formModel.warehouseId,
    warehouseName.value,
    existingOrderIds
  )
}

// 确认选择订单
const handleOrderConfirm = (orders: PendingOrderVO[]) => {
  // 每个订单的每个 item 映射为一个出库明细
  const newItems: OutboundItemFormData[] = orders.flatMap(order =>
    (order.items || []).map(item => ({
      key: `${order.id}-${item.skuCode || item.platformItemId}`,
      erpOrderId: order.id,
      platformOrderId: order.platformOrderId,
      platform: order.platform,
      skuCode: item.skuCode || '',
      skuBrief: item.skuBrief,
      quantity: item.quantity,
      stockStatus: item.stockStatus,
      availableStock: item.availableStock
    }))
  )
  outboundItems.value = [...outboundItems.value, ...newItems]
  hasUnsavedChanges.value = true
}

// 移除明细项
const handleRemoveItem = (index: number) => {
  outboundItems.value.splice(index, 1)
  hasUnsavedChanges.value = true
}

// 校验表单
const validateForm = async (): Promise<boolean> => {
  try {
    await formRef.value?.validate()
  } catch {
    return false
  }

  // 校验明细
  if (outboundItems.value.length === 0) {
    message.error('请添加出库订单')
    return false
  }

  return true
}

// 构建提交数据
const buildSubmitData = (): SalesOutboundDTO => {
  const items: SalesOutboundItemDTO[] = outboundItems.value.map(item => ({
    erpOrderId: item.erpOrderId,
    platformOrderId: item.platformOrderId,
    skuCode: item.skuCode,
    quantity: item.quantity,
    remark: item.remark
  }))

  return {
    ...formModel,
    items
  }
}

// 提交表单
const submitForm = async (): Promise<number | null> => {
  const valid = await validateForm()
  if (!valid) return null

  const data = buildSubmitData()
  submitLoading.value = true

  try {
    if (isUpdateForm.value) {
      const result = await updateSalesOutbound(data)
      if (isSuccess(result)) {
        message.success('保存成功')
        hasUnsavedChanges.value = false
        return formModel.id!
      } else {
        message.error(result.message || '保存失败')
        return null
      }
    } else {
      const result = await createSalesOutbound(data)
      if (isSuccess(result)) {
        message.success('保存成功')
        hasUnsavedChanges.value = false
        return result.data!
      } else {
        message.error(result.message || '保存失败')
        return null
      }
    }
  } catch (error) {
    console.error('提交失败:', error)
    message.error('提交失败，请重试')
    return null
  } finally {
    submitLoading.value = false
  }
}

// 返回列表并刷新
const goBackToList = async () => {
  resetDirty()
  const currentPath = route.path
  emitter.emit('refresh-sales-outbound-list')
  await router.push('/wms/sales-outbound')
  emitter.emit('close-current-tab', currentPath)
}

// 处理返回
const handleBack = () => {
  confirmIfDirty(() => goBackToList())
}

// 处理取消
const handleCancel = () => {
  handleBack()
}

// 保存草稿
const handleSaveDraft = async () => {
  const id = await submitForm()
  if (id) {
    goBackToList()
  }
}

// 提交仓库：这里只预留库存并进入仓库作业，最终签出才扣减。
const handleConfirmOutbound = async () => {
  const grouped = new Map<string, { required: number; available: number; known: boolean; name: string }>()
  outboundItems.value.forEach(item => {
    const current = grouped.get(item.skuCode) || {
      required: 0,
      available: Number(item.availableStock || 0),
      known: item.availableStock != null,
      name: item.skuCode
    }
    current.required += Number(item.quantity || 0)
    current.available = Math.max(current.available, Number(item.availableStock || 0))
    current.known = current.known && item.availableStock != null
    grouped.set(item.skuCode, current)
  })
  const shortages = [...grouped.entries()]
    .filter(([, value]) => value.known && value.available < value.required)
    .map(([skuCode, value]) => ({
      skuCode,
      skuName: value.name,
      requiredQty: value.required,
      availableQty: value.available,
      shortage: value.required - value.available
    }))
  if (shortages.length) {
    stockShortageModalRef.value?.open(shortages)
    return
  }
  Modal.confirm({
    title: '提交仓库',
    content: '提交后将预留库存并进入海外仓下架流程，最终签出时才扣减实物库存。',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      const id = await submitForm()
      if (!id) return
      try {
        const result = await confirmOutbound(id)
        if (isSuccess(result)) {
          message.success('已提交仓库并预留库存')
          emitter.emit('refresh-sales-outbound-list')
          goBackToList()
        } else {
          // 库存不足：展示缺货明细（与详情抽屉一致）
          const shortages = result.data
          if (shortages && shortages.length > 0) {
            stockShortageModalRef.value?.open(shortages)
          } else {
            message.error(result.message || '提交仓库失败')
          }
        }
      } catch (e) {
        message.error('提交仓库失败')
      }
    }
  })
}

// 加载出库单详情
const loadOutboundDetail = async (id: number) => {
  loading.value = true
  try {
    const result = await getSalesOutboundDetail(id)
    if (isSuccess(result) && result.data) {
      const detail = result.data

      // 检查是否可以进入编辑页
      if (detail.orderStatus !== OutboundOrderStatus.DRAFT) {
        message.warning('只有草稿状态的出库单可以编辑')
        goBackToList()
        return
      }

      orderStatus.value = detail.orderStatus
      outboundNo.value = detail.outboundNo

      // 基本信息
      formModel.id = detail.id
      formModel.warehouseId = detail.warehouseId
      formModel.platform = detail.platform
      formModel.outboundDate = detail.outboundDate
      formModel.remark = detail.remark
      formModel.logisticsProductId = detail.logisticsProductId
      formModel.documentMode = detail.documentMode || 'WAREHOUSE_PRINT'
      warehouseName.value = detail.warehouseName || ''

      // 设置日期显示值
      outboundDateValue.value = detail.outboundDate

      // 出库明细
      outboundItems.value = detail.items.map(item => ({
        key: `${item.erpOrderId}-${item.skuCode}`,
        erpOrderId: item.erpOrderId,
        platformOrderId: item.platformOrderId,
        platform: detail.platform,
        skuCode: item.skuCode,
        skuBrief: item.skuBrief,
        quantity: item.quantity,
        remark: item.remark,
        stockStatus: item.stockStatus,
        availableStock: item.availableStock,
        shortage: item.shortage
      }))
    } else {
      message.error(result.message || '获取详情失败')
    }
  } catch (error) {
    console.error('加载出库单详情失败:', error)
    message.error('加载出库单详情失败')
  } finally {
    loading.value = false
  }
}

// 初始化页面
const initPage = async () => {
  if (formMode.value === 'edit' && outboundOrderId.value) {
    await loadOutboundDetail(outboundOrderId.value)
  } else if (formMode.value === 'create') {
    // 从路由参数获取平台和仓库
    const { platform, warehouseId, warehouseName: wName } = route.query
    if (!platform || !warehouseId) {
      message.warning('请从列表页新建出库单')
      router.push('/wms/sales-outbound')
      return
    }
    formModel.platform = platform as string
    formModel.warehouseId = Number(warehouseId)
    warehouseName.value = (wName as string) || ''

    // 设置默认出库日期为今天
    const today = new Date().toISOString().split('T')[0]
    outboundDateValue.value = today
    formModel.outboundDate = today
  }
}

onMounted(() => {
  initPage()
  loadProductOptions()
})
</script>

<style scoped lang="less">
@import '@/styles/antd-less-bridge.less';

// 自定义页面头部
.custom-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
}

.custom-page-header .header-left {
  display: flex;
  align-items: center;
}

.custom-page-header .page-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.custom-page-header .header-right {
  display: flex;
  align-items: center;
}

// 出库单号
.outbound-no {
  font-size: 13px;
  color: #595959;
  font-family: @code-family;
}

// 覆盖 PageContainer 默认的 padding
:deep(.ant-pro-page-container-warp) {
  padding: 0;
}

/* 表单内容区域 */
.form-content {
  flex: 1;
  overflow-y: auto;
}

/* 表单区块样式 */
.form-section {
  padding: 16px 20px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.section-icon {
  margin-right: 8px;
  color: #1890ff;
  font-size: 16px;
}

/* 表单项标签样式 */
:deep(.ant-form-item-label) {
  font-weight: 500;
}

:deep(.ant-form-item) {
  margin-bottom: 12px;
}

:deep(.ant-form-item-label > label) {
  font-size: 13px;
  color: #595959;
}

/* 字段提示 */
.field-hint {
  font-size: 12px;
  color: #faad14;
  margin-top: 4px;
}

/* 只读字段样式 */
.readonly-field {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 11px;
  background: #fafafa;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  color: #595959;
  min-height: 32px;
}

.lock-icon {
  color: #bfbfbf;
  font-size: 12px;
}
</style>
