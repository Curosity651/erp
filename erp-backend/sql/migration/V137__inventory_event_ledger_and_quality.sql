-- Logical-location inventory is the only warehouse stock source of truth.

UPDATE wms_location_inventory
SET quality = 'DEFECTIVE'
WHERE UPPER(quality) = 'DAMAGED';

ALTER TABLE wms_location_inventory
    MODIFY COLUMN quality VARCHAR(32) NOT NULL DEFAULT 'GOOD' COMMENT 'GOOD or DEFECTIVE',
    ADD CONSTRAINT chk_location_inventory_quality CHECK (quality IN ('GOOD', 'DEFECTIVE')),
    ADD KEY idx_location_inventory_owner_warehouse_quality_sku
        (erp_tenant_id, warehouse_id, quality, sku_code, deleted),
    ADD KEY idx_location_inventory_owner_sku_quality
        (erp_tenant_id, sku_code, quality, deleted);

CREATE TABLE IF NOT EXISTS wms_inventory_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_no VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL,
    wms_tenant_id BIGINT NOT NULL,
    erp_tenant_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) DEFAULT NULL,
    source_id BIGINT DEFAULT NULL,
    source_no VARCHAR(128) DEFAULT NULL,
    operator_id BIGINT DEFAULT NULL,
    operator_name VARCHAR(128) DEFAULT NULL,
    reason VARCHAR(500) DEFAULT NULL,
    idempotency_key VARCHAR(191) NOT NULL,
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_event_no (event_no),
    UNIQUE KEY uk_inventory_event_idempotency (tenant_id, idempotency_key),
    KEY idx_inventory_event_owner_time (erp_tenant_id, occurred_at, id),
    KEY idx_inventory_event_warehouse_time (warehouse_id, occurred_at, id),
    KEY idx_inventory_event_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='logical inventory mutation event';

CREATE TABLE IF NOT EXISTS wms_inventory_event_line (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    inventory_id BIGINT DEFAULT NULL,
    location_id BIGINT NOT NULL,
    counterpart_location_id BIGINT DEFAULT NULL,
    sku_code VARCHAR(128) NOT NULL,
    quality VARCHAR(32) NOT NULL,
    quantity_delta INT NOT NULL DEFAULT 0,
    reserved_delta INT NOT NULL DEFAULT 0,
    before_quantity INT NOT NULL,
    after_quantity INT NOT NULL,
    before_reserved INT NOT NULL,
    after_reserved INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_event_line_no (event_id, line_no),
    KEY idx_inventory_event_line_inventory (inventory_id, id),
    KEY idx_inventory_event_line_location (location_id, id),
    KEY idx_inventory_event_line_sku (sku_code, id),
    CONSTRAINT fk_inventory_event_line_event FOREIGN KEY (event_id) REFERENCES wms_inventory_event (id),
    CONSTRAINT chk_inventory_event_line_quality CHECK (quality IN ('GOOD', 'DEFECTIVE')),
    CONSTRAINT chk_inventory_event_line_after_quantity CHECK (after_quantity >= 0),
    CONSTRAINT chk_inventory_event_line_after_reserved
        CHECK (after_reserved >= 0 AND after_reserved <= after_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='logical inventory mutation event line';
