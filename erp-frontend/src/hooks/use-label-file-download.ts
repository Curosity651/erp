/**
 * 面单文件下载组合式函数
 * 封装文件下载和 ZIP 打包的通用逻辑
 */
import JSZip from 'jszip'
import { fileDownload } from '@/utils/file-utils'
import type { LabelBatchFileVO } from '@/api/order/label-batch'

// 并发控制参数
const CONCURRENCY = 4
// 单个文件下载超时时间（毫秒）
const SINGLE_FILE_TIMEOUT_MS = 30000
// 整体打包超时时间（毫秒），根据文件数量动态计算的基础值
const BASE_TIMEOUT_MS = 60000
// 每个文件额外增加的超时时间
const PER_FILE_TIMEOUT_MS = 5000

// 从环境变量获取 OSS 域名
const OSS_BUCKET_DOMAIN = import.meta.env.VITE_OSS_BUCKET_DOMAIN

/**
 * 分组项
 */
export interface GroupItem {
  key: string
  name?: string
  files: LabelBatchFileVO[]
  downloadableCount: number
}

/**
 * 分组错误
 */
export interface GroupError {
  name: string
  message: string
  file: LabelBatchFileVO
}

/**
 * 下载结果
 */
export interface DownloadResult {
  /** 成功数量 */
  successCount: number
  /** 失败数量 */
  failedCount: number
  /** 总数量 */
  totalCount: number
  /** 是否因错误提前取消 */
  cancelled: boolean
  /** 取消原因 */
  cancelReason?: string
}

/**
 * ZIP 任务
 */
interface ZipTask {
  file: LabelBatchFileVO
  group: string
  idx: number
}

/**
 * 构建 ZIP 选项
 */
interface BuildZipOptions {
  mode: 'group' | 'all'
  tasks: ZipTask[]
  groupName?: string
  platform?: string
  batchNo?: string
  onProgress: () => void
  onError: (f: LabelBatchFileVO, idx: number, err: any) => void
  onFinally: (result: DownloadResult) => void
}

/**
 * 使用面单文件下载
 */
