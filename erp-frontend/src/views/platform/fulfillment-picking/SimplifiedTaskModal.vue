<template>
  <a-modal
    :open="open"
    :title="`${t('platform.picking.simple.title')}${task ? ` · ${task.taskNo}` : ''}`"
    :width="1120"
    :mask-closable="false"
    :footer="null"
    @cancel="close"
  >
    <a-spin :spinning="loading">
      <a-alert
        type="info"
        show-icon
        :message="t('platform.picking.simple.instructions')"
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
            {{ t('platform.return.skuKindsAndPieces', { kinds: record.skuCount, pieces: record.totalQuantity }) }}
          </template>
          <template v-else-if="column.key === 'label'">
            <a-tag v-if="record.taskOrder.orderStatus === 'CANCELLED'">{{ t('platform.picking.status.cancelled') }}</a-tag>
            <a-tag v-else-if="labelReady(record)" color="green">{{ t('platform.picking.simple.generated') }}</a-tag>
            <a-tag v-else-if="labelFailures[record.fulfillmentOrder.id]" color="red">{{ t('platform.picking.simple.generateFailed') }}</a-tag>
            <a-tag v-else>{{ t('platform.picking.simple.pendingGeneration') }}</a-tag>
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
            >{{ t('platform.picking.simple.openLabel') }}</a>
            <span v-else>-</span>
          </template>
        </template>
      </a-table>

      <div class="evidence-section">
        <div>
          <div class="section-title">{{ t('platform.picking.simple.evidence') }}</div>
          <div class="secondary">{{ t('platform.picking.simple.evidenceHint') }}</div>
        </div>
        <div class="evidence-list">
          <div v-for="file in evidenceFiles" :key="file.fileId" class="evidence-item">
            <a-image :src="file.url" :width="72" :height="72" />
            <a-button type="text" danger size="small" @click="removeEvidence(file.fileId)">{{ t('platform.picking.simple.remove') }}</a-button>
          </div>
          <a-upload
            :custom-request="uploadEvidence"
            :show-upload-list="false"
            accept="image/jpeg,image/png"
            :disabled="evidenceFiles.length >= 6"
          >
            <a-button :loading="uploading">{{ t('platform.picking.simple.uploadPhoto') }}</a-button>
          </a-upload>
        </div>
      </div>

      <div v-if="generatedPackage" class="package-result">
        <div>
          <div class="section-title">拣货文件包已生成</div>
          <div class="secondary">
            {{ generatedPackage.orderCount }} 单 / {{ generatedPackage.totalQuantity }} 件 · 批次 {{ generatedPackage.batchNo }}
          </div>
        </div>
        <div class="package-links">
          <a-button type="link" :href="generatedPackage.warehouseDownloadUrl" target="_blank">
            下载俄文仓库包
          </a-button>
          <a-button type="link" :href="generatedPackage.archiveDownloadUrl" target="_blank">
            下载中文留底包
          </a-button>
        </div>
      </div>

      <div class="modal-actions">
        <a-button :loading="generatingPackage" @click="printPickingList">
          {{ t('platform.picking.simple.printPickList') }}
        </a-button>
        <a-button :loading="printingLabels" @click="printAllLabels">{{ t('platform.picking.simple.printAllLabels') }}</a-button>
        <a-button
          type="primary"
          :loading="submitting"
          :disabled="!canComplete"
          @click="completeTask"
        >{{ t('platform.picking.simple.complete') }}</a-button>
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import {
  completeSimplifiedFulfillmentTask,
  generateFulfillmentPickPackage,
  getFulfillmentPickTask,
  printFulfillmentLabel
} from '@/api/wms/fulfillment'
import type {
  FulfillmentPickTask,
  FulfillmentPickPackage,
  FulfillmentPickTaskDetail,
  FulfillmentPickTaskOrderDetail
} from '@/api/wms/fulfillment/types'
import { listAllErpTenants } from '@/api/tenant'
import type { TenantBrief } from '@/api/tenant/types'
import { getFileDownloadUrl } from '@/api/system/file'
import { useFileUpload } from '@/hooks/use-file-upload'
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
const { t, locale } = useI18n()

const loading = ref(false)
const printingLabels = ref(false)
const generatingPackage = ref(false)
const submitting = ref(false)
const detail = ref<FulfillmentPickTaskDetail>()
const generatedPackage = ref<FulfillmentPickPackage>()
const owners = ref<TenantBrief[]>([])
const evidenceFiles = ref<EvidenceFile[]>([])
const generatedLabelUrls = reactive<Record<number, string>>({})
const labelFailures = reactive<Record<number, string>>({})
const { uploading, uploadFile } = useFileUpload()

