-- Demo data for the complete owner -> shelf -> picking -> packing -> shipping flow.
-- External Ozon ACCEPT/FETCH_LABEL calls are represented by successful action snapshots.
-- All warehouse reservation, picking, packing, billing and shipping logic remains real.

START TRANSACTION;

-- Repair the existing fake Ozon order so it can pass the external ACCEPT boundary.
UPDATE wms_fulfillment_platform_action action_row
JOIN wms_fulfillment_order fulfillment ON fulfillment.id = action_row.fulfillment_order_id
SET action_row.action_status = 'SUCCEEDED',
    action_row.response_payload = JSON_OBJECT(
      'success', TRUE,
      'message', '测试订单：模拟 Ozon 确认成功',
      'externalReference', fulfillment.source_order_no
    ),
    action_row.error_message = NULL,
    action_row.completed_time = NOW(),
    action_row.update_time = NOW()
WHERE fulfillment.source_order_no = 'TEST-OZON-FLOW-20260824201812'
  AND action_row.action_type = 'ACCEPT';

INSERT INTO wms_fulfillment_platform_action (
  fulfillment_order_id, action_type, action_status, request_fingerprint,
  request_payload, response_payload, error_message, attempt_count,
  started_time, completed_time, create_time, update_time
)
SELECT fulfillment.id, 'FETCH_LABEL', 'SUCCEEDED',
       SHA2(CONCAT(fulfillment.id, ':TEST:FETCH_LABEL'), 256),
       JSON_OBJECT('testOrder', fulfillment.source_order_no),
       JSON_OBJECT(
         'success', TRUE,
         'message', '测试订单：模拟面单已生成',
         'externalReference', fulfillment.source_order_no,
         'labelUrl', CONCAT('data:text/html;charset=utf-8,%3Chtml%3E%3Cbody%3E%3Ch1%3ETEST%20OZON%20LABEL%3C%2Fh1%3E%3Cp%3E', fulfillment.source_order_no, '%3C%2Fp%3E%3C%2Fbody%3E%3C%2Fhtml%3E'),
         'labelBarcode', CONCAT('TEST-', fulfillment.id, '-OZON')
       ),
       NULL, 1, NOW(), NOW(), NOW(), NOW()
FROM wms_fulfillment_order fulfillment
WHERE fulfillment.source_order_no = 'TEST-OZON-FLOW-20260824201812'
  AND NOT EXISTS (
    SELECT 1 FROM wms_fulfillment_platform_action existing
    WHERE existing.fulfillment_order_id = fulfillment.id
      AND existing.action_type = 'FETCH_LABEL'
  );

