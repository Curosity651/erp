-- =====================================================================
-- D2 重构 · 自定义入库单页权限按钮
--   162001「自定义入库单」(wms/manual-inbound/ManualInboundPage) 原本无权限子节点，
--   补齐 read/add/edit/del 四个权限，挂在 162001 下，并授予 ROLE_ADMIN(货主管理员)。
--   平台超管(super_admin, admin)走权限旁路，无需显式授权。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V16__manual_inbound_perms.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

INSERT INTO sys_menu (id, parent_id, title, permission, path, target_type, uri, sort, keep_alive, hidden, type, deleted)
VALUES
  (162005, 162001, '自定义入库单查看', 'wms:manual-inbound:read', NULL, 1, '', 1, 1, 0, 2, 0),
  (162006, 162001, '自定义入库单新增', 'wms:manual-inbound:add',  NULL, 1, '', 2, 1, 0, 2, 0),
  (162007, 162001, '自定义入库单编辑', 'wms:manual-inbound:edit', NULL, 1, '', 3, 1, 0, 2, 0),
  (162008, 162001, '自定义入库单删除', 'wms:manual-inbound:del',  NULL, 1, '', 4, 1, 0, 2, 0);

INSERT INTO sys_role_menu (role_code, menu_id)
VALUES
  ('ROLE_ADMIN', 162005),
  ('ROLE_ADMIN', 162006),
  ('ROLE_ADMIN', 162007),
  ('ROLE_ADMIN', 162008);
