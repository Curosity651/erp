<template>
  <div class="form-section">
    <div class="section-header">
      <div class="section-title-inline">
        <paper-clip-outlined class="section-icon" />
        其他附件
      </div>
      <sys-file-upload
        v-if="!disabled"
        v-model="newFileId"
        bucket-key="private-files"
        button-text="添加附件"
        :show-file-info="false"
        @success="handleUploadSuccess"
      >
        <a-button size="small" type="primary">
          <template #icon><plus-outlined /></template>
          添加附件
        </a-button>
      </sys-file-upload>
    </div>

    <div v-if="fileList.length > 0" class="file-list">
      <div v-for="file in fileList" :key="file.id || file.sysFileId" class="file-item">
        <div class="file-info">
          <component :is="getFileIcon(file.contentType)" class="file-icon" />
          <span class="file-name" :title="file.fileName">{{ file.fileName }}</span>
          <span class="file-size">{{ formatFileSize(file.fileSize) }}</span>
        </div>
        <div class="file-actions">
          <a-button type="link" size="small" @click="handlePreview(file)">
            <eye-outlined />
          </a-button>
          <a-button type="link" size="small" @click="handleDownload(file)">
            <download-outlined />
          </a-button>
          <a-button v-if="!disabled" type="link" size="small" danger @click="handleDelete(file)">
            <delete-outlined />
          </a-button>
        </div>
      </div>
    </div>

    <div v-else class="empty-hint">暂无其他附件</div>

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
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  PlusOutlined,
  DownloadOutlined,
  DeleteOutlined,
  FileImageOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  FileOutlined,
  PaperClipOutlined,
  EyeOutlined
} from '@ant-design/icons-vue'
import { SysFileUpload } from '@/components/Upload'
import { PrivateFilePreviewModal } from '@/components/File'
import { getFileDownloadUrl } from '@/api/system/file'
import { formatFileSize } from '@/hooks/use-sys-file-upload'
import { useFilePreview } from '@/hooks/use-file-preview'
import type { FileInfoVO } from '@/api/wms/purchase-order/types'
import type { SysFileUploadResult } from '@/hooks/use-sys-file-upload'

interface LocalFileInfo {
  id?: number
  sysFileId: number
  fileName: string
  fileSize: number
  contentType: string
  isNew?: boolean
}

interface Props {
  // 其他附件文件ID列表
  otherFileIds: number[]
  // 已有的其他附件列表（详情返回）
  existingOtherFiles?: FileInfoVO[]
  // 是否禁用
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
})

const emits = defineEmits<{
  (e: 'update:otherFileIds', value: number[]): void
}>()

const newFileId = ref<number>()

// 新上传的文件列表
const newFiles = ref<LocalFileInfo[]>([])

// 合并的文件列表
const fileList = computed<LocalFileInfo[]>(() => {
  const existing: LocalFileInfo[] = (props.existingOtherFiles || [])
    .filter(f => props.otherFileIds.includes(f.sysFileId))
    .map(f => ({
      id: f.id,
      sysFileId: f.sysFileId,
      fileName: f.fileName,
      fileSize: f.fileSize,
      contentType: f.contentType
    }))

  return [...existing, ...newFiles.value]
})

// 获取文件图标
const getFileIcon = (contentType: string) => {
  if (contentType.startsWith('image/')) return FileImageOutlined
  if (contentType.includes('pdf')) return FilePdfOutlined
  if (contentType.includes('word') || contentType.includes('document')) return FileWordOutlined
  return FileOutlined
}

// 文件预览
const { state: previewState, openPreview, closePreview, downloadFile } = useFilePreview()

// 预览文件
const handlePreview = (file: LocalFileInfo) => {
  openPreview(file.sysFileId, file.fileName, file.contentType)
}

// 上传成功
const handleUploadSuccess = (result: SysFileUploadResult) => {
  newFiles.value.push({
    sysFileId: result.fileId,
    fileName: result.fileName,
    fileSize: result.fileSize,
    contentType: result.contentType,
    isNew: true
  })

  emits('update:otherFileIds', [...props.otherFileIds, result.fileId])
  newFileId.value = undefined
}

// 下载文件
const handleDownload = async (file: LocalFileInfo) => {
  try {
    const res = await getFileDownloadUrl(file.sysFileId)
    if (res.code === 200 && res.data) {
      window.open(res.data, '_blank')
    }
  } catch (error) {
    console.error('获取下载链接失败:', error)
  }
}

// 删除文件
const handleDelete = (file: LocalFileInfo) => {
  // 从新上传列表中移除
  const newIndex = newFiles.value.findIndex(f => f.sysFileId === file.sysFileId)
  if (newIndex > -1) {
    newFiles.value.splice(newIndex, 1)
  }

  // 从ID列表中移除
  const ids = props.otherFileIds.filter(id => id !== file.sysFileId)
  emits('update:otherFileIds', ids)
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

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title-inline {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: #262626;
}

.section-icon {
  margin-right: 8px;
  color: #1890ff;
  font-size: 16px;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
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
  font-size: 16px;
  color: #1890ff;
  flex-shrink: 0;
}

.file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
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

.empty-hint {
  color: #8c8c8c;
  font-size: 13px;
  text-align: center;
  padding: 24px 0;
}
</style>
