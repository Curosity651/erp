<template>
  <page-container
    class="shipping-order-form-page"
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
            v-if="isUpdateForm && shippingStatus"
            :color="ShippingStatusColorMap[shippingStatus]"
            style="margin-left: 12px"
          >
            {{ ShippingStatusMap[shippingStatus] }}
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
      <!-- 非待发货状态提示 -->
      <a-alert
        v-if="isUpdateForm && !editableConfig.basicInfo"
        :message="editableFieldsTip"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      />

      <!-- 基本信息区块 -->
      <div class="form-section">
        <div class="section-title">
          <profile-outlined class="section-icon section-icon--basic" />
          基本信息
        </div>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item
              label="物流单号"
              name="shippingNo"
              :rules="[
                { required: true, message: '请输入物流单号', trigger: 'blur' },
                { max: 50, message: '物流单号长度不能超过50个字符', trigger: 'blur' }
              ]"
            >
              <a-input
                v-model:value="formModel.shippingNo"
                placeholder="请输入物流单号"
                :disabled="!editableConfig.basicInfo"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item
              label="物流商"
              name="providerId"
              :rules="[{ required: true, message: '请选择物流商', trigger: 'change' }]"
            >
              <logistics-provider-select
                v-model:value="formModel.providerId"
                :disabled="!editableConfig.basicInfo"
                :allow-clear="false"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item
              label="目标区域"
              name="targetRegionId"
              :rules="[{ required: true, message: '请选择目标区域', trigger: 'change' }]"
            >
              <region-select
                v-model:value="formModel.targetRegionId"
                :disabled="!editableConfig.targetRegion"
                placeholder="请选择目标区域"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item
              label="发货日期"
              name="shippingDate"
              :rules="[{ required: true, message: '请选择发货日期', trigger: 'change' }]"
            >
              <a-date-picker
                v-model:value="shippingDateValue"
                style="width: 100%"
                value-format="YYYY-MM-DD"
                :disabled="!editableConfig.basicInfo"
                @change="handleShippingDateChange"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item label="预计时效(天)">
              <a-input-number
                v-model:value="formModel.estimatedDays"
                :min="1"
                :precision="0"
                style="width: 100%"
                placeholder="请输入天数"
                :disabled="!editableConfig.timeInfo"
                @change="handleEstimatedDaysChange"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item label="预计到货日期">
              <a-input
                :value="computedEstimatedArrivalDate"
                style="width: 100%"
                placeholder="根据发货日期和时效自动计算"
                disabled
              />
              <div
                v-if="computedEstimatedArrivalDate"
                style="font-size: 12px; color: #8c8c8c; margin-top: 4px"
              >
                根据发货日期和预计时效自动计算
              </div>
            </a-form-item>
          </a-col>
        </a-row>
      </div>

      <!-- 货物明细区块 -->
      <div class="form-section">
        <div class="section-header">
          <div class="section-title-inline">
            <shopping-outlined class="section-icon section-icon--items" />
            货物明细
          </div>
          <div class="section-header-right">
            <a-button
              v-if="editableConfig.items"
              type="primary"
              size="small"
              @click="handleAddItems"
            >
              <plus-outlined /> 从采购单添加
            </a-button>
          </div>
        </div>
        <!-- 统计摘要条 -->
        <div v-if="formModel.items.length > 0" class="items-summary">
          <div class="summary-item">
            <span class="summary-label">已添加</span>
            <span class="summary-value">{{ formModel.items.length }}</span>
            <span class="summary-unit">项</span>
          </div>
          <a-divider type="vertical" />
          <div class="summary-item">
            <span class="summary-label">总发货数量</span>
            <span class="summary-value highlight">{{ totalShippingQuantity }}</span>
            <span class="summary-unit">件</span>
          </div>
          <a-divider type="vertical" />
          <div class="summary-item">
            <span class="summary-label">关联采购单</span>
            <span class="summary-value">{{ relatedPurchaseOrderCount }}</span>
            <span class="summary-unit">个</span>
          </div>
        </div>
        <shipping-order-item-table
          ref="itemTableRef"
          v-model="formModel.items"
          :disabled="!editableConfig.items"
        />
      </div>

      <!-- 物流信息区块 -->
      <div class="form-section">
        <div class="section-title">
          <car-outlined class="section-icon section-icon--logistics" />
          物流信息
        </div>
        <a-row :gutter="24">
          <a-col :xs="24" :sm="12">
            <a-form-item
              label="物流方式"
              name="shippingMethod"
              :rules="[{ required: true, message: '请选择物流方式', trigger: 'change' }]"
            >
              <a-radio-group
                v-model:value="formModel.shippingMethod"
                :disabled="!editableConfig.logisticsInfo"
                @change="handleShippingMethodChange"
              >
                <a-radio v-for="(label, value) in ShippingMethodMap" :key="value" :value="value">
                  {{ label }}
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item
              label="物流线路"
              name="shippingRoute"
              :rules="[{ required: true, message: '请选择物流线路', trigger: 'change' }]"
            >
              <a-radio-group
                v-model:value="formModel.shippingRoute"
                :disabled="!editableConfig.logisticsInfo"
              >
                <a-radio v-for="(label, value) in ShippingRouteMap" :key="value" :value="value">
                  {{ label }}
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :xs="24" :sm="12">
            <a-form-item
              label="总重量(KG)"
              name="totalWeight"
              :rules="[{ required: true, message: '请输入总重量', trigger: 'blur' }]"
            >
              <a-input-number
                v-model:value="formModel.totalWeight"
                :min="0.01"
                :precision="2"
                style="width: 60%"
                placeholder="请输入"
                :disabled="!editableConfig.logisticsInfo"
                @change="calculateTotalAmount"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </div>

      <!-- 费用信息区块 -->
      <cost-info-section
        ref="costSectionRef"
        :shipping-method="formModel.shippingMethod"
        :total-weight="formModel.totalWeight"
        :unit-price="formModel.unitPrice"
        :shipping-fee="formModel.shippingFee"
        :misc-fee="formModel.miscFee"
        :disabled="!editableConfig.costInfo"
        @update:unit-price="handleUnitPriceChange"
        @update:shipping-fee="handleShippingFeeChange"
        @update:misc-fee="handleMiscFeeChange"
        @amount-change="handleAmountChange"
      />

      <!-- 付款信息区块 -->
      <div class="form-section">
        <div class="section-title">
          <wallet-outlined class="section-icon section-icon--payment" />
          付款信息
          <a-badge
            :status="formModel.paymentStatus === 1 ? 'success' : 'default'"
            :text="formModel.paymentStatus === 1 ? '已付' : '未付'"
            style="margin-left: 12px"
          />
        </div>
        <a-row :gutter="24">
          <a-col :xs="24" :sm="12">
            <a-form-item label="付款状态">
              <a-radio-group
                v-model:value="formModel.paymentStatus"
                :disabled="initialPaymentStatus === 1"
                @change="handlePaymentStatusChange"
              >
                <a-radio :value="0">未付</a-radio>
                <a-radio :value="1">已付</a-radio>
              </a-radio-group>
              <div v-if="initialPaymentStatus === 1" class="payment-locked-tip">
                已付款状态不可更改
              </div>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="付款凭证" :required="formModel.paymentStatus === 1">
              <sys-file-upload
                v-model="formModel.paymentVoucherFileId"
                bucket-key="private-files"
                button-text="上传凭证"
                :allowed-types="['application/pdf', 'image/jpeg', 'image/png']"
                :disabled="formModel.paymentStatus !== 1"
                :hide-upload-button="initialPaymentStatus === 1"
                @update:model-value="handleVoucherFileChange"
              />
              <div v-if="initialPaymentStatus !== 1" class="upload-tip">
                {{ formModel.paymentStatus === 1 ? '支持 PDF、JPG、PNG 格式' : '请先选择已付状态' }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
      </div>

      <!-- 备注区块 -->
      <div class="form-section">
        <div class="section-title">
          <edit-outlined class="section-icon section-icon--remark" />
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
        <a-button :loading="submitLoading" @click="handleSave">保存</a-button>
        <a-button
          v-if="!isUpdateForm || shippingStatus === ShippingStatus.PENDING"
          type="primary"
          :loading="submitLoading"
          @click="handleSaveAndShip"
        >
          保存并确认发货
          <right-outlined />
        </a-button>
      </a-space>
    </template>

    <!-- 从采购单添加货物弹窗 -->
    <available-items-modal
      ref="availableItemsModalRef"
      :exclude-item-ids="excludeItemIds"
      :shipping-order-id="shippingOrderId"
      @confirm="handleItemsConfirm"
    />
  </page-container>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  ProfileOutlined,
  CarOutlined,
  ShoppingOutlined,
  EditOutlined,
  PlusOutlined,
  RightOutlined,
  WalletOutlined
} from '@ant-design/icons-vue'
import PageContainer from '#/layout/components/PageContainer'
import { emitter } from '@/hooks/mitt'
import { useUnsavedChangesGuard } from '@/hooks/useUnsavedChangesGuard'
import CostInfoSection from './components/CostInfoSection.vue'
import ShippingOrderItemTable from './components/ShippingOrderItemTable.vue'
import AvailableItemsModal from './components/AvailableItemsModal.vue'
import LogisticsProviderSelect from '../../../components/Lov/LogisticsProviderSelect.vue'
import RegionSelect from '@/components/Lov/RegionSelect.vue'
import SysFileUpload from '@/components/Upload/SysFileUpload.vue'
import {
  createShippingOrder,
  updateShippingOrder,
  getShippingOrderDetail,
  confirmShip
} from '@/api/wms/shipping-order'
import {
  ShippingStatusMap,
  ShippingStatusColorMap,
  ShippingMethodMap,
  ShippingRouteMap,
  ShippingStatus,
  ShippingMethod,
  ShippingRoute
} from '@/api/wms/shipping-order/types'
import type {
  ShippingOrderDTO,
  ShippingOrderItemDTO,
  ShippingOrderItemVO,
  AvailableItemVO
} from '@/api/wms/shipping-order/types'
import { doRequest } from '@/utils/axios/request'
import { isSuccess } from '@/api'

