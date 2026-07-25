ALTER TABLE `wms_outbound_pick_task`
  MODIFY COLUMN `order_count` int NOT NULL DEFAULT 0 COMMENT '任务关联的销售/自定义出库单数',
  ADD COLUMN `sales_order_count` int NOT NULL DEFAULT 0 COMMENT '关联的电商平台销售订单数' AFTER `order_count`;

UPDATE `wms_outbound_pick_task` t
SET t.sales_order_count = (
  SELECT COALESCE(SUM(o.order_count), 0)
  FROM `wms_outbound_pick_task_order` r
  JOIN `wms_sales_outbound_order` o ON o.id = r.outbound_order_id
  WHERE r.task_id = t.id
);
