-- ERP 平台订单在海外仓实际签出前始终保持 ALLOCATED。
-- 同时补齐历史草稿/待下架单与销售出库单之间的绑定关系。
UPDATE erp_order eo
INNER JOIN wms_sales_outbound_order_item soi ON soi.erp_order_id = eo.id
INNER JOIN wms_sales_outbound_order so ON so.id = soi.outbound_order_id
SET eo.outbound_status = 'ALLOCATED',
    eo.outbound_order_id = so.id,
    eo.outbound_time = NULL,
    eo.update_time = NOW()
WHERE so.source_type = 'SALES'
  AND so.order_status IN ('DRAFT', 'CONFIRMED', 'BACKORDER', 'PICKING', 'PICKED', 'PACKED')
  AND eo.outbound_status IN ('ALLOCATED', 'COMPLETED');

-- 旧逻辑下，已确认后再取消的单可能残留为 COMPLETED；仅释放仍指向该取消单的订单。
UPDATE erp_order eo
INNER JOIN wms_sales_outbound_order_item soi ON soi.erp_order_id = eo.id
INNER JOIN wms_sales_outbound_order so ON so.id = soi.outbound_order_id
SET eo.outbound_status = 'NONE',
    eo.outbound_order_id = NULL,
    eo.outbound_time = NULL,
    eo.update_time = NOW()
WHERE so.source_type = 'SALES'
  AND so.order_status = 'CANCELLED'
  AND eo.outbound_order_id = so.id
  AND eo.outbound_status IN ('ALLOCATED', 'COMPLETED');
