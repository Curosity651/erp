import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { usePrivateFile } from './use-private-file'

/**
 * 文件预览类型
 */
export type FilePreviewType = 'image' | 'pdf' | 'word' | 'excel' | 'unsupported'

/**
 * 文件预览状态
 */
export interface FilePreviewState {
  /** 弹窗是否可见 */
  open: boolean
  /** 文件ID */
  fileId: number | null
  /** 文件名 */
  fileName: string
  /** 文件类型 */
  fileType: FilePreviewType
  /** 签名URL */
  fileUrl: string | null
  /** 加载中 */
  loading: boolean
  /** 错误信息 */
  error: string | null
}

/**
 * 根据文件名或 contentType 检测文件预览类型
 */
export function detectFileType(fileName: string, contentType?: string): FilePreviewType {
  // 优先使用 contentType（更准确）
  if (contentType) {
    const type = contentType.toLowerCase()
    if (type.startsWith('image/')) return 'image'
    if (type.includes('pdf')) return 'pdf'
    if (type.includes('word') || type.includes('document')) return 'word'
    if (type.includes('sheet') || type.includes('excel')) return 'excel'
  }

  // 降级到文件扩展名
  const ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()
  const extMap: Record<string, FilePreviewType> = {
    // 图片
    '.jpg': 'image',
    '.jpeg': 'image',
    '.png': 'image',
    '.gif': 'image',
    '.webp': 'image',
    '.bmp': 'image',
    // PDF
    '.pdf': 'pdf',
    // Word
    '.doc': 'word',
    '.docx': 'word',
    // Excel
    '.xls': 'excel',
    '.xlsx': 'excel'
  }

  return extMap[ext] || 'unsupported'
}

/**
 * 判断文件是否支持预览
 */
export function isFilePreviewable(fileName: string, contentType?: string): boolean {
  return detectFileType(fileName, contentType) !== 'unsupported'
}

/**
 * 文件预览 Hook
 * 提供私有文件的预览状态管理和签名 URL 获取
 */
export function useFilePreview() {
  const { getSignedUrl } = usePrivateFile()

  const state = ref<FilePreviewState>({
    open: false,
    fileId: null,
    fileName: '',
    fileType: 'unsupported',
    fileUrl: null,
    loading: false,
    error: null
  })

  /** 是否可预览 */
  const canPreview = computed(() => state.value.fileType !== 'unsupported')

  /**
   * 打开预览
   * @param fileId 文件ID
   * @param fileName 文件名
   * @param contentType MIME类型（可选，用于更准确的类型判断）
   */
  const openPreview = async (fileId: number, fileName: string, contentType?: string) => {
    if (!fileId) {
      message.error('文件ID不存在')
      return
    }

    // 重置状态并显示弹窗
    state.value = {
      open: true,
      fileId,
      fileName,
      fileType: detectFileType(fileName, contentType),
      fileUrl: null,
      loading: true,
      error: null
    }

    try {
      // 获取签名 URL
      const url = await getSignedUrl(fileId)
      if (url) {
        state.value.fileUrl = url
      } else {
        state.value.error = '获取文件访问地址失败'
      }
    } catch (err) {
      console.error('文件预览加载失败:', err)
      state.value.error = '加载文件失败，请稍后重试'
    } finally {
      state.value.loading = false
    }
  }

  /**
   * 关闭预览
   */
  const closePreview = () => {
    state.value = {
      open: false,
      fileId: null,
      fileName: '',
      fileType: 'unsupported',
      fileUrl: null,
      loading: false,
      error: null
    }
  }

  /**
   * 下载当前预览的文件
   */
  const downloadFile = async () => {
    if (!state.value.fileId) {
      message.error('文件ID不存在')
      return
    }

    try {
      // 如果已有 URL 且未过期，直接使用
      let url = state.value.fileUrl
      if (!url) {
        url = await getSignedUrl(state.value.fileId)
      }

      if (url) {
        window.open(url, '_blank')
      } else {
        message.error('获取下载地址失败')
      }
    } catch (err) {
      console.error('下载文件失败:', err)
      message.error('下载失败，请稍后重试')
    }
  }

  return {
    state,
    canPreview,
    openPreview,
    closePreview,
    downloadFile
  }
}
