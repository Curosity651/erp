-- The warehouse, rows, zones and logical locations are master data and remain in place.
-- The new inventory ledger intentionally starts empty after cleanup.
SELECT w.id AS warehouse_id, w.warehouse_name, COUNT(l.id) AS logical_location_count
FROM wms_warehouse w
LEFT JOIN wms_location l ON l.warehouse_id = w.id AND l.deleted = 0
WHERE w.deleted = 0
GROUP BY w.id, w.warehouse_name
ORDER BY w.id;

SELECT COUNT(*) AS logical_inventory_rows FROM wms_location_inventory;
SELECT COUNT(*) AS active_reservations FROM wms_inventory_reservation WHERE reservation_status = 'RESERVED';
