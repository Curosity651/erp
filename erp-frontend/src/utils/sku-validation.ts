import type { Rule } from 'ant-design-vue/es/form'
import httpClient from '@/utils/axios'
import type { ApiResult } from '@/api/types'

/**
 * SKU业务验证规则
 */
export class SkuValidationRules {
  /**
   * SKU编码验证规则
   */
  static skuCodeRules(excludeId?: number): Rule[] {
    return [
      { required: true, message: 'SKU编码不能为空' },
      { min: 3, max: 50, message: 'SKU编码长度应在3-50个字符之间' },
      { pattern: /^[A-Z0-9_-]+$/, message: 'SKU编码只能包含大写字母、数字、下划线和中划线' },
      {
        validator: async (rule, value) => {
          if (!value || value.trim() === '') {
            return Promise.resolve()
          }

          try {
            const response = await httpClient.get<ApiResult<boolean>>(
              '/product/sku/validate-code',
              {
                params: { code: value.trim(), excludeId }
              }
            )
            if (response.data === false) {
              return Promise.reject(new Error('SKU编码已存在'))
            }
            return Promise.resolve()
          } catch (error) {
            console.warn('SKU编码验证失败:', error)
            // 验证失败时不阻止提交，由后端最终验证
            return Promise.resolve()
          }
        },
        trigger: 'blur'
      }
    ]
  }

  /**
   * SPU编码验证规则
   */
  static spuCodeRules(): Rule[] {
    return [
      { required: true, message: 'SPU编码不能为空' },
      { min: 3, max: 50, message: 'SPU编码长度应在3-50个字符之间' },
      { pattern: /^[A-Z0-9_-]+$/, message: 'SPU编码只能包含大写字母、数字、下划线和中划线' }
    ]
  }

  /**
   * SKU序号验证规则
   */
  static skuNoRules(excludeId?: number): Rule[] {
    return [
      { required: true, message: 'SKU序号不能为空' },
      { type: 'number', min: 1, message: 'SKU序号必须大于0' },
      { type: 'number', max: 999999, message: 'SKU序号不能超过999999' }
    ]
  }

  /**
   * 销售国家验证规则
   */
  static salesCountryRules(): Rule[] {
    return [{ required: true, message: '请选择销售国家' }]
  }

  /**
   * 品类验证规则
   */
  static categoryRules(): Rule[] {
    return [{ required: true, message: '请选择品类' }]
  }

  /**
   * 产品状态验证规则
   */
  static productStatusRules(): Rule[] {
    return [
      { required: true, message: '请选择产品状态' },
      { type: 'number', min: 1, max: 4, message: '产品状态值无效' }
    ]
  }

  /**
   * 头程验证规则
   */
  static shippingTypeRules(): Rule[] {
    return [
      { required: true, message: '请选择头程类型' },
      { type: 'number', min: 1, max: 2, message: '头程类型值无效' }
    ]
  }

  /**
   * 品牌验证规则
   */
  static brandRules(): Rule[] {
    return [{ max: 100, message: '品牌名称不能超过100个字符' }]
  }

  /**
   * 项目组验证规则
   */
  static projectGroupRules(): Rule[] {
    return [{ max: 100, message: '项目组名称不能超过100个字符' }]
  }

  /**
   * 产品描述验证规则
   */
  static descriptionRules(): Rule[] {
    return [{ max: 1000, message: '产品描述不能超过1000个字符' }]
  }

  /**
   * 中文名验证规则
   */
  static chineseNameRules(): Rule[] {
    return [{ max: 200, message: '中文名不能超过200个字符' }]
  }

  /**
   * 俄文名验证规则
   */
  static russianNameRules(): Rule[] {
    return [{ max: 200, message: '俄文名不能超过200个字符' }]
  }

  /**
   * 海关申报名验证规则
   */
  static customsDeclarationNameRules(): Rule[] {
    return [{ max: 200, message: '海关申报名不能超过200个字符' }]
  }

  /**
   * 海关申报代码验证规则
   */
  static customsDeclarationCodeRules(): Rule[] {
    return [{ max: 100, message: '海关申报代码不能超过100个字符' }]
  }

  /**
   * 重量验证规则
   */
  static weightRules(): Rule[] {
    return [
      { type: 'number', min: 0, message: '重量必须大于等于0' },
      { type: 'number', max: 999999.999, message: '重量不能超过999999.999' }
    ]
  }

  /**
   * 包装尺寸验证规则
   */
  static packageDimensionRules(): Rule[] {
    return [{ type: 'number', min: 0, message: '尺寸必须大于等于0' }]
  }

  /**
   * 供应商编码验证规则
   */
  static supplierCodeRules(): Rule[] {
    return [{ max: 100, message: '供应商编码不能超过100个字符' }]
  }

  /**
   * 税率验证规则
   */
  static taxRateRules(includeTax: boolean): Rule[] {
    const rules: Rule[] = []

    if (includeTax) {
      rules.unshift({ required: true, message: '含税时税率不能为空' })
    }

    return rules
  }

