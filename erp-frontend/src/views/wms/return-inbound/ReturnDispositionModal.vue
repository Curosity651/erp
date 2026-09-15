<template>
  <a-modal
    :open="open"
    :title="readonly ? '退货处理详情' : '决定退货处理方式'"
    :width="1040"
    :footer="readonly ? null : undefined"
    :confirm-loading="submitting"
    ok-text="提交处置决定"
    @ok="submit"
    @cancel="close"
  >
    <a-spin :spinning="loading">
      <a-alert
        v-if="!readonly"
        type="info"
        show-icon
        message="每条SKU可拆分为直接上架、返工检测和销毁，三项数量之和必须等于实收数量。提交后由海外仓执行。"
        style="margin-bottom: 16px"
      />
      <a-descriptions v-if="detail" bordered size="small" :column="4" style="margin-bottom: 16px">
        <a-descriptions-item label="处理单">{{ detail.returnNo }}</a-descriptions-item>
        <a-descriptions-item label="退货批次">{{
          detail.returnBatchNo || '-'
        }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ detail.warehouseName }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{
          statusText(detail.returnStatus)
        }}</a-descriptions-item>
      </a-descriptions>
      <a-table
        :data-source="lines"
        :pagination="false"
        row-key="id"
        size="small"
        :scroll="{ x: 960 }"
      >
        <a-table-column title="内部 SKU" :width="190"
          ><template #default="{ record }"
            ><strong>{{ record.warehouseSkuCode || record.skuCode }}</strong>
            <div class="muted">{{ record.skuName }}</div></template
          ></a-table-column
        >
        <a-table-column title="来源订单/凭证" :width="170"
          ><template #default="{ record }"
            ><div>{{ record.platformOrderId || '-' }}</div>
            <a-image-preview-group v-if="record.qcPhotoFileIds?.length"
              ><a-image
                v-for="fileId in record.qcPhotoFileIds"
                :key="fileId"
                :width="34"
                :height="34"
                :src="photoUrls[fileId]" /></a-image-preview-group
            ><span v-else class="muted">无照片</span></template
          ></a-table-column
        >
        <a-table-column title="实收" data-index="receivedQty" :width="70" align="right" />
        <a-table-column title="直接上架" :width="110"
          ><template #default="{ record }"
            ><span v-if="readonly">{{ record.restockQty || 0 }}</span
            ><a-input-number
              v-else
              v-model:value="record.restockQty"
              :min="0"
              :max="record.receivedQty"
              style="width: 90px" /></template
        ></a-table-column>
        <a-table-column title="返工检测" :width="110"
          ><template #default="{ record }"
            ><span v-if="readonly">{{ record.reworkQty || 0 }}</span
            ><a-input-number
              v-else
              v-model:value="record.reworkQty"
              :min="0"
              :max="record.receivedQty"
              style="width: 90px" /></template
        ></a-table-column>
        <a-table-column title="销毁" :width="110"
          ><template #default="{ record }"
            ><span v-if="readonly">{{ record.scrapQty || 0 }}</span
            ><a-input-number
              v-else
              v-model:value="record.scrapQty"
              :min="0"
              :max="record.receivedQty"
              style="width: 90px" /></template
        ></a-table-column>
        <a-table-column title="合计" :width="90"
          ><template #default="{ record }"
            ><span :class="{ invalid: sum(record) !== record.receivedQty }"
              >{{ sum(record) }} / {{ record.receivedQty }}</span
            ></template
          ></a-table-column
        >
        <a-table-column title="说明" :width="220"
          ><template #default="{ record }"
            ><span v-if="readonly">{{ record.dispositionRemark || '-' }}</span
            ><a-input
              v-else
              v-model:value="record.dispositionRemark"
              :maxlength="500"
              allow-clear /></template
        ></a-table-column>
      </a-table>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { getReturnInboundDetail, submitReturnDisposition } from '@/api/wms/return-inbound'
import type { ReturnInboundDetailVO } from '@/api/wms/return-inbound/types'
import { isSuccess } from '@/api'
import { batchGetDownloadUrls } from '@/api/system/file'

const props = defineProps<{ open: boolean; orderId?: number; readonly?: boolean }>()
const emit = defineEmits<{ (e: 'update:open', value: boolean): void; (e: 'success'): void }>()
const loading = ref(false),
  submitting = ref(false)
const detail = ref<ReturnInboundDetailVO | null>(null)
const lines = ref<any[]>([])
const photoUrls = ref<Record<number, string>>({})
const sum = (line: any) =>
  Number(line.restockQty || 0) + Number(line.reworkQty || 0) + Number(line.scrapQty || 0)
const statusText = (status: string) =>
  (
    ({
      PENDING_OWNER: '待货主处置',
      PENDING_OPERATION: '待仓库处理',
      COMPLETED: '已完成',
      CLOSED: '已关闭'
    }) as Record<string, string>
  )[status] || status
function close() {
  emit('update:open', false)
}
async function load(id: number) {
  loading.value = true
  try {
    const res = await getReturnInboundDetail(id)
    if (isSuccess(res) && res.data) {
      detail.value = res.data
      lines.value = (res.data.items || []).map(item => ({
        ...item,
        restockQty: props.readonly ? item.restockQty || 0 : item.receivedQty,
        reworkQty: item.reworkQty || 0,
        scrapQty: item.scrapQty || 0
      }))
      const ids = lines.value.flatMap(item => item.qcPhotoFileIds || [])
      if (ids.length) {
        const urls = await batchGetDownloadUrls(ids)
        if (isSuccess(urls) && urls.data) photoUrls.value = urls.data as Record<number, string>
      }
    }
  } finally {
    loading.value = false
  }
}
watch(
  () => [props.open, props.orderId] as const,
  ([open, id]) => {
    if (open && id) load(id)
  },
  { immediate: true }
)
async function submit() {
  if (!detail.value) return
  if (lines.value.some(line => sum(line) !== Number(line.receivedQty)))
    return message.warning('每条SKU的三项处置数量之和必须等于实收数量')
  submitting.value = true
  try {
    const res = await submitReturnDisposition({
      returnOrderId: detail.value.id,
      items: lines.value.map(line => ({
        itemId: line.id,
        restockQty: Number(line.restockQty || 0),
        reworkQty: Number(line.reworkQty || 0),
        scrapQty: Number(line.scrapQty || 0),
        remark: line.dispositionRemark
      }))
    })
    if (!isSuccess(res)) return message.error(res.message || '提交失败')
    message.success('处置决定已提交给海外仓')
    emit('success')
    close()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.muted {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 3px;
}
.invalid {
  color: #ff4d4f;
  font-weight: 600;
}
</style>
