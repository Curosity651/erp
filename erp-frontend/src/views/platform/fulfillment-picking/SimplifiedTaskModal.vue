<template>
  <a-modal
    :open="open"
    :title="`整单作业${task ? ` · ${task.taskNo}` : ''}`"
    :width="1120"
    :mask-closable="false"
    :footer="null"
    @cancel="close"
  >
    <a-spin :spinning="loading">
      <a-alert
        type="info"
        show-icon
        message="先打印拣货单和全部平台面单，按拣货单顺序取货并随手贴单；全部完成后上传现场凭证并确认。"
        class="workflow-alert"
      />

      <a-table
        row-key="fulfillmentOrder.id"
        :data-source="detail?.orderQueue || []"
        :columns="columns"
        :pagination="false"
        :scroll="{ x: 960, y: 360 }"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'orderNo'">
            <div>{{ record.fulfillmentOrder.sourceOrderNo }}</div>
            <div class="secondary">{{ record.fulfillmentOrder.sourceType }}</div>
          </template>
          <template v-else-if="column.key === 'owner'">
            {{ ownerName(record.fulfillmentOrder.erpTenantId) }}
          </template>
          <template v-else-if="column.key === 'goods'">
            {{ record.skuCount }} 种 · {{ record.totalQuantity }} 件
          </template>
          <template v-else-if="column.key === 'label'">
            <a-tag v-if="record.taskOrder.orderStatus === 'CANCELLED'">已取消</a-tag>
            <a-tag v-else-if="labelReady(record)" color="green">已生成</a-tag>
            <a-tag v-else-if="labelFailures[record.fulfillmentOrder.id]" color="red">生成失败</a-tag>
            <a-tag v-else>待生成</a-tag>
            <div v-if="labelFailures[record.fulfillmentOrder.id]" class="failure-text">
              {{ labelFailures[record.fulfillmentOrder.id] }}
            </div>
          </template>
          <template v-else-if="column.key === 'operate'">
            <a
              v-if="labelUrl(record)"
              :href="labelUrl(record)"
              target="_blank"
              rel="noopener noreferrer"
            >打开面单</a>
            <span v-else>-</span>
          </template>
        </template>
      </a-table>

      <div class="evidence-section">
        <div>
          <div class="section-title">作业凭证</div>
          <div class="secondary">全部货物完成取货、贴面单和打包后，上传至少一张现场照片。</div>
        </div>
        <div class="evidence-list">
          <div v-for="file in evidenceFiles" :key="file.fileId" class="evidence-item">
            <a-image :src="file.url" :width="72" :height="72" />
            <a-button type="text" danger size="small" @click="removeEvidence(file.fileId)">移除</a-button>
          </div>
          <a-upload
            :custom-request="uploadEvidence"
            :show-upload-list="false"
            accept="image/jpeg,image/png"
            :disabled="evidenceFiles.length >= 6"
          >
            <a-button :loading="uploading">上传照片</a-button>
          </a-upload>
        </div>
      </div>

      <div class="modal-actions">
        <a-button @click="printPickingList">打印拣货单</a-button>
        <a-button :loading="printingLabels" @click="printAllLabels">打印所有面单</a-button>
        <a-button
          type="primary"
          :loading="submitting"
          :disabled="!canComplete"
          @click="completeTask"
        >确认完成</a-button>
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import {
  completeSimplifiedFulfillmentTask,
  getFulfillmentPickTask,
  printFulfillmentLabel
} from '@/api/wms/fulfillment'
import type {
  FulfillmentPickTask,
  FulfillmentPickTaskDetail,
  FulfillmentPickTaskOrderDetail
} from '@/api/wms/fulfillment/types'
import { listAllErpTenants } from '@/api/tenant'
import type { TenantBrief } from '@/api/tenant/types'
import { getFileDownloadUrl } from '@/api/system/file'
import { useFileUpload } from '@/hooks/use-file-upload'
import { buildPickingTaskPrintHtml } from './picking-task-print'
import { canCompleteSimplifiedTask } from './simplified-task-flow'

interface EvidenceFile { fileId: number; url: string }

