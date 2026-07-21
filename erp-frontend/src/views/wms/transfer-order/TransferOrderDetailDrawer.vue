<template>
  <a-drawer v-model:open="visible" title="调拨单详情" width="800" :destroy-on-close="true">
    <a-spin :spinning="loading">
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="调拨单号">{{ detail?.transferNo }}</a-descriptions-item>
        <a-descriptions-item label="源仓库">{{ detail?.fromWarehouseName }}</a-descriptions-item>
        <a-descriptions-item label="目标仓库">{{ detail?.toWarehouseName }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(detail?.orderStatus)">
            {{ TransferOrderStatusMap[detail?.orderStatus || ''] }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="出库时间">{{ detail?.shipTime || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="getEndTimeLabel(detail?.orderStatus)">{{
          detail?.endTime || '-'
        }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ detail?.createTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{
          detail?.remark || '-'
        }}</a-descriptions-item>
      </a-descriptions>

      <a-divider orientation="left">调拨明细</a-divider>
      <a-table
        :columns="itemColumns"
        :data-source="detail?.items || []"
        :pagination="false"
        size="small"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'skuBrief'">
            <sku-brief-cell :brief="record.skuBrief" />
          </template>
          <template v-if="column.key === 'receivedQuantity'">
            {{ record.receivedQuantity ?? '-' }}
          </template>
        </template>
      </a-table>
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { ColumnType } from 'ant-design-vue/es/table'
import { getTransferOrderDetail } from '@/api/wms/transfer-order'
import {
  TransferOrderStatusMap,
  TransferOrderStatusColorMap,
  TransferOrderStatusMeta,
  TransferOrderStatus
} from '@/api/wms/transfer-order/types'
import type { TransferOrderDetailVO } from '@/api/wms/transfer-order/types'
import { isSuccess } from '@/api'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'

defineOptions({ name: 'TransferOrderDetailDrawer' })

const visible = ref(false)
const loading = ref(false)
const detail = ref<TransferOrderDetailVO | null>(null)

/**
 * 获取状态颜色
 */
const getStatusColor = (status?: string): string => {
  if (!status) return 'default'
  return TransferOrderStatusColorMap[status] || 'default'
}

/**
 * 获取结束时间标签
 */
const getEndTimeLabel = (status?: string): string => {
  if (!status) return '结束时间'
  return TransferOrderStatusMeta[status as TransferOrderStatus]?.endTimeLabel || '结束时间'
}

// 明细表格列
const itemColumns: ColumnType[] = [
  { title: 'SKU信息', key: 'skuBrief', width: 150 },
  { title: '调拨数量', dataIndex: 'quantity', width: 100, align: 'center' },
  { title: '实际入库数量', key: 'receivedQuantity', width: 120, align: 'center' },
  { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true }
]

/**
 * 加载详情
 */
const loadDetail = async (id: number) => {
  loading.value = true
  try {
    const result = await getTransferOrderDetail(id)
    if (isSuccess(result) && result.data) {
      detail.value = result.data
    }
  } catch (e) {
    console.error('加载调拨单详情失败', e)
  } finally {
    loading.value = false
  }
}

/**
 * 打开抽屉
 */
const open = (id: number) => {
  visible.value = true
  detail.value = null
  loadDetail(id)
}

defineExpose({ open })
</script>

<style scoped>
:deep(.ant-descriptions-item-label) {
  width: 120px;
  font-weight: 500;
}
</style>
