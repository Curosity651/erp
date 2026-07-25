import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import type { SkuDTO, SkuFileVO } from '@/api/product/sku/types'
import { SkuValidationRules, SkuFormValidator } from '@/utils/sku-validation'

/**
 * 扩展的表单数据类型，包含UI专用字段
 */
interface SkuFormData extends SkuDTO {
  id?: number // 编辑模式下的ID字段
  supplierName?: string
  orderUnit?: string
  cycleUnit?: string
  paymentTerms?: string
  // 文件信息 - 用于回显，与 SkuDetailVO.files 格式一致
  files?: Record<string, SkuFileVO[]>
}

/**
 * SKU表单默认值
 */
const DEFAULT_SKU_FORM_DATA: SkuFormData = {
  id: undefined,
  skuCode: '',
  barcodes: [],
  skuNo: undefined,
  spuCode: '',
  salesCountry: 'RU', // 默认选中俄罗斯
  categoryId: undefined,
  productStatus: 1, // 默认为在售状态
  shippingType: 1, // 默认为陆运
  brandCode: '',
  projectGroupCode: '',
  description: '',
  chineseName: '',
  russianName: '',
  packageType: 'normal', // 默认为普通包裹
  billingWeightType: 1, // 默认选择取大值
  remarks: '',
  customsDeclarationName: '',
  customsDeclarationCode: '',
  needsPower: 0, // 使用数字类型，是否有排插
  seasonal: 0, // 使用数字类型
  hasRgbLight: 0, // 使用数字类型，默认为否
  hasGlass: 0, // 使用数字类型，默认为否
  bcBoxMinBreakage: '',
  packaging: '',
  frameColor: '',
  surfaceColor: '',
  material: '',
  weight: undefined, // 使用数字类型
  weightUnit: 'kg', // 默认为KG
  packageLength: undefined, // 使用数字类型
  packageWidth: undefined, // 使用数字类型
  packageHeight: undefined, // 使用数字类型
  packageUnit: 'mm', // 默认为毫米
  functionalRequirements: '',
  supplierCode: '',
  supplierName: '',
  includeTax: 0,
  taxRate: undefined, // 使用数字类型
  purchasePrice: undefined, // 使用数字类型
  minimumOrderQuantity: undefined,
  productionCycle: undefined,
  developerId: undefined,
  operatorId: undefined,
  qcId: undefined,
  purchaserId: undefined,
  orderUnit: '个',
  cycleUnit: '天',
  paymentTerms: '',
  files: undefined
}

/**
 * 创建默认表单数据的深拷贝
 */
const createDefaultFormData = (): SkuFormData => {
  return JSON.parse(JSON.stringify(DEFAULT_SKU_FORM_DATA))
}

/**
 * SKU表单状态管理
 */
