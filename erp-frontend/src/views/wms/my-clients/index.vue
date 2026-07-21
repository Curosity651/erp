<template>
  <!-- ① 搜索栏 -->
  <my-clients-search :loading="tableRef?.loading" @search="searchTable" />

  <!-- ② 列表 -->
  <pro-table
    ref="tableRef"
    header-title="货主管理"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 760 }"
    size="middle"
  >
    <template #toolBarRender>
      <new-button @click="handleNew" />
    </template>

    <template #bodyCell="{ column, record }">
      <!-- 货主信息列 -->
      <template v-if="column.key === 'clientInfo'">
        <div class="client-info">
          <div class="client-name">
            <a @click="handleDetail(record)">{{ record.tenantName }}</a>
          </div>
          <div class="client-code">{{ record.tenantCode }}</div>
        </div>
      </template>

      <!-- 联系方式列 -->
      <template v-else-if="column.key === 'contact'">
        <div>{{ record.contactName || '-' }}</div>
        <div style="color: #8c8c8c; font-size: 12px">{{ record.contactPhone || '-' }}</div>
      </template>

      <!-- 状态列 -->
      <template v-else-if="column.key === 'status'">
        <a-tag :color="record.status === 1 ? 'green' : 'red'">
          {{ record.status === 1 ? '启用' : '停用' }}
        </a-tag>
      </template>

      <!-- 操作列 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a @click="handleDetail(record)">详情</a>
          <a v-if="record.status === 1" class="danger-link" @click="handleToggle(record)">停用</a>
          <a v-else @click="handleToggle(record)">启用</a>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- ③ 开通弹窗 + 详情抽屉 -->
  <client-form-modal ref="formModalRef" @success="() => reloadTable(true)" />
  <client-detail-drawer ref="detailDrawerRef" />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { NewButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { mergePageParam } from '@/utils/page-utils'
import { isSuccess } from '@/api'
import { pageErpTenants, setErpTenantStatus } from '@/api/tenant'
import type { TenantBrief, TenantPageParam } from '@/api/tenant/types'
import MyClientsSearch from './MyClientsSearch.vue'
import ClientFormModal from './ClientFormModal.vue'
import ClientDetailDrawer from './ClientDetailDrawer.vue'

const tableRef = ref<ProTableInstanceExpose>()
const formModalRef = ref<InstanceType<typeof ClientFormModal>>()
const detailDrawerRef = ref<InstanceType<typeof ClientDetailDrawer>>()

let searchParams: TenantPageParam = {}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageErpTenants({ ...pageParam, ...searchParams })
}

const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

const searchTable = (params: TenantPageParam) => {
  searchParams = params
  reloadTable(true)
}

const columns: ProColumns[] = [
  { title: '货主', key: 'clientInfo', width: 220, fixed: 'left' },
  { title: '联系方式', key: 'contact', width: 160 },
  { title: '状态', key: 'status', width: 90, align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'operate', width: 140, align: 'center', fixed: 'right' }
]

const handleNew = () => formModalRef.value?.openCreate()
const handleDetail = (record: TenantBrief) => detailDrawerRef.value?.openDetail(record)

const handleToggle = (record: TenantBrief) => {
  const toEnable = record.status !== 1
  Modal.confirm({
    title: toEnable ? '启用货主' : '停用货主',
    content: toEnable
      ? `确定启用「${record.tenantName}」吗？`
      : `确定停用「${record.tenantName}」吗？停用后该货主管理员及其成员将无法登录。`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      const res = await setErpTenantStatus(record.id, toEnable ? 1 : 0)
      if (isSuccess(res)) {
        message.success(toEnable ? '已启用' : '已停用')
        reloadTable()
      } else {
        message.error(res.message || '操作失败')
      }
    }
  })
}
</script>

<script lang="ts">
export default {
  name: 'MyClientsPage'
}
</script>

<style scoped>
.client-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.client-name {
  font-weight: 600;
  color: #1890ff;
  cursor: pointer;
}
.client-code {
  font-size: 12px;
  color: #8c8c8c;
  font-family: 'SFMono-Regular', Consolas, 'Courier New', monospace;
}
.danger-link {
  color: #ff4d4f;
}
</style>
