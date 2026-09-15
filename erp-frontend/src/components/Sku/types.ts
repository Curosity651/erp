/**
 * SKU 选择组件共享类型定义
 */

/**
 * SKU 选择行数据（用于组件内部和事件传递）
 */
export interface SkuRow {
  /** 主键ID */
  id: number
  /** SKU编码 */
  skuCode: string
  /** 中文名 */
  chineseName?: string
  /** 主图URL */
  mainImage?: string
  /** 品类全路径名称 */
  categoryFullPath?: string
  /** 外箱长度（毫米） */
  outerLengthMm?: number
  /** 外箱宽度（毫米） */
  outerWidthMm?: number
  /** 外箱高度（毫米） */
  outerHeightMm?: number
}

/**
 * SKU 选择弹窗组件 Props
 */
export interface SkuSelectModalDialogProps {
  /** 弹窗是否可见 */
  visible: boolean
  /** 是否多选，默认 false */
  multiple?: boolean
  /** 初始选中的 SKU 编码列表（用于回显） */
  initialSelectedCodes?: string[]
}

/**
 * SKU 选择弹窗组件 Emits
 */
export interface SkuSelectModalDialogEmits {
  (e: 'update:visible', value: boolean): void
  (e: 'confirm', rows: SkuRow[]): void
}

/**
 * SKU 选择输入框组件 Props
 */
export interface SkuSelectModalProps {
  /** 当前选中SKU：单选为 string，多选为 string[] */
  modelValue?: string | string[]
  /** 是否多选，默认 false */
  multiple?: boolean
  /** 占位文本 */
  placeholder?: string
}

/**
 * SKU 选择输入框组件 Emits
 */
export interface SkuSelectModalEmits {
  (e: 'update:modelValue', value: string | string[] | undefined): void
  (e: 'sku-selected', row: SkuRow | SkuRow[] | null): void
}
