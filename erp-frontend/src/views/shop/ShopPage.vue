<template>
  <!-- 查询表单 -->
  <ShopPageSearch :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    :header-title="t('shop.pageTitle')"
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
          {{ getShopStatusText(record.status) }}
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
          {{ record.lastTestStatus !== undefined ? getTestStatusText(record.lastTestStatus) : '—' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a v-if="hasPermission('system:shop:edit')" @click="handleEdit(record)">{{
            t('action.edit')
          }}</a>
          <a v-if="hasPermission('system:shop:edit')" @click="retest(record)">{{
            t('shop.test')
          }}</a>
          <a v-if="hasPermission('system:shop:edit')" @click="toggleStatus(record)">{{
            record.status === 1 ? t('shop.disable') : t('shop.enable')
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
import { ShopStatus, ShopTestStatus } from '@/api/shop/types'
import { Modal } from 'ant-design-vue'
import ShopFormModal from './ShopFormModal.vue'
import { FormAction } from '@/hooks/form'
import { useI18n } from 'vue-i18n'

defineOptions({ name: 'ShopPage' })

// 鉴权方法
const { hasPermission } = useAuthorize()
const { t } = useI18n()

const getShopStatusText = (status: number) => t(`shop.status.${status}`, String(status))
const getTestStatusText = (status: number) => t(`shop.testStatus.${status}`, '—')

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
    title: row.status === 1 ? t('shop.confirmDisable') : t('shop.confirmEnable'),
    onOk: async () => {
      await toggleShopStatus(row.id, row.status === 1 ? 0 : 1)
      reloadTable()
    }
  })
}
function retest(row: ShopVO) {
  Modal.confirm({
    title: t('shop.confirmRetest'),
    onOk: async () => {
      await retestShop(row.id)
      reloadTable()
    }
  })
}
// removed local modal logic; delegated to ShopFormModal

const columns = computed<ProColumns[]>(() => [
  { title: '#', dataIndex: 'id', width: 80 },
  { title: t('shop.platform'), dataIndex: 'platform', key: 'platform', width: 120 },
  { title: t('shop.erpShopName'), dataIndex: 'erpShopName', width: 180, ellipsis: true },
  { title: t('shop.platformShopName'), dataIndex: 'name', width: 180, ellipsis: true },
  { title: t('shop.shopId'), dataIndex: 'platformShopId', width: 180, ellipsis: true },
  { title: t('shop.statusLabel'), dataIndex: 'status', key: 'status', width: 100 },
  { title: t('shop.lastTest'), dataIndex: 'lastTestedAt', width: 180 },
  { title: t('shop.testStatusLabel'), dataIndex: 'lastTestStatus', width: 120 },
  { key: 'operate', title: t('common.operation'), align: 'center', width: 160 }
])
</script>
