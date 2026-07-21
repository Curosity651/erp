-- =====================================================================
-- C1 · 库位管理菜单（挂在 仓储管理 160000 下）
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V8__location_mgmt_menu.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

INSERT INTO sys_menu (id, parent_id, title, icon, path, target_type, uri, sort, keep_alive, hidden, type)
VALUES (161800, 160000, '库位管理', 'appstore', 'location-mgmt', 1, 'wms/location-mgmt', 95, 0, 0, 1);

INSERT INTO sys_role_menu (role_code, menu_id) VALUES ('ROLE_ADMIN', 161800);

-- ===== 验证 =====
SELECT id, parent_id, title, path, uri FROM sys_menu WHERE id = 161800;
