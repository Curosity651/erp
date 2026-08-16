ALTER TABLE wms_location_transfer_item
    ADD COLUMN source_inventory_id BIGINT NULL COMMENT '源逻辑库位库存ID' AFTER physical_inventory_id,
    ADD COLUMN target_location_id BIGINT NULL COMMENT '目标逻辑库位ID' AFTER target_location_code,
    ADD INDEX idx_transfer_source_inventory (source_inventory_id),
    ADD INDEX idx_transfer_target_location (target_location_id);
