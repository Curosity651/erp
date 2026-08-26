-- WMS logistics products are settled between ERP owners and their WMS provider in CNY.
-- Repair the product definition and every persisted downstream currency snapshot together.
UPDATE wms_client_billing_record b
JOIN wms_logistics_product p ON p.id = b.logistics_product_id
SET b.currency = 'CNY'
WHERE b.biz_id LIKE 'FULFILLMENT_LOGISTICS:%'
  AND UPPER(COALESCE(b.currency, '')) = 'RUB'
  AND UPPER(COALESCE(p.currency, '')) = 'RUB';

UPDATE wms_fulfillment_order o
JOIN wms_logistics_product p ON p.id = o.logistics_product_id
SET o.logistics_product_currency = 'CNY'
WHERE UPPER(COALESCE(o.logistics_product_currency, '')) = 'RUB'
  AND UPPER(COALESCE(p.currency, '')) = 'RUB';

UPDATE wms_logistics_product
SET currency = 'CNY'
WHERE UPPER(COALESCE(currency, '')) = 'RUB';
