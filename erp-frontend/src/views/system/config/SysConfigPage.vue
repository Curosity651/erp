<template>
  <!-- 查询表单 -->
  <sys-config-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    :header-title="t('system.config.pageTitle')"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1000 }"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('system:config:add')" @click="handleNew" />
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'operate'">
        <operation-group>
          <a v-if="hasPermission('system:config:edit')" @click="handleEdit(record)">{{
            t('action.edit')
          }}</a>
          <delete-text-button
            v-if="hasPermission('system:config:del')"
            @confirm="() => handleDelete(record)"
          />
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 系统配置新建修改的表单弹窗 -->
  <sys-config-form-modal ref="formModalRef" @submit-success="reloadTable" />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import SysConfigPageSearch from './SysConfigPageSearch.vue'
import { useAuthorize } from '@/hooks/permission'
import type { ProTableInstanceExpose, TableRequest } from '#/table'
import { doRequest } from '@/utils/axios/request'
import { mergePageParam } from '@/utils/page-utils'
import { pageConfigs, deleteConfig } from '@/api/system/config'
import type { SysConfigPageVO, SysConfigQO } from '@/api/system/config/types'
import SysConfigFormModal from '@/views/system/config/SysConfigFormModal.vue'
import { FormAction } from '@/hooks/form'
import { NewButton, DeleteTextButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useI18n } from 'vue-i18n'

defineOptions({ name: 'SysConfigPage' })

// 鉴权方法
const { hasPermission } = useAuthorize()
const { t } = useI18n()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const formModalRef = ref<InstanceType<typeof SysConfigFormModal>>()

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

// 查询参数
let searchParams: SysConfigQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageConfigs({ ...pageParam, ...searchParams })
}

/* 查询表格 */
const searchTable = (params: SysConfigQO) => {
  searchParams = params
  reloadTable(true) // 会调用 tableRequest
}

/* 新建配置 */
const handleNew = () => {
  formModalRef.value?.open(FormAction.CREATE)
}

/* 编辑配置 */
const handleEdit = (record: SysConfigPageVO) => {
  formModalRef.value?.open(FormAction.UPDATE, record)
}

/* 删除配置 */
const handleDelete = (record: SysConfigPageVO) => {
  doRequest(deleteConfig(record.confKey), {
    successMessage: t('message.removeSuccess'),
    onSuccess: () => reloadTable()
  })
}

const columns = computed<ProColumns[]>(() => [
  {
    title: t('system.config.name'),
    dataIndex: 'name',
    width: 100,
    ellipsis: true
  },
  {
    title: 'Key',
    dataIndex: 'confKey',
    width: 100,
    ellipsis: true
  },
  {
    title: 'Value',
    dataIndex: 'confValue',
    width: 100,
    ellipsis: true
  },
  {
    title: t('system.config.category'),
    dataIndex: 'category',
    width: 100
  },
  {
    title: t('common.remarks'),
    dataIndex: 'remarks',
    width: 200,
    ellipsis: true
  },
  {
    title: t('common.createTime'),
    dataIndex: 'createTime',
    width: 150,
    sorter: true
  },
  {
    title: t('common.updateTime'),
    dataIndex: 'updateTime',
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
