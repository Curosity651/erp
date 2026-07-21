-- =====================================================================
-- A2 人工验证探针：在租户2 插入一条 SKU，用于确认租户1用户看不到它。
-- 仅供验证，验证完请执行文末清理。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < A2_tenant_isolation_probe.sql
-- =====================================================================

-- 插入探针（复制租户1最小id的SKU到租户2，sku_code 加 -T2VERIFY 标记）
INSERT INTO sku (tenant_id, sku_code,
  sku_no,spu_code,sales_country,category_id,product_status,brand_code,project_group_code,chinese_name,russian_name,
  description,remarks,customs_declaration_name,customs_declaration_code,needs_power,seasonal,shipping_type,package_type,
  billing_weight_type,bc_box_min_breakage,packaging,surface_color,frame_color,material,has_rgb_light,has_glass,weight,
  weight_unit,package_length,package_width,package_height,package_unit,quantity_per_pallet,functional_requirements,
  supplier_code,include_tax,tax_rate,purchase_price,minimum_order_quantity,production_cycle,developer_id,operator_id,
  qc_id,purchaser_id,create_time,update_time,create_by,update_by)
SELECT 2, CONCAT(sku_code, '-T2VERIFY'),
  sku_no,spu_code,sales_country,category_id,product_status,brand_code,project_group_code,chinese_name,russian_name,
  description,remarks,customs_declaration_name,customs_declaration_code,needs_power,seasonal,shipping_type,package_type,
  billing_weight_type,bc_box_min_breakage,packaging,surface_color,frame_color,material,has_rgb_light,has_glass,weight,
  weight_unit,package_length,package_width,package_height,package_unit,quantity_per_pallet,functional_requirements,
  supplier_code,include_tax,tax_rate,purchase_price,minimum_order_quantity,production_cycle,developer_id,operator_id,
  qc_id,purchaser_id,create_time,update_time,create_by,update_by
FROM sku WHERE tenant_id = 1 ORDER BY id LIMIT 1;

-- 检查：总数应 = 原数+1；租户1数量不变；租户2有1条探针
SELECT CONCAT('total=', COUNT(*)) AS r FROM sku;
SELECT CONCAT('tenant1=', COUNT(*)) AS r FROM sku WHERE tenant_id = 1;
SELECT CONCAT('tenant2=', COUNT(*)) AS r FROM sku WHERE tenant_id = 2;

-- ===== 验证完成后清理（取消注释执行） =====
-- DELETE FROM sku WHERE tenant_id = 2 AND sku_code LIKE '%-T2VERIFY';
