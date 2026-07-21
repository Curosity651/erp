-- =====================================================================
-- V21 校验：货主单据隔离 + 管理员补权限（import V21 后执行，只读）
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/verify/V21_verify_owner_isolation.sql
-- =====================================================================

-- 1) 6 张表均已加 erp_tenant_id 且存量回填为 1（应各有非空且 distinct 含 1）
SELECT 'wms_logistics_provider' t, COUNT(*) rows_cnt, COUNT(DISTINCT erp_tenant_id) distinct_tenant, MIN(erp_tenant_id) min_t FROM wms_logistics_provider
UNION ALL SELECT 'wms_purchase_order', COUNT(*), COUNT(DISTINCT erp_tenant_id), MIN(erp_tenant_id) FROM wms_purchase_order
UNION ALL SELECT 'wms_shipping_order', COUNT(*), COUNT(DISTINCT erp_tenant_id), MIN(erp_tenant_id) FROM wms_shipping_order
UNION ALL SELECT 'wms_sales_outbound_order', COUNT(*), COUNT(DISTINCT erp_tenant_id), MIN(erp_tenant_id) FROM wms_sales_outbound_order
UNION ALL SELECT 'wms_return_inbound_order', COUNT(*), COUNT(DISTINCT erp_tenant_id), MIN(erp_tenant_id) FROM wms_return_inbound_order
UNION ALL SELECT 'wms_inventory_config', COUNT(*), COUNT(DISTINCT erp_tenant_id), MIN(erp_tenant_id) FROM wms_inventory_config;

-- 2) 存量行 erp_tenant_id 是否全为 1（应无非1行）
SELECT 'non_1_rows' label,
  (SELECT COUNT(*) FROM wms_purchase_order WHERE erp_tenant_id<>1)
  + (SELECT COUNT(*) FROM wms_shipping_order WHERE erp_tenant_id<>1)
  + (SELECT COUNT(*) FROM wms_sales_outbound_order WHERE erp_tenant_id<>1)
  + (SELECT COUNT(*) FROM wms_return_inbound_order WHERE erp_tenant_id<>1)
  + (SELECT COUNT(*) FROM wms_logistics_provider WHERE erp_tenant_id<>1)
  + (SELECT COUNT(*) FROM wms_inventory_config WHERE erp_tenant_id<>1) AS cnt;

-- 3) sku:export 菜单已建
SELECT id, parent_id, title, permission FROM sys_menu WHERE id=130405;

-- 4) 租户管理员角色已补 共享只读码 + 货主补 sku:export
SELECT rm.role_code, m.permission
FROM sys_role_menu rm JOIN sys_menu m ON m.id=rm.menu_id
WHERE rm.role_code IN ('ROLE_ADMIN_T1','ROLE_ADMIN_T2','ROLE_ADMIN_T3','ROLE_ADMIN_T4')
  AND m.id IN (160101,161601,130405)
ORDER BY rm.role_code, m.permission;
-- 期望：T1/T4 各有 warehouse:read+region:read+sku:export；T2/T3 各有 warehouse:read+region:read
