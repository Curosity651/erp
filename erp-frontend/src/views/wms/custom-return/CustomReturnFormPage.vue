<template>
  <page-container
    class="custom-return-form-page"
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
        <!-- 场景引导：按订单退货请走「退货入库」 -->
        <div class="form-tip">如需按订单退货并核销可退数量，请使用【退货入库】</div>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item
              label="退货单号"
              name="inboundNo"
              :rules="[
                { required: true, message: '请输入退货单号', trigger: 'blur' },
                { max: 50, message: '退货单号长度不能超过50个字符', trigger: 'blur' }
              ]"
            >
              <a-input
                v-model:value="formModel.inboundNo"
                placeholder="请输入退货单号"
                :disabled="isUpdateForm"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item
              label="退货类型"
              name="returnType"
              :rules="[{ required: true, message: '请选择退货类型', trigger: 'change' }]"
            >
              <a-select
                v-model:value="formModel.returnType"
                placeholder="请选择退货类型"
                @change="markDirty"
              >
                <a-select-option
                  v-for="(label, value) in CUSTOM_RETURN_TYPE_MAP"
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
            <a-form-item
              label="入库仓库"
              name="warehouseId"
              :rules="[{ required: true, message: '请选择入库仓库', trigger: 'change' }]"
            >
              <warehouse-select
                v-model:value="formModel.warehouseId"
                warehouse-type="OWN"
                placeholder="请选择入库仓库"
                width="100%"
                @change="markDirty"
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
              label="关联单号"
              name="refNo"
              :rules="[{ max: 100, message: '关联单号长度不能超过100个字符', trigger: 'blur' }]"
            >
              <a-input
                v-model:value="formModel.refNo"
                placeholder="选填，平台订单号/物流追踪号等参考信息"
                :maxlength="100"
                @change="markDirty"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </div>

      <!-- 退货明细区块 -->
      <div class="form-section">
        <div class="section-title">
          <inbox-outlined class="section-icon" />
          退货明细
        </div>
        <a-table
          :data-source="returnItems"
          :columns="itemColumns"
          :pagination="false"
          row-key="rowKey"
          size="small"
          :scroll="{ x: 780 }"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'skuCode'">
              <sku-select-input
                v-model="record.skuCode"
                placeholder="请选择SKU"
                @sku-selected="row => handleSkuSelected(record, row)"
              />
            </template>
            <template v-else-if="column.key === 'expectedQuantity'">
              <a-input-number
                v-model:value="record.expectedQuantity"
                :min="1"
                style="width: 100%"
                @change="markDirty"
              />
            </template>
            <template v-else-if="column.key === 'expectedQuality'">
              <a-select
                v-model:value="record.expectedQuality"
                style="width: 100%"
                @change="markDirty"
              >
                <a-select-option
                  v-for="(label, value) in EXPECTED_QUALITY_MAP"
                  :key="value"
                  :value="value"
                >
                  {{ label }}
                </a-select-option>
              </a-select>
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
  PlusOutlined
} from '@ant-design/icons-vue'
import PageContainer from '#/layout/components/PageContainer'
import { emitter } from '@/hooks/mitt'
import { useUnsavedChangesGuard } from '@/hooks/useUnsavedChangesGuard'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import SkuSelectInput from '@/components/Sku/SkuSelectInput.vue'
import {
  createCustomReturn,
  updateCustomReturn,
  getCustomReturnDetail,
  submitCustomReturn
} from '@/api/wms/custom-return'
import type { CustomReturnDTO, CustomReturnItemDTO } from '@/api/wms/custom-return/types'
import { CUSTOM_RETURN_TYPE_MAP, EXPECTED_QUALITY_MAP } from '@/api/wms/custom-return/types'
import { InboundStatusMap, InboundStatus } from '@/api/wms/purchase-inbound/types'
import type { SkuRow } from '@/components/Sku/types'
import { doRequest } from '@/utils/axios/request'
import { isSuccess } from '@/api'

defineOptions({ name: 'CustomReturnFormPage' })

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()

const formMode = computed(() => route.params.mode as 'create' | 'edit')

const returnOrderId = computed(() => {
  const id = route.params.id as string
  return id ? Number(id) : undefined
})

const loading = ref(false)
const submitLoading = ref(false)
const hasUnsavedChanges = ref(false)

const { confirmIfDirty, resetDirty, useRouteLeaveGuard } = useUnsavedChangesGuard(hasUnsavedChanges)
useRouteLeaveGuard()

const orderStatus = ref<string>('')

const isUpdateForm = computed(() => formMode.value === 'edit')

const pageTitle = computed(() => (isUpdateForm.value ? '编辑自定义退货单' : '新建自定义退货单'))

const inboundDateValue = ref<string>()

// 明细行（rowKey 仅用于表格 key，递增）
interface ItemRow {
  rowKey: number
  skuCode?: string
  expectedQuantity: number
  expectedQuality: string
  remark?: string
}
let rowKeySeq = 0
const returnItems = ref<ItemRow[]>([])

