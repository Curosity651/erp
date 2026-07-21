<template>
  <page-container
    class="purchase-order-form-page"
    :page-header-render="false"
    ghost
    :loading="loading"
  >
    <!-- 自定义页面头部 - 渲染在 GridContent 外部 -->
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
            v-if="isUpdateForm && orderStatusDesc"
            :color="statusColor"
            style="margin-left: 12px"
          >
            {{ orderStatusDesc }}
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
      <!-- 非草稿状态提示 -->
      <a-alert
        v-if="isUpdateForm && !editableConfig.basicInfo"
        :message="editableFieldsHint"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      />

      <!-- 基本信息区块 -->
      <div class="form-section">
        <div class="section-title">
          <profile-outlined class="section-icon" />
          基本信息
        </div>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item
              label="采购单号（合同编号）"
              name="orderNo"
              :rules="[
                { required: true, message: '请输入采购单号', trigger: 'blur' },
                { max: 50, message: '采购单号长度不能超过50个字符', trigger: 'blur' }
              ]"
            >
              <a-input
                v-model:value="formModel.orderNo"
                placeholder="请输入采购单号"
                :disabled="!editableConfig.basicInfo"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item
              label="供应商"
              name="supplierId"
              :rules="[{ required: true, message: '请选择供应商', trigger: 'change' }]"
            >
              <supplier-select
                v-model:value="supplierCode"
                :disabled="!editableConfig.basicInfo"
                @change="handleSupplierChange"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item
              label="下单日期"
              name="orderDate"
              :rules="[{ required: true, message: '请选择下单日期', trigger: 'change' }]"
            >
              <a-date-picker
                v-model:value="orderDateValue"
                style="width: 100%"
                value-format="YYYY-MM-DD"
                :disabled="!editableConfig.basicInfo"
                @change="handleOrderDateChange"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item label="预计交货日期">
              <a-date-picker
                v-model:value="expectedDateValue"
                style="width: 100%"
                value-format="YYYY-MM-DD"
                :disabled="!editableConfig.basicInfo"
                @change="handleExpectedDateChange"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item label="实际交货日期">
              <a-date-picker
                v-model:value="actualDeliveryDateValue"
                style="width: 100%"
                value-format="YYYY-MM-DD"
                :disabled="!editableConfig.qcData"
                @change="handleActualDeliveryDateChange"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item label="币种">
              <a-select
                v-model:value="formModel.currencyCode"
                :disabled="!editableConfig.basicInfo"
                :options="currencyOptions"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item label="是否含税">
              <a-radio-group
                v-model:value="formModel.taxIncluded"
                :disabled="!editableConfig.basicInfo"
              >
                <a-radio :value="1">含税</a-radio>
                <a-radio :value="0">不含税</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
      </div>

      <!-- 采购明细区块 -->
      <div class="form-section form-section-no-border">
        <div class="section-header">
          <div class="section-title-inline">
            <shopping-outlined class="section-icon" />
            采购明细
          </div>
          <div class="section-header-right">
            <span class="total-amount-display">
              合同总金额：<span class="amount-value">{{ formatAmountDisplay(totalAmount) }}</span>
            </span>
            <a-button
              v-if="editableConfig.items"
              type="primary"
              size="small"
              style="margin-left: 16px"
              @click="handleAddItem"
            >
              <plus-outlined /> 添加商品
            </a-button>
          </div>
        </div>
        <purchase-order-item-table
          ref="itemTableRef"
          v-model="formModel.items"
          :disabled="!editableConfig.items"
          :currency-code="formModel.currencyCode"
          :sku-brief-map="skuBriefMap"
          @change="handleItemsChange"
          @sku-brief-update="handleSkuBriefUpdate"
        />
      </div>

      <!-- 付款信息区块 -->
      <payment-info-section
        ref="paymentSectionRef"
        :form-model="paymentFormModel"
        :total-amount="totalAmount"
        :currency-code="formModel.currencyCode"
        :disabled="!editableConfig.paymentStatus"
        :payment-terms-disabled="!editableConfig.paymentTerms"
        :prepay-already-paid="originalPrepayPaid"
        :balance-already-paid="originalBalancePaid"
        @update:payment-info="handlePaymentInfoUpdate"
      />

      <!-- 合同附件区块 -->
      <contract-section
        :contract-info="formModel.contractInfo!"
        :existing-contract-file="existingContractFile"
        :disabled="!editableConfig.contract"
        @update:contract-info="handleContractInfoUpdate"
      />

      <!-- 质检数据区块 -->
      <qc-data-section
        ref="qcDataSectionRef"
        :qc-data="formModel.qcData!"
        :order-items="formModel.items"
        :sku-brief-map="skuBriefMap"
        :disabled="!editableConfig.qcData"
        @update:qc-data="handleQcDataUpdate"
      />

      <!-- 其他附件区块 -->
      <other-files-section
        :other-file-ids="formModel.otherFileIds || []"
        :existing-other-files="existingOtherFiles"
        :disabled="!editableConfig.otherFiles"
        @update:other-file-ids="handleOtherFileIdsUpdate"
      />

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
            :disabled="!editableConfig.remark"
          />
        </a-form-item>
      </div>
    </a-form>

    <!-- 底部操作按钮区域 -->
    <template #footer>
      <a-space>
        <a-button @click="handleCancel">取消</a-button>
        <template v-if="!isUpdateForm || isDraft">
          <a-button :loading="submitLoading" @click="handleSaveDraft"> 保存草稿 </a-button>
          <a-button type="primary" :loading="submitLoading" @click="handleConfirmSubmit">
            确认并提交
          </a-button>
        </template>
        <template v-else>
          <a-button type="primary" :loading="submitLoading" @click="handleSave"> 保存 </a-button>
        </template>
      </a-space>
    </template>
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
  ShoppingOutlined,
  EditOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'
