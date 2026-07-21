<template>
  <a-modal v-model:open="visible" :title="title" :footer="null" :width="600">
    <div class="voucher-content">
      <!-- 付款信息 -->
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="凭证类型">
          {{ voucherTypeText }}
        </a-descriptions-item>
        <a-descriptions-item label="付款金额">
          <span class="amount"
            >{{ currencySymbolValue }}{{ formatAmountNumber(paymentAmount) }}</span
          >
        </a-descriptions-item>
        <a-descriptions-item label="付款时间" :span="2">
          {{ paymentTime || '-' }}
        </a-descriptions-item>
      </a-descriptions>

      <!-- 凭证预览 -->
      <div class="voucher-preview">
        <div class="preview-title">付款凭证</div>

        <!-- 图片预览 -->
        <div v-if="isImage && previewUrl" class="image-preview">
          <a-image :src="previewUrl" :alt="fileName" />
        </div>

        <!-- PDF 预览 -->
        <div v-else-if="isPdf && previewUrl" class="pdf-preview">
          <vue-office-pdf
            :key="previewUrl"
            :src="previewUrl"
            class="pdf-viewer"
            @rendered="handlePdfRendered"
            @error="handlePdfError"
          />
          <div v-if="pdfLoading" class="pdf-loading">
            <a-spin size="large" />
            <p>正在加载PDF...</p>
          </div>
        </div>

        <!-- 其他文件下载 -->
        <div v-else-if="fileInfo" class="file-download">
          <div class="file-card">
            <file-pdf-outlined v-if="isPdf" class="file-icon pdf" />
            <file-outlined v-else class="file-icon" />
            <div class="file-details">
              <span class="file-name">{{ fileName }}</span>
              <span class="file-size">{{ formatFileSize(fileInfo.fileSize) }}</span>
            </div>
            <a-button type="primary" @click="handleDownload">
              <download-outlined /> 下载查看
            </a-button>
          </div>
        </div>

        <!-- 无凭证 -->
        <div v-else class="no-voucher">
          <file-unknown-outlined class="empty-icon" />
          <span>暂无凭证文件</span>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  FilePdfOutlined,
  FileOutlined,
  FileUnknownOutlined,
  DownloadOutlined
} from '@ant-design/icons-vue'
import VueOfficePdf from '@vue-office/pdf'
import { getFileDownloadUrl, getFileInfo } from '@/api/system/file'
import { formatFileSize } from '@/hooks/use-sys-file-upload'
import { getCurrencySymbol } from '@/utils/currency-utils'
import type { SysFileVO } from '@/api/system/file/types'
import type { FileInfoVO } from '@/api/wms/purchase-order/types'

type VoucherType = 'PREPAY' | 'BALANCE'

interface Props {
  currencyCode?: string
}

const props = withDefaults(defineProps<Props>(), {
  currencyCode: 'CNY'
})

interface VoucherData {
  type: VoucherType
  amount: number
  paymentTime?: string
  fileInfo?: FileInfoVO
}

const visible = ref(false)
const voucherData = ref<VoucherData>()
const fileInfo = ref<SysFileVO>()
const previewUrl = ref<string>()
const pdfLoading = ref(false)

// 币种符号
const currencySymbolValue = computed(() => getCurrencySymbol(props.currencyCode))

// 标题
const title = computed(() => {
  if (voucherData.value?.type === 'PREPAY') {
    return '首付款凭证'
  }
  return '尾款凭证'
})

// 凭证类型文本
const voucherTypeText = computed(() => {
  if (voucherData.value?.type === 'PREPAY') {
    return '首付款凭证'
  }
  return '尾款凭证'
})

// 付款金额
const paymentAmount = computed(() => voucherData.value?.amount || 0)

// 付款时间
const paymentTime = computed(() => voucherData.value?.paymentTime)

// 文件名
const fileName = computed(() => {
  return fileInfo.value?.fileName || voucherData.value?.fileInfo?.fileName || ''
})

// 文件类型判断
const contentType = computed(() => {
  return fileInfo.value?.contentType || voucherData.value?.fileInfo?.contentType || ''
})

const isImage = computed(() => contentType.value.startsWith('image/'))
const isPdf = computed(() => contentType.value.includes('pdf'))

// 格式化金额数值
const formatAmountNumber = (amount: number): string => {
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 加载文件信息和预览
const loadFileInfo = async (fileId: number) => {
  try {
    // 获取文件信息
    const infoRes = await getFileInfo(fileId)
    if (infoRes.code === 200 && infoRes.data) {
      fileInfo.value = infoRes.data
    }

    // 如果是图片或PDF，获取预览URL
    if (isImage.value || isPdf.value) {
      if (isPdf.value) {
        pdfLoading.value = true
      }
      const urlRes = await getFileDownloadUrl(fileId)
      if (urlRes.code === 200 && urlRes.data) {
        previewUrl.value = urlRes.data
      }
    }
  } catch (error) {
    console.error('加载文件信息失败:', error)
  }
}

// PDF 渲染成功
const handlePdfRendered = () => {
  pdfLoading.value = false
}

// PDF 渲染失败
const handlePdfError = (err: Error) => {
  console.error('PDF渲染失败:', err)
  pdfLoading.value = false
  message.error('PDF预览失败，请下载查看')
}

// 下载文件
const handleDownload = async () => {
  const fileId = voucherData.value?.fileInfo?.sysFileId
  if (!fileId) return

  try {
    const res = await getFileDownloadUrl(fileId)
    if (res.code === 200 && res.data) {
      window.open(res.data, '_blank')
    }
  } catch (error) {
    console.error('获取下载链接失败:', error)
  }
}

defineExpose({
  open(data: VoucherData) {
    voucherData.value = data
    fileInfo.value = undefined
    previewUrl.value = undefined
    visible.value = true

    // 加载文件信息
    if (data.fileInfo?.sysFileId) {
      loadFileInfo(data.fileInfo.sysFileId)
    }
  }
})
</script>

<style scoped>
.voucher-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.amount {
  font-size: 16px;
  font-weight: 600;
  color: #f5222d;
}

.voucher-preview {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 16px;
}

.preview-title {
  font-weight: 500;
  margin-bottom: 16px;
  color: #262626;
}

.image-preview {
  text-align: center;
}

.image-preview :deep(.ant-image) {
  max-width: 100%;
}

.image-preview :deep(.ant-image img) {
  max-height: 400px;
  object-fit: contain;
}

.pdf-preview {
  position: relative;
  min-height: 400px;
  background: #f5f5f5;
  border-radius: 6px;
  overflow: hidden;
}

.pdf-viewer {
  width: 100%;
  height: 400px;
}

.pdf-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  z-index: 10;
}

.pdf-loading p {
  margin-top: 12px;
  color: #8c8c8c;
}

.file-download {
  display: flex;
  justify-content: center;
}

.file-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  background: #fafafa;
  border-radius: 6px;
}

.file-icon {
  font-size: 32px;
  color: #8c8c8c;
}

.file-icon.pdf {
  color: #ff4d4f;
}

.file-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-name {
  font-weight: 500;
}

.file-size {
  color: #8c8c8c;
  font-size: 12px;
}

.no-voucher {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px;
  color: #8c8c8c;
}

.empty-icon {
  font-size: 48px;
}
</style>
