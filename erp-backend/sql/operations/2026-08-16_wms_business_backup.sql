-- Execute before cleanup. The production command uses mysqldump --single-transaction.
SELECT 'sys_user' AS preserved_table, COUNT(*) AS row_count FROM sys_user
UNION ALL SELECT 'sys_menu', COUNT(*) FROM sys_menu
UNION ALL SELECT 'sys_tenant', COUNT(*) FROM sys_tenant
UNION ALL SELECT 'shop', COUNT(*) FROM shop
UNION ALL SELECT 'sku', COUNT(*) FROM sku
UNION ALL SELECT 'sku_mapping', COUNT(*) FROM sku_mapping
UNION ALL SELECT 'erp_order', COUNT(*) FROM erp_order
UNION ALL SELECT 'wms_warehouse', COUNT(*) FROM wms_warehouse
UNION ALL SELECT 'wms_location', COUNT(*) FROM wms_location
UNION ALL SELECT 'wms_service_contract', COUNT(*) FROM wms_service_contract
UNION ALL SELECT 'wms_fee_rate_card', COUNT(*) FROM wms_fee_rate_card;