import PageContainer from '#/layout/components/PageContainer'
import { emitter } from '@/hooks/mitt'
import { useUnsavedChangesGuard } from '@/hooks/useUnsavedChangesGuard'
import { doRequest } from '@/utils/axios/request'
import type {
  PurchaseOrderDTO,
  PurchaseOrderStatus,
  PaymentInfoDTO,
  ContractInfoDTO,
  QcDataDTO,
  FileInfoVO,
  SkuBriefVO
} from '@/api/wms/purchase-order/types'
import type { SupplierPageVO } from '@/api/product/supplier/types'
import {
  createPurchaseOrder,
  updatePurchaseOrder,
  getPurchaseOrderDetail,
  confirmPurchaseOrder
} from '@/api/wms/purchase-order'
import {
  getEditableFieldConfig,
  getEditableFieldsHint,
  ORDER_STATUS_COLOR_MAP,
  canEnterEditPage
} from '@/api/wms/purchase-order/status-utils'
import { SupplierSelect } from '@/components/Lov'
import PurchaseOrderItemTable from './components/PurchaseOrderItemTable.vue'
import PaymentInfoSection from './components/PaymentInfoSection.vue'
import ContractSection from './components/ContractSection.vue'
import QcDataSection from './components/QcDataSection.vue'
import OtherFilesSection from './components/OtherFilesSection.vue'
import { formatAmount } from '@/utils/currency-utils'

defineOptions({ name: 'PurchaseOrderFormPage' })

const route = useRoute()
const router = useRouter()

// 表单引用
const formRef = ref<FormInstance>()
const itemTableRef = ref<InstanceType<typeof PurchaseOrderItemTable>>()
const paymentSectionRef = ref<InstanceType<typeof PaymentInfoSection>>()
const qcDataSectionRef = ref<InstanceType<typeof QcDataSection>>()

// 表单模式：create | edit
const formMode = computed(() => route.params.mode as 'create' | 'edit')

// 采购单 ID
const purchaseOrderId = computed(() => {
  const id = route.params.id as string
  return id ? Number(id) : undefined
})

// 页面状态
const loading = ref(false)
const submitLoading = ref(false)
const hasUnsavedChanges = ref(false)

// 未保存变更守卫
const { confirmIfDirty, resetDirty, useRouteLeaveGuard } = useUnsavedChangesGuard(hasUnsavedChanges)
// 注册路由离开守卫，拦截浏览器前进/后退/关标签时的未保存改动
useRouteLeaveGuard()

