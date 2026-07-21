<template>
  <div class="sys-file-upload">
    <a-upload
      v-if="!hideUploadButton"
      :custom-request="handleCustomRequest"
      :before-upload="handleBeforeUpload"
      :show-upload-list="false"
      :accept="acceptTypes"
      :disabled="disabled || uploading"
    >
      <slot>
        <a-button :loading="uploading" :disabled="disabled">
          <template #icon><UploadOutlined /></template>
          {{ buttonText }}
        </a-button>
      </slot>
    </a-upload>

    <!-- 上传进度 -->
    <div v-if="uploading && showProgress" class="upload-progress">
      <a-progress :percent="progress" size="small" />
    </div>

    <!-- 已上传文件展示 -->
    <div v-if="fileInfo && showFileInfo" class="uploaded-file">
      <div class="file-info">
        <component :is="fileIconComponent" class="file-icon" />
        <span class="file-name" :title="fileInfo.fileName">{{ fileInfo.fileName }}</span>
        <span class="file-size">{{ formatFileSize(fileInfo.fileSize) }}</span>
      </div>
      <div class="file-actions">
        <a-button type="link" size="small" title="预览" @click="handlePreview">
          <EyeOutlined />
        </a-button>
        <a-button type="link" size="small" title="下载" @click="handleDownload">
          <DownloadOutlined />
        </a-button>
        <a-button
          v-if="!disabled && allowDelete"
          type="link"
          size="small"
          danger
          title="删除"
          @click="handleDelete"
        >
          <DeleteOutlined />
        </a-button>
      </div>
    </div>

    <!-- 文件预览弹窗 -->
    <private-file-preview-modal
      v-model:open="previewState.open"
      :file-id="previewState.fileId"
      :file-name="previewState.fileName"
      :file-type="previewState.fileType"
      :file-url="previewState.fileUrl"
      :loading="previewState.loading"
      :error="previewState.error"
      @close="closePreview"
      @download="downloadPreviewFile"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  UploadOutlined,
  DownloadOutlined,
  DeleteOutlined,
  EyeOutlined,
  FileImageOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  FileOutlined
} from '@ant-design/icons-vue'
import {
  useSysFileUpload,
  formatFileSize,
  getFileIcon,
  ALLOWED_FILE_TYPES,
  MAX_FILE_SIZE
} from '@/hooks/use-sys-file-upload'
import { useFilePreview } from '@/hooks/use-file-preview'
import { getFileDownloadUrl, getFileInfo } from '@/api/system/file'
import type { SysFileVO } from '@/api/system/file/types'
import type { SysFileUploadResult } from '@/hooks/use-sys-file-upload'
import { isSuccess } from '@/api'
import PrivateFilePreviewModal from '@/components/File/PrivateFilePreviewModal.vue'

interface Props {
  /** 文件ID（v-model） */
  modelValue?: number
  /** 桶别名，默认 'private-files' */
  bucketKey?: string
  /** 按钮文字 */
  buttonText?: string
  /** 允许的文件类型 */
  allowedTypes?: string[]
  /** 最大文件大小（字节） */
  maxSize?: number
  /** 是否禁用 */
  disabled?: boolean
  /** 是否显示进度条 */
  showProgress?: boolean
  /** 是否显示文件信息 */
  showFileInfo?: boolean
  /** 是否允许删除 */
  allowDelete?: boolean
  /** 是否隐藏上传按钮 */
  hideUploadButton?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  bucketKey: 'private-files',
  buttonText: '上传文件',
  allowedTypes: () => ALLOWED_FILE_TYPES,
  maxSize: MAX_FILE_SIZE,
  disabled: false,
  showProgress: true,
  showFileInfo: true,
  allowDelete: true,
  hideUploadButton: false
})

const emits = defineEmits<{
  (e: 'update:modelValue', value: number | undefined): void
  (e: 'success', result: SysFileUploadResult): void
  (e: 'delete'): void
}>()

const { uploading, progress, uploadFile, validateFile } = useSysFileUpload()

// 文件预览
const {
  state: previewState,
  openPreview,
  closePreview,
  downloadFile: downloadPreviewFile
} = useFilePreview()

const fileInfo = ref<SysFileVO | null>(null)

// 计算接受的文件类型
const acceptTypes = computed(() => {
  return props.allowedTypes.join(',')
})

// 文件图标组件
const fileIconComponent = computed(() => {
  if (!fileInfo.value) return FileOutlined
  const iconType = getFileIcon(fileInfo.value.contentType)
  switch (iconType) {
    case 'file-image':
      return FileImageOutlined
    case 'file-pdf':
      return FilePdfOutlined
    case 'file-word':
      return FileWordOutlined
    default:
      return FileOutlined
  }
})

// 加载文件信息
const loadFileInfo = async (fileId: number) => {
  try {
    const res = await getFileInfo(fileId)
    if (isSuccess(res) && res.data) {
      fileInfo.value = res.data
    }
  } catch (error) {
    console.error('加载文件信息失败:', error)
  }
}

// 监听 modelValue 变化，加载文件信息
watch(
  () => props.modelValue,
  async newValue => {
    if (newValue) {
      await loadFileInfo(newValue)
    } else {
      fileInfo.value = null
    }
  },
  { immediate: true }
)

// 上传前校验
const handleBeforeUpload = (file: File): boolean => {
  return validateFile(file, {
    allowedTypes: props.allowedTypes,
    maxSize: props.maxSize
  })
}

// 自定义上传
const handleCustomRequest = async (options: any) => {
  const { file } = options

  const result = await uploadFile(file, {
    bucketKey: props.bucketKey,
    allowedTypes: props.allowedTypes,
    maxSize: props.maxSize
  })

  if (result) {
    emits('update:modelValue', result.fileId)
    emits('success', result)
    // 重新加载文件信息
    await loadFileInfo(result.fileId)
  }
}

// 下载文件
const handleDownload = async () => {
  if (!props.modelValue) return

  try {
    const res = await getFileDownloadUrl(props.modelValue)
    if (isSuccess(res) && res.data) {
      window.open(res.data, '_blank')
    }
  } catch (error) {
    console.error('获取下载链接失败:', error)
  }
}

// 预览文件
const handlePreview = () => {
  if (!fileInfo.value || !props.modelValue) return
  openPreview(props.modelValue, fileInfo.value.fileName, fileInfo.value.contentType)
}

// 删除文件
const handleDelete = () => {
  emits('update:modelValue', undefined)
  fileInfo.value = null
  emits('delete')
}

// 暴露方法
defineExpose({
  uploading,
  progress,
  fileInfo
})
</script>

<style scoped>
.sys-file-upload {
  display: inline-block;
}

.upload-progress {
  margin-top: 8px;
  width: 200px;
}

.uploaded-file {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding: 8px 12px;
  background: #fafafa;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  max-width: 400px;
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
  color: #999;
  font-size: 12px;
  flex-shrink: 0;
}

.file-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
</style>
