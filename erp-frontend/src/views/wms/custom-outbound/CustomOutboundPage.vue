<template>
  <!-- 查询表单 -->
  <custom-outbound-page-search :loading="tableRef?.loading" @search="searchTable" />

  <pro-table
    ref="tableRef"
    header-title="自定义出库单管理"
    row-key="id"
    :request="tableRequest"
    :columns="columns"
    :scroll="{ x: 1100 }"
    size="middle"
  >
    <!-- 操作按钮区域 -->
    <template #toolBarRender>
      <new-button v-if="hasPermission('wms:custom-outbound:add')" @click="handleNew" />
      <a-button :loading="exportLoading" @click="handleExport">
        <download-outlined />
        导出
      </a-button>
    </template>

    <!-- 数据表格区域 -->
    <template #bodyCell="{ column, record }">
      <!-- 出库单号列 -->
      <template v-if="column.key === 'outboundNo'">
        <div class="outbound-cell">
          <a class="outbound-no" @click="handleViewDetail(record)">{{ record.outboundNo }}</a>
          <span class="create-time">{{ record.createTime }}</span>
        </div>
      </template>

      <!-- 出库类型列 -->
      <template v-else-if="column.key === 'customType'">
        <a-tag :color="CustomOutboundTypeColorMap[record.customType] || 'default'">
          {{ CustomOutboundTypeMap[record.customType] || record.customType }}
        </a-tag>
      </template>

      <!-- 状态列 -->
      <template v-else-if="column.key === 'status'">
        <custom-outbound-status-badge :status="record.orderStatus" />
      </template>

      <!-- 出库内容列：只展示系统有真实来源的数据，不用应出数量伪造实出/差异。 -->
      <template v-else-if="column.key === 'quantity'">
        <div class="quantity-cell">
          <div class="qty-item">
            <span class="qty-value">{{ record.skuCount ?? 0 }}</span>
            <span class="qty-label">SKU</span>
          </div>
          <span class="qty-divider">/</span>
          <div class="qty-item">
            <span class="qty-value">{{ record.totalQuantity ?? 0 }}</span>
            <span class="qty-label">件数</span>
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
                      title="确认要提交吗？提交后将按可售库存校验并预占，流转至海外仓作业台。"
                      text="提交"
                      :is-link="false"
                      @confirm="handleSubmit(record)"
                    />
                  </a-menu-item>
                  <a-menu-item v-if="canCancel(record.orderStatus)">
                    <confirm-text-button
                      title="确认要取消此出库单吗？已提交的单据将释放库存预占。"
                      text="取消出库"
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

  <!-- 出库单详情抽屉 -->
  <custom-outbound-detail-drawer ref="detailDrawerRef" @update-success="reloadTable" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import CustomOutboundPageSearch from './CustomOutboundPageSearch.vue'
import CustomOutboundDetailDrawer from './CustomOutboundDetailDrawer.vue'
import CustomOutboundStatusBadge from './components/CustomOutboundStatusBadge.vue'
import { NewButton, DeleteTextButton, ConfirmTextButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useAuthorize } from '@/hooks/permission'
import { mergePageParam } from '@/utils/page-utils'
import { doRequest } from '@/utils/axios/request'
import { remoteFileDownload } from '@/utils/file-utils'
import { emitter } from '@/hooks/mitt'
import { useTableActivateReload } from '@/hooks/useTableActivateReload'
import { isSuccess } from '@/api'
import {
  pageCustomOutbound,
  submitCustomOutbound,
  cancelCustomOutbound,
  deleteCustomOutbound,
  exportCustomOutbound
} from '@/api/wms/custom-outbound'
import type { CustomOutboundQO, CustomOutboundPageVO } from '@/api/wms/custom-outbound/types'
import {
  CustomOutboundStatus,
  CustomOutboundTypeMap,
  CustomOutboundTypeColorMap
} from '@/api/wms/custom-outbound/types'

defineOptions({ name: 'CustomOutboundPage' })

const router = useRouter()
const { hasPermission } = useAuthorize()

const tableRef = ref<ProTableInstanceExpose>()
const detailDrawerRef = ref<InstanceType<typeof CustomOutboundDetailDrawer>>()
const exportLoading = ref(false)

// ==================== 权限/状态判断 ====================

// 编辑/提交/删除仅草稿；取消支持 草稿 + 已提交（未开始下架，释放预占）
const canEdit = (status: string) =>
  hasPermission('wms:custom-outbound:edit') && status === CustomOutboundStatus.DRAFT
const canSubmit = (status: string) =>
  hasPermission('wms:custom-outbound:edit') && status === CustomOutboundStatus.DRAFT
const canCancel = (status: string) =>
  hasPermission('wms:custom-outbound:edit') &&
  (status === CustomOutboundStatus.DRAFT || status === CustomOutboundStatus.CONFIRMED)
const canDelete = (status: string) =>
  hasPermission('wms:custom-outbound:del') && status === CustomOutboundStatus.DRAFT

// ==================== 表格配置 ====================

const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

useTableActivateReload(() => reloadTable(false))

let searchParams: CustomOutboundQO = {}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageCustomOutbound({ ...pageParam, ...searchParams })
}

const searchTable = (params: CustomOutboundQO) => {
  searchParams = params
  reloadTable(true)
}

// ==================== 列定义 ====================

const columns: ProColumns[] = [
  {
    title: '出库单号',
    key: 'outboundNo',
    width: 180,
    fixed: 'left'
  },
  {
    title: '状态',
    key: 'status',
    width: 100
  },
  {
    title: '出库类型',
    key: 'customType',
    width: 110
  },
  {
    title: '出库仓库',
    dataIndex: 'warehouseName',
    width: 140
  },
  {
    title: '出库内容',
    key: 'quantity',
    width: 170
  },
  {
    title: '出库日期',
    dataIndex: 'outboundDate',
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
  router.push('/wms/custom-outbound/form/create')
}

const handleEdit = (record: CustomOutboundPageVO) => {
  router.push(`/wms/custom-outbound/form/edit/${record.id}`)
}

const handleViewDetail = (record: CustomOutboundPageVO) => {
  detailDrawerRef.value?.open(record.id)
}

const handleSubmit = async (record: CustomOutboundPageVO) => {
  try {
    const result = await submitCustomOutbound(record.id)
    if (isSuccess(result)) {
      message.success('提交成功')
      reloadTable()
    } else {
      // 库存不足时后端 message 已带 SKU 缺口明细
      message.error(result.message || '提交失败')
    }
  } catch (e) {
    message.error('提交失败，请重试')
  }
}

const handleCancel = (record: CustomOutboundPageVO) => {
  doRequest(cancelCustomOutbound(record.id), {
    successMessage: '取消成功',
    onSuccess: () => reloadTable()
  })
}

const handleDelete = (record: CustomOutboundPageVO) => {
  doRequest(deleteCustomOutbound([record.id]), {
    successMessage: '删除成功',
    onSuccess: () => reloadTable(true)
  })
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const response = await exportCustomOutbound(searchParams)
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
  emitter.on('refresh-custom-outbound-list', () => reloadTable())
})

onUnmounted(() => {
  emitter.off('refresh-custom-outbound-list')
})
</script>

<style scoped>
/* ==================== 出库单号列 ==================== */
.outbound-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.outbound-no {
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
