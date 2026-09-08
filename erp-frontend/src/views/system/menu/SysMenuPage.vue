<template>
  <sys-menu-page-search
    v-model:search-title="searchTitle"
    :loading="tableRef?.loading"
    @search="searchTable"
  />
  <pro-table
    ref="tableRef"
    v-model:expanded-row-keys="expandedRowKeys"
    :header-title="t('system.menu.title')"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :expand-icon-column-index="1"
    :pagination="false"
    :scroll="{ x: 1100 }"
    table-class-name="menu-tree-table"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('system:role:add')" @click="handleNew()" />
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <!-- 菜单标题 -->
      <template v-if="column.key === 'title'">
        <AntIcon v-if="record.icon" :type="record.icon" style="margin-right: 6px" />
        <a-tooltip placement="top">
          <template #title>
            <span>{{ enableI18n ? record.i18nTitle || record.title : record.title }}</span>
          </template>
          {{ enableI18n ? record.i18nTitle || record.title : record.title }}
        </a-tooltip>
      </template>
      <!-- 操作栏 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="hasPermission('system:menu:add')" @click="handleNew(record)">{{
            t('system.menu.add')
          }}</a>
          <a v-if="hasPermission('system:menu:edit')" @click="handleEdit(record)">{{
            t('system.menu.edit')
          }}</a>
          <delete-text-button
            v-if="hasPermission('system:menu:del')"
            @confirm="handleDelete(record)"
          />
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 菜单新建/编辑弹窗 -->
  <sys-menu-form-modal
    ref="sysMenuFormModalRef"
    :menu-list="menuList"
    @submit-success="reloadTable"
  />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProTableInstanceExpose } from '#/table'
import type { TableRequest } from '#/table'
import type { ProColumns } from '#/table'
import { useAuthorize } from '@/hooks/permission'
import { listMenus, deleteMenu } from '@/api/system/menu'
import type { SysMenuVOTree, SysMenuVO, SysMenuQO } from '@/api/system/menu/types'
import { listToTree, matchedParentKeys, pruneTree } from '@/utils/tree-utils'
import type { Key } from '@/utils/tree-utils'
import AntIcon from '#/layout/components/AntIcon/index'
import SysMenuPageSearch from '@/views/system/menu/SysMenuPageSearch.vue'
import SysMenuFormModal from '@/views/system/menu/SysMenuFormModal.vue'
import { FormAction } from '@/hooks/form'
import { doRequest } from '@/utils/axios/request'
import { DictText } from '@/components/Dict'
import { NewButton, DeleteTextButton } from '@/components/Button'
import OperationGroup from '@/components/Operation/OperationGroup.vue'
import { enableI18n } from '@/config'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

defineOptions({ name: 'SysMenuPage' })

const { hasPermission } = useAuthorize()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const sysMenuFormModalRef = ref<InstanceType<typeof SysMenuFormModal>>()

// 当前展开的节点 key
const expandedRowKeys = ref<Key[]>()

// 菜单列表，用于透传到子组件，减少查询开销
const menuList = ref<SysMenuVO[]>([])

// 搜索菜单标题，由前端进行数据检索过滤
const searchTitle = ref('')

/* 菜单标题的匹配逻辑 */
const titleMatcher = (node: SysMenuVOTree) => {
  const title = enableI18n ? node.i18nTitle || node.title : node.title
  return searchTitle.value ? title.indexOf(searchTitle.value) > -1 : true
}

// 远程搜索参数
let searchParams: SysMenuQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = async () => {
  const res = await listMenus({ ...searchParams })
  if (res?.data) {
    const data = res.data
    menuList.value = data
    const tree = listToTree<SysMenuVOTree>(data as SysMenuVOTree[], 0)

    // 展开树
    if (searchTitle.value) {
      expandedRowKeys.value = matchedParentKeys<SysMenuVOTree>(tree, titleMatcher)
    }

    return {
      data: {
        records: pruneTree<SysMenuVOTree>(tree, titleMatcher)
      }
    }
  }
  return []
}

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

/* 查询表格 */
const searchTable = (params: SysMenuQO) => {
  searchParams = params
  reloadTable(true) // 会调用 tableRequest
}

/* 新建菜单 */
const handleNew = (record?: SysMenuVO) => {
  sysMenuFormModalRef.value?.open(FormAction.CREATE, record)
}

/* 编辑菜单 */
const handleEdit = (record: SysMenuVO) => {
  sysMenuFormModalRef.value?.open(FormAction.UPDATE, record)
}

/* 删除菜单 */
const handleDelete = (record: SysMenuVO) => {
  doRequest(deleteMenu(record.id), {
    successMessage: t('system.user.deleteSuccess'),
    onSuccess: () => reloadTable()
  })
}

const columns = computed<ProColumns[]>(() => [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80
  },
  {
    title: t('system.menu.name'),
    dataIndex: 'title',
    width: 200,
    ellipsis: true
  },
  {
    title: t('system.menu.permission'),
    dataIndex: 'permission',
    width: 150,
    ellipsis: true
  },
  {
    title: t('system.menu.path'),
    dataIndex: 'path',
    width: 120,
    ellipsis: true
  },
  {
    title: t('system.menu.uri'),
    dataIndex: 'uri',
    width: 180,
    ellipsis: true
  },
  {
    title: t('system.menu.sort'),
    dataIndex: 'sort',
    width: 50
  },
  {
    title: t('system.menu.visible'),
    dataIndex: 'hidden',
    width: 50,
    customRender: function ({ value: hidden }) {
      return h(DictText, { dictCode: 'yes_or_no', value: hidden ? 0 : 1 })
    }
  },
  {
    title: t('system.menu.createTime'),
    dataIndex: 'createTime',
    width: 150
  },
  {
    key: 'operate',
    title: t('system.menu.operation'),
    align: 'center',
    width: 150,
    fixed: 'right'
  }
])
</script>
