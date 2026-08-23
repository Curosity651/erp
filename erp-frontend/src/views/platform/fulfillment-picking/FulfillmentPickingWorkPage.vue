<template>
  <div class="work-page">
    <a-card :bordered="false">
      <div class="task-head">
        <a-space>
          <a-button @click="router.back()">返回</a-button>
          <div>
            <h3>{{ detail?.task.taskNo || '拣货任务' }}</h3>
            <span class="muted">
              {{ warehouseName }} · {{ completedCount }}/{{ detail?.task.orderCount || 0 }} 单完成
            </span>
          </div>
        </a-space>
        <a-tag :color="canOperate ? 'blue' : 'default'">
          {{ canOperate ? '当前由你操作' : '只读查看' }}
        </a-tag>
      </div>
    </a-card>

    <div v-if="detail" class="work-layout">
      <a-card title="订单队列" :bordered="false" class="queue-card">
        <div class="queue-filter">
          <a-segmented v-model:value="queueFilter" :options="queueFilters" block />
        </div>
        <div class="queue-list">
          <button
            v-for="item in filteredQueue"
            :key="item.taskOrder.id"
            type="button"
            class="queue-item"
            :class="{ active: selectedOrderId === item.fulfillmentOrder.id }"
            @click="selectOrder(item.fulfillmentOrder.id)"
          >
            <div class="queue-title">
              <strong>{{ item.fulfillmentOrder.sourceOrderNo }}</strong>
              <a-tag :color="orderStatusColor[item.taskOrder.orderStatus]">
                {{ orderStatusText[item.taskOrder.orderStatus] }}
              </a-tag>
            </div>
            <div class="queue-meta">
              <span>{{ item.fulfillmentOrder.sourceType }}</span>
              <span>{{ item.firstLocationCode || '-' }}</span>
              <span>{{ item.skuCount }} SKU · {{ item.totalQuantity }} 件</span>
            </div>
          </button>
        </div>
      </a-card>

      <a-card :bordered="false" class="order-card">
        <a-empty v-if="!current" description="请选择一张订单" />
        <template v-else>
          <div class="order-head">
            <div>
              <h3>{{ current.fulfillmentOrder.sourceOrderNo }}</h3>
              <span class="muted">
                {{ current.fulfillmentOrder.sourceType }} ·
                货主 #{{ current.fulfillmentOrder.erpTenantId }} ·
                {{ current.fulfillmentOrder.logisticsProductName || '未设置物流产品' }}
              </span>
            </div>
            <a-tag :color="orderStatusColor[current.taskOrder.orderStatus]">
              {{ orderStatusText[current.taskOrder.orderStatus] }}
            </a-tag>
          </div>

          <a-steps :current="stepIndex" size="small" class="steps">
            <a-step title="扫描取货" />
            <a-step title="打印核验面单" />
            <a-step title="完成打包" />
          </a-steps>

          <a-alert
            v-if="!canOperate"
            type="warning"
            show-icon
            message="该任务由其他拣货员领取，当前页面只允许查看。"
            class="workflow-alert"
          />
          <a-alert
            v-else-if="isPicking"
            type="info"
            show-icon
            message="请扫描货物所在库位，再扫描箱上的内部 SKU 条形码。"
            class="workflow-alert"
          />
          <a-alert
            v-else-if="isWaitingLabel"
            type="success"
            show-icon
            message="商品已取齐，请立即打印并粘贴当前订单的平台面单。"
            class="workflow-alert"
          />

          <a-table
            row-key="id"
            :data-source="current.routeLines"
            :columns="lineColumns"
            :pagination="false"
            size="middle"
            class="line-table"
          />

          <a-form v-if="isPicking" layout="vertical" class="action-form" @finish="submitScan">
            <a-row :gutter="16">
              <a-col :span="9">
                <a-form-item label="扫描库位码" required>
                  <a-input v-model:value="scan.locationCode" :disabled="!canOperate" autofocus />
                </a-form-item>
              </a-col>
              <a-col :span="9">
                <a-form-item label="扫描内部 SKU" required>
                  <a-input v-model:value="scan.warehouseSkuCode" :disabled="!canOperate" />
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="本次数量" required>
                  <a-input-number
                    v-model:value="scan.quantity"
                    :min="1"
                    :disabled="!canOperate"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <div class="form-actions">
              <a-button danger :disabled="!canOperate" @click="exceptionOpen = true">
                标记异常
              </a-button>
              <a-button type="primary" html-type="submit" :disabled="!canOperate">
                确认取货
              </a-button>
            </div>
          </a-form>

          <div v-else-if="isWaitingLabel" class="label-pack">
            <a-space class="label-row">
              <a-button type="primary" :disabled="!canOperate" @click="printLabel">
                打印平台面单
              </a-button>
              <a-tag :color="current.fulfillmentOrder.labelVerifiedTime ? 'green' : 'blue'">
                {{ current.fulfillmentOrder.labelVerifiedTime ? '面单已核验' : '面单待核验' }}
              </a-tag>
            </a-space>
            <a-form layout="vertical">
              <a-form-item label="扫描已粘贴的面单条码" required>
                <a-input v-model:value="pack.barcode" :disabled="!canOperate" />
              </a-form-item>
              <a-button
                v-if="!current.fulfillmentOrder.labelVerifiedTime"
                :disabled="!canOperate || !pack.barcode"
                @click="verifyLabel"
              >
                核验面单
              </a-button>
              <template v-else>
                <a-row :gutter="16">
                  <a-col :span="8"><a-form-item label="承运商" required><a-input v-model:value="pack.carrierName" :disabled="!canOperate" /></a-form-item></a-col>
                  <a-col :span="8"><a-form-item label="运输方式" required><a-input v-model:value="pack.shippingMethod" :disabled="!canOperate" /></a-form-item></a-col>
                  <a-col :span="8"><a-form-item label="跟踪号" required><a-input v-model:value="pack.trackingNo" :disabled="!canOperate" /></a-form-item></a-col>
                </a-row>
                <a-row :gutter="16">
                  <a-col :span="8">
                    <a-form-item label="包裹重量（kg）" required>
                      <a-input-number v-model:value="pack.packageWeightKg" :min="0.01" :disabled="!canOperate" style="width: 100%" />
                    </a-form-item>
                  </a-col>
                </a-row>
                <div class="form-actions">
                  <a-button danger :disabled="!canOperate" @click="exceptionOpen = true">标记异常</a-button>
                  <a-button type="primary" :disabled="!canOperate" @click="completePack">完成打包</a-button>
                </div>
              </template>
            </a-form>
          </div>

          <a-result
            v-else-if="current.taskOrder.orderStatus === 'COMPLETED'"
            status="success"
            title="当前订单已经完成打包"
            sub-title="包裹已进入出库作业，等待签出。"
          />
          <a-result
            v-else-if="current.taskOrder.orderStatus === 'EXCEPTION'"
            status="warning"
            title="当前订单处于异常状态"
            :sub-title="current.taskOrder.exceptionReason"
          >
            <template #extra>
              <a-button type="primary" :disabled="!canOperate" @click="restoreException">恢复拣货</a-button>
              <a-popconfirm
                title="确认取消该订单？已取货商品必须先退回原库位，库存才会释放。"
                @confirm="cancelException"
              >
                <a-button danger :disabled="!canOperate">取消订单</a-button>
              </a-popconfirm>
            </template>
          </a-result>
        </template>
      </a-card>
    </div>

    <a-modal v-model:open="exceptionOpen" title="标记订单异常" @ok="submitException">
      <a-form layout="vertical">
        <a-form-item label="异常类型" required>
          <a-select v-model:value="exceptionForm.exceptionType" :options="exceptionTypes" />
        </a-form-item>
        <a-form-item label="异常说明" required>
          <a-textarea v-model:value="exceptionForm.reason" :rows="3" />
        </a-form-item>
        <a-form-item label="现场照片（选填）">
          <a-upload :custom-request="uploadEvidence" :show-upload-list="false" accept="image/*">
            <a-button :loading="uploading">上传照片</a-button>
          </a-upload>
          <div v-if="exceptionForm.imageUrls.length" class="file-list">
            已上传 {{ exceptionForm.imageUrls.length }} 张
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { isSuccess } from '@/api'
import {
  cancelFulfillmentPickException,
  getFulfillmentPickTask,
  markFulfillmentPickException,
  packFulfillment,
  printFulfillmentLabel,
  restoreFulfillmentPickException,
  scanFulfillmentPickLine,
  verifyFulfillmentLabel
} from '@/api/wms/fulfillment'
import type {
  FulfillmentPickTaskDetail,
  FulfillmentPickTaskOrderDetail
} from '@/api/wms/fulfillment/types'
import { getWarehouseOptions } from '@/api/wms/warehouse'
import { useUserStore } from '@/stores/user-store'
import { useFileUpload } from '@/hooks/use-file-upload'
import type { PickingOrderStatus } from './picking-task-flow'

