<script setup lang="ts">
import { ref, computed } from 'vue'
import { Modal, Descriptions, DescriptionsItem, Table, Alert } from 'ant-design-vue'
import { doRequest } from '@/utils/axios/request'
import { isSuccess } from '@/api'
import { getTransferOrderDetail, revokeTransferOrder } from '@/api/wms/transfer-order'
import type { TransferOrderDetailVO, TransferOrderPageVO } from '@/api/wms/transfer-order/types'

defineOptions({ name: 'RevokeConfirmModal' })

const emit = defineEmits<{ success: [] }>()

const open = ref(false)
const loading = ref(false)
const confirmLoading = ref(false)
const detail = ref<TransferOrderDetailVO | null>(null)

const columns = [
  { title: 'SKU编码', dataIndex: 'skuCode', width: 150 },
  { title: '商品名称', dataIndex: ['skuBrief', 'skuName'], ellipsis: true },
  { title: '调拨数量', dataIndex: 'quantity', width: 100, align: 'right' as const }
]

const totalQuantity = computed(() => {
  return detail.value?.items?.reduce((sum, item) => sum + item.quantity, 0) ?? 0
})

const openModal = async (record: TransferOrderPageVO) => {
  open.value = true
  loading.value = true
  try {
    const result = await getTransferOrderDetail(record.id)
    if (isSuccess(result)) {
      detail.value = result.data
    }
  } finally {
    loading.value = false
  }
}

const handleConfirm = async () => {
  if (!detail.value) return

  confirmLoading.value = true
  doRequest(revokeTransferOrder(detail.value.id), {
    successMessage: '撤回成功',
    onSuccess: () => {
      open.value = false
      emit('success')
    },
    onFinally: () => {
      confirmLoading.value = false
    }
  })
}

const handleCancel = () => {
  open.value = false
}

defineExpose({ open: openModal })
</script>

<template>
  <Modal
    v-model:open="open"
    title="撤回调拨"
    :confirm-loading="confirmLoading"
    ok-text="确认撤回"
    cancel-text="取消"
    width="600px"
    @ok="handleConfirm"
    @cancel="handleCancel"
  >
    <template v-if="detail">
      <Alert type="warning" show-icon class="revoke-alert">
        <template #message>确认撤回此调拨单？</template>
        <template #description>
          <div>撤回后将自动恢复库存：</div>
          <ul class="revoke-list">
            <li>源仓库（{{ detail.fromWarehouseName }}）可用库存 +{{ totalQuantity }}</li>
            <li>目标仓库（{{ detail.toWarehouseName }}）在途库存 -{{ totalQuantity }}</li>
          </ul>
        </template>
      </Alert>

      <Descriptions :column="2" size="small" bordered class="revoke-descriptions">
        <DescriptionsItem label="调拨单号">{{ detail.transferNo }}</DescriptionsItem>
        <DescriptionsItem label="当前状态">在途</DescriptionsItem>
        <DescriptionsItem label="源仓库">{{ detail.fromWarehouseName }}</DescriptionsItem>
        <DescriptionsItem label="目标仓库">{{ detail.toWarehouseName }}</DescriptionsItem>
      </Descriptions>

      <Table
        :columns="columns"
        :data-source="detail.items"
        :pagination="false"
        size="small"
        :loading="loading"
        row-key="id"
      />
    </template>
  </Modal>
</template>

<style scoped>
.revoke-alert {
  margin-bottom: 16px;
}

.revoke-list {
  margin: 8px 0 0 0;
  padding-left: 20px;
}

.revoke-descriptions {
  margin-bottom: 16px;
}
</style>
