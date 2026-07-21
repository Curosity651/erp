<template>
  <div class="form-section">
    <div class="section-title">
      <file-protect-outlined class="section-icon" />
      合同附件
    </div>

    <div class="contract-content">
      <!-- 未上传状态 -->
      <div v-if="!hasContract" class="upload-area">
        <sys-file-upload
          v-model="internalFileId"
          bucket-key="private-files"
          button-text="上传合同文件"
          :allowed-types="contractAllowedTypes"
          :disabled="disabled"
          :show-file-info="false"
          @success="handleUploadSuccess"
        />
        <div class="upload-hint">支持 PDF、Word、图片格式</div>
      </div>

      <!-- 已上传状态 -->
      <div v-else class="contract-file">
        <div class="file-info">
          <file-pdf-outlined v-if="isPdf" class="file-icon pdf" />
          <file-word-outlined v-else-if="isWord" class="file-icon word" />
          <file-image-outlined v-else-if="isImage" class="file-icon image" />
          <file-outlined v-else class="file-icon" />
          <span class="file-name" :title="contractFileName">{{ contractFileName }}</span>
          <span v-if="contractFileSize" class="file-size">{{
            formatFileSize(contractFileSize)
          }}</span>
        </div>
        <div class="file-actions">
          <a-button type="link" size="small" @click="handlePreview">
            <eye-outlined /> 预览
          </a-button>
          <a-button type="link" size="small" @click="handleDownload">
            <download-outlined /> 下载
          </a-button>
          <a-button v-if="!disabled" type="link" size="small" @click="handleReplace">
            <swap-outlined /> 替换
          </a-button>
          <!-- 合同文件不支持删除 -->
        </div>
      </div>

      <!-- 文件预览弹窗 -->
      <private-file-preview-modal
        v-bind="previewState"
        @update:open="
          (val: boolean) => {
            if (!val) closePreview()
          }
        "
        @close="closePreview"
        @download="downloadFile"
      />

      <!-- 替换上传（隐藏） -->
      <sys-file-upload
        v-show="false"
        ref="replaceUploadRef"
        v-model="replaceFileId"
        bucket-key="private-files"
        :allowed-types="contractAllowedTypes"
        :show-file-info="false"
        @success="handleReplaceSuccess"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  FilePdfOutlined,
  FileWordOutlined,
  FileImageOutlined,
  FileOutlined,
  DownloadOutlined,
  SwapOutlined,
  FileProtectOutlined,
  EyeOutlined
} from '@ant-design/icons-vue'
import { SysFileUpload } from '@/components/Upload'
import { PrivateFilePreviewModal } from '@/components/File'
import { getFileDownloadUrl } from '@/api/system/file'
import { isSuccess } from '@/api'
import { formatFileSize } from '@/hooks/use-sys-file-upload'
import { useFilePreview } from '@/hooks/use-file-preview'
import type { ContractInfoDTO, FileInfoVO } from '@/api/wms/purchase-order/types'
import type { SysFileUploadResult } from '@/hooks/use-sys-file-upload'

interface Props {
  // 合同信息DTO
  contractInfo: ContractInfoDTO
  // 已有的合同文件信息（详情返回）
  existingContractFile?: FileInfoVO
  // 是否禁用
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
})

const emits = defineEmits<{
  (e: 'update:contractInfo', value: ContractInfoDTO): void
}>()

// 合同允许的文件类型
const contractAllowedTypes = [
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'image/jpeg',
  'image/png'
]

const replaceUploadRef = ref()
const internalFileId = ref<number>()
const replaceFileId = ref<number>()

// 新上传的文件信息
const newFileInfo = ref<{ fileName: string; fileSize: number; contentType: string }>()

// 是否有合同文件
const hasContract = computed(() => {
  return props.existingContractFile || props.contractInfo.contractFileId
})

// 合同文件名
const contractFileName = computed(() => {
  if (newFileInfo.value) return newFileInfo.value.fileName
  return props.existingContractFile?.fileName || '合同文件'
})

// 合同文件大小
const contractFileSize = computed(() => {
  if (newFileInfo.value) return newFileInfo.value.fileSize
  return props.existingContractFile?.fileSize
})

// 文件类型判断
const contentType = computed(() => {
  if (newFileInfo.value) return newFileInfo.value.contentType
  return props.existingContractFile?.contentType || ''
})

const isPdf = computed(() => contentType.value.includes('pdf'))
const isWord = computed(
  () => contentType.value.includes('word') || contentType.value.includes('document')
)
const isImage = computed(() => contentType.value.startsWith('image/'))

// 文件预览
const { state: previewState, openPreview, closePreview, downloadFile } = useFilePreview()

// 预览文件
const handlePreview = () => {
  const fileId = props.contractInfo.contractFileId || props.existingContractFile?.sysFileId
  if (!fileId) return
  openPreview(fileId, contractFileName.value, contentType.value)
}

// 上传成功
const handleUploadSuccess = (result: SysFileUploadResult) => {
  newFileInfo.value = {
    fileName: result.fileName,
    fileSize: result.fileSize,
    contentType: result.contentType
  }
  emits('update:contractInfo', {
    contractFileId: result.fileId,
    action: 'UPLOAD'
  })
}

// 替换文件
const handleReplace = () => {
  // 触发隐藏的上传组件
  const uploadEl = replaceUploadRef.value?.$el?.querySelector('input[type="file"]')
  uploadEl?.click()
}

// 替换成功
const handleReplaceSuccess = (result: SysFileUploadResult) => {
  newFileInfo.value = {
    fileName: result.fileName,
    fileSize: result.fileSize,
    contentType: result.contentType
  }
  emits('update:contractInfo', {
    contractFileId: result.fileId,
    action: 'REPLACE'
  })
}

// 下载文件
const handleDownload = async () => {
  const fileId = props.contractInfo.contractFileId || props.existingContractFile?.sysFileId
  if (!fileId) return

  try {
    const res = await getFileDownloadUrl(fileId)
    if (isSuccess(res) && res.data) {
      window.open(res.data, '_blank')
    }
  } catch (error) {
    console.error('获取下载链接失败:', error)
  }
}
</script>

<style scoped>
.form-section {
  padding: 16px 20px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.section-icon {
  margin-right: 8px;
  color: #1890ff;
  font-size: 16px;
}

.upload-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upload-hint {
  color: #8c8c8c;
  font-size: 12px;
}

.contract-file {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.file-icon {
  font-size: 20px;
  color: #8c8c8c;
}

.file-icon.pdf {
  color: #ff4d4f;
}

.file-icon.word {
  color: #1890ff;
}

.file-icon.image {
  color: #52c41a;
}

.file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  color: #8c8c8c;
  font-size: 12px;
  flex-shrink: 0;
}

.file-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
</style>
