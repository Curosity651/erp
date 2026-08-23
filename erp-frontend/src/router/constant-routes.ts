// notFound 路由，使用函数获取，方便使用不同的路由名称，支持登录后 content 中显示异常信息
import type { RouteRecordRaw } from 'vue-router'
import { loginPath } from '@/config'

export const ExceptionComponentImport = () => import('@/views/basic/exception/index.vue')

export const buildNotFoundRoute = (routeName: string): RouteRecordRaw => ({
  path: '/:pathMatch(.*)*',
  name: routeName,
  component: ExceptionComponentImport,
  meta: {
    name: '404',
    hideInMenu: true,
    hideInTab: true
  },
  props: {
    exceptionStatus: '404'
  }
})

// 根路由
export const HOME_ROUTE: RouteRecordRaw = {
  path: '/',
  name: '/',
  component: () => import('@/layouts/BasicLayout.vue'),
  meta: {
    keepAlive: false
  },
  children: [
    {
      path: 'ops/fulfillment-picking/work/:taskId',
      name: 'FulfillmentPickingWork',
      component: () =>
        import('@/views/platform/fulfillment-picking/FulfillmentPickingWorkPage.vue'),
      meta: {
        name: '拣货任务作业',
        hideInMenu: true,
        hideInTab: false,
        keepAlive: false
      }
    },
    // SKU表单路由 - 作为根路由的子路由
    {
      path: 'product/sku/form/:mode/:id?',
      name: 'SkuForm',
      component: () => import('@/views/product/sku/SkuFormPage.vue'),
      meta: {
        name: 'SKU表单',
        hideInMenu: true,
        hideInTab: false,
        keepAlive: true
      }
    },
    // SKU销量排名详情页
    {
      path: 'statistics/sku-ranking',
      name: 'SkuRanking',
      component: () => import('@/views/dashboard/SkuRanking.vue'),
      meta: {
        name: 'SKU销量排名',
        hideInMenu: true,
        hideInTab: false,
        keepAlive: true
      }
    },
    // 采购单表单路由
    {
      path: 'wms/purchase-order/form/:mode/:id?',
      name: 'PurchaseOrderForm',
      component: () => import('@/views/wms/purchase-order/PurchaseOrderFormPage.vue'),
      meta: {
        name: '采购单表单',
        hideInMenu: true,
        hideInTab: false,
        keepAlive: true
      }
    },
    // 物流单表单路由
    {
      path: 'wms/shipping-order/form/:mode/:id?',
      name: 'ShippingOrderForm',
      component: () => import('@/views/wms/shipping-order/ShippingOrderFormPage.vue'),
      meta: {
        name: '物流单表单',
        hideInMenu: true,
        hideInTab: false,
        keepAlive: true
      }
    },
    // 采购入库单表单路由
    {
      path: 'wms/purchase-inbound/form/:mode/:id?',
      name: 'PurchaseInboundForm',
      component: () => import('@/views/wms/purchase-inbound/PurchaseInboundFormPage.vue'),
      meta: {
        name: '采购入库单表单',
        hideInMenu: true,
        hideInTab: false,
        keepAlive: true
      }
    },
    // 自定义入库单表单路由
    {
      path: 'wms/manual-inbound/form/:mode/:id?',
      name: 'ManualInboundForm',
      component: () => import('@/views/wms/manual-inbound/ManualInboundFormPage.vue'),
      meta: {
        name: '自定义入库单表单',
        hideInMenu: true,
        hideInTab: false,
        keepAlive: true
      }
    },
    // 自定义退货单表单路由
    {
      path: 'wms/custom-return/form/:mode/:id?',
      name: 'CustomReturnForm',
      component: () => import('@/views/wms/custom-return/CustomReturnFormPage.vue'),
      meta: {
        name: '自定义退货单表单',
        hideInMenu: true,
        hideInTab: false,
        keepAlive: true
      }
    },
    // 调拨单表单路由
    {
      path: 'wms/transfer-order/form/:mode/:id?',
      name: 'TransferOrderForm',
      component: () => import('@/views/wms/transfer-order/TransferOrderFormPage.vue'),
      meta: {
        name: '调拨单表单',
        hideInMenu: true,
        hideInTab: false,
        keepAlive: true
      }
    },
    // 盘点录入页面路由
    {
      path: 'wms/stocktake/input/:id',
      name: 'StocktakeInput',
      component: () => import('@/views/wms/stocktake/StocktakeInputPage.vue'),
      meta: {
        name: '盘点录入',
        hideInMenu: true,
        hideInTab: false,
        keepAlive: true
      }
    },
    {
      path: 'wms/stocktake/review/:id',
      name: 'StocktakeReview',
      component: () => import('@/views/wms/stocktake/StocktakeReviewPage.vue'),
      meta: {
        name: '盘点差异复核',
        hideInMenu: true,
        hideInTab: false,
        keepAlive: true
      }
    },
    // 销售出库单单表单路由
    {
      path: 'wms/sales-outbound/form/:mode/:id?',
      name: 'SalseIOutboundForm',
      component: () => import('@/views/wms/sales-outbound/SalesOutboundFormPage.vue'),
      meta: {
        name: '销售出库单表单',
        hideInMenu: true,
        hideInTab: false,
        keepAlive: true
      }
    },
    // 自定义出库单表单路由
    {
      path: 'wms/custom-outbound/form/:mode/:id?',
      name: 'CustomOutboundForm',
      component: () => import('@/views/wms/custom-outbound/CustomOutboundFormPage.vue'),
      meta: {
        name: '自定义出库单表单',
        hideInMenu: true,
        hideInTab: false,
        keepAlive: true
      }
    }
  ]
}

const constantRoutes: RouteRecordRaw[] = [
  {
    path: loginPath,
    name: 'Login',
    // route level code-splitting
    // this generates a separate chunk (About.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () => import('@/views/login/index.vue'),
    meta: {
      allowAnonymous: true,
      withoutLayout: true
    }
  },
  {
    // OAuth2 授权码登录处理页
    path: '/oauth2/authorize',
    name: 'OAuth2Authorize',
    component: () => import('@/views/oauth2/OAuth2Authorize.vue'),
    meta: {
      withoutLayout: true
    }
  },
  {
    // OAuth2 授权码登录处理页
    path: '/oauth2/consent',
    name: 'OAuth2Consent',
    component: () => import('@/views/oauth2/OAuth2Consent.vue'),
    meta: {
      withoutLayout: true
    }
  },
  // 主应用路由
  HOME_ROUTE,
  // 404 路由
  buildNotFoundRoute('GlobalNotFound')
]

// 静态路由的名称集合
export const constantRouteNames: string[] = []
const fillConstantRouteNames = (array: any[]) =>
  array.forEach(item => {
    constantRouteNames.push(item.name)
    fillConstantRouteNames(item.children || [])
  })
fillConstantRouteNames(constantRoutes)

export default constantRoutes
