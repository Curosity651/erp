<template>
  <!-- 查询表单 -->
  <manual-inbound-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="自定义入库单管理"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1000 }"
    size="middle"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('wms:manual-inbound:add')" @click="handleNew" />
      <a-button :loading="exportLoading" @click="handleExport">
        <download-outlined />
        导出
      </a-button>
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <!-- 入库单号列 -->
      <template v-if="column.key === 'inboundNo'">
        <div class="inbound-cell">
          <a class="inbound-no" @click="handleViewDetail(record)">{{ record.inboundNo }}</a>
          <span class="create-time">{{ record.createTime }}</span>
        </div>
      </template>

      <!-- 状态列 -->
      <template v-else-if="column.key === 'status'">
        <inbound-status-badge :status="record.orderStatus" />
      </template>

      <!-- 数量列：实到/未到仅在平台收货后(已收货/已完成)显示数字，否则显示「—」 -->
      <template v-else-if="column.key === 'quantity'">
        <div class="quantity-cell">
          <div class="qty-item">
            <span class="qty-value">{{ record.totalExpectedQty ?? 0 }}</span>
            <span class="qty-label">应到</span>
          </div>
          <span class="qty-divider">/</span>
          <div class="qty-item">
            <span class="qty-value">
              {{ showActualQty(record.orderStatus) ? (record.totalActualQty ?? 0) : '—' }}
            </span>
            <span class="qty-label">实到</span>
          </div>
          <span class="qty-divider">/</span>
          <div class="qty-item">
            <span
              class="qty-value"
              :class="{
                'qty-short': showActualQty(record.orderStatus) && (record.totalShortQty ?? 0) > 0
              }"
            >
              {{ showActualQty(record.orderStatus) ? (record.totalShortQty ?? 0) : '—' }}
            </span>
            <span class="qty-label">未到</span>
          </div>
        </div>
      </template>

      <!-- 操作列 -->
      <template v-else-if="column.key === 'operate'">
        <operation-group>
          <a @click="handleViewDetail(record)">查看</a>
          <a v-if="canEdit(record.orderStatus)" @click="handleEdit(record)">编辑</a>
          <template
            v-if="
              canSubmit(record.orderStatus) ||
              canCancel(record.orderStatus) ||
              canDelete(record.orderStatus)
            "
          >
            <a-dropdown>
              <a class="ant-dropdown-link" @click.prevent> 更多 </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item v-if="canSubmit(record.orderStatus)">
                    <confirm-text-button
                      title="确认要提交吗？提交后将流转至平台收货上架。"
                      text="提交"
                      :is-link="false"
                      @confirm="handleSubmit(record)"
                    />
                  </a-menu-item>
                  <a-menu-item v-if="canCancel(record.orderStatus)">
                    <confirm-text-button
                      title="确认要取消此入库单吗？"
                      text="取消入库"
                      :is-link="false"
                      danger
                      @confirm="handleCancel(record)"
                    />
                  </a-menu-item>
                  <a-menu-item v-if="canDelete(record.orderStatus)">
                    <delete-text-button :is-link="false" @confirm="handleDelete(record)" />
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
        </operation-group>
      </template>
    </template>
  </pro-table>

  <!-- 入库单详情抽屉 -->
  <manual-inbound-detail-drawer ref="detailDrawerRef" @update-success="reloadTable" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import ManualInboundPageSearch from './ManualInboundPageSearch.vue'
