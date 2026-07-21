import { message, notification } from 'ant-design-vue'
import type { AxiosError } from 'axios'
import type { ApiResult } from '@/api/types'

/**
 * SKU业务错误码枚举
 */
export enum SkuErrorCode {
  // SKU编码相关
  SKU_CODE_DUPLICATE = 'SKU_CODE_DUPLICATE',
  SKU_CODE_INVALID_FORMAT = 'SKU_CODE_INVALID_FORMAT',
  SKU_CODE_REQUIRED = 'SKU_CODE_REQUIRED',

  // SKU数据相关
  SKU_NOT_FOUND = 'SKU_NOT_FOUND',
  SKU_DATA_INVALID = 'SKU_DATA_INVALID',
  SKU_STATUS_INVALID = 'SKU_STATUS_INVALID',

  // 文件上传相关
  FILE_TYPE_NOT_SUPPORTED = 'FILE_TYPE_NOT_SUPPORTED',
  FILE_SIZE_EXCEEDED = 'FILE_SIZE_EXCEEDED',
  FILE_UPLOAD_FAILED = 'FILE_UPLOAD_FAILED',
  FILE_NOT_FOUND = 'FILE_NOT_FOUND',

  // 权限相关
  PERMISSION_DENIED = 'PERMISSION_DENIED',

  // 业务逻辑相关
  CATEGORY_NOT_FOUND = 'CATEGORY_NOT_FOUND',
  SUPPLIER_NOT_FOUND = 'SUPPLIER_NOT_FOUND',
  USER_NOT_FOUND = 'USER_NOT_FOUND',

  // 数据验证相关
  REQUIRED_FIELD_MISSING = 'REQUIRED_FIELD_MISSING',
  FIELD_FORMAT_INVALID = 'FIELD_FORMAT_INVALID',
  FIELD_VALUE_OUT_OF_RANGE = 'FIELD_VALUE_OUT_OF_RANGE'
}

/**
 * 错误消息映射
 */
const ERROR_MESSAGE_MAP: Record<string, string> = {
  [SkuErrorCode.SKU_CODE_DUPLICATE]: 'SKU编码已存在，请使用其他编码',
  [SkuErrorCode.SKU_CODE_INVALID_FORMAT]: 'SKU编码只能包含大写字母、数字、横线和下划线，长度3-100',
  [SkuErrorCode.SKU_CODE_REQUIRED]: 'SKU编码不能为空',

  [SkuErrorCode.SKU_NOT_FOUND]: '未找到指定的SKU',
  [SkuErrorCode.SKU_DATA_INVALID]: 'SKU数据格式不正确',
  [SkuErrorCode.SKU_STATUS_INVALID]: '产品状态值无效',

  [SkuErrorCode.FILE_TYPE_NOT_SUPPORTED]: '不支持的文件类型',
  [SkuErrorCode.FILE_SIZE_EXCEEDED]: '文件大小超出限制',
  [SkuErrorCode.FILE_UPLOAD_FAILED]: '文件上传失败',
  [SkuErrorCode.FILE_NOT_FOUND]: '文件不存在',

  [SkuErrorCode.PERMISSION_DENIED]: '没有权限执行此操作',

  [SkuErrorCode.CATEGORY_NOT_FOUND]: '未找到指定的品类',
  [SkuErrorCode.SUPPLIER_NOT_FOUND]: '未找到指定的供应商',
  [SkuErrorCode.USER_NOT_FOUND]: '未找到指定的用户',

  [SkuErrorCode.REQUIRED_FIELD_MISSING]: '必填字段不能为空',
  [SkuErrorCode.FIELD_FORMAT_INVALID]: '字段格式不正确',
  [SkuErrorCode.FIELD_VALUE_OUT_OF_RANGE]: '字段值超出有效范围'
}

/**
 * SKU业务错误处理器
 */
export class SkuErrorHandler {
  /**
   * 处理API错误
   * @param error 错误对象
   * @param context 错误上下文
   */
  static handleApiError(error: AxiosError | any, context?: string) {
    console.error('SKU API Error:', error, 'Context:', context)

    // 如果错误已经被处理过，直接返回
    if (error.resolved) {
      return
    }

    let errorMessage = '操作失败'
    let errorCode = ''

    if (error.response?.data) {
      const apiResult = error.response.data as ApiResult
      errorCode = apiResult.code?.toString() || ''
      errorMessage = apiResult.message || errorMessage
    } else if (error.message) {
      errorMessage = error.message
    }

    // 根据错误码获取友好的错误消息
    const friendlyMessage = this.getFriendlyErrorMessage(errorCode, errorMessage)

    // 根据错误类型选择不同的提示方式
    if (this.isCriticalError(errorCode)) {
      notification.error({
        message: '系统错误',
        description: friendlyMessage,
        duration: 5
      })
    } else {
      message.error(friendlyMessage)
    }

    // 标记错误已处理
    error.resolved = true
  }

