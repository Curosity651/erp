-- Run only after a successful owner-side FBO snapshot synchronization.
-- Back up wms_inventory and wms_warehouse before execution.
START TRANSACTION;

DELETE i
FROM wms_inventory i
JOIN wms_warehouse w ON w.id = i.warehouse_id
JOIN (SELECT DISTINCT tenant_id FROM wms_fbo_inventory_snapshot) s
  ON s.tenant_id = i.erp_tenant_id
WHERE w.warehouse_type = 'FBO';

UPDATE wms_warehouse w
SET w.deleted = UNIX_TIMESTAMP(NOW())
WHERE w.warehouse_type = 'FBO'
  AND w.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM wms_inventory i WHERE i.warehouse_id = w.id);

COMMIT;