DROP PROCEDURE IF EXISTS seed_fulfillment_demo_order;
DELIMITER $$
CREATE PROCEDURE seed_fulfillment_demo_order(
  IN p_sequence INT,
  IN p_sku_code VARCHAR(128),
  IN p_quantity INT
)
BEGIN
  DECLARE v_order_id BIGINT;
  DECLARE v_fulfillment_id BIGINT;
  DECLARE v_fulfillment_item_id BIGINT;
  DECLARE v_inventory_id BIGINT DEFAULT NULL;
  DECLARE v_location_id BIGINT DEFAULT NULL;
  DECLARE v_sku_name VARCHAR(500);
  DECLARE v_length_mm INT;
  DECLARE v_width_mm INT;
  DECLARE v_height_mm INT;
  DECLARE v_weight_g INT;
  DECLARE v_order_no VARCHAR(128);
  DECLARE v_recipient VARCHAR(128);

  SET v_order_no = CONCAT('TEST-OZON-PICK-20260825-', LPAD(p_sequence, 2, '0'));
  SET v_recipient = CONCAT('流程测试收货人', LPAD(p_sequence, 2, '0'));

  IF EXISTS (
    SELECT 1 FROM erp_order
    WHERE tenant_id = 6
      AND CONVERT(platform USING utf8mb4) COLLATE utf8mb4_unicode_ci
          = CONVERT('ozon' USING utf8mb4) COLLATE utf8mb4_unicode_ci
      AND CONVERT(platform_order_id USING utf8mb4) COLLATE utf8mb4_unicode_ci
          = CONVERT(v_order_no USING utf8mb4) COLLATE utf8mb4_unicode_ci
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '测试订单已经存在，请勿重复执行脚本';
  END IF;

  SELECT inventory.id, inventory.location_id, sku.chinese_name,
         sku.outer_length_mm, sku.outer_width_mm, sku.outer_height_mm,
         sku.outer_gross_weight_g
  INTO v_inventory_id, v_location_id, v_sku_name,
       v_length_mm, v_width_mm, v_height_mm, v_weight_g
  FROM wms_location_inventory inventory
  JOIN sku ON sku.tenant_id = inventory.erp_tenant_id
          AND CONVERT(sku.sku_code USING utf8mb4) COLLATE utf8mb4_unicode_ci
              = CONVERT(inventory.sku_code USING utf8mb4) COLLATE utf8mb4_unicode_ci
  WHERE inventory.tenant_id = -1
    AND inventory.wms_tenant_id = 5
    AND inventory.erp_tenant_id = 6
    AND inventory.warehouse_id = 53
    AND CONVERT(inventory.sku_code USING utf8mb4) COLLATE utf8mb4_unicode_ci
        = CONVERT(p_sku_code USING utf8mb4) COLLATE utf8mb4_unicode_ci
    AND CONVERT(inventory.quality USING utf8mb4) COLLATE utf8mb4_unicode_ci
        = CONVERT('GOOD' USING utf8mb4) COLLATE utf8mb4_unicode_ci
    AND inventory.deleted = 0
    AND inventory.quantity - inventory.reserved_quantity >= p_quantity
  ORDER BY inventory.location_id, inventory.id
  LIMIT 1;

  IF v_inventory_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '测试订单可用库存不足';
  END IF;

  INSERT INTO erp_order (
    tenant_id, shop_id, platform, platform_order_id, total_quantity, sku_count,
    shipment_id, warehouse_id, warehouse_name, delivery_method_id,
    delivery_method_name, destination_warehouse_id, fulfillment_type,
    platform_status, platform_substatus, erp_status, outbound_status,
    locked, confirm_state, product_total_amount, product_currency_code,
    total_amount, currency_code, comment, platform_created_at,
    platform_created_at_moscow, synced_at, create_time, update_time,
    raw_json, version, wms_warehouse_id, warehouse_fulfillment_status
  ) VALUES (
    6, 6004, 'ozon', v_order_no, p_quantity, 1,
    CONCAT('TEST-SHIP-', LPAD(p_sequence, 2, '0')), '1020005006216570', '大件',
    1020005006216570, '测试 Ozon 配送方式', '1020005006216570', 'FBS',
    'awaiting_packaging', 'posting_created', 'READY_TO_SHIP', 'NONE',
    0, 'NONE', 199900 * p_quantity, 'RUB', 1999.00 * p_quantity, 'RUB',
    '完整拣货出库流程测试数据', NOW(), NOW(), NOW(), NOW(), NOW(),
    JSON_OBJECT(
      'status', 'awaiting_packaging',
      'substatus', 'posting_created',
      'posting_number', v_order_no,
      'recipient_name', v_recipient,
      'recipient_phone', CONCAT('+799900001', LPAD(p_sequence, 2, '0')),
      'recipient_address', CONCAT('Russia, Moscow, Demo Street ', p_sequence),
      'products', JSON_ARRAY(JSON_OBJECT(
        'name', v_sku_name,
        'offer_id', p_sku_code,
        'quantity', p_quantity,
        'price', '1999.00',
        'currency_code', 'RUB'
      ))
    ),
    0, 53, 'WAITING_SHELF'
  );
  SET v_order_id = LAST_INSERT_ID();

  INSERT INTO erp_order_item (
    tenant_id, order_id, platform_item_id, sku_code, quantity,
    item_price, item_amount, item_amount_rub, returned_quantity,
    create_time, update_time
  ) VALUES (
    6, v_order_id, CONCAT(v_order_no, '-', p_sku_code), p_sku_code, p_quantity,
    1999.00, 1999.00 * p_quantity, 1999.00 * p_quantity, 0, NOW(), NOW()
  );

  INSERT INTO wms_fulfillment_order (
    tenant_id, wms_tenant_id, erp_tenant_id, warehouse_id, shop_id,
    logistics_product_id, logistics_product_code, logistics_product_name,
    logistics_product_description, logistics_product_default_fee,
    logistics_product_actual_fee, logistics_product_currency,
    fulfillment_no, source_type, source_order_id, source_order_no,
    platform_status, fulfillment_status, recipient_name, recipient_phone,
    recipient_address, version, create_time, update_time, deleted
  ) VALUES (
    -1, 5, 6, 53, 6004, 2, 'STANDARD', '标准物流服务',
    '仓库按实际情况选择承运商和运输方式；签出时记录物流信息。',
    20.00, 20.00, 'RUB', CONCAT('FO-OZON-', v_order_id), 'OZON',
    v_order_id, v_order_no, 'awaiting_packaging', 'WAITING_SHELF',
    v_recipient, CONCAT('+799900001', LPAD(p_sequence, 2, '0')),
    CONCAT('Russia, Moscow, Demo Street ', p_sequence), 0, NOW(), NOW(), 0
  );
  SET v_fulfillment_id = LAST_INSERT_ID();

  UPDATE erp_order
  SET fulfillment_order_id = v_fulfillment_id,
      wms_warehouse_id = 53,
      warehouse_fulfillment_status = 'WAITING_SHELF'
  WHERE id = v_order_id;

  INSERT INTO wms_fulfillment_item (
    fulfillment_order_id, sku_code, warehouse_sku_code, sku_name,
    quality, quantity, picked_quantity, outer_length_mm, outer_width_mm,
    outer_height_mm, outer_gross_weight_g, create_time, update_time, deleted
  ) VALUES (
    v_fulfillment_id, p_sku_code, CONCAT('JHIN-', p_sku_code), v_sku_name,
    'GOOD', p_quantity, 0, v_length_mm, v_width_mm, v_height_mm,
    v_weight_g, NOW(), NOW(), 0
  );
  SET v_fulfillment_item_id = LAST_INSERT_ID();

  UPDATE wms_location_inventory
  SET reserved_quantity = reserved_quantity + p_quantity,
      version = version + 1,
      update_time = NOW()
  WHERE id = v_inventory_id
    AND quantity - reserved_quantity >= p_quantity;

  IF ROW_COUNT() <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '测试订单库存预占冲突';
  END IF;

  INSERT INTO wms_inventory_reservation (
    fulfillment_order_id, fulfillment_item_id, inventory_id, location_id,
    quantity, reservation_status, version, create_time, update_time
  ) VALUES (
    v_fulfillment_id, v_fulfillment_item_id, v_inventory_id, v_location_id,
    p_quantity, 'RESERVED', 0, NOW(), NOW()
  );

  INSERT INTO wms_fulfillment_platform_action (
    fulfillment_order_id, action_type, action_status, request_fingerprint,
    request_payload, response_payload, error_message, attempt_count,
    started_time, completed_time, create_time, update_time
  ) VALUES (
    v_fulfillment_id, 'ACCEPT', 'SUCCEEDED',
    SHA2(CONCAT(v_fulfillment_id, ':TEST:ACCEPT'), 256),
    JSON_OBJECT('testOrder', v_order_no),
    JSON_OBJECT(
      'success', TRUE,
      'message', '测试订单：模拟 Ozon 确认成功',
      'externalReference', v_order_no
    ),
    NULL, 1, NOW(), NOW(), NOW(), NOW()
  );

  INSERT INTO wms_fulfillment_platform_action (
    fulfillment_order_id, action_type, action_status, request_fingerprint,
    request_payload, response_payload, error_message, attempt_count,
    started_time, completed_time, create_time, update_time
  ) VALUES (
    v_fulfillment_id, 'FETCH_LABEL', 'SUCCEEDED',
    SHA2(CONCAT(v_fulfillment_id, ':TEST:FETCH_LABEL'), 256),
    JSON_OBJECT('testOrder', v_order_no),
    JSON_OBJECT(
      'success', TRUE,
      'message', '测试订单：模拟面单已生成',
      'externalReference', v_order_no,
      'labelUrl', CONCAT('data:text/html;charset=utf-8,%3Chtml%3E%3Cbody%3E%3Ch1%3ETEST%20OZON%20LABEL%3C%2Fh1%3E%3Cp%3E', v_order_no, '%3C%2Fp%3E%3C%2Fbody%3E%3C%2Fhtml%3E'),
      'labelBarcode', CONCAT('TEST-', v_fulfillment_id, '-OZON')
    ),
    NULL, 1, NOW(), NOW(), NOW(), NOW()
  );
END$$
DELIMITER ;

CALL seed_fulfillment_demo_order(1, 'X001-BL-LED', 1);
CALL seed_fulfillment_demo_order(2, 'ADNZ-004-QXM-02', 1);
CALL seed_fulfillment_demo_order(3, 'ADNZ-012-SXM-02', 1);
CALL seed_fulfillment_demo_order(4, 'DJZ-017', 1);

DROP PROCEDURE seed_fulfillment_demo_order;
COMMIT;
