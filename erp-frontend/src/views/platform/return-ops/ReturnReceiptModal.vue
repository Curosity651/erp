<template>
  <a-modal
    :open="open"
    :title="t('platform.return.receipt.title')"
    :width="1180"
    :confirm-loading="submitting"
    :ok-text="t('platform.return.receipt.confirm')"
    :cancel-text="t('platform.common.cancel')"
    @ok="submit"
    @cancel="close"
  >
    <a-form layout="inline" class="receipt-head">
      <a-form-item :label="t('platform.return.receipt.warehouse')" required>
        <WarehouseSelect v-model:value="form.warehouseId" warehouse-type="OWN" width="240px" />
      </a-form-item>
      <a-form-item :label="t('platform.return.receipt.date')">
        <a-date-picker v-model:value="form.returnDate" value-format="YYYY-MM-DD" />
      </a-form-item>
      <a-form-item :label="t('platform.return.receipt.remark')">
        <a-input v-model:value="form.remark" :maxlength="500" style="width: 300px" allow-clear />
      </a-form-item>
    </a-form>

    <div class="table-actions">
      <strong>{{ t('platform.return.receipt.items') }}</strong>
      <a-space>
        <a-button :loading="downloading" @click="downloadTemplate">
          <download-outlined />{{ t('platform.return.receipt.downloadTemplate') }}
        </a-button>
        <a-upload
          accept=".xlsx"
          :show-upload-list="false"
          :before-upload="importTemplate"
          :disabled="importing"
        >
          <a-button :loading="importing">
            <file-excel-outlined />{{ t('platform.return.receipt.importExcel') }}
          </a-button>
        </a-upload>
        <a-button type="primary" ghost @click="addLine">
          <plus-outlined />{{ t('platform.return.receipt.addItem') }}
        </a-button>
      </a-space>
    </div>
    <a-table
      :data-source="lines"
      :pagination="false"
      row-key="key"
      size="small"
      :scroll="{ x: 1050 }"
    >
      <a-table-column :title="t('platform.return.receipt.globalSku')" :width="330">
        <template #default="{ record }">
          <a-input
            v-model:value="record.warehouseSkuCode"
            :status="record.resolveError ? 'error' : undefined"
            allow-clear
            @change="onSkuChanged(record)"
          />
          <div v-if="record.resolving" class="sku-status muted">
            {{ t('platform.return.receipt.resolving') }}
          </div>
          <div v-else-if="record.matched" class="sku-status matched">
            {{ record.skuName || '-' }} · {{ record.ownerName || '-' }} ·
            {{ record.originalSkuCode || '-' }}
          </div>
          <div v-else-if="record.resolveError" class="sku-status error">
            {{ record.resolveError }}
          </div>
        </template>
      </a-table-column>
      <a-table-column :title="t('platform.return.receipt.quantity')" :width="110">
        <template #default="{ record }"
          ><a-input-number v-model:value="record.receivedQty" :min="1" style="width: 90px"
        /></template>
      </a-table-column>
      <a-table-column :title="t('platform.return.receipt.platformOrderNo')" :width="180">
        <template #default="{ record }"
          ><a-input v-model:value="record.platformOrderId" allow-clear
        /></template>
      </a-table-column>
      <a-table-column :title="t('platform.return.receipt.reason')" :width="150">
        <template #default="{ record }"
          ><a-select
            v-model:value="record.returnReason"
            :options="reasonOptions"
            style="width: 130px"
        /></template>
      </a-table-column>
      <a-table-column :title="t('platform.return.receipt.evidence')" :width="170">
        <template #default="{ record }">
          <a-upload
            :custom-request="(options: any) => upload(options, record)"
            :show-upload-list="false"
            accept="image/jpeg,image/png"
            :disabled="record.photoFileIds.length >= 6"
          >
            <a-button size="small" :loading="record.uploading"
              ><upload-outlined />{{
                t('platform.return.receipt.photos', { count: record.photoFileIds.length })
              }}</a-button
            >
          </a-upload>
          <a-button
            v-if="record.photoFileIds.length"
            type="link"
            size="small"
            @click="record.photoFileIds = []"
            >{{ t('platform.return.receipt.clear') }}</a-button
          >
        </template>
      </a-table-column>
      <a-table-column :title="t('platform.common.operation')" :width="70" fixed="right">
        <template #default="{ record }"
          ><a-button type="text" danger @click="removeLine(record.key)"
            ><delete-outlined /></a-button
        ></template>
      </a-table-column>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, h, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  DeleteOutlined,
  DownloadOutlined,
  FileExcelOutlined,
  PlusOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import { registerReturnReceipt, resolveReturnWarehouseSkus } from '@/api/wms/return-qc'
import { isSuccess } from '@/api'
import { useFileUpload } from '@/hooks/use-file-upload'
import {
  applyResolvedReturnSkus,
  buildReturnReceiptPayload,
  type ReturnReceiptDraftLine
} from './return-receipt-flow'
import {
  downloadReturnReceiptTemplate,
  parseReturnReceiptTemplateFile,
  validateReturnReceiptRows
} from './return-receipt-excel'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ (e: 'update:open', value: boolean): void; (e: 'success'): void }>()
const { t } = useI18n()
const { uploadFile } = useFileUpload()
const submitting = ref(false)
const importing = ref(false)
const downloading = ref(false)
let sequence = 0
const resolveTimers = new Map<number, ReturnType<typeof setTimeout>>()
const form = reactive<{ warehouseId?: number; returnDate: string; remark?: string }>({
  warehouseId: undefined,
  returnDate: dayjs().format('YYYY-MM-DD')
})
const lines = ref<ReturnReceiptDraftLine[]>([])
const reasonOptions = computed(() => [
  { label: t('platform.return.reason.notWanted'), value: 'NOT_WANTED' },
  { label: t('platform.return.reason.damaged'), value: 'DAMAGED' },
  { label: t('platform.return.reason.wrongItem'), value: 'WRONG_ITEM' },
  { label: t('platform.return.reason.qualityIssue'), value: 'QUALITY_ISSUE' },
  { label: t('platform.return.reason.other'), value: 'OTHER' }
])

