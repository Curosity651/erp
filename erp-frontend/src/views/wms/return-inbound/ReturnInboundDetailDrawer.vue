<template>
  <a-drawer
    v-model:open="visible"
    title="退货入库单详情"
    :width="700"
    :body-style="{ paddingBottom: '80px' }"
  >
    <a-spin :spinning="loading">
      <template v-if="detail">
        <!-- 基本信息 -->
        <a-descriptions title="基本信息" :column="2" bordered size="small">
          <a-descriptions-item label="退货单号">{{ detail.returnNo }}</a-descriptions-item>
          <a-descriptions-item label="平台订单号">{{ detail.platformOrderId }}</a-descriptions-item>
          <a-descriptions-item label="平台">
            <PlatformTag :platform="detail.platform" />
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            {{ RETURN_STATUS_TEXT[detail.returnStatus] || detail.returnStatus }}
          </a-descriptions-item>
          <a-descriptions-item label="入库仓库">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="退货日期">{{ detail.returnDate }}</a-descriptions-item>
          <a-descriptions-item label="退货原因">
            {{ ReturnReasonMap[detail.returnReason] || detail.returnReason || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="创建人">{{ detail.createByName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detail.createTime }}</a-descriptions-item>
        </a-descriptions>

        <!-- 退货商品（单 SKU 场景） -->
        <a-card size="small" title="退货商品" style="margin-top: 16px">
          <div class="sku-info-row">
            <SkuBriefCell :brief="detail.skuBrief" show-code />
          </div>
          <a-descriptions :column="4" size="small" class="mt-3">
            <a-descriptions-item label="可退数量">
              <span class="quantity-value">{{ detail.returnableQuantity }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="退货数量">
              <span class="quantity-value">{{ detail.totalQuantity }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="合格入库">
              <span class="quantity-value qualified">{{ detail.qualifiedQuantity }}</span>
            </a-descriptions-item>
            <template v-if="detail.unqualifiedQuantity > 0">
              <a-descriptions-item label="不合格">
                <span class="quantity-value unqualified">{{ detail.unqualifiedQuantity }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="处理方式" :span="4">
                转残品 {{ detail.toDamagedQuantity }}，报废 {{ detail.scrapQuantity }}
              </a-descriptions-item>
            </template>
            <a-descriptions-item v-else label="不合格">
              <span class="quantity-value">0</span>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

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
        </a-space>
      </div>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { getReturnInboundDetail } from '@/api/wms/return-inbound'
import { ReturnReasonMap } from '@/api/wms/return-inbound/types'
import type { ReturnInboundDetailVO } from '@/api/wms/return-inbound/types'
import { PlatformTag } from '@/components/Platform'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'

const RETURN_STATUS_TEXT: Record<string, string> = {
  RETURN_PENDING: '待仓库收货',
  QC_PENDING: '待质检',
  COMPLETED: '已完成',
  CLOSED: '已关闭'
}

const visible = ref(false)
const loading = ref(false)
const detail = ref<ReturnInboundDetailVO | null>(null)

/**
 * 打开抽屉
 */
const open = async (id: number) => {
  visible.value = true
  loading.value = true
  try {
    const result = await getReturnInboundDetail(id)
    if (isSuccess(result)) {
      detail.value = result.data
    } else {
      message.error(result.message || '获取详情失败')
    }
  } catch (e) {
    message.error('获取详情失败')
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

defineExpose({ open })
</script>

<style scoped>
.drawer-footer {
  text-align: right;
}

.sku-info-row {
  padding: 8px 0;
}

.quantity-value {
  font-weight: 600;
  font-size: 14px;
}

.qualified {
  color: #52c41a;
}

.unqualified {
  color: #ff4d4f;
}
</style>
