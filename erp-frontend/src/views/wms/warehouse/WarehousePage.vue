<template>
  <!-- 查询表单 -->
  <warehouse-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="仓库管理"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 600 }"
    size="middle"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('wms:warehouse:add')" @click="handleNew">
        新增自有仓
      </new-button>
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <!-- 仓库信息列 -->
      <template v-if="column.key === 'warehouseInfo'">
        <div class="warehouse-info-cell">
          <div class="warehouse-name">{{ record.warehouseName }}</div>
          <div class="warehouse-code">{{ record.warehouseCode }}</div>
        </div>
      </template>

      <!-- 所属平台列 -->
      <template v-else-if="column.key === 'platformInfo'">
        <div v-if="record.warehouseType === 'FBO'" class="platform-info-cell">
          <div class="platform-name">{{ getPlatformLabel(record.platform) }}</div>
          <div v-if="record.platformWarehouseId" class="platform-id">
            {{ record.platformWarehouseId }}
          </div>
        </div>
        <span v-else class="empty-text">-</span>
      </template>

      <!-- 仓库类型列 -->
      <template v-else-if="column.key === 'warehouseType'">
        <a-tag :color="record.warehouseType === 'OWN' ? 'blue' : 'orange'">
          {{ record.warehouseType === 'OWN' ? '自有仓' : 'FBO仓' }}
        </a-tag>
      </template>

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
            v-if="hasPermission('wms:warehouse:edit')"
            type="link"
            size="small"
            @click="handleEdit(record)"
          >
            编辑
          </a-button>
          <a-popconfirm
            v-if="hasPermission('wms:warehouse:edit')"
            :title="`确定要${record.status === 1 ? '停用' : '启用'}该仓库吗？`"
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

  <!-- 仓库新建修改的表单弹窗 -->
  <warehouse-form-modal ref="formModalRef" @submit-success="reloadTable" />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import type { ProTableInstanceExpose, TableRequest } from '#/table'
import WarehousePageSearch from './WarehousePageSearch.vue'
import WarehouseFormModal from './WarehouseFormModal.vue'
import { NewButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useAuthorize } from '@/hooks/permission'
import { mergePageParam } from '@/utils/page-utils'
import { pageWarehouse, updateWarehouseStatus } from '@/api/wms/warehouse'
import { getPlatformLabel } from '@/components/Platform'
import type { WarehousePageVO, WarehouseQO } from '@/api/wms/warehouse/types'
import { FormAction } from '@/hooks/form'
import { doRequest } from '@/utils/axios/request'

defineOptions({ name: 'WarehousePage' })

// 鉴权方法
const { hasPermission } = useAuthorize()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const formModalRef = ref<InstanceType<typeof WarehouseFormModal>>()

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

// 查询参数
let searchParams: WarehouseQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageWarehouse({ ...pageParam, ...searchParams })
}

/* 查询仓库管理 */
const searchTable = (params: WarehouseQO) => {
  searchParams = params
  reloadTable(true) // 会调用 tableRequest
}

/* 新建仓库 */
const handleNew = () => {
  formModalRef.value?.open(FormAction.CREATE)
}

/* 编辑仓库 */
const handleEdit = (record: WarehousePageVO) => {
  formModalRef.value?.open(FormAction.UPDATE, record)
}

/* 切换状态 */
const handleToggleStatus = (record: WarehousePageVO) => {
  const action = record.status === 1 ? '停用' : '启用'
  const newStatus = record.status === 1 ? 0 : 1
  doRequest(updateWarehouseStatus(record.id, newStatus), {
    successMessage: `${action}成功`,
    onSuccess: () => reloadTable()
  })
}

const columns: ProColumns[] = [
  {
    title: '仓库信息',
    key: 'warehouseInfo',
    width: 160,
    fixed: 'left'
  },
  {
    title: '类型',
    key: 'warehouseType',
    width: 100
  },
  {
    title: '所属区域',
    dataIndex: 'regionName',
    width: 120
  },
  {
    title: '所属平台',
    key: 'platformInfo',
    width: 140
  },
  {
    title: '联系人',
    dataIndex: 'contactName',
    width: 100,
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
    width: 100,
    align: 'center'
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

/* 仓库信息单元格 */
.warehouse-info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.warehouse-name {
  font-weight: 500;
  font-size: 14px;
  color: #1f1f1f;
  margin-bottom: 2px;
}

.warehouse-code {
  font-size: 12px;
  font-family: 'SFMono-Regular', Consolas, 'Courier New', monospace;
  color: #8c8c8c;
}

/* 平台信息单元格 */
.platform-info-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.platform-name {
  font-size: 14px;
  color: #262626;
}

.platform-id {
  font-size: 12px;
  font-family: 'SFMono-Regular', Consolas, 'Courier New', monospace;
  color: #8c8c8c;
}

.empty-text {
  color: #bfbfbf;
}
</style>
