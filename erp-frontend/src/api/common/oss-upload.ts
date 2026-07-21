import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'

// OSS 签名响应类型 - 对应服务端 OssPostSignatureVO
export interface OSSSignatureResponse {
  /** 签名版本 */
  version: string
  /** 上传策略 */
  policy: string
  /** OSS凭证 */
  xOssCredential: string
  /** OSS日期 */
  xOssDate: string
  /** 签名 */
  signature: string
  /** 安全令牌 */
  securityToken?: string
  /** 上传目录 */
  dir: string
  /** 主机地址 */
  host: string
}

/**
 * 获取 OSS 上传签名 - 基础API调用
 * @param bucketKey 桶别名，默认 public-files
 */
export const getOSSSignature = async (
  bucketKey: string = 'public-files'
): Promise<OSSSignatureResponse> => {
  const apiResult = await httpClient.get<ApiResult<OSSSignatureResponse>>(
    '/system/file/upload-signature',
    { params: { bucketKey } }
  )

  if (apiResult.code !== 200) {
    throw new Error(apiResult.message || '获取OSS签名失败')
  }

  return apiResult.data!
}

// 上传选项接口
export interface UploadOptions {
  // 是否添加时间戳防重复
  addTimestamp?: boolean
  // 是否清理文件名
  sanitizeFileName?: boolean
  // 自定义文件名前缀
  filePrefix?: string
  // 时间格式类型：'timestamp' | 'readable'
  timeFormat?: 'timestamp' | 'readable'
  // 文件夹路径，在 signatureData.dir 内的相对路径
  folder?: string
}

/**
 * OSS 直传上传文件 - 基础上传实现（纯净版，无业务逻辑）
 *
 * @param file 要上传的文件
 * @param signatureData OSS 签名数据
 * @param objectKey 完整的对象键名（包含路径和文件名）
 * @param onProgress 上传进度回调
 * @returns 上传结果
 */
export const uploadFileToOSS = async (
  file: File,
  signatureData: OSSSignatureResponse,
  objectKey: string,
  onProgress?: (percent: number) => void
): Promise<{ url: string; objectKey: string }> => {
  return new Promise((resolve, reject) => {
    // 创建 FormData
    const formData = new FormData()
    formData.append('success_action_status', '200')
    formData.append('policy', signatureData.policy)
    formData.append('x-oss-signature', signatureData.signature)
    formData.append('x-oss-signature-version', signatureData.version || 'OSS4-HMAC-SHA256')
    formData.append('x-oss-credential', signatureData.xOssCredential)
    formData.append('x-oss-date', signatureData.xOssDate)
    formData.append('key', objectKey)

    // 如果有安全令牌，添加它
    if (signatureData.securityToken) {
      formData.append('x-oss-security-token', signatureData.securityToken)
    }

    // file 必须为最后一个表单域
    formData.append('file', file)

    // 创建 XMLHttpRequest
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
      if (xhr.status === 200) {
        const fileUrl = `${signatureData.host}/${objectKey}`
        resolve({
          url: fileUrl,
          objectKey: objectKey
        })
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

    // 设置超时时间（120秒）
    xhr.timeout = 120000

    // 发送请求到 OSS
    xhr.open('POST', signatureData.host)
    xhr.send(formData)
  })
}
