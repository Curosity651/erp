-- Picking tasks are warehouse-level work batches. Order and reservation rows
-- retain owner/provider ownership; the task header no longer represents one.
ALTER TABLE wms_fulfillment_pick_task
    MODIFY COLUMN wms_tenant_id BIGINT NULL,
    MODIFY COLUMN erp_tenant_id BIGINT NULL;

DROP INDEX idx_fulfillment_pick_scope ON wms_fulfillment_pick_task;
CREATE INDEX idx_fulfillment_pick_scope
    ON wms_fulfillment_pick_task (warehouse_id, task_status);
