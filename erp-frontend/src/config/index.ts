import type { ProSettings } from '#/layout/defaultSettings'
import { DEFAULT_LOCALE } from '../locales/locale-contract'

export type LanguageInfo = {
  lang: string
  title: string
  symbol: string
}

export type SupportLanguage = Record<string, LanguageInfo>

/**
 * 认证类型
 */
export enum AuthenticationType {
  // 表单登录
  FORM_LOGIN,
  // OAUTH2 密码授权模式
  OAUTH2_PASSWORD_GRANT_TYPE
}

/**
 * 鉴权方式
 */
export enum AuthenticationMethod {
  // 使用 cookie JSESSIONID 进行认证
  COOKIE_JSESSIONID,
  // 使用 OAUTH2 ACCESS_TOKEN 进行认证
  OAUTH2_ACCESS_TOKEN
  // // token 请求头
  // HEADER_TOKEN,
  // // 使用 cookie token, 便于在 OAuth2 授权服 务器使用，跳转可自动携带
  // COOKIE_TOKEN
}

// 项目标题
export const projectTitle = 'HYLDSys ERP'
// 项目描述
export const projectDesc = '让复杂留在系统，把简单留给生意'

// Local Storage/ Session Storage 的 key 前缀 prefix
export const storageKeyPrefix = 'erp/'

// 认证类型
export const authenticationType: AuthenticationType = AuthenticationType.FORM_LOGIN

// 鉴权方式
export const authenticationMethod: AuthenticationMethod = AuthenticationMethod.COOKIE_JSESSIONID

// 开启 websocket，开启此选项需要服务端同步支持 websocket 功能
// 若服务端不支持，则本地启动时，抛出 socket 异常，导致 proxyServer 关闭
export const enableWebsocket = false

// 开启布局设置
export const enableLayoutSetting = false

// 开启登录验证码
export const enableLoginCaptcha = false

// 是否开启国际化
export const enableI18n = true
// 项目默认语言
export const defaultLanguage = DEFAULT_LOCALE
// 支持的语言信息
export const supportLanguage: SupportLanguage = {
  'zh-CN': {
    lang: 'zh-CN',
    title: '简体中文',
    symbol: '🇨🇳'
  },
  'en-US': {
    lang: 'en-US',
    title: 'English',
    symbol: '🇺🇸'
  },
  'uk-UA': {
    lang: 'uk-UA',
    title: 'Українська',
    symbol: '🇺🇦'
  },
  'ru-RU': {
    lang: 'ru-RU',
    title: 'Русский',
    symbol: '🇷🇺'
  }
}

// 路由布局的组件名称
export const routerLayoutName = 'RouterLayout'
// 刷新时占位的空组件名
export const emptyNodeName = '_EmptyNode'
// 重定向的路由路径
export const redirectPath = '/redirect'
// 登录页的地址
export const loginPath = '/login'

/* 应用设置 */
export const appSettings: ProSettings = {
  navTheme: 'dark',
  headerTheme: 'dark',
  layout: 'side',
  contentWidth: 'Fluid',
  fixedHeader: false,
  fixSiderbar: true,
  headerHeight: 48,
  iconfontUrl: '',
  primaryColor: '#1890ff',
  splitMenus: false,

  // 布局内容默认都渲染
  headerRender: undefined,
  footerRender: undefined,
  menuRender: undefined,
  menuHeaderRender: undefined
}
