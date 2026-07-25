<template>
  <a-modal
    v-model:open="visible"
    title="选择目标库位"
    :width="760"
    :confirm-loading="submitting"
    ok-text="保存计划"
    @ok="submit"
  >
    <a-alert
      type="info"
      show-icon
      message="货物已为关联出库单预留。请为每个批次选择实际物理库位，保存后再执行调整。"
      style="margin-bottom: 16px"
    />
    <a-spin :spinning="loading">
      <a-table :data-source="rows" :columns="columns" :pagination="false" row-key="id" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'source'">
            {{ record.sourceLocationCode || '-' }}
          </template>
          <template v-else-if="column.key === 'target'">
            <a-select
              v-model:value="record.targetLocationCode"
              show-search
              option-filter-prop="label"
              placeholder="选择物理库位"
              :options="record.options"
              style="width: 100%"
            />
          </template>
        </template>
      </a-table>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import {
  completeLocationTransferPlan,
  getLocationTransferDetail,
  listTargetCandidates
} from '@/api/wms/location-transfer'
import type { LocationTransferItemVO } from '@/api/wms/location-transfer/types'

defineOptions({ name: 'LocationTransferPlanModal' })

const emit = defineEmits<{ (event: 'success'): void }>()
type PlanRow = LocationTransferItemVO & {
  targetLocationCode?: string
  options: Array<{ label: string; value: string }>
}

const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const transferId = ref<number>()
const rows = ref<PlanRow[]>([])

const columns = [
  { title: 'SKU', dataIndex: 'skuCode', width: 150, ellipsis: true },
  { title: '来源库位', key: 'source', width: 130 },
  { title: '数量', dataIndex: 'quantity', width: 80, align: 'right' as const },
  { title: '目标物理库位', key: 'target' }
]

async function open(id: number) {
  visible.value = true
  loading.value = true
  transferId.value = id
  rows.value = []
  try {
    const detailRes = await getLocationTransferDetail(id)
    if (!isSuccess(detailRes) || !detailRes.data) return
    rows.value = await Promise.all(
      detailRes.data.items.map(async item => {
        const candidateRes = await listTargetCandidates(item.physicalInventoryId as number)
        const candidates = isSuccess(candidateRes) && candidateRes.data ? candidateRes.data : []
        return {
          ...item,
          options: candidates
            .filter(candidate => candidate.isVirtual !== 1)
            .map(candidate => ({
              label: `${candidate.locationCode}${candidate.zoneName ? ` · ${candidate.zoneName}` : ''}`,
              value: candidate.locationCode
            }))
        }
      })
    )
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!transferId.value || rows.value.some(row => !row.targetLocationCode)) {
    message.warning('请为全部明细选择目标物理库位')
    return
  }
  submitting.value = true
  try {
    const res = await completeLocationTransferPlan(transferId.value, {
      items: rows.value.map(row => ({
        id: row.id,
        targetLocationCode: row.targetLocationCode as string
      }))
    })
    if (isSuccess(res)) {
      message.success('目标库位已保存，请执行调整')
      visible.value = false
      emit('success')
    }
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