  /**
   * 采购价验证规则
   */
  static purchasePriceRules(): Rule[] {
    return [
      { type: 'number', min: 0, message: '采购价必须大于等于0' },
      { type: 'number', max: 9999999999.99, message: '采购价不能超过9999999999.99' }
    ]
  }

  /**
   * 起订量验证规则
   */
  static minimumOrderQuantityRules(): Rule[] {
    return [
      { type: 'integer', min: 1, message: '起订量必须为正整数' },
      { type: 'integer', max: 999999999, message: '起订量不能超过999999999' }
    ]
  }

  /**
   * 生产周期验证规则
   */
  static productionCycleRules(): Rule[] {
    return [
      { type: 'integer', min: 1, message: '生产周期必须为正整数' },
      { type: 'integer', max: 9999, message: '生产周期不能超过9999天' }
    ]
  }

  /**
   * 岗位负责人验证规则
   */
  static positionUserRules(): Rule[] {
    return [{ max: 64, message: '用户ID不能超过64个字符' }]
  }
}

/**
 * 表单验证工具类
 */
export class SkuFormValidator {
  /**
   * 验证包装尺寸的完整性
   */
  static validatePackageDimensions(
    length?: number,
    width?: number,
    height?: number
  ): string | null {
    const hasAny = length || width || height
    const hasAll = length && width && height

    if (hasAny && !hasAll) {
      return '包装尺寸必须同时填写长度、宽度和高度'
    }

    return null
  }

  /**
   * 验证含税和税率的关联性
   */
  static validateTaxRelation(includeTax: boolean, taxRate?: number): string | null {
    if (includeTax && (taxRate === undefined || taxRate === null)) {
      return '选择含税时必须填写税率'
    }

    if (!includeTax && taxRate !== undefined && taxRate !== null) {
      return '未选择含税时不应填写税率'
    }

    return null
  }

  /**
   * 验证SKU编码格式
   */
  static validateSkuCodeFormat(code: string): string | null {
    if (!code || code.trim() === '') {
      return 'SKU编码不能为空'
    }

    const trimmedCode = code.trim()

    if (trimmedCode.length < 3 || trimmedCode.length > 50) {
      return 'SKU编码长度应在3-50个字符之间'
    }

    if (!/^[A-Z0-9_-]+$/.test(trimmedCode)) {
      return 'SKU编码只能包含大写字母、数字、下划线和中划线'
    }

    return null
  }

  /**
   * 验证SKU序号格式
   */
  static validateSkuNoFormat(skuNo: number | undefined | null): string | null {
    if (skuNo === undefined || skuNo === null) {
      return 'SKU序号不能为空'
    }

    if (typeof skuNo !== 'number' || isNaN(skuNo)) {
      return 'SKU序号必须是数字'
    }

    if (skuNo < 1) {
      return 'SKU序号必须大于0'
    }

    if (skuNo > 999999) {
      return 'SKU序号不能超过999999'
    }

    return null
  }

  /**
   * 验证海关申报代码格式
   */
  static validateCustomsCodeFormat(code: string): string | null {
    if (!code || code.trim() === '') {
      return null // 非必填字段
    }

    // 移除内容校验逻辑，只保留长度校验
    return null
  }

  /**
   * 批量验证表单数据
   */
  static validateFormData(formData: any): Record<string, string> {
    const errors: Record<string, string> = {}

    // 验证SKU编码
    const skuCodeError = this.validateSkuCodeFormat(formData.skuCode)
    if (skuCodeError) {
      errors.skuCode = skuCodeError
    }

    // 验证SKU序号
    const skuNoError = this.validateSkuNoFormat(formData.skuNo)
    if (skuNoError) {
      errors.skuNo = skuNoError
    }

    // 验证包装尺寸
    const dimensionError = this.validatePackageDimensions(
      formData.packageLength,
      formData.packageWidth,
      formData.packageHeight
    )
    if (dimensionError) {
      errors.packageDimensions = dimensionError
    }

    // 验证含税和税率关系
    const taxError = this.validateTaxRelation(formData.includeTax, formData.taxRate)
    if (taxError) {
      errors.taxRate = taxError
    }

    // 验证海关申报代码
    const customsCodeError = this.validateCustomsCodeFormat(formData.customsDeclarationCode)
    if (customsCodeError) {
      errors.customsDeclarationCode = customsCodeError
    }

    return errors
  }
}

/**
 * 创建防抖验证器
 */
export function createDebouncedValidator(validator: (value: any) => Promise<void>, delay = 500) {
  let timeoutId: NodeJS.Timeout | null = null

  return (rule: any, value: any) => {
    return new Promise<void>((resolve, reject) => {
      if (timeoutId) {
        clearTimeout(timeoutId)
      }

      timeoutId = setTimeout(async () => {
        try {
          await validator(value)
          resolve()
        } catch (error) {
          reject(error)
        }
      }, delay)
    })
  }
}
