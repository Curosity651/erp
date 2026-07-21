/**
 * OSS POST 上传签名响应（与后端 OssPostSignatureVO 匹配）
 */
export interface UploadSignatureVO {
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
  /** 安全令牌（STS临时凭证时有值） */
  securityToken?: string
  /** 上传目录 */
  dir: string
  /** 主机地址 */
  host: string
}

/**
 * 文件元数据保存DTO
 */
export interface SysFileUploadDTO {
  /** 桶别名（如 public-files、private-files） */
  bucketKey: string
  /** 对象键 */
  objectKey: string
  /** 原始文件名 */
  fileName: string
  /** 文件大小(字节) */
  fileSize: number
  /** MIME类型 */
  contentType: string
}

/**
 * 文件视图对象
 */
export interface SysFileVO {
  /** 文件ID */
  id: number
  /** 桶别名 */
  bucketKey: string
  /** 桶名称 */
  bucketName: string
  /** 对象键 */
  objectKey: string
  /** 原始文件名 */
  fileName: string
  /** 文件大小(字节) */
  fileSize: number
  /** MIME类型 */
  contentType: string
  /** 访问URL */
  url?: string
  /** 上传人 */
  createBy?: number
  /** 上传时间 */
  createTime: string
}
