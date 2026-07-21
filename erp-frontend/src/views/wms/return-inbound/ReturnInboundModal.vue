<template>
  <a-modal
    v-model:open="visible"
    title="新建退货入库单"
    :width="900"
    :confirm-loading="confirmLoading"
    :mask-closable="false"
    @cancel="handleCancel"
  >
    <!-- 步骤条 -->
    <div class="mb-6">
      <a-steps :current="currentStep" size="small">
        <a-step title="选择订单" />
        <a-step title="填写退货信息" />
      </a-steps>
    </div>

    <!-- 步骤1: 选择订单 -->
    <div v-show="currentStep === 0">
      <!-- 搜索区域 -->
      <div class="mb-4">
        <a-form layout="inline">
          <a-form-item label="订单号/SKU">
            <a-input
              v-model:value="searchForm.keyword"
              placeholder="输入订单号或SKU编码"
              allow-clear
              style="width: 200px"
              @press-enter="doSearch"
            />
          </a-form-item>
          <a-form-item label="平台">
            <PlatformSelect v-model:value="searchForm.platform" allow-clear style="width: 140px" />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="doSearch">查询</a-button>
          </a-form-item>
        </a-form>
      </div>

      <div class="mb-4">
        <a-alert type="info" show-icon>
          <template #message>仅显示已出库且有可退货数量的订单</template>
        </a-alert>
      </div>

      <!-- 订单列表 -->
      <a-table
        :data-source="orderList"
        :columns="orderColumns"
        :loading="orderLoading"
        :pagination="orderPagination"
        :row-selection="rowSelection"
        row-key="orderItemId"
        size="small"
        @change="handleTableChange"
      >
        <!-- 平台列 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'platform'">
            <PlatformTag :platform="record.platform" />
          </template>
          <!-- SKU 列使用 SkuBriefCell -->
          <template v-else-if="column.key === 'sku'">
            <SkuBriefCell :brief="record.skuBrief" show-code />
          </template>
        </template>
      </a-table>
    </div>

    <!-- 步骤2: 填写退货信息 -->
    <div v-show="currentStep === 1">
      <!-- 订单信息卡片 -->
      <div class="mb-4">
        <a-card size="small" class="info-card">
          <a-descriptions :column="3" size="small">
            <a-descriptions-item label="平台订单号">{{
              selectedOrder?.platformOrderId
            }}</a-descriptions-item>
            <a-descriptions-item label="平台">
              <PlatformTag :platform="selectedOrder?.platform" />
            </a-descriptions-item>
            <a-descriptions-item label="发货时间">{{
              formatDate(selectedOrder?.outboundTime)
            }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
      </div>

      <!-- SKU 信息卡片，使用 SkuBriefCell -->
      <div class="mb-4">
        <a-card size="small" class="info-card">
          <div class="flex items-center justify-between">
            <SkuBriefCell :brief="selectedOrder?.skuBrief" show-code />
            <div class="text-right">
              <span class="text-gray-500">可退数量：</span>
              <span class="text-lg font-medium">{{ selectedOrder?.returnableQuantity }}</span>
            </div>
          </div>
        </a-card>
      </div>

      <!-- 退货表单 -->
      <a-form ref="formRef" :model="formModel" :rules="formRules" :label-col="{ span: 6 }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="入库仓库" name="warehouseId">
              <WarehouseSelect v-model:value="formModel.warehouseId" warehouse-type="OWN" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="退货日期" name="returnDate">
              <a-date-picker
                v-model:value="formModel.returnDate"
                style="width: 100%"
                value-format="YYYY-MM-DD"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="退货数量" name="quantity">
              <a-input-number
                v-model:value="formModel.quantity"
                :min="1"
                :max="selectedOrder?.returnableQuantity"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="退货原因" name="returnReason">
              <a-select v-model:value="formModel.returnReason" allow-clear>
                <a-select-option
                  v-for="(label, value) in ReturnReasonMap"
                  :key="value"
                  :value="value"
                >
                  {{ label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <!-- 质检与入库分配由海外仓平台在收货质检环节决定，货主仅申报退货 -->
          <a-col :span="24">
            <a-form-item label="备注" name="remark" :label-col="{ span: 3 }">
              <a-textarea v-model:value="formModel.remark" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <a-button @click="handleCancel">取消</a-button>
      <a-button v-if="currentStep === 1" @click="handlePrev">上一步</a-button>
      <a-button
        v-if="currentStep === 0"
        type="primary"
        :disabled="!selectedOrder"
        @click="handleNext"
      >
        下一步
      </a-button>
      <a-button
        v-if="currentStep === 1"
        type="primary"
        :loading="confirmLoading"
        @click="handleSubmit"
      >
        提交退货
      </a-button>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlatformTag, PlatformSelect } from '@/components/Platform'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'
import { getReturnableOrders, createReturnInbound } from '@/api/wms/return-inbound'
import { isSuccess } from '@/api'
import { ReturnReasonMap } from '@/api/wms/return-inbound/types'
import type {
  ReturnableOrderVO,
  ReturnableOrderQO,
  ReturnInboundDTO
} from '@/api/wms/return-inbound/types'

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const confirmLoading = ref(false)
const currentStep = ref(0)

// ==================== 步骤1: 选择订单 ====================

const orderLoading = ref(false)
const orderList = ref<ReturnableOrderVO[]>([])
const orderPagination = reactive({ current: 1, pageSize: 10, total: 0 })
const searchForm = reactive<ReturnableOrderQO>({ keyword: undefined, platform: undefined })
const selectedOrder = ref<ReturnableOrderVO | null>(null)
const selectedKeys = ref<number[]>([])

const orderColumns = [
  { title: '平台订单号', dataIndex: 'platformOrderId', width: 160 },
  { title: '平台', key: 'platform', width: 80 },
  {
    title: '发货时间',
    dataIndex: 'outboundTime',
    width: 100,
    customRender: ({ text }: any) => formatDate(text, 'MM-DD')
  },
  { title: 'SKU', key: 'sku', width: 200 },
  { title: '发货', dataIndex: 'shippedQuantity', width: 60, align: 'center' as const },
  { title: '处理中', dataIndex: 'inFlightQuantity', width: 70, align: 'center' as const },
  { title: '可退', dataIndex: 'returnableQuantity', width: 60, align: 'center' as const }
]

const rowSelection = reactive({
  type: 'radio' as const,
  selectedRowKeys: selectedKeys,
  onChange: (keys: number[], rows: ReturnableOrderVO[]) => {
    selectedKeys.value = keys
    selectedOrder.value = rows[0] || null
  }
})

const loadOrders = async () => {
  orderLoading.value = true
  try {
    const result = await getReturnableOrders({
      ...searchForm,
      current: orderPagination.current,
      size: orderPagination.pageSize
    })
    if (isSuccess(result)) {
      orderList.value = result.data?.records || []
      orderPagination.total = result.data?.total || 0
    }
  } finally {
    orderLoading.value = false
  }
}

const doSearch = () => {
  orderPagination.current = 1
  loadOrders()
}

const handleTableChange = (pagination: any) => {
  orderPagination.current = pagination.current
  orderPagination.pageSize = pagination.pageSize
  loadOrders()
}

// ==================== 步骤2: 填写退货信息 ====================

const formRef = ref()
const formModel = reactive({
  warehouseId: undefined as number | undefined,
  returnDate: dayjs().format('YYYY-MM-DD'),
  quantity: 0,
  returnReason: undefined as string | undefined,
  remark: undefined as string | undefined
})

const formRules = {
  warehouseId: [{ required: true, message: '请选择入库仓库' }],
  returnDate: [{ required: true, message: '请选择退货日期' }],
  quantity: [{ required: true, message: '请输入退货数量' }],
  returnReason: [{ required: true, message: '请选择退货原因' }]
}

// 选中订单后初始化退货数量（质检分配交由平台，货主不填）
watch(selectedOrder, order => {
  if (order) {
    formModel.quantity = order.returnableQuantity
  }
})

// ==================== 步骤控制 ====================

const handleNext = () => {
  if (!selectedOrder.value) {
    message.warning('请选择一个订单')
    return
  }
  currentStep.value = 1
}

const handlePrev = () => {
  currentStep.value = 0
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  const dto: ReturnInboundDTO = {
    orderItemId: selectedOrder.value!.orderItemId,
    warehouseId: formModel.warehouseId!,
    returnDate: formModel.returnDate,
    returnReason: formModel.returnReason!,
    remark: formModel.remark,
    quantity: formModel.quantity
  }

  confirmLoading.value = true
  try {
    const result = await createReturnInbound(dto)
    if (isSuccess(result)) {
      message.success('退货申请已提交，待海外仓平台质检')
      visible.value = false
      emit('success')
    }
  } finally {
    confirmLoading.value = false
  }
}

const handleCancel = () => {
  visible.value = false
}

// ==================== 工具方法 ====================

const formatDate = (dateStr?: string, format = 'YYYY-MM-DD') => {
  if (!dateStr) return '-'
  return dayjs(dateStr).format(format)
}

// ==================== 公开方法 ====================

const open = () => {
  visible.value = true
  currentStep.value = 0
  selectedOrder.value = null
  selectedKeys.value = []
  searchForm.keyword = undefined
  searchForm.platform = undefined
  Object.assign(formModel, {
    warehouseId: undefined,
    returnDate: dayjs().format('YYYY-MM-DD'),
    quantity: 0,
    returnReason: undefined,
    remark: undefined
  })
  loadOrders()
}

defineExpose({ open })
</script>

<style scoped>
/* 信息卡片背景 - 使用 :deep 穿透 antd Card 样式 */
.info-card :deep(.ant-card-body) {
  background-color: #fafafa;
}

/* 入库分配区域 */
.allocation-group {
  background: var(--ant-color-bg-layout);
  border: 1px solid var(--ant-color-border);
  border-radius: var(--ant-border-radius);
  padding: 12px 16px;
}

.allocation-row {
  display: flex;
  align-items: flex-end;
  gap: 24px;
}

.allocation-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.allocation-label {
  font-size: 12px;
  color: var(--ant-color-text-secondary);
}

.all-qualified-hint {
  color: var(--ant-color-success);
  font-size: 13px;
  padding-bottom: 4px;
}

.allocation-summary {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--ant-color-border);
  font-size: 12px;
  color: var(--ant-color-text-secondary);
}

.valid-icon {
  color: var(--ant-color-success);
  margin-left: 4px;
}
</style>
