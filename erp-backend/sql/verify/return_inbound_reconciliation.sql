-- Completed linked returns whose received totals disagree with the order item snapshot.
SELECT
    ri.order_item_id,
    oi.order_id,
    oi.sku_code,
    COALESCE(oi.returned_quantity, 0) AS recorded_returned_quantity,
    SUM(COALESCE(q.received_qty, 0)) AS completed_received_quantity
FROM wms_return_inbound_order ri
INNER JOIN wms_return_qc_item q ON q.return_order_id = ri.id
INNER JOIN erp_order_item oi ON oi.id = ri.order_item_id
WHERE ri.deleted = 0
  AND ri.return_status = 'COMPLETED'
GROUP BY ri.order_item_id, oi.order_id, oi.sku_code, oi.returned_quantity
HAVING recorded_returned_quantity <> completed_received_quantity;