// 采购单状态
const orderStatus = ref<PurchaseOrderStatus>('DRAFT')
const orderStatusDesc = ref('')

// 是否编辑模式
const isUpdateForm = computed(() => formMode.value === 'edit')

// 是否草稿状态
const isDraft = computed(() => orderStatus.value === 'DRAFT')

// 页面标题
const pageTitle = computed(() => {
  return isUpdateForm.value ? '编辑采购单' : '新建采购单'
})

// 状态颜色
const statusColor = computed(() => ORDER_STATUS_COLOR_MAP[orderStatus.value])

// 可编辑字段配置
const editableConfig = computed(() => {
  if (!isUpdateForm.value) {
    // 新建时所有字段可编辑
    return {
      basicInfo: true,
      items: true,
      paymentTerms: true,
      paymentStatus: true,
      contract: true,
      qcData: true,
      otherFiles: true,
      remark: true
    }
  }
  return getEditableFieldConfig(orderStatus.value)
})

// 可编辑字段提示
const editableFieldsHint = computed(() => getEditableFieldsHint(orderStatus.value))

// 供应商编码（用于显示）
const supplierCode = ref<string>()

// 日期值
const orderDateValue = ref<string>()
const expectedDateValue = ref<string>()
const actualDeliveryDateValue = ref<string>()

// 合同总金额
const totalAmount = ref(0)

// 原始付款状态（用于判断是否已付）
const originalPrepayPaid = ref(false)
const originalBalancePaid = ref(false)

// 已有的文件信息
const existingContractFile = ref<FileInfoVO>()
const existingOtherFiles = ref<FileInfoVO[]>([])

// SKU 展示信息映射表
const skuBriefMap = ref<Record<string, SkuBriefVO>>({})

// 币种选项
const currencyOptions = [
  { label: 'CNY - 人民币', value: 'CNY' },
  { label: 'USD - 美元', value: 'USD' },
  { label: 'EUR - 欧元', value: 'EUR' }
]

// 表单模型
const formModel = reactive<PurchaseOrderDTO>({
  id: undefined,
  orderNo: '',
  supplierId: 0,
  orderDate: '',
  expectedDeliveryDate: undefined,
  actualDeliveryDate: undefined,
  currencyCode: 'CNY',
  taxIncluded: 1,
  prepayRatio: 30,
  balancePaymentDays: 30,
  remark: undefined,
  items: [],
  paymentInfo: {
    prepayStatus: 0,
    prepayVoucherFileId: undefined,
    balanceStatus: 0,
    balanceVoucherFileId: undefined
  },
  contractInfo: {
    contractFileId: undefined,
    action: undefined
  },
  qcData: {
    items: []
  },
  otherFileIds: []
})

// 付款表单模型（用于 PaymentInfoSection）
// 注意：直接传递 formModel 的引用，而不是创建新对象
// 这样子组件修改 prepayRatio/balancePaymentDays 时能正确同步回父组件
const paymentFormModel = formModel as {
  prepayRatio?: number
  balancePaymentDays?: number
  paymentInfo: PaymentInfoDTO
}

// 处理供应商选择变化
const handleSupplierChange = (_value?: string, option?: SupplierPageVO) => {
  formModel.supplierId = option?.id || 0
  hasUnsavedChanges.value = true
}

// 处理下单日期变化
const handleOrderDateChange = (date: string | null) => {
  formModel.orderDate = date || ''
  hasUnsavedChanges.value = true
}

// 处理预计交货日期变化
const handleExpectedDateChange = (date: string | null) => {
  formModel.expectedDeliveryDate = date || undefined
  hasUnsavedChanges.value = true
}

// 处理实际交货日期变化
const handleActualDeliveryDateChange = (date: string | null) => {
  formModel.actualDeliveryDate = date || undefined
  hasUnsavedChanges.value = true
}

// 处理明细变化
const handleItemsChange = (amount: number) => {
  totalAmount.value = amount
  hasUnsavedChanges.value = true
}