defineOptions({ name: 'FulfillmentPickingWorkPage' })
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const taskId = Number(route.params.taskId)
const detail = ref<FulfillmentPickTaskDetail>()
const selectedOrderId = ref<number>()
const queueFilter = ref('ALL')
const warehouseName = ref('')
const exceptionOpen = ref(false)
const uploading = ref(false)
const { uploadFile } = useFileUpload()
const scan = reactive({ locationCode: '', warehouseSkuCode: '', quantity: 1 })
const pack = reactive({
  barcode: '',
  carrierName: '',
  shippingMethod: '',
  trackingNo: '',
  packageWeightKg: 1
})
const exceptionForm = reactive({
  exceptionType: undefined as string | undefined,
  reason: '',
  imageUrls: [] as string[]
})

const orderStatusText: Record<string, string> = {
  PENDING: '待处理',
  PICKING: '处理中',
  WAITING_LABEL: '待贴面单',
  COMPLETED: '已完成',
  EXCEPTION: '异常',
  CANCELLED: '已取消'
}
const orderStatusColor: Record<string, string> = {
  PENDING: 'default',
  PICKING: 'blue',
  WAITING_LABEL: 'cyan',
  COMPLETED: 'green',
  EXCEPTION: 'red',
  CANCELLED: 'default'
}
const queueFilters = [
  { label: '全部', value: 'ALL' },
  { label: '待处理', value: 'PENDING' },
  { label: '处理中', value: 'PICKING' },
  { label: '待贴面单', value: 'WAITING_LABEL' },
  { label: '异常', value: 'EXCEPTION' }
]
const exceptionTypes = [
  { label: '缺货', value: 'SHORTAGE' },
  { label: '货损', value: 'DAMAGED' },
  { label: '条码不符', value: 'BARCODE_MISMATCH' },
  { label: '面单失败', value: 'LABEL_FAILURE' },
  { label: '其他', value: 'OTHER' }
]
const lineColumns = [
  { title: '取货库位', dataIndex: 'locationCode', width: 150 },
  { title: '内部 SKU', dataIndex: 'warehouseSkuCode' },
  { title: '商品 SKU', dataIndex: 'skuCode' },
  {
    title: '应取/已取',
    width: 120,
    customRender: ({ record }: any) => `${record.plannedQuantity} / ${record.pickedQuantity}`
  }
]

