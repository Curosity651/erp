-- Linked return integrity: split QC result and speed up in-flight reservation checks.
ALTER TABLE wms_return_qc_item
    ADD COLUMN qualified_qty INT NULL COMMENT '良品数量' AFTER received_qty,
    ADD COLUMN damaged_qty INT NULL COMMENT '残次品数量' AFTER qualified_qty,
    ADD COLUMN qualified_zone VARCHAR(20) NULL COMMENT '良品回库分区' AFTER damaged_qty,
    ADD COLUMN qualified_location_code VARCHAR(50) NULL COMMENT '良品回库库位' AFTER qualified_zone,
    ADD COLUMN damaged_location_code VARCHAR(50) NULL COMMENT '残次品回库库位' AFTER qualified_location_code;

ALTER TABLE wms_return_inbound_order
    ADD INDEX idx_return_item_status (order_item_id, return_status, deleted);

-- Preserve readable split results for returns completed before this migration.
UPDATE wms_return_qc_item
SET qualified_qty = CASE WHEN qc_result = 'PASS' THEN received_qty ELSE 0 END,
    damaged_qty = CASE WHEN qc_result = 'FAIL' THEN received_qty ELSE 0 END,
    qualified_zone = CASE WHEN qc_result = 'PASS' THEN zone ELSE NULL END,
    qualified_location_code = CASE WHEN qc_result = 'PASS' THEN location_code ELSE NULL END,
    damaged_location_code = CASE WHEN qc_result = 'FAIL' THEN location_code ELSE NULL END
WHERE qualified_qty IS NULL
  AND qc_result IN ('PASS', 'FAIL');
