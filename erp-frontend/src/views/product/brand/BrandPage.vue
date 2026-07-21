<template>
  <!-- 查询表单 -->
  <brand-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="品牌管理"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1200 }"
    size="middle"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('product:brand:add')" @click="handleNew" />
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'logoUrl'">
        <div class="logo-cell">
          <a-image
            v-if="record.logoUrl"
            :src="fileAbsoluteUrl(record.logoUrl)"
            :width="32"
            :height="32"
            :preview="{ mask: '预览' }"
            style="border-radius: 4px; object-fit: cover"
          />
          <a-avatar v-else :size="32" style="background-color: #f5f5f5; color: #999">
            <template #icon>
              <picture-outlined />
            </template>
          </a-avatar>
        </div>
      </template>
      <template v-else-if="column.key === 'status'">
        <dict-tag dict-code="enable_status" :value="record.status"></dict-tag>
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a-button
            v-if="hasPermission('product:brand:edit')"
            type="link"
            size="small"
            @click="handleEdit(record)"
          >
            编辑
          </a-button>
          <delete-text-button
            v-if="hasPermission('product:brand:del')"
            size="small"
            @confirm="() => handleDelete(record)"
          />
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 系统配置新建修改的表单弹窗 -->
  <brand-form-modal ref="formModalRef" @submit-success="reloadTable" />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import type { ProTableInstanceExpose, TableRequest } from '#/table'
import BrandPageSearch from './BrandPageSearch.vue'
import BrandFormModal from './BrandFormModal.vue'
import { NewButton, DeleteTextButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useAuthorize } from '@/hooks/permission'
import { mergePageParam } from '@/utils/page-utils'
import { doRequest } from '@/utils/axios/request'
import { pageBrand, deleteBrand } from '@/api/product/brand'
import type { BrandPageVO, BrandQO } from '@/api/product/brand/types'
import { FormAction } from '@/hooks/form'
import { DictTag } from '@/components/Dict'
import { PictureOutlined } from '@ant-design/icons-vue'
import { fileAbsoluteUrl } from '@/utils/file-utils'

defineOptions({ name: 'BrandPage' })

// 鉴权方法
const { hasPermission } = useAuthorize()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const formModalRef = ref<InstanceType<typeof BrandFormModal>>()

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

// 查询参数
let searchParams: BrandQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageBrand({ ...pageParam, ...searchParams })
}

/* 查询品牌管理 */
const searchTable = (params: BrandQO) => {
  searchParams = params
  reloadTable(true) // 会调用 tableRequest
}

/* 新建品牌管理 */
const handleNew = () => {
  formModalRef.value?.open(FormAction.CREATE)
}

/* 编辑品牌管理 */
const handleEdit = (record: BrandPageVO) => {
  formModalRef.value?.open(FormAction.UPDATE, record)
}

/* 删除品牌管理 */
const handleDelete = (record: BrandPageVO) => {
  doRequest(deleteBrand(record.id), {
    successMessage: '删除成功！',
    onSuccess: () => reloadTable()
  })
}

const columns: ProColumns[] = [
  {
    title: '品牌名称',
    dataIndex: 'name',
    width: 120,
    fixed: 'left'
  },
  {
    title: '品牌编码',
    dataIndex: 'code',
    width: 120
  },
  {
    title: '品牌LOGO',
    dataIndex: 'logoUrl',
    key: 'logoUrl',
    width: 80,
    align: 'center'
  },
  {
    title: '原产国家/地区',
    dataIndex: 'originCountry',
    width: 140,
    ellipsis: true
  },
  {
    title: '品牌介绍',
    dataIndex: 'description',
    ellipsis: true,
    width: 200
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80,
    align: 'center'
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 160,
    sorter: true
  },
  {
    key: 'operate',
    title: '操作',
    align: 'center',
    width: 120,
    fixed: 'right'
  }
]
</script>

<style scoped>
.logo-cell {
  display: flex;
  justify-content: center;
  align-items: center;
}

:deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 600;
}
</style>