export function useLabelFileDownload() {
  /**
   * 将文件名转换为安全的文件名（移除特殊字符）
   */
  function makeSafeName(name: string): string {
    return name.replace(/[\\/:*?"<>|]/g, '_')
  }

  /**
   * 获取文件 Blob（带超时控制）
   * @param file 文件对象
   * @param signal 外部中止信号（可选）
   * @param timeout 超时时间（毫秒），默认 30 秒
   */
  async function fetchFileBlob(
    file: LabelBatchFileVO,
    signal?: AbortSignal,
    timeout = SINGLE_FILE_TIMEOUT_MS
  ): Promise<Blob> {
    // 创建单文件超时控制器
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), timeout)

    // 合并外部信号和内部超时信号
    const combinedSignal = signal
      ? (AbortSignal as any).any
        ? (AbortSignal as any).any([signal, controller.signal])
        : controller.signal
      : controller.signal

    try {
      // 优先使用外链
      if (file.downloadUrl) {
        try {
          const resp = await fetch(file.downloadUrl, { signal: combinedSignal })
          if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
          const blob = await resp.blob()
          if (!blob || blob.size === 0) throw new Error('文件内容为空')
          return blob
        } catch (e: any) {
          // 如果是中止错误，直接抛出；否则尝试回退到 objectKey
          if (e.name === 'AbortError') {
            throw new Error('下载超时')
          }
          // 有 objectKey 时尝试回退
          if (!file.objectKey) {
            throw new Error(`下载失败: ${e.message || '网络错误'}`)
          }
          console.warn(`外链下载失败，尝试使用 objectKey: ${e.message}`)
        }
      }

      // 使用 objectKey 从 OSS 下载
      if (file.objectKey) {
        const ossUrl = file.objectKey.startsWith('http')
          ? file.objectKey
          : `${OSS_BUCKET_DOMAIN}/${file.objectKey.replace(/^\//, '')}`
        const resp = await fetch(ossUrl, { signal: combinedSignal })
        if (!resp.ok) {
          if (resp.status === 404) throw new Error('文件不存在')
          if (resp.status === 403) throw new Error('无访问权限')
          throw new Error(`HTTP ${resp.status}`)
        }
        const blob = await resp.blob()
        if (!blob || blob.size === 0) throw new Error('文件内容为空')
        return blob
      }

      throw new Error('无可用下载地址')
    } catch (e: any) {
      if (e.name === 'AbortError') {
        throw new Error('下载超时')
      }
      throw e
    } finally {
      clearTimeout(timeoutId)
    }
  }

  /**
   * 可取消的并发映射函数
   * @param items 待处理项
   * @param worker 工作函数
   * @param concurrency 并发数
   * @param shouldCancel 取消检查函数，返回 true 时停止启动新任务
   */
  async function pMapCancellable<T, R>(
    items: T[],
    worker: (item: T, index: number) => Promise<R>,
    concurrency = 4,
    shouldCancel?: () => boolean
  ): Promise<{ results: R[]; cancelled: boolean }> {
    const ret: R[] = []
    let i = 0
    let cancelled = false
    const executing: Promise<void>[] = []

    const runOne = async (idx: number) => {
      ret[idx] = await worker(items[idx], idx)
    }

    while (i < items.length) {
      // 检查是否需要取消
      if (shouldCancel?.()) {
        cancelled = true
        break
      }

      const p = runOne(i++)
      executing.push(
        p.then(() => {
          const pos = executing.indexOf(p as any)
          if (pos > -1) executing.splice(pos, 1)
        })
      )
      if (executing.length >= concurrency) await Promise.race(executing)
    }

    // 等待正在执行的任务完成
    await Promise.all(executing)
    return { results: ret, cancelled }
  }

  /**
   * 生成唯一文件名（处理文件名冲突）
   * @param group 分组名
   * @param raw 原始文件名
   * @param usedNamesMap 已使用的文件名映射
   */
  function uniqueName(
    group: string,
    raw: string,
    usedNamesMap: Map<string, Map<string, number>>
  ): string {
    const base = makeSafeName(raw || 'file.pdf')
    if (!usedNamesMap.has(group)) usedNamesMap.set(group, new Map())
    const used = usedNamesMap.get(group)!
    const times = used.get(base) || 0
    used.set(base, times + 1)

    if (times === 0) return base

    const dot = base.lastIndexOf('.')
    if (dot > 0) return `${base.slice(0, dot)}(${times})${base.slice(dot)}`
    return `${base}(${times})`
  }

  /**
   * 生成 ZIP 文件名（避免连续的分隔符）
   * @param parts 文件名部分
   * @param suffix 后缀（如 .zip）
   */
  function buildZipFileName(parts: (string | undefined)[], suffix = '.zip'): string {
    const validParts = parts.filter(p => p && p.trim())
    const name = validParts.length > 0 ? validParts.join('-') : 'download'
    return makeSafeName(name) + suffix
  }

  /**
   * 构建并下载 ZIP 文件
   * @param opts 构建选项
   */
  async function buildAndDownloadZip(opts: BuildZipOptions): Promise<void> {
    const { mode, tasks, groupName, platform, batchNo, onProgress, onError, onFinally } = opts
    const zip = new JSZip()
    const folderCache = new Map<string, JSZip>()
    const usedNamesMap = new Map<string, Map<string, number>>()
    let successCount = 0
    let failedCount = 0
    let hasError = false
    let overallTimeout = false

    // 下载结果
    const result: DownloadResult = {
      successCount: 0,
      failedCount: 0,
      totalCount: tasks.length,
      cancelled: false
    }

    // 获取或创建文件夹
    const getFolder = (group: string): JSZip => {
      if (!folderCache.has(group)) {
        const f = zip.folder(makeSafeName(group)) as JSZip
        folderCache.set(group, f)
      }
      return folderCache.get(group) as JSZip
    }

    // 根据文件数量动态计算总超时时间
    const totalTimeout = BASE_TIMEOUT_MS + tasks.length * PER_FILE_TIMEOUT_MS
    const controller = new AbortController()
    const timer = setTimeout(() => {
      overallTimeout = true
      controller.abort()
    }, totalTimeout)

    try {
      // 并发下载所有文件（每个文件有独立的超时控制）
      // 当检测到错误时，停止启动新任务
      const { cancelled } = await pMapCancellable(
        tasks,
        async (t, globalIdx) => {
          const { file: f, group } = t
          try {
            const blob = await fetchFileBlob(f, controller.signal, SINGLE_FILE_TIMEOUT_MS)
            const display = uniqueName(
              group,
              (f.fileName as string) || (f.objectKey as string) || `file-${globalIdx}.pdf`,
              usedNamesMap
            )
            getFolder(group).file(display, blob)
            successCount += 1
            onProgress()
          } catch (err: any) {
            hasError = true
            failedCount += 1
            // 如果是整体超时导致的中止，添加特定错误信息
            if (overallTimeout && err?.message === '下载超时') {
              onError(f, globalIdx, new Error('整体下载超时，任务已取消'))
            } else {
              onError(f, globalIdx, err)
            }
            // 中止剩余请求
            controller.abort()
          }
        },
        CONCURRENCY,
        () => hasError // 检测到错误时停止启动新任务
      )

      // 更新结果
      result.successCount = successCount
      result.failedCount = failedCount
      result.cancelled = cancelled || hasError

      if (cancelled || hasError) {
        // 计算被取消的任务数量
        const processedCount = successCount + failedCount
        const cancelledCount = tasks.length - processedCount
        if (cancelledCount > 0) {
          result.cancelReason = `检测到下载失败，已取消剩余 ${cancelledCount} 个文件的下载`
        } else if (overallTimeout) {
          result.cancelReason = '整体下载超时，任务已取消'
        } else {
          result.cancelReason = '检测到下载失败，已停止下载'
        }
      }

      // 只有全部成功才生成并下载 ZIP，防止下载到文件不全的压缩包
      if (successCount > 0 && !hasError) {
        const blob = await zip.generateAsync({ type: 'blob' })
        const name =
          mode === 'group'
            ? buildZipFileName([platform, batchNo, groupName])
            : buildZipFileName([platform, batchNo, 'ALL'])
        fileDownload(blob, name)
      }
    } finally {
      clearTimeout(timer)
      onFinally(result)
    }
  }

  /**
   * 下载单个文件
   * @param file 文件对象
   * @returns Promise，成功时 resolve，失败时 reject 并携带错误信息
   */
  async function downloadSingle(file: LabelBatchFileVO): Promise<void> {
    // 优先使用外链直接打开（浏览器处理下载）
    if (file.downloadUrl) {
      window.open(file.downloadUrl, '_blank')
      return
    }

    // 使用 objectKey 构建 OSS 链接直接打开
    if (file.objectKey) {
      const ossUrl = file.objectKey.startsWith('http')
        ? file.objectKey
        : `${OSS_BUCKET_DOMAIN}/${file.objectKey.replace(/^\//, '')}`
      window.open(ossUrl, '_blank')
      return
    }

    throw new Error('无可用下载地址')
  }

  /**
   * 下载分组 ZIP
   * @param group 分组项
   * @param platform 平台名称
   * @param batchNo 批次号
   * @param onProgress 进度回调
   * @param onError 错误回调
   * @param onFinally 完成回调（携带下载结果）
   */
  async function downloadGroupZip(
    group: GroupItem,
    platform?: string,
    batchNo?: string,
    onProgress?: () => void,
    onError?: (f: LabelBatchFileVO, idx: number, err: any) => void,
    onFinally?: (result: DownloadResult) => void
  ): Promise<void> {
    const files = group.files.filter(f => !!(f?.downloadUrl || f?.objectKey))
    if (!files.length) return

    await buildAndDownloadZip({
      mode: 'group',
      groupName: group.name,
      platform,
      batchNo,
      tasks: files.map((f, idx) => ({ file: f, group: group.name || '未命名', idx })),
      onProgress: onProgress || (() => {}),
      onError: onError || (() => {}),
      onFinally: onFinally || (() => {})
    })
  }

  /**
   * 下载所有文件为 ZIP
   * @param groups 分组列表
   * @param platform 平台名称
   * @param batchNo 批次号
   * @param onProgress 进度回调
   * @param onError 错误回调
   * @param onFinally 完成回调（携带下载结果）
   */
  async function downloadAllZip(
    groups: GroupItem[],
    platform?: string,
    batchNo?: string,
    onProgress?: () => void,
    onError?: (f: LabelBatchFileVO, idx: number, err: any) => void,
    onFinally?: (result: DownloadResult) => void
  ): Promise<void> {
    const tasks: ZipTask[] = []
    groups.forEach(g => {
      g.files.forEach((f, i) => {
        if (f?.downloadUrl || f?.objectKey) {
          tasks.push({ file: f, group: g.name || '未命名', idx: i })
        }
      })
    })

    if (!tasks.length) return

    await buildAndDownloadZip({
      mode: 'all',
      tasks,
      platform,
      batchNo,
      onProgress: onProgress || (() => {}),
      onError: onError || (() => {}),
      onFinally: onFinally || (() => {})
    })
  }

  /**
   * 判断文件是否可下载
   * @param file 文件对象
   */
  function canDownload(file: LabelBatchFileVO): boolean {
    return !!(file?.downloadUrl || file?.objectKey)
  }

  return {
    downloadSingle,
    downloadGroupZip,
    downloadAllZip,
    fetchFileBlob,
    canDownload
  }
}
