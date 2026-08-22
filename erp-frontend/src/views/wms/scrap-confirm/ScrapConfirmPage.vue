<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ padding: '20px 24px' }">
    <a-form layout="inline" :label-col="{ style: { lineHeight: '32px' } }">
      <a-form-item label="报废单号">
        <a-input v-model:value="search.adjustmentNo" placeholder="请输入" allow-clear style="width: 180px" />
      </a-form-item>
      <a-form-item label="状态">
        <a-select
          v-model:value="search.orderStatus"
          :options="statusOptions"
          placeholder="全部"
          allow-clear
          style="width: 160px"
        />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="searchTable">查询</a-button>
          <a-button @click="resetSearch">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>
  </a-card>

  <pro-table
    ref="tableRef"
    header-title="待确认报废"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1000 }"
    size="middle"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'no'">
        <a @click="handleViewDetail(record)">{{ record.adjustmentNo }}</a>
      </template>
      <template v-else-if="column.key === 'statistics'">
        {{ record.skuCount }} 批次 · {{ record.totalQuantity }} 件
      </template>
      <template v-else-if="column.key === 'status'">
        <a-badge :status="badge(record.orderStatus)" :text="statusText(record.orderStatus)" />
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a @click="handleViewDetail(record)">查看</a>
          <template v-if="record.orderStatus === 'PENDING_OWNER'">
            <confirm-text-button
              title="同意仓库报废这批货物吗？同意后进入待仓库销毁，暂不扣减库存。"
              text="同意报废"
              @confirm="handleConfirm(record)"
            />
            <a style="color: #ff4d4f" @click="openReject(record)">驳回</a>
          </template>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <adjustment-detail-drawer ref="detailRef" />

  <a-modal v-model:open="rejectOpen" title="驳回报废" :confirm-loading="rejecting" @ok="doReject">
    <p>驳回后该批报废将取消，货物恢复正常。</p>
    <a-textarea v-model:value="rejectReason" placeholder="驳回原因（可选）" :rows="3" :maxlength="500" />
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { OperationGroup } from '@/components/Operation'
import { ConfirmTextButton } from '@/components/Button'
import { mergePageParam } from '@/utils/page-utils'
import { doRequest } from '@/utils/axios/request'
import { pageAdjustment, ownerConfirmScrap, ownerRejectScrap } from '@/api/wms/adjustment'
import { ScrapStatusList } from '@/api/wms/adjustment/types'
import type { AdjustmentPageVO, AdjustmentQO, ScrapStatus } from '@/api/wms/adjustment/types'
import AdjustmentDetailDrawer from '../adjustment/AdjustmentDetailDrawer.vue'

defineOptions({ name: 'ScrapConfirmPage' })

const tableRef = ref<ProTableInstanceExpose>()
const detailRef = ref<InstanceType<typeof AdjustmentDetailDrawer>>()

const statusOptions = ScrapStatusList.map(s => ({ label: s.label, value: s.value }))
const statusText = (s: ScrapStatus) => ScrapStatusList.find(x => x.value === s)?.label || s
const badge = (s: ScrapStatus) => (ScrapStatusList.find(x => x.value === s)?.badge as any) || 'default'

// 默认展示全部状态：货主确认/驳回后记录仍保留可查（留痕）
const search = reactive<AdjustmentQO>({ adjustmentNo: undefined, orderStatus: undefined })
let searchParams: AdjustmentQO = {}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageAdjustment({ ...pageParam, ...searchParams })
}
const reloadTable = (reset?: boolean) => tableRef.value?.actionRef?.reload(reset)

useTableActivateReload(() => reloadTable(false))
const searchTable = () => {
  searchParams = { ...search }
  reloadTable(true)
}
const resetSearch = () => {
  search.adjustmentNo = undefined
  search.orderStatus = undefined
  searchTable()
}

const columns: ProColumns[] = [
  { title: '报废单号', key: 'no', width: 170, fixed: 'left' },
  { title: '仓库', dataIndex: 'warehouseName', width: 120, ellipsis: true },
  { title: '统计', key: 'statistics', width: 130 },
  { title: '状态', key: 'status', width: 110 },
  { title: '报废原因', dataIndex: 'adjustmentReason', width: 160, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { key: 'operate', title: '操作', align: 'center', width: 180, fixed: 'right' }
]

const handleViewDetail = (r: AdjustmentPageVO) => detailRef.value?.open(r.id)
const handleConfirm = (r: AdjustmentPageVO) => {
  doRequest(ownerConfirmScrap(r.id), {
    successMessage: '已同意报废，等待仓库实际销毁',
    onSuccess: () => reloadTable()
  })
}

// 驳回
const rejectOpen = ref(false)
const rejecting = ref(false)
const rejectReason = ref<string>()
const rejectTarget = ref<AdjustmentPageVO>()
const openReject = (r: AdjustmentPageVO) => {
  rejectTarget.value = r
  rejectReason.value = undefined
  rejectOpen.value = true
}
const doReject = () => {
  if (!rejectTarget.value) return
  if (!rejectReason.value?.trim()) {
    message.warning('请填写驳回原因')
    return
  }
  rejecting.value = true
  doRequest(ownerRejectScrap(rejectTarget.value.id, rejectReason.value), {
    successMessage: '已驳回',
    onSuccess: () => {
      rejectOpen.value = false
      reloadTable()
    },
    onFinally: () => {
      rejecting.value = false
    }
  })
}
</script>