const current = computed(() => detail.value?.currentOrder)
const completedCount = computed(
  () => detail.value?.orderQueue.filter(item => item.taskOrder.orderStatus === 'COMPLETED').length || 0
)
const canOperate = computed(
  () =>
    detail.value?.task.operatorId === userStore.userInfo?.userId &&
    ['PICKING', 'PARTIAL_EXCEPTION'].includes(detail.value.task.taskStatus)
)
const isPicking = computed(
  () => current.value && ['PENDING', 'PICKING'].includes(current.value.taskOrder.orderStatus)
)
const isWaitingLabel = computed(() => current.value?.taskOrder.orderStatus === 'WAITING_LABEL')
const stepIndex = computed(() => {
  if (isPicking.value) return 0
  if (isWaitingLabel.value && !current.value?.fulfillmentOrder.labelVerifiedTime) return 1
  return 2
})
const filteredQueue = computed(() =>
  (detail.value?.orderQueue || []).filter(
    item => queueFilter.value === 'ALL' || item.taskOrder.orderStatus === queueFilter.value
  )
)

const load = async (orderId?: number) => {
  const result = await getFulfillmentPickTask(taskId, orderId)
  if (!isSuccess(result)) return
  detail.value = result.data
  const active = result.data.currentOrder?.fulfillmentOrder
  selectedOrderId.value = active?.id
  if (active) {
    Object.assign(pack, {
      barcode: '',
      carrierName: active.carrierName || '',
      shippingMethod: active.shippingMethod || '',
      trackingNo: active.trackingNo || '',
      packageWeightKg: active.packageWeightKg || 1
    })
  }
}
const selectOrder = async (id: number) => {
  const item = detail.value?.orderQueue.find(row => row.fulfillmentOrder.id === id)
  if (!item) return
  selectedOrderId.value = id
  await load(id)
}
const submitScan = async () => {
  if (!current.value || !scan.locationCode || !scan.warehouseSkuCode) {
    return message.warning('请扫描库位码和内部 SKU')
  }
  const result = await scanFulfillmentPickLine({
    taskId,
    fulfillmentNo: current.value.fulfillmentOrder.fulfillmentNo,
    ...scan
  })
  if (isSuccess(result)) {
    message.success('取货已记录')
    scan.locationCode = ''
    scan.warehouseSkuCode = ''
    scan.quantity = 1
    await load(selectedOrderId.value)
  }
}
const printLabel = async () => {
  if (!current.value) return
  const result = await printFulfillmentLabel(current.value.fulfillmentOrder.id)
  if (isSuccess(result)) {
    if (result.data.labelUrl) window.open(result.data.labelUrl, '_blank')
    message.success('面单已生成，请打印并贴到当前包裹')
    await load(selectedOrderId.value)
  }
}
const verifyLabel = async () => {
  if (!current.value || !pack.barcode) return
  const result = await verifyFulfillmentLabel(current.value.fulfillmentOrder.id, pack.barcode)
  if (isSuccess(result)) {
    message.success('面单核验成功')
    await load(selectedOrderId.value)
  }
}
const completePack = async () => {
  if (!current.value || !pack.carrierName || !pack.shippingMethod || !pack.trackingNo) {
    return message.warning('请填写完整承运信息')
  }
  const result = await packFulfillment(current.value.fulfillmentOrder.id, pack)
  if (isSuccess(result)) {
    message.success('当前订单打包完成')
    await load()
  }
}
const submitException = async () => {
  if (!current.value || !exceptionForm.exceptionType || !exceptionForm.reason.trim()) {
    return message.warning('请选择异常类型并填写说明')
  }
  const result = await markFulfillmentPickException(taskId, current.value.fulfillmentOrder.id, {
    exceptionType: exceptionForm.exceptionType,
    reason: exceptionForm.reason,
    imageUrls: exceptionForm.imageUrls
  })
  if (isSuccess(result)) {
    exceptionOpen.value = false
    message.success('订单已标记异常，可以继续处理其他订单')
    await load()
  }
}
const restoreException = async () => {
  if (!current.value) return
  const result = await restoreFulfillmentPickException(taskId, current.value.fulfillmentOrder.id)
  if (isSuccess(result)) {
    message.success('异常订单已恢复')
    await load(selectedOrderId.value)
  }
}
const cancelException = async () => {
  if (!current.value) return
  const result = await cancelFulfillmentPickException(taskId, current.value.fulfillmentOrder.id)
  if (isSuccess(result)) {
    const hasPicked = current.value.routeLines.some(line => line.pickedQuantity > 0)
    message.success(hasPicked ? '订单已取消，请按原拣货记录完成退回' : '订单已取消，库存预占已释放')
    await load()
  }
}
const uploadEvidence = async (options: any) => {
  uploading.value = true
  try {
    const result = await uploadFile(options.file, {
      bucketKey: 'private-files',
      folder: `fulfillment-picking/${taskId}`
    })
    if (!result?.fileId) throw new Error('上传失败')
    exceptionForm.imageUrls.push(String(result.fileId))
    options.onSuccess?.(result)
  } catch (error) {
    options.onError?.(error)
    message.error('照片上传失败')
  } finally {
    uploading.value = false
  }
}

