<template>
  <a-drawer
    v-model:open="visible"
    title="商品入库详情"
    :width="760"
    :body-style="{ paddingBottom: '80px' }"
  >
    <a-spin :spinning="loading">
      <template v-if="detail">
        <!-- 基本信息 -->
        <a-descriptions title="基本信息" :column="2" bordered size="small">
          <a-descriptions-item label="入库单号">{{ detail.inboundNo }}</a-descriptions-item>
          <a-descriptions-item label="单据状态">
            <inbound-status-badge :status="detail.orderStatus" />
          </a-descriptions-item>
          <a-descriptions-item label="入库仓库">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="入库日期">{{ detail.inboundDate }}</a-descriptions-item>
          <a-descriptions-item label="创建人">{{ detail.createByName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detail.createTime }}</a-descriptions-item>
        </a-descriptions>

        <!-- 入库明细 -->
        <div style="margin-top: 16px">
          <h4>入库明细</h4>
          <a-table
            :data-source="detail.items"
            :columns="itemColumns"
            :pagination="false"
            size="small"
            row-key="id"
            :scroll="{ x: 900 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'skuBrief'">
                <sku-brief-cell :brief="record.skuBrief" />
              </template>
              <template v-else-if="column.key === 'dimensions'">
                <span :class="{ 'missing-dimension': !hasDimensions(record) }">
                  {{ dimensionsText(record) }}
                </span>
              </template>
              <template v-else-if="column.key === 'volume'">
                {{ volumeText(record) }}
              </template>
              <!-- 实到数量：草稿/已提交显示状态文字，已收货/已完成显示平台录入的数字 -->
              <template v-else-if="column.key === 'actualQuantity'">
                <span v-if="showActualNumbers">{{ record.actualQuantity }}</span>
                <span v-else class="status-placeholder">{{ statusText }}</span>
              </template>
              <!-- 未到数量：同上 -->
              <template v-else-if="column.key === 'shortQuantity'">
                <span
                  v-if="showActualNumbers"
                  :class="{ 'short-quantity': record.shortQuantity > 0 }"
                >
                  {{ record.shortQuantity }}
                </span>
                <span v-else class="status-placeholder">{{ statusText }}</span>
              </template>
            </template>
            <template #summary>
              <a-table-summary fixed>
                <a-table-summary-row>
                  <a-table-summary-cell :index="0" align="right">
                    <strong>合计</strong>
                  </a-table-summary-cell>
                  <a-table-summary-cell :index="1" align="center">
                    <strong>{{ totalExpected }}</strong>
                  </a-table-summary-cell>
                  <a-table-summary-cell :index="2" align="center">
                    <strong v-if="showActualNumbers">{{ totalActual }}</strong>
                    <span v-else class="status-placeholder">—</span>
                  </a-table-summary-cell>
                  <a-table-summary-cell :index="3" align="center">
                    <strong v-if="showActualNumbers" :class="{ 'short-quantity': totalShort > 0 }">
                      {{ totalShort }}
                    </strong>
                    <span v-else class="status-placeholder">—</span>
                  </a-table-summary-cell>
                </a-table-summary-row>
              </a-table-summary>
            </template>
          </a-table>
          <div class="dimension-summary">
            <a-tag color="blue">累计长：{{ totalLengthText }}</a-tag>
            <a-tag color="blue">累计宽：{{ totalWidthText }}</a-tag>
            <a-tag color="blue">累计高：{{ totalHeightText }}</a-tag>
            <a-tag color="green">总体积：{{ totalVolumeText }}</a-tag>
          </div>
        </div>

        <!-- 备注 -->
        <a-descriptions
          v-if="detail.remark"
          title="备注"
          :column="1"
          bordered
          size="small"
          style="margin-top: 16px"
        >
          <a-descriptions-item>{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </a-spin>

    <!-- 底部操作按钮 -->
    <template #footer>
      <div class="drawer-footer">
        <a-space>
          <a-button @click="handleClose">关闭</a-button>
          <a-button v-if="canEdit" type="primary" @click="handleEdit">编辑</a-button>
          <a-button v-if="canSubmit" type="primary" @click="handleSubmit">提交</a-button>
        </a-space>
      </div>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getManualInboundDetail, submitManualInbound } from '@/api/wms/manual-inbound'
import { InboundStatus } from '@/api/wms/purchase-inbound/types'
import type { PurchaseInboundDetailVO } from '@/api/wms/purchase-inbound/types'
import InboundStatusBadge from '@/views/wms/purchase-inbound/components/InboundStatusBadge.vue'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'
import { isSuccess } from '@/api'
import { doRequest } from '@/utils/axios/request'
import { useInboundPermission } from '@/views/wms/purchase-inbound/hooks/useInboundPermission'
import { useInboundItemSummary } from '@/views/wms/purchase-inbound/hooks/useInboundItemSummary'

const emits = defineEmits<{
  (e: 'update-success'): void
}>()

const router = useRouter()

