import type { AxiosResponse, AxiosResponseHeaders } from 'axios'

export const fileAbsoluteUrl = (
  relativePath: string,
  fileDomain = import.meta.env.VITE_OSS_DOMAIN
) => {
  if (!relativePath || relativePath.length === 0) return
  return relativePath.startsWith('/')
    ? `${fileDomain}${relativePath}`
    : `${fileDomain}/${relativePath}`
}

/**
 * Convert BASE64 to BLOB
 * @param base64Image Pass Base64 image data to convert into the BLOB
 */
export function convertBase64ToBlob(base64Image: string) {
  // Split into two parts
  const parts = base64Image.split(';base64,')

  // Hold the content type
  const imageType = parts[0].split(':')[1]

  // Decode Base64 string
  const decodedData = window.atob(parts[1])

  // Create UNIT8ARRAY of size same as row data length
  const uInt8Array = new Uint8Array(decodedData.length)

  // Insert all character code into uInt8Array
  for (let i = 0; i < decodedData.length; ++i) {
    uInt8Array[i] = decodedData.charCodeAt(i)
  }

  // Return BLOB image after conversion
  return new Blob([uInt8Array], { type: imageType })
}

/**
 * 远程文件下载
 * @param response
 * @param filename
 */
export function remoteFileDownload(response: AxiosResponse, filename?: string) {
  if (response.data) {
    // 构造一个blob对象来处理数据，并设置文件类型
    const headers = response.headers as AxiosResponseHeaders
    const contentType = headers['content-type']
    const blob = new Blob([response.data], { type: contentType })

    // 不存在则从响应头中解析
    if (!filename) {
      filename = resolveFilename(headers)
      console.log(filename)
    }
    fileDownload(blob, filename)
  }
}

/**
 * 根据 response header 解析文件名
 * 支持标准的 filename 和 RFC 5987 的 filename*
 * @param headers 响应头
 */
