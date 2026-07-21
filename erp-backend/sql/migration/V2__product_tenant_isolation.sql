-- =====================================================================
-- A2 · 商品模块接入租户隔离
-- 给商品族表加 tenant_id（存量回填=默认租户1），并把唯一键改为租户内唯一。
-- 手工执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V2__product_tenant_isolation.sql
-- 注：本脚本为一次性迁移，勿重复执行。
-- =====================================================================

-- 1. sku：加 tenant_id + 唯一键改为 (tenant_id, sku_code)
ALTER TABLE sku
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id),
  DROP INDEX uk_sku_code,
  ADD UNIQUE KEY uk_tenant_sku_code (tenant_id, sku_code);

-- 2. category：唯一索引名为 code
ALTER TABLE category
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id),
  DROP INDEX code,
  ADD UNIQUE KEY uk_tenant_category_code (tenant_id, code);

-- 3. brand：唯一索引名为 code
ALTER TABLE brand
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id),
  DROP INDEX code,
  ADD UNIQUE KEY uk_tenant_brand_code (tenant_id, code);

-- 4. supplier：唯一索引名为 supplier_code
ALTER TABLE supplier
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id),
  DROP INDEX supplier_code,
  ADD UNIQUE KEY uk_tenant_supplier_code (tenant_id, supplier_code);

-- 5. sku_mapping：唯一索引名为 ux_mapping(platform_item_id)
ALTER TABLE sku_mapping
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id),
  DROP INDEX ux_mapping,
  ADD UNIQUE KEY uk_tenant_mapping (tenant_id, platform_item_id);

-- 6. sku_files：无唯一键，仅加 tenant_id
ALTER TABLE sku_files
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID' AFTER id,
  ADD INDEX idx_tenant_id (tenant_id);
