export const toCssBgUrl = (src?: string) => {
  if (!src) return ''
  // 使用 encodeURI 处理中文/空格等；用引号包裹避免括号破坏 url() 解析
  return `url("${encodeURI(src)}")`
}
