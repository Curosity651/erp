-- Repair installations where V106 created the reservation table before
-- fulfillment item and logical location tracking was introduced.
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory_reservation'
     AND COLUMN_NAME = 'fulfillment_item_id') = 0,
  'ALTER TABLE wms_inventory_reservation ADD COLUMN fulfillment_item_id bigint NULL AFTER fulfillment_order_id',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory_reservation'
     AND COLUMN_NAME = 'location_id') = 0,
  'ALTER TABLE wms_inventory_reservation ADD COLUMN location_id bigint NULL AFTER inventory_id',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory_reservation'
     AND COLUMN_NAME = 'released_time') = 0,
  'ALTER TABLE wms_inventory_reservation ADD COLUMN released_time datetime NULL AFTER version',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory_reservation'
     AND COLUMN_NAME = 'shipped_time') = 0,
  'ALTER TABLE wms_inventory_reservation ADD COLUMN shipped_time datetime NULL AFTER released_time',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE wms_inventory_reservation r
JOIN wms_location_inventory i ON i.id = r.inventory_id
SET r.location_id = i.location_id
WHERE r.location_id IS NULL;

UPDATE wms_inventory_reservation r
JOIN wms_location_inventory i ON i.id = r.inventory_id
JOIN wms_fulfillment_item fi
  ON fi.fulfillment_order_id = r.fulfillment_order_id
 AND fi.sku_code = i.sku_code
 AND fi.quality = i.quality
 AND fi.deleted = 0
SET r.fulfillment_item_id = fi.id
WHERE r.fulfillment_item_id IS NULL;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory_reservation'
     AND INDEX_NAME = 'idx_reservation_item_status') = 0,
  'CREATE INDEX idx_reservation_item_status ON wms_inventory_reservation (fulfillment_item_id, reservation_status)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory_reservation'
     AND INDEX_NAME = 'idx_reservation_location_status') = 0,
  'CREATE INDEX idx_reservation_location_status ON wms_inventory_reservation (location_id, reservation_status)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
