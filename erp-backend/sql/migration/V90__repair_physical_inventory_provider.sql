-- Physical inventory must belong to the WMS provider linked to its ERP owner.
-- Rows without an unambiguous owner/provider relationship are intentionally left unchanged.
UPDATE wms_physical_inventory pi
JOIN sys_tenant owner ON owner.id = pi.erp_tenant_id
SET pi.wms_tenant_id = owner.parent_wms_tenant_id,
    pi.update_time = CURRENT_TIMESTAMP
WHERE owner.parent_wms_tenant_id IS NOT NULL
  AND owner.parent_wms_tenant_id > 0
  AND (pi.wms_tenant_id IS NULL OR pi.wms_tenant_id <> owner.parent_wms_tenant_id);
