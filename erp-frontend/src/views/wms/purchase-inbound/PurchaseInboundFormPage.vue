<template>
  <page-container
    class="purchase-inbound-form-page"
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
            {{ InboundStatusMap[orderStatus] }}
          </a-tag>
        </div>
      </div>
    </template>

    <!-- 表单内容 -->
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
          <a-col :xs="24" :sm="12">
            <a-form-item
              label="货主入库单号"
              extra="保存后系统会自动拼接货主名称，例如 JHIN-RK-001"
              name="inboundNo"
              :rules="[
                { required: true, message: '请输入入库单号', trigger: 'blur' },
                { max: 50, message: '入库单号长度不能超过50个字符', trigger: 'blur' }
              ]"
            >
              <a-input
                v-model:value="formModel.inboundNo"
                placeholder="请输入入库单号"
                :disabled="isUpdateForm"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item
              label="入库日期"
              name="inboundDate"
              :rules="[{ required: true, message: '请选择入库日期', trigger: 'change' }]"
            >
              <a-date-picker
                v-model:value="inboundDateValue"
                style="width: 100%"
                value-format="YYYY-MM-DD"
                @change="handleInboundDateChange"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item
              label="关联物流单"
              name="shippingOrderId"
              :rules="[{ required: true, message: '请选择关联物流单', trigger: 'change' }]"
            >
              <a-input-group compact>
                <a-input
                  :value="selectedShippingNo"
                  placeholder="请选择物流单"
                  style="width: calc(100% - 80px)"
                  readonly
                />
                <a-button type="primary" :disabled="isUpdateForm" @click="handleSelectShipping">
                  选择
                </a-button>
              </a-input-group>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item
              label="入库仓库"
              name="warehouseId"
              :rules="[{ required: true, message: '请选择入库仓库', trigger: 'change' }]"
            >
              <warehouse-select
                v-model:value="formModel.warehouseId"
                :region-id="selectedShipping?.targetRegionId"
                warehouse-type="OWN"
                :disabled="!selectedShipping"
                placeholder="请先选择物流单"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </div>

      <!-- 物流单信息区块 -->
      <shipping-info-section :shipping="selectedShipping" />

      <!-- 入库明细区块 -->
      <div class="form-section">
        <div class="section-title">
          <inbox-outlined class="section-icon" />
          入库明细
        </div>
        <inbound-item-table v-model:items="inboundItems" :disabled="false" />
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
        <a-button :loading="submitLoading" @click="handleSaveDraft">保存草稿</a-button>
        <a-button type="primary" :loading="submitLoading" @click="handleSubmit"> 提交 </a-button>
      </a-space>
    </template>

    <!-- 选择物流单弹窗 -->
    <select-shipping-modal ref="selectShippingModalRef" @confirm="handleShippingConfirm" />
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
  InboxOutlined,
  EditOutlined
} from '@ant-design/icons-vue'
import PageContainer from '#/layout/components/PageContainer'
import { emitter } from '@/hooks/mitt'
import { useUnsavedChangesGuard } from '@/hooks/useUnsavedChangesGuard'
import ShippingInfoSection from './components/ShippingInfoSection.vue'
import InboundItemTable from './components/InboundItemTable.vue'
import SelectShippingModal from './components/SelectShippingModal.vue'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import {
  createPurchaseInbound,
  updatePurchaseInbound,
  getPurchaseInboundDetail,
  submitInbound,
  getShippingItemsForInbound,
  getShippingDetail
} from '@/api/wms/purchase-inbound'
import { InboundStatusMap, InboundStatus } from '@/api/wms/purchase-inbound/types'
import type {
  PurchaseInboundDTO,
  PurchaseInboundItemDTO,
  PurchaseInboundItemVO,
  AvailableShippingVO,
  ShippingItemForInboundVO,
  InboundItemFormData
} from '@/api/wms/purchase-inbound/types'
import { doRequest } from '@/utils/axios/request'
import { isSuccess } from '@/api'

defineOptions({ name: 'PurchaseInboundFormPage' })

const route = useRoute()
const router = useRouter()

