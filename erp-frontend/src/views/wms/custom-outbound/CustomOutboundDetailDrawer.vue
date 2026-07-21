<template>
  <a-drawer
    v-model:open="visible"
    title="自定义出库单详情"
    :width="760"
    :body-style="{ paddingBottom: '80px' }"
  >
    <a-spin :spinning="loading">
      <template v-if="detail">
        <!-- 基本信息 -->
        <a-descriptions title="基本信息" :column="2" bordered size="small">
          <a-descriptions-item label="出库单号">{{ detail.outboundNo }}</a-descriptions-item>
          <a-descriptions-item label="单据状态">
            <custom-outbound-status-badge :status="detail.orderStatus" />
          </a-descriptions-item>
          <a-descriptions-item label="出库类型">
            <a-tag :color="CustomOutboundTypeColorMap[detail.customType] || 'default'">
              {{ CustomOutboundTypeMap[detail.customType] || detail.customType }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="关联单号">{{ detail.refNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="出库仓库">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="出库日期">{{ detail.outboundDate }}</a-descriptions-item>
          <a-descriptions-item label="创建时间" :span="2">{{ detail.createTime }}</a-descriptions-item>
        </a-descriptions>

        <a-descriptions
          v-if="hasLogisticsInfo"
          title="物流与签出"
          :column="2"
          bordered
          size="small"
          style="margin-top: 16px"
        >
          <a-descriptions-item label="物流产品">
            {{ detail.logisticsProductName || '未选择' }}
          </a-descriptions-item>
          <a-descriptions-item label="物流渠道">{{ detail.channelName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="计费重量">
            {{ detail.weight != null ? `${detail.weight} kg` : '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="物流费用">
            {{ detail.shippingFee != null ? `₽${detail.shippingFee}` : '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="运单号" :span="2">
            {{ detail.trackingNo || '未填写' }}
          </a-descriptions-item>
        </a-descriptions>

        <!-- 收货人信息（无收货人的销毁类等不展示） -->
        <a-descriptions
          v-if="hasReceiver"
          title="收货人信息"
          :column="2"
          bordered
          size="small"
          style="margin-top: 16px"
        >
          <a-descriptions-item label="收件人">{{ detail.receiverName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="电话">{{ detail.receiverPhone || '-' }}</a-descriptions-item>
          <a-descriptions-item label="收货地址" :span="2">
            {{ detail.receiverAddress || '-' }}
          </a-descriptions-item>
        </a-descriptions>

        <!-- 出库明细 -->
        <div style="margin-top: 16px">
          <h4>出库明细</h4>
          <a-table
            :data-source="detail.items"
            :columns="itemColumns"
            :pagination="false"
            size="small"
            row-key="id"
            :scroll="{ x: 620 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'skuBrief'">
                <sku-brief-cell :brief="record.skuBrief" />
              </template>
              <!-- 可售库存：草稿显示实时可售与缺口；已提交及之后库存已预占 -->
              <template v-else-if="column.key === 'availableStock'">
                <template v-if="isDraft">
                  <span :class="{ 'shortage-quantity': (record.shortage ?? 0) > 0 }">
                    {{ record.availableStock ?? 0 }}
                    <template v-if="(record.shortage ?? 0) > 0">（缺{{ record.shortage }}）</template>
                  </span>
                </template>
                <span v-else class="status-placeholder">已预占</span>
              </template>
            </template>
            <template #summary>
              <a-table-summary fixed>
                <a-table-summary-row>
                  <a-table-summary-cell :index="0" align="right">
                    <strong>合计</strong>
                  </a-table-summary-cell>
                  <a-table-summary-cell :index="1" align="center">
                    <strong>{{ totalQuantity }}</strong>
                  </a-table-summary-cell>
                  <a-table-summary-cell :index="2" align="center">
                    <span class="status-placeholder">—</span>
                  </a-table-summary-cell>
                  <a-table-summary-cell :index="3" />
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
        <div>
          <a-popconfirm
            v-if="canDelete"
            title="确定要删除该草稿出库单吗？"
            ok-text="确定"
            cancel-text="取消"
            @confirm="handleDelete"
          >
            <a-button danger :loading="actionLoading">删除</a-button>
          </a-popconfirm>
          <a-popconfirm
            v-else-if="canCancel"
            title="确定要取消出库吗？取消后将释放已占用库存。"
            ok-text="确定"
            cancel-text="取消"
            @confirm="handleCancelOutbound"
          >
            <a-button danger :loading="actionLoading">取消出库</a-button>
          </a-popconfirm>
        </div>
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
import {
  getCustomOutboundDetail,
  submitCustomOutbound,
  cancelCustomOutbound,
  deleteCustomOutbound
} from '@/api/wms/custom-outbound'
import type { CustomOutboundDetailVO } from '@/api/wms/custom-outbound/types'
import {
  CustomOutboundStatus,
  CustomOutboundTypeMap,
  CustomOutboundTypeColorMap
} from '@/api/wms/custom-outbound/types'
import CustomOutboundStatusBadge from './components/CustomOutboundStatusBadge.vue'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'
import { isSuccess } from '@/api'
import { useAuthorize } from '@/hooks/permission'

const emits = defineEmits<{
  (e: 'update-success'): void
}>()

const router = useRouter()
const { hasPermission } = useAuthorize()

const visible = ref(false)
const loading = ref(false)
const actionLoading = ref(false)
const detail = ref<CustomOutboundDetailVO | null>(null)

const itemColumns = [
  { title: 'SKU信息', key: 'skuBrief', width: 240 },
  { title: '出库数量', dataIndex: 'quantity', width: 100, align: 'center' },
  { title: '可售库存', key: 'availableStock', width: 130, align: 'center' },
  { title: '备注', dataIndex: 'remark', width: 150 }
]

const isDraft = computed(() => detail.value?.orderStatus === CustomOutboundStatus.DRAFT)

const hasReceiver = computed(() => {
  const d = detail.value
  return !!(d && (d.receiverName || d.receiverPhone || d.receiverAddress))
})

const hasLogisticsInfo = computed(() => {
  const d = detail.value
  return !!(d && (d.logisticsProductId || d.channelName || d.weight != null || d.shippingFee != null))
})

const totalQuantity = computed(() =>
  (detail.value?.items || []).reduce((sum, item) => sum + (item.quantity || 0), 0)
)

const canEdit = computed(() => isDraft.value && hasPermission('wms:custom-outbound:edit'))
const canSubmit = computed(() => isDraft.value && hasPermission('wms:custom-outbound:edit'))
const canDelete = computed(() => isDraft.value && hasPermission('wms:custom-outbound:del'))
const canCancel = computed(
  () =>
    detail.value?.orderStatus === CustomOutboundStatus.CONFIRMED &&
    hasPermission('wms:custom-outbound:edit')
)

const open = async (id: number) => {
  visible.value = true
  loading.value = true
  try {
    const result = await getCustomOutboundDetail(id)
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
    router.push(`/wms/custom-outbound/form/edit/${detail.value.id}`)
  }
}

const handleSubmit = () => {
  if (!detail.value) return
  Modal.confirm({
    title: '提交出库单',
    content: '确认要提交吗？提交后将按可售库存校验并预占，流转至海外仓作业台下架/打包/签出。',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        const result = await submitCustomOutbound(detail.value!.id)
        if (isSuccess(result)) {
          message.success('提交成功')
          emits('update-success')
          handleClose()
        } else {
          // 库存不足时后端 message 已带 SKU 缺口明细
          message.error(result.message || '提交失败')
          // 刷新详情以展示最新库存缺口
          await open(detail.value!.id)
        }
      } catch (e) {
        message.error('提交失败，请重试')
      }
    }
  })
}

const handleCancelOutbound = async () => {
  if (!detail.value) return
  actionLoading.value = true
  try {
    const result = await cancelCustomOutbound(detail.value.id)
    if (isSuccess(result)) {
      message.success('取消成功，已释放占用库存')
      emits('update-success')
      await open(detail.value.id)
    } else {
      message.error(result.message || '取消失败')
    }
  } finally {
    actionLoading.value = false
  }
}

const handleDelete = async () => {
  if (!detail.value) return
  actionLoading.value = true
  try {
    const result = await deleteCustomOutbound([detail.value.id])
    if (isSuccess(result)) {
      message.success('删除成功')
      emits('update-success')
      handleClose()
    } else {
      message.error(result.message || '删除失败')
    }
  } finally {
    actionLoading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.drawer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.shortage-quantity {
  color: #f5222d;
  font-weight: 600;
}

.status-placeholder {
  color: #8c8c8c;
}
</style>
