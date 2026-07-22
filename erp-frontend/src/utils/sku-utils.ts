export interface SkuFileItem {
  objectKey?: string
  fileUrl?: string
  url?: string
}
export interface SkuFilesMap {
  [key: string]: SkuFileItem[]
}

/**
 * 编码 URL 路径中的特殊字符（保留协议和域名部分）
 * 处理中文、括号、空格等特殊字符，确保图片 URL 可以正常访问
 *
 * 示例：
 * - "https://oss.com/产品(新).jpg" => "https://oss.com/%E4%BA%A7%E5%93%81(%E6%96%B0).jpg"
 * - "https://oss.com/测试 文件.png" => "https://oss.com/%E6%B5%8B%E8%AF%95%20%E6%96%87%E4%BB%B6.png"
 *
 * @param url 原始 URL
 * @returns 编码后的 URL
 */
function encodeUrlPath(url: string): string {
  try {
    // 如果是完整 URL（包含协议）
    if (url.startsWith('http://') || url.startsWith('https://')) {
      const urlObj = new URL(url)
      // 对路径的每个段分别编码（避免编码斜杠 '/'）
      // encodeURIComponent 会编码所有特殊字符，包括中文、括号、空格等
      const encodedPath = urlObj.pathname
        .split('/')
        .map(segment => (segment ? encodeURIComponent(segment) : ''))
        .join('/')
      return `${urlObj.origin}${encodedPath}${urlObj.search}${urlObj.hash}`
    }
    // 如果是相对路径，直接编码每个段
    return url
      .split('/')
      .map(segment => (segment ? encodeURIComponent(segment) : ''))
      .join('/')
  } catch (error) {
    // 如果 URL 解析失败，返回原始 URL
    console.warn('URL encoding failed:', url, error)
    return url
  }
}

/**
 * 获取 SKU 主图，优先 platform_image -> actual_image -> product_image
 * 如果所有图片都不存在或无效，返回默认占位图
 * @param files 文件分组 map
 * @param ossDomain OSS 域名（可选）
 * @returns 图片 URL，始终返回有效的字符串（可能是占位图）
 */
export function getSkuMainImage(files?: SkuFilesMap, ossDomain?: string): string {
  const PLACEHOLDER = '/placeholder-product.svg'

  // 如果 files 为空，直接返回占位图
  if (!files) return PLACEHOLDER

  /**
   * 从指定类型中提取图片 URL
   */
  const pick = (type: string): string | undefined => {
    const arr = files[type]
    if (!arr || arr.length === 0) return undefined

    const first = arr[0]

    // 私有 OSS 必须优先使用后端生成的限时签名 URL。
    if (first.fileUrl) {
      return first.fileUrl
    } else if (first.url) {
      return encodeUrlPath(first.url)
    } else if (first.objectKey && ossDomain) {
      // 只对 objectKey 进行编码（域名部分不需要编码）
      const encodedObjectKey = first.objectKey
        .split('/')
        .map(segment => (segment ? encodeURIComponent(segment) : ''))
        .join('/')
      return `${ossDomain}/${encodedObjectKey}`
    }
    return undefined
  }

  // 按优先级尝试获取图片
  const imageUrl = pick('platform_image') || pick('actual_image') || pick('product_image')

  // 如果获取到的是占位图本身，也返回占位图
  // 这样可以统一处理各种边界情况
  return imageUrl && imageUrl !== PLACEHOLDER ? imageUrl : PLACEHOLDER
}
