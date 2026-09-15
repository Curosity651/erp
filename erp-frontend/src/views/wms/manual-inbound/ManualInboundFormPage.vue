<template>
  <page-container
    class="manual-inbound-form-page"
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
              label="入库单号"
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
        </a-row>
      </div>

      <!-- 入库明细区块 -->
      <div class="form-section">
        <div class="section-title">
          <inbox-outlined class="section-icon" />
          入库明细
        </div>
        <div class="detail-toolbar">
          <span class="detail-tip">包装尺寸自动取自 SKU 资料</span>
          <a-space>
            <a-button size="small" :loading="templateLoading" @click="downloadTemplate">
              <download-outlined />
              下载模板
            </a-button>
            <a-upload
              accept=".xlsx"
              :show-upload-list="false"
              :before-upload="beforeImportTemplate"
            >
              <a-button size="small" :loading="templateLoading">
                <upload-outlined />
                导入模板
              </a-button>
            </a-upload>
          </a-space>
        </div>
        <a-table
          :data-source="inboundItems"
          :columns="itemColumns"
          :pagination="false"
          row-key="rowKey"
          size="small"
          :scroll="{ x: 980 }"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'skuCode'">
              <sku-select-input
                v-model="record.skuCode"
                placeholder="请选择SKU"
                @sku-selected="row => handleSkuSelected(record, row)"
              />
            </template>
            <template v-else-if="column.key === 'dimensions'">
              <span :class="{ 'missing-dimension': !hasDimensions(record) }">
                {{ dimensionsText(record) }}
              </span>
            </template>
            <template v-else-if="column.key === 'volume'">
              {{ volumeText(record) }}
            </template>
            <template v-else-if="column.key === 'expectedQuantity'">
              <a-input-number
                v-model:value="record.expectedQuantity"
                :min="1"
                style="width: 100%"
                @change="markDirty"
              />
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
        <div class="dimension-summary">
          <a-tag color="blue">累计长：{{ totalLengthText }}</a-tag>
          <a-tag color="blue">累计宽：{{ totalWidthText }}</a-tag>
          <a-tag color="blue">累计高：{{ totalHeightText }}</a-tag>
          <a-tag color="green">总体积：{{ totalVolumeText }}</a-tag>
          <span v-if="missingDimensionCount > 0" class="missing-dimension">
            {{ missingDimensionCount }} 个 SKU 缺少包装尺寸
          </span>
        </div>
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
  PlusOutlined,
  DownloadOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'
import PageContainer from '#/layout/components/PageContainer'
import { emitter } from '@/hooks/mitt'
import { useUnsavedChangesGuard } from '@/hooks/useUnsavedChangesGuard'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import SkuSelectInput from '@/components/Sku/SkuSelectInput.vue'
import {
  createManualInbound,
  updateManualInbound,
  getManualInboundDetail,
  submitManualInbound
} from '@/api/wms/manual-inbound'
import { listSkuByCodes } from '@/api/product/sku'
import type { ManualInboundDTO, ManualInboundItemDTO } from '@/api/wms/manual-inbound/types'
import { InboundStatusMap, InboundStatus } from '@/api/wms/purchase-inbound/types'
import type { SkuRow } from '@/components/Sku/types'
import { doRequest } from '@/utils/axios/request'
import { isSuccess } from '@/api'
import {
  downloadManualInboundTemplate,
  parseManualInboundTemplateFile
} from './manual-inbound-excel'

defineOptions({ name: 'ManualInboundFormPage' })

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()

const formMode = computed(() => route.params.mode as 'create' | 'edit')

const inboundOrderId = computed(() => {
  const id = route.params.id as string
  return id ? Number(id) : undefined
})

const loading = ref(false)
const submitLoading = ref(false)
const templateLoading = ref(false)
const hasUnsavedChanges = ref(false)

const { confirmIfDirty, resetDirty, useRouteLeaveGuard } = useUnsavedChangesGuard(hasUnsavedChanges)
useRouteLeaveGuard()

const orderStatus = ref<string>('')

const isUpdateForm = computed(() => formMode.value === 'edit')

