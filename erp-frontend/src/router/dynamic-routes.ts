import { listToTree } from '@/utils/tree-utils'
import { firstUpperCase } from '@/utils/str-utils'
import type { SysMenuRouterVO } from '@/api/system/menu/types'
import type { RouteMeta, RouteRecordRaw } from 'vue-router'
import { buildNotFoundRoute, ExceptionComponentImport, HOME_ROUTE } from '@/router/constant-routes'

type SysMenuRouterTree = SysMenuRouterVO & { key: number; children: SysMenuRouterTree[] }

// 动态组件模块
const dynamicViewModules = import.meta.glob('/src/views/**/*.{vue,tsx}')

/**
 * 前端组件覆盖（按菜单 path）。
 * 部分菜单在 DB(sys_menu.uri) 中仍指向占位组件 common/Placeholder，
 * 这里按菜单 path 覆盖为真实页面，避免改动数据库。
 * - platform-dashboard(900300)：海外仓平台「平台数据分析」看板。
 */
const COMPONENT_OVERRIDES: Record<string, () => Promise<unknown>> = {
  'platform-dashboard': () => import('@/views/dashboard/platform/index.vue'),
  // receivable-bill(170601)：海外仓平台「平台财务 · 应收账单」
  'receivable-bill': () => import('@/views/platform-finance/receivable-bill/index.vue'),
  // picking(170503)：海外仓平台「海外仓作业 · 下架(拣货)」
  picking: () => import('@/views/platform/outbound-ops/OutboundPickingPage.vue'),
  // packing(170504)：海外仓平台「海外仓作业 · 打包签出」
  packing: () => import('@/views/platform/outbound-ops/PackShipPage.vue'),
  // return-qc(170505)：海外仓平台「海外仓作业 · 退货质检」
  'return-qc': () => import('@/views/platform/return-ops/ReturnQcPage.vue'),
  // inbound-ops(170501)：海外仓平台「入库收货」（收货与上架已拆分为两页）
  'inbound-ops': () => import('@/views/platform/inbound-ops/InboundReceivePage.vue'),
  // putaway(170502)：海外仓平台「入库上架」
  putaway: () => import('@/views/platform/inbound-ops/InboundPutawayPage.vue'),
  'location-inventory': () => import('@/views/platform/location-inventory/index.vue'),
  // Compatible with databases that have not applied the menu cutover migration yet.
  pallets: () => import('@/views/platform/location-inventory/index.vue'),
  // rack-inventory(180400)：WMS 服务商「仓储概览」（原“货架库存(只读)”占位，改真实只读页）
  'rack-inventory': () => import('@/views/wms/storage-overview/index.vue')
}

export const generatorDynamicRouter = (userMenus: SysMenuRouterVO[]): RouteRecordRaw => {
  const routes: RouteRecordRaw = { ...HOME_ROUTE }
  // 后端数据, 根级树数组,  根级 PID
  const menuTree = listToTree(userMenus, 0) as SysMenuRouterTree[]
  const dynamicRoutes = menuToRoutes(menuTree)

  // 将动态路由和静态子路由合并
  routes.children = [...(HOME_ROUTE.children || []), ...dynamicRoutes]
  routes.children.push(buildNotFoundRoute('PageNotFound'))
  fillRedirect(routes)

  return routes
}

const menuToRoutes = (menuTree: SysMenuRouterTree[], parent?: RouteRecordRaw) => {
  return menuTree.map(item => {
    // 内容打开方式
    const targetType = item.targetType
    let path = `${(parent && parent.path) || ''}/${item.path}`

    // 路由名称，由路由地址生成，大驼峰形式
    const name = path
      .replace('-', '/')
      .split('/')
      .filter(x => x && x !== '')
      .map(x => firstUpperCase(x))
      .join('')

    const meta: RouteMeta = {
      name: item.title,
      icon: item.icon || undefined,
      targetType: targetType
    }

    let component
    switch (item.type) {
      case 0:
        // 目录类型组件, 固定使用 RouterLayout
        component = () => import('@/layouts/RouterLayout.vue')
        break
      case 1:
        // 前端覆盖优先：菜单 path 命中覆盖表则直接用真实页面（绕过占位组件）
        if (COMPONENT_OVERRIDES[item.path]) {
          component = COMPONENT_OVERRIDES[item.path]
          break
        }
        // 菜单类型需要拼接组件地址
        if (targetType === 1) {
          // 内置组件
          item.uri && (component = getComponent(item.uri))
        } else if (targetType === 2) {
          // 内嵌iframe
          meta.target = item.uri
          component = () => import('@/views/basic/iframe/index.vue')
        } else if (targetType === 3) {
          // 外链
          path = item.uri
          meta.target = item.uri
          meta.hideInTab = true
        }
    }

    // 是否设置了隐藏菜单
    if (item.hidden === 1) {
      meta.hideInMenu = true
    }

    // @ts-ignore
    const currentRouter: RouteRecordRaw = {
      // 如果路由设置了 path，则作为默认 path，否则 路由地址 动态拼接生成如 /dashboard/workplace
      path,
      // 路由名称，建议唯一
      name,
      // meta: 页面标题, 菜单图标, 页面权限(供指令权限用，可去掉)
      meta,
      // 组件
      component
    }

    // 有子菜单则递归处理
    if (item.children && item.children.length > 0) {
      // 给子节点添加一个默认的 404 页面，以便在 content 中显示 404
      currentRouter.children = menuToRoutes(item.children, currentRouter)
      fillRedirect(currentRouter)
    }
    return currentRouter
  })
}

/**
 * 设置当前路由的默认跳转地址为其子路由的path
 * @param currentRouter
 */
function fillRedirect(currentRouter: RouteRecordRaw) {
  if (!currentRouter.children) return
  const redirectRouter = currentRouter.children.find(x => !x.meta?.hideInMenu)
  redirectRouter && (currentRouter.redirect = redirectRouter.path)
}

/**
 * 动态获取组件
 * @param componentPath 组件地址
 */
const getComponent = function (componentPath: string) {
  // 如果有后缀，直接返回
  const isFullPath = componentPath.endsWith('.vue') || componentPath.endsWith('.tsx')
  if (isFullPath) {
    return dynamicViewModules[componentPath]
  }

  // 没有后缀的情况下，按顺序尝试加载
  let viewModule = dynamicViewModules[`/src/views/${componentPath}.vue`]
  if (!viewModule) {
    viewModule = dynamicViewModules[`/src/views/${componentPath}/index.vue`]
  }
  if (!viewModule) {
    viewModule = dynamicViewModules[`/src/views/${componentPath}.tsx`]
  }
  if (!viewModule) {
    viewModule = dynamicViewModules[`/src/views/${componentPath}/index.tsx`]
  }
  if (!viewModule) {
    import.meta.env.DEV &&
      console.warn(
        '在src/views/下找不到`' +
          componentPath +
          '.vue` 或 `' +
          componentPath +
          '.tsx`, 请自行创建!'
      )
    viewModule = ExceptionComponentImport
  }

  return viewModule
}
