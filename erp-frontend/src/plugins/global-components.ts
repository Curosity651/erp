import type { App, Component } from 'vue'

// 共享 UI 组件 barrel。这些组件在模板里以 kebab 标签直接使用（如 <search-actions>、
// <dict-select>、<operation-group>），原依赖 unplugin-vue-components 的 src/components
// 目录扫描自动全局注册。但本项目所在绝对路径含括号 `(1)`（hyldsys-erp-master(1)），
// fast-glob 会把括号当作分组语法，导致 src/components/**/*.vue 扫描匹配为空 →
// 全部项目自有组件的裸标签「Failed to resolve component」→ 表单/搜索/操作列控件不渲染
// （性别/状态消失、搜索按钮消失、操作列按钮消失等）。AntDesignVueResolver 走解析器不依赖
// 目录扫描故不受影响，显式 import 的组件也不受影响。
//
// 这里显式集中注册，绕过坏掉的目录扫描，一次修复全站裸标签用法。
import * as ButtonComponents from '@/components/Button'
import * as DictComponents from '@/components/Dict'
import * as SearchComponents from '@/components/Search'
import * as OperationComponents from '@/components/Operation'
import * as LovComponents from '@/components/Lov'
import * as FileComponents from '@/components/File'

const registry: Record<string, Component> = {
  ...ButtonComponents,
  ...DictComponents,
  ...SearchComponents,
  ...OperationComponents,
  ...LovComponents,
  ...FileComponents
} as Record<string, Component>

/**
 * 全局注册共享 UI 组件（PascalCase 名，Vue 自动匹配 kebab 标签）。
 */
export function installGlobalComponents(app: App): void {
  for (const [name, comp] of Object.entries(registry)) {
    if (comp && (typeof comp === 'object' || typeof comp === 'function')) {
      app.component(name, comp)
    }
  }
}
