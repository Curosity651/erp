-- =====================================================================
-- 自定义退货单（CUSTOM_RETURN）
--   自定义退货单 = 不挂靠平台订单的退货入库单（平台批量退货混包/无单退件/样品收回/发错召回），
--   与采购/自定义入库单共用 wms_purchase_inbound_order 表（source_type=CUSTOM_RETURN），
--   建单→提交后同样流转给平台收货/上架。
--   ① 主表加 return_type(退货类型) / ref_no(关联单号, 纯文本参考)；明细表加 expected_quality(货品预判)；
--   ② 菜单：占位 162004「自定义退货单」升级为真实页面（对齐 V15 对 162001 的做法）；
--   ③ 权限：read/add/edit/del 四个按钮（162009~162012），授予 ROLE_ADMIN(货主管理员)。
--      平台超管(super_admin, admin)走权限旁路，无需显式授权。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V34__custom_return.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- ---------- 1. wms_purchase_inbound_order：加退货类型/关联单号 ----------
ALTER TABLE wms_purchase_inbound_order
  ADD COLUMN return_type VARCHAR(32) NULL COMMENT '退货类型(仅CUSTOM_RETURN): PLATFORM_BATCH平台批量退货/NO_ORDER无单退件/SAMPLE_BACK样品收回/WRONG_SHIPMENT发错召回/OTHER其他' AFTER source_type,
  ADD COLUMN ref_no VARCHAR(100) NULL COMMENT '关联单号(仅CUSTOM_RETURN, 纯文本参考: 平台订单号/物流追踪号等)' AFTER return_type;

-- ---------- 2. wms_purchase_inbound_order_item：加货品预判 ----------
ALTER TABLE wms_purchase_inbound_order_item
  ADD COLUMN expected_quality VARCHAR(16) NULL COMMENT '货品预判(仅CUSTOM_RETURN): GOOD良品/PENDING待检/DAMAGED不良品' AFTER actual_quantity;

-- ---------- 3. 菜单：162004 指向真实「自定义退货单」页 ----------
UPDATE sys_menu
   SET title = '自定义退货单', path = 'custom-return', uri = 'wms/custom-return/CustomReturnPage'
 WHERE id = 162004;

-- ---------- 4. 权限按钮：read/add/edit/del，挂在 162004 下 ----------
INSERT INTO sys_menu (id, parent_id, title, permission, path, target_type, uri, sort, keep_alive, hidden, type, deleted)
VALUES
  (162009, 162004, '自定义退货单查看', 'wms:custom-return:read', NULL, 1, '', 1, 1, 0, 2, 0),
  (162010, 162004, '自定义退货单新增', 'wms:custom-return:add',  NULL, 1, '', 2, 1, 0, 2, 0),
  (162011, 162004, '自定义退货单编辑', 'wms:custom-return:edit', NULL, 1, '', 3, 1, 0, 2, 0),
  (162012, 162004, '自定义退货单删除', 'wms:custom-return:del',  NULL, 1, '', 4, 1, 0, 2, 0);

INSERT INTO sys_role_menu (role_code, menu_id)
VALUES
  ('ROLE_ADMIN', 162009),
  ('ROLE_ADMIN', 162010),
  ('ROLE_ADMIN', 162011),
  ('ROLE_ADMIN', 162012);
