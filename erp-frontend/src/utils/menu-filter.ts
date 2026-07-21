import type { SysMenuRouterVO } from '@/api/system/menu/types'

/**
 * 各身份可见的顶级菜单 path（三平台隔离）。
 * - 货主 ERP_USER：商品/订单/财务(WB)/我的仓储(OMS单据)/库存管理/系统/日志/数据分析/个人页。
 * - WMS服务商 WMS_OPERATOR（纯中间人）：服务商运营 + 系统 + 个人页。
 * - 海外仓平台 OVERSEAS_PLATFORM：平台管理(客户/仓库/作业/平台财务) + 系统 + 日志 + 数据分析 + 个人页。
 */
const ERP_USER_TOP_PATHS = [
  'statistics',
  'product',
  'order',
  'financial',
  'wms',
  'inventory',
  'system',
  'log',
  'account'
]
const WMS_OPERATOR_TOP_PATHS = ['wms-console', 'system', 'account']
// 海外仓平台顶级：客户管理/仓库管理/海外仓作业/平台财务（V17 拍平）
const OVERSEAS_PLATFORM_TOP_PATHS = [
  'customer-mgmt',
  'warehouse-mgmt',
  'ops',
  'platform-finance',
  'system',
  'log',
  'account',
  'statistics'
]

/**
 * 系统管理（system 100000）按实例隔离（RBAC 阶段2 / V20）。各身份允许的系统子模块：
 * - 货主 ERP_USER：用户100100/角色100200/组织100700/店铺101100/项目组100900/岗位101000。
 * - WMS服务商 WMS_OPERATOR：用户100100/角色100200（不含组织架构）。
 * - 海外仓平台 OVERSEAS_PLATFORM：用户100100/角色100200/组织100700/字典100500/配置100400/菜单权限100800/项目组100900/岗位101000。
 * 字典/配置/菜单权限仅海外仓平台可管；用户/组织/角色按实例隔离（后端 sys_user/sys_organization/sys_role 行级隔离）。
 */
const SYSTEM_TOP_ID = 100000
const ERP_SYSTEM_MODULE_IDS = new Set<number>([100100, 100200, 100700, 101100, 100900, 101000])
const WMS_SYSTEM_MODULE_IDS = new Set<number>([100100, 100200])
const PLATFORM_SYSTEM_MODULE_IDS = new Set<number>([
  100100, 100200, 100700, 100500, 100400, 100800, 100900, 101000
])

/**
 * 数据分析（statistics 900000）按身份裁剪：三身份各看各的看板。
 * - 货主：Dashboard(900100) + 销售目标(900200)。
 * - 海外仓平台：平台数据分析(900300)。
 * - WMS服务商：运营数据分析(900400)。
 */
const STATISTICS_TOP_ID = 900000
const ERP_DASHBOARD_IDS = new Set<number>([900100, 900200])
const PLATFORM_DASHBOARD_IDS = new Set<number>([900300])
const WMS_DASHBOARD_IDS = new Set<number>([900400])

// 货主端隐藏的业务菜单根节点；其页面与接口仍保留，供其他身份或后续恢复使用。
const ERP_HIDDEN_MENU_ROOT_IDS = new Set<number>([162004])

/**
 * 按身份层级裁剪菜单（页面分配 V10）。
 *
 * 入参为后端 /system/menu/router 返回的扁平菜单数组（含 parentId）。
 * 后端对全部菜单统一授 ROLE_ADMIN，真正的身份可见性由本函数裁剪。
 *
 * @param menus 扁平菜单数组
 * @param identityType 当前用户身份层级（OVERSEAS_PLATFORM / WMS_OPERATOR / ERP_USER）
 * @return 裁剪后的扁平菜单数组
 */
