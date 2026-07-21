/**
 * SKU计算属性工具类
 * 用于计算包装体积、密度、装柜量等衍生属性
 *
 * @author ballcat
 */

/**
 * SKU计算结果接口
 */
export interface SkuCalculationResult {
  /** 包装体积（立方米） */
  packageVolume: number
  /** 密度（立方米/kg） */
  densityKgM3: number
  /** 密度（立方厘米/g） */
  densityGCm3: number
  /** 装柜量 */
  containerCapacity: number
}

/**
 * SKU尺寸信息接口
 */
export interface SkuDimensions {
  /** 包装长度（米） */
  packageLength?: number
  /** 包装宽度（米） */
  packageWidth?: number
  /** 包装高度（米） */
  packageHeight?: number
  /** 重量（千克） */
  weight?: number
}

/**
 * SKU计算工具类
 */
export class SkuCalculator {
  /**
   * 计算包装体积（立方米）
   * @param length 长度（米）
   * @param width 宽度（米）
   * @param height 高度（米）
   * @returns 体积（立方米），保留足够的小数位数
   */
  static calculatePackageVolume(length?: number, width?: number, height?: number): number {
    // 更严格的参数检查
    if (
      typeof length !== 'number' ||
      typeof width !== 'number' ||
      typeof height !== 'number' ||
      isNaN(length) ||
      isNaN(width) ||
      isNaN(height) ||
      length <= 0 ||
      width <= 0 ||
      height <= 0
    ) {
      return 0
    }
    const volume = length * width * height

    // 动态确定小数位数，确保不会因为精度问题变成0
    let precision = 6
    if (volume < 1e-6) {
      precision = Math.max(6, Math.ceil(-Math.log10(volume)) + 2)
    }

    const result = Number(volume.toFixed(precision))
    return result
  }

  /**
   * 计算密度（kg/m³）
   * @param weight 重量（千克）
   * @param volume 体积（立方米）
   * @returns 密度（kg/m³），保留6位小数
   */
  static calculateDensityKgM3(weight?: number, volume?: number): number {
    if (
      typeof weight !== 'number' ||
      typeof volume !== 'number' ||
      isNaN(weight) ||
      isNaN(volume) ||
      weight <= 0 ||
      volume <= 0
    ) {
      return 0
    }
    const density = weight / volume
    return Number(density.toFixed(6))
  }

  /**
   * 计算密度（g/cm³）
   * @param weight 重量（千克）
   * @param volume 体积（立方米）
   * @returns 密度（g/cm³），保留3位小数
   */
  static calculateDensityGCm3(weight?: number, volume?: number): number {
    if (
      typeof weight !== 'number' ||
      typeof volume !== 'number' ||
      isNaN(weight) ||
      isNaN(volume) ||
      weight <= 0 ||
      volume <= 0
    ) {
      return 0
    }
    // 转换单位：立方米转立方厘米，kg转g
    const volumeCm3 = volume * 1000000 // 1立方米 = 1000000立方厘米
    const weightG = weight * 1000 // 1kg = 1000g
    const density = weightG / volumeCm3
    return Number(density.toFixed(3))
  }

  /**
   * 计算装柜量
   * @param volume 体积（立方米）
   * @returns 装柜量，保留2位小数
   */
  static calculateContainerCapacity(volume?: number): number {
    if (typeof volume !== 'number' || isNaN(volume) || volume <= 0) {
      return 0
    }
    // 66立方米/体积（立方米）
    const capacity = 66 / volume
    return Number(capacity.toFixed(2))
  }

  /**
   * 批量计算所有属性
   * @param dimensions SKU尺寸信息
   * @returns 计算结果
   */
  static calculateAll(dimensions: SkuDimensions): SkuCalculationResult {
    const { packageLength, packageWidth, packageHeight, weight } = dimensions

    // 计算体积
    const packageVolume = this.calculatePackageVolume(packageLength, packageWidth, packageHeight)

    // 计算密度
    const densityKgM3 = this.calculateDensityKgM3(weight, packageVolume)
    const densityGCm3 = this.calculateDensityGCm3(weight, packageVolume)

    // 计算装柜量
    const containerCapacity = this.calculateContainerCapacity(packageVolume)

    const result = {
      packageVolume,
      densityKgM3,
      densityGCm3,
      containerCapacity
    }

    return result
  }

