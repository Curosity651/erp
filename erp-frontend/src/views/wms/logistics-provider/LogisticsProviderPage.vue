<template>
  <!-- 查询表单 -->
  <logistics-provider-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="物流商管理"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 800 }"
    size="middle"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('wms:logistics-provider:add')" @click="handleNew" />
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <!-- 物流商信息列 -->
      <template v-if="column.key === 'providerInfo'">
        <div class="provider-info-cell">
          <div class="provider-name">{{ record.providerName }}</div>
          <div class="provider-code">{{ record.providerCode }}</div>
        </div>
      </template>

      <!-- 创建时间列 (已移除，直接使用 dataIndex 展示) -->

      <!-- 状态列 -->
      <template v-else-if="column.key === 'status'">
        <a-badge
          :status="record.status === 1 ? 'success' : 'default'"
          :text="record.status === 1 ? '启用' : '停用'"
        />
      </template>

      <!-- 操作列 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a-button
            v-if="hasPermission('wms:logistics-provider:edit')"
            type="link"
            size="small"
            @click="handleEdit(record)"
          >
            编辑
          </a-button>
          <a-popconfirm
            v-if="hasPermission('wms:logistics-provider:edit')"
            :title="`确定要${record.status === 1 ? '停用' : '启用'}该物流商吗？`"
            ok-text="确定"
            cancel-text="取消"
            @confirm="handleToggleStatus(record)"
          >
            <a-button type="link" size="small" :danger="record.status === 1">
              {{ record.status === 1 ? '停用' : '启用' }}
            </a-button>
          </a-popconfirm>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 物流商新建修改的表单弹窗 -->
  <logistics-provider-form-modal ref="formModalRef" @submit-success="reloadTable" />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import type { ProTableInstanceExpose, TableRequest } from '#/table'
import LogisticsProviderPageSearch from './LogisticsProviderPageSearch.vue'
import LogisticsProviderFormModal from './LogisticsProviderFormModal.vue'
import { NewButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useAuthorize } from '@/hooks/permission'
import { mergePageParam } from '@/utils/page-utils'
import { pageLogisticsProvider, updateLogisticsProviderStatus } from '@/api/wms/logistics-provider'
import type {
  LogisticsProviderPageVO,
  LogisticsProviderQO
} from '@/api/wms/logistics-provider/types'
import { FormAction } from '@/hooks/form'
import { doRequest } from '@/utils/axios/request'

defineOptions({ name: 'LogisticsProviderPage' })

// 鉴权方法
const { hasPermission } = useAuthorize()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const formModalRef = ref<InstanceType<typeof LogisticsProviderFormModal>>()

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

// 查询参数
let searchParams: LogisticsProviderQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageLogisticsProvider({ ...pageParam, ...searchParams })
}

/* 查询物流商管理 */
const searchTable = (params: LogisticsProviderQO) => {
  searchParams = params
  reloadTable(true)
}

/* 新建物流商 */
const handleNew = () => {
  formModalRef.value?.open(FormAction.CREATE)
}

/* 编辑物流商 */
const handleEdit = (record: LogisticsProviderPageVO) => {
  formModalRef.value?.open(FormAction.UPDATE, record)
}

/* 切换状态 */
const handleToggleStatus = (record: LogisticsProviderPageVO) => {
  const action = record.status === 1 ? '停用' : '启用'
  const newStatus = record.status === 1 ? 0 : 1
  doRequest(updateLogisticsProviderStatus(record.id, newStatus), {
    successMessage: `${action}成功`,
    onSuccess: () => reloadTable()
  })
}

const columns: ProColumns[] = [
  {
    title: '物流商信息',
    key: 'providerInfo',
    width: 200,
    fixed: 'left'
  },
  {
    title: '联系人',
    dataIndex: 'contactName',
    width: 120,
    ellipsis: true
  },
  {
    title: '联系电话',
    dataIndex: 'contactPhone',
    width: 140,
    copyable: true
  },
  {
    title: '状态',
    key: 'status',
    width: 100
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
    sorter: true
  },
  {
    key: 'operate',
    title: '操作',
    align: 'center',
    width: 140,
    fixed: 'right'
  }
]
</script>

<style scoped>
:deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 600;
}

/* 物流商信息单元格 */
.provider-info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.provider-name {
  font-weight: 500;
  font-size: 14px;
  color: #1f1f1f;
  margin-bottom: 2px;
}

.provider-code {
  font-size: 12px;
  font-family: 'SFMono-Regular', Consolas, 'Courier New', monospace;
  color: #8c8c8c;
}

/* 创建时间单元格 (已移除) */
</style>
