/**
 * SKU 简要信息 VO
 * 与后端 com.erp.admin.product.model.vo.SkuBriefVO 完全一致
 */
export interface SkuBriefVO {
  /** SKU 编码 */
  skuCode: string
  /** SKU 名称 */
  skuName?: string
  /** 主图 URL */
  mainImage?: string
  /** SKU 编号 */
  skuNo?: number
}
