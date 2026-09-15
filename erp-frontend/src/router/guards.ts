import NProgress from 'nprogress'
import '@/styles/nprogress.less'

import type { RouteLocationNormalizedLoaded, Router } from 'vue-router'
import {
  AuthenticationMethod,
  authenticationMethod,
  loginPath,
  projectTitle,
  redirectPath
} from '@/config'
import { useUserStore } from '@/stores/user-store'
import { generatorDynamicRouter } from '@/router/dynamic-routes'

NProgress.configure({ showSpinner: false })

function checkLogin() {
  switch (authenticationMethod) {
    // 如果是 JSESSIONID, 由于 cookie 是 httpOnly 的，直接放行，交给服务端检测
    case AuthenticationMethod.COOKIE_JSESSIONID:
      return true
    // 如果是 ACCESS_TOKEN, 先判断下本地是否有存储
    case AuthenticationMethod.OAUTH2_ACCESS_TOKEN:
      return useUserStore().accessToken
  }
}

const routerGuards = (router: Router) => {
  router.beforeEach(async (to, from) => {
    NProgress.start()

    if (checkLogin()) {
      // 如果已经登录的情况下访问登录页，直接跳转到首页
      if (
        authenticationMethod === AuthenticationMethod.OAUTH2_ACCESS_TOKEN &&
        to.path === loginPath
      ) {
        NProgress.done()
        return { path: '/' }
      }

      // 如果是 layout 内部的页面，且没有动态路由，则更新
      const userStore = useUserStore()
      if (!to.meta.withoutLayout && (!userStore.userMenus || userStore.userMenus.length === 0)) {
        let userMenus
        try {
          userMenus = await userStore.fetchUserMenus()
        } catch {
          // A persisted token can outlive the server session. Do not leave the
          // initial navigation rejected (a blank page); reset it and log in again.
          userStore.clean()
          NProgress.done()
          return {
            path: loginPath,
            query: { redirect: to.fullPath }
          }
        }
        // 仅当拉到非空菜单才重建动态路由并重导航；
        // 空菜单直接放行（fail-closed，空侧边栏），避免 length===0 反复重导航形成死循环
        if (userMenus && userMenus.length > 0) {
          const dynamicRouter = generatorDynamicRouter(userMenus)
          router.addRoute(dynamicRouter)
          return to.fullPath
        }
      }

      // 安全网：动态路由已按当前身份重建后，若目标命中 404（多为换身份登录时
      // 残留的 redirect/历史地址指向上一身份才有的路由），回首页而非死在 404 页。
      if (isNotFoundRoute(to) && (from.path === loginPath || from.matched.length === 0)) {
        return { path: '/' }
      }
    } else if (!to.meta.allowAnonymous) {
      // 如果没有登录，访问地址又不允许匿名访问，就跳转到登录页
      return {
        path: loginPath,
        query: {
          redirect: to.fullPath
        }
      }
    }

    updateDocumentTitle(to)
    return true
  })

  router.afterEach(() => {
    NProgress.done()
  })
}

/** 是否命中 404 路由（全局或 content 内的 NotFound，meta.name 统一为 '404'） */
const isNotFoundRoute = function (route: RouteLocationNormalizedLoaded): boolean {
  return route.matched.some(record => record.meta?.name === '404')
}

/** 更新标签页标题 */
const updateDocumentTitle = function (route: RouteLocationNormalizedLoaded) {
  if (!route?.path?.startsWith(`${redirectPath}/`)) {
    const pageTitle = route?.meta?.name
    document.title = pageTitle ? `${pageTitle} - ${projectTitle}` : projectTitle
  }
}

export default routerGuards