// 表单引用
const formRef = ref<FormInstance>()
const selectShippingModalRef = ref<InstanceType<typeof SelectShippingModal>>()

// 表单模式：create | edit
const formMode = computed(() => route.params.mode as 'create' | 'edit')

// 入库单 ID
const inboundOrderId = computed(() => {
  const id = route.params.id as string
  return id ? Number(id) : undefined
})

// 页面状态
const loading = ref(false)
const submitLoading = ref(false)
const hasUnsavedChanges = ref(false)

// 未保存变更守卫
const { confirmIfDirty, resetDirty, useRouteLeaveGuard } = useUnsavedChangesGuard(hasUnsavedChanges)

// 注册路由离开守卫
useRouteLeaveGuard()

// 入库单状态
const orderStatus = ref<string>('')

// 是否编辑模式
const isUpdateForm = computed(() => formMode.value === 'edit')

// 页面标题
const pageTitle = computed(() => {
  return isUpdateForm.value ? '编辑采购入库单' : '新建采购入库单'
})

// 日期值
const inboundDateValue = ref<string>()

// 选中的物流单
const selectedShipping = ref<AvailableShippingVO | undefined>(undefined)
const selectedShippingNo = computed(() => selectedShipping.value?.shippingNo || '')

// 入库明细
const inboundItems = ref<InboundItemFormData[]>([])

// 表单模型
const formModel = reactive<PurchaseInboundDTO>({
  id: undefined,
  inboundNo: '',
  shippingOrderId: undefined as unknown as number,
  warehouseId: undefined as unknown as number,
  inboundDate: '',
  remark: undefined,
  items: []
})

/**
 * 重置表单状态（用于新建模式）
 */
const resetForm = () => {
  // 重置表单模型
  formModel.id = undefined
  formModel.inboundNo = ''
  formModel.shippingOrderId = undefined as unknown as number
  formModel.warehouseId = undefined as unknown as number
  formModel.inboundDate = ''
  formModel.remark = undefined
  formModel.items = []

  // 重置关联状态
  selectedShipping.value = undefined
  inboundItems.value = []
  inboundDateValue.value = undefined
  orderStatus.value = ''
  hasUnsavedChanges.value = false

  // 重置表单校验状态
  formRef.value?.resetFields()
}

/**
 * 将物流单待入库明细映射为表单数据
 */
const mapShippingItemsToFormData = (items: ShippingItemForInboundVO[]): InboundItemFormData[] => {
  return items.map(item => ({
    shippingOrderItemId: item.shippingOrderItemId,
    purchaseOrderId: item.purchaseOrderId,
    purchaseOrderNo: item.purchaseOrderNo,
    purchaseOrderItemId: item.purchaseOrderItemId,
    skuCode: item.skuCode,
    skuBrief: item.skuBrief,
    expectedQuantity: item.pendingQuantity,
    // 建单仅声明应到数量；实到由平台收货时录入，故初始为 0
    actualQuantity: 0,
    shortQuantity: 0,
    selected: true,
    expectedDeliveryDate: item.expectedDeliveryDate
  }))
}

/**
 * 将入库单明细 VO 映射为表单数据（用于编辑时回显）
 */
const mapInboundItemsToFormData = (items: PurchaseInboundItemVO[]): InboundItemFormData[] => {
  return items.map(item => ({
    shippingOrderItemId: item.shippingOrderItemId,
    purchaseOrderId: item.purchaseOrderId,
    purchaseOrderNo: item.purchaseOrderNo,
    purchaseOrderItemId: item.purchaseOrderItemId,
    skuCode: item.skuCode,
    skuBrief: item.skuBrief,
    expectedQuantity: item.expectedQuantity,
    actualQuantity: item.actualQuantity,
    shortQuantity: item.shortQuantity,
    selected: true,
    remark: item.remark
  }))
}

// 获取状态颜色
const getStatusColor = (status: string): string => {
  const colorMap: Record<string, string> = {
    [InboundStatus.DRAFT]: 'default',
    [InboundStatus.SUBMITTED]: 'processing',
    [InboundStatus.RECEIVED]: 'warning',
    [InboundStatus.COMPLETED]: 'success',
    [InboundStatus.CANCELLED]: 'error'
  }
  return colorMap[status] || 'default'
}