  /**
   * 验证尺寸数据的有效性
   * @param dimensions SKU尺寸信息
   * @returns 验证结果
   */
  static validateDimensions(dimensions: SkuDimensions): {
    isValid: boolean
    errors: string[]
  } {
    const errors: string[] = []
    const { packageLength, packageWidth, packageHeight, weight } = dimensions

    // 验证长度
    if (packageLength !== undefined && packageLength <= 0) {
      errors.push('包装长度必须大于0')
    }

    // 验证宽度
    if (packageWidth !== undefined && packageWidth <= 0) {
      errors.push('包装宽度必须大于0')
    }

    // 验证高度
    if (packageHeight !== undefined && packageHeight <= 0) {
      errors.push('包装高度必须大于0')
    }

    // 验证重量
    if (weight !== undefined && weight <= 0) {
      errors.push('重量必须大于0')
    }

    return {
      isValid: errors.length === 0,
      errors
    }
  }

  /**
   * 格式化体积显示
   * @param volume 体积（立方米）
   * @param unit 单位，默认为'm³'
   * @returns 格式化后的字符串
   */
  static formatVolume(volume: number, unit = 'm³'): string {
    if (volume === 0) {
      return '-'
    }

    // 使用科学记数法显示极小的数值
    if (volume < 1e-6) {
      return `${volume.toExponential(3)} ${unit}`
    }

    // 统一保留3位小数
    const formatted = volume.toFixed(3)
    const cleanFormatted = formatted.replace(/\.?0+$/, '')

    return `${cleanFormatted} ${unit}`
  }

  /**
   * 格式化密度显示
   * @param density 密度
   * @param unit 单位
   * @returns 格式化后的字符串
   */
  static formatDensity(density: number, unit: string): string {
    if (density === 0) {
      return '-'
    }

    // 统一保留3位小数
    const formatted = density.toFixed(3)
    const cleanFormatted = formatted.replace(/\.?0+$/, '')

    return `${cleanFormatted} ${unit}`
  }

  /**
   * 格式化装柜量显示
   * @param capacity 装柜量
   * @returns 格式化后的字符串
   */
  static formatContainerCapacity(capacity: number): string {
    if (capacity === 0) {
      return '-'
    }

    // 统一保留3位小数
    const formatted = capacity.toFixed(3)
    const cleanFormatted = formatted.replace(/\.?0+$/, '')

    return cleanFormatted
  }

  /**
   * 检查是否需要重新计算
   * @param oldDimensions 旧的尺寸信息
   * @param newDimensions 新的尺寸信息
   * @returns 是否需要重新计算
   */
  static needsRecalculation(oldDimensions: SkuDimensions, newDimensions: SkuDimensions): boolean {
    return (
      oldDimensions.packageLength !== newDimensions.packageLength ||
      oldDimensions.packageWidth !== newDimensions.packageWidth ||
      oldDimensions.packageHeight !== newDimensions.packageHeight ||
      oldDimensions.weight !== newDimensions.weight
    )
  }

  /**
   * 获取计算公式说明
   * @returns 公式说明对象
   */
  static getFormulas(): Record<string, string> {
    return {
      packageVolume: '包装体积 = 长度 × 宽度 × 高度',
      densityKgM3: '密度(kg/m³) = 重量(kg) ÷ 体积(m³)',
      densityGCm3: '密度(g/cm³) = 重量(g) ÷ 体积(cm³)',
      containerCapacity: '装柜量 = 66 ÷ 体积(m³)'
    }
  }

  /**
   * 获取单位说明
   * @returns 单位说明对象
   */
  static getUnits(): Record<string, string> {
    return {
      length: 'm (米)',
      width: 'm (米)',
      height: 'm (米)',
      weight: 'kg (千克)',
      volume: 'm³ (立方米)',
      densityKgM3: 'kg/m³',
      densityGCm3: 'g/cm³',
      containerCapacity: '个'
    }
  }
}

/**
 * 默认导出计算器类
 */
export default SkuCalculator
