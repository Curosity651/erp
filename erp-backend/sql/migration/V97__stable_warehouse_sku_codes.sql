ALTER TABLE sys_tenant
    ADD COLUMN warehouse_sku_prefix VARCHAR(32) NULL COMMENT '海外仓内部SKU稳定前缀' AFTER tenant_name;

UPDATE sys_tenant
SET warehouse_sku_prefix = UPPER(REPLACE(TRIM(tenant_name), ' ', '_'))
WHERE tenant_type = 'ERP_USER'
  AND warehouse_sku_prefix IS NULL;

ALTER TABLE sys_tenant
    ADD UNIQUE KEY uk_warehouse_sku_prefix (warehouse_sku_prefix);

ALTER TABLE wms_outbound_scan_event
    MODIFY COLUMN scan_code VARCHAR(256) NOT NULL COMMENT '扫描码';
