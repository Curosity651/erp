import type { AxiosError, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import type { ApiResult } from '@/api/types'
import { SkuErrorHandler, SkuSuccessHandler } from '@/utils/sku-error-handler'
import { message } from 'ant-design-vue'

/**
 * SKU相关的请求拦截器
 */
export const skuRequestInterceptor = (config: InternalAxiosRequestConfig) => {
  try {
    // 为SKU相关的请求添加特殊标识
    if (config.url?.includes('/product/sku')) {
      config.headers = config.headers || {}
      config.headers['X-Business-Module'] = 'SKU'

      // 为文件上传请求设置更长的超时时间
      if (config.url.includes('/files/') || config.url.includes('/upload')) {
        config.timeout = 300000 // 5分钟
      }

      // 为批量操作设置更长的超时时间
      if (config.url.includes('/batch-') || config.url.includes('/export')) {
        config.timeout = 180000 // 3分钟
      }
    }

    return config
  } catch (error) {
    console.warn('SKU请求拦截器处理失败:', error)
    return config
  }
}

/**
 * SKU相关的响应拦截器
 */
export const skuResponseInterceptor = (response: AxiosResponse) => {
  try {
    const config = response?.config
    const data = response?.data as ApiResult

    // 检查config和url是否存在
    if (!config || !config.url) {
      return response
    }

    // 只处理SKU相关的响应
    if (!config.url.includes('/product/sku')) {
      return response
    }

    // 处理成功响应的业务逻辑
    if (data && (data.code === 200 || data.code === 0)) {
      // 根据不同的操作类型显示成功消息
      const url = config.url
      const method = config.method?.toUpperCase()

      // 只对写操作显示成功消息，查询操作不显示
      if (method === 'POST') {
        if (url.includes('/save')) {
          SkuSuccessHandler.showSuccess('create')
        } else if (url.includes('/update')) {
          SkuSuccessHandler.showSuccess('update')
        } else if (url.includes('/delete')) {
          SkuSuccessHandler.showSuccess('delete')
        } else if (url.includes('/batch-update-status')) {
          const successCount = data.data as number
          SkuSuccessHandler.showBatchSuccess('updateStatus', successCount)
        }
      }

      // 文件操作成功消息
      if (url.includes('/files/save')) {
        SkuSuccessHandler.showSuccess('upload', '文件信息已保存')
      } else if (url.includes('/files/delete')) {
        SkuSuccessHandler.showSuccess('delete', '文件已删除')
      }
    }

    return response
  } catch (error) {
    console.warn('SKU响应拦截器处理失败:', error)
    return response
  }
}

/**
 * SKU相关的错误响应拦截器
 */
export const skuErrorInterceptor = (error: AxiosError) => {
  const config = error.config

  // 只处理SKU相关的错误
  if (!config?.url?.includes('/product/sku')) {
    return Promise.reject(error)
  }

  // 获取错误上下文
  const url = config.url
  const method = config.method?.toUpperCase()
  let context = 'SKU操作'

  if (url.includes('/validate-code')) {
    context = 'SKU编码验证'
  } else if (url.includes('/files/')) {
    context = 'SKU文件操作'
  } else if (url.includes('/batch-')) {
    context = 'SKU批量操作'
  } else if (url.includes('/export')) {
    context = 'SKU数据导出'
  } else if (method === 'POST' && url.includes('/save')) {
    context = 'SKU创建'
  } else if (method === 'POST' && url.includes('/update')) {
    context = 'SKU更新'
  } else if (method === 'POST' && url.includes('/delete')) {
    context = 'SKU删除'
  } else if (method === 'GET' && url.includes('/page')) {
    context = 'SKU查询'
  }

  // 使用SKU专用的错误处理器
  SkuErrorHandler.handleApiError(error, context)

  return Promise.reject(error)
}

/**
 * SKU编码验证专用拦截器
 */
export const skuCodeValidationInterceptor = {
  request: (config: InternalAxiosRequestConfig) => {
    if (config.url?.includes('/validate-code')) {
      // 为编码验证请求添加防抖标识
      config.headers = config.headers || {}
      config.headers['X-Debounce'] = 'true'
      config.timeout = 10000 // 10秒超时
    }
    return config
  },

  response: (response: AxiosResponse) => {
    const config = response.config
    const data = response.data as ApiResult<boolean>

    if (config.url?.includes('/validate-code')) {
      // 不显示成功消息，由组件自行处理
      return response
    }

    return response
  },

  error: (error: AxiosError) => {
    const config = error.config

    if (config?.url?.includes('/validate-code')) {
      // 编码验证失败时的特殊处理
      console.warn('SKU编码验证请求失败:', error.message)

      // 不显示错误消息，由组件自行处理
      error.resolved = true
    }

    return Promise.reject(error)
  }
}

/**
 * 文件上传专用拦截器
 */
export const fileUploadInterceptor = {
  request: (config: InternalAxiosRequestConfig) => {
    if (config.url?.includes('/upload-token') || config.url?.includes('/files/')) {
      config.headers = config.headers || {}
      config.headers['X-File-Operation'] = 'true'

      // 文件操作使用更长的超时时间
      config.timeout = 300000 // 5分钟
    }
    return config
  },

  response: (response: AxiosResponse) => {
    // 文件上传相关的成功响应由主拦截器处理
    return response
  },

  error: (error: AxiosError) => {
    const config = error.config

    if (config?.url?.includes('/upload-token') || config?.url?.includes('/files/')) {
      // 使用文件上传专用的错误处理
      const fileName = config.params?.fileName || '未知文件'
      SkuErrorHandler.handleFileUploadError(error, fileName)
    }

    return Promise.reject(error)
  }
}

/**
 * 批量操作专用拦截器
 */
export const batchOperationInterceptor = {
  request: (config: InternalAxiosRequestConfig) => {
    if (config.url?.includes('/batch-')) {
      config.headers = config.headers || {}
      config.headers['X-Batch-Operation'] = 'true'
      config.timeout = 180000 // 3分钟超时
    }
    return config
  },

  response: (response: AxiosResponse) => {
    // 批量操作的成功响应由主拦截器处理
    return response
  },

  error: (error: AxiosError) => {
    const config = error.config

    if (config?.url?.includes('/batch-')) {
      // 提取操作类型
      const operationType = config.url.split('/batch-')[1]?.split('?')[0] || 'unknown'
      const requestData = config.data
      const affectedCount = Array.isArray(requestData) ? requestData.length : undefined

      SkuErrorHandler.handleBatchOperationError(error, operationType, affectedCount)
    }

    return Promise.reject(error)
  }
}

/**
 * 注册所有SKU相关的拦截器到axios实例
 */
export function registerSkuInterceptors(axiosInstance: any) {
  // 检查axios实例是否有效
  if (!axiosInstance || !axiosInstance.interceptors) {
    console.warn('SKU拦截器注册失败: axios实例无效')
    return
  }

  try {
    // 请求拦截器
    axiosInstance.interceptors.request.use(skuRequestInterceptor)

    // 响应拦截器（使用更安全的版本）
    axiosInstance.interceptors.response.use(skuResponseInterceptor)
    axiosInstance.interceptors.response.use(undefined, skuErrorInterceptor)

    console.log('SKU拦截器注册成功')
  } catch (error) {
    console.error('SKU拦截器注册失败:', error)
  }
}
