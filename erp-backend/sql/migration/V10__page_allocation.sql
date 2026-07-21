-- =====================================================================
-- 页面分配（按三身份重组菜单 + 占位页）
--   货主 ERP_USER：商品/订单/财务(WB)/我的仓储(OMS单据+库存只读)/系统/日志
--   WMS服务商 WMS_OPERATOR（纯中间人）：服务商运营(开货主+物流产品+收支+货架库存只读)/系统
--   平台超管 SUPER_ADMIN：平台管理(开服务商/货架分配/仓库建模/海外仓作业/平台财务)/系统/日志
-- 物理作业(收货/上架/下架/打包/质检)归平台超管；WMS服务商不碰货。
-- 裁剪在前端 menu-filter.ts 按身份做；后端统一授 ROLE_ADMIN。
-- 重挂仅限“无路由耦合”的页面(仓库/库位/区域→平台；货主管理→服务商运营)；
-- 盘点/调拨/库存调整因被库存记录深链引用，保留在 wms 下，超管单独放行、货主隐藏。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V10__page_allocation.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- ---------- 1. 新建分组目录（type=0） ----------
INSERT INTO sys_menu (id, parent_id, title, icon, path, target_type, uri, sort, keep_alive, hidden, type) VALUES
  (170400, 170000, '仓库建模',   'home',         'warehouse-mgmt',  1, NULL, 10, 0, 0, 0),
  (170500, 170000, '海外仓作业', 'inbox',        'ops',             1, NULL, 11, 0, 0, 0),
  (170600, 170000, '平台财务',   'account-book', 'platform-finance',1, NULL, 12, 0, 0, 0),
  (180000, 0,      '服务商运营', 'shop',         'wms-console',     1, NULL,  6, 0, 0, 0);

-- ---------- 2. 重挂无耦合页面 ----------
-- 仓库管理/库位管理/区域管理 → 平台·仓库建模
UPDATE sys_menu SET parent_id = 170400, sort = 1 WHERE id = 160100; -- 仓库管理
UPDATE sys_menu SET parent_id = 170400, sort = 2 WHERE id = 161800; -- 库位管理
UPDATE sys_menu SET parent_id = 170400, sort = 3 WHERE id = 161600; -- 区域管理
-- 货主管理 → 服务商运营
UPDATE sys_menu SET parent_id = 180000, sort = 1 WHERE id = 161700; -- 货主管理

-- ---------- 3. 占位叶子（type=1，共用占位组件 common/Placeholder） ----------
INSERT INTO sys_menu (id, parent_id, title, icon, path, target_type, uri, sort, keep_alive, hidden, type) VALUES
  -- 平台超管 · 海外仓作业
  (170501, 170500, '收货',       'scan',     'receiving',       1, 'common/Placeholder', 1, 0, 0, 1),
  (170502, 170500, '上架',       'upload',   'putaway',         1, 'common/Placeholder', 2, 0, 0, 1),
  (170503, 170500, '下架(拣货)', 'download', 'picking',         1, 'common/Placeholder', 3, 0, 0, 1),
  (170504, 170500, '打包签出',   'gift',     'packing',         1, 'common/Placeholder', 4, 0, 0, 1),
  (170505, 170500, '退货质检',   'safety',   'return-qc',       1, 'common/Placeholder', 5, 0, 0, 1),
  -- 平台超管 · 平台财务（链路一应收）
  (170601, 170600, '应收账单',   'pay-circle','receivable-bill',1, 'common/Placeholder', 1, 0, 0, 1),
  -- WMS服务商 · 服务商运营
  (180100, 180000, '物流产品管理','appstore-add','logistics-product',1, 'common/Placeholder', 2, 0, 0, 1),
  (180200, 180000, '财务-收入',  'rise',     'income',          1, 'common/Placeholder', 3, 0, 0, 1),
  (180300, 180000, '财务-支出',  'fall',     'expense',         1, 'common/Placeholder', 4, 0, 0, 1),
  (180400, 180000, '货架库存(只读)','database','rack-inventory', 1, 'common/Placeholder', 5, 0, 0, 1),
  -- 货主 · 我的仓储（OMS 单据）
  (162001, 160000, '自定义入库单','file-add', 'manual-inbound',  1, 'common/Placeholder', 41, 0, 0, 1),
  (162002, 160000, '自定义出库单','file-done','manual-outbound', 1, 'common/Placeholder', 42, 0, 0, 1),
  (162003, 160000, '退货订单',    'rollback', 'return-order',    1, 'common/Placeholder', 43, 0, 0, 1),
  (162004, 160000, '自定义退货单','file-sync','manual-return',   1, 'common/Placeholder', 44, 0, 0, 1);

-- ---------- 4. 新菜单统一授权 ROLE_ADMIN（前端按身份再裁剪） ----------
INSERT INTO sys_role_menu (role_code, menu_id) VALUES
  ('ROLE_ADMIN', 170400),
  ('ROLE_ADMIN', 170500),
  ('ROLE_ADMIN', 170600),
  ('ROLE_ADMIN', 180000),
  ('ROLE_ADMIN', 170501),
  ('ROLE_ADMIN', 170502),
  ('ROLE_ADMIN', 170503),
  ('ROLE_ADMIN', 170504),
  ('ROLE_ADMIN', 170505),
  ('ROLE_ADMIN', 170601),
  ('ROLE_ADMIN', 180100),
  ('ROLE_ADMIN', 180200),
  ('ROLE_ADMIN', 180300),
  ('ROLE_ADMIN', 180400),
  ('ROLE_ADMIN', 162001),
  ('ROLE_ADMIN', 162002),
  ('ROLE_ADMIN', 162003),
  ('ROLE_ADMIN', 162004);

-- ===== 验证 =====
SELECT id, parent_id, title, path, REPLACE(uri,'\n','') uri, type FROM sys_menu
 WHERE deleted=0 AND (parent_id IN (170000,170400,170500,170600,180000,160000) OR id IN (170400,170500,170600,180000))
 ORDER BY parent_id, sort;
