-- =====================================================================
-- 货主端新增页面「资产与账务」（挂在 财务管理 150000 下）
--   资产 = 采购成本(A–E 持有 × 加权平均采购单价) + 物流附加(A+B+C 已发运 × 物流单位成本，USD)。
--   账务 = 应付生产采购商(采购单预付/尾款) + 应付物流商(物流单)。分币种不折算、零建表。
--   路由 = 父path(financial) + 子path(asset-finance) = /financial/asset-finance
--   组件走 sys_menu.uri = wms/asset-finance/AssetFinancePage。
--   可见性/新租户授权：150000 已在 menu-filter 顶级白名单(financial) + TenantRoleMapper 递归根集，
--   本菜单作为其子孙自动覆盖，无需改前端/XML。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V39__asset_finance_menu.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

INSERT INTO sys_menu (id, parent_id, title, icon, path, target_type, uri, sort, keep_alive, hidden, type)
VALUES (150200, 150000, '资产与账务', 'wallet', 'asset-finance', 1,
        'wms/asset-finance/AssetFinancePage', 3, 0, 0, 1);

-- 授权：凡持有 财务管理(150000) 的实例角色，补授本菜单（覆盖当前全部货主）
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT DISTINCT role_code, 150200 FROM sys_role_menu WHERE menu_id = 150000;