export function useSkuForm() {
  // 表单状态
  const formState = reactive({
    loading: false,
    submitting: false,
    validating: false,
    hasChanges: false,
    errors: {} as Record<string, string[]>
  })

  // 表单数据
  const formData = reactive<SkuFormData>(createDefaultFormData())

  // 原始数据（用于检测变更）
  const originalData = ref<SkuFormData | null>(null)

  // 是否为更新模式
  const isUpdateMode = computed(() => !!formData.id)

  // 是否有未保存的更改
  const hasUnsavedChanges = computed(() => {
    if (!originalData.value) return false
    return JSON.stringify(formData) !== JSON.stringify(originalData.value)
  })

  // 验证规则
  const validationRules = computed(() => ({
    skuCode: SkuValidationRules.skuCodeRules(isUpdateMode.value ? formData.id : undefined),
    skuNo: SkuValidationRules.skuNoRules(isUpdateMode.value ? formData.id : undefined),
    spuCode: SkuValidationRules.spuCodeRules(),
    salesCountry: SkuValidationRules.salesCountryRules(),
    categoryId: SkuValidationRules.categoryRules(),
    productStatus: SkuValidationRules.productStatusRules(),
    shippingType: SkuValidationRules.shippingTypeRules(),
    brandCode: SkuValidationRules.brandRules(),
    projectGroupCode: SkuValidationRules.projectGroupRules(),
    description: SkuValidationRules.descriptionRules(),
    chineseName: SkuValidationRules.chineseNameRules(),
    russianName: SkuValidationRules.russianNameRules(),
    customsDeclarationName: SkuValidationRules.customsDeclarationNameRules(),
    customsDeclarationCode: SkuValidationRules.customsDeclarationCodeRules(),
    weight: SkuValidationRules.weightRules(),
    packageLength: SkuValidationRules.packageDimensionRules(),
    packageWidth: SkuValidationRules.packageDimensionRules(),
    packageHeight: SkuValidationRules.packageDimensionRules(),
    supplierCode: SkuValidationRules.supplierCodeRules(),
    taxRate: SkuValidationRules.taxRateRules(!!formData.includeTax),
    purchasePrice: SkuValidationRules.purchasePriceRules(),
    minimumOrderQuantity: SkuValidationRules.minimumOrderQuantityRules(),
    productionCycle: SkuValidationRules.productionCycleRules()
  }))

  /**
   * 初始化表单数据
   */
  const initForm = (data?: Partial<SkuFormData>) => {
    // 先重置表单到默认状态
    resetForm()

    // 如果有传入数据，则合并到表单中
    if (data) {
      const processedData = {
        ...data,
        // 确保布尔值正确转换为数字
        needsPower: data.needsPower
          ? typeof data.needsPower === 'boolean'
            ? data.needsPower
              ? 1
              : 0
            : data.needsPower
          : 0,
        seasonal: data.seasonal
          ? typeof data.seasonal === 'boolean'
            ? data.seasonal
              ? 1
              : 0
            : data.seasonal
          : 0
      }

      Object.assign(formData, processedData)
      console.log('初始化表单数据:', formData)
      originalData.value = JSON.parse(JSON.stringify(formData))
    }
  }

  /**
   * 重置表单
   */
  const resetForm = () => {
    // 先清空 formData 的所有属性
    for (const key in formData) {
      delete (formData as any)[key]
    }

    // 使用默认数据重置表单
    Object.assign(formData, createDefaultFormData())

    originalData.value = null
    formState.errors = {}
    formState.hasChanges = false
  }

  /**
   * 验证表单
   */
  const validateForm = async (): Promise<boolean> => {
    formState.validating = true
    formState.errors = {}

    try {
      // 使用自定义验证器
      const errors = SkuFormValidator.validateFormData(formData)

      if (Object.keys(errors).length > 0) {
        formState.errors = Object.fromEntries(
          Object.entries(errors).map(([key, value]) => [key, [value]])
        )
        // 不在这里显示通用错误提示，让具体的字段错误自己显示
        return false
      }

      return true
    } catch (error) {
      console.error('表单验证失败:', error)
      // 只在异常情况下显示错误提示
      message.error('表单验证异常，请稍后重试')
      return false
    } finally {
      formState.validating = false
    }
  }

  /**
   * 获取提交数据
   */
  const getSubmitData = (): SkuDTO => {
    const submitData = { ...formData }

    // 数据类型转换和格式化
    // 确保数值类型正确
    if (submitData.categoryId) {
      submitData.categoryId = Number(submitData.categoryId)
    }
    if (submitData.productStatus) {
      submitData.productStatus = Number(submitData.productStatus)
    }
    if (submitData.shippingType) {
      submitData.shippingType = Number(submitData.shippingType)
    }
    if (submitData.billingWeightType) {
      submitData.billingWeightType = Number(submitData.billingWeightType)
    }
    if (submitData.includeTax !== undefined) {
      submitData.includeTax = Number(submitData.includeTax)
    }
    if (submitData.minimumOrderQuantity) {
      submitData.minimumOrderQuantity = Number(submitData.minimumOrderQuantity)
    }
    if (submitData.productionCycle) {
      submitData.productionCycle = Number(submitData.productionCycle)
    }
    if (submitData.needsPower !== undefined) {
      submitData.needsPower = Number(submitData.needsPower)
    }
    if (submitData.seasonal !== undefined) {
      submitData.seasonal = Number(submitData.seasonal)
    }

    // 字符串类型处理 - 去除前后空格
    const stringFields: (keyof SkuFormData)[] = [
      'skuCode',
      'spuCode',
      'salesCountry',
      'brandCode',
      'projectGroupCode',
      'description',
      'chineseName',
      'russianName',
      'packageType',
      'remarks',
      'customsDeclarationName',
      'customsDeclarationCode',
      'bcBoxMinBreakage',
      'packaging',
      'frameColor',
      'surfaceColor',
      'material',
      'weightUnit',
      'packageUnit',
      'functionalRequirements',
      'supplierCode',
      'developerId',
      'operatorId',
      'qcId',
      'purchaserId'
    ]

    stringFields.forEach(field => {
      if (submitData[field] && typeof submitData[field] === 'string') {
        ;(submitData as any)[field] = (submitData[field] as string).trim()
      }
    })

    // 移除不需要提交的临时字段
    delete submitData.supplierName
    delete submitData.orderUnit
    delete submitData.cycleUnit
    delete submitData.paymentTerms

    return submitData
  }

  /**
   * 设置字段错误
   */
  const setFieldError = (field: string, error: string) => {
    formState.errors[field] = [error]
  }

  /**
   * 清除字段错误
   */
  const clearFieldError = (field: string) => {
    delete formState.errors[field]
  }

  /**
   * 清除所有错误
   */
  const clearAllErrors = () => {
    formState.errors = {}
  }

  /**
   * 检查是否可以离开页面
   */
  const canLeave = (): boolean => {
    if (hasUnsavedChanges.value) {
      return confirm('您有未保存的更改，确定要离开吗？')
    }
    return true
  }

  return {
    // 状态
    formState,
    formData,
    isUpdateMode,
    hasUnsavedChanges,
    validationRules,

    // 方法
    initForm,
    resetForm,
    validateForm,
    getSubmitData,
    setFieldError,
    clearFieldError,
    clearAllErrors,
    canLeave
  }
}
