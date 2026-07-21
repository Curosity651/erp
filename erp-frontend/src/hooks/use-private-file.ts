import { ref } from 'vue'
import { getFileDownloadUrl, batchGetDownloadUrls } from '@/api/system/file'
import { isSuccess } from '@/api'

/**
 * URL 缓存项
 */
interface CacheEntry {
  url: string
  expireAt: number
}

/**
 * URL 缓存（模块级，跨组件共享）
 * 签名 URL 有效期 2 小时，提前 5 分钟刷新
 */
const urlCache = new Map<number, CacheEntry>()

/** 缓存有效期（2小时） */
const CACHE_DURATION = 2 * 60 * 60 * 1000

/** 提前刷新时间（5分钟） */
const REFRESH_AHEAD = 5 * 60 * 1000

/**
 * 检查缓存是否有效
 */
function isCacheValid(entry: CacheEntry): boolean {
  return Date.now() < entry.expireAt - REFRESH_AHEAD
}

/**
 * 私有文件 URL 管理 Hook
 * 提供签名 URL 获取、缓存管理功能
 */
export function usePrivateFile() {
  const loading = ref(false)

  /**
   * 获取单个文件的签名 URL（带缓存）
   */
  const getSignedUrl = async (fileId: number): Promise<string | null> => {
    if (!fileId) return null

    // 检查缓存
    const cached = urlCache.get(fileId)
    if (cached && isCacheValid(cached)) {
      return cached.url
    }

    loading.value = true
    try {
      const res = await getFileDownloadUrl(fileId)
      if (!isSuccess(res) || !res.data) {
        console.error('获取文件下载URL失败:', res.message)
        return null
      }

      // 缓存 URL
      urlCache.set(fileId, {
        url: res.data,
        expireAt: Date.now() + CACHE_DURATION
      })

      return res.data
    } catch (error) {
      console.error('获取文件下载URL异常:', error)
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 批量获取签名 URL（带缓存）
   */
  const batchGetSignedUrls = async (fileIds: number[]): Promise<Map<number, string>> => {
    const result = new Map<number, string>()
    if (!fileIds || fileIds.length === 0) return result

    // 分离：已缓存 vs 需要请求
    const needFetch: number[] = []
    for (const id of fileIds) {
      const cached = urlCache.get(id)
      if (cached && isCacheValid(cached)) {
        result.set(id, cached.url)
      } else {
        needFetch.push(id)
      }
    }

    // 如果都已缓存，直接返回
    if (needFetch.length === 0) return result

    loading.value = true
    try {
      const res = await batchGetDownloadUrls(needFetch)
      if (!isSuccess(res) || !res.data) {
        console.error('批量获取下载URL失败:', res.message)
        return result
      }

      // 更新缓存并合并结果
      const now = Date.now()
      for (const [idStr, url] of Object.entries(res.data)) {
        const id = Number(idStr)
        urlCache.set(id, {
          url,
          expireAt: now + CACHE_DURATION
        })
        result.set(id, url)
      }

      return result
    } catch (error) {
      console.error('批量获取下载URL异常:', error)
      return result
    } finally {
      loading.value = false
    }
  }

  /**
   * 清除指定文件的缓存
   */
  const clearCache = (fileId?: number) => {
    if (fileId) {
      urlCache.delete(fileId)
    } else {
      urlCache.clear()
    }
  }

  return {
    loading,
    getSignedUrl,
    batchGetSignedUrls,
    clearCache
  }
}