function resolveFilename(headers: AxiosResponseHeaders): string {
  const disposition = headers['content-disposition']
  if (!disposition) {
    return ''
  }

  // 优先尝试解析 RFC 5987 格式: filename*=UTF-8''encoded-filename
  const filenameStarMatch = disposition.match(/filename\*=([^']+)''(.+)/)
  if (filenameStarMatch && filenameStarMatch.length >= 3) {
    try {
      // filenameStarMatch[1] 是编码格式（如 UTF-8）
      // filenameStarMatch[2] 是 URL 编码的文件名
      return decodeURIComponent(filenameStarMatch[2])
    } catch (e) {
      console.error('解析 filename* 失败:', e)
    }
  }

  // 回退到标准格式: filename="filename" 或 filename=filename
  const filenameMatch = disposition.match(/filename=["']?([^"';]+)["']?/)
  if (filenameMatch && filenameMatch.length > 1) {
    try {
      return decodeURIComponent(filenameMatch[1])
    } catch (e) {
      // 如果 decodeURIComponent 失败，尝试 decodeURI
      try {
        return decodeURI(filenameMatch[1])
      } catch (e2) {
        // 都失败了，直接返回原始值
        return filenameMatch[1]
      }
    }
  }

  return ''
}

/**
 * 根据 blob 和 文件名进行文件下载
 * @param blob
 * @param filename
 */
export function fileDownload(blob: Blob, filename: string) {
  //兼容IE10，后续可以移除
  const navigator = window.navigator as any
  if (navigator.msSaveOrOpenBlob) {
    navigator.msSaveBlob(blob, filename)
  } else {
    const href = URL.createObjectURL(blob) //创建新的URL表示指定的blob对象
    const a = document.createElement('a') //创建a标签
    a.style.display = 'none'
    a.href = href // 指定下载链接
    a.download = filename //指定下载文件名
    a.click() //触发下载
    URL.revokeObjectURL(a.href) //释放URL对象
  }
}
// ==================== 文件名处理相关功能 ====================

/**
 * 生成唯一的文件名
 * @param originalName 原始文件名
 * @param addTimestamp 是否添加时间戳（默认true）
 * @returns 处理后的文件名
 */
export function generateUniqueFileName(originalName: string, addTimestamp = true): string {
  // 1. 提取文件扩展名
  const lastDotIndex = originalName.lastIndexOf('.')
  const name = lastDotIndex > 0 ? originalName.substring(0, lastDotIndex) : originalName
  const extension = lastDotIndex > 0 ? originalName.substring(lastDotIndex) : ''

  // 2. 清理文件名 - 移除特殊字符
  const cleanName = sanitizeFileName(name)

  // 3. 生成唯一标识
  if (addTimestamp) {
    const now = new Date()
    const timestamp = now.getTime() // 毫秒时间戳
    const dateStr = now.toISOString().slice(0, 19).replace(/[-:T]/g, '') // YYYYMMDDHHMMSS 格式
    return `${cleanName}_${dateStr}_${timestamp}${extension}`
  }

  return `${cleanName}${extension}`
}

/**
 * 清理文件名，移除特殊字符
 * @param fileName 原始文件名
 * @returns 清理后的文件名
 */
export function sanitizeFileName(fileName: string): string {
  if (!fileName) return fileName
  return fileName
    // 移除 Windows/Unix 文件名非法字符 \ / : * ? " < > |
    .replace(/[\\/:*?"<>|]/g, '_')
    // 移除控制字符
    // eslint-disable-next-line no-control-regex
    .replace(/[\x00-\x1f]/g, '')
    // 折叠多余空白并去除首尾空白/点
    .replace(/\s+/g, ' ')
    .replace(/^[\s.]+|[\s.]+$/g, '')
}

/**
 * 验证文件名是否安全
 * @param fileName 文件名
 * @returns 是否安全
 */
export function isFileNameSafe(fileName: string): boolean {
  // 检查是否包含危险字符
  const dangerousChars = /[<>:"/\\|?*\x00-\x1f\x80-\x9f]/
  if (dangerousChars.test(fileName)) {
    return false
  }

  // 检查是否为保留名称（Windows）
  const reservedNames = [
    'CON',
    'PRN',
    'AUX',
    'NUL',
    'COM1',
    'COM2',
    'COM3',
    'COM4',
    'COM5',
    'COM6',
    'COM7',
    'COM8',
    'COM9',
    'LPT1',
    'LPT2',
    'LPT3',
    'LPT4',
    'LPT5',
    'LPT6',
    'LPT7',
    'LPT8',
    'LPT9'
  ]

  const nameWithoutExt = fileName.split('.')[0].toUpperCase()
  if (reservedNames.includes(nameWithoutExt)) {
    return false
  }

  // 检查长度
  if (fileName.length === 0 || fileName.length > 255) {
    return false
  }

  return true
}

/**
 * 获取文件扩展名
 * @param fileName 文件名
 * @returns 扩展名（包含点）
 */
export function getFileExtension(fileName: string): string {
  const lastDotIndex = fileName.lastIndexOf('.')
  return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : ''
}

/**
 * 获取文件名（不包含扩展名）
 * @param fileName 文件名
 * @returns 文件名（不包含扩展名）
 */
export function getFileNameWithoutExtension(fileName: string): string {
  const lastDotIndex = fileName.lastIndexOf('.')
  return lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName
}

/**
 * 格式化文件大小
 * @param bytes 字节数
 * @returns 格式化后的文件大小
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'

  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 检查文件类型是否为图片
 * @param fileName 文件名或 MIME 类型
 * @returns 是否为图片
 */
export function isImageFile(fileName: string): boolean {
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg']
  const extension = getFileExtension(fileName).toLowerCase()
  return imageExtensions.includes(extension)
}

/**
 * 生成文件的哈希值（简单版本）
 * @param file 文件对象
 * @returns Promise<string> 文件哈希值
 */
export function generateFileHash(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()

    reader.onload = () => {
      const arrayBuffer = reader.result as ArrayBuffer
      const uint8Array = new Uint8Array(arrayBuffer)

      // 简单的哈希算法（实际项目中建议使用 crypto API）
      let hash = 0
      for (let i = 0; i < uint8Array.length; i++) {
        const char = uint8Array[i]
        hash = (hash << 5) - hash + char
        hash = hash & hash // 转换为32位整数
      }

      resolve(Math.abs(hash).toString(36))
    }

    reader.onerror = () => reject(new Error('读取文件失败'))
    reader.readAsArrayBuffer(file)
  })
} /**

 * 生成基于时间的唯一文件名（简化版）
 * @param originalName 原始文件名
 * @param useReadableTime 是否使用可读时间格式（默认false，使用时间戳）
 * @returns 处理后的文件名
 */
export function generateTimeBasedFileName(originalName: string, useReadableTime = true): string {
  // 1. 提取文件扩展名
  const lastDotIndex = originalName.lastIndexOf('.')
  const name = lastDotIndex > 0 ? originalName.substring(0, lastDotIndex) : originalName
  const extension = lastDotIndex > 0 ? originalName.substring(lastDotIndex) : ''

  // 2. 清理文件名
  const cleanName = sanitizeFileName(name)

  // 3. 生成时间标识
  const now = new Date()

  if (useReadableTime) {
    // 使用可读时间格式：YYYYMMDD_HHMMSS
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    const hours = String(now.getHours()).padStart(2, '0')
    const minutes = String(now.getMinutes()).padStart(2, '0')
    const seconds = String(now.getSeconds()).padStart(2, '0')
    const timeStr = `${year}${month}${day}_${hours}${minutes}${seconds}`
    return `${cleanName}_${timeStr}${extension}`
  } else {
    // 使用时间戳（毫秒）
    const timestamp = now.getTime()
    return `${cleanName}_${timestamp}${extension}`
  }
}
