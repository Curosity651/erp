-- =====================================================================
-- 自定义出库单（Custom Outbound）迁移脚本
--   1. wms_sales_outbound_order 扩展来源/类型/收货人字段：
--      与销售出库单共表（对齐「自定义入库单复用采购入库表」先例），source_type 区分 SALES/CUSTOM；
--      CUSTOM 单 platform 为 NULL（不挂平台订单），故 platform 放开为可空；
--      提交（校验可售+预占）后与销售出库单同链路流转至海外仓平台作业台（下架→打包→签出），仓库端零改动。
--   2. 菜单：162002「自定义出库单」占位页指向真实页面，并补 read/add/edit/del 权限按钮，授予 ROLE_ADMIN(货主管理员)。
--      平台超管(super_admin, admin)走权限旁路，无需显式授权。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/custom_outbound_migration.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- ---------- 1. 表结构扩展 ----------

ALTER TABLE wms_sales_outbound_order
    ADD COLUMN source_type      VARCHAR(20)  NOT NULL DEFAULT 'SALES' COMMENT '来源类型：SALES销售出库/CUSTOM自定义出库' AFTER outbound_no,
    ADD COLUMN custom_type      VARCHAR(32)  NULL COMMENT '自定义出库类型：OFFLINE_ORDER线下订单/SAMPLE_SEND样品寄送/SCRAP销毁报废/RETURN_TO_SUPPLIER退供应商/OTHER其他（仅CUSTOM）' AFTER source_type,
    ADD COLUMN ref_no           VARCHAR(100) NULL COMMENT '关联单号文本（线下订单号/退供单号等，仅CUSTOM，可空）' AFTER custom_type,
    ADD COLUMN receiver_name    VARCHAR(100) NULL COMMENT '收件人姓名（仅CUSTOM，可空，销毁类无收货人）' AFTER ref_no,
    ADD COLUMN receiver_phone   VARCHAR(50)  NULL COMMENT '收件人电话（仅CUSTOM，可空）' AFTER receiver_name,
    ADD COLUMN receiver_address VARCHAR(500) NULL COMMENT '收货地址（仅CUSTOM，可空）' AFTER receiver_phone,
    ADD KEY idx_source_type (source_type);

-- 自定义出库单不挂平台订单，platform 放开为可空（仅放宽，不缩窄，存量数据不受影响）
ALTER TABLE wms_sales_outbound_order
    MODIFY COLUMN platform VARCHAR(50) NULL COMMENT '平台：wildberries/ozon/yandex（自定义出库为NULL）';

-- 存量单据显式回填为销售出库（DEFAULT 已保证，显式兜底）
UPDATE wms_sales_outbound_order SET source_type = 'SALES' WHERE source_type IS NULL OR source_type = '';

-- ---------- 2. 菜单：162002 指向真实「自定义出库单」页 ----------

UPDATE sys_menu
   SET title = '自定义出库单', path = 'custom-outbound', uri = 'wms/custom-outbound/CustomOutboundPage'
 WHERE id = 162002;

-- ---------- 3. 权限按钮（挂在 162002 下，ID 段避开自定义入库单 162005~162008） ----------

INSERT INTO sys_menu (id, parent_id, title, permission, path, target_type, uri, sort, keep_alive, hidden, type, deleted)
VALUES
  (162020, 162002, '自定义出库单查看', 'wms:custom-outbound:read', NULL, 1, '', 1, 1, 0, 2, 0),
  (162021, 162002, '自定义出库单新增', 'wms:custom-outbound:add',  NULL, 1, '', 2, 1, 0, 2, 0),
  (162022, 162002, '自定义出库单编辑', 'wms:custom-outbound:edit', NULL, 1, '', 3, 1, 0, 2, 0),
  (162023, 162002, '自定义出库单删除', 'wms:custom-outbound:del',  NULL, 1, '', 4, 1, 0, 2, 0);

INSERT INTO sys_role_menu (role_code, menu_id)
VALUES
  ('ROLE_ADMIN', 162020),
  ('ROLE_ADMIN', 162021),
  ('ROLE_ADMIN', 162022),
  ('ROLE_ADMIN', 162023);
