-- V25 库存配置按货主隔离收尾（P5+）。
-- 背景：wms_inventory_config 已带 erp_tenant_id（NOT NULL DEFAULT 1，MyMetaObjectHandler 新建盖章），
-- 且已纳入 WmsOwnerDataPermissionHandler.OWNER_TABLES（读取按货主注入 erp_tenant_id 过滤）。
-- 唯一缺陷：唯一键仍是 uk_region_sku(region_id, sku_code) 全局唯一，导致「第二个货主无法对同一
-- (区域,SKU) 保存配置」（撞唯一键）。改为按货主唯一即可让各货主独立配置同一区域同一 SKU。

ALTER TABLE wms_inventory_config
  DROP INDEX uk_region_sku,
  ADD UNIQUE KEY uk_tenant_region_sku (erp_tenant_id, region_id, sku_code);
