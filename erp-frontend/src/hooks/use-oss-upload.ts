import { ref, reactive } from 'vue'
import {
  getOSSSignature,
  uploadFileToOSS,
  type OSSSignatureResponse,
  type UploadOptions
} from '@/api/common/oss-upload'
import { generateTimeBasedFileName, sanitizeFileName, isFileNameSafe } from '@/utils/file-utils'

// 缓存的签名数据接口
interface CachedSignatureData {
  signature: OSSSignatureResponse
  expireTime: number
}

/**
 * OSS 签名缓存管理器 - 业务逻辑层
 */
class OSSSignatureCacheManager {
  private signatureCache: CachedSignatureData | null = null
  private readonly CACHE_DURATION = 15 * 60 * 1000 // 15分钟缓存时间

  /**
   * 获取有效的OSS签名，优先使用缓存
   */
  async getValidSignature(): Promise<OSSSignatureResponse> {
    // 检查缓存是否有效
    if (this.isSignatureCacheValid()) {
      console.log('使用缓存的 OSS 签名')
      return this.signatureCache!.signature
    }

    // 缓存无效，重新获取签名
    console.log('获取新的 OSS 签名')
    const signature = await getOSSSignature()

    // 缓存新签名
    this.signatureCache = {
      signature,
      expireTime: Date.now() + this.CACHE_DURATION
    }

    return signature
  }

  /**
   * 检查签名缓存是否有效
   */
  private isSignatureCacheValid(): boolean {
    return this.signatureCache !== null && Date.now() < this.signatureCache.expireTime
  }

  /**
   * 手动清除签名缓存
   */
  clearSignatureCache(): void {
    this.signatureCache = null
    console.log('OSS 签名缓存已清除')
  }

  /**
   * 获取缓存状态信息
   */
  getCacheStatus(): { isCached: boolean; expiresIn?: number } {
    if (!this.signatureCache) {
      return { isCached: false }
    }

    const expiresIn = this.signatureCache.expireTime - Date.now()
    return {
      isCached: expiresIn > 0,
      expiresIn: Math.max(0, Math.floor(expiresIn / 1000))
    }
  }
}

// 创建全局单例
const ossSignatureCacheManager = new OSSSignatureCacheManager()

/**
 * 文件名处理逻辑
 */
const processFileName = (file: File, options: UploadOptions): string => {
  const {
    addTimestamp = true,
    sanitizeFileName: shouldSanitize = true,
    filePrefix = '',
    timeFormat = 'timestamp'
  } = options

  let processedFileName = file.name

  if (shouldSanitize) {
    // 检查原始文件名是否安全
    if (!isFileNameSafe(file.name)) {
      console.warn('检测到不安全的文件名，将进行清理:', file.name)
    }

    // 根据时间格式选择生成方法
    if (addTimestamp) {
      if (timeFormat === 'readable') {
        processedFileName = generateTimeBasedFileName(file.name, true)
      } else {
        processedFileName = generateTimeBasedFileName(file.name, false)
      }
    } else {
      // 只清理，不添加时间戳
      const lastDotIndex = file.name.lastIndexOf('.')
      const name = lastDotIndex > 0 ? file.name.substring(0, lastDotIndex) : file.name
      const extension = lastDotIndex > 0 ? file.name.substring(lastDotIndex) : ''
      processedFileName = sanitizeFileName(name) + extension
    }
  }

  // 添加自定义前缀
  if (filePrefix) {
    const sanitizedPrefix = sanitizeFileName(filePrefix)
    processedFileName = `${sanitizedPrefix}_${processedFileName}`
  }

  return processedFileName
}

/**
 * 文件夹路径处理逻辑
 */
const processFolderPath = (folder: string): string => {
  if (!folder) return ''

  // 清理文件夹路径，确保格式正确
  let folderPath = folder
    .replace(/^\/+|\/+$/g, '') // 移除开头和结尾的斜杠
    .replace(/\/+/g, '/') // 将多个连续斜杠替换为单个斜杠

  // 如果文件夹路径不为空，确保以斜杠结尾
  if (folderPath) {
    folderPath = folderPath + '/'
  }

  return folderPath
}

/**
 * 构建完整的对象键名
 */
const buildObjectKey = (
  file: File,
  signatureData: OSSSignatureResponse,
  options: UploadOptions
): { objectKey: string; processedFileName: string } => {
  const processedFileName = processFileName(file, options)
  const folderPath = processFolderPath(options.folder || '')

  // 构建完整的 objectKey（基础路径 + 文件夹 + 文件名）
  const objectKey = signatureData.dir + folderPath + processedFileName

  console.log('文件名处理:', {
    original: file.name,
    processed: processedFileName,
    folder: folderPath,
    objectKey: objectKey,
    options
  })

  return { objectKey, processedFileName }
}

/**
 * 兼容函数：带缓存的上传 - 供不使用Hook的代码调用
 */
export const uploadToOSSWithCache = async (
  file: File,
  options: UploadOptions = {},
  onProgress?: (percent: number) => void
): Promise<{ url: string; fileName: string; originalName: string; objectKey: string }> => {
  // 1. 获取签名（带缓存）
  const signatureData = await ossSignatureCacheManager.getValidSignature()

  // 2. 处理文件名和路径
  const { objectKey, processedFileName } = buildObjectKey(file, signatureData, options)

  // 3. 执行上传
  const result = await uploadFileToOSS(file, signatureData, objectKey, onProgress)

  // 4. 返回完整结果
  return {
    url: result.url,
    fileName: processedFileName,
    originalName: file.name,
    objectKey: result.objectKey
  }
}

/**
 * 兼容函数：完整的上传逻辑 - 供旧代码调用
 */
export const uploadToOSS = async (
  file: File,
  signatureData: OSSSignatureResponse,
  options: UploadOptions = {},
  onProgress?: (percent: number) => void
): Promise<{ url: string; fileName: string; originalName: string; objectKey: string }> => {
  // 1. 处理文件名和路径
  const { objectKey, processedFileName } = buildObjectKey(file, signatureData, options)

  // 2. 执行上传
  const result = await uploadFileToOSS(file, signatureData, objectKey, onProgress)

  // 3. 返回完整结果
  return {
    url: result.url,
    fileName: processedFileName,
    originalName: file.name,
    objectKey: result.objectKey
  }
}

/**
 * 导出缓存管理器实例（供外部使用）
 */
export { ossSignatureCacheManager as ossUploadManager }