import ManualInboundDetailDrawer from './ManualInboundDetailDrawer.vue'
import { NewButton, DeleteTextButton, ConfirmTextButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import InboundStatusBadge from '@/views/wms/purchase-inbound/components/InboundStatusBadge.vue'
import { useAuthorize } from '@/hooks/permission'
import { useInboundRecordPermission } from '@/views/wms/purchase-inbound/hooks/useInboundPermission'
import { mergePageParam } from '@/utils/page-utils'
import { doRequest } from '@/utils/axios/request'
import { remoteFileDownload } from '@/utils/file-utils'
import { emitter } from '@/hooks/mitt'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'
import {
  pageManualInbound,
  submitManualInbound,
  cancelManualInbound,
  deleteManualInbound,
  exportManualInbound
} from '@/api/wms/manual-inbound'
import type { ManualInboundQO } from '@/api/wms/manual-inbound/types'
import type { PurchaseInboundPageVO } from '@/api/wms/purchase-inbound/types'
import { InboundStatus } from '@/api/wms/purchase-inbound/types'

defineOptions({ name: 'ManualInboundPage' })

// 实到/未到仅在平台收货后(已收货/已完成)才有真实数据
const showActualQty = (status: string): boolean =>
  status === InboundStatus.RECEIVED || status === InboundStatus.COMPLETED

const router = useRouter()
const { hasPermission } = useAuthorize()

const tableRef = ref<ProTableInstanceExpose>()
const detailDrawerRef = ref<InstanceType<typeof ManualInboundDetailDrawer>>()
const exportLoading = ref(false)

// ==================== 权限判断 ====================

const { canEdit, canSubmit, canCancel, canDelete } =
  useInboundRecordPermission('wms:manual-inbound')

// ==================== 表格配置 ====================

const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

useTableActivateReload(() => reloadTable(false))

let searchParams: ManualInboundQO = {}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageManualInbound({ ...pageParam, ...searchParams })
}

const searchTable = (params: ManualInboundQO) => {
  searchParams = params
  reloadTable(true)
}

// ==================== 列定义 ====================

const columns: ProColumns[] = [
  {
    title: '入库单号',
    key: 'inboundNo',
    width: 180,
    fixed: 'left'
  },
  {
    title: '状态',
    key: 'status',
    width: 100
  },
  {
    title: '入库仓库',
    dataIndex: 'warehouseName',
    width: 140
  },
  {
    title: '数量',
    key: 'quantity',
    width: 160
  },
  {
    title: '入库日期',
    dataIndex: 'inboundDate',
    width: 120
  },
  {
    key: 'operate',
    title: '操作',
    align: 'center',
    width: 160,
    fixed: 'right'
  }
]

// ==================== 操作方法 ====================

const handleNew = () => {
  router.push('/wms/manual-inbound/form/create')
}

const handleEdit = (record: PurchaseInboundPageVO) => {
  router.push(`/wms/manual-inbound/form/edit/${record.id}`)
}

const handleViewDetail = (record: PurchaseInboundPageVO) => {
  detailDrawerRef.value?.open(record.id)
}

const handleSubmit = (record: PurchaseInboundPageVO) => {
  doRequest(submitManualInbound(record.id), {
    successMessage: '提交成功',
    onSuccess: () => reloadTable()
  })
}

const handleCancel = (record: PurchaseInboundPageVO) => {
  doRequest(cancelManualInbound(record.id), {
    successMessage: '取消成功',
    onSuccess: () => reloadTable()
  })
}

const handleDelete = (record: PurchaseInboundPageVO) => {
  doRequest(deleteManualInbound([record.id]), {
    successMessage: '删除成功',
    onSuccess: () => reloadTable(true)
  })
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const response = await exportManualInbound(searchParams)
    remoteFileDownload(response)
    message.success('导出成功')
  } catch (e) {
    message.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

// ==================== 事件监听 ====================

onMounted(() => {
  emitter.on('refresh-manual-inbound-list', () => reloadTable())
})

onUnmounted(() => {
  emitter.off('refresh-manual-inbound-list')
})
</script>

<style scoped>
/* ==================== 入库单号列 ==================== */
.inbound-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.inbound-no {
  font-weight: 500;
  font-family: 'SFMono-Regular', Consolas, 'Courier New', monospace;
  font-size: 14px;
  color: #1677ff;
}

.create-time {
  font-size: 12px;
  color: #8c8c8c;
}

/* ==================== 数量列 ==================== */
.quantity-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qty-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 36px;
}

.qty-value {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  line-height: 1.2;
}

.qty-value.qty-short {
  color: #f5222d;
}

.qty-label {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 2px;
}

.qty-divider {
  color: #d9d9d9;
  font-size: 14px;
}
</style>
