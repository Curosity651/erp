<template>
  <!-- 查询表单 -->
  <sku-mapping-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="SKU映射"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1000 }"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('product:sku-mapping:add')" @click="handleNew" />
      <a-button
        v-if="hasPermission('product:sku-mapping:read')"
        type="default"
        @click="handleExport"
        >导出</a-button
      >
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'operate'">
        <operation-group>
          <a v-if="hasPermission('product:sku-mapping:edit')" @click="handleEdit(record)">编辑</a>
          <delete-text-button
            v-if="hasPermission('product:sku-mapping:del')"
            @confirm="() => handleDelete(record)"
          />
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 系统配置新建修改的表单弹窗 -->
  <sku-mapping-form-modal ref="formModalRef" @submit-success="reloadTable" />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import type { ProTableInstanceExpose, TableRequest } from '#/table'
import SkuMappingPageSearch from './SkuMappingPageSearch.vue'
import SkuMappingFormModal from './SkuMappingFormModal.vue'
import { NewButton, DeleteTextButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useAuthorize } from '@/hooks/permission'
import { mergePageParam } from '@/utils/page-utils'
import { doRequest } from '@/utils/axios/request'
import { pageSkuMapping, deleteSkuMapping, exportSkuMapping } from '@/api/product/sku-mapping'
import type { SkuMappingPageVO, SkuMappingQO } from '@/api/product/sku-mapping/types'
import { FormAction } from '@/hooks/form'

defineOptions({ name: 'SkuMappingPage' })

// 鉴权方法
const { hasPermission } = useAuthorize()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const formModalRef = ref<InstanceType<typeof SkuMappingFormModal>>()

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

// 查询参数
let searchParams: SkuMappingQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageSkuMapping({ ...pageParam, ...searchParams })
}

/* 查询SKU映射 */
const searchTable = (params: SkuMappingQO) => {
  searchParams = params
  reloadTable(true) // 会调用 tableRequest
}

/* 新建SKU映射 */
const handleNew = () => {
  formModalRef.value?.open(FormAction.CREATE)
}

/* 编辑SKU映射 */
const handleEdit = (record: SkuMappingPageVO) => {
  formModalRef.value?.open(FormAction.UPDATE, record)
}

/* 删除SKU映射 */
const handleDelete = (record: SkuMappingPageVO) => {
  doRequest(deleteSkuMapping(record.id), {
    successMessage: '删除成功！',
    onSuccess: () => reloadTable()
  })
}

/* 导出 */
const handleExport = () => {
  exportSkuMapping(searchParams)
}

const columns: ProColumns[] = [
  {
    title: '#',
    dataIndex: 'id'
  },
  {
    title: '平台商品ID',
    dataIndex: 'platformItemId'
  },
  {
    title: 'ERP SKU 编码',
    dataIndex: 'skuCode'
  },
  {
    title: 'SKU中文名',
    dataIndex: 'skuChineseName'
  },
  {
    title: '品类',
    dataIndex: 'categoryName'
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