// 处理入库日期变化
const handleInboundDateChange = (date: string | null) => {
  formModel.inboundDate = date || ''
  hasUnsavedChanges.value = true
}

// 打开选择物流单弹窗
const handleSelectShipping = () => {
  selectShippingModalRef.value?.open()
}

// 确认选择物流单
const handleShippingConfirm = (shipping: AvailableShippingVO) => {
  selectedShipping.value = shipping
  formModel.shippingOrderId = shipping.id
  formModel.warehouseId = undefined as unknown as number // 不再自动填充，需手动选择
  hasUnsavedChanges.value = true

  // 加载物流单待入库明细
  loadShippingItems(shipping.id)
}

// 加载物流单待入库明细
const loadShippingItems = (shippingOrderId: number) => {
  doRequest(getShippingItemsForInbound(shippingOrderId), {
    onSuccess: res => {
      if (res.data && res.data.length > 0) {
        inboundItems.value = mapShippingItemsToFormData(res.data)
      } else {
        message.warning('该物流单没有待入库明细')
        inboundItems.value = []
      }
    },
    onError: () => {
      message.error('加载入库明细失败')
      inboundItems.value = []
    }
  })
}

/**
 * 校验表单
 */
const validateForm = async (): Promise<boolean> => {
  // 1. 校验基本表单字段
  try {
    await formRef.value?.validate()
  } catch {
    return false
  }

  // 2. 校验明细（建单仅声明应到数量，实到由平台收货时录入，此处不校验实到）
  const selectedItems = inboundItems.value.filter(item => item.selected)

  if (selectedItems.length === 0) {
    message.error('请勾选入库明细')
    return false
  }

  for (const item of selectedItems) {
    if (item.expectedQuantity == null || item.expectedQuantity <= 0) {
      message.error(`SKU[${item.skuCode}]应到数量必须大于0`)
      return false
    }
  }

  return true
}

// 构建提交数据
const buildSubmitData = (): PurchaseInboundDTO => {
  const selectedItems = inboundItems.value.filter(item => item.selected)
  const items: PurchaseInboundItemDTO[] = selectedItems.map(item => ({
    shippingOrderItemId: item.shippingOrderItemId,
    purchaseOrderId: item.purchaseOrderId,
    purchaseOrderItemId: item.purchaseOrderItemId,
    skuCode: item.skuCode,
    expectedQuantity: item.expectedQuantity,
    actualQuantity: item.actualQuantity,
    remark: item.remark
  }))

  return {
    ...formModel,
    items
  }
}

