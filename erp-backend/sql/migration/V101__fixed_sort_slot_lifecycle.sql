ALTER TABLE wms_sales_outbound_package
    ADD COLUMN sort_slot_id BIGINT NULL AFTER platform_order_id,
    ADD COLUMN sort_slot_scan_code VARCHAR(100) NULL AFTER sort_slot_id;

CREATE TABLE wms_sort_slot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    slot_code VARCHAR(32) NOT NULL,
    scan_code VARCHAR(100) NOT NULL,
    slot_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    task_id BIGINT NULL,
    outbound_order_id BIGINT NULL,
    package_id BIGINT NULL,
    reserved_time DATETIME NULL,
    occupied_time DATETIME NULL,
    ready_time DATETIME NULL,
    packing_time DATETIME NULL,
    released_time DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sort_slot_warehouse_code (warehouse_id, slot_code),
    UNIQUE KEY uk_sort_slot_scan_code (scan_code),
    UNIQUE KEY uk_sort_slot_package (package_id),
    KEY idx_sort_slot_task (task_id),
    KEY idx_sort_slot_status (warehouse_id, slot_status, slot_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库固定物理分货格';

INSERT INTO wms_sort_slot (warehouse_id, slot_code, scan_code, slot_status)
SELECT w.id,
       CONCAT('B', LPAD(numbers.n, 2, '0')),
       CONCAT('SORT:', UPPER(w.warehouse_code), ':B', LPAD(numbers.n, 2, '0')),
       'AVAILABLE'
FROM wms_warehouse w
CROSS JOIN (
    SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
    UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
    UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25
    UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
    UNION ALL SELECT 31 UNION ALL SELECT 32 UNION ALL SELECT 33 UNION ALL SELECT 34 UNION ALL SELECT 35
    UNION ALL SELECT 36 UNION ALL SELECT 37 UNION ALL SELECT 38 UNION ALL SELECT 39 UNION ALL SELECT 40
) numbers
WHERE w.deleted = 0;

CREATE TEMPORARY TABLE tmp_active_sort_binding AS
SELECT p.id AS package_id,
       p.outbound_order_id,
       o.warehouse_id,
       (
           SELECT relation.task_id
           FROM wms_outbound_pick_task_order relation
           INNER JOIN wms_outbound_pick_task task ON task.id = relation.task_id
           WHERE relation.outbound_order_id = p.outbound_order_id
             AND task.task_status != 'CANCELLED'
           ORDER BY task.id DESC
           LIMIT 1
       ) AS task_id,
       ROW_NUMBER() OVER (
           PARTITION BY o.warehouse_id
           ORDER BY COALESCE(p.sort_time, p.create_time), p.id
       ) AS slot_no,
       p.sort_status,
       p.pack_status
FROM wms_sales_outbound_package p
INNER JOIN wms_sales_outbound_order o ON o.id = p.outbound_order_id
WHERE COALESCE(p.ship_status, 'PENDING') != 'SHIPPED'
  AND p.sort_status IN ('PENDING', 'SORTED')
  AND p.sort_code IS NOT NULL
  AND o.order_status IN ('PICKING', 'PICKED', 'PACKED');

UPDATE wms_sales_outbound_package p
INNER JOIN tmp_active_sort_binding binding ON binding.package_id = p.id
INNER JOIN wms_sort_slot slot
        ON slot.warehouse_id = binding.warehouse_id
       AND slot.slot_code = CONCAT('B', LPAD(binding.slot_no, 2, '0'))
SET p.sort_slot_id = slot.id,
    p.sort_slot_scan_code = slot.scan_code,
    p.sort_code = slot.slot_code;

UPDATE wms_sort_slot slot
INNER JOIN tmp_active_sort_binding binding
        ON binding.warehouse_id = slot.warehouse_id
       AND slot.slot_code = CONCAT('B', LPAD(binding.slot_no, 2, '0'))
SET slot.slot_status = CASE
        WHEN binding.pack_status = 'PACKED' THEN 'PACKING'
        WHEN binding.sort_status = 'SORTED' THEN 'READY'
        ELSE 'SORTING'
    END,
    slot.task_id = binding.task_id,
    slot.outbound_order_id = binding.outbound_order_id,
    slot.package_id = binding.package_id,
    slot.reserved_time = NOW(),
    slot.occupied_time = NOW(),
    slot.ready_time = CASE WHEN binding.sort_status = 'SORTED' THEN NOW() ELSE NULL END,
    slot.packing_time = CASE WHEN binding.pack_status = 'PACKED' THEN NOW() ELSE NULL END;

DROP TEMPORARY TABLE tmp_active_sort_binding;

CREATE INDEX idx_outbound_package_sort_slot
    ON wms_sales_outbound_package (sort_slot_id);
