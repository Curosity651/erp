import type { PageParam } from '@/api/types'

/**
 * 供应商
 */
export interface SupplierDTO {
  // 供应商ID
  id: number
  // 供应商编码，唯一标识
  supplierCode: string
  // 供应商名称
  name: string
  // 供应商所在城市
  city: string
  // 详细地址
  address: string
  // 税号
  taxNumber: string
  // 法人姓名
  legalPersonName: string
  // 法人电话
  legalPersonPhone: string
  // 业务联系人
  businessContactName: string
  // 业务联系人电话
  businessContactPhone: string
  // 业务联系人邮箱
  businessContactEmail: string
  // 公账账户名称
  publicAccountName: string
  // 公账银行
  publicBankName: string
  // 公账开户行地址
  publicBankAddress: string
  // 公账账户卡号（非必填）
  publicAccountNo: string
  // 私账账户名称
  privateAccountName: string
  // 私账银行
  privateBankName: string
  // 私账开户行地址
  privateBankAddress: string
  // 私账账户卡号（必填）
  privateAccountNo: string
  // 营业执照照片OSS key
  businessLicensePhoto: string
  // 状态（1-启用，0-停用）
  status: number
  // 备注
  remarks: string
}

export interface SupplierQO {
  // 供应商ID
  id?: number
  // 供应商编码，唯一标识
  supplierCode?: string
  // 供应商名称
  name?: string
  // 供应商所在城市
  city?: string
  // 详细地址
  address?: string
  // 税号
  taxNumber?: string
  // 法人姓名
  legalPersonName?: string
  // 法人电话
  legalPersonPhone?: string
  // 业务联系人
  businessContactName?: string
  // 业务联系人电话
  businessContactPhone?: string
  // 业务联系人邮箱
  businessContactEmail?: string
  // 公账账户名称
  publicAccountName?: string
  // 公账银行
  publicBankName?: string
  // 公账开户行地址
  publicBankAddress?: string
  // 公账账户卡号
  publicAccountNo?: string
  // 私账账户名称
  privateAccountName?: string
  // 私账银行
  privateBankName?: string
  // 私账开户行地址
  privateBankAddress?: string
  // 私账账户卡号
  privateAccountNo?: string
  // 营业执照照片OSS key
  businessLicensePhoto?: string
  // 状态（1-启用，0-停用）
  status?: number
  // 备注
  remarks?: string
}

/**
 * 供应商分页参数
 */
export type SupplierPageParam = SupplierQO & PageParam

/**
 * 供应商分页视图对象
 */
export interface SupplierPageVO extends SupplierDTO {
  // 创建时间
  createTime: string
  // 修改时间
  updateTime: string
}
