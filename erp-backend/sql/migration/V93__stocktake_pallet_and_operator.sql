ALTER TABLE wms_stocktake_order_item
    ADD COLUMN capacity_percent decimal(6,2) NULL COMMENT '盘点账外货物确认后的托盘利用率' AFTER slot_id,
    ADD COLUMN manual_full tinyint NOT NULL DEFAULT 0 COMMENT '账外货物是否人工满托' AFTER capacity_percent;

ALTER TABLE wms_stocktake_location_task
    ADD KEY idx_stocktake_active_location (warehouse_id, location_code, task_status);
