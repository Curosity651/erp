-- =====================================================================
-- D2 重构 · 去中转层：入库单「同一张表 + 状态驱动」流转 + 货主隔离
--   决策修正(用户)：不要 OMS 中转层(outbox/dispatcher/独立下发单)；货主在采购入库单/自定义入库单页
--   建单→点「提交」→ 同一张 wms_purchase_inbound_order 流转给平台收货/上架，状态回到同一张单。
--   状态机：DRAFT 草稿 → SUBMITTED 已提交 → RECEIVED 已收货 → COMPLETED 已完成 (+ CANCELLED)。
--   货主隔离：入库单加 erp_tenant_id(货物归属货主)，货主只看自己、平台看全部。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V15__inbound_single_order_refactor.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- ---------- 1. 删除 OMS 中转层三表（不再使用）----------
DROP TABLE IF EXISTS oms_sync_outbox;
DROP TABLE IF EXISTS oms_inbound_order_item;
DROP TABLE IF EXISTS oms_inbound_order;

-- ---------- 2. wms_purchase_inbound_order：加货主归属，删无用的 OMS 回链列 ----------
ALTER TABLE wms_purchase_inbound_order
  ADD COLUMN erp_tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '货物归属(货主)租户ID' AFTER source_type,
  DROP COLUMN oms_inbound_order_id;

ALTER TABLE wms_purchase_inbound_order ADD KEY idx_erp_tenant (erp_tenant_id);

-- 存量入库单回填默认货主 1（DEFAULT 已保证，显式兜底）
UPDATE wms_purchase_inbound_order SET erp_tenant_id = 1 WHERE erp_tenant_id IS NULL OR erp_tenant_id = 0;

-- ---------- 3. 菜单：162001 指向真实「自定义入库单」页；移除 OMS 权限按钮 ----------
UPDATE sys_menu
   SET title = '自定义入库单', path = 'manual-inbound', uri = 'wms/manual-inbound/ManualInboundPage'
 WHERE id = 162001;

DELETE FROM sys_role_menu WHERE menu_id IN (162005, 162006, 162007);
DELETE FROM sys_menu WHERE id IN (162005, 162006, 162007);
