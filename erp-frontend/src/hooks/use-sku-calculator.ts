import { ref, computed, watch, type Ref } from 'vue'
import SkuCalculator, {
  type SkuDimensions,
  type SkuCalculationResult
} from '@/utils/sku-calculator'

/**
 * SKU计算属性组合式函数
 * 提供响应式的SKU计算功能
 *
 * @author ballcat
 */

export interface UseSkuCalculatorOptions {
  /** 是否自动计算，默认为true */
  autoCalculate?: boolean
  /** 计算防抖延迟（毫秒），默认为300 */
  debounceDelay?: number
}

export interface UseSkuCalculatorReturn {
  /** 包装长度 */
  packageLength: Ref<number | undefined>
  /** 包装宽度 */
  packageWidth: Ref<number | undefined>
  /** 包装高度 */
  packageHeight: Ref<number | undefined>
  /** 重量 */
  weight: Ref<number | undefined>
  /** 计算结果 */
  calculationResult: Ref<SkuCalculationResult>
  /** 是否正在计算 */
  isCalculating: Ref<boolean>
  /** 验证错误信息 */
  validationErrors: Ref<string[]>
  /** 是否有效 */
  isValid: Ref<boolean>
  /** 格式化的显示值 */
  formattedValues: {
    packageVolume: Ref<string>
    densityKgM3: Ref<string>
    densityGCm3: Ref<string>
    containerCapacity: Ref<string>
  }
  /** 手动触发计算 */
  calculate: () => void
  /** 重置所有值 */
  reset: () => void
  /** 设置尺寸数据 */
  setDimensions: (dimensions: SkuDimensions) => void
  /** 获取当前尺寸数据 */
  getDimensions: () => SkuDimensions
}

/**
 * 通用的SKU计算方法，处理单位转换和计算
 * @param packageLength 包装长度
 * @param packageWidth 包装宽度
 * @param packageHeight 包装高度
 * @param packageUnit 包装单位
 * @param weight 重量
 * @param weightUnit 重量单位
 * @returns 计算结果
 */
export function calculateSkuProperties(
  packageLength?: number,
  packageWidth?: number,
  packageHeight?: number,
  packageUnit?: string,
  weight?: number,
  weightUnit?: string
): SkuCalculationResult {
  if (!packageLength || !packageWidth || !packageHeight || !weight) {
    return { packageVolume: 0, densityKgM3: 0, densityGCm3: 0, containerCapacity: 0 }
  }

  // 统一转换为米和千克进行计算
  let lengthInM = packageLength
  let widthInM = packageWidth
  let heightInM = packageHeight
  let weightInKg = weight

  // 尺寸单位转换
  if (packageUnit === 'CM' || packageUnit === 'cm') {
    lengthInM = packageLength / 100
    widthInM = packageWidth / 100
    heightInM = packageHeight / 100
  } else if (packageUnit === 'MM' || packageUnit === 'mm') {
    lengthInM = packageLength / 1000
    widthInM = packageWidth / 1000
    heightInM = packageHeight / 1000
  }

  // 重量单位转换
  if (weightUnit === 'g') {
    weightInKg = weight / 1000
  }

  // 使用标准化的尺寸数据进行计算
  const dimensions: SkuDimensions = {
    packageLength: lengthInM,
    packageWidth: widthInM,
    packageHeight: heightInM,
    weight: weightInKg
  }

  return SkuCalculator.calculateAll(dimensions)
}

/**
 * 使用SKU计算器
 * @param options 配置选项
 * @returns 计算器相关的响应式数据和方法
 */