const pageTitle = computed(() => (isUpdateForm.value ? '编辑商品入库' : '新建商品入库'))

const inboundDateValue = ref<string>()

// 明细行（rowKey 仅用于表格 key，递增）
interface ItemRow {
  rowKey: number
  skuCode?: string
  expectedQuantity: number
  outerLengthMm?: number
  outerWidthMm?: number
  outerHeightMm?: number
  remark?: string
}
let rowKeySeq = 0
const inboundItems = ref<ItemRow[]>([])

const itemColumns = [
  { title: 'SKU', key: 'skuCode', width: 280 },
  { title: '包装尺寸（长×宽×高）', key: 'dimensions', width: 180, align: 'center' },
  { title: '应到数量', key: 'expectedQuantity', width: 140, align: 'center' },
  { title: '包装体积', key: 'volume', width: 120, align: 'center' },
  { title: '备注', key: 'remark', width: 180 },
  { title: '操作', key: 'operate', width: 80, align: 'center' }
]

const formModel = reactive<ManualInboundDTO>({
  id: undefined,
  inboundNo: '',
  warehouseId: undefined as unknown as number,
  inboundDate: '',
  remark: undefined,
  items: []
})

const markDirty = () => {
  hasUnsavedChanges.value = true
}

const hasDimensions = (record: ItemRow) =>
  [record.outerLengthMm, record.outerWidthMm, record.outerHeightMm].every(
    value => Number(value) > 0
  )

const dimensionsText = (record: ItemRow) =>
  hasDimensions(record)
    ? (record.outerLengthMm! / 10).toFixed(1) +
      ' × ' +
      (record.outerWidthMm! / 10).toFixed(1) +
      ' × ' +
      (record.outerHeightMm! / 10).toFixed(1) +
      ' cm'
    : '待维护'

const volumeCbm = (record: ItemRow) =>
  hasDimensions(record)
    ? (record.outerLengthMm! *
        record.outerWidthMm! *
        record.outerHeightMm! *
        Number(record.expectedQuantity || 0)) /
      1_000_000_000
    : 0

const volumeText = (record: ItemRow) =>
  hasDimensions(record) ? volumeCbm(record).toFixed(4) + ' m³' : '-'

const totalLengthMm = computed(() =>
  inboundItems.value.reduce(
    (sum, item) => sum + (hasDimensions(item) ? item.outerLengthMm! * item.expectedQuantity : 0),
    0
  )
)
const totalWidthMm = computed(() =>
  inboundItems.value.reduce(
    (sum, item) => sum + (hasDimensions(item) ? item.outerWidthMm! * item.expectedQuantity : 0),
    0
  )
)
const totalHeightMm = computed(() =>
  inboundItems.value.reduce(
    (sum, item) => sum + (hasDimensions(item) ? item.outerHeightMm! * item.expectedQuantity : 0),
    0
  )
)
const totalVolumeCbm = computed(() => inboundItems.value.reduce((sum, item) => sum + volumeCbm(item), 0))
const missingDimensionCount = computed(() =>
  inboundItems.value.filter(item => !hasDimensions(item)).length
)
const totalLengthText = computed(() => (totalLengthMm.value / 10).toFixed(1) + ' cm')
const totalWidthText = computed(() => (totalWidthMm.value / 10).toFixed(1) + ' cm')
const totalHeightText = computed(() => (totalHeightMm.value / 10).toFixed(1) + ' cm')
const totalVolumeText = computed(() => totalVolumeCbm.value.toFixed(4) + ' m³')

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
  inboundItems.value.push({ rowKey: rowKeySeq++, skuCode: undefined, expectedQuantity: 1 })
  markDirty()
}

const removeItem = (index: number) => {
  inboundItems.value.splice(index, 1)
  markDirty()
}

const handleSkuSelected = (record: ItemRow, row: SkuRow | SkuRow[] | null) => {
  const sku = Array.isArray(row) ? row[0] : row
  record.skuCode = sku?.skuCode
  record.outerLengthMm = sku?.outerLengthMm
  record.outerWidthMm = sku?.outerWidthMm
  record.outerHeightMm = sku?.outerHeightMm
  markDirty()
}