  /**
   * 处理表单验证错误
   * @param errors 验证错误对象
   */
  static handleFormValidationError(errors: Record<string, string[]>) {
    const errorMessages = Object.entries(errors)
      .map(([field, messages]) => `${field}: ${messages.join(', ')}`)
      .join('\n')

    notification.error({
      message: '表单验证失败',
      description: errorMessages,
      duration: 4
    })
  }

  /**
   * 处理文件上传错误
   * @param error 错误对象
   * @param fileName 文件名
   */
  static handleFileUploadError(error: any, fileName?: string) {
    console.error('File Upload Error:', error, 'File:', fileName)

    let errorMessage = '文件上传失败'

    if (error.code) {
      errorMessage = this.getFriendlyErrorMessage(error.code, error.message)
    } else if (error.message) {
      errorMessage = error.message
    }

    if (fileName) {
      errorMessage = `文件 "${fileName}" ${errorMessage}`
    }

    message.error(errorMessage)
  }

  /**
   * 处理批量操作错误
   * @param error 错误对象
   * @param operationType 操作类型
   * @param affectedCount 影响的记录数
   */
  static handleBatchOperationError(error: any, operationType: string, affectedCount?: number) {
    console.error('Batch Operation Error:', error, 'Type:', operationType, 'Count:', affectedCount)

    const operationNames: Record<string, string> = {
      updateStatus: '批量更新状态',
      delete: '批量删除',
      export: '批量导出'
    }

    const operationName = operationNames[operationType] || operationType
    let errorMessage = `${operationName}失败`

    if (error.response?.data?.message) {
      errorMessage = `${operationName}失败: ${error.response.data.message}`
    } else if (error.message) {
      errorMessage = `${operationName}失败: ${error.message}`
    }

    if (affectedCount !== undefined) {
      errorMessage += `（影响 ${affectedCount} 条记录）`
    }

    notification.error({
      message: '批量操作失败',
      description: errorMessage,
      duration: 5
    })
  }

  /**
   * 获取友好的错误消息
   * @param errorCode 错误码
   * @param originalMessage 原始错误消息
   */
  private static getFriendlyErrorMessage(errorCode: string, originalMessage: string): string {
    // 首先尝试从错误码映射中获取
    if (errorCode && ERROR_MESSAGE_MAP[errorCode]) {
      return ERROR_MESSAGE_MAP[errorCode]
    }

    // 根据错误码模式匹配
    if (errorCode.includes('DUPLICATE')) {
      return '数据重复，请检查输入'
    }

    if (errorCode.includes('NOT_FOUND')) {
      return '未找到相关数据'
    }

    if (errorCode.includes('PERMISSION')) {
      return '没有权限执行此操作'
    }

    if (errorCode.includes('VALIDATION') || errorCode.includes('INVALID')) {
      return '数据验证失败，请检查输入'
    }

    if (errorCode.includes('FILE')) {
      return '文件操作失败'
    }

    // 返回原始消息或默认消息
    return originalMessage || '操作失败，请稍后重试'
  }

  /**
   * 判断是否为严重错误（需要使用notification而不是message）
   * @param errorCode 错误码
   */
  private static isCriticalError(errorCode: string): boolean {
    const criticalErrorPatterns = ['PERMISSION', 'SYSTEM', 'DATABASE', 'NETWORK', 'TIMEOUT']

    return criticalErrorPatterns.some(pattern => errorCode.includes(pattern))
  }
}

/**
 * SKU操作成功消息处理器
 */
export class SkuSuccessHandler {
  /**
   * 显示操作成功消息
   * @param operation 操作类型
   * @param details 详细信息
   */
  static showSuccess(operation: string, details?: string) {
    const operationNames: Record<string, string> = {
      create: '创建SKU',
      update: '更新SKU',
      delete: '删除SKU',
      batchUpdate: '批量更新',
      batchDelete: '批量删除',
      export: '导出数据',
      upload: '上传文件',
      validate: '验证数据'
    }

    const operationName = operationNames[operation] || operation
    let successMessage = `${operationName}成功`

    if (details) {
      successMessage += `：${details}`
    }

    message.success(successMessage)
  }

  /**
   * 显示批量操作成功消息
   * @param operation 操作类型
   * @param successCount 成功数量
   * @param totalCount 总数量
   */
  static showBatchSuccess(operation: string, successCount: number, totalCount?: number) {
    const operationNames: Record<string, string> = {
      updateStatus: '更新状态',
      delete: '删除',
      export: '导出'
    }

    const operationName = operationNames[operation] || operation
    let successMessage = `批量${operationName}成功`

    if (totalCount !== undefined && totalCount > 0) {
      successMessage += `（${successCount}/${totalCount}）`
    } else {
      successMessage += `（${successCount} 条记录）`
    }

    message.success(successMessage)
  }
}

/**
 * 创建带有错误处理的API调用包装器
 * @param apiCall API调用函数
 * @param context 错误上下文
 */
export function withErrorHandling<T extends (...args: any[]) => Promise<any>>(
  apiCall: T,
  context?: string
): T {
  return (async (...args: Parameters<T>) => {
    try {
      return await apiCall(...args)
    } catch (error) {
      SkuErrorHandler.handleApiError(error, context)
      throw error
    }
  }) as T
}
