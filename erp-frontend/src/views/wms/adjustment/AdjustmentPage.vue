<template>
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ padding: '20px 24px' }">
    <a-form layout="inline" class="adjustment-search" :label-col="{ style: { lineHeight: '32px' } }">
      <a-form-item label="报废单号">
        <a-input v-model:value="search.adjustmentNo" placeholder="请输入" allow-clear style="width: 150px" />
      </a-form-item>
      <a-form-item label="状态">
        <a-select
          v-model:value="search.orderStatus"
          :options="statusOptions"
          placeholder="全部"
          allow-clear
          style="width: 110px"
        />
      </a-form-item>
      <a-form-item label="日期范围">
        <a-range-picker
          v-model:value="dateRange"
          value-format="YYYY-MM-DD"
          style="width: 220px"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="服务商">
        <wms-operator-select
          v-model:value="search.wmsTenantId"
          placeholder="全部"
          width="140px"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="货主">
        <platform-owner-select
          v-model:value="search.erpTenantId"
          :operator-id="search.wmsTenantId"
          placeholder="全部"
          width="140px"
          allow-clear
        />
      </a-form-item>
      <a-form-item class="search-actions-item">
        <a-space>
          <a-button type="primary" @click="searchTable">查询</a-button>
          <a-button @click="resetSearch">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>
  </a-card>

  <pro-table
    ref="tableRef"
    header-title="报废单"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1240 }"
    size="middle"
  >
    <template #toolBarRender>
      <a-button v-if="hasPermission('wms:adjustment:add')" type="primary" danger @click="handleNew">
        <plus-outlined />
        发起报废
      </a-button>
    </template>

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
          <confirm-text-button
            v-if="record.orderStatus === 'PENDING_OWNER' && hasPermission('wms:adjustment:cancel')"
            title="确认撤销该报废申请吗？冻结的货物将释放。"
            text="撤销"
            @confirm="handleCancel(record)"
          />
        </operation-group>
      </template>
    </template>
  </pro-table>

  <scrap-create-drawer ref="createRef" @success="reloadTable" />
  <adjustment-detail-drawer ref="detailRef" />
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { OperationGroup } from '@/components/Operation'
import { ConfirmTextButton } from '@/components/Button'
import WmsOperatorSelect from '@/components/Lov/WmsOperatorSelect.vue'
import PlatformOwnerSelect from '@/components/Lov/PlatformOwnerSelect.vue'
import { useAuthorize } from '@/hooks/permission'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'
import { mergePageParam } from '@/utils/page-utils'
import { doRequest } from '@/utils/axios/request'
import { pageAdjustment, cancelScrap } from '@/api/wms/adjustment'
import { ScrapStatusList } from '@/api/wms/adjustment/types'
import type { AdjustmentPageVO, AdjustmentQO, ScrapStatus } from '@/api/wms/adjustment/types'
import ScrapCreateDrawer from './ScrapCreateDrawer.vue'
import AdjustmentDetailDrawer from './AdjustmentDetailDrawer.vue'

defineOptions({ name: 'AdjustmentPage' })

const { hasPermission } = useAuthorize()
const tableRef = ref<ProTableInstanceExpose>()
const createRef = ref<InstanceType<typeof ScrapCreateDrawer>>()
const detailRef = ref<InstanceType<typeof AdjustmentDetailDrawer>>()

const statusOptions = ScrapStatusList.map(s => ({ label: s.label, value: s.value }))
const statusText = (s: ScrapStatus) => ScrapStatusList.find(x => x.value === s)?.label || s
const badge = (s: ScrapStatus) => (ScrapStatusList.find(x => x.value === s)?.badge as any) || 'default'

const search = reactive<AdjustmentQO>({
  adjustmentNo: undefined,
  wmsTenantId: undefined,
  erpTenantId: undefined,
  orderStatus: undefined
})
// 日期范围（[开始, 结束]，按报废日期 adjustment_date 过滤）
const dateRange = ref<[string, string]>()
let searchParams: AdjustmentQO = {}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageAdjustment({ ...pageParam, ...searchParams })
}
const reloadTable = (reset?: boolean) => tableRef.value?.actionRef?.reload(reset)

useTableActivateReload(() => reloadTable(false))
const searchTable = () => {
  searchParams = {
    ...search,
    adjustmentDateStart: dateRange.value?.[0],
    adjustmentDateEnd: dateRange.value?.[1]
  }
  reloadTable(true)
}
const resetSearch = () => {
  search.adjustmentNo = undefined
  search.wmsTenantId = undefined
  search.erpTenantId = undefined
  search.orderStatus = undefined
  dateRange.value = undefined
  searchTable()
}

const columns: ProColumns[] = [
  { title: '报废单号', key: 'no', width: 170, fixed: 'left' },
  { title: '所属服务商', dataIndex: 'operatorName', width: 130, ellipsis: true },
  { title: '货主', dataIndex: 'ownerName', width: 130, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', width: 120, ellipsis: true },
  { title: '统计', key: 'statistics', width: 130 },
  { title: '状态', key: 'status', width: 110 },
  { title: '报废原因', dataIndex: 'adjustmentReason', width: 150, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { key: 'operate', title: '操作', align: 'center', width: 130, fixed: 'right' }
]

const handleNew = () => createRef.value?.open()
const handleViewDetail = (r: AdjustmentPageVO) => detailRef.value?.open(r.id)
const handleCancel = (r: AdjustmentPageVO) => {
  doRequest(cancelScrap(r.id), { successMessage: '已撤销', onSuccess: () => reloadTable() })
}
</script>

<style scoped>
/* 搜索栏：所有筛选项一排排列，查询/重置按钮靠右对齐 */
.adjustment-search {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  row-gap: 8px;
}
.adjustment-search :deep(.ant-form-item) {
  margin-right: 12px;
}
.adjustment-search .search-actions-item {
  margin-left: auto;
  margin-right: 0;
}
</style>
