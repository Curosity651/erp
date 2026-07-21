<template>
  <!-- 查询表单 -->
  <category-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    v-model:expanded-row-keys="expandedRowKeys"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1000 }"
    :pagination="false"
  >
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
      <new-button v-if="hasPermission('product:category:add')" @click="handleNew">
        新建顶级品类
      </new-button>
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <!-- 品类名称，根据查询条件高亮显示 -->
      <template v-if="column.dataIndex === 'name'">
        <span
          v-if="searchParams.name && record.name && record.name.indexOf(searchParams.name) > -1"
        >
          {{ record.name.substring(0, record.name.indexOf(searchParams.name)) }}
          <span style="color: #fa8c16; background-color: #fff7e6; padding: 0 2px">{{
            searchParams.name
          }}</span>
          {{
            record.name.substring(record.name.indexOf(searchParams.name) + searchParams.name.length)
          }}
        </span>
        <span v-else>{{ record.name || '' }}</span>
      </template>
      <!-- 品类编码，根据查询条件高亮显示 -->
      <template v-else-if="column.dataIndex === 'code'">
        <span
          v-if="searchParams.code && record.code && record.code.indexOf(searchParams.code) > -1"
        >
          {{ record.code.substring(0, record.code.indexOf(searchParams.code)) }}
          <span style="color: #fa8c16; background-color: #fff7e6; padding: 0 2px">{{
            searchParams.code
          }}</span>
          {{
            record.code.substring(record.code.indexOf(searchParams.code) + searchParams.code.length)
          }}
        </span>
        <span v-else>{{ record.code || '' }}</span>
      </template>
      <template v-else-if="column.dataIndex === 'level'">
        <a-tag :color="getLevelColor(record.level)">
          {{ getLevelText(record.level) }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'status'">
        <dict-tag dict-code="enable_status" :value="record.status"></dict-tag>
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a
            v-if="hasPermission('product:category:add') && (record.level || 1) < 4"
            style="color: #1890ff"
            @click="handleAddChild(record)"
          >
            添加子品类
          </a>
          <a v-if="hasPermission('product:category:edit')" @click="handleEdit(record)">编辑</a>
          <delete-text-button
            v-if="hasPermission('product:category:del')"
            @confirm="() => handleDelete(record)"
          />
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 系统配置新建修改的表单弹窗 -->
  <category-form-modal ref="formModalRef" @submit-success="reloadTable" />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import type { ProTableInstanceExpose, TableRequest } from '#/table'
import CategoryPageSearch from './CategoryPageSearch.vue'
import CategoryFormModal from './CategoryFormModal.vue'
import { NewButton, DeleteTextButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useAuthorize } from '@/hooks/permission'
import { doRequest } from '@/utils/axios/request'
import { listCategory, deleteCategory } from '@/api/product/category'
import type { CategoryPageVO, CategoryQO } from '@/api/product/category/types'

// 扩展CategoryPageVO以支持树形结构
interface CategoryTreeVO extends CategoryPageVO {
  children?: CategoryTreeVO[]
}
import { FormAction } from '@/hooks/form'
import { DictTag } from '@/components/Dict'
import { CaretDownOutlined, CaretRightOutlined } from '@ant-design/icons-vue'
import type { Key } from '@/utils/tree-utils'

defineOptions({ name: 'CategoryPage' })

// 鉴权方法
const { hasPermission } = useAuthorize()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const formModalRef = ref<InstanceType<typeof CategoryFormModal>>()

// 当前展开的节点 key
const expandedRowKeys = ref<Key[]>([])

// 缓存的树形数据
const categoryTree = ref<CategoryTreeVO[]>([])

const dataSource = reactive({
  data: { records: [] as CategoryTreeVO[] }
})

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

// 查询参数
let searchParams: CategoryQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = async () => {
  // 使用专门的列表接口获取所有数据
  const response = await listCategory(searchParams)

  // 将扁平数据转换为树形结构
  const buildTree = (items: CategoryPageVO[], parentId = 0): CategoryTreeVO[] => {
    return items
      .filter(item => (item.parentId || 0) === parentId)
      .sort((a, b) => (a.sort || 0) - (b.sort || 0))
      .map(item => ({
        ...item,
        children: buildTree(items, item.id)
      }))
  }

  const treeData = buildTree(response.data || [])

  // 缓存树形数据
  categoryTree.value = treeData

  // 根据搜索条件过滤数据
  const filteredData = filterTreeData(treeData, searchParams)
  dataSource.data.records = filteredData

  // 如果有搜索条件，展开所有匹配的节点（status 可能为 0，需显式判空）
  if (
    searchParams.name ||
    searchParams.code ||
    (searchParams.status !== undefined && searchParams.status !== null && (searchParams.status as unknown) !== '')
  ) {
    expandedRowKeys.value = getExpandedKeys(filteredData, searchParams)
  } else if (expandedRowKeys.value.length === 0) {
    // 默认展开第一级
    expandedRowKeys.value = treeData.map(x => x.id)
  }

  return dataSource
}

/* 查询商品品类 */
const searchTable = (params: CategoryQO) => {
  searchParams = params
  reloadTable(true) // 会调用 tableRequest
}

/* 根据搜索条件过滤树形数据 */
const filterTreeData = (tree: CategoryTreeVO[], searchParams: CategoryQO): CategoryTreeVO[] => {
  // status 可能为 0（停用），不能用 !status 判断是否启用了状态筛选
  const statusActive =
    searchParams.status !== undefined && searchParams.status !== null && (searchParams.status as unknown) !== ''
  if (!searchParams.name && !searchParams.code && !statusActive) {
    return tree
  }

  const filterNode = (node: CategoryTreeVO): CategoryTreeVO | null => {
    const matchesSearch =
      (!searchParams.name || (node.name && node.name.includes(searchParams.name))) &&
      (!searchParams.code || (node.code && node.code.includes(searchParams.code))) &&
      (!statusActive || node.status === searchParams.status)

    const filteredChildren = node.children
      ? (node.children.map(filterNode).filter(Boolean) as CategoryTreeVO[])
      : []

    if (matchesSearch || filteredChildren.length > 0) {
      return {
        ...node,
        children: filteredChildren
      }
    }

    return null
  }

  return tree.map(filterNode).filter(Boolean) as CategoryTreeVO[]
}

/* 获取所有匹配节点的父节点keys */
const getExpandedKeys = (tree: CategoryTreeVO[], searchParams: CategoryQO): Key[] => {
  const keys: Key[] = []

  const traverse = (nodes: CategoryTreeVO[]) => {
    nodes.forEach(node => {
      if (node.children && node.children.length > 0) {
        keys.push(node.id)
        traverse(node.children)
      }
    })
  }

  const statusActive =
    searchParams.status !== undefined && searchParams.status !== null && (searchParams.status as unknown) !== ''
  if (searchParams.name || searchParams.code || statusActive) {
    traverse(tree)
  }

  return keys
}

/* 新建商品品类 */
const handleNew = () => {
  formModalRef.value?.open(FormAction.CREATE)
}

/* 添加子品类 */
const handleAddChild = (record: CategoryTreeVO) => {
  formModalRef.value?.open(FormAction.CREATE, undefined, record)
}

/* 编辑商品品类 */
const handleEdit = (record: CategoryTreeVO) => {
  formModalRef.value?.open(FormAction.UPDATE, record)
}

/* 删除商品品类 */
const handleDelete = (record: CategoryTreeVO) => {
  doRequest(deleteCategory(record.id), {
    successMessage: '删除成功！',
    onSuccess: () => reloadTable()
  })
}

/* 获取层级颜色 */
const getLevelColor = (level: number) => {
  const colors = ['blue', 'orange', 'green', 'red', 'purple']
  return colors[(level - 1) % colors.length]
}

/* 获取层级文本 */
const getLevelText = (level: number) => {
  return `${level}级`
}

const columns: ProColumns[] = [
  {
    title: '品类名称',
    dataIndex: 'name',
    width: 250
  },
  {
    title: '品类编码',
    dataIndex: 'code',
    width: 150
  },
  {
    title: '层级',
    dataIndex: 'level',
    width: 80,
    align: 'center'
  },
  {
    title: '排序',
    dataIndex: 'sort',
    width: 80,
    align: 'center'
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100,
    align: 'center'
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 150
  },
  {
    key: 'operate',
    title: '操作',
    align: 'center',
    width: 160
  }
]
</script>

<style scoped lang="less">
.expandIcon {
  margin-right: 8px;
}

.leafNode {
  padding-left: 14px;
}
</style>