// 静默保存 - 用于"确认入库"流程，不显示保存成功消息
const submitFormSilent = async (): Promise<number | null> => {
  const valid = await validateForm()
  if (!valid) return null

  const data = buildSubmitData()
  submitLoading.value = true

  try {
    if (isUpdateForm.value) {
      const result = await updatePurchaseInbound(data)
      if (isSuccess(result)) {
        resetDirty()
        return formModel.id!
      } else {
        message.error(result.message || '保存失败')
        return null
      }
    } else {
      const result = await createPurchaseInbound(data)
      if (isSuccess(result)) {
        resetDirty()
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

// 保存草稿 - 显示保存成功消息
const submitFormWithMessage = async (): Promise<number | null> => {
  const id = await submitFormSilent()
  if (id) {
    message.success('保存成功')
  }
  return id
}

// 返回列表 - 先跳转到列表页，再关闭当前 tab
const goBackToList = async () => {
  resetDirty()
  const currentPath = route.path
  await router.push('/wms/purchase-inbound')
  emitter.emit('close-current-tab', currentPath)
}

// 处理返回
const handleBack = () => {
  confirmIfDirty(goBackToList)
}

// 处理取消
const handleCancel = () => {
  handleBack()
}

// 保存草稿
const handleSaveDraft = async () => {
  const id = await submitFormWithMessage()
  if (id) {
    emitter.emit('refresh-purchase-inbound-list')
    goBackToList()
  }
}

// 提交入库单（先保存草稿，再提交流转给平台收货/上架）
const handleSubmit = async () => {
  // 先做表单校验：有必填项未填则直接提示，不再弹出确认框
  const valid = await validateForm()
  if (!valid) return

  Modal.confirm({
    title: '提交入库单',
    content: '确认要提交吗？提交后将流转至平台收货上架。',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      // 先保存（新建则创建，编辑则更新），再调用提交接口流转状态
      const id = await submitFormSilent()
      if (!id) return
      doRequest(submitInbound(id), {
        successMessage: '提交成功',
        onSuccess: () => {
          resetDirty()
          emitter.emit('refresh-purchase-inbound-list')
          goBackToList()
        }
      })
    }
  })
}

// 加载入库单详情
const loadInboundDetail = (id: number) => {
  loading.value = true
  doRequest(getPurchaseInboundDetail(id), {
    onSuccess: res => {
      const detail = res.data!

      // 检查是否可以进入编辑页
      if (detail.orderStatus !== InboundStatus.DRAFT) {
        message.warning('只有草稿状态的入库单可以编辑')
        goBackToList()
        return
      }

      orderStatus.value = detail.orderStatus

      // 基本信息
      formModel.id = detail.id
      formModel.inboundNo = detail.inboundNo
      formModel.shippingOrderId = detail.shippingOrderId
      formModel.warehouseId = detail.warehouseId
      formModel.inboundDate = detail.inboundDate
      formModel.remark = detail.remark

      // 设置日期显示值
      inboundDateValue.value = detail.inboundDate

      // 设置选中的物流单信息（直接使用接口返回的完整对象）
      if (detail.shippingOrder) {
        selectedShipping.value = detail.shippingOrder
      }

      // 入库明细
      inboundItems.value = mapInboundItemsToFormData(detail.items)
    },
    onFinally: () => {
      loading.value = false
    }
  })
}

// 初始化页面
const initPage = () => {
  if (formMode.value === 'edit' && inboundOrderId.value) {
    loadInboundDetail(inboundOrderId.value)
  } else if (formMode.value === 'create') {
    // 重置所有表单状态
    resetForm()

    // 设置默认入库日期为当天
    const today = new Date().toISOString().split('T')[0]
    inboundDateValue.value = today
    formModel.inboundDate = today

    // 检查是否从物流单详情页跳转过来，预填物流单
    const shippingOrderId = route.query.shippingOrderId as string
    if (shippingOrderId) {
      loadShippingFromQuery(Number(shippingOrderId))
    }
  }
}

// 从 query 参数加载物流单信息
const loadShippingFromQuery = async (shippingOrderId: number) => {
  loading.value = true

  try {
    // 并行请求
    const [shippingResult, itemsResult] = await Promise.all([
      getShippingDetail(shippingOrderId),
      getShippingItemsForInbound(shippingOrderId)
    ])

    // 统一检查结果
    if (isSuccess(shippingResult) && isSuccess(itemsResult)) {
      // 设置物流单信息
      const shipping = shippingResult.data!
      selectedShipping.value = shipping
      formModel.shippingOrderId = shipping.id

      // 设置明细
      if (itemsResult.data && itemsResult.data.length > 0) {
        inboundItems.value = mapShippingItemsToFormData(itemsResult.data)
      }
    } else {
      message.error('加载物流单信息失败')
    }
  } catch (error) {
    console.error('加载物流单信息失败:', error)
    message.error('加载物流单信息失败，请重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initPage()
})
</script>

<style scoped>
.purchase-inbound-form-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f5f5;
}

.custom-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

/* 表单内容区域 */
.form-content {
  flex: 1;
  padding: 16px 24px;
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

:deep(.ant-form-item-label > label) {
  font-size: 13px;
  color: #595959;
}

/* 底部操作按钮区域 */
.page-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}

.footer-content {
  display: flex;
  justify-content: flex-end;
  padding: 12px 24px;
  margin-left: 208px;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .footer-content {
    margin-left: 0;
  }
}
</style>
