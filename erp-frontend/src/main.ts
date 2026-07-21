import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from '@/router'
import { install as installI18n } from '@/locales'

// 公共工具类（布局、间距、文本）
import '@/styles/utilities.css'
// WMS 模块样式变量
import '@/views/wms/styles/variables.css'
// 全局样式
import '@/styles/global.less'

// 注意：ant-design-vue 4.x 使用 CSS-in-JS，不再需要手动导入组件样式
// 以下导入已移除：
// - 'ant-design-vue/es/message/style/index.less'
// - 'ant-design-vue/es/notification/style/index.less'
// - 'ant-design-vue/es/modal/style/index.less'

import App from './App.vue'
import { enableI18n } from '@/config'

const app = createApp(App)
app.use(createPinia())

// 全局注册共享组件（绕过因路径含 `(1)` 而失效的 unplugin 目录扫描）。
// 用动态 import 在 pinia 挂载之后再加载，避免其组件图里 dict-store 的模块级
// useDictStore() 在 pinia 就绪前执行而报 getActivePinia() 错误。
async function bootstrap() {
  const { installGlobalComponents } = await import('@/plugins/global-components')
  installGlobalComponents(app)

  if (enableI18n) {
    await installI18n(app)
  }
  app.use(router)
  app.mount('#app')
}

bootstrap()
