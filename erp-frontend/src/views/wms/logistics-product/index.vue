<template>
  <!-- ① 搜索栏 -->
  <a-card :bordered="false" style="margin-bottom: 16px" :body-style="{ paddingBottom: 0 }">
    <a-form :model="searchModel" :label-col="{ style: { width: '80px' } }">
      <a-row :gutter="[16, 8]">
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="产品名称">
            <a-input v-model:value="searchModel.keyword" placeholder="名称/编码" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <a-form-item label="状态">
            <a-select v-model:value="searchModel.status" placeholder="全部" allow-clear>
              <a-select-option :value="1">启用</a-select-option>
              <a-select-option :value="0">停用</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :xl="6" :lg="8" :md="12" :sm="24">
          <search-actions :loading="tableRef?.loading" @search="searchTable" @reset="resetSearch" />
        </a-col>
      </a-row>
    </a-form>
  </a-card>

  <!-- ② 列表 -->
  <pro-table
    ref="tableRef"
    header-title="物流产品"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 860 }"
    size="middle"
  >
    <template #toolBarRender>
      <a-button type="primary" @click="handleNew">
        <plus-outlined />
        新建产品
      </a-button>
    </template>

    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'tags'">
        <a-tag v-for="t in record.tags" :key="t" :color="tagColor(t)">{{ t }}</a-tag>
        <span v-if="!record.tags || record.tags.length === 0" style="color: rgba(0, 0, 0, 0.25)"
          >—</span
        >
      </template>
      <template v-else-if="column.key === 'unitPrice'">
        <span class="price">{{ record.currency || 'RUB' }} {{ formatMoney(record.unitPrice) }}</span>
        <span class="price-per"> / 次</span>
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag :color="record.status === 1 ? 'green' : 'red'">
          {{ record.status === 1 ? '启用' : '停用' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a @click="handleEdit(record)">编辑</a>
          <a @click="toggleStatus(record)">{{ record.status === 1 ? '停用' : '启用' }}</a>
          <a-popconfirm
            title="删除后货主将无法再选用该产品，确认删除？"
            @confirm="handleDelete(record)"
          >
            <a style="color: #ff4d4f">删除</a>
          </a-popconfirm>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- ③ 新建/编辑弹窗 -->
  <product-form-modal ref="formModalRef" @success="reloadTable" />
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { OperationGroup } from '@/components/Operation'
import { SearchActions } from '@/components/Search'
import { mergePageParam } from '@/utils/page-utils'
import { doRequest } from '@/utils/axios/request'
import {
  pageLogisticsProducts,
  updateLogisticsProductStatus,
  deleteLogisticsProduct
} from '@/api/wms/logistics-product'
import type { LogisticsProductVO } from '@/api/wms/logistics-product/types'
import ProductFormModal from './ProductFormModal.vue'

const tableRef = ref<ProTableInstanceExpose>()
const formModalRef = ref<InstanceType<typeof ProductFormModal>>()

const searchModel = reactive<{ keyword?: string; status?: number }>({})
let searchParams: { keyword?: string; status?: number } = {}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageLogisticsProducts(pageParam, searchParams.keyword, searchParams.status)
}
const reloadTable = (resetPageIndex?: boolean) => tableRef.value?.actionRef?.reload(resetPageIndex)
const searchTable = () => {
  searchParams = { ...searchModel }
  reloadTable(true)
}
const resetSearch = () => {
  searchModel.keyword = undefined
  searchModel.status = undefined
  searchTable()
}

const columns: ProColumns[] = [
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 180, fixed: 'left' },
  { title: '编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '特性词条', key: 'tags', width: 220 },
  { title: '单价', key: 'unitPrice', width: 130, align: 'right' },
  { title: '产品说明', dataIndex: 'productDescription', key: 'productDescription', width: 260, ellipsis: true },
  { title: '状态', key: 'status', width: 90, align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'operate', width: 150, align: 'center', fixed: 'right' }
]

// 词条按内容稳定配色
const TAG_COLORS = ['blue', 'green', 'orange', 'purple', 'cyan', 'magenta', 'geekblue', 'volcano']
const tagColor = (tag: string) => {
  let h = 0
  for (let i = 0; i < tag.length; i++) h = (h * 31 + tag.charCodeAt(i)) % 997
  return TAG_COLORS[h % TAG_COLORS.length]
}

const formatMoney = (v?: number) =>
  v == null
    ? '0.00'
    : Number(v).toLocaleString('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const handleNew = () => formModalRef.value?.open()
const handleEdit = (record: LogisticsProductVO) => formModalRef.value?.open(record)

const toggleStatus = (record: LogisticsProductVO) => {
  doRequest(updateLogisticsProductStatus(record.id, record.status === 1 ? 0 : 1), {
    successMessage: record.status === 1 ? '已停用' : '已启用',
    onSuccess: () => reloadTable()
  })
}
const handleDelete = (record: LogisticsProductVO) => {
  doRequest(deleteLogisticsProduct(record.id), {
    successMessage: '删除成功',
    onSuccess: () => reloadTable()
  })
}
</script>

<script lang="ts">
export default {
  name: 'LogisticsProductPage'
}
</script>

<style scoped>
.price {
  font-weight: 600;
}
.price-per {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
</style>
