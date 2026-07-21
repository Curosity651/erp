-- =====================================================================
-- D2.5 · 入库链路 菜单 + 权限
--   ① 货主：把占位「自定义入库单」(162001) 升级为真实 OMS 入库下发页，支持 采购/自定义 两条链；
--      新增按钮权限 oms:inbound:read/add/edit（否则货主调 /oms/inbound-order 接口 403）。
--   ② 平台超管：把占位「收货」(170501) 升级为「入库收货上架」作业页，调 /wms/inbound-execution；
--      合并「上架」(170502) 入同一页 → 隐藏 170502。收货/上架接口复用既有 wms:purchase-inbound:read/edit
--      （ROLE_ADMIN 已授，无需新增）。
--   前端按身份裁剪不变；后端统一授 ROLE_ADMIN。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V14__oms_inbound_menu.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- ---------- 1. 货主 OMS 入库下发页（升级 162001 占位）----------
UPDATE sys_menu
   SET title = '入库下发单', path = 'oms-inbound', uri = 'oms/inbound-order/OmsInboundPage'
 WHERE id = 162001;

-- 货主入库按钮权限（type=2）
INSERT INTO sys_menu (id, parent_id, title, path, target_type, uri, permission, sort, keep_alive, hidden, type) VALUES
  (162005, 162001, '入库下发查看', NULL, 1, NULL, 'oms:inbound:read', 1, 0, 0, 2),
  (162006, 162001, '入库下发新增', NULL, 1, NULL, 'oms:inbound:add',  2, 0, 0, 2),
  (162007, 162001, '入库下发取消', NULL, 1, NULL, 'oms:inbound:edit', 3, 0, 0, 2);

-- ---------- 2. 平台 入库收货上架 作业页（升级 170501 占位，合并 170502）----------
UPDATE sys_menu
   SET title = '入库收货上架', path = 'inbound-ops', uri = 'platform/inbound-ops/InboundOpsPage'
 WHERE id = 170501;

UPDATE sys_menu SET hidden = 1 WHERE id = 170502; -- 上架并入收货上架页

-- ---------- 3. 授权 ROLE_ADMIN（前端按身份再裁剪）----------
INSERT INTO sys_role_menu (role_code, menu_id) VALUES
  ('ROLE_ADMIN', 162005),
  ('ROLE_ADMIN', 162006),
  ('ROLE_ADMIN', 162007);
