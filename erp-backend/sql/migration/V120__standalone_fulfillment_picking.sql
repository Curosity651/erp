-- Standalone picking tasks are claimed by the warehouse employee who performs
-- the work. Historical in-progress tasks keep their existing operator.
ALTER TABLE wms_fulfillment_pick_task
    ADD COLUMN claimed_time DATETIME NULL AFTER operator_id;

ALTER TABLE wms_fulfillment_pick_task_order
    ADD COLUMN previous_order_status VARCHAR(32) NULL AFTER order_status,
    ADD COLUMN previous_fulfillment_status VARCHAR(32) NULL AFTER previous_order_status,
    ADD COLUMN exception_type VARCHAR(32) NULL AFTER previous_fulfillment_status,
    ADD COLUMN exception_reason VARCHAR(500) NULL AFTER exception_type,
    ADD COLUMN exception_image_urls TEXT NULL AFTER exception_reason,
    ADD COLUMN started_time DATETIME NULL AFTER exception_image_urls,
    ADD COLUMN completed_time DATETIME NULL AFTER started_time;

UPDATE wms_fulfillment_pick_task
SET claimed_time = COALESCE(claimed_time, create_time)
WHERE task_status = 'PICKING'
  AND operator_id IS NOT NULL;

CREATE INDEX idx_pick_task_operator_status
    ON wms_fulfillment_pick_task (operator_id, task_status);