const downloadTemplate = async () => {
  templateLoading.value = true
  try {
    await downloadManualInboundTemplate({
      inboundNo: formModel.inboundNo,
      inboundDate: formModel.inboundDate,
      warehouseId: formModel.warehouseId
    })
    message.success('入库模板已下载')
  } catch (error) {
    console.error('下载入库模板失败:', error)
    message.error('模板下载失败')
  } finally {
    templateLoading.value = false
  }
}

const beforeImportTemplate = async (file: File) => {
  if (!file.name.toLowerCase().endsWith('.xlsx')) {
    message.error('仅支持导入 .xlsx 文件')
    return false
  }
  templateLoading.value = true
  try {
    const rows = await parseManualInboundTemplateFile(file)
    const errors: string[] = []
    if (rows.length === 0) errors.push('模板中没有可导入的明细行')

    const first = rows[0]
    const inboundNo = String(first?.inboundNo || '').trim()
    const inboundDate = String(first?.inboundDate || '').trim()
    const warehouseId = Number(first?.warehouseId)
    const codes = new Set<string>()
    if (isUpdateForm.value && inboundNo !== formModel.inboundNo.trim()) {
      errors.push('编辑已有入库单时，模板中的入库单号必须与当前单据一致')
    }

    rows.forEach((row, index) => {
      const excelRow = index + 2
      const code = String(row.skuCode || '').trim()
      const rowWarehouseId = Number(row.warehouseId)
      if (!String(row.inboundNo || '').trim()) errors.push('第 ' + excelRow + ' 行：入库单号不能为空')
      if (!String(row.inboundDate || '').trim()) errors.push('第 ' + excelRow + ' 行：入库日期不能为空')
      if (
        String(row.inboundDate || '').trim() &&
        !/^\d{4}-\d{2}-\d{2}$/.test(String(row.inboundDate || '').trim())
      ) {
        errors.push('第 ' + excelRow + ' 行：入库日期格式必须为 YYYY-MM-DD')
      }
      if (!Number.isInteger(rowWarehouseId) || rowWarehouseId <= 0) {
        errors.push('第 ' + excelRow + ' 行：入库仓库ID必须是正整数')
      }
      if (String(row.inboundNo || '').trim() !== inboundNo) {
        errors.push('第 ' + excelRow + ' 行：同一模板内入库单号必须一致')
      }
      if (String(row.inboundDate || '').trim() !== inboundDate) {
        errors.push('第 ' + excelRow + ' 行：同一模板内入库日期必须一致')
      }
      if (rowWarehouseId !== warehouseId) {
        errors.push('第 ' + excelRow + ' 行：同一模板内入库仓库ID必须一致')
      }
      if (!code) errors.push('第 ' + excelRow + ' 行：SKU编码不能为空')
      if (!Number.isInteger(row.expectedQuantity) || row.expectedQuantity <= 0) {
        errors.push('第 ' + excelRow + ' 行：应到数量必须是大于0的整数')
      }
      const normalizedCode = code.toUpperCase()
      if (normalizedCode && codes.has(normalizedCode)) {
        errors.push('第 ' + excelRow + ' 行：SKU重复，请合并数量')
      }
      if (normalizedCode) codes.add(normalizedCode)
    })

    if (errors.length === 0) {
      const skuResult = await listSkuByCodes([...codes])
      const skuMap = new Map(
        (isSuccess(skuResult) ? skuResult.data || [] : []).map(sku => [
          sku.skuCode.trim().toUpperCase(),
          sku
        ])
      )
      rows.forEach((row, index) => {
        if (!skuMap.has(row.skuCode.trim().toUpperCase())) {
          errors.push('第 ' + (index + 2) + ' 行：SKU不存在或当前货主无权使用')
        }
      })
      if (errors.length === 0) {
        formModel.inboundNo = inboundNo
        formModel.inboundDate = inboundDate
        formModel.warehouseId = warehouseId
        inboundDateValue.value = inboundDate
        inboundItems.value = rows.map(row => {
          const sku = skuMap.get(row.skuCode.trim().toUpperCase())!
          return {
            rowKey: rowKeySeq++,
            skuCode: sku.skuCode,
            expectedQuantity: row.expectedQuantity,
            outerLengthMm: sku.outerLengthMm,
            outerWidthMm: sku.outerWidthMm,
            outerHeightMm: sku.outerHeightMm,
            remark: row.remark || undefined
          }
        })
        markDirty()
        message.success('已导入 ' + rows.length + ' 条入库明细，请核对后保存')
      }
    }

    if (errors.length > 0) {
      Modal.error({
        title: '入库模板校验未通过',
        width: 680,
        content: errors.slice(0, 30).join('；')
      })
    }
  } catch (error: any) {
    message.error(error?.message || '模板读取失败，请重新下载模板后填写')
  } finally {
    templateLoading.value = false
  }
  return false
}

