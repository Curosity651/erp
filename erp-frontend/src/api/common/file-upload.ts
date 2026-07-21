import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'

/**
 * OSS上传Token
 */
export interface OssUploadTokenVO {
  // 临时访问密钥ID
  accessKeyId: string
  // 临时访问密钥Secret
  accessKeySecret: string
  // 安全令牌
  securityToken: string
  // OSS存储桶名称
  bucket: string
  // OSS区域
  region: string
  // 建议的OSS对象键
  ossKey: string
  // 上传地址
  uploadUrl: string
  // 过期时间戳
  expiration: number
  // 上传策略
  policy: string
  // 签名
  signature: string
}

/**
 * 文件信息DTO
 */
export interface FileInfoDTO {
  // 文件类型
  fileType: string
  // 文件名
  fileName: string
  // OSS对象键
  ossKey: string
  // 文件访问URL
  fileUrl: string
  // 文件大小(字节)
  fileSize: number
  // MIME类型
  mimeType: string
  // 排序
  sortOrder?: number
}

/**
 * 文件信息VO
 */
export interface FileInfoVO extends FileInfoDTO {
  // 文件ID
  id: number
  // 创建时间
  createTime: string
  // 创建人
  createBy: string
}

/**
 * 获取文件上传临时Token
 * @param bizType 业务类型，如 "sku"
 * @param fileType 文件类型
 */
export function getUploadToken(bizType: string, fileType: string) {
  return httpClient.get<ApiResult<OssUploadTokenVO>>('/common/file/upload-token', {
    params: { bizType, fileType }
  })
}

/**
 * 保存文件信息到数据库
 * @param skuId SKU ID
 * @param fileInfo 文件信息
 */
export function saveSkuFileInfo(skuId: number, fileInfo: FileInfoDTO) {
  return httpClient.post<ApiResult<FileInfoVO>>('/product/sku/files/save', fileInfo, {
    params: { skuId }
  })
}

/**
 * 删除SKU文件
 * @param fileId 文件ID
 */
export function deleteSkuFile(fileId: number) {
  return httpClient.post<ApiResult<void>>('/product/sku/files/delete', null, {
    params: { fileId }
  })
}

/**
 * 获取SKU文件列表
 * @param skuId SKU ID
 * @param fileType 文件类型（可选）
 */
export function getSkuFiles(skuId: number, fileType?: string) {
  return httpClient.get<ApiResult<FileInfoVO[]>>('/product/sku/files/list', {
    params: { skuId, fileType }
  })
}

/**
 * 更新文件排序
 * @param fileId 文件ID
 * @param sortOrder 排序值
 */
export function updateFileSortOrder(fileId: number, sortOrder: number) {
  return httpClient.post<ApiResult<void>>('/product/sku/files/update-sort', null, {
    params: { fileId, sortOrder }
  })
}

/**
 * 批量删除SKU文件
 * @param fileIds 文件ID列表
 */
export function batchDeleteSkuFiles(fileIds: number[]) {
  return httpClient.post<ApiResult<number>>('/product/sku/files/batch-delete', fileIds)
}

/**
 * 文件类型枚举
 */
export enum SkuFileType {
  PRODUCT_IMAGE = 'product_image', // 实物图片
  PLATFORM_IMAGE = 'platform_image', // 平台图片
  MODEL_3D = 'model_3d', // 3D模型文件
  MANUAL = 'manual', // 说明书文件
  INSTALL_VIDEO = 'install_video', // 安装视频
  ENGINEERING_DRAWING = 'engineering_drawing', // 工程图文件
  BOM = 'bom', // 物料清单文件
  BOX_MARK = 'box_mark', // 箱唛
  DROP_TEST_VIDEO = 'drop_test_video', // 摔箱视频
  PACKAGE_MANUAL = 'package_manual', // 包装说明书
  QUOTATION = 'quotation', // 报价单
  SAMPLE_CONFIRMATION = 'sample_confirmation', // 样品确认单
  QUALITY_REPORT = 'quality_report', // 质检报告
  PLATFORM_RENDER_VIDEO = 'platform_render_video' // 平台渲染视频
}

/**
 * 文件类型配置
 */
export const FILE_TYPE_CONFIG = {
  [SkuFileType.PRODUCT_IMAGE]: {
    name: '实物图片',
    accept: '.jpg,.jpeg,.png,.gif',
    multiple: true
  },
  [SkuFileType.PLATFORM_IMAGE]: {
    name: '平台图片',
    accept: '.jpg,.jpeg,.png,.gif',
    multiple: true
  },
  [SkuFileType.MODEL_3D]: {
    name: '3D模型文件',
    accept: '.obj,.fbx,.3ds,.dae',
    multiple: false
  },
  [SkuFileType.MANUAL]: {
    name: '说明书文件',
    accept: '.pdf,.doc,.docx',
    multiple: false
  },
  [SkuFileType.INSTALL_VIDEO]: {
    name: '安装视频',
    accept: '.mp4,.avi,.mov',
    multiple: false
  },
  [SkuFileType.ENGINEERING_DRAWING]: {
    name: '工程图文件',
    accept: '.pdf,.dwg,.dxf,.jpg,.png',
    multiple: true
  },
  [SkuFileType.BOM]: {
    name: '物料清单文件',
    accept: '.xls,.xlsx,.csv',
    multiple: false
  },
  [SkuFileType.BOX_MARK]: {
    name: '箱唛',
    accept: '.pdf',
    multiple: false
  },
  [SkuFileType.DROP_TEST_VIDEO]: {
    name: '摔箱视频',
    accept: '.mp4,.avi,.mov',
    multiple: false
  },
  [SkuFileType.PACKAGE_MANUAL]: {
    name: '包装说明书',
    accept: '.xls,.xlsx,.doc,.docx,.pdf',
    multiple: false
  },
  [SkuFileType.QUOTATION]: {
    name: '报价单',
    accept: '.pdf',
    multiple: false
  },
  [SkuFileType.SAMPLE_CONFIRMATION]: {
    name: '样品确认单',
    accept: '.xls,.xlsx,.doc,.docx,.pdf',
    multiple: false
  },
  [SkuFileType.QUALITY_REPORT]: {
    name: '质检报告',
    accept: '.pdf,.doc,.docx,.xls,.xlsx',
    multiple: false
  },
  [SkuFileType.PLATFORM_RENDER_VIDEO]: {
    name: '平台渲染视频',
    accept: '.mp4,.avi,.mov',
    multiple: false
  }
}
