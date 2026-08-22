-- Lightweight WMS-provider logistics products and immutable fulfillment snapshots.

ALTER TABLE wms_logistics_product
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'RUB' AFTER unit_price,
    ADD COLUMN product_description VARCHAR(2000) NULL AFTER currency,
    ADD UNIQUE KEY uk_logistics_product_code (wms_tenant_id, product_code, deleted);

ALTER TABLE shop
    ADD COLUMN default_logistics_product_id BIGINT NULL AFTER default_wms_warehouse_id;

ALTER TABLE wms_fulfillment_order
    ADD COLUMN logistics_product_id BIGINT NULL AFTER shop_id,
    ADD COLUMN logistics_product_code VARCHAR(50) NULL AFTER logistics_product_id,
    ADD COLUMN logistics_product_name VARCHAR(100) NULL AFTER logistics_product_code,
    ADD COLUMN logistics_product_description VARCHAR(2000) NULL AFTER logistics_product_name,
    ADD COLUMN logistics_product_default_fee DECIMAL(12, 2) NULL AFTER logistics_product_description,
    ADD COLUMN logistics_product_actual_fee DECIMAL(12, 2) NULL AFTER logistics_product_default_fee,
    ADD COLUMN logistics_product_currency VARCHAR(3) NULL AFTER logistics_product_actual_fee,
    ADD COLUMN logistics_fee_adjustment_reason VARCHAR(500) NULL AFTER logistics_product_currency,
    ADD COLUMN shipping_method VARCHAR(128) NULL AFTER carrier_name;

UPDATE wms_logistics_product
SET currency = 'RUB'
WHERE currency IS NULL OR currency = '';
