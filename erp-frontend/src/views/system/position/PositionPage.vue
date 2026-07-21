<template>
  <!-- 查询表单 -->
  <position-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="岗位管理"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1000 }"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('system:position:add')" @click="handleNew" />
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'status'">
        <dict-tag dict-code="enable_status" :value="record.status"></dict-tag>
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="hasPermission('system:position:edit')" @click="handleEdit(record)">编辑</a>
          <delete-text-button
            v-if="hasPermission('system:position:del')"
            @confirm="() => handleDelete(record)"
          />
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 系统配置新建修改的表单弹窗 -->
  <position-form-modal ref="formModalRef" @submit-success="reloadTable" />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import type { ProTableInstanceExpose, TableRequest } from '#/table'
import PositionPageSearch from './PositionPageSearch.vue'
import PositionFormModal from './PositionFormModal.vue'
import { NewButton, DeleteTextButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useAuthorize } from '@/hooks/permission'
import { mergePageParam } from '@/utils/page-utils'
import { doRequest } from '@/utils/axios/request'
import { pagePosition, deletePosition } from '@/api/system/position'
import type { PositionPageVO, PositionQO } from '@/api/system/position/types'
import { FormAction } from '@/hooks/form'
import { DictTag } from '@/components/Dict'

defineOptions({ name: 'PositionPage' })

// 鉴权方法
const { hasPermission } = useAuthorize()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const formModalRef = ref<InstanceType<typeof PositionFormModal>>()

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

// 查询参数
let searchParams: PositionQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pagePosition({ ...pageParam, ...searchParams })
}

/* 查询岗位管理 */
const searchTable = (params: PositionQO) => {
  searchParams = params
  reloadTable(true) // 会调用 tableRequest
}

/* 新建岗位管理 */
const handleNew = () => {
  formModalRef.value?.open(FormAction.CREATE)
}

/* 编辑岗位管理 */
const handleEdit = (record: PositionPageVO) => {
  formModalRef.value?.open(FormAction.UPDATE, record)
}

/* 删除岗位管理 */
const handleDelete = (record: PositionPageVO) => {
  doRequest(deletePosition(record.id), {
    successMessage: '删除成功！',
    onSuccess: () => reloadTable()
  })
}

const columns: ProColumns[] = [
  {
    title: '#',
    dataIndex: 'id'
  },
  {
    title: '岗位名称',
    dataIndex: 'name'
  },
  {
    title: '岗位编码',
    dataIndex: 'code'
  },
  {
    title: '岗位描述',
    dataIndex: 'description'
  },
  {
    title: '显示顺序',
    dataIndex: 'sort'
  },
  {
    title: '状态',
    dataIndex: 'status'
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 150,
    sorter: true
  },
  {
    key: 'operate',
    title: '操作',
    align: 'center',
    width: 100
  }
]
</script>
