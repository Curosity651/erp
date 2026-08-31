-- Retire the old sales-outbound entry while retaining read-only compatibility APIs.
DELETE FROM sys_role_menu
WHERE menu_id IN (
    SELECT id FROM sys_menu WHERE id = 160600 OR parent_id = 160600
);

UPDATE sys_menu
SET hidden = 1,
    update_time = NOW()
WHERE id = 160600
  AND deleted = 0;

-- Existing JHIN shops predate the warehouse-default field. Use the active internal warehouse.
UPDATE shop s
JOIN sys_tenant t ON t.id = s.tenant_id AND t.tenant_code = 'jhin_CODE' AND t.deleted = 0
JOIN (
    SELECT MIN(id) AS warehouse_id
    FROM wms_warehouse
    WHERE warehouse_type = 'OWN' AND status = 1 AND deleted = 0
) w ON w.warehouse_id IS NOT NULL
SET s.default_wms_warehouse_id = w.warehouse_id,
    s.update_time = NOW()
WHERE s.default_wms_warehouse_id IS NULL;

-- Remove references left by previously cleared legacy outbound test data.
UPDATE erp_order o
SET o.outbound_order_id = NULL,
    o.outbound_status = 'NONE',
    o.outbound_time = NULL,
    o.update_time = NOW()
WHERE o.outbound_order_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM wms_sales_outbound_order legacy_order
      WHERE legacy_order.id = o.outbound_order_id
  );
