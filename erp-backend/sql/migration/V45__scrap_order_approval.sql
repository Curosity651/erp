-- =====================================================================
-- 报废单审批流改造：原「库存调整单」收敛为仅「报废」，并改为货主审批流。
--   表头补货主归属 + 货主确认/驳回字段；明细补批次锁定字段。
--   状态机：PENDING_OWNER(待货主确认) → SCRAPPED(已销毁) / REJECTED(已驳回) / CANCELLED(已取消)。
--   菜单/权限(货主端「待确认报废」wms:scrap:confirm)随前端页面一并迁移，另见后续脚本。
-- 注：MySQL 8 不支持 ADD COLUMN IF NOT EXISTS；本脚本仅执行一次（列尚不存在）。
-- =====================================================================

ALTER TABLE wms_adjustment_order
    ADD COLUMN erp_tenant_id BIGINT NULL COMMENT '货主（货物归属），报废需通知该货主确认' AFTER warehouse_id,
    ADD COLUMN owner_action_time DATETIME NULL COMMENT '货主确认/驳回时间' AFTER confirm_by,
    ADD COLUMN owner_action_by BIGINT NULL COMMENT '货主确认/驳回操作人' AFTER owner_action_time,
    ADD COLUMN reject_reason VARCHAR(500) NULL COMMENT '货主驳回原因' AFTER owner_action_by;

-- 新单据默认进入「待货主确认」（旧默认 DRAFT 已废弃）
ALTER TABLE wms_adjustment_order
    ALTER COLUMN order_status SET DEFAULT 'PENDING_OWNER';

ALTER TABLE wms_adjustment_order_item
    ADD COLUMN physical_inventory_id BIGINT NULL COMMENT '目标批次ID(wms_physical_inventory.id)' AFTER sku_code,
    ADD COLUMN location_code VARCHAR(50) NULL COMMENT '库位编码(冗余展示，取自批次)' AFTER physical_inventory_id,
    ADD COLUMN quality VARCHAR(20) NULL COMMENT '品质 GOOD/DAMAGED(冗余展示，取自批次)' AFTER location_code;
