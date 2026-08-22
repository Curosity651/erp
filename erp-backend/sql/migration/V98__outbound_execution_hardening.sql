ALTER TABLE wms_outbound_pick_task_line
    ADD COLUMN return_required_qty INT NOT NULL DEFAULT 0 AFTER shortage_qty,
    ADD COLUMN returned_qty INT NOT NULL DEFAULT 0 AFTER return_required_qty,
    ADD COLUMN picked_by BIGINT NULL AFTER line_status,
    ADD COLUMN picked_by_name VARCHAR(100) NULL AFTER picked_by,
    ADD COLUMN pick_time DATETIME NULL AFTER picked_by_name,
    ADD COLUMN return_by BIGINT NULL AFTER pick_time,
    ADD COLUMN return_by_name VARCHAR(100) NULL AFTER return_by,
    ADD COLUMN return_time DATETIME NULL AFTER return_by_name;

ALTER TABLE wms_sales_outbound_package
    ADD COLUMN sort_by_id BIGINT NULL AFTER sort_status,
    ADD COLUMN packer_id BIGINT NULL AFTER pack_status;

ALTER TABLE wms_sales_outbound_order
    ADD COLUMN shipped_by BIGINT NULL AFTER tracking_no,
    ADD COLUMN shipped_by_name VARCHAR(100) NULL AFTER shipped_by,
    ADD COLUMN shipped_time DATETIME NULL AFTER shipped_by_name;

CREATE INDEX idx_pick_task_status_warehouse
    ON wms_outbound_pick_task (task_status, warehouse_id, platform_tenant_id);

CREATE INDEX idx_pick_line_task_return
    ON wms_outbound_pick_task_line (task_id, line_status, return_required_qty, returned_qty);

