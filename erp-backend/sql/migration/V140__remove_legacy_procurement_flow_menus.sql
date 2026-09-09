-- Remove the four retired ERP business entries while retaining historical data tables.
-- Button permissions are removed through their stable permission prefixes.
DELETE rm
FROM sys_role_menu rm
INNER JOIN sys_menu menu ON menu.id = rm.menu_id
WHERE menu.path IN ('logistics-provider', 'purchase-order', 'shipping-order', 'purchase-inbound')
   OR menu.uri LIKE '%wms/logistics-provider%'
   OR menu.uri LIKE '%wms/purchase-order%'
   OR menu.uri LIKE '%wms/shipping-order%'
   OR menu.uri LIKE '%wms/purchase-inbound%'
   OR menu.permission LIKE 'wms:logistics-provider:%'
   OR menu.permission LIKE 'wms:purchase-order:%'
   OR menu.permission LIKE 'wms:shipping-order:%'
   OR menu.permission LIKE 'wms:purchase-inbound:%';

UPDATE sys_menu
SET deleted = 1,
    hidden = 1
WHERE path IN ('logistics-provider', 'purchase-order', 'shipping-order', 'purchase-inbound')
   OR uri LIKE '%wms/logistics-provider%'
   OR uri LIKE '%wms/purchase-order%'
   OR uri LIKE '%wms/shipping-order%'
   OR uri LIKE '%wms/purchase-inbound%'
   OR permission LIKE 'wms:logistics-provider:%'
   OR permission LIKE 'wms:purchase-order:%'
   OR permission LIKE 'wms:shipping-order:%'
   OR permission LIKE 'wms:purchase-inbound:%';
