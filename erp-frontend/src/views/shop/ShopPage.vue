<template>
  <!-- 查询表单 -->
  <ShopPageSearch :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="店铺管理"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1200 }"
  >
    <template #toolBarRender>
      <new-button v-if="hasPermission('system:shop:add')" @click="openCreate" />
    </template>
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'platform'">{{ getPlatformLabel(record.platform) }}</template>
      <template v-else-if="column.key === 'status'">
        <a-tag
          :color="
            record.status === ShopStatus.ENABLED
              ? 'green'
              : record.status === ShopStatus.ABNORMAL
                ? 'red'
                : 'default'
          "
        >
          {{ ShopStatusText[record.status as keyof typeof ShopStatusText] || record.status }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'lastTestStatus'">
        <a-tag
          :color="
            record.lastTestStatus === ShopTestStatus.SUCCESS
              ? 'green'
              : record.lastTestStatus === ShopTestStatus.FAIL
                ? 'red'
                : 'default'
          "
        >
          {{
            record.lastTestStatus !== undefined
              ? ShopTestStatusText[record.lastTestStatus as keyof typeof ShopTestStatusText] || '—'
              : '—'
          }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="hasPermission('system:shop:edit')" @click="handleEdit(record)">编辑</a>
          <a v-if="hasPermission('system:shop:edit')" @click="retest(record)">测试</a>
          <a v-if="hasPermission('system:shop:edit')" @click="toggleStatus(record)">{{
            record.status === 1 ? '禁用' : '启用'
          }}</a>
        </operation-group>
      </template>
    </template>
  </pro-table>
  <shop-form-modal ref="shopFormModalRef" @submit-success="reloadTable" />
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import type { ProTableInstanceExpose, TableRequest } from '#/table'
import { getPlatformLabel } from '@/components/Platform'
import ShopPageSearch from './ShopPageSearch.vue'
import { NewButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useAuthorize } from '@/hooks/permission'
import { mergePageParam } from '@/utils/page-utils'
import { listShops, toggleShopStatus, retestShop } from '@/api/shop'
import type { ShopVO } from '@/api/shop/types'
import { ShopStatus, ShopTestStatus, ShopStatusText, ShopTestStatusText } from '@/api/shop/types'
import { Modal } from 'ant-design-vue'
import ShopFormModal from './ShopFormModal.vue'
import { FormAction } from '@/hooks/form'

defineOptions({ name: 'ShopPage' })

// 鉴权方法
const { hasPermission } = useAuthorize()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const shopFormModalRef = ref<InstanceType<typeof ShopFormModal>>()

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

// 查询参数
let searchParams: { platform?: string; status?: number; keyword?: string } = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = async (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return listShops({ ...pageParam, ...searchParams })
}

/* 查询店铺（含凭证信息） */
const searchTable = (params: { platform?: string; status?: number; keyword?: string }) => {
  searchParams = { ...params }
  reloadTable(true) // 会调用 tableRequest
}

/* 新建店铺（含凭证信息） */
function openCreate() {
  shopFormModalRef.value?.open(FormAction.CREATE)
}
function handleEdit(record: ShopVO) {
  shopFormModalRef.value?.open(FormAction.UPDATE, record)
}
function toggleStatus(row: ShopVO) {
  Modal.confirm({
    title: row.status === 1 ? '确认禁用该店铺？' : '确认启用该店铺？',
    onOk: async () => {
      await toggleShopStatus(row.id, row.status === 1 ? 0 : 1)
      reloadTable()
    }
  })
}
function retest(row: ShopVO) {
  Modal.confirm({
    title: '重新测试凭证？',
    onOk: async () => {
      await retestShop(row.id)
      reloadTable()
    }
  })
}
// removed local modal logic; delegated to ShopFormModal

const columns: ProColumns[] = [
  { title: '#', dataIndex: 'id', width: 80 },
  { title: '平台', dataIndex: 'platform', key: 'platform', width: 120 },
  { title: 'ERP店铺名称', dataIndex: 'erpShopName', width: 180, ellipsis: true },
  { title: '平台店铺名称', dataIndex: 'name', width: 180, ellipsis: true },
  { title: '店铺ID', dataIndex: 'platformShopId', width: 180, ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '最近测试', dataIndex: 'lastTestedAt', width: 180 },
  { title: '测试状态', dataIndex: 'lastTestStatus', width: 120 },
  { key: 'operate', title: '操作', align: 'center', width: 160 }
]
</script>
