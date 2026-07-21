<template>
  <a-drawer
    v-model:open="visible"
    title="采购入库单详情"
    :width="800"
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
          <a-descriptions-item label="关联物流单">
            {{ detail.shippingOrderNo }}
          </a-descriptions-item>
          <a-descriptions-item label="入库仓库">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="入库日期">{{ detail.inboundDate }}</a-descriptions-item>
          <a-descriptions-item label="创建人">{{ detail.createByName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间" :span="2">{{
            detail.createTime
          }}</a-descriptions-item>
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
            :scroll="{ x: 700 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'skuBrief'">
                <sku-brief-cell :brief="record.skuBrief" />
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
                  <a-table-summary-cell :index="0" :col-span="2" align="right">
                    <strong>合计</strong>
                  </a-table-summary-cell>
                  <a-table-summary-cell :index="2" align="center">
                    <strong>{{ totalExpected }}</strong>
                  </a-table-summary-cell>
                  <a-table-summary-cell :index="3" align="center">
                    <strong v-if="showActualNumbers">{{ totalActual }}</strong>
                    <span v-else class="status-placeholder">—</span>
                  </a-table-summary-cell>
                  <a-table-summary-cell :index="4" align="center">
                    <strong v-if="showActualNumbers" :class="{ 'short-quantity': totalShort > 0 }">
                      {{ totalShort }}
                    </strong>
                    <span v-else class="status-placeholder">—</span>
                  </a-table-summary-cell>
                </a-table-summary-row>
              </a-table-summary>
            </template>
          </a-table>
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
import { getPurchaseInboundDetail, submitInbound } from '@/api/wms/purchase-inbound'
import { InboundStatus } from '@/api/wms/purchase-inbound/types'
import type { PurchaseInboundDetailVO } from '@/api/wms/purchase-inbound/types'
import InboundStatusBadge from './components/InboundStatusBadge.vue'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'
import { isSuccess } from '@/api'
import { doRequest } from '@/utils/axios/request'
import { useInboundPermission } from './hooks/useInboundPermission'
import { useInboundItemSummary } from './hooks/useInboundItemSummary'

const emits = defineEmits<{
  (e: 'update-success'): void
}>()

const router = useRouter()

const visible = ref(false)
const loading = ref(false)
const detail = ref<PurchaseInboundDetailVO | null>(null)

// 入库明细表格列
const itemColumns = [
  { title: '采购单号', dataIndex: 'purchaseOrderNo', width: 140 },
  { title: 'SKU信息', key: 'skuBrief', width: 200 },
  { title: '应到数量', dataIndex: 'expectedQuantity', width: 90, align: 'center' },
  { title: '实到数量', key: 'actualQuantity', width: 90, align: 'center' },
  {
    title: '未到数量',
    key: 'shortQuantity',
    width: 90,
    align: 'center'
  }
]

// 实到/未到列：草稿/已提交阶段尚无实收数据，显示状态文字；已收货/已完成显示平台录入的数字
const showActualNumbers = computed(() => {
  const s = detail.value?.orderStatus
  return s === InboundStatus.RECEIVED || s === InboundStatus.COMPLETED
})
const statusText = computed(() =>
  detail.value?.orderStatus === InboundStatus.SUBMITTED ? '已提交' : '草稿'
)

// 合计计算（使用 Hook）
const { totalExpected, totalActual, totalShort } = useInboundItemSummary(
  computed(() => detail.value?.items || [])
)

// 权限判断（使用 Hook）
const { canEdit, canSubmit } = useInboundPermission(computed(() => detail.value?.orderStatus))

/**
 * 打开抽屉
 */
const open = async (id: number) => {
  visible.value = true
  loading.value = true
  try {
    const result = await getPurchaseInboundDetail(id)
    if (isSuccess(result)) {
      detail.value = result.data
    } else {
      message.error(result.message || '获取详情失败')
      visible.value = false // 失败时关闭抽屉
    }
  } catch (e) {
    message.error('获取详情失败')
    visible.value = false // 失败时关闭抽屉
  } finally {
    loading.value = false
  }
}

/**
 * 关闭抽屉
 */
const handleClose = () => {
  visible.value = false
  detail.value = null
}

/**
 * 编辑
 */
const handleEdit = () => {
  if (detail.value) {
    visible.value = false
    router.push(`/wms/purchase-inbound/form/edit/${detail.value.id}`)
  }
}

/**
 * 提交入库单
 */
const handleSubmit = () => {
  if (!detail.value) return
  Modal.confirm({
    title: '提交入库单',
    content: '确认要提交吗？提交后将流转至平台收货上架。',
    okText: '确定',
    cancelText: '取消',
    onOk: () => {
      doRequest(submitInbound(detail.value!.id), {
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
</style>
