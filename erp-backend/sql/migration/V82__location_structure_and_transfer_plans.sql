-- Location structure safety and unified location-transfer planning.

ALTER TABLE wms_location_transfer_order
    ADD COLUMN wms_tenant_id BIGINT NULL COMMENT 'WMS operator snapshot' AFTER warehouse_id,
    ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL/SALES_OUTBOUND' AFTER order_status,
    ADD COLUMN source_id BIGINT NULL COMMENT 'source business id' AFTER source_type,
    ADD COLUMN source_no VARCHAR(64) NULL COMMENT 'source business number' AFTER source_id,
    ADD COLUMN source_key VARCHAR(128) NULL COMMENT 'idempotency key for generated plans' AFTER source_no,
    ADD COLUMN reason_code VARCHAR(32) NULL COMMENT 'location transfer reason code' AFTER source_key,
    ADD COLUMN reason VARCHAR(255) NULL COMMENT 'location transfer reason' AFTER reason_code,
    ADD UNIQUE KEY uk_lt_source_key (source_key),
    ADD KEY idx_lt_wms_tenant (wms_tenant_id),
    ADD KEY idx_lt_source (source_type, source_id);

ALTER TABLE wms_location_transfer_order
    MODIFY COLUMN order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PLANNED/PENDING/COMPLETED/CANCELLED';

UPDATE wms_warehouse
SET rack_rows = 16, rack_columns = 20
WHERE warehouse_code = 'KDEF'
  AND (SELECT COUNT(DISTINCT rack_no)
       FROM wms_location
       WHERE warehouse_id = wms_warehouse.id AND is_virtual = 0) = 16;

ALTER TABLE wms_zone
    ADD UNIQUE KEY uk_zone_warehouse_type (warehouse_id, zone_type);
