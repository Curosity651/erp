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
    <a-alert
      type="info"
      show-icon
      :message="t('platform.return.receipt.description')"
      style="margin-bottom: 16px"
    />
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
      <a-button type="primary" ghost @click="addLine"
        ><plus-outlined />{{ t('platform.return.receipt.addItem') }}</a-button
      >
    </div>
    <a-table
      :data-source="lines"
      :pagination="false"
      row-key="key"
      size="small"
      :scroll="{ x: 1080 }"
    >
      <a-table-column :title="t('platform.common.owner')" :width="170">
        <template #default="{ record }">
          <PlatformOwnerSelect v-model:value="record.erpTenantId" width="150px" />
        </template>
      </a-table-column>
      <a-table-column :title="t('platform.return.receipt.internalSku')" :width="180">
        <template #default="{ record }"><a-input v-model:value="record.skuCode" /></template>
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
              ><upload-outlined />{{ t('platform.return.receipt.photos', { count: record.photoFileIds.length }) }}</a-button
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
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import WarehouseSelect from '@/components/Lov/WarehouseSelect.vue'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import { registerReturnReceipt } from '@/api/wms/return-qc'
import { isSuccess } from '@/api'
import { useFileUpload } from '@/hooks/use-file-upload'

interface ReceiptLine {
  key: number
  erpTenantId?: number
  skuCode: string
  receivedQty: number
  platformOrderId?: string
  returnReason: string
  photoFileIds: number[]
  uploading: boolean
}

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ (e: 'update:open', value: boolean): void; (e: 'success'): void }>()
const { t } = useI18n()
const { uploadFile } = useFileUpload()
const submitting = ref(false)
let sequence = 0
const form = reactive<{ warehouseId?: number; returnDate: string; remark?: string }>({
  warehouseId: undefined,
  returnDate: dayjs().format('YYYY-MM-DD')
})
const lines = ref<ReceiptLine[]>([])
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
    skuCode: '',
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

async function upload(options: any, record: ReceiptLine) {
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

async function submit() {
  if (!form.warehouseId) return message.warning(t('platform.return.receipt.selectWarehouse'))
  if (
    lines.value.some(line => !line.erpTenantId || !line.skuCode.trim() || line.receivedQty <= 0)
  ) {
    return message.warning(t('platform.return.receipt.completeItems'))
  }
  submitting.value = true
  try {
    const res = await registerReturnReceipt({
      warehouseId: form.warehouseId,
      returnDate: form.returnDate,
      remark: form.remark,
      items: lines.value.map(line => ({
        erpTenantId: line.erpTenantId!,
        skuCode: line.skuCode.trim(),
        receivedQty: line.receivedQty,
        platformOrderId: line.platformOrderId?.trim(),
        returnReason: line.returnReason,
        photoFileIds: line.photoFileIds
      }))
    })
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
</style>
