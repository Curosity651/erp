ALTER TABLE wms_location_transfer_item
    ADD COLUMN move_mode VARCHAR(20) NOT NULL DEFAULT 'PARTIAL'
        COMMENT 'PARTIAL or WHOLE_PALLET' AFTER source_quality,
    ADD COLUMN source_pallet_id BIGINT NULL
        COMMENT 'source pallet id snapshot' AFTER move_mode,
    ADD COLUMN source_pallet_no VARCHAR(64) NULL
        COMMENT 'source pallet number snapshot' AFTER source_pallet_id,
    ADD KEY idx_lti_source_pallet (source_pallet_id);

UPDATE wms_location_transfer_item item
LEFT JOIN wms_physical_inventory inventory ON inventory.id = item.physical_inventory_id
LEFT JOIN wms_pallet pallet ON pallet.id = inventory.pallet_id
SET item.source_pallet_id = inventory.pallet_id,
    item.source_pallet_no = pallet.pallet_no
WHERE item.source_pallet_id IS NULL;
