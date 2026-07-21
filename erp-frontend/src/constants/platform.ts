/**
 * 平台常量定义 - 单一数据源
 */
export const PLATFORMS = {
  WILDBERRIES: 'wildberries',
  OZON: 'ozon',
  YANDEX: 'yandex'
} as const

/** 平台类型（自动派生） */
export type PlatformType = (typeof PLATFORMS)[keyof typeof PLATFORMS]

/** 平台配置（名称、样式等） */
export const PLATFORM_CONFIG: Record<
  PlatformType,
  {
    label: string
    tagClass: string
    gradient: string
  }
> = {
  wildberries: {
    label: 'Wildberries',
    tagClass: 'platform-tag--wb',
    gradient: 'linear-gradient(45deg, #6a11cb, #2575fc)'
  },
  ozon: {
    label: 'Ozon',
    tagClass: 'platform-tag--ozon',
    gradient: 'linear-gradient(45deg, #007bff, #00d4ff)'
  },
  yandex: {
    label: 'Yandex',
    tagClass: 'platform-tag--yandex',
    gradient: 'linear-gradient(45deg, #ff6b00, #ffc107)'
  }
}

/** 平台选项列表（用于 Select/Radio/Segmented） */
export const PLATFORM_OPTIONS: { value: PlatformType; label: string }[] = Object.entries(
  PLATFORM_CONFIG
).map(([value, config]) => ({ value: value as PlatformType, label: config.label }))

/** 获取平台显示名称 */
export const getPlatformLabel = (platform?: string): string => {
  return PLATFORM_CONFIG[platform as PlatformType]?.label ?? platform ?? ''
}
