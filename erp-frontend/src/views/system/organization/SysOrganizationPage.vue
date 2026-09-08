<template>
  <pro-table
    ref="tableRef"
    v-model:expanded-row-keys="expandedRowKeys"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :pagination="false"
    :scroll="{ x: 800 }"
  >
    <template #headerTitle>
      <a-space :size="24">
        {{ t('system.organization.title') }}
        <a-input-search
          v-model:value="searchName"
          :placeholder="t('system.organization.name')"
          allow-clear
          @search="searchTable"
        />
      </a-space>
    </template>

    <!-- 修改展开的 icon -->
    <template #expandIcon="props">
      <template v-if="props.record.children?.length > 0">
        <span class="expandIcon" @click="e => props.onExpand(props.record, e)">
          <CaretDownOutlined v-if="props.expanded" />
          <CaretRightOutlined v-else />
        </span>
      </template>
      <template v-else>
        <span class="expandIcon leafNode"></span>
      </template>
    </template>

    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <a-popconfirm
        v-if="hasPermission('system:organization:revised')"
        :title="t('system.organization.reviseConfirm')"
        :ok-text="t('common.confirm')"
        :cancel-text="t('common.cancel')"
        @confirm="handleRevised"
      >
        <a-button type="primary" danger>
          <InteractionOutlined />{{ t('system.organization.revise') }}
        </a-button>
      </a-popconfirm>
      <new-button v-if="hasPermission('system:organization:add')" @click="handleNew" />
    </template>

    <!--数据表格区域-->
    <template #bodyCell="{ column, record }">
      <!-- 组织名，根据查询条件高亮显示 -->
      <template v-if="column.key === 'name'">
        <span v-if="record.name.indexOf(searchName) > -1">
          {{ record.name.substring(0, record.name.indexOf(searchName)) }}
          <span style="color: #f50">{{ searchName }}</span>
          {{ record.name.substring(record.name.indexOf(searchName) + searchName.length) }}
        </span>
        <span v-else>{{ record.name }}</span>
      </template>
      <!-- 操作栏 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="hasPermission('system:organization:edit')" @click="handleEdit(record)">{{
            t('system.organization.edit')
          }}</a>
          <delete-text-button
            v-if="hasPermission('system:organization:del')"
            @confirm="handleDelete(record)"
          />
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 系统组织新建编辑的表单弹窗 -->
  <sys-organization-form-modal
    ref="fromModalRef"
    :organization-tree="organizationTree"
    @submit-success="reloadTable"
  />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import SysOrganizationFormModal from './SysOrganizationFormModal.vue'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { useAuthorize } from '@/hooks/permission'
import {
  listOrganizations,
  deleteOrganization,
  revisedOrganization
} from '@/api/system/organization'
import { listToTree, matchedParentKeys, pruneTree } from '@/utils/tree-utils'
import type { Key } from '@/utils/tree-utils'
import type { SysOrganizationTree, SysOrganizationVO } from '@/api/system/organization/types'
import { FormAction } from '@/hooks/form'
import { doRequest } from '@/utils/axios/request'
import { NewButton, DeleteTextButton } from '@/components/Button'
import OperationGroup from '@/components/Operation/OperationGroup.vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

defineOptions({ name: 'SysOrganizationPage' })

const { hasPermission } = useAuthorize()

const tableRef = ref<ProTableInstanceExpose>()
const fromModalRef = ref<InstanceType<typeof SysOrganizationFormModal>>()

const organizationTree = ref<SysOrganizationTree[]>([])

// 当前展开的节点 key
const expandedRowKeys = ref<Key[]>([])

const dataSource = reactive({
  data: { records: [] as SysOrganizationTree[] }
})

const searchName = ref('')

/* 组织名称的匹配逻辑 */
const nameMatcher = (node: SysOrganizationTree) => node.name.indexOf(searchName.value) > -1

watch(
  () => [organizationTree.value, searchName.value],
  () => {
    // 根据查询条件剪枝
    const tree = pruneTree(organizationTree.value, nameMatcher)
    dataSource.data.records = tree
    // 展开树
    if (searchName.value) {
      expandedRowKeys.value = matchedParentKeys(tree, nameMatcher)
    } else if (expandedRowKeys.value.length == 0) {
      expandedRowKeys.value = tree.map(x => x.id)
    }
  }
)

/* 远程加载表格数据 */
const tableRequest: TableRequest = async () => {
  return listOrganizations().then(res => {
    // 缓存下，重置的时候使用
    organizationTree.value = listToTree<SysOrganizationTree>(res.data as SysOrganizationTree[], 0)
    dataSource.data.records = organizationTree.value
    return dataSource
  })
}

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

/* 查询表格 */
const searchTable = () => {
  reloadTable(true) // 会调用 tableRequest
}

/* 新建组织 */
const handleNew = () => {
  fromModalRef.value?.open(FormAction.CREATE)
}

/* 编辑组织 */
const handleEdit = (record: SysOrganizationVO) => {
  fromModalRef.value?.open(FormAction.UPDATE, record)
}

/* 删除组织 */
const handleDelete = (record: SysOrganizationVO) => {
  doRequest(deleteOrganization(record.id), {
    successMessage: t('system.user.deleteSuccess'),
    onSuccess: () => reloadTable()
  })
}

/** 校正层级深度 */
const handleRevised = () => {
  doRequest(revisedOrganization(), {
    successMessage: t('system.organization.reviseSuccess'),
    onSuccess: () => reloadTable()
  })
}

const columns = computed<ProColumns[]>(() => [
  {
    title: t('system.organization.level'),
    width: 250,
    dataIndex: 'name'
  },
  {
    title: t('system.organization.sort'),
    width: 80,
    dataIndex: 'sort'
  },
  {
    title: t('system.organization.remarks'),
    dataIndex: 'remarks',
    width: 200,
    ellipsis: true
  },
  {
    title: t('system.organization.createTime'),
    dataIndex: 'createTime',
    width: 150
  },
  {
    key: 'operate',
    title: t('system.organization.operation'),
    align: 'center',
    width: 120
  }
])
</script>

<style scoped lang="less">
.expandIcon {
  margin-right: 8px;
}

.leafNode {
  padding-left: 14px;
}
</style>
