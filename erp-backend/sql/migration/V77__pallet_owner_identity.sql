-- A pallet is a physical handling unit owned by exactly one ERP owner.

ALTER TABLE wms_pallet
    ADD COLUMN wms_tenant_id bigint NULL COMMENT 'parent WMS operator' AFTER warehouse_id,
    ADD COLUMN erp_tenant_id bigint NULL COMMENT 'single ERP owner of pallet' AFTER wms_tenant_id,
    ADD COLUMN label_version int NOT NULL DEFAULT 1 COMMENT 'printed label format version' AFTER locked_reason,
    ADD KEY idx_pallet_owner (erp_tenant_id, warehouse_id, pallet_status),
    ADD KEY idx_pallet_operator (wms_tenant_id, warehouse_id);

UPDATE wms_pallet p
JOIN (
    SELECT pallet_id, MIN(erp_tenant_id) erp_tenant_id, COUNT(DISTINCT erp_tenant_id) owner_count
    FROM wms_physical_inventory
    WHERE pallet_id IS NOT NULL AND quantity > 0
    GROUP BY pallet_id
) x ON x.pallet_id = p.id
LEFT JOIN sys_tenant owner ON owner.id = x.erp_tenant_id
SET p.erp_tenant_id = CASE WHEN x.owner_count = 1 THEN x.erp_tenant_id ELSE NULL END,
    p.wms_tenant_id = CASE WHEN x.owner_count = 1 THEN owner.parent_wms_tenant_id ELSE NULL END,
    p.locked_reason = CASE WHEN x.owner_count > 1
        THEN '历史跨货主混托，请拆托后继续作业' ELSE p.locked_reason END,
    p.pallet_status = CASE WHEN x.owner_count > 1 THEN 'LOCKED' ELSE p.pallet_status END;

UPDATE wms_warehouse
SET allow_cross_owner_mix = 0
WHERE deleted = 0;

