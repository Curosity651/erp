-- Remove retired ERP feature menus without deleting historical business data.
DELETE rm
FROM sys_role_menu rm
INNER JOIN sys_menu menu ON menu.id = rm.menu_id
WHERE menu.path IN (
    'asset-finance', 'ship-prod-calc',
    'logistics-provider', 'purchase-order', 'shipping-order', 'purchase-inbound'
)
   OR menu.uri LIKE '%wms/asset-finance%'
   OR menu.uri LIKE '%wms/ship-prod-calc%'
   OR menu.uri LIKE '%wms/logistics-provider%'
   OR menu.uri LIKE '%wms/purchase-order%'
   OR menu.uri LIKE '%wms/shipping-order%'
   OR menu.uri LIKE '%wms/purchase-inbound%'
   OR menu.permission LIKE 'wms:asset-finance:%'
   OR menu.permission LIKE 'wms:ship-prod-calc:%'
   OR menu.permission LIKE 'wms:logistics-provider:%'
   OR menu.permission LIKE 'wms:purchase-order:%'
   OR menu.permission LIKE 'wms:shipping-order:%'
   OR menu.permission LIKE 'wms:purchase-inbound:%';

DELETE FROM sys_menu
WHERE path IN (
    'asset-finance', 'ship-prod-calc',
    'logistics-provider', 'purchase-order', 'shipping-order', 'purchase-inbound'
)
   OR uri LIKE '%wms/asset-finance%'
   OR uri LIKE '%wms/ship-prod-calc%'
   OR uri LIKE '%wms/logistics-provider%'
   OR uri LIKE '%wms/purchase-order%'
   OR uri LIKE '%wms/shipping-order%'
   OR uri LIKE '%wms/purchase-inbound%'
   OR permission LIKE 'wms:asset-finance:%'
   OR permission LIKE 'wms:ship-prod-calc:%'
   OR permission LIKE 'wms:logistics-provider:%'
   OR permission LIKE 'wms:purchase-order:%'
   OR permission LIKE 'wms:shipping-order:%'
   OR permission LIKE 'wms:purchase-inbound:%';
