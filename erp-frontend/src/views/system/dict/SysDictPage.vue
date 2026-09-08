<template>
  <!-- 查询组件 -->
  <sys-dict-page-search :loading="tableRef?.loading" @search="searchTable" />

  <!-- 表格区域 -->
  <pro-table
    ref="tableRef"
    :header-title="t('system.dict.pageTitle')"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 800 }"
  >
    <template #toolBarRender>
      <new-button v-if="hasPermission('system:dict:add')" @click="handleNew" />
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'operate'">
        <a-dropdown :trigger="['click']">
          <a class="ant-dropdown-link" @click.prevent>{{ t('common.operation') }}</a>
          <template #overlay>
            <a-menu>
              <a-menu-item v-if="hasPermission('system:dict:edit')">
                <a @click="handleEdit(record)">{{ t('action.edit') }}</a>
              </a-menu-item>
              <a-menu-item v-if="hasPermission('system:dict:read')">
                <a @click="openDictItemModal(record)">{{ t('system.dict.items') }}</a>
              </a-menu-item>
              <a-menu-item v-if="hasPermission('system:dict:edit')">
                <a @click="handleRefresh(record)">{{ t('system.dict.refreshHash') }}</a>
              </a-menu-item>
              <a-menu-item v-if="hasPermission('system:dict:del')">
                <delete-text-button @confirm="handleDelete(record)" />
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </template>
    </template>
  </pro-table>

  <!-- 字典新建修改的表单弹窗 -->
  <sys-dict-form-modal ref="sysDictFormModalRef" @submit-success="reloadTable" />

  <!-- 字典项的弹窗 -->
  <sys-dict-item-modal ref="sysDictItemModalRef" />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import type { ProTableInstanceExpose } from '#/table/Table'
import type { TableRequest } from '#/table/typing'
import { mergePageParam } from '@/utils/page-utils'
import { useAuthorize } from '@/hooks/permission'
import type { SysDictPageVO, SysDictQO } from '@/api/system/dict/types'
import { pageDicts, deleteDict, refreshHash } from '@/api/system/dict'
import { FormAction } from '@/hooks/form'
import { doRequest } from '@/utils/axios/request'
import SysDictPageSearch from '@/views/system/dict/SysDictPageSearch.vue'
import SysDictFormModal from '@/views/system/dict/SysDictFormModal.vue'
import SysDictItemModal from '@/views/system/dict/SysDictItemModal.vue'
import { DictTag } from '@/components/Dict'
import { NewButton, DeleteTextButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useI18n } from 'vue-i18n'

defineOptions({ name: 'SysDictPage' })

// 鉴权方法
const { hasPermission } = useAuthorize()
const { t } = useI18n()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const sysDictFormModalRef = ref<InstanceType<typeof SysDictFormModal>>()
const sysDictItemModalRef = ref<InstanceType<typeof SysDictItemModal>>()

// 查询参数
let searchParams: SysDictQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageDicts({ ...pageParam, ...searchParams })
}

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

/* 查询表格 */
const searchTable = (params: SysDictQO) => {
  searchParams = params
  reloadTable(true) // 会调用 tableRequest
}

/* 新建字典 */
const handleNew = () => {
  sysDictFormModalRef.value?.open(FormAction.CREATE)
}

/* 编辑字典 */
const handleEdit = (record: SysDictPageVO) => {
  sysDictFormModalRef.value?.open(FormAction.UPDATE, record)
}

/* 编辑字典 */
const handleRefresh = (record: SysDictPageVO) => {
  doRequest(refreshHash(record.id), {
    successMessage: t('system.dict.refreshSuccess'),
    onSuccess: () => reloadTable()
  })
}

/* 删除字典 */
const handleDelete = (record: SysDictPageVO) => {
  doRequest(deleteDict(record.id), {
    successMessage: t('message.removeSuccess'),
    onSuccess: () => reloadTable()
  })
}

const openDictItemModal = (record: SysDictPageVO) => {
  sysDictItemModalRef.value?.open(record)
}

const columns = computed<ProColumns[]>(() => [
  {
    title: t('system.dict.code'),
    dataIndex: 'code',
    width: 180,
    ellipsis: true
  },
  {
    title: t('system.dict.title'),
    dataIndex: 'title',
    width: 180,
    ellipsis: true
  },
  {
    title: t('system.dict.valueType'),
    dataIndex: 'valueType',
    width: 180,
    customRender: function ({ value }) {
      return h(DictTag, { dictCode: 'dict_value_type', value: value })
    }
  },
  {
    title: t('common.remarks'),
    dataIndex: 'remarks',
    width: 250,
    ellipsis: true
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
    width: 80
  }
])
</script>