export function useSkuCalculator(options: UseSkuCalculatorOptions = {}): UseSkuCalculatorReturn {
  const { autoCalculate = true, debounceDelay = 300 } = options

  // 响应式数据
  const packageLength = ref<number | undefined>()
  const packageWidth = ref<number | undefined>()
  const packageHeight = ref<number | undefined>()
  const weight = ref<number | undefined>()
  const isCalculating = ref(false)

  // 计算结果
  const calculationResult = ref<SkuCalculationResult>({
    packageVolume: 0,
    densityKgM3: 0,
    densityGCm3: 0,
    containerCapacity: 0
  })

  // 验证错误
  const validationErrors = ref<string[]>([])

  // 计算属性：是否有效
  const isValid = computed(() => validationErrors.value.length === 0)

  // 计算属性：格式化的显示值
  const formattedValues = {
    packageVolume: computed(() =>
      SkuCalculator.formatVolume(calculationResult.value.packageVolume)
    ),
    densityKgM3: computed(() =>
      SkuCalculator.formatDensity(calculationResult.value.densityKgM3, 'kg/m³')
    ),
    densityGCm3: computed(() =>
      SkuCalculator.formatDensity(calculationResult.value.densityGCm3, 'g/cm³')
    ),
    containerCapacity: computed(() =>
      SkuCalculator.formatContainerCapacity(calculationResult.value.containerCapacity)
    )
  }

  // 防抖计时器
  let debounceTimer: NodeJS.Timeout | null = null

  /**
   * 执行计算
   */
  const calculate = () => {
    isCalculating.value = true
    try {
      const dimensions: SkuDimensions = {
        packageLength: packageLength.value,
        packageWidth: packageWidth.value,
        packageHeight: packageHeight.value,
        weight: weight.value
      }

      // 验证数据
      const validation = SkuCalculator.validateDimensions(dimensions)
      validationErrors.value = validation.errors

      // 如果数据有效，进行计算
      if (validation.isValid) {
        const result = SkuCalculator.calculateAll(dimensions)
        calculationResult.value = result
      } else {
        // 数据无效时重置计算结果
        calculationResult.value = {
          packageVolume: 0,
          densityKgM3: 0,
          densityGCm3: 0,
          containerCapacity: 0
        }
      }
    } catch (error) {
      console.error('SKU计算出错:', error)
      validationErrors.value = ['计算过程中发生错误']
    } finally {
      isCalculating.value = false
    }
  }

  /**
   * 防抖计算
   */
  const debouncedCalculate = () => {
    if (debounceTimer) {
      clearTimeout(debounceTimer)
    }
    debounceTimer = setTimeout(() => {
      calculate()
    }, debounceDelay)
  }

  /**
   * 重置所有值
   */
  const reset = () => {
    packageLength.value = undefined
    packageWidth.value = undefined
    packageHeight.value = undefined
    weight.value = undefined
    calculationResult.value = {
      packageVolume: 0,
      densityKgM3: 0,
      densityGCm3: 0,
      containerCapacity: 0
    }
    validationErrors.value = []
  }

  /**
   * 设置尺寸数据
   */
  const setDimensions = (dimensions: SkuDimensions) => {
    packageLength.value = dimensions.packageLength
    packageWidth.value = dimensions.packageWidth
    packageHeight.value = dimensions.packageHeight
    weight.value = dimensions.weight
  }

  /**
   * 获取当前尺寸数据
   */
  const getDimensions = (): SkuDimensions => {
    return {
      packageLength: packageLength.value,
      packageWidth: packageWidth.value,
      packageHeight: packageHeight.value,
      weight: weight.value
    }
  }

  // 监听尺寸变化，自动计算
  if (autoCalculate) {
    watch(
      [packageLength, packageWidth, packageHeight, weight],
      () => {
        debouncedCalculate()
      },
      { deep: true }
    )
  }

  // 组件卸载时清理定时器
  const cleanup = () => {
    if (debounceTimer) {
      clearTimeout(debounceTimer)
      debounceTimer = null
    }
  }

  // 在组件卸载时清理
  if (typeof window !== 'undefined') {
    window.addEventListener('beforeunload', cleanup)
  }

  return {
    packageLength,
    packageWidth,
    packageHeight,
    weight,
    calculationResult,
    isCalculating,
    validationErrors,
    isValid,
    formattedValues,
    calculate,
    reset,
    setDimensions,
    getDimensions
  }
}

/**
 * 默认导出
 */
export default useSkuCalculator
