import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { validateFile, generateTimeBasedFileName } from '@/utils/upload-core'
import { getUploadSignature, saveFileMetadata } from '@/api/system/file'
import type { SysFileUploadDTO, UploadSignatureVO } from '@/api/system/file/types'
import { isSuccess } from '@/api'

/**
 * 文件上传选项
 */
export interface FileUploadOptions {
  /** 必填：目标桶别名 */
  bucketKey: string
  /** 可选：文件夹路径 */
  folder?: string
  /** 上传进度回调 */
  onProgress?: (percent: number) => void
  /** 允许的文件类型 */
  allowedTypes?: string[]
  /** 最大文件大小（字节） */
  maxSize?: number
}

/**
 * 文件上传结果
 */
export interface FileUploadResult {
  fileId: number
  objectKey: string
  fileName: string
  fileSize: number
  contentType: string
}

/**
 * 构建 objectKey
 */
function buildObjectKey(signature: UploadSignatureVO, fileName: string, folder?: string): string {
  const processedFileName = generateTimeBasedFileName(fileName)
  const folderPath = folder ? folder.replace(/^\/+|\/+$/g, '') + '/' : ''
  return signature.dir + folderPath + processedFileName
}

/**
 * 使用 POST 表单方式上传到 OSS
 */
function uploadToOSSByPost(
  file: File,
  signature: UploadSignatureVO,
  objectKey: string,
  onProgress?: (percent: number) => void
): Promise<string> {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('key', objectKey)
    formData.append('policy', signature.policy)
    formData.append('x-oss-signature', signature.signature)
    formData.append('x-oss-signature-version', signature.version)
    formData.append('x-oss-credential', signature.xOssCredential)
    formData.append('x-oss-date', signature.xOssDate)
    formData.append('success_action_status', '200')

    if (signature.securityToken) {
      formData.append('x-oss-security-token', signature.securityToken)
    }

    // file 必须是最后一个字段
    formData.append('file', file)

    const xhr = new XMLHttpRequest()

    if (onProgress) {
      xhr.upload.addEventListener('progress', e => {
        if (e.lengthComputable) {
          onProgress(Math.round((e.loaded / e.total) * 100))
        }
      })
    }

    xhr.addEventListener('load', () => {
      if (xhr.status === 200) {
        resolve(`${signature.host}/${objectKey}`)
      } else {
        reject(new Error(`上传失败: ${xhr.status}`))
      }
    })

    xhr.addEventListener('error', () => reject(new Error('网络错误')))
    xhr.addEventListener('timeout', () => reject(new Error('上传超时')))
    xhr.timeout = 120000

    xhr.open('POST', signature.host)
    xhr.send(formData)
  })
}

/**
 * 统一文件上传 Hook
 * 实现：获取签名 → POST 表单上传 OSS → 保存元数据
 */
export function useFileUpload() {
  const uploading = ref(false)
  const progress = ref(0)

  const uploadFile = async (
    file: File,
    options: FileUploadOptions
  ): Promise<FileUploadResult | null> => {
    const { bucketKey, folder, onProgress, allowedTypes, maxSize } = options

    // 校验
    const validation = validateFile(file, { allowedTypes, maxSize })
    if (!validation.valid) {
      message.error(validation.message)
      return null
    }

    uploading.value = true
    progress.value = 0

    try {
      // 1. 获取上传签名（只需 bucketKey）
      const signatureRes = await getUploadSignature(bucketKey)
      if (!isSuccess(signatureRes) || !signatureRes.data) {
        throw new Error(signatureRes.message || '获取上传签名失败')
      }
      const signature = signatureRes.data

      // 2. 构建 objectKey
      const objectKey = buildObjectKey(signature, file.name, folder)

      // 3. POST 表单上传到 OSS
      await uploadToOSSByPost(file, signature, objectKey, p => {
        progress.value = p
        onProgress?.(p)
      })

      // 4. 保存元数据
      const dto: SysFileUploadDTO = {
        bucketKey,
        objectKey,
        fileName: file.name,
        fileSize: file.size,
        contentType: file.type
      }
      const saveRes = await saveFileMetadata(dto)
      if (!isSuccess(saveRes) || !saveRes.data) {
        throw new Error(saveRes.message || '保存文件信息失败')
      }

      message.success('上传成功')
      return {
        fileId: saveRes.data,
        objectKey,
        fileName: file.name,
        fileSize: file.size,
        contentType: file.type
      }
    } catch (error: any) {
      console.error('文件上传失败:', error)
      message.error(error.message || '上传失败')
      return null
    } finally {
      uploading.value = false
    }
  }

  return { uploading, progress, uploadFile }
}
