<template>
  <page-container
    class="custom-outbound-form-page"
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
          <a-tag v-if="isUpdateForm && orderStatus" style="margin-left: 12px">
            {{ CustomOutboundStatusMap[orderStatus] || orderStatus }}
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
            <a-form-item label="出库单号">
              <a-input
                :value="isUpdateForm ? outboundNo : ''"
                placeholder="系统自动生成"
                disabled
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
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
          <a-col :xs="24" :sm="12">
            <a-form-item
              label="出库仓库"
              name="warehouseId"
              :rules="[{ required: true, message: '请选择出库仓库', trigger: 'change' }]"
            >
              <warehouse-select
                v-model:value="formModel.warehouseId"
                warehouse-type="OWN"
                placeholder="请选择出库仓库"
                width="100%"
                @change="handleWarehouseChange"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item
              label="出库类型"
              name="customType"
              :rules="[{ required: true, message: '请选择出库类型', trigger: 'change' }]"
            >
              <a-select
                v-model:value="formModel.customType"
                placeholder="请选择出库类型"
                @change="markDirty"
              >
                <a-select-option
                  v-for="(label, value) in CustomOutboundTypeMap"
                  :key="value"
                  :value="value"
                >
                  {{ label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="关联单号" name="refNo">
              <a-input
                v-model:value="formModel.refNo"
                placeholder="线下订单号/退供单号等(可选)"
                :maxlength="100"
                allow-clear
                @change="markDirty"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="物流产品" name="logisticsProductId">
              <a-select
                v-model:value="formModel.logisticsProductId"
                placeholder="需要运输或计费时选择（选填）"
                allow-clear
                :loading="productLoading"
                @change="markDirty"
              >
                <a-select-option v-for="product in productOptions" :key="product.id" :value="product.id">
                  <span>{{ product.productName }}</span>
                  <span style="color: #fa8c16; margin-left: 8px">₽{{ product.unitPrice }}/次</span>
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </div>

      <!-- 收货人信息区块（可选折叠，销毁类无收货人可不填） -->
      <div class="form-section receiver-section">
        <a-collapse v-model:active-key="receiverActiveKey" ghost expand-icon-position="end">
          <a-collapse-panel key="receiver">
            <template #header>
              <div class="section-title collapse-title">
                <environment-outlined class="section-icon" />
                收货人信息
                <span class="optional-hint">（可选，销毁报废等无收货人场景可不填）</span>
              </div>
            </template>
            <a-row :gutter="16">
              <a-col :xs="24" :sm="12">
                <a-form-item label="收件人" name="receiverName">
                  <a-input
                    v-model:value="formModel.receiverName"
                    placeholder="请输入收件人姓名(可选)"
                    :maxlength="100"
                    allow-clear
                    @change="markDirty"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12">
                <a-form-item label="电话" name="receiverPhone">
                  <a-input
                    v-model:value="formModel.receiverPhone"
                    placeholder="请输入收件人电话(可选)"
                    :maxlength="50"
                    allow-clear
                    @change="markDirty"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :xs="24">
                <a-form-item label="收货地址" name="receiverAddress">
                  <a-input
                    v-model:value="formModel.receiverAddress"
                    placeholder="请输入收货地址(可选)"
                    :maxlength="500"
                    allow-clear
                    @change="markDirty"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </a-collapse-panel>
        </a-collapse>
      </div>

      <!-- 出库明细区块 -->
      <div class="form-section">
        <div class="section-title">
          <inbox-outlined class="section-icon" />
          出库明细
        </div>
        <a-table
          :data-source="outboundItems"
          :columns="itemColumns"
          :pagination="false"
          row-key="rowKey"
          size="small"
          :scroll="{ x: 800 }"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'skuCode'">
              <sku-select-input
                v-model="record.skuCode"
                placeholder="请选择SKU"
                @sku-selected="row => handleSkuSelected(record, row)"
              />
            </template>
            <template v-else-if="column.key === 'quantity'">
              <a-input-number
                v-model:value="record.quantity"
                :min="1"
                style="width: 100%"
                @change="markDirty"
              />
            </template>
            <template v-else-if="column.key === 'availableStock'">
              <!-- 每行实时显示该 SKU 在所选仓库的可售库存，数量超过可售时红字提示 -->
              <template v-if="!formModel.warehouseId || !record.skuCode">
                <span class="stock-placeholder">—</span>
              </template>
              <template v-else>
                <div class="stock-cell" :class="{ 'stock-insufficient': isRowInsufficient(record) }">
                  <span class="stock-value">{{ availableOf(record.skuCode) }}</span>
                  <span v-if="isRowInsufficient(record)" class="stock-warning">
                    超出可售 {{ (record.quantity ?? 0) - availableOf(record.skuCode) }}
                  </span>
                </div>
              </template>
            </template>
            <template v-else-if="column.key === 'remark'">
              <a-input
                v-model:value="record.remark"
                placeholder="备注(可选)"
                :maxlength="500"
                @change="markDirty"
              />
            </template>
            <template v-else-if="column.key === 'operate'">
              <a class="del-link" @click="removeItem(index)">删除</a>
            </template>
          </template>
        </a-table>
        <a-button type="dashed" block style="margin-top: 12px" @click="addItem">
          <plus-outlined />
          添加明细
        </a-button>
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
            @change="markDirty"
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
  EditOutlined,
  EnvironmentOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'
import PageContainer from '#/layout/components/PageContainer'
import { emitter } from '@/hooks/mitt'
import { useUnsavedChangesGuard } from '@/hooks/useUnsavedChangesGuard'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import SkuSelectInput from '@/components/Sku/SkuSelectInput.vue'
import { listOwnerLogisticsProducts } from '@/api/wms/logistics-product'
import type { LogisticsProductVO } from '@/api/wms/logistics-product/types'
import {
  createCustomOutbound,
  updateCustomOutbound,
  getCustomOutboundDetail,
  submitCustomOutbound,
  batchQueryAvailableStock
} from '@/api/wms/custom-outbound'
import type { CustomOutboundDTO, CustomOutboundItemDTO } from '@/api/wms/custom-outbound/types'
import { CustomOutboundStatus, CustomOutboundStatusMap, CustomOutboundTypeMap } from '@/api/wms/custom-outbound/types'
import type { SkuRow } from '@/components/Sku/types'
import { doRequest } from '@/utils/axios/request'
import { isSuccess } from '@/api'

defineOptions({ name: 'CustomOutboundFormPage' })

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()

const formMode = computed(() => route.params.mode as 'create' | 'edit')

const outboundOrderId = computed(() => {
  const id = route.params.id as string
  return id ? Number(id) : undefined
})

const loading = ref(false)
const submitLoading = ref(false)
const hasUnsavedChanges = ref(false)

const { confirmIfDirty, resetDirty, useRouteLeaveGuard } = useUnsavedChangesGuard(hasUnsavedChanges)
useRouteLeaveGuard()

const orderStatus = ref<string>('')
const outboundNo = ref<string>('')

const isUpdateForm = computed(() => formMode.value === 'edit')

const pageTitle = computed(() => (isUpdateForm.value ? '编辑自定义出库单' : '新建自定义出库单'))

const outboundDateValue = ref<string>()

// 收货人区块折叠（默认收起，填写过则展开）
const receiverActiveKey = ref<string[]>([])

// 明细行（rowKey 仅用于表格 key，递增）
interface ItemRow {
  rowKey: number
  skuCode?: string
  quantity: number
  remark?: string
}
let rowKeySeq = 0
const outboundItems = ref<ItemRow[]>([])

const productOptions = ref<LogisticsProductVO[]>([])
const productLoading = ref(false)

const loadProductOptions = async () => {
  productLoading.value = true
  try {
    const result = await listOwnerLogisticsProducts()
    if (isSuccess(result)) productOptions.value = result.data || []
  } finally {
    productLoading.value = false
  }
}

// 可售库存缓存：skuCode -> availableQuantity（随仓库切换/SKU选择刷新）
const stockMap = ref<Record<string, number>>({})

const itemColumns = [
  { title: 'SKU', key: 'skuCode', width: 280 },
  { title: '出库数量', key: 'quantity', width: 130, align: 'center' },
  { title: '可售库存', key: 'availableStock', width: 130, align: 'center' },
  { title: '备注', key: 'remark', width: 180 },
  { title: '操作', key: 'operate', width: 80, align: 'center' }
]

const formModel = reactive<CustomOutboundDTO>({
  id: undefined,
  warehouseId: undefined as unknown as number,
  outboundDate: '',
  customType: undefined as unknown as string,
  refNo: undefined,
  receiverName: undefined,
  receiverPhone: undefined,
  receiverAddress: undefined,
  logisticsProductId: undefined,
  remark: undefined,
  items: []
})

const markDirty = () => {
  hasUnsavedChanges.value = true
}

// ==================== 可售库存 ====================

const availableOf = (skuCode?: string): number => {
  if (!skuCode) return 0
  return stockMap.value[skuCode] ?? 0
}

const isRowInsufficient = (record: ItemRow): boolean => {
  if (!formModel.warehouseId || !record.skuCode) return false
  return (record.quantity ?? 0) > availableOf(record.skuCode)
}

/**
 * 刷新可售库存（查询当前明细所有 SKU 在所选仓库的可售量）
 */
const refreshStockMap = async () => {
  const warehouseId = formModel.warehouseId
  const skuCodes = [...new Set(outboundItems.value.map(i => i.skuCode).filter(Boolean))] as string[]
  if (!warehouseId || skuCodes.length === 0) {
    stockMap.value = {}
    return
  }
  try {
    const result = await batchQueryAvailableStock({ warehouseId, skuCodes })
    if (isSuccess(result)) {
      stockMap.value = result.data || {}
    }
  } catch (e) {
    console.error('查询可售库存失败:', e)
  }
}

const handleWarehouseChange = () => {
  markDirty()
  refreshStockMap()
}

// ==================== 表单操作 ====================

const handleOutboundDateChange = (date: string | null) => {
  formModel.outboundDate = date || ''
  markDirty()
}

const addItem = () => {
  outboundItems.value.push({ rowKey: rowKeySeq++, skuCode: undefined, quantity: 1 })
  markDirty()
}

const removeItem = (index: number) => {
  outboundItems.value.splice(index, 1)
  markDirty()
}

const handleSkuSelected = (record: ItemRow, row: SkuRow | SkuRow[] | null) => {
  const sku = Array.isArray(row) ? row[0] : row
  record.skuCode = sku?.skuCode
  markDirty()
  refreshStockMap()
}

const validateForm = async (): Promise<boolean> => {
  try {
    await formRef.value?.validate()
  } catch {
    return false
  }

  const validItems = outboundItems.value.filter(i => i.skuCode && i.quantity > 0)
  if (validItems.length === 0) {
    message.error('请至少添加一条有效明细（选择SKU并填写出库数量）')
    return false
  }

  // 校验 SKU 不重复
  const codes = validItems.map(i => i.skuCode)
  if (new Set(codes).size !== codes.length) {
    message.error('存在重复的SKU，请合并后再提交')
    return false
  }

  return true
}

const buildSubmitData = (): CustomOutboundDTO => {
  const items: CustomOutboundItemDTO[] = outboundItems.value
    .filter(i => i.skuCode && i.quantity > 0)
    .map(i => ({
      skuCode: i.skuCode!,
      quantity: i.quantity,
      remark: i.remark
    }))
  return {
    ...formModel,
    items
  }
}

// 静默保存（新建则创建，编辑则更新），返回出库单ID
const submitFormSilent = async (): Promise<number | null> => {
  const valid = await validateForm()
  if (!valid) return null

  const data = buildSubmitData()
  submitLoading.value = true
  try {
    // 以 formModel.id 判断（新建页首次保存成功后回填 id，避免提交被库存拦截后重复建草稿）
    if (formModel.id) {
      const result = await updateCustomOutbound(data)
      if (isSuccess(result)) {
        resetDirty()
        return formModel.id!
      }
      message.error(result.message || '保存失败')
      return null
    } else {
      const result = await createCustomOutbound(data)
      if (isSuccess(result)) {
        resetDirty()
        formModel.id = result.data!
        return result.data!
      }
      message.error(result.message || '保存失败')
      return null
    }
  } catch (error) {
    console.error('提交失败:', error)
    message.error('提交失败，请重试')
    return null
  } finally {
    submitLoading.value = false
  }
}

const goBackToList = async () => {
  resetDirty()
  const currentPath = route.path
  await router.push('/wms/custom-outbound')
  emitter.emit('close-current-tab', currentPath)
}

const handleBack = () => {
  confirmIfDirty(goBackToList)
}

const handleCancel = () => {
  handleBack()
}

const handleSaveDraft = async () => {
  const id = await submitFormSilent()
  if (id) {
    message.success('保存成功')
    emitter.emit('refresh-custom-outbound-list')
    goBackToList()
  }
}

const handleSubmit = async () => {
  // 先做表单校验：有必填项未填则直接提示，不再弹出确认框
  const valid = await validateForm()
  if (!valid) return

  // 前端预警：数量超过可售的行给出提示（后端提交时仍会硬校验）
  const insufficientRows = outboundItems.value.filter(i => isRowInsufficient(i))
  const warnContent =
    insufficientRows.length > 0
      ? `存在 ${insufficientRows.length} 行数量超过可售库存，提交将被库存校验拦截。确认继续吗？`
      : '确认要提交吗？提交后将按可售库存校验并预占，流转至海外仓作业台下架/打包/签出。'

  Modal.confirm({
    title: '提交出库单',
    content: warnContent,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      const id = await submitFormSilent()
      if (!id) return
      try {
        const result = await submitCustomOutbound(id)
        if (isSuccess(result)) {
          message.success('提交成功')
          resetDirty()
          emitter.emit('refresh-custom-outbound-list')
          goBackToList()
        } else {
          // 库存不足时后端 message 已带 SKU 缺口明细；单据已保存为草稿留在表单
          message.error(result.message || '提交失败')
          refreshStockMap()
        }
      } catch (e) {
        message.error('提交失败，请重试')
      }
    }
  })
}

const loadOutboundDetail = (id: number) => {
  loading.value = true
  doRequest(getCustomOutboundDetail(id), {
    onSuccess: res => {
      const detail = res.data!
      if (detail.orderStatus !== CustomOutboundStatus.DRAFT) {
        message.warning('只有草稿状态的出库单可以编辑')
        goBackToList()
        return
      }
      orderStatus.value = detail.orderStatus
      outboundNo.value = detail.outboundNo
      formModel.id = detail.id
      formModel.warehouseId = detail.warehouseId
      formModel.outboundDate = detail.outboundDate
      formModel.customType = detail.customType
      formModel.refNo = detail.refNo
      formModel.receiverName = detail.receiverName
      formModel.receiverPhone = detail.receiverPhone
      formModel.receiverAddress = detail.receiverAddress
      formModel.logisticsProductId = detail.logisticsProductId
      formModel.remark = detail.remark
      outboundDateValue.value = detail.outboundDate
      outboundItems.value = (detail.items || []).map(item => ({
        rowKey: rowKeySeq++,
        skuCode: item.skuCode,
        quantity: item.quantity,
        remark: item.remark
      }))
      // 填写过收货人则默认展开收货人区块
      if (detail.receiverName || detail.receiverPhone || detail.receiverAddress) {
        receiverActiveKey.value = ['receiver']
      }
      refreshStockMap()
    },
    onFinally: () => {
      loading.value = false
    }
  })
}

const initPage = () => {
  if (formMode.value === 'edit' && outboundOrderId.value) {
    loadOutboundDetail(outboundOrderId.value)
  } else {
    const today = new Date().toISOString().split('T')[0]
    outboundDateValue.value = today
    formModel.outboundDate = today
    addItem()
  }
}

onMounted(() => {
  initPage()
  loadProductOptions()
})
</script>

<style scoped>
.custom-outbound-form-page {
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

.form-content {
  flex: 1;
  padding: 16px 24px;
  overflow-y: auto;
}

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

/* 收货人折叠区块：标题由 collapse header 承载，去掉默认下边距/分隔线 */
.receiver-section {
  padding: 4px 8px;
}

.collapse-title {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.optional-hint {
  font-size: 12px;
  font-weight: 400;
  color: #8c8c8c;
  margin-left: 4px;
}

.del-link {
  color: #f5222d;
}

/* 可售库存列 */
.stock-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1.4;
}

.stock-value {
  font-weight: 600;
  color: #262626;
}

.stock-insufficient .stock-value {
  color: #f5222d;
}

.stock-warning {
  font-size: 11px;
  color: #f5222d;
}

.stock-placeholder {
  color: #bfbfbf;
}

:deep(.ant-form-item-label > label) {
  font-size: 13px;
  color: #595959;
}
</style>
