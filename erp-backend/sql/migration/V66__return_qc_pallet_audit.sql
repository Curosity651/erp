-- Keep the actual three-level pallet placement used by return QC.
ALTER TABLE wms_return_qc_item
    ADD COLUMN qualified_pallet_id BIGINT NULL AFTER qualified_location_code,
    ADD COLUMN qualified_slot_id BIGINT NULL AFTER qualified_pallet_id,
    ADD COLUMN qualified_slot_code VARCHAR(64) NULL AFTER qualified_slot_id,
    ADD COLUMN damaged_pallet_id BIGINT NULL AFTER damaged_location_code,
    ADD COLUMN damaged_slot_id BIGINT NULL AFTER damaged_pallet_id,
    ADD COLUMN damaged_slot_code VARCHAR(64) NULL AFTER damaged_slot_id;

-- Older receiving code marked every SKU as non-electronic. Use SKU power requirements as the
-- existing product-level electronic classification for pending and historical QC evidence rules.
UPDATE wms_return_qc_item q
JOIN wms_return_inbound_order r ON r.id = q.return_order_id
JOIN sku s ON s.tenant_id = r.erp_tenant_id AND s.sku_code = q.sku_code
SET q.electronic = COALESCE(s.needs_power, 0)
WHERE q.electronic = 0;
