<template>
  <a-modal
    v-model:open="visible"
    title="确认付款"
    :confirm-loading="loading"
    :mask-closable="false"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="物流单号">
        <span>{{ shippingNo }}</span>
      </a-form-item>
      <a-form-item label="付款凭证" required>
        <sys-file-upload
          v-model="voucherFileId"
          bucket-key="private-files"
          button-text="上传凭证"
          :allowed-types="['application/pdf', 'image/jpeg', 'image/png']"
        />
        <div class="upload-tip">支持 PDF、JPG、PNG 格式</div>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import SysFileUpload from '@/components/Upload/SysFileUpload.vue'
import { updatePaymentStatus } from '@/api/wms/shipping-order'
import { doRequest } from '@/utils/axios/request'

const emits = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const loading = ref(false)
const shippingOrderId = ref<number>()
const shippingNo = ref('')
const voucherFileId = ref<number>()

/**
 * 打开弹窗
 */
const open = (id: number, no: string) => {
  shippingOrderId.value = id
  shippingNo.value = no
  voucherFileId.value = undefined
  visible.value = true
}

/**
 * 确认付款
 */
const handleOk = async () => {
  if (!voucherFileId.value) {
    message.warning('请上传付款凭证')
    return
  }

  loading.value = true
  doRequest(updatePaymentStatus(shippingOrderId.value!, 1, voucherFileId.value), {
    successMessage: '付款确认成功',
    onSuccess: () => {
      visible.value = false
      emits('success')
    },
    onFinally: () => {
      loading.value = false
    }
  })
}

/**
 * 取消
 */
const handleCancel = () => {
  visible.value = false
  voucherFileId.value = undefined
}

defineExpose({ open })
</script>

<style scoped>
.upload-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}
</style>
