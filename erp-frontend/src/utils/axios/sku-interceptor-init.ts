import httpClient from '@/utils/axios'
import { registerSkuInterceptors } from '@/utils/axios/sku-interceptors'

let interceptorsRegistered = false

/**
 * 初始化SKU拦截器（仅在需要时调用）
 */
export function initSkuInterceptors() {
  if (interceptorsRegistered) {
    return
  }

  try {
    registerSkuInterceptors(httpClient.instance)
    interceptorsRegistered = true
    console.log('SKU拦截器初始化成功')
  } catch (error) {
    console.warn('SKU拦截器初始化失败:', error)
  }
}

/**
 * 检查拦截器是否已注册
 */
export function areSkuInterceptorsRegistered(): boolean {
  return interceptorsRegistered
}
