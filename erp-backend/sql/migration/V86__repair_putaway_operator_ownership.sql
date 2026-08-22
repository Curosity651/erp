UPDATE wms_physical_inventory inventory
INNER JOIN wms_pallet pallet ON pallet.id = inventory.pallet_id
SET inventory.wms_tenant_id = pallet.wms_tenant_id
WHERE inventory.pallet_id IS NOT NULL
  AND pallet.wms_tenant_id IS NOT NULL
  AND (inventory.wms_tenant_id IS NULL
    OR inventory.wms_tenant_id <> pallet.wms_tenant_id);
