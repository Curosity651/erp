-- Repair the legacy test product name that was double-encoded by a Windows
-- mysql client. The byte literal keeps this migration terminal-independent.
UPDATE wms_logistics_product
SET product_name = CONVERT(0xE6A087E58786E789A9E6B581E69C8DE58AA1 USING utf8mb4),
    update_time = NOW()
WHERE product_code = 'STANDARD'
  AND deleted = 0;

-- The current fulfillment rows are test data. Refresh their complete product
-- snapshot from the product master so the shelf page can display and filter it.
UPDATE wms_fulfillment_order fulfillment
JOIN wms_logistics_product product
  ON product.id = fulfillment.logistics_product_id
 AND product.deleted = 0
SET fulfillment.logistics_product_code = product.product_code,
    fulfillment.logistics_product_name = product.product_name,
    fulfillment.logistics_product_description = product.product_description,
    fulfillment.logistics_product_default_fee = product.unit_price,
    fulfillment.logistics_product_actual_fee = COALESCE(
        fulfillment.logistics_product_actual_fee, product.unit_price),
    fulfillment.logistics_product_currency = product.currency,
    fulfillment.update_time = NOW()
WHERE fulfillment.deleted = 0;
