import { theme } from 'ant-design-vue'
import type { GlobalToken } from 'ant-design-vue/es/theme'
import { watch, onMounted } from 'vue'

/**
 * camelCase 转 kebab-case
 * colorPrimary -> color-primary
 * borderRadiusLG -> border-radius-lg
 */
function toKebabCase(str: string): string {
  return str
    .replace(/([a-z])([A-Z])/g, '$1-$2')
    .replace(/([A-Z])([A-Z][a-z])/g, '$1-$2')
    .toLowerCase()
}

/**
 * 需要添加 px 单位的尺寸类属性关键词
 */
const SIZE_KEYWORDS = [
  'radius',
  'width',
  'height',
  'margin',
  'padding',
  'gap',
  'fontSize',
  'controlHeight',
  'controlOutlineWidth',
  'controlInteractiveSize',
  'screenXS',
  'screenSM',
  'screenMD',
  'screenLG',
  'screenXL',
  'screenXXL',
  'screenXXXL'
]

/**
 * 不需要 px 的数字属性（白名单排除）
 * - lineHeight: 比例值如 1.5714
 * - fontWeight: 字重如 600
 * - zIndex: 层级数字
 * - motionUnit/motionBase: 动效参数
 * - sizeUnit/sizeStep: 尺寸计算参数
 */
const NO_PX_PROPERTIES = [
  'lineHeight',
  'fontWeight',
  'zIndex',
  'motionUnit',
  'motionBase',
  'sizeUnit',
  'sizeStep'
]

function isSizeProperty(key: string): boolean {
  const lowerKey = key.toLowerCase()

  // 先检查是否在排除列表中
  if (NO_PX_PROPERTIES.some(prop => lowerKey.includes(prop.toLowerCase()))) {
    return false
  }

  return SIZE_KEYWORDS.some(keyword => lowerKey.includes(keyword.toLowerCase()))
}

/**
 * 应该排除的属性
 * - 内部属性（以 _ 开头）
 * - motionEase*: 缓动函数字符串，但保留 motionDuration*
 * - motionUnit/motionBase: 动效计算参数
 */
const EXCLUDE_PREFIXES = ['_', 'motionEase', 'motionUnit', 'motionBase']

function shouldExclude(key: string): boolean {
  return EXCLUDE_PREFIXES.some(prefix => key.startsWith(prefix))
}

/**
 * 格式化 token 值
 */
function formatValue(key: string, value: unknown): string | null {
  if (value === null || value === undefined) {
    return null
  }

  if (typeof value === 'number') {
    // 尺寸类属性添加 px 单位
    if (isSizeProperty(key)) {
      return `${value}px`
    }
    return String(value)
  }

  if (typeof value === 'string') {
    return value
  }

  return null
}

const STYLE_ID = 'antd-css-variables'

/**
 * 将 token 对象转为 CSS Variables 并通过 :root 规则注入
 */
function injectCssVariables(token: GlobalToken): void {
  if (typeof document === 'undefined') {
    return
  }

  // 构建 CSS 变量列表
  const cssVars: string[] = []

  Object.entries(token).forEach(([key, value]) => {
    if (shouldExclude(key)) {
      return
    }

    const formattedValue = formatValue(key, value)
    if (formattedValue === null) {
      return
    }

    const cssVarName = `--ant-${toKebabCase(key)}`
    cssVars.push(`  ${cssVarName}: ${formattedValue};`)
  })

  // 生成 :root 规则
  const cssContent = `:root {\n${cssVars.join('\n')}\n}`

  // 查找或创建 style 标签
  let styleEl = document.getElementById(STYLE_ID) as HTMLStyleElement | null
  if (!styleEl) {
    styleEl = document.createElement('style')
    styleEl.id = STYLE_ID
    styleEl.setAttribute('data-source', 'antd-token')
    document.head.appendChild(styleEl)
  }

  // 更新样式内容
  styleEl.textContent = cssContent
}

/**
 * 将 Ant Design Vue token 注入为全局 CSS Variables
 * 在 App.vue 中调用一次即可
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * import { useAntdCssVar } from '@/hooks/use-antd-css-var'
 * useAntdCssVar()
 * </script>
 * ```
 */
export function useAntdCssVar(): void {
  const { token } = theme.useToken()

  // 初始注入
  onMounted(() => {
    injectCssVariables(token.value)
  })

  // 监听 token 变化（主题切换时）
  watch(
    token,
    newToken => {
      injectCssVariables(newToken)
    },
    { deep: true }
  )
}

/**
 * 获取 CSS 变量名（供 TypeScript 类型提示）
 *
 * @example
 * ```ts
 * antdVar('colorPrimary') // 'var(--ant-color-primary)'
 * ```
 */
export function antdVar(tokenName: keyof GlobalToken): string {
  return `var(--ant-${toKebabCase(tokenName)})`
}