defineOptions({ name: 'ShippingOrderFormPage' })

const route = useRoute()
const router = useRouter()

// 表单引用
const formRef = ref<FormInstance>()
const costSectionRef = ref<InstanceType<typeof CostInfoSection>>()
const itemTableRef = ref<InstanceType<typeof ShippingOrderItemTable>>()
const availableItemsModalRef = ref<InstanceType<typeof AvailableItemsModal>>()

// 表单模式：create | edit
const formMode = computed(() => route.params.mode as 'create' | 'edit')

// 物流单 ID
const shippingOrderId = computed(() => {
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

// 物流单状态
const shippingStatus = ref<string>('')

// 初始付款状态（从服务器加载的原始值）
const initialPaymentStatus = ref<number>(0)

// 原始目标区域ID（从服务器加载的原始值，用于检测变更）
const originalTargetRegionId = ref<number>()

// 是否编辑模式
const isUpdateForm = computed(() => formMode.value === 'edit')

// 页面标题
const pageTitle = computed(() => {
  return isUpdateForm.value ? '编辑物流单' : '新建物流单'
})

// 可编辑字段提示文案
const editableFieldsTip = computed(() => {
  const fields: string[] = []
  if (editableConfig.value.targetRegion) fields.push('目标区域')
  if (editableConfig.value.logisticsInfo) fields.push('物流信息')
  if (editableConfig.value.costInfo) fields.push('费用信息')
  if (editableConfig.value.timeInfo) fields.push('预计时效')
  if (editableConfig.value.remark) fields.push('备注')
  // 付款信息：只要初始状态不是已付，就可以编辑
  if (initialPaymentStatus.value !== 1) fields.push('付款信息')

  if (fields.length === 0) {
    return '当前状态下不可编辑'
  }
  return `当前状态下仅可编辑${fields.join('、')}`
})

// 可编辑字段配置
const editableConfig = computed(() => {
  const notPaid = initialPaymentStatus.value !== 1
  if (!isUpdateForm.value) {
    return { basicInfo: true, targetRegion: true, logisticsInfo: true, costInfo: true, items: true, timeInfo: true, remark: true }
  }
  if (shippingStatus.value === ShippingStatus.PENDING) {
    return { basicInfo: true, targetRegion: true, logisticsInfo: true, costInfo: true, items: true, timeInfo: true, remark: true }
  }
  if (shippingStatus.value === ShippingStatus.SHIPPED) {
    // 已发货状态：目标区域、时效和备注可编辑；物流/费用未付款时可编辑
    return { basicInfo: false, targetRegion: true, logisticsInfo: notPaid, costInfo: notPaid, items: false, timeInfo: true, remark: true }
  }
  if (shippingStatus.value === ShippingStatus.COMPLETED) {
    return {
      basicInfo: false,
      targetRegion: false,
      logisticsInfo: false,
      costInfo: false,
      items: false,
      timeInfo: false,
      remark: false
    }
  }
  // 部分到货及之后状态：时效和备注可编辑，物流/费用未付款时可编辑
  return { basicInfo: false, targetRegion: false, logisticsInfo: notPaid, costInfo: notPaid, items: false, timeInfo: true, remark: true }
})

// 日期值
const shippingDateValue = ref<string>()

// 表单模型
const formModel = reactive<ShippingOrderDTO>({
  id: undefined,
  shippingNo: '',
  providerId: undefined as unknown as number,
  targetRegionId: undefined as unknown as number,
  shippingDate: '',
  estimatedArrivalDate: undefined,
  estimatedDays: undefined,
  shippingMethod: ShippingMethod.GRAY,
  shippingRoute: ShippingRoute.EAST,
  packageCount: undefined as unknown as number,
  totalWeight: undefined as unknown as number,
  unitPrice: undefined,
  shippingFee: undefined,
  miscFee: undefined,
  totalAmount: undefined,
  totalAmountCny: undefined,
  remark: undefined,
  items: [],
  // 付款信息
  paymentStatus: 0,
  paymentVoucherFileId: undefined
})

// 已添加的采购单明细ID列表（用于排除）
const excludeItemIds = computed(() => {
  return formModel.items.map(item => item.purchaseOrderItemId)
})

// 统计摘要：总发货数量
const totalShippingQuantity = computed(() => {
  return formModel.items.reduce((sum, item) => sum + (item.quantity || 0), 0)
})

// 统计摘要：关联采购单数量
const relatedPurchaseOrderCount = computed(() => {
  const purchaseOrderIds = new Set(formModel.items.map(item => item.purchaseOrderId))
  return purchaseOrderIds.size
})

/**
 * 计算预计到货日期
 * 根据发货日期和预计时效自动计算
 */
const computedEstimatedArrivalDate = computed<string | undefined>(() => {
  if (!shippingDateValue.value || !formModel.estimatedDays) {
    return undefined
  }
  const shippingDate = new Date(shippingDateValue.value)
  shippingDate.setDate(shippingDate.getDate() + formModel.estimatedDays)
  const year = shippingDate.getFullYear()
  const month = String(shippingDate.getMonth() + 1).padStart(2, '0')
  const day = String(shippingDate.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
})

// 目标区域是否变更
const isRegionChanged = computed(() =>
  originalTargetRegionId.value !== undefined &&
  originalTargetRegionId.value !== formModel.targetRegionId
)

// 是否已发货状态
const isShippedStatus = computed(() =>
  shippingStatus.value === ShippingStatus.SHIPPED
)

// 处理发货日期变化
const handleShippingDateChange = (date: string | null) => {
  formModel.shippingDate = date || ''
  hasUnsavedChanges.value = true
}

// 处理预计时效变化
const handleEstimatedDaysChange = () => {
  hasUnsavedChanges.value = true
}

// 处理物流方式变化
const handleShippingMethodChange = () => {
  // 切换物流方式时清空费用字段
  formModel.unitPrice = undefined
  formModel.shippingFee = undefined
  formModel.miscFee = undefined
  formModel.totalAmount = undefined
  formModel.totalAmountCny = undefined
  hasUnsavedChanges.value = true
}

// 处理单价变化
const handleUnitPriceChange = (value: number | undefined) => {
  formModel.unitPrice = value
  hasUnsavedChanges.value = true
}

// 处理运费变化
const handleShippingFeeChange = (value: number | undefined) => {
  formModel.shippingFee = value
  hasUnsavedChanges.value = true
}

// 处理杂费变化
const handleMiscFeeChange = (value: number | undefined) => {
  formModel.miscFee = value
  hasUnsavedChanges.value = true
}

// 处理金额变化
const handleAmountChange = (usd: number, cny: number) => {
  formModel.totalAmount = usd
  formModel.totalAmountCny = cny
}

// 处理付款状态变化
const handlePaymentStatusChange = () => {
  hasUnsavedChanges.value = true
}

// 处理凭证文件变化
const handleVoucherFileChange = () => {
  hasUnsavedChanges.value = true
}

// 计算总金额
const calculateTotalAmount = () => {
  costSectionRef.value?.calculate()
}

// 添加货物
const handleAddItems = () => {
  availableItemsModalRef.value?.open()
}

// 确认添加货物
const handleItemsConfirm = (items: AvailableItemVO[]) => {
  const newItems = items.map(item => ({
    purchaseOrderId: item.purchaseOrderId,
    purchaseOrderItemId: item.purchaseOrderItemId,
    skuCode: item.skuCode,
    quantity: item.availableQuantity,
    // 扩展字段用于显示
    purchaseOrderNo: item.purchaseOrderNo,
    skuBrief: item.skuBrief,
    availableQuantity: item.availableQuantity
  }))

  formModel.items = [...formModel.items, ...newItems]
  hasUnsavedChanges.value = true
}

// 校验表单
const validateForm = async (): Promise<boolean> => {
  try {
    await formRef.value?.validate()
  } catch {
    return false
  }

  // 校验费用字段（根据物流方式）
  if (formModel.shippingMethod === ShippingMethod.GRAY) {
    if (!formModel.unitPrice || formModel.unitPrice <= 0) {
      message.error('灰关模式下，物流单价不能为空')
      return false
    }
  } else if (formModel.shippingMethod === ShippingMethod.WHITE) {
    if (!formModel.shippingFee || formModel.shippingFee <= 0) {
      message.error('白关模式下，运输费用不能为空')
      return false
    }
    if (!formModel.miscFee || formModel.miscFee < 0) {
      message.error('白关模式下，杂费不能为空')
      return false
    }
  }

  // 校验明细
  if (formModel.items.length === 0) {
    message.error('请添加货物明细')
    return false
  }

  // 校验发货数量
  for (const item of formModel.items) {
    if (!item.quantity || item.quantity <= 0) {
      message.error(`SKU[${item.skuCode}]的发货数量必须大于0`)
      return false
    }
  }

  return true
}

// 提交表单
const submitForm = (onSuccess?: (id: number) => void) => {
  validateForm().then(valid => {
    if (!valid) return

    // SHIPPED 状态下仓库变更需要二次确认
    if (isRegionChanged.value && isShippedStatus.value) {
      Modal.confirm({
        title: '确认修改目标区域',
        content: '修改目标区域将触发在途库存调整，确认继续？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => doSubmit(onSuccess)
      })
      return
    }

    doSubmit(onSuccess)
  })
}

// 执行提交
const doSubmit = (onSuccess?: (id: number) => void) => {
  // 自动计算发货件数（从明细汇总）
  formModel.packageCount = totalShippingQuantity.value

  // 同步计算的预计到货日期到表单模型
  formModel.estimatedArrivalDate = computedEstimatedArrivalDate.value

  // 未付款状态时清除付款凭证
  if (formModel.paymentStatus === 0) {
    formModel.paymentVoucherFileId = undefined
  }

  const request = isUpdateForm.value
    ? updateShippingOrder(formModel)
    : createShippingOrder(formModel)

  submitLoading.value = true
  doRequest(request, {
    successMessage: '保存成功',
    onSuccess: res => {
      resetDirty()
      const id = isUpdateForm.value ? formModel.id! : (res.data as number)
      onSuccess?.(id)
    },
    onFinally: () => {
      submitLoading.value = false
    }
  })
}

// 返回列表 - 先跳转到列表页，再关闭当前 tab
const goBackToList = async () => {
  resetDirty()
  const currentPath = route.path
  await router.push('/wms/shipping-order')
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

// 保存
const handleSave = () => {
  submitForm(() => {
    emitter.emit('refresh-shipping-order-list')
    goBackToList()
  })
}

// 保存并确认发货
const handleSaveAndShip = () => {
  Modal.confirm({
    title: '确认发货',
    content: '保存后将自动确认发货，确定要继续吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: () => {
      submitForm(id => {
        doRequest(confirmShip(id), {
          successMessage: '确认发货成功',
          onSuccess: () => {
            emitter.emit('refresh-shipping-order-list')
            goBackToList()
          }
        })
      })
    }
  })
}

// 加载物流单详情
const loadShippingOrderDetail = async (id: number) => {
  loading.value = true
  try {
    const result = await getShippingOrderDetail(id)
    if (isSuccess(result) && result.data) {
      const detail = result.data

      // 检查是否可以进入编辑页
      if (detail.shippingStatus === ShippingStatus.COMPLETED) {
        message.warning('已完成的物流单不允许编辑')
        goBackToList()
        return
      }

      shippingStatus.value = detail.shippingStatus

      // 基本信息
      formModel.id = detail.id
      formModel.shippingNo = detail.shippingNo
      formModel.providerId = detail.providerId
      formModel.targetRegionId = detail.targetRegionId
      // 记录原始目标区域ID，用于检测变更
      originalTargetRegionId.value = detail.targetRegionId
      formModel.shippingDate = detail.shippingDate
      formModel.estimatedArrivalDate = detail.estimatedArrivalDate
      formModel.estimatedDays = detail.estimatedDays
      formModel.shippingMethod = detail.shippingMethod
      formModel.shippingRoute = detail.shippingRoute
      formModel.packageCount = detail.packageCount
      formModel.totalWeight = detail.totalWeight
      formModel.unitPrice = detail.unitPrice
      formModel.shippingFee = detail.shippingFee
      formModel.miscFee = detail.miscFee
      formModel.totalAmount = detail.totalAmount
      formModel.totalAmountCny = detail.totalAmountCny
      formModel.remark = detail.remark

      // 货物明细
      formModel.items = detail.items.map((item: ShippingOrderItemVO) => ({
        purchaseOrderId: item.purchaseOrderId,
        purchaseOrderItemId: item.purchaseOrderItemId,
        skuCode: item.skuCode,
        quantity: item.quantity,
        // 扩展字段用于显示
        purchaseOrderNo: item.purchaseOrderNo,
        skuBrief: item.skuBrief,
        availableQuantity: item.availableQuantity
      }))

      // 设置日期显示值
      shippingDateValue.value = detail.shippingDate

      // 填充付款信息
      formModel.paymentStatus = detail.paymentStatus || 0
      formModel.paymentVoucherFileId = detail.paymentVoucherFileId
      // 记录初始付款状态，用于判断是否可编辑
      initialPaymentStatus.value = detail.paymentStatus || 0
    } else {
      message.error(result.message || '获取详情失败')
    }
  } catch (error) {
    console.error('加载物流单详情失败:', error)
    message.error('加载物流单详情失败')
  } finally {
    loading.value = false
  }
}

// 重置表单到初始状态
const resetForm = () => {
  formModel.id = undefined
  formModel.shippingNo = ''
  formModel.providerId = undefined as unknown as number
  formModel.targetRegionId = undefined as unknown as number
  formModel.shippingDate = ''
  formModel.estimatedArrivalDate = undefined
  formModel.estimatedDays = undefined
  formModel.shippingMethod = ShippingMethod.GRAY
  formModel.shippingRoute = ShippingRoute.EAST
  formModel.packageCount = undefined as unknown as number
  formModel.totalWeight = undefined as unknown as number
  formModel.unitPrice = undefined
  formModel.shippingFee = undefined
  formModel.miscFee = undefined
  formModel.totalAmount = undefined
  formModel.totalAmountCny = undefined
  formModel.remark = undefined
  formModel.items = []
  formModel.paymentStatus = 0
  formModel.paymentVoucherFileId = undefined

  // 重置日期显示值
  shippingDateValue.value = undefined

  // 重置状态
  shippingStatus.value = ''
  initialPaymentStatus.value = 0
  originalTargetRegionId.value = undefined
  hasUnsavedChanges.value = false

  // 清除表单校验状态
  formRef.value?.resetFields()
}

// 初始化页面
const initPage = async () => {
  if (route.name !== 'ShippingOrderForm') {
    return
  }

  if (formMode.value === 'edit' && shippingOrderId.value) {
    await loadShippingOrderDetail(shippingOrderId.value)
  } else {
    // 新建模式：重置表单
    resetForm()
  }
}

// 监听路由参数变化，处理同一组件不同模式切换的场景
watch(
  () => [route.name, route.params.mode, route.params.id],
  () => {
    initPage()
  }
)

onMounted(() => {
  initPage()
})
</script>

<style scoped>
/* 自定义页面头部 */
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

.section-icon {
  margin-right: 8px;
  font-size: 16px;
}

/* 区块图标语义化色彩 */
.section-icon--basic {
  color: #1890ff;
}

.section-icon--logistics {
  color: #722ed1;
}

.section-icon--items {
  color: #52c41a;
}

.section-icon--remark {
  color: #8c8c8c;
}

.section-icon--payment {
  color: #faad14;
}

.payment-locked-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}

.upload-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}

/* 货物明细统计摘要条 */
.items-summary {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  margin-bottom: 12px;
  background: #fafafa;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
}

.summary-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.summary-label {
  font-size: 13px;
  color: #8c8c8c;
}

.summary-value {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

/* 突出显示总发货数量 */
.summary-value.highlight {
  color: #1890ff;
  font-size: 18px;
}

.summary-unit {
  font-size: 12px;
  color: #8c8c8c;
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
