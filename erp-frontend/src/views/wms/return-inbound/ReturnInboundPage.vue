<template>
  <return-inbound-page-search :loading="tableRef?.loading" @search="searchTable" />
  <pro-table
    ref="tableRef"
    header-title="退货处理"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1180 }"
    size="middle"
  >
    <template #toolBarRender
      ><a-button @click="handleExport"><download-outlined />导出</a-button></template
    >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'returnInfo'"
        ><a @click="openDisposition(record, true)">{{ record.returnNo }}</a>
        <div class="muted">批次 {{ record.returnBatchNo || '-' }}</div></template
      >
      <template v-else-if="column.key === 'skuSummary'"
        >{{ record.skuKinds || 0 }} 种 · {{ record.totalQuantity }} 件</template
      >
      <template v-else-if="column.key === 'returnStatus'"
        ><a-badge
          :status="statusBadge(record.returnStatus) as any"
          :text="statusText(record.returnStatus)"
      /></template>
      <template v-else-if="column.key === 'quantity'"
        ><span>上架 {{ record.qualifiedQuantity || 0 }}</span
        ><span class="split">销毁 {{ record.scrapQuantity || 0 }}</span></template
      >
      <template v-else-if="column.key === 'operate'"
        ><operation-group
          ><a v-if="record.returnStatus === 'PENDING_OWNER'" @click="openDisposition(record, false)"
            >决定处理方式</a
          ><a v-else @click="openDisposition(record, true)">查看</a></operation-group
        ></template
      >
    </template>
  </pro-table>
  <ReturnDispositionModal
    v-model:open="modalOpen"
    :order-id="currentId"
    :readonly="readonly"
    @success="reloadTable(false)"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { OperationGroup } from '@/components/Operation'
import ReturnInboundPageSearch from './ReturnInboundPageSearch.vue'
import ReturnDispositionModal from './ReturnDispositionModal.vue'
import { mergePageParam } from '@/utils/page-utils'
import { remoteFileDownload } from '@/utils/file-utils'
import { pageReturnInbound, exportReturnInbound } from '@/api/wms/return-inbound'
import type { ReturnInboundPageVO, ReturnInboundQO } from '@/api/wms/return-inbound/types'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'

defineOptions({ name: 'ReturnInboundPage' })
const tableRef = ref<ProTableInstanceExpose>()
let searchParams: ReturnInboundQO = {}
const tableRequest: TableRequest = (params, sorter, filter) =>
  pageReturnInbound({ ...mergePageParam(params, sorter, filter), ...searchParams })
const reloadTable = (reset?: boolean) => tableRef.value?.actionRef?.reload(reset)
useTableActivateReload(() => reloadTable(false))
const searchTable = (params: ReturnInboundQO) => {
  searchParams = params
  reloadTable(true)
}
const columns: ProColumns[] = [
  { title: '退货处理单', key: 'returnInfo', width: 210, fixed: 'left' },
  { title: '仓库', dataIndex: 'warehouseName', width: 140 },
  { title: 'SKU/件数', key: 'skuSummary', width: 120 },
  { title: '退货日期', dataIndex: 'returnDate', width: 120 },
  { title: '状态', key: 'returnStatus', width: 130 },
  { title: '处理结果', key: 'quantity', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', width: 170, sorter: true },
  { title: '操作', key: 'operate', width: 130, align: 'center', fixed: 'right' }
]
const statusText = (value: string) =>
  (
    ({
      PENDING_OWNER: '待货主处置',
      PENDING_OPERATION: '待仓库处理',
      COMPLETED: '已完成',
      CLOSED: '已关闭'
    }) as Record<string, string>
  )[value] || value
const statusBadge = (value: string) =>
  (
    ({
      PENDING_OWNER: 'warning',
      PENDING_OPERATION: 'processing',
      COMPLETED: 'success',
      CLOSED: 'default'
    }) as Record<string, string>
  )[value] || 'default'
const modalOpen = ref(false)
const readonly = ref(false)
const currentId = ref<number>()
function openDisposition(record: ReturnInboundPageVO, read: boolean) {
  currentId.value = record.id
  readonly.value = read
  modalOpen.value = true
}
async function handleExport() {
  try {
    remoteFileDownload(await exportReturnInbound(searchParams))
    message.success('导出成功')
  } catch {
    message.error('导出失败')
  }
}
</script>

<style scoped>
.muted {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 3px;
}
.split {
  margin-left: 12px;
  color: #cf1322;
}
</style>
