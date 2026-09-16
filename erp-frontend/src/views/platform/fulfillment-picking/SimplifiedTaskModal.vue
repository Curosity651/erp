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
        :scroll="{ x: 760, y: 360 }"
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

      <div class="modal-actions">
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
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { completeSimplifiedFulfillmentTask, getFulfillmentPickTask } from '@/api/wms/fulfillment'
import type {
  FulfillmentPickTask,
  FulfillmentPickTaskDetail
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
const { t } = useI18n()

const loading = ref(false)
const submitting = ref(false)
const detail = ref<FulfillmentPickTaskDetail>()
const owners = ref<TenantBrief[]>([])
const evidenceFiles = ref<EvidenceFile[]>([])
const { uploading, uploadFile } = useFileUpload()

const columns = computed(() => [
  { title: t('platform.picking.simple.platformOrder'), key: 'orderNo', width: 230, fixed: 'left' as const },
  { title: t('platform.common.owner'), key: 'owner', width: 140 },
  { title: t('platform.picking.simple.logisticsProduct'), dataIndex: ['fulfillmentOrder', 'logisticsProductName'], width: 180 },
  { title: t('platform.picking.simple.firstLocation'), dataIndex: 'firstLocationCode', width: 130 },
  { title: t('platform.picking.simple.goods'), key: 'goods', width: 110 }
])

const ownerName = (id: number) =>
  owners.value.find(item => item.id === id)?.tenantName || t('platform.picking.simple.ownerFallback', { id })
const canComplete = computed(() => canCompleteSimplifiedTask(
  evidenceFiles.value.length,
  (detail.value?.orderQueue || []).map(item => ({
    orderStatus: item.taskOrder.orderStatus
  }))
))

watch(() => [props.open, props.task?.id] as const, ([open, id]) => {
  if (open && id) void load(id)
}, { immediate: true })

async function load(taskId: number) {
  loading.value = true
  evidenceFiles.value = []
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
.evidence-section { display: grid; grid-template-columns: 240px minmax(0, 1fr); gap: 20px; margin-top: 20px; padding-top: 18px; border-top: 1px solid #f0f0f0; }
.section-title { margin-bottom: 4px; font-weight: 600; }
.evidence-list { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }
.evidence-item { display: grid; justify-items: center; gap: 4px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 22px; }
</style>
