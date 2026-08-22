ALTER TABLE wms_location_transfer_item
    ADD COLUMN source_slot_id BIGINT NULL AFTER source_pallet_no,
    ADD COLUMN source_slot_code VARCHAR(64) NULL AFTER source_slot_id,
    ADD COLUMN target_slot_id BIGINT NULL AFTER target_location_code,
    ADD COLUMN target_slot_code VARCHAR(64) NULL AFTER target_slot_id,
    ADD COLUMN target_pallet_id BIGINT NULL AFTER target_slot_code,
    ADD COLUMN target_pallet_no VARCHAR(80) NULL AFTER target_pallet_id,
    ADD KEY idx_lti_source_slot (source_slot_id),
    ADD KEY idx_lti_target_slot (target_slot_id),
    ADD KEY idx_lti_target_pallet (target_pallet_id);

UPDATE wms_location_transfer_item item
LEFT JOIN wms_physical_inventory inventory ON inventory.id = item.physical_inventory_id
LEFT JOIN wms_location_slot source_slot ON source_slot.id = inventory.slot_id
SET item.source_slot_id = inventory.slot_id,
    item.source_slot_code = source_slot.slot_code
WHERE item.source_slot_id IS NULL;