export function filterMenusByIdentity(
  menus: SysMenuRouterVO[],
  identityType?: string
): SysMenuRouterVO[] {
  if (!menus || menus.length === 0) {
    return menus
  }
  // 未知身份一律不放行任何菜单（fail-closed）：未登录/身份缺失（如残留会话、身份接口失败）
  // 时绝不暴露全量菜单，避免越权显示。正常登录会先写入 identityType 再拉菜单，不受影响。
  if (
    identityType !== 'OVERSEAS_PLATFORM' &&
    identityType !== 'WMS_OPERATOR' &&
    identityType !== 'ERP_USER'
  ) {
    return []
  }

  const byId = new Map<number, SysMenuRouterVO>()
  menus.forEach(m => byId.set(m.id, m))

  // 取某菜单的顶级祖先 path（沿 parentId 上溯至 parentId===0）
  const topPath = (menu: SysMenuRouterVO): string | undefined => {
    let cur: SysMenuRouterVO | undefined = menu
    const guard = new Set<number>()
    while (cur && cur.parentId && cur.parentId !== 0) {
      if (guard.has(cur.id)) break // 防止脏数据成环
      guard.add(cur.id)
      cur = byId.get(cur.parentId)
    }
    return cur?.path
  }

  // 取“直属某顶级容器”的那个子节点 id（用于按模块裁剪子树）；菜单即容器本身返回 undefined
  const moduleUnderTop = (menu: SysMenuRouterVO, topId: number): number | undefined => {
    let cur: SysMenuRouterVO | undefined = menu
    let child: SysMenuRouterVO | undefined = undefined
    const guard = new Set<number>()
    while (cur) {
      if (cur.id === topId) return child?.id
      if (!cur.parentId || cur.parentId === 0) break
      if (guard.has(cur.id)) break
      guard.add(cur.id)
      child = cur
      cur = byId.get(cur.parentId)
    }
    return undefined
  }

  const isUnderHiddenRoot = (menu: SysMenuRouterVO, roots: Set<number>): boolean => {
    let cur: SysMenuRouterVO | undefined = menu
    const guard = new Set<number>()
    while (cur) {
      if (roots.has(cur.id)) return true
      if (!cur.parentId || cur.parentId === 0 || guard.has(cur.id)) break
      guard.add(cur.id)
      cur = byId.get(cur.parentId)
    }
    return false
  }

  const keep = (m: SysMenuRouterVO): boolean => {
    const top = topPath(m)

    // 系统管理子树：按实例隔离，各身份允许的系统模块集不同（用户/角色/组织各管各的）
    if (top === 'system') {
      if (m.id === SYSTEM_TOP_ID) return true // 保留容器
      const mod = moduleUnderTop(m, SYSTEM_TOP_ID)
      if (mod === undefined) return false
      if (identityType === 'ERP_USER') return ERP_SYSTEM_MODULE_IDS.has(mod)
      if (identityType === 'WMS_OPERATOR') return WMS_SYSTEM_MODULE_IDS.has(mod)
      return PLATFORM_SYSTEM_MODULE_IDS.has(mod) // OVERSEAS_PLATFORM
    }

    // 数据分析子树：三身份各自看板
    if (top === 'statistics') {
      if (m.id === STATISTICS_TOP_ID) return true // 保留容器
      const mod = moduleUnderTop(m, STATISTICS_TOP_ID)
      if (mod === undefined) return false
      if (identityType === 'ERP_USER') return ERP_DASHBOARD_IDS.has(mod)
      if (identityType === 'OVERSEAS_PLATFORM') return PLATFORM_DASHBOARD_IDS.has(mod)
      return WMS_DASHBOARD_IDS.has(mod) // WMS_OPERATOR
    }

    if (identityType === 'ERP_USER') {
      if (isUnderHiddenRoot(m, ERP_HIDDEN_MENU_ROOT_IDS)) return false
      // 货主：可见顶级内全部（盘点/调拨/库存调整已迁至 ops 顶级，自然被排除）
      return !!top && ERP_USER_TOP_PATHS.includes(top)
    }
    if (identityType === 'WMS_OPERATOR') {
      // WMS服务商：仅服务商运营 + 系统(上面已处理) + 个人页
      return !!top && WMS_OPERATOR_TOP_PATHS.includes(top)
    }
    // OVERSEAS_PLATFORM：仅平台可见顶级（含 ops 下的入库收货上架/盘点/调拨/库存调整）
    return !!top && OVERSEAS_PLATFORM_TOP_PATHS.includes(top)
  }

  return menus.filter(keep)
}
