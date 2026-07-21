-- =====================================================================
-- D1 · 库存方案②：批次级 SSOT + 现有库存降级聚合 + 流水/库存加双ID
--   只覆盖 OWN 自有仓；FBO 同步路径(FboStockSyncController)不动。
--   台账/库存/流水表【不加 deleted】；审计 create_by/create_time/update_by/update_time。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V12__physical_inventory.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- ---------- 1. 新建批次级 SSOT ----------
CREATE TABLE IF NOT EXISTS wms_physical_inventory (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  wms_tenant_id   BIGINT       NOT NULL DEFAULT 0  COMMENT '货架归属(WMS服务商)，无则0',
  erp_tenant_id   BIGINT       NOT NULL            COMMENT '货物归属(货主)',
  warehouse_id    BIGINT       NOT NULL            COMMENT '仓库(自有仓)',
  sku_code        VARCHAR(100) NOT NULL            COMMENT 'SKU编码',
  inbound_item_id BIGINT       NOT NULL DEFAULT 0  COMMENT '入库明细ID(批次溯源)',
  inbound_date    DATE         NOT NULL            COMMENT 'FIFO主键-实际上架日',
  pick_order      INT          NOT NULL DEFAULT 0  COMMENT '同日顺序(FIFO次序)',
  quantity        INT          NOT NULL DEFAULT 0  COMMENT '批次数量',
  reserved_qty    INT          NOT NULL DEFAULT 0  COMMENT '锁定(占用)数',
  quality         VARCHAR(20)  NOT NULL DEFAULT 'GOOD' COMMENT 'GOOD良品/DAMAGED次品',
  location_code   VARCHAR(50)  NOT NULL            COMMENT '库位编码(发生地)',
  zone_id         BIGINT       DEFAULT NULL        COMMENT '分区ID',
  allocatable     TINYINT      NOT NULL DEFAULT 1  COMMENT '冗余:可分配(标准/退货区=1,不良品/暂存=0)',
  version         INT          NOT NULL DEFAULT 0  COMMENT '乐观锁版本',
  create_by       BIGINT       DEFAULT NULL,
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by       BIGINT       DEFAULT NULL,
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_batch (wms_tenant_id, erp_tenant_id, warehouse_id, sku_code, inbound_item_id, location_code),
  KEY idx_fifo_alloc (wms_tenant_id, erp_tenant_id, warehouse_id, sku_code, quality, allocatable, inbound_date, pick_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批次级库存SSOT(方案②·自有仓)';

-- ---------- 2. wms_inventory 降级聚合 + 双ID ----------
ALTER TABLE wms_inventory
  ADD COLUMN wms_tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '货架归属(WMS服务商)，无则0' AFTER id,
  ADD COLUMN erp_tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '货物归属(货主)' AFTER wms_tenant_id;
-- 回填：现有库存(多为FBO)归默认货主 tenant1、无服务商(0)
UPDATE wms_inventory SET erp_tenant_id = 1 WHERE erp_tenant_id = 0;
-- 唯一键改为含租户：原 uk_warehouse_sku(warehouse_id,sku_code) → 加双ID
ALTER TABLE wms_inventory DROP INDEX uk_warehouse_sku;
ALTER TABLE wms_inventory
  ADD UNIQUE KEY uk_tenant_wh_sku (wms_tenant_id, erp_tenant_id, warehouse_id, sku_code);

-- ---------- 3. wms_stock_flow 双ID（决策S作废，不新建流水表） ----------
ALTER TABLE wms_stock_flow
  ADD COLUMN wms_tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '货架归属(WMS服务商)，无则0' AFTER warehouse_id,
  ADD COLUMN erp_tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '货物归属(货主)' AFTER wms_tenant_id;
UPDATE wms_stock_flow SET erp_tenant_id = 1 WHERE erp_tenant_id = 0;
ALTER TABLE wms_stock_flow ADD KEY idx_erp_tenant (erp_tenant_id, warehouse_id, sku_code);

-- ===== 验证 =====
SELECT COUNT(*) phys_tbl FROM information_schema.tables WHERE table_schema='erp' AND table_name='wms_physical_inventory';
SELECT COUNT(*) inv_with_tenant FROM wms_inventory WHERE erp_tenant_id = 1;
SHOW INDEX FROM wms_inventory WHERE Key_name = 'uk_tenant_wh_sku';
