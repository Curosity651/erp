import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'
import type { SysFileUploadDTO, SysFileVO, UploadSignatureVO } from './types'

/**
 * 获取上传签名（只需 bucketKey 参数）
 * @param bucketKey 桶别名，如 public-files、private-files
 */
export function getUploadSignature(bucketKey: string) {
  return httpClient.get<ApiResult<UploadSignatureVO>>('/system/file/upload-signature', {
    params: { bucketKey }
  })
}

/**
 * 保存文件元数据
 * @param dto 文件元数据
 */
export function saveFileMetadata(dto: SysFileUploadDTO) {
  return httpClient.post<ApiResult<number>>('/system/file', dto)
}

/**
 * 获取文件信息
 * @param id 文件ID
 */
export function getFileInfo(id: number) {
  return httpClient.get<ApiResult<SysFileVO>>('/system/file/info', {
    params: { id }
  })
}

/**
 * 获取文件下载URL
 * @param id 文件ID
 */
export function getFileDownloadUrl(id: number) {
  return httpClient.get<ApiResult<string>>('/system/file/download-url', {
    params: { id }
  })
}

/**
 * 批量获取下载URL
 * @param ids 文件ID列表
 */
export function batchGetDownloadUrls(ids: number[]) {
  return httpClient.post<ApiResult<Record<number, string>>>('/system/file/batch-download-urls', ids)
}

/**
 * 删除文件
 * @param id 文件ID
 */
export function deleteFile(id: number) {
  return httpClient.delete<ApiResult<void>>('/system/file', {
    params: { id }
  })
}
