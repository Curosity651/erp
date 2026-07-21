-- =====================================================================
-- 平台角色补授 product:sku:read（菜单 130401）。
--   盘点录入「追加SKU」弹窗走通用 SKU 选择接口 /product/sku/select-page，
--   该接口要求 product:sku:read；平台角色此前无任何 product 权限 → 403「没有权限访问」。
--   只授权限节点（type=2），不授其父页面菜单 130400，故不会在平台菜单里多出 SKU 管理页。
-- 幂等：NOT EXISTS 去重。
-- =====================================================================

INSERT INTO sys_role_menu (role_code, menu_id)
SELECT 'ROLE_OVERSEAS_PLATFORM_ADMIN', m.id
FROM (SELECT 130401 id) m
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu x WHERE x.role_code = 'ROLE_OVERSEAS_PLATFORM_ADMIN' AND x.menu_id = m.id
);