const columns = computed(() => [
  { title: t('platform.picking.simple.platformOrder'), key: 'orderNo', width: 230, fixed: 'left' as const },
  { title: t('platform.common.owner'), key: 'owner', width: 140 },
  { title: t('platform.picking.simple.logisticsProduct'), dataIndex: ['fulfillmentOrder', 'logisticsProductName'], width: 180 },
  { title: t('platform.picking.simple.firstLocation'), dataIndex: 'firstLocationCode', width: 130 },
  { title: t('platform.picking.simple.goods'), key: 'goods', width: 110 },
  { title: t('platform.picking.simple.label'), key: 'label', width: 180 },
  { title: t('platform.common.operation'), key: 'operate', width: 90, fixed: 'right' as const }
])

const ownerName = (id: number) =>
  owners.value.find(item => item.id === id)?.tenantName || t('platform.picking.simple.ownerFallback', { id })
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
  generatedPackage.value = undefined
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

async function printPickingList() {
  if (!props.task) return
  generatingPackage.value = true
  try {
    const result = await generateFulfillmentPickPackage(props.task.id)
    if (!isSuccess(result)) return
    generatedPackage.value = result.data
    downloadPackage(result.data.warehouseDownloadUrl, result.data.warehouseFileName)
    window.setTimeout(() => {
      downloadPackage(result.data.archiveDownloadUrl, result.data.archiveFileName)
    }, 350)
    message.success('俄文仓库包和中文留底包已生成')
    await load(props.task.id)
    generatedPackage.value = result.data
  } finally {
    generatingPackage.value = false
  }
}

function downloadPackage(url: string, fileName: string) {
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  anchor.target = '_blank'
  anchor.rel = 'noopener noreferrer'
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
}

async function printAllLabels() {
  const activeOrders = (detail.value?.orderQueue || []).filter(
    item => item.taskOrder.orderStatus !== 'CANCELLED'
  )
  if (!activeOrders.length) return message.warning(t('platform.picking.simple.noLabels'))
  const center = window.open('', '_blank', 'width=1100,height=780')
  if (!center) return message.error(t('platform.picking.simple.labelPopupBlocked'))
  center.document.write(`<!doctype html><meta charset="UTF-8"><title>${t('platform.picking.simple.labelCenter')}</title><p>${t('platform.picking.simple.generating')}</p>`)
  printingLabels.value = true
  let successCount = 0
  try {
    for (const item of activeOrders) {
      const id = item.fulfillmentOrder.id
      delete labelFailures[id]
      try {
        const result = await printFulfillmentLabel(id)
        if (!isSuccess(result) || !result.data.labelUrl) {
          labelFailures[id] = result.message || t('platform.picking.simple.platformNoFile')
          continue
        }
        generatedLabelUrls[id] = result.data.labelUrl
        item.fulfillmentOrder.labelFileUrl = result.data.labelUrl
        item.fulfillmentOrder.labelBarcode = result.data.labelBarcode
        successCount++
      } catch (error: any) {
        labelFailures[id] = error?.message || t('platform.picking.simple.generateFailed')
      }
    }
    const rows = activeOrders.map(item => {
      const id = item.fulfillmentOrder.id
      const url = labelUrl(item)
      const text = url
        ? `<a href="${url}" target="_blank">${t('platform.picking.simple.openAndPrint')}</a>`
        : `<span style="color:#cf1322">${labelFailures[id] || t('platform.picking.simple.generateFailed')}</span>`
      return `<tr><td>${item.fulfillmentOrder.sourceOrderNo}</td><td>${item.fulfillmentOrder.sourceType}</td><td>${text}</td></tr>`
    }).join('')
    center.document.open()
    center.document.write(`<!doctype html><html lang="${locale.value}"><head><meta charset="UTF-8"><title>${t('platform.picking.simple.labelCenter')}</title>
      <style>body{font-family:Arial,Microsoft YaHei;padding:24px}table{width:100%;border-collapse:collapse}th,td{border:1px solid #ddd;padding:10px;text-align:left}</style>
      </head><body><h2>${t('platform.picking.simple.labelCenter')}</h2><p>${t('platform.picking.simple.generatedSummary', { success: successCount, total: activeOrders.length })}</p>
      <table><thead><tr><th>${t('platform.picking.simple.platformOrder')}</th><th>${t('dashboard.platform')}</th><th>${t('platform.picking.simple.labelFile')}</th></tr></thead><tbody>${rows}</tbody></table></body></html>`)
    center.document.close()
    if (successCount === activeOrders.length) message.success(t('platform.picking.simple.allGenerated'))
    else message.warning(t('platform.picking.simple.failedCount', { count: activeOrders.length - successCount }))
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
  if (!result) return options.onError?.(new Error(t('platform.picking.simple.uploadFailed')))
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
  if (!props.task || !canComplete.value) return message.warning(t('platform.picking.simple.completePrerequisites'))
  submitting.value = true
  try {
    const result = await completeSimplifiedFulfillmentTask(
      props.task.id,
      evidenceFiles.value.map(file => file.fileId)
    )
    if (!isSuccess(result)) return
    message.success(t('platform.picking.simple.completed'))
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
.package-result { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-top: 18px; padding: 12px 16px; background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 6px; }
.package-links { display: flex; flex-wrap: wrap; justify-content: flex-end; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 22px; }
</style>
