-- =====================================================================
-- 货主端新增页面「发货生产测算」（挂在 库存管理 165000 下）
--   实现 model_core.py 测算模型：35天发货 / 80天生产 / 45天备货。
--   路由 = 父path(inventory) + 子path(ship-prod-calc) = /inventory/ship-prod-calc
--   组件走 sys_menu.uri = wms/ship-prod-calc/ShipProdCalcPage。
--   可见性/新租户授权：165000 已在 menu-filter 顶级白名单 + TenantRoleMapper 递归根集，
--   本菜单作为其子孙自动覆盖，无需改前端/XML。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V38__ship_prod_calc_menu.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

INSERT INTO sys_menu (id, parent_id, title, icon, path, target_type, uri, sort, keep_alive, hidden, type)
VALUES (165100, 165000, '发货生产测算', 'calculator', 'ship-prod-calc', 1,
        'wms/ship-prod-calc/ShipProdCalcPage', 6, 0, 0, 1);

-- 授权：凡持有 库存管理(165000) 的实例角色，补授本菜单（覆盖当前全部货主）
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT DISTINCT role_code, 165100 FROM sys_role_menu WHERE menu_id = 165000;