onMounted(async () => {
  if (!Number.isFinite(taskId)) return message.error('拣货任务参数不正确')
  const warehouseResult = await getWarehouseOptions()
  await load()
  const warehouse = warehouseResult.data?.find(item => item.id === detail.value?.task.warehouseId)
  warehouseName.value = warehouse?.warehouseName || `仓库 #${detail.value?.task.warehouseId || '-'}`
})
</script>

<style scoped>
.work-page { display: grid; gap: 16px; min-width: 0; }
.task-head, .order-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.task-head h3, .order-head h3 { margin: 0 0 4px; }
.muted { color: rgba(0, 0, 0, .45); font-size: 12px; }
.work-layout { display: grid; grid-template-columns: 320px minmax(0, 1fr); gap: 16px; min-width: 0; }
.queue-card, .order-card { min-width: 0; }
.queue-filter { margin-bottom: 12px; }
.queue-list { display: grid; gap: 8px; max-height: calc(100vh - 300px); overflow-y: auto; }
.queue-item { width: 100%; border: 1px solid #e8e8e8; border-radius: 5px; background: #fff; padding: 12px; text-align: left; cursor: pointer; }
.queue-item.active { border-color: #1677ff; background: #e6f4ff; }
.queue-title { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.queue-meta { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 8px; color: rgba(0, 0, 0, .55); font-size: 12px; }
.steps { margin: 22px 0; }
.workflow-alert { margin-bottom: 16px; }
.line-table { margin-bottom: 18px; }
.action-form, .label-pack { border-top: 1px solid #f0f0f0; padding-top: 18px; }
.form-actions { display: flex; justify-content: space-between; }
.label-row { margin-bottom: 16px; }
.file-list { margin-top: 8px; color: rgba(0, 0, 0, .55); }
@media (max-width: 1100px) {
  .work-layout { grid-template-columns: 270px minmax(0, 1fr); }
}
</style>
