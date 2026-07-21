-- =====================================================================
-- B3 · 开通管理菜单
-- 平台管理(顶级,仅超管前端可见) > WMS服务商；货主管理(挂仓储管理下)。
-- 均授权 ROLE_ADMIN；前端按身份裁剪顶级菜单(platform 仅超管、my-clients 在 wms 下)。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V6__provision_menus.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- 1) 平台管理（顶级目录）
INSERT INTO sys_menu (id, parent_id, title, icon, path, target_type, uri, sort, keep_alive, hidden, type)
VALUES (170000, 0, '平台管理', 'cluster', 'platform', 1, '', 5, 0, 0, 0);

-- 2) WMS 服务商（平台管理子菜单）
INSERT INTO sys_menu (id, parent_id, title, icon, path, target_type, uri, sort, keep_alive, hidden, type)
VALUES (170100, 170000, 'WMS服务商', 'apartment', 'wms-operators', 1, 'platform/wms-operators', 1, 0, 0, 1);

-- 3) 货主管理（挂在 仓储管理 160000 下）
INSERT INTO sys_menu (id, parent_id, title, icon, path, target_type, uri, sort, keep_alive, hidden, type)
VALUES (161700, 160000, '货主管理', 'team', 'my-clients', 1, 'wms/my-clients', 90, 0, 0, 1);

-- 4) 授权给 ROLE_ADMIN（super_admin/wms_admin 均持有；前端按身份裁剪）
INSERT INTO sys_role_menu (role_code, menu_id) VALUES
  ('ROLE_ADMIN', 170000),
  ('ROLE_ADMIN', 170100),
  ('ROLE_ADMIN', 161700);

-- ===== 验证 =====
SELECT id, parent_id, title, path, type FROM sys_menu WHERE id IN (170000, 170100, 161700) ORDER BY id;
SELECT role_code, menu_id FROM sys_role_menu WHERE menu_id IN (170000, 170100, 161700);
