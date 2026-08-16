-- Logical locations and batch-free location inventory core.
-- Legacy pallet, slot and physical inventory tables remain read-only during cutover.

ALTER TABLE wms_location
    ADD COLUMN length_mm INT NULL COMMENT 'logical location inner length in millimetres' AFTER max_weight_kg,
    ADD COLUMN width_mm INT NULL COMMENT 'logical location inner width in millimetres' AFTER length_mm,
    ADD COLUMN height_mm INT NULL COMMENT 'logical location inner height in millimetres' AFTER width_mm,
    ADD COLUMN max_sku_kinds INT NOT NULL DEFAULT 0 COMMENT '0 means unlimited SKU kinds' AFTER height_mm,
    ADD COLUMN public_shared TINYINT NOT NULL DEFAULT 0 COMMENT '1 allows all WMS providers to use this location' AFTER max_sku_kinds;

CREATE TABLE wms_location_inventory (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'location inventory id',
    tenant_id BIGINT NOT NULL COMMENT 'platform tenant id',
    wms_tenant_id BIGINT NOT NULL COMMENT 'WMS provider id',
    erp_tenant_id BIGINT NOT NULL COMMENT 'goods owner id',
    warehouse_id BIGINT NOT NULL COMMENT 'internal warehouse id',
    location_id BIGINT NOT NULL COMMENT 'logical location id',
    sku_code VARCHAR(128) NOT NULL COMMENT 'ERP SKU',
    quality VARCHAR(32) NOT NULL DEFAULT 'GOOD' COMMENT 'GOOD or DAMAGED',
    quantity INT NOT NULL DEFAULT 0 COMMENT 'physical quantity at this location',
    reserved_quantity INT NOT NULL DEFAULT 0 COMMENT 'quantity reserved by fulfillment orders',
    version INT NOT NULL DEFAULT 0 COMMENT 'optimistic lock version',
    create_by BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0 COMMENT 'logical deletion marker',
    PRIMARY KEY (id),
    UNIQUE KEY uk_location_inventory_owner_sku_quality
        (tenant_id, wms_tenant_id, erp_tenant_id, warehouse_id, location_id, sku_code, quality, deleted),
    KEY idx_location_inventory_location (warehouse_id, location_id, deleted),
    KEY idx_location_inventory_available (warehouse_id, erp_tenant_id, sku_code, quality, deleted),
    CONSTRAINT chk_location_inventory_quantities
        CHECK (quantity >= 0 AND reserved_quantity >= 0 AND reserved_quantity <= quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='batch-free inventory by logical location';
