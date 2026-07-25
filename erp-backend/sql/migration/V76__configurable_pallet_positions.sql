-- Configurable pallet levels and positions per level.
-- Existing slots become position P01; no inventory or pallet rows are deleted.

ALTER TABLE wms_warehouse
    ADD COLUMN pallet_positions_per_level int NOT NULL DEFAULT 1
        COMMENT 'pallet positions on each rack level' AFTER pallet_levels;

ALTER TABLE wms_location_slot
    ADD COLUMN position_no tinyint NOT NULL DEFAULT 1
        COMMENT 'pallet position within a level' AFTER level_no;

ALTER TABLE wms_location_slot
    DROP INDEX uk_location_level,
    ADD UNIQUE KEY uk_location_level_position (location_id, level_no, position_no);

-- Existing installations used one pallet position per level. Preserve those rows as P01.
UPDATE wms_location_slot
SET position_no = 1
WHERE position_no IS NULL OR position_no < 1;

UPDATE wms_location_slot
SET slot_code = CONCAT(slot_code, '-P01')
WHERE slot_code NOT REGEXP '-P[0-9]+$';

UPDATE wms_pallet p
JOIN wms_location_slot s ON s.id = p.current_slot_id
SET p.slot_code = s.slot_code
WHERE p.current_slot_id IS NOT NULL;

UPDATE wms_stocktake_order_item i
JOIN wms_location_slot s ON s.id = i.slot_id
SET i.slot_code = s.slot_code
WHERE i.slot_id IS NOT NULL;

-- Current physical warehouses use six levels and three pallet positions per level.
-- New warehouses keep the same defaults and remain configurable through the structure page.
UPDATE wms_warehouse
SET pallet_levels = 6,
    pallet_positions_per_level = 3,
    allow_cross_owner_mix = 0
WHERE warehouse_type = 'OWN' AND deleted = 0;
