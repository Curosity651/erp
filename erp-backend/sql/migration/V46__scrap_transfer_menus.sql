-- =====================================================================
-- 报废单 / 库位调整 / 待确认报废 菜单与权限。
--   平台(ops 170500)：报废单(160900 改名) + 库位调整(160910 新)；
--   货主(wms 160000)：待确认报废(160920 新，wms:scrap:confirm)。
--   授权：平台 ROLE_OVERSEAS_PLATFORM_ADMIN 得库位调整 + 批次查看(wms:inventory:read)；
--        货主 ROLE_ADMIN_T%(ERP_USER) 得待确认报废 + 报废单查看(wms:adjustment:read)。
-- 幂等：NOT EXISTS 去重，可重复执行。
-- =====================================================================

-- 报废单改名（原「库存调整」）
UPDATE sys_menu SET title = '报废单' WHERE id = 160900;

-- 平台：库位调整 页面 + 权限
INSERT INTO sys_menu (id, parent_id, title, permission, path, uri, type, sort, target_type, keep_alive, hidden, deleted)
SELECT 160910, 170500, '库位调整', NULL, 'location-transfer', 'wms/location-transfer/LocationTransferPage', 1, 20, 1, 0, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 160910);

INSERT INTO sys_menu (id, parent_id, title, permission, path, uri, type, sort, target_type, keep_alive, hidden, deleted)
SELECT 160911, 160910, '移库', 'wms:location:transfer', NULL, NULL, 2, 1, 1, 0, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 160911);

-- 货主：待确认报废 页面 + 权限
INSERT INTO sys_menu (id, parent_id, title, permission, path, uri, type, sort, target_type, keep_alive, hidden, deleted)
SELECT 160920, 160000, '待确认报废', NULL, 'scrap-confirm', 'wms/scrap-confirm/ScrapConfirmPage', 1, 95, 1, 0, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 160920);

INSERT INTO sys_menu (id, parent_id, title, permission, path, uri, type, sort, target_type, keep_alive, hidden, deleted)
SELECT 160921, 160920, '确认/驳回', 'wms:scrap:confirm', NULL, NULL, 2, 1, 1, 0, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 160921);

-- 授权：平台角色 → 库位调整 + 批次查看
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT 'ROLE_OVERSEAS_PLATFORM_ADMIN', m.id
FROM (SELECT 160910 id UNION SELECT 160911 UNION SELECT 161002) m
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu x WHERE x.role_code = 'ROLE_OVERSEAS_PLATFORM_ADMIN' AND x.menu_id = m.id
);

-- 授权：货主管理员角色 → 待确认报废 + 报废单查看
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT r.code, m.id
FROM sys_role r
JOIN sys_tenant t ON t.id = r.tenant_id AND t.tenant_type = 'ERP_USER'
CROSS JOIN (SELECT 160920 id UNION SELECT 160921 UNION SELECT 160901) m
WHERE r.code LIKE 'ROLE_ADMIN_T%'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu x WHERE x.role_code = r.code AND x.menu_id = m.id
  );