function addLine() {
  lines.value.push({
    key: ++sequence,
    warehouseSkuCode: '',
    receivedQty: 1,
    returnReason: 'OTHER',
    photoFileIds: [],
    uploading: false
  })
}
function removeLine(key: number) {
  if (lines.value.length === 1) return message.warning(t('platform.return.receipt.keepOne'))
  lines.value = lines.value.filter(line => line.key !== key)
}
function reset() {
  resolveTimers.forEach(timer => clearTimeout(timer))
  resolveTimers.clear()
  form.warehouseId = undefined
  form.returnDate = dayjs().format('YYYY-MM-DD')
  form.remark = undefined
  lines.value = []
  addLine()
}
watch(
  () => props.open,
  value => value && reset()
)
function close() {
  emit('update:open', false)
}

async function upload(options: any, record: ReturnReceiptDraftLine) {
  record.uploading = true
  try {
    const result = await uploadFile(options.file, {
      bucketKey: 'private-files',
      folder: `return-receipt/${Date.now()}`,
      allowedTypes: ['image/jpeg', 'image/jpg', 'image/png'],
      maxSize: 10 * 1024 * 1024
    })
    if (result) {
      record.photoFileIds.push(result.fileId)
      options.onSuccess?.(result)
    } else options.onError?.(new Error(t('platform.picking.simple.uploadFailed')))
  } finally {
    record.uploading = false
  }
}

function onSkuChanged(record: ReturnReceiptDraftLine) {
  const previous = resolveTimers.get(record.key)
  if (previous) clearTimeout(previous)
  record.matched = false
  record.ownerName = undefined
  record.originalSkuCode = undefined
  record.skuName = undefined
  record.resolveError = undefined
  record.resolving = false
  if (!record.warehouseSkuCode.trim()) return
  resolveTimers.set(
    record.key,
    setTimeout(() => resolveLine(record), 350)
  )
}

async function resolveLine(record: ReturnReceiptDraftLine) {
  const requestedCode = record.warehouseSkuCode.trim()
  if (!requestedCode) return
  record.resolving = true
  record.resolveError = undefined
  try {
    const res = await resolveReturnWarehouseSkus([requestedCode])
    if (record.warehouseSkuCode.trim() !== requestedCode) return
    if (!isSuccess(res)) {
      record.matched = false
      record.resolveError = res.message || t('platform.return.receipt.unmatched')
      return
    }
    const applied = applyResolvedReturnSkus([record], res.data || [])
    if (applied.errors.length) {
      record.matched = false
      record.resolveError = applied.errors[0].replace(/^第 1 行：/, '')
      return
    }
    Object.assign(record, applied.lines[0])
  } catch (error: any) {
    if (record.warehouseSkuCode.trim() === requestedCode) {
      record.matched = false
      record.resolveError = error?.message || t('platform.return.receipt.unmatched')
    }
  } finally {
    if (record.warehouseSkuCode.trim() === requestedCode) record.resolving = false
  }
}

async function downloadTemplate() {
  downloading.value = true
  try {
    await downloadReturnReceiptTemplate()
  } finally {
    downloading.value = false
  }
}

async function importTemplate(file: File) {
  importing.value = true
  try {
    const parsed = await parseReturnReceiptTemplateFile(file)
    const validation = validateReturnReceiptRows(parsed)
    if (validation.errors.length) throw new Error(validation.errors.join('\n'))
    if (!validation.lines.length) throw new Error(t('platform.return.receipt.importEmpty'))
    const res = await resolveReturnWarehouseSkus(
      validation.lines.map(line => line.warehouseSkuCode)
    )
    if (!isSuccess(res)) throw new Error(res.message || t('platform.return.receipt.importFailed'))
    const applied = applyResolvedReturnSkus(validation.lines, res.data || [])
    if (applied.errors.length) throw new Error(applied.errors.join('\n'))
    lines.value = applied.lines.map(line => ({ ...line, key: ++sequence }))
    message.success(t('platform.return.receipt.importSuccess', { count: lines.value.length }))
  } catch (error: any) {
    const details = String(error?.message || t('platform.return.receipt.importFailed')).split('\n')
    Modal.error({
      title: t('platform.return.receipt.importFailed'),
      content: h(
        'div',
        details.map(detail => h('div', detail))
      )
    })
  } finally {
    importing.value = false
  }
  return false
}

async function submit() {
  if (!form.warehouseId) return message.warning(t('platform.return.receipt.selectWarehouse'))
  if (lines.value.some(line => !line.matched || line.resolving || line.receivedQty <= 0)) {
    return message.warning(t('platform.return.receipt.completeItems'))
  }
  submitting.value = true
  try {
    const res = await registerReturnReceipt(buildReturnReceiptPayload(form, lines.value))
    if (!isSuccess(res)) return message.error(res.message || t('platform.return.receipt.failed'))
    message.success(t('platform.return.receipt.success', { count: res.data?.length || 0 }))
    emit('success')
    close()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.receipt-head {
  margin-bottom: 16px;
  row-gap: 8px;
}
.table-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 12px 0;
}
.sku-status {
  margin-top: 4px;
  font-size: 12px;
  line-height: 18px;
}
.sku-status.muted {
  color: #8c8c8c;
}
.sku-status.matched {
  color: #389e0d;
}
.sku-status.error {
  color: #ff4d4f;
}
</style>