// 处理 SKU 展示信息更新（选择 SKU 时触发）
const handleSkuBriefUpdate = (newSkuBriefMap: Record<string, SkuBriefVO>) => {
  skuBriefMap.value = { ...skuBriefMap.value, ...newSkuBriefMap }
}

// 添加商品
const handleAddItem = () => {
  itemTableRef.value?.openSkuSelector()
}

// 处理合同信息更新
const handleContractInfoUpdate = (info: ContractInfoDTO) => {
  formModel.contractInfo = info
  hasUnsavedChanges.value = true
}

// 处理质检数据更新
const handleQcDataUpdate = (data: QcDataDTO) => {
  formModel.qcData = data
  hasUnsavedChanges.value = true
}

// 处理付款信息更新
const handlePaymentInfoUpdate = (info: PaymentInfoDTO) => {
  formModel.paymentInfo = { ...formModel.paymentInfo, ...info }
  hasUnsavedChanges.value = true
}

// 处理其他附件更新
const handleOtherFileIdsUpdate = (ids: number[]) => {
  formModel.otherFileIds = ids
  hasUnsavedChanges.value = true
}

// 格式化金额
const formatAmountDisplay = (amount: number): string => {
  return formatAmount(amount, formModel.currencyCode, false)
}

// 校验表单
const validateForm = async (): Promise<boolean> => {
  try {
    await formRef.value?.validate()
  } catch {
    return false
  }

  // 校验供应商
  if (!formModel.supplierId) {
    message.error('请选择供应商')
    return false
  }

  // 校验明细
  const validation = itemTableRef.value?.validate()
  if (validation && !validation.valid) {
    message.error(validation.message)
    return false
  }

  // 校验付款信息
  const paymentError = paymentSectionRef.value?.validate()
  if (paymentError) {
    message.error(paymentError)
    return false
  }

  // 校验质检数据
  const qcError = qcDataSectionRef.value?.validate()
  if (qcError) {
    message.error(qcError)
    return false
  }

  return true
}

// 提交表单
const submitForm = async (confirmStatus: boolean, onSuccess?: () => void) => {
  const valid = await validateForm()
  if (!valid) return

  // paymentFormModel 现在直接引用 formModel，无需同步

  const submitData = { ...formModel }
  const isCreate = !isUpdateForm.value
  const request = isCreate
    ? createPurchaseOrder(submitData)
    : updatePurchaseOrder(submitData)

  submitLoading.value = true
  doRequest(request, {
    successMessage: confirmStatus ? undefined : '保存成功',
    onSuccess: async (res) => {
      // 如果需要确认，调用确认接口
      if (confirmStatus) {
        // 新建时从返回结果获取ID，编辑时使用表单中的ID
        const orderId = isCreate ? (res.data as number) : formModel.id!
        doRequest(confirmPurchaseOrder(orderId), {
          successMessage: '提交成功',
          onSuccess: () => {
            resetDirty()
            onSuccess?.()
          },
          onFinally: () => {
            submitLoading.value = false
          }
        })
      } else {
        resetDirty()
        onSuccess?.()
        submitLoading.value = false
      }
    },
    onFinally: () => {
      // 只有在非确认模式下才在这里关闭loading
      // 确认模式下由确认请求的onFinally处理
      if (!confirmStatus) {
        submitLoading.value = false
      }
    }
  })
}

