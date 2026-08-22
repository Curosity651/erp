ALTER TABLE wms_stocktake_order_item
    ADD COLUMN source_inventory_id BIGINT NULL COMMENT '逻辑库位库存ID' AFTER physical_inventory_id,
    ADD INDEX idx_stocktake_item_source_inventory (source_inventory_id);

ALTER TABLE wms_adjustment_order_item
    ADD COLUMN source_inventory_id BIGINT NULL COMMENT '逻辑库位库存ID' AFTER physical_inventory_id,
    ADD INDEX idx_adjustment_item_source_inventory (source_inventory_id);

ALTER TABLE wms_return_qc_item
    ADD COLUMN qualified_location_id BIGINT NULL COMMENT '良品目标逻辑库位ID' AFTER qualified_location_code,
    ADD COLUMN damaged_location_id BIGINT NULL COMMENT '不良品目标逻辑库位ID' AFTER damaged_location_code,
    ADD INDEX idx_return_qc_qualified_location (qualified_location_id),
    ADD INDEX idx_return_qc_damaged_location (damaged_location_id);
