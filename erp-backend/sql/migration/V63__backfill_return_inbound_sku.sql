-- Backfill linked return declarations created from order items whose sku_code is empty.
UPDATE wms_return_inbound_order ri
INNER JOIN erp_order_item oi ON oi.id = ri.order_item_id
INNER JOIN sku_mapping sm
    ON sm.tenant_id = ri.erp_tenant_id
    AND sm.platform_item_id = oi.platform_item_id
    AND sm.deleted = 0
SET ri.sku_code = sm.sku_code
WHERE ri.deleted = 0
  AND (ri.sku_code IS NULL OR ri.sku_code = '');
