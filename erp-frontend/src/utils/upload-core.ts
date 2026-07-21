/**
 * 上传核心工具
 * 提供 XHR 上传、文件校验、格式化等公共功能
 */

/**
 * XHR 上传配置
 */
export interface XhrUploadOptions {
  url: string
  method?: 'POST' | 'PUT'
  headers?: Record<string, string>
  body: FormData | File
  timeout?: number
  onProgress?: (percent: number) => void
}

/**
 * XHR 上传核心逻辑
 */
export function xhrUpload(options: XhrUploadOptions): Promise<void> {
  const { url, method = 'POST', headers, body, timeout = 120000, onProgress } = options

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()

    // 监听上传进度
    if (onProgress) {
      xhr.upload.addEventListener('progress', event => {
        if (event.lengthComputable) {
          const percent = Math.round((event.loaded / event.total) * 100)
          onProgress(percent)
        }
      })
    }

    // 监听请求完成
    xhr.addEventListener('load', () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve()
      } else {
        reject(new Error(`上传失败: ${xhr.status} ${xhr.statusText}`))
      }
    })

    // 监听请求错误
    xhr.addEventListener('error', () => {
      reject(new Error('网络错误，上传失败'))
    })

    // 监听请求超时
    xhr.addEventListener('timeout', () => {
      reject(new Error('上传超时'))
    })

    xhr.timeout = timeout
    xhr.open(method, url)

    // 设置请求头
    if (headers) {
      Object.entries(headers).forEach(([key, value]) => {
        xhr.setRequestHeader(key, value)
      })
    }

    xhr.send(body)
  })
}

/**
 * 文件校验选项
 */
export interface FileValidateOptions {
  allowedTypes?: string[]
  maxSize?: number
}

/**
 * 文件校验结果
 */
export interface FileValidateResult {
  valid: boolean
  message?: string
}

/**
 * 允许的文件类型（默认）
 */
export const DEFAULT_ALLOWED_TYPES = [
  'application/pdf',
  'image/jpeg',
  'image/jpg',
  'image/png',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
]

/**
 * 最大文件大小（默认 10MB）
 */
export const DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024

/**
 * 文件类型描述映射
 */
export const FILE_TYPE_LABELS: Record<string, string> = {
  'application/pdf': 'PDF',
  'image/jpeg': 'JPEG图片',
  'image/jpg': 'JPG图片',
  'image/png': 'PNG图片',
  'application/msword': 'Word文档',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'Word文档'
}

/**
 * 校验文件
 */
export function validateFile(file: File, options?: FileValidateOptions): FileValidateResult {
  const { allowedTypes = DEFAULT_ALLOWED_TYPES, maxSize = DEFAULT_MAX_FILE_SIZE } = options || {}

  // 校验文件类型
  if (!allowedTypes.includes(file.type)) {
    const allowedLabels = allowedTypes.map(t => FILE_TYPE_LABELS[t] || t).join('、')
    return {
      valid: false,
      message: `不支持的文件类型，仅支持：${allowedLabels}`
    }
  }

  // 校验文件大小
  if (file.size > maxSize) {
    const maxSizeMB = (maxSize / 1024 / 1024).toFixed(0)
    return {
      valid: false,
      message: `文件大小不能超过 ${maxSizeMB}MB`
    }
  }

  return { valid: true }
}

/**
 * 格式化文件大小
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 获取文件扩展名
 */
export function getFileExtension(fileName: string): string {
  const lastDotIndex = fileName.lastIndexOf('.')
  return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1).toLowerCase() : ''
}

/**
 * 根据 MIME 类型获取文件图标类型
 */
export function getFileIconType(contentType: string): string {
  if (contentType.startsWith('image/')) {
    return 'file-image'
  }
  if (contentType === 'application/pdf') {
    return 'file-pdf'
  }
  if (
    contentType === 'application/msword' ||
    contentType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  ) {
    return 'file-word'
  }
  return 'file'
}

/**
 * 生成基于时间的文件名
 * 格式：yyyyMMdd_HHmmss_随机数_原始文件名
 */
export function generateTimeBasedFileName(originalFileName: string): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  const random = Math.random().toString(36).substring(2, 8)

  const ext = getFileExtension(originalFileName)
  const baseName =
    originalFileName.substring(0, originalFileName.lastIndexOf('.')) || originalFileName

  // 清理文件名中的特殊字符
  const cleanBaseName = baseName.replace(/[^\w\u4e00-\u9fa5.-]/g, '_')

  return `${year}${month}${day}_${hours}${minutes}${seconds}_${random}_${cleanBaseName}.${ext}`
}
