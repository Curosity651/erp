/**
 * Platform 业务组件统一导出
 */
export { default as PlatformSelect } from './PlatformSelect.vue'
export { default as PlatformRadioGroup } from './PlatformRadioGroup.vue'
export { default as PlatformSegmented } from './PlatformSegmented.vue'
export { default as PlatformTag } from './PlatformTag.vue'

// Re-export types and utils from constants
export {
  PLATFORMS,
  PLATFORM_OPTIONS,
  PLATFORM_CONFIG,
  getPlatformLabel
} from '@/constants/platform'
export type { PlatformType } from '@/constants/platform'
