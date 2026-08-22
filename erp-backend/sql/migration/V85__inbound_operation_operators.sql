ALTER TABLE wms_purchase_inbound_order
    ADD COLUMN receive_by BIGINT NULL COMMENT '收货操作员ID' AFTER stock_posting_id,
    ADD COLUMN receive_time DATETIME NULL COMMENT '收货时间' AFTER receive_by,
    ADD COLUMN putaway_by BIGINT NULL COMMENT '上架操作员ID' AFTER receive_time,
    ADD COLUMN putaway_time DATETIME NULL COMMENT '上架时间' AFTER putaway_by,
    ADD INDEX idx_inbound_receive_by (receive_by),
    ADD INDEX idx_inbound_putaway_by (putaway_by);

UPDATE wms_purchase_inbound_order inbound_order
INNER JOIN (
    SELECT
        CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(request_body, '"inboundOrderId":', -1), ',', 1) AS UNSIGNED) AS inbound_order_id,
        CAST(SUBSTRING_INDEX(GROUP_CONCAT(user_id ORDER BY create_time DESC), ',', 1) AS UNSIGNED) AS operator_id,
        MAX(create_time) AS operation_time
    FROM log_access_log
    WHERE request_uri = '/wms/inbound-execution/receive'
      AND request_method = 'POST'
      AND response_status = 200
      AND user_id IS NOT NULL
      AND request_body LIKE '%"inboundOrderId":%'
    GROUP BY CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(request_body, '"inboundOrderId":', -1), ',', 1) AS UNSIGNED)
) receive_log ON receive_log.inbound_order_id = inbound_order.id
SET inbound_order.receive_by = receive_log.operator_id,
    inbound_order.receive_time = receive_log.operation_time
WHERE inbound_order.receive_by IS NULL;

UPDATE wms_purchase_inbound_order inbound_order
INNER JOIN (
    SELECT
        CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(request_body, '"inboundOrderId":', -1), ',', 1) AS UNSIGNED) AS inbound_order_id,
        CAST(SUBSTRING_INDEX(GROUP_CONCAT(user_id ORDER BY create_time DESC), ',', 1) AS UNSIGNED) AS operator_id,
        MAX(create_time) AS operation_time
    FROM log_access_log
    WHERE request_uri = '/wms/inbound-execution/putaway'
      AND request_method = 'POST'
      AND response_status = 200
      AND user_id IS NOT NULL
      AND request_body LIKE '%"inboundOrderId":%'
    GROUP BY CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(request_body, '"inboundOrderId":', -1), ',', 1) AS UNSIGNED)
) putaway_log ON putaway_log.inbound_order_id = inbound_order.id
SET inbound_order.putaway_by = putaway_log.operator_id,
    inbound_order.putaway_time = putaway_log.operation_time
WHERE inbound_order.putaway_by IS NULL;
