ALTER TABLE wms_fulfillment_pick_task_line
  ADD COLUMN returned_quantity INT NOT NULL DEFAULT 0 AFTER picked_quantity;
