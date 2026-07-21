<template>
  <!-- 查询表单 -->
  <region-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="区域管理"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 800 }"
    size="middle"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <a-button v-if="hasPermission('wms:region:edit')" @click="handleMapping">
        <template #icon><setting-outlined /></template>
        平台映射
      </a-button>
      <new-button v-if="hasPermission('wms:region:add')" @click="handleNew">
        新增区域
      </new-button>
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <!-- 状态列 -->
      <template v-if="column.key === 'status'">
        <a-badge
          :status="record.status === 1 ? 'success' : 'default'"
          :text="record.status === 1 ? '启用' : '停用'"
        />
      </template>

      <!-- 操作列 -->
      <template v-else-if="column.key === 'operate'">
        <a-button
          v-if="hasPermission('wms:region:edit')"
          type="link"
          size="small"
          @click="handleEdit(record)"
        >
          编辑
        </a-button>
      </template>
    </template>
  </pro-table>

  <!-- 区域新建修改的表单弹窗 -->
  <region-form-modal ref="formModalRef" @submit-success="reloadTable" />

  <!-- 平台映射弹窗 -->
  <platform-mapping-modal ref="mappingModalRef" @change="reloadTable" />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import RegionPageSearch from './RegionPageSearch.vue'
import RegionFormModal from './RegionFormModal.vue'
import PlatformMappingModal from './PlatformMappingModal.vue'
import { NewButton } from '@/components/Button'
import { SettingOutlined } from '@ant-design/icons-vue'
import { useAuthorize } from '@/hooks/permission'
import { mergePageParam } from '@/utils/page-utils'
import { pageRegion } from '@/api/wms/region'
import type { RegionPageVO, RegionQO } from '@/api/wms/region/types'
import { FormAction } from '@/hooks/form'

defineOptions({ name: 'RegionPage' })

// 鉴权方法
const { hasPermission } = useAuthorize()

// 组件引用
const tableRef = ref<ProTableInstanceExpose>()
const formModalRef = ref<InstanceType<typeof RegionFormModal>>()
const mappingModalRef = ref<InstanceType<typeof PlatformMappingModal>>()

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

// 查询参数
let searchParams: RegionQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageRegion({ ...pageParam, ...searchParams })
}

/* 查询 */
const searchTable = (params: RegionQO) => {
  searchParams = params
  reloadTable(true)
}

/* 新建区域 */
const handleNew = () => {
  formModalRef.value?.open(FormAction.CREATE)
}

/* 编辑区域 */
const handleEdit = (record: RegionPageVO) => {
  formModalRef.value?.open(FormAction.UPDATE, record)
}

/* 配置平台映射 */
const handleMapping = () => {
  mappingModalRef.value?.open()
}

const columns: ProColumns[] = [
  {
    title: '区域编码',
    dataIndex: 'regionCode',
    width: 120,
    fixed: 'left'
  },
  {
    title: '区域名称',
    dataIndex: 'regionName',
    width: 140
  },
  {
    title: '仓库数量',
    dataIndex: 'warehouseCount',
    width: 100,
    align: 'center'
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
    width: 80,
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

.empty-text {
  color: #bfbfbf;
}
</style>