const visible = ref(false)
const loading = ref(false)
const detail = ref<PurchaseInboundDetailVO | null>(null)

const itemColumns = [
  { title: 'SKU信息', key: 'skuBrief', width: 220 },
  { title: '包装尺寸（长×宽×高）', key: 'dimensions', width: 180, align: 'center' },
  { title: '应到数量', dataIndex: 'expectedQuantity', width: 100, align: 'center' },
  { title: '包装体积', key: 'volume', width: 120, align: 'center' },
  { title: '实到数量', key: 'actualQuantity', width: 100, align: 'center' },
  { title: '未到数量', key: 'shortQuantity', width: 100, align: 'center' }
]

// 实到/未到列：草稿/已提交阶段尚无实收数据，显示状态文字；已收货/已完成显示平台录入的数字
const showActualNumbers = computed(() => {
  const s = detail.value?.orderStatus
  return s === InboundStatus.RECEIVED || s === InboundStatus.COMPLETED
})
const statusText = computed(() =>
  detail.value?.orderStatus === InboundStatus.SUBMITTED ? '已提交' : '草稿'
)

const { totalExpected, totalActual, totalShort } = useInboundItemSummary(
  computed(() => detail.value?.items || [])
)

const hasDimensions = (record: PurchaseInboundDetailVO['items'][number]) =>
  [record.outerLengthMm, record.outerWidthMm, record.outerHeightMm].every(
    value => Number(value) > 0
  )
const volumeCbm = (record: PurchaseInboundDetailVO['items'][number]) =>
  hasDimensions(record)
    ? (record.outerLengthMm! *
        record.outerWidthMm! *
        record.outerHeightMm! *
        Number(record.expectedQuantity || 0)) /
      1_000_000_000
    : 0
const dimensionsText = (record: PurchaseInboundDetailVO['items'][number]) =>
  hasDimensions(record)
    ? (record.outerLengthMm! / 10).toFixed(1) +
      ' × ' +
      (record.outerWidthMm! / 10).toFixed(1) +
      ' × ' +
      (record.outerHeightMm! / 10).toFixed(1) +
      ' cm'
    : '待维护'
const volumeText = (record: PurchaseInboundDetailVO['items'][number]) =>
  hasDimensions(record) ? volumeCbm(record).toFixed(4) + ' m³' : '-'
const totalLengthMm = computed(() =>
  (detail.value?.items || []).reduce(
    (sum, item) => sum + (hasDimensions(item) ? item.outerLengthMm! * item.expectedQuantity : 0),
    0
  )
)
const totalWidthMm = computed(() =>
  (detail.value?.items || []).reduce(
    (sum, item) => sum + (hasDimensions(item) ? item.outerWidthMm! * item.expectedQuantity : 0),
    0
  )
)
const totalHeightMm = computed(() =>
  (detail.value?.items || []).reduce(
    (sum, item) => sum + (hasDimensions(item) ? item.outerHeightMm! * item.expectedQuantity : 0),
    0
  )
)
const totalVolumeCbm = computed(() =>
  (detail.value?.items || []).reduce((sum, item) => sum + volumeCbm(item), 0)
)
const totalLengthText = computed(() => (totalLengthMm.value / 10).toFixed(1) + ' cm')
const totalWidthText = computed(() => (totalWidthMm.value / 10).toFixed(1) + ' cm')
const totalHeightText = computed(() => (totalHeightMm.value / 10).toFixed(1) + ' cm')
const totalVolumeText = computed(() => totalVolumeCbm.value.toFixed(4) + ' m³')

const { canEdit, canSubmit } = useInboundPermission(
  computed(() => detail.value?.orderStatus),
  'wms:manual-inbound'
)

const open = async (id: number) => {
  visible.value = true
  loading.value = true
  try {
    const result = await getManualInboundDetail(id)
    if (isSuccess(result)) {
      detail.value = result.data
    } else {
      message.error(result.message || '获取详情失败')
      visible.value = false
    }
  } catch (e) {
    message.error('获取详情失败')
    visible.value = false
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  visible.value = false
  detail.value = null
}

const handleEdit = () => {
  if (detail.value) {
    visible.value = false
    router.push(`/wms/manual-inbound/form/edit/${detail.value.id}`)
  }
}

const handleSubmit = () => {
  if (!detail.value) return
  Modal.confirm({
    title: '提交入库单',
    content: '确认要提交吗？提交后将流转至平台收货上架。',
    okText: '确定',
    cancelText: '取消',
    onOk: () => {
      doRequest(submitManualInbound(detail.value!.id), {
        successMessage: '提交成功',
        onSuccess: () => {
          emits('update-success')
          handleClose()
        }
      })
    }
  })
}

defineExpose({ open })
</script>

<style scoped>
.drawer-footer {
  text-align: right;
}

.short-quantity {
  color: #f5222d;
  font-weight: 600;
}

.status-placeholder {
  color: #8c8c8c;
}

.dimension-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}

.missing-dimension {
  color: #fa8c16;
}
</style>
