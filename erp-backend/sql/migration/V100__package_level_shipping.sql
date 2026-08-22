ALTER TABLE wms_sales_outbound_package
    ADD COLUMN ship_status VARCHAR(24) NOT NULL DEFAULT 'PENDING' AFTER pack_time,
    ADD COLUMN channel_code VARCHAR(64) NULL AFTER ship_status,
    ADD COLUMN channel_name VARCHAR(100) NULL AFTER channel_code,
    ADD COLUMN tracking_no VARCHAR(128) NULL AFTER channel_name,
    ADD COLUMN weight DECIMAL(12, 3) NULL AFTER tracking_no,
    ADD COLUMN shipped_by BIGINT NULL AFTER weight,
    ADD COLUMN shipped_by_name VARCHAR(100) NULL AFTER shipped_by,
    ADD COLUMN shipped_time DATETIME NULL AFTER shipped_by_name;

ALTER TABLE wms_outbound_pick_allocation
    ADD COLUMN shipped_qty INT NOT NULL DEFAULT 0 AFTER take_qty;

UPDATE wms_sales_outbound_package p
INNER JOIN wms_sales_outbound_order o ON o.id = p.outbound_order_id
SET p.ship_status = 'SHIPPED',
    p.channel_name = o.channel_name,
    p.tracking_no = o.tracking_no,
    p.weight = o.weight,
    p.shipped_by = o.shipped_by,
    p.shipped_by_name = o.shipped_by_name,
    p.shipped_time = o.shipped_time
WHERE o.order_status IN ('SHIPPED', 'COMPLETED');

UPDATE wms_outbound_pick_allocation a
INNER JOIN wms_sales_outbound_order o ON o.id = a.outbound_order_id
SET a.shipped_qty = a.take_qty
WHERE o.order_status IN ('SHIPPED', 'COMPLETED');

CREATE INDEX idx_outbound_package_work
    ON wms_sales_outbound_package (ship_status, pack_status, erp_tenant_id, update_time);

CREATE INDEX idx_outbound_package_platform_order
    ON wms_sales_outbound_package (platform_order_id);

