<template>
  <!-- 查询表单 -->
  <project-group-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    :header-title="t('system.projectGroup.pageTitle')"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1000 }"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('system:project-group:add')" @click="handleNew" />
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'status'">
        <dict-tag dict-code="enable_status" :value="record.status"></dict-tag>
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="hasPermission('system:project-group:edit')" @click="handleEdit(record)">{{
            t('action.edit')
          }}</a>
          <delete-text-button
            v-if="hasPermission('system:project-group:del')"
            @confirm="() => handleDelete(record)"
          />
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 系统配置新建修改的表单弹窗 -->
  <project-group-form-modal ref="formModalRef" @submit-success="reloadTable" />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import type { ProTableInstanceExpose, TableRequest } from '#/table'
import ProjectGroupPageSearch from './ProjectGroupPageSearch.vue'
import ProjectGroupFormModal from './ProjectGroupFormModal.vue'
import { NewButton, DeleteTextButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useAuthorize } from '@/hooks/permission'
import { mergePageParam } from '@/utils/page-utils'
import { doRequest } from '@/utils/axios/request'
import { pageProjectGroup, deleteProjectGroup } from '@/api/system/project-group'
import type { ProjectGroupPageVO, ProjectGroupQO } from '@/api/system/project-group/types'
import { FormAction } from '@/hooks/form'
import { DictTag } from '@/components/Dict'
import { useI18n } from 'vue-i18n'

defineOptions({ name: 'ProjectGroupPage' })

// 鉴权方法
const { hasPermission } = useAuthorize()
const { t } = useI18n()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const formModalRef = ref<InstanceType<typeof ProjectGroupFormModal>>()

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

// 查询参数
let searchParams: ProjectGroupQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageProjectGroup({ ...pageParam, ...searchParams })
}

/* 查询项目组管理 */
const searchTable = (params: ProjectGroupQO) => {
  searchParams = params
  reloadTable(true) // 会调用 tableRequest
}

/* 新建项目组管理 */
const handleNew = () => {
  formModalRef.value?.open(FormAction.CREATE)
}

/* 编辑项目组管理 */
const handleEdit = (record: ProjectGroupPageVO) => {
  formModalRef.value?.open(FormAction.UPDATE, record)
}

/* 删除项目组管理 */
const handleDelete = (record: ProjectGroupPageVO) => {
  doRequest(deleteProjectGroup(record.id), {
    successMessage: t('message.removeSuccess'),
    onSuccess: () => reloadTable()
  })
}

const columns = computed<ProColumns[]>(() => [
  {
    title: t('system.projectGroup.name'),
    dataIndex: 'name'
  },
  {
    title: t('system.projectGroup.code'),
    dataIndex: 'code'
  },
  {
    title: t('system.projectGroup.description'),
    dataIndex: 'description'
  },
  {
    title: t('system.projectGroup.status'),
    dataIndex: 'status',
    key: 'status'
  },
  {
    title: t('common.createTime'),
    dataIndex: 'createTime',
    width: 150,
    sorter: true
  },
  {
    key: 'operate',
    title: t('common.operation'),
    align: 'center',
    width: 100
  }
])
</script>