const itemColumns = [
  { title: 'SKU', key: 'skuCode', width: 280 },
  { title: '应退数量', key: 'expectedQuantity', width: 140, align: 'center' },
  { title: '货品预判', key: 'expectedQuality', width: 120, align: 'center' },
  { title: '备注', key: 'remark', width: 180 },
  { title: '操作', key: 'operate', width: 80, align: 'center' }
]

const formModel = reactive<CustomReturnDTO>({
  id: undefined,
  inboundNo: '',
  returnType: undefined as unknown as string,
  refNo: undefined,
  warehouseId: undefined as unknown as number,
  inboundDate: '',
  remark: undefined,
  items: []
})

const markDirty = () => {
  hasUnsavedChanges.value = true
}

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

const handleInboundDateChange = (date: string | null) => {
  formModel.inboundDate = date || ''
  markDirty()
}

const addItem = () => {
  // 货品预判默认良品
  returnItems.value.push({
    rowKey: rowKeySeq++,
    skuCode: undefined,
    expectedQuantity: 1,
    expectedQuality: 'GOOD'
  })
  markDirty()
}

const removeItem = (index: number) => {
  returnItems.value.splice(index, 1)
  markDirty()
}

const handleSkuSelected = (record: ItemRow, row: SkuRow | SkuRow[] | null) => {
  const sku = Array.isArray(row) ? row[0] : row
  record.skuCode = sku?.skuCode
  markDirty()
}

const validateForm = async (): Promise<boolean> => {
  try {
    await formRef.value?.validate()
  } catch {
    return false
  }

  const validItems = returnItems.value.filter(i => i.skuCode && i.expectedQuantity > 0)
  if (validItems.length === 0) {
    message.error('请至少添加一条有效明细（选择SKU并填写应退数量）')
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

const buildSubmitData = (): CustomReturnDTO => {
  const items: CustomReturnItemDTO[] = returnItems.value
    .filter(i => i.skuCode && i.expectedQuantity > 0)
    .map(i => ({
      skuCode: i.skuCode!,
      expectedQuantity: i.expectedQuantity,
      expectedQuality: i.expectedQuality,
      remark: i.remark
    }))
  return {
    ...formModel,
    items
  }
}

// 静默保存（新建则创建，编辑则更新），返回退货单ID
const submitFormSilent = async (): Promise<number | null> => {
  const valid = await validateForm()
  if (!valid) return null

  const data = buildSubmitData()
  submitLoading.value = true
  try {
    if (isUpdateForm.value) {
      const result = await updateCustomReturn(data)
      if (isSuccess(result)) {
        resetDirty()
        return formModel.id!
      }
      message.error(result.message || '保存失败')
      return null
    } else {
      const result = await createCustomReturn(data)
      if (isSuccess(result)) {
        resetDirty()
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
  await router.push('/wms/custom-return')
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
    emitter.emit('refresh-custom-return-list')
    goBackToList()
  }
}

const handleSubmit = async () => {
  // 先做表单校验：有必填项未填则直接提示，不再弹出确认框
  const valid = await validateForm()
  if (!valid) return

  Modal.confirm({
    title: '提交退货单',
    content: '确认要提交吗？提交后将流转至平台收货上架。',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      const id = await submitFormSilent()
      if (!id) return
      doRequest(submitCustomReturn(id), {
        successMessage: '提交成功',
        onSuccess: () => {
          resetDirty()
          emitter.emit('refresh-custom-return-list')
          goBackToList()
        }
      })
    }
  })
}

const loadReturnDetail = (id: number) => {
  loading.value = true
  doRequest(getCustomReturnDetail(id), {
    onSuccess: res => {
      const detail = res.data!
      if (detail.orderStatus !== InboundStatus.DRAFT) {
        message.warning('只有草稿状态的退货单可以编辑')
        goBackToList()
        return
      }
      orderStatus.value = detail.orderStatus
      formModel.id = detail.id
      formModel.inboundNo = detail.inboundNo
      formModel.returnType = detail.returnType!
      formModel.refNo = detail.refNo
      formModel.warehouseId = detail.warehouseId
      formModel.inboundDate = detail.inboundDate
      formModel.remark = detail.remark
      inboundDateValue.value = detail.inboundDate
      returnItems.value = (detail.items || []).map(item => ({
        rowKey: rowKeySeq++,
        skuCode: item.skuCode,
        expectedQuantity: item.expectedQuantity,
        expectedQuality: item.expectedQuality || 'GOOD',
        remark: item.remark
      }))
    },
    onFinally: () => {
      loading.value = false
    }
  })
}

const initPage = () => {
  if (formMode.value === 'edit' && returnOrderId.value) {
    loadReturnDetail(returnOrderId.value)
  } else {
    const today = new Date().toISOString().split('T')[0]
    inboundDateValue.value = today
    formModel.inboundDate = today
    addItem()
  }
}

onMounted(() => {
  initPage()
})
</script>

<style scoped>
.custom-return-form-page {
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

.form-tip {
  margin-bottom: 12px;
  font-size: 12px;
  color: #8c8c8c;
}

.del-link {
  color: #f5222d;
}

:deep(.ant-form-item-label > label) {
  font-size: 13px;
  color: #595959;
}
</style>
