<template>
  <a-card :bordered="false">
    <a-tabs v-model:active-key="activeTab">
      <a-tab-pane key="handover" tab="出库交接">
        <a-form :model="searchModel" layout="inline" class="shipping-search">
          <a-form-item label="货主">
            <platform-owner-select
              v-model:value="searchModel.erpTenantId"
              placeholder="全部"
              width="150px"
            />
          </a-form-item>
          <a-form-item label="仓库">
            <warehouse-select
              v-model:value="searchModel.warehouseId"
              placeholder="全部"
              width="160px"
            />
          </a-form-item>
          <a-form-item label="交接状态">
            <a-select
              v-model:value="searchModel.fulfillmentStatus"
              :options="statusOptions"
              placeholder="全部"
              allow-clear
              style="width: 130px"
            />
          </a-form-item>
          <a-form-item label="创建日期">
            <a-range-picker
              v-model:value="dateRange"
              value-format="YYYY-MM-DD"
              allow-clear
              style="width: 230px"
            />
          </a-form-item>
          <a-form-item>
            <search-actions
              :loading="tableRef?.loading"
              @search="searchTable"
              @reset="resetSearch"
            />
          </a-form-item>
        </a-form>

        <pro-table
          ref="tableRef"
          header-title="出库交接单"
          row-key="id"
          :request="tableRequest"
          :columns="columns"
          :scroll="{ x: 2180 }"
          size="middle"
        >
          <template #toolBarRender>
            <a-button type="primary" @click="openCreate">新建出库交接单</a-button>
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'source'">
              <div>{{ record.sourceOrderNo }}</div>
              <span class="secondary-text">{{ record.sourceType }}</span>
            </template>
            <template v-else-if="column.key === 'transport'">
              <div>{{ record.carrierName || '-' }} / {{ record.shippingMethod || '-' }}</div>
              <span class="secondary-text">{{ record.trackingNo || '-' }}</span>
            </template>
            <template v-else-if="column.key === 'vehicle'">
              <div>{{ record.vehiclePlate || '-' }}</div>
              <span class="secondary-text">{{ record.driverName || '-' }}</span>
            </template>
            <template v-else-if="column.key === 'freight'">
              <span>{{ record.freightCost }} {{ record.currency || 'CNY' }}</span>
              <div class="record-only">仅记录</div>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-badge
                :status="record.handoverStatus === 'HANDED_OVER' ? 'success' : 'processing'"
                :text="record.handoverStatus === 'HANDED_OVER' ? '已交接' : '待交接'"
              />
            </template>
            <template v-else-if="column.key === 'operate'">
              <a-space>
                <a @click="openEdit(record)">{{ record.handoverStatus === 'HANDED_OVER' ? '查看/编辑' : '编辑' }}</a>
                <a
                  v-if="record.handoverStatus === 'READY_HANDOVER'"
                  @click="confirmHandover(record)"
                  >确认交接</a
                >
                <a v-else @click="downloadPdf(record)">PDF</a>
              </a-space>
            </template>
          </template>
        </pro-table>
      </a-tab-pane>

      <a-tab-pane key="fuel" tab="油费记录">
        <expense-pane type="FUEL" :active="activeTab === 'fuel'" />
      </a-tab-pane>
      <a-tab-pane key="driver" tab="司机月结">
        <expense-pane type="DRIVER_MONTHLY" :active="activeTab === 'driver'" />
      </a-tab-pane>
    </a-tabs>
  </a-card>

  <a-modal
    v-model:open="editorOpen"
    :title="editorMode === 'create' ? '新建出库交接单' : '出库交接单资料'"
    width="780px"
    :confirm-loading="saving"
    :ok-text="editorMode === 'create' ? '创建交接单' : '保存修改'"
    @ok="submitEditor"
  >
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item v-if="editorMode === 'create'" label="关联已打包货物" name="fulfillmentOrderId">
        <a-select
          v-model:value="form.fulfillmentOrderId"
          show-search
          :filter-option="filterFulfillment"
          :options="availableOptions"
          placeholder="请选择已经拣货并完成打包的履约单"
          @change="fillFromFulfillment"
        />
      </a-form-item>

      <a-descriptions v-if="linkedOrder" size="small" :column="2" bordered class="order-summary">
        <a-descriptions-item label="履约单号">{{ linkedOrder.fulfillmentNo }}</a-descriptions-item>
        <a-descriptions-item label="平台订单">{{ linkedOrder.sourceOrderNo }}</a-descriptions-item>
        <a-descriptions-item label="货主">{{ linkedOrder.ownerName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ linkedOrder.warehouseName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="承运信息">
          {{ linkedOrder.carrierName || '-' }} / {{ linkedOrder.shippingMethod || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="跟踪号">{{ linkedOrder.trackingNo || '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="车牌" name="vehiclePlate">
            <a-input v-model:value="form.vehiclePlate" maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="司机" name="driverName">
            <a-input v-model:value="form.driverName" maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="司机电话" name="driverPhone">
            <a-input v-model:value="form.driverPhone" maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="发车时间" name="departureTime">
            <a-date-picker
              v-model:value="form.departureTime"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="目的地" name="destination">
            <a-auto-complete
              v-model:value="form.destination"
              :options="destinationOptions"
              placeholder="请输入或选择最近使用的目的地"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="运费（仅记录）" name="freightCost">
            <a-input-number
              v-model:value="form.freightCost"
              :min="0"
              :precision="2"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="币种">
            <a-select v-model:value="form.currency" :options="currencyOptions" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注">
            <a-textarea v-model:value="form.remark" :rows="2" maxlength="500" show-count />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="物流照片（最多6张）">
            <div class="photo-list">
              <sys-file-upload
                v-for="(_, index) in photoIds"
                :key="index"
                v-model="photoIds[index]"
                button-text="上传照片"
                :allowed-types="['image/jpeg', 'image/jpg', 'image/png']"
                :max-size="10 * 1024 * 1024"
              />
              <a-button
                v-if="photoIds.length < 6"
                type="dashed"
                @click="photoIds.push(undefined)"
                >添加照片</a-button
              >
            </div>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <a-alert
      type="warning"
      show-icon
      message="本单运费只作为运输记录保存，不会进入原有仓储计费或物流产品计费。"
    />
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import dayjs from 'dayjs'
import type { FormInstance } from 'ant-design-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { SearchActions } from '@/components/Search'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import SysFileUpload from '@/components/Upload/SysFileUpload.vue'
import ExpensePane from './TransportExpensePane.vue'
import { mergePageParam } from '@/utils/page-utils'
import { isSuccess } from '@/api'
import {
  confirmOutboundHandover,
  createOutboundHandover,
  downloadOutboundHandoverPdf,
  getOutboundHandover,
  listAvailableHandoverFulfillments,
  listRecentHandoverDestinations,
  pageOutboundHandoverOrders,
  saveOutboundHandover
} from '@/api/wms/fulfillment'
import type {
  FulfillmentHandoverForm,
  FulfillmentShippingOrder,
  FulfillmentShippingQuery,
  OutboundHandoverOrder
} from '@/api/wms/fulfillment/types'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'

defineOptions({ name: 'FulfillmentWorkbenchPage' })

interface EditorForm extends FulfillmentHandoverForm {
  fulfillmentOrderId?: number
}

const activeTab = ref('handover')
const tableRef = ref<ProTableInstanceExpose>()
const dateRange = ref<[string, string]>()
const searchModel = reactive<FulfillmentShippingQuery>({})
let searchParams: FulfillmentShippingQuery = {}
const statusOptions = [
  { label: '待交接', value: 'READY_HANDOVER' },
  { label: '已交接', value: 'HANDED_OVER' }
]
const currencyOptions = ['CNY', 'USD', 'EUR', 'RUB'].map(value => ({ label: value, value }))

const tableRequest: TableRequest = (params, sorter, filter) =>
  pageOutboundHandoverOrders({
    ...mergePageParam(params, sorter, filter),
    ...searchParams
  })
const reloadTable = (resetPageIndex?: boolean) => tableRef.value?.actionRef?.reload(resetPageIndex)
useTableActivateReload(() => reloadTable(false))

const searchTable = () => {
  searchParams = {
    ...searchModel,
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1]
  }
  reloadTable(true)
}
const resetSearch = () => {
  Object.assign(searchModel, {
    erpTenantId: undefined,
    warehouseId: undefined,
    fulfillmentStatus: undefined,
    shippedBy: undefined
  })
  dateRange.value = undefined
  searchTable()
}

const editorOpen = ref(false)
const editorMode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const formRef = ref<FormInstance>()
const currentHandover = ref<OutboundHandoverOrder>()
const availableFulfillments = ref<FulfillmentShippingOrder[]>([])
const recentDestinations = ref<string[]>([])
const photoIds = ref<(number | undefined)[]>([undefined])
const form = reactive<EditorForm>({
  vehiclePlate: '',
  driverName: '',
  driverPhone: '',
  departureTime: '',
  destination: '',
  freightCost: 0,
  currency: 'CNY',
  remark: ''
})
const rules = {
  fulfillmentOrderId: [{ required: true, message: '请选择已打包货物' }],
  vehiclePlate: [{ required: true, message: '请输入车牌' }],
  driverName: [{ required: true, message: '请输入司机' }],
  departureTime: [{ required: true, message: '请选择发车时间' }],
  destination: [{ required: true, message: '请输入目的地' }],
  freightCost: [{ required: true, message: '请输入运费' }]
}

const availableOptions = computed(() =>
  availableFulfillments.value.map(item => ({
    value: item.id,
    label: `${item.fulfillmentNo}｜${item.sourceOrderNo}｜${item.ownerName || '-'}｜${item.warehouseName || '-'}`
  }))
)
const destinationOptions = computed(() =>
  recentDestinations.value.map(value => ({ label: value, value }))
)
const linkedOrder = computed<Partial<FulfillmentShippingOrder> | undefined>(() => {
  if (editorMode.value === 'edit' && currentHandover.value) return currentHandover.value
  return availableFulfillments.value.find(item => item.id === form.fulfillmentOrderId)
})

const resetEditor = () => {
  Object.assign(form, {
    fulfillmentOrderId: undefined,
    vehiclePlate: '',
    driverName: '',
    driverPhone: '',
    departureTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    destination: '',
    freightCost: 0,
    currency: 'CNY',
    remark: ''
  })
  photoIds.value = [undefined]
  currentHandover.value = undefined
  formRef.value?.clearValidate()
}

const loadCreationOptions = async () => {
  const [fulfillments, destinations] = await Promise.all([
    listAvailableHandoverFulfillments(),
    listRecentHandoverDestinations()
  ])
  availableFulfillments.value = isSuccess(fulfillments) ? fulfillments.data || [] : []
  recentDestinations.value = isSuccess(destinations) ? destinations.data || [] : []
}

const openCreate = async () => {
  editorMode.value = 'create'
  resetEditor()
  await loadCreationOptions()
  editorOpen.value = true
}

const openEdit = async (record: OutboundHandoverOrder) => {
  const res = await getOutboundHandover(record.id)
  if (!isSuccess(res)) return
  editorMode.value = 'edit'
  currentHandover.value = res.data
  Object.assign(form, {
    fulfillmentOrderId: res.data.fulfillmentOrderId,
    vehiclePlate: res.data.vehiclePlate || '',
    driverName: res.data.driverName || '',
    driverPhone: res.data.driverPhone || '',
    departureTime: res.data.departureTime,
    destination: res.data.destination || '',
    freightCost: res.data.freightCost ?? 0,
    currency: res.data.currency || 'CNY',
    remark: res.data.remark || ''
  })
  const ids = (res.data.logisticsPhotoFileIds || '')
    .split(',')
    .map(Number)
    .filter(Boolean)
  photoIds.value = ids.length ? ids : [undefined]
  editorOpen.value = true
}

const fillFromFulfillment = (id: number) => {
  const fulfillment = availableFulfillments.value.find(item => item.id === id)
  if (fulfillment?.recipientAddress) form.destination = fulfillment.recipientAddress
}

const filterFulfillment = (input: string, option: { label?: string }) =>
  String(option.label || '')
    .toLowerCase()
    .includes(input.toLowerCase())

const handoverPayload = (): FulfillmentHandoverForm => ({
  vehiclePlate: form.vehiclePlate,
  driverName: form.driverName,
  driverPhone: form.driverPhone,
  departureTime: form.departureTime,
  destination: form.destination,
  freightCost: form.freightCost,
  currency: form.currency,
  remark: form.remark,
  photoFileIds: photoIds.value.filter((id): id is number => typeof id === 'number')
})

const submitEditor = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = handoverPayload()
    const res =
      editorMode.value === 'create'
        ? await createOutboundHandover({
            ...payload,
            fulfillmentOrderId: form.fulfillmentOrderId as number
          })
        : await saveOutboundHandover(currentHandover.value!.id, payload)
    if (isSuccess(res)) {
      message.success(editorMode.value === 'create' ? '出库交接单已创建' : '交接资料已更新')
      editorOpen.value = false
      reloadTable(editorMode.value === 'create')
    }
  } finally {
    saving.value = false
  }
}

const confirmHandover = (record: OutboundHandoverOrder) => {
  Modal.confirm({
    title: `确认交接 ${record.handoverNo}？`,
    content: '确认后，关联货物的最终状态将变为“已交接”。',
    async onOk() {
      const res = await confirmOutboundHandover(record.id, {
        vehiclePlate: record.vehiclePlate,
        driverName: record.driverName,
        driverPhone: record.driverPhone,
        departureTime: record.departureTime,
        destination: record.destination,
        freightCost: record.freightCost,
        currency: record.currency,
        remark: record.remark,
        photoFileIds: (record.logisticsPhotoFileIds || '')
          .split(',')
          .map(Number)
          .filter(Boolean)
      })
      if (isSuccess(res)) {
        message.success('货物已交接')
        reloadTable(false)
      }
    }
  })
}

const downloadPdf = async (record: OutboundHandoverOrder) => {
  const response = await downloadOutboundHandoverPdf(record.id)
  const blob =
    response.data instanceof Blob
      ? response.data
      : new Blob([response.data], { type: 'application/pdf' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `出库交接凭证-${record.handoverNo}.pdf`
  link.click()
  URL.revokeObjectURL(link.href)
}

const columns: ProColumns[] = [
  { title: '交接单号', dataIndex: 'handoverNo', key: 'handoverNo', width: 220, fixed: 'left' },
  { title: '履约单号', dataIndex: 'fulfillmentNo', key: 'fulfillmentNo', width: 190 },
  { title: '平台订单', key: 'source', width: 190 },
  { title: '货主', dataIndex: 'ownerName', key: 'ownerName', width: 130, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 140, ellipsis: true },
  { title: '承运信息/跟踪号', key: 'transport', width: 240 },
  { title: '车牌/司机', key: 'vehicle', width: 150 },
  { title: '发车时间', dataIndex: 'departureTime', key: 'departureTime', width: 170 },
  { title: '目的地', dataIndex: 'destination', key: 'destination', width: 220, ellipsis: true },
  { title: '运费', key: 'freight', width: 120 },
  { title: '状态', key: 'status', width: 100, align: 'center' },
  { title: '交接人员', dataIndex: 'handoverByName', key: 'handoverByName', width: 110 },
  { title: '交接时间', dataIndex: 'handoverTime', key: 'handoverTime', width: 170 },
  { title: '操作', key: 'operate', width: 180, align: 'center', fixed: 'right' }
]
</script>

<style scoped>
.shipping-search {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 20px;
  margin-bottom: 16px;
}
.shipping-search :deep(.ant-form-item) {
  margin: 0;
}
.secondary-text,
.record-only {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
.record-only {
  color: #d48806;
}
.order-summary {
  margin-bottom: 20px;
}
.photo-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.photo-list :deep(.sys-file-upload) {
  width: 100%;
}
.photo-list :deep(.uploaded-file) {
  max-width: none;
}
:deep(.expense-toolbar) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
:deep(.record-only-tip) {
  margin-left: 10px;
  color: #d48806;
  font-size: 12px;
}
</style>