// 返回列表 - 先跳转到列表页，再关闭当前 tab
const goBackToList = async () => {
  resetDirty()
  const currentPath = route.path
  await router.push('/wms/purchase-order')
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
const handleSaveDraft = () => {
  submitForm(false, () => {
    emitter.emit('refresh-purchase-order-list')
    goBackToList()
  })
}

// 确认并提交
const handleConfirmSubmit = () => {
  Modal.confirm({
    title: '确认提交',
    content: '确认提交后，采购单将变为已确认状态，部分字段将无法修改。确定要提交吗？',
    okText: '确定提交',
    cancelText: '取消',
    onOk: () => {
      submitForm(true, () => {
        emitter.emit('refresh-purchase-order-list')
        goBackToList()
      })
    }
  })
}

// 保存（非草稿状态）
const handleSave = () => {
  submitForm(false, () => {
    emitter.emit('refresh-purchase-order-list')
    goBackToList()
  })
}

// 加载采购单详情
const loadPurchaseOrderDetail = (id: number) => {
  loading.value = true
  doRequest(getPurchaseOrderDetail(id), {
    onSuccess: res => {
      const detail = res.data!

      // 检查是否可以进入编辑页
      if (!canEnterEditPage(detail.orderStatus)) {
        message.warning('该采购单状态不允许编辑')
        goBackToList()
        return
      }

      orderStatus.value = detail.orderStatus
      orderStatusDesc.value = detail.orderStatusDesc

      // 基本信息
      formModel.id = detail.id
      formModel.orderNo = detail.orderNo
      formModel.supplierId = detail.supplierId
      formModel.orderDate = detail.orderDate
      formModel.expectedDeliveryDate = detail.expectedDeliveryDate
      formModel.currencyCode = detail.currencyCode
      formModel.taxIncluded = detail.taxIncluded
      formModel.prepayRatio = detail.prepayRatio
      formModel.balancePaymentDays = detail.balancePaymentDays
      formModel.remark = detail.remark
      formModel.actualDeliveryDate = detail.actualDeliveryDate

      // 采购明细
      formModel.items = detail.items.map(item => ({
        id: item.id,
        skuCode: item.skuCode,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        remark: item.remark
      }))

      // 初始化 SKU 展示信息映射表
      skuBriefMap.value = detail.skuBriefMap || {}

      // 付款信息
      formModel.paymentInfo = {
        prepayStatus: detail.prepayStatus,
        prepayVoucherFileId: detail.prepayVoucherFile?.sysFileId,
        balanceStatus: detail.balanceStatus,
        balanceVoucherFileId: detail.balanceVoucherFile?.sysFileId
      }
      originalPrepayPaid.value = detail.prepayStatus === 1
      originalBalancePaid.value = detail.balanceStatus === 1

      // 合同信息
      existingContractFile.value = detail.contractFile
      if (detail.contractFile) {
        formModel.contractInfo = {
          contractFileId: detail.contractFile.sysFileId,
          action: undefined
        }
      }

      // 质检数据
      formModel.qcData = {
        items:
          detail.qcItems?.map(item => ({
            skuCode: item.skuCode,
            lengthCm: item.lengthCm,
            widthCm: item.widthCm,
            heightCm: item.heightCm,
            grossWeightKg: item.grossWeightKg,
            netWeightKg: item.netWeightKg,
            qcFileId: item.qcFileId
          })) || []
      }

      // 其他附件
      existingOtherFiles.value = detail.otherFiles || []
      formModel.otherFileIds = detail.otherFiles?.map(f => f.sysFileId) || []

      // 设置显示值
      orderDateValue.value = detail.orderDate
      expectedDateValue.value = detail.expectedDeliveryDate
      actualDeliveryDateValue.value = detail.actualDeliveryDate
      totalAmount.value = detail.totalAmount
      supplierCode.value = detail.supplierName
    },
    onFinally: () => {
      loading.value = false
    }
  })
}

// 初始化页面
const initPage = () => {
  if (formMode.value === 'edit' && purchaseOrderId.value) {
    loadPurchaseOrderDetail(purchaseOrderId.value)
  } else {
    orderStatus.value = 'DRAFT'
  }
}

onMounted(() => {
  initPage()
})
</script>

<style scoped>
/* 自定义页面头部 - 保持原有样式 */
.custom-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
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

/* 覆盖 PageContainer 默认的 padding */
:deep(.ant-pro-page-container-warp) {
  padding: 0;
}

/* 表单内容区域 */
.form-content {
  padding: 16px 24px;
}

/* 表单区块样式 */
.form-section {
  padding: 16px 20px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.section-header-right {
  display: flex;
  align-items: center;
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

.section-title-inline {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: #262626;
}

.form-section-no-border .section-header {
  border-bottom: none;
  padding-bottom: 8px;
}

.section-icon {
  margin-right: 8px;
  color: #1890ff;
  font-size: 16px;
}

.total-amount-display {
  font-size: 14px;
  color: #595959;
}

.amount-value {
  font-size: 18px;
  font-weight: 600;
  color: #f5222d;
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
</style>
