-- =====================================================================
-- 页面分配补充：平台超管 / WMS服务商 各自独立的「数据分析」看板
--   现有 数据分析(statistics 900000) 与 系统管理(system 100000) 属货主(ERP+OMS)。
--   平台超管、WMS服务商：
--     · 系统管理 仅保留 菜单权限(100800)/角色管理(100200)/配置信息(100400)（前端裁剪，无需建表）
--     · 数据分析 各给一个独立看板（本期占位）
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V11__platform_wms_dashboard.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

INSERT INTO sys_menu (id, parent_id, title, icon, path, target_type, uri, sort, keep_alive, hidden, type) VALUES
  (900300, 900000, '平台数据分析', 'dashboard', 'platform-dashboard', 1, 'common/Placeholder', 3, 0, 0, 1),
  (900400, 900000, '运营数据分析', 'dashboard', 'ops-dashboard',      1, 'common/Placeholder', 4, 0, 0, 1);

INSERT INTO sys_role_menu (role_code, menu_id) VALUES
  ('ROLE_ADMIN', 900300),
  ('ROLE_ADMIN', 900400);

-- ===== 验证 =====
SELECT id, parent_id, title, path, REPLACE(uri,'\n','') uri FROM sys_menu
 WHERE parent_id = 900000 AND deleted = 0 ORDER BY sort;
