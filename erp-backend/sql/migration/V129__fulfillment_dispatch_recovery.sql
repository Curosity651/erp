ALTER TABLE wms_fulfillment_order
    ADD COLUMN dispatch_status varchar(20) NULL COMMENT 'PENDING/SUCCEEDED/FAILED' AFTER fulfillment_status,
    ADD COLUMN dispatch_error varchar(500) NULL AFTER dispatch_status,
    ADD KEY idx_fulfillment_dispatch (warehouse_id, dispatch_status, fulfillment_status);

UPDATE wms_fulfillment_order fulfillment
JOIN wms_fulfillment_pick_task_order task_order
  ON task_order.fulfillment_order_id = fulfillment.id
SET fulfillment.dispatch_status = 'SUCCEEDED',
    fulfillment.dispatch_error = NULL
WHERE fulfillment.deleted = 0;

UPDATE wms_fulfillment_order fulfillment
SET fulfillment.dispatch_status = 'FAILED',
    fulfillment.dispatch_error = '历史订单已下架但未生成拣货任务，请重新派单'
WHERE fulfillment.deleted = 0
  AND fulfillment.fulfillment_status = 'WAITING_PICK'
  AND NOT EXISTS (
      SELECT 1 FROM wms_fulfillment_pick_task_order task_order
      WHERE task_order.fulfillment_order_id = fulfillment.id
  );