const props = defineProps<{
  open: boolean
  task?: FulfillmentPickTask
  warehouseName: string
}>()
const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const loading = ref(false)
const printingLabels = ref(false)
const submitting = ref(false)
const detail = ref<FulfillmentPickTaskDetail>()
const owners = ref<TenantBrief[]>([])
const evidenceFiles = ref<EvidenceFile[]>([])
const generatedLabelUrls = reactive<Record<number, string>>({})
const labelFailures = reactive<Record<number, string>>({})
const { uploading, uploadFile } = useFileUpload()

const columns = [
  { title: '平台订单', key: 'orderNo', width: 230, fixed: 'left' as const },
  { title: '货主', key: 'owner', width: 140 },
  { title: '物流产品', dataIndex: ['fulfillmentOrder', 'logisticsProductName'], width: 180 },
  { title: '首个库位', dataIndex: 'firstLocationCode', width: 130 },
  { title: '商品', key: 'goods', width: 110 },
  { title: '面单', key: 'label', width: 180 },
  { title: '操作', key: 'operate', width: 90, fixed: 'right' as const }
]

const ownerName = (id: number) => owners.value.find(item => item.id === id)?.tenantName || `货主 #${id}`
const labelUrl = (record: FulfillmentPickTaskOrderDetail) =>
  generatedLabelUrls[record.fulfillmentOrder.id] || record.fulfillmentOrder.labelFileUrl
const labelReady = (record: FulfillmentPickTaskOrderDetail) => Boolean(
  labelUrl(record) && record.fulfillmentOrder.labelBarcode
)
const canComplete = computed(() => canCompleteSimplifiedTask(
  evidenceFiles.value.length,
  (detail.value?.orderQueue || []).map(item => ({
    orderStatus: item.taskOrder.orderStatus,
    labelReady: labelReady(item)
  }))
))

watch(() => [props.open, props.task?.id] as const, ([open, id]) => {
  if (open && id) void load(id)
}, { immediate: true })

async function load(taskId: number) {
  loading.value = true
  evidenceFiles.value = []
  Object.keys(generatedLabelUrls).forEach(key => delete generatedLabelUrls[Number(key)])
  Object.keys(labelFailures).forEach(key => delete labelFailures[Number(key)])
  try {
    const [detailResult, ownerResult] = await Promise.all([
      getFulfillmentPickTask(taskId),
      listAllErpTenants()
    ])
    if (isSuccess(detailResult)) detail.value = detailResult.data
    if (isSuccess(ownerResult)) owners.value = ownerResult.data || []
  } finally {
    loading.value = false
  }
}

function pickingPrintData() {
  const orders = (detail.value?.orderQueue || []).map(item => ({
    orderNo: item.fulfillmentOrder.sourceOrderNo,
    ownerName: ownerName(item.fulfillmentOrder.erpTenantId),
    sourceType: item.fulfillmentOrder.sourceType,
    lines: item.routeLines.map(line => ({
      locationCode: line.locationCode,
      warehouseSkuCode: line.warehouseSkuCode,
      skuCode: line.skuCode,
      plannedQuantity: line.plannedQuantity
    }))
  }))
  return {
    taskNo: props.task?.taskNo || '-',
    warehouseName: props.warehouseName,
    printedAt: new Date().toLocaleString('zh-CN', { hour12: false }),
    orderCount: orders.length,
    totalQuantity: orders.reduce(
      (sum, order) => sum + order.lines.reduce((subtotal, line) => subtotal + line.plannedQuantity, 0),
      0
    ),
    orders
  }
}

function printPickingList() {
  const page = window.open('', '_blank', 'width=980,height=760')
  if (!page) return message.error('浏览器阻止了打印窗口')
  page.document.write(buildPickingTaskPrintHtml(pickingPrintData()))
  page.document.close()
}

