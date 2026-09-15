<template>
  <!-- ① 搜索栏 -->
  <wms-operator-search :loading="tableRef?.loading" @search="searchTable" />

  <!-- ② 列表 -->
  <pro-table
    ref="tableRef"
    :header-title="t('platform.operator.title')"
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
      <!-- 服务商信息列 -->
      <template v-if="column.key === 'operatorInfo'">
        <div class="op-info">
          <div class="op-name">
            <a @click="handleDetail(record)">{{ record.tenantName }}</a>
          </div>
          <div class="op-code">{{ record.tenantCode }}</div>
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
          {{ record.status === 1 ? t('platform.operator.enabled') : t('platform.operator.disabled') }}
        </a-tag>
      </template>

      <!-- 操作列 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a @click="handleDetail(record)">{{ t('platform.operator.detail') }}</a>
          <a v-if="record.status === 1" class="danger-link" @click="handleToggle(record)">{{ t('platform.operator.disabled') }}</a>
          <a v-else @click="handleToggle(record)">{{ t('platform.operator.enabled') }}</a>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- ③ 开通弹窗 + 详情抽屉 -->
  <wms-operator-form-modal ref="formModalRef" @success="() => reloadTable(true)" />
  <wms-operator-detail-drawer ref="detailDrawerRef" />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { NewButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { mergePageParam } from '@/utils/page-utils'
import { isSuccess } from '@/api'
import { pageWmsOperators, setWmsOperatorStatus } from '@/api/tenant'
import type { TenantBrief, TenantPageParam } from '@/api/tenant/types'
import WmsOperatorSearch from './WmsOperatorSearch.vue'
import WmsOperatorFormModal from './WmsOperatorFormModal.vue'
import WmsOperatorDetailDrawer from './WmsOperatorDetailDrawer.vue'

const tableRef = ref<ProTableInstanceExpose>()
const { t } = useI18n()
const formModalRef = ref<InstanceType<typeof WmsOperatorFormModal>>()
const detailDrawerRef = ref<InstanceType<typeof WmsOperatorDetailDrawer>>()

let searchParams: TenantPageParam = {}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageWmsOperators({ ...pageParam, ...searchParams })
}

const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

const searchTable = (params: TenantPageParam) => {
  searchParams = params
  reloadTable(true)
}

const columns = computed<ProColumns[]>(() => [
  { title: t('platform.common.provider'), key: 'operatorInfo', width: 220, fixed: 'left' },
  { title: t('platform.operator.contact'), key: 'contact', width: 160 },
  { title: t('platform.common.status'), key: 'status', width: 90, align: 'center' },
  { title: t('platform.common.createdAt'), dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: t('platform.common.operation'), key: 'operate', width: 140, align: 'center', fixed: 'right' }
])

const handleNew = () => formModalRef.value?.openCreate()
const handleDetail = (record: TenantBrief) => detailDrawerRef.value?.openDetail(record)

const handleToggle = (record: TenantBrief) => {
  const toEnable = record.status !== 1
  Modal.confirm({
    title: toEnable ? t('platform.operator.enableTitle') : t('platform.operator.disableTitle'),
    content: toEnable
      ? t('platform.operator.enableConfirm', { name: record.tenantName })
      : t('platform.operator.disableConfirm', { name: record.tenantName }),
    okText: t('platform.operator.confirm'),
    cancelText: t('platform.common.cancel'),
    onOk: async () => {
      const res = await setWmsOperatorStatus(record.id, toEnable ? 1 : 0)
      if (isSuccess(res)) {
        message.success(toEnable ? t('platform.operator.enabledSuccess') : t('platform.operator.disabledSuccess'))
        reloadTable()
      } else {
        message.error(res.message || t('platform.operator.operationFailed'))
      }
    }
  })
}
</script>

<script lang="ts">
export default {
  name: 'WmsOperatorsPage'
}
</script>

<style scoped>
.op-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.op-name {
  font-weight: 600;
  color: #1890ff;
  cursor: pointer;
}
.op-code {
  font-size: 12px;
  color: #8c8c8c;
  font-family: 'SFMono-Regular', Consolas, 'Courier New', monospace;
}
.danger-link {
  color: #ff4d4f;
}
</style>
