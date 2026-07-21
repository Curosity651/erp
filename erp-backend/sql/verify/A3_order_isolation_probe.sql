-- =====================================================================
-- A3 人工验证探针：在租户2 插入一条订单，确认租户1用户看不到它。
-- 验证完请执行文末清理。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < A3_order_isolation_probe.sql
-- =====================================================================

INSERT INTO erp_order (tenant_id,
  shop_id,platform,platform_order_id,total_quantity,sku_count,shipment_id,warehouse_id,destination_warehouse_id,
  fulfillment_type,platform_status,platform_substatus,erp_status,outbound_status,outbound_order_id,outbound_time,locked,
  product_total_amount,product_currency_code,product_amount_cny,total_amount,currency_code,converted_amount,
  converted_currency_code,total_amount_rub,comment,label_base64,platform_created_at,platform_created_at_moscow,synced_at,
  create_time,update_time,raw_json,version,returned_quantity)
SELECT 2,
  shop_id,platform,CONCAT(platform_order_id,'-T2VERIFY'),total_quantity,sku_count,shipment_id,warehouse_id,destination_warehouse_id,
  fulfillment_type,platform_status,platform_substatus,erp_status,outbound_status,outbound_order_id,outbound_time,locked,
  product_total_amount,product_currency_code,product_amount_cny,total_amount,currency_code,converted_amount,
  converted_currency_code,total_amount_rub,comment,label_base64,platform_created_at,platform_created_at_moscow,synced_at,
  create_time,update_time,raw_json,version,returned_quantity
FROM erp_order WHERE tenant_id = 1 ORDER BY id LIMIT 1;

SELECT CONCAT('total=', COUNT(*)) AS r FROM erp_order;
SELECT CONCAT('tenant1=', COUNT(*)) AS r FROM erp_order WHERE tenant_id = 1;
SELECT CONCAT('tenant2=', COUNT(*)) AS r FROM erp_order WHERE tenant_id = 2;

-- ===== 验证完成后清理（取消注释执行） =====
-- DELETE FROM erp_order WHERE tenant_id = 2 AND platform_order_id LIKE '%-T2VERIFY';