async function printAllLabels() {
  const activeOrders = (detail.value?.orderQueue || []).filter(
    item => item.taskOrder.orderStatus !== 'CANCELLED'
  )
  if (!activeOrders.length) return message.warning('任务没有可打印面单的订单')
  const center = window.open('', '_blank', 'width=1100,height=780')
  if (!center) return message.error('浏览器阻止了面单打印窗口')
  center.document.write('<!doctype html><meta charset="UTF-8"><title>面单打印中心</title><p>正在生成全部面单，请稍候...</p>')
  printingLabels.value = true
  let successCount = 0
  try {
    for (const item of activeOrders) {
      const id = item.fulfillmentOrder.id
      delete labelFailures[id]
      try {
        const result = await printFulfillmentLabel(id)
        if (!isSuccess(result) || !result.data.labelUrl) {
          labelFailures[id] = result.message || '平台未返回面单文件'
          continue
        }
        generatedLabelUrls[id] = result.data.labelUrl
        item.fulfillmentOrder.labelFileUrl = result.data.labelUrl
        item.fulfillmentOrder.labelBarcode = result.data.labelBarcode
        successCount++
      } catch (error: any) {
        labelFailures[id] = error?.message || '面单生成失败'
      }
    }
    const rows = activeOrders.map(item => {
      const id = item.fulfillmentOrder.id
      const url = labelUrl(item)
      const text = url
        ? `<a href="${url}" target="_blank">打开并打印面单</a>`
        : `<span style="color:#cf1322">${labelFailures[id] || '生成失败'}</span>`
      return `<tr><td>${item.fulfillmentOrder.sourceOrderNo}</td><td>${item.fulfillmentOrder.sourceType}</td><td>${text}</td></tr>`
    }).join('')
    center.document.open()
    center.document.write(`<!doctype html><html><head><meta charset="UTF-8"><title>面单打印中心</title>
      <style>body{font-family:Arial,Microsoft YaHei;padding:24px}table{width:100%;border-collapse:collapse}th,td{border:1px solid #ddd;padding:10px;text-align:left}</style>
      </head><body><h2>面单打印中心</h2><p>成功 ${successCount} / ${activeOrders.length}，请按订单顺序打开并打印。</p>
      <table><thead><tr><th>平台订单</th><th>平台</th><th>面单文件</th></tr></thead><tbody>${rows}</tbody></table></body></html>`)
    center.document.close()
    if (successCount === activeOrders.length) message.success('全部面单已生成')
    else message.warning(`有 ${activeOrders.length - successCount} 张面单生成失败，请处理后重试`)
  } finally {
    printingLabels.value = false
  }
}

async function uploadEvidence(options: any) {
  const result = await uploadFile(options.file, {
    bucketKey: 'private-files',
    folder: `fulfillment-picking/simple/${props.task?.id}`,
    allowedTypes: ['image/jpeg', 'image/png'],
    maxSize: 10 * 1024 * 1024
  })
  if (!result) return options.onError?.(new Error('上传失败'))
  const urlResult = await getFileDownloadUrl(result.fileId)
  evidenceFiles.value.push({
    fileId: result.fileId,
    url: isSuccess(urlResult) ? urlResult.data : ''
  })
  options.onSuccess?.(result)
}

function removeEvidence(fileId: number) {
  evidenceFiles.value = evidenceFiles.value.filter(file => file.fileId !== fileId)
}

async function completeTask() {
  if (!props.task || !canComplete.value) return message.warning('请先生成全部面单并上传作业凭证')
  submitting.value = true
  try {
    const result = await completeSimplifiedFulfillmentTask(
      props.task.id,
      evidenceFiles.value.map(file => file.fileId)
    )
    if (!isSuccess(result)) return
    message.success('整单作业已完成，订单已进入待签出')
    emit('update:open', false)
    emit('success')
  } finally {
    submitting.value = false
  }
}

function close() {
  emit('update:open', false)
}
</script>

<style scoped>
.workflow-alert { margin-bottom: 16px; }
.secondary { color: rgba(0, 0, 0, .45); font-size: 12px; }
.failure-text { margin-top: 4px; color: #cf1322; font-size: 12px; }
.evidence-section { display: grid; grid-template-columns: 240px minmax(0, 1fr); gap: 20px; margin-top: 20px; padding-top: 18px; border-top: 1px solid #f0f0f0; }
.section-title { margin-bottom: 4px; font-weight: 600; }
.evidence-list { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }
.evidence-item { display: grid; justify-items: center; gap: 4px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 22px; }
</style>
