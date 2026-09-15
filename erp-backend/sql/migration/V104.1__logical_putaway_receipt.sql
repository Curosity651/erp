ALTER TABLE wms_putaway_receipt_line
    ADD COLUMN location_id BIGINT NULL COMMENT '逻辑库位ID' AFTER inbound_order_id,
    ADD COLUMN location_code VARCHAR(64) NULL COMMENT '逻辑库位编码快照' AFTER location_id,
    ADD COLUMN override_reason VARCHAR(500) NULL COMMENT '人工覆盖推荐原因' AFTER quantity,
    ADD KEY idx_putaway_receipt_location (location_id),
    ADD UNIQUE KEY uk_putaway_receipt_location_sku (
        inbound_order_id, location_id, sku_code, quality
    );