const validateForm = async (): Promise<boolean> => {
  try {
    await formRef.value?.validate()
  } catch {
    return false
  }

  const validItems = inboundItems.value.filter(i => i.skuCode && i.expectedQuantity > 0)
  if (validItems.length === 0) {
    message.error('请至少添加一条有效明细（选择SKU并填写应到数量）')
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

const buildSubmitData = (): ManualInboundDTO => {
  const items: ManualInboundItemDTO[] = inboundItems.value
    .filter(i => i.skuCode && i.expectedQuantity > 0)
    .map(i => ({
      skuCode: i.skuCode!,
      expectedQuantity: i.expectedQuantity,
      remark: i.remark
    }))
  return {
    ...formModel,
    items
  }
}

// 静默保存（新建则创建，编辑则更新），返回入库单ID
const submitFormSilent = async (): Promise<number | null> => {
  const valid = await validateForm()
  if (!valid) return null

  const data = buildSubmitData()
  submitLoading.value = true
  try {
    if (isUpdateForm.value) {
      const result = await updateManualInbound(data)
      if (isSuccess(result)) {
        resetDirty()
        return formModel.id!
      }
      message.error(result.message || '保存失败')
      return null
    } else {
      const result = await createManualInbound(data)
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
  await router.push('/wms/manual-inbound')
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
    emitter.emit('refresh-manual-inbound-list')
    goBackToList()
  }
}

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
      const id = await submitFormSilent()
      if (!id) return
      doRequest(submitManualInbound(id), {
        successMessage: '提交成功',
        onSuccess: () => {
          resetDirty()
          emitter.emit('refresh-manual-inbound-list')
          goBackToList()
        }
      })
    }
  })
}

const loadInboundDetail = (id: number) => {
  loading.value = true
  doRequest(getManualInboundDetail(id), {
    onSuccess: res => {
      const detail = res.data!
      if (detail.orderStatus !== InboundStatus.DRAFT) {
        message.warning('只有草稿状态的入库单可以编辑')
        goBackToList()
        return
      }
      orderStatus.value = detail.orderStatus
      formModel.id = detail.id
      formModel.inboundNo = detail.inboundNo
      formModel.warehouseId = detail.warehouseId
      formModel.inboundDate = detail.inboundDate
      formModel.remark = detail.remark
      inboundDateValue.value = detail.inboundDate
      inboundItems.value = (detail.items || []).map(item => ({
        rowKey: rowKeySeq++,
        skuCode: item.skuCode,
        expectedQuantity: item.expectedQuantity,
        outerLengthMm: item.outerLengthMm,
        outerWidthMm: item.outerWidthMm,
        outerHeightMm: item.outerHeightMm,
        remark: item.remark
      }))
    },
    onFinally: () => {
      loading.value = false
    }
  })
}

const initPage = () => {
  if (formMode.value === 'edit' && inboundOrderId.value) {
    loadInboundDetail(inboundOrderId.value)
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
.manual-inbound-form-page {
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

.detail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 12px;
}

.detail-tip {
  color: #8c8c8c;
  font-size: 12px;
}

.dimension-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
}

.missing-dimension {
  color: #fa8c16;
}

.del-link {
  color: #f5222d;
}

:deep(.ant-form-item-label > label) {
  font-size: 13px;
  color: #595959;
}
</style>
