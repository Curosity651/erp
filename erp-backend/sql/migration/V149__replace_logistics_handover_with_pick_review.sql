-- Remove the abandoned logistics-style outbound handover design.
DROP TABLE IF EXISTS wms_outbound_handover_order;
DROP TABLE IF EXISTS wms_transport_expense_record;

SET @drop_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'wms_fulfillment_order' AND index_name = 'idx_fulfillment_handover_page'),
  'ALTER TABLE wms_fulfillment_order DROP INDEX idx_fulfillment_handover_page', 'SELECT 1');
PREPARE stmt FROM @drop_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @drop_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'wms_fulfillment_order' AND index_name = 'idx_fulfillment_handover_by'),
  'ALTER TABLE wms_fulfillment_order DROP INDEX idx_fulfillment_handover_by', 'SELECT 1');
PREPARE stmt FROM @drop_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @columns = 'handover_status,vehicle_plate,driver_name,driver_phone,departure_time,handover_destination,recorded_freight_cost,recorded_freight_currency,logistics_photo_file_ids,handover_remark,handover_time,handover_by';
SET @drop_columns = (
  SELECT GROUP_CONCAT(CONCAT('DROP COLUMN `', column_name, '`') ORDER BY ordinal_position SEPARATOR ', ')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'wms_fulfillment_order'
    AND FIND_IN_SET(column_name, @columns) > 0
);
SET @drop_sql = IF(@drop_columns IS NULL, 'SELECT 1', CONCAT('ALTER TABLE wms_fulfillment_order ', @drop_columns));
PREPARE stmt FROM @drop_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- One review record per platform/date/warehouse scope. warehouse_id=0 means all warehouses.
CREATE TABLE IF NOT EXISTS wms_outbound_pick_review (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id BIGINT NOT NULL COMMENT '海外仓平台租户',
  review_no VARCHAR(40) NOT NULL COMMENT '出库复核单号',
  work_date DATE NOT NULL COMMENT '复核的拣货任务日期',
  warehouse_id BIGINT NOT NULL DEFAULT 0 COMMENT '仓库ID，0表示全部仓库',
  review_status VARCHAR(24) NOT NULL DEFAULT 'COMPLETED' COMMENT 'COMPLETED',
  task_count INT NOT NULL DEFAULT 0 COMMENT '复核时任务总数',
  completed_count INT NOT NULL DEFAULT 0 COMMENT '复核时已完成任务数',
  cancelled_count INT NOT NULL DEFAULT 0 COMMENT '复核时已取消任务数',
  exception_count INT NOT NULL DEFAULT 0 COMMENT '复核时异常任务数',
  task_snapshot_time DATETIME NULL COMMENT '复核时任务最大更新时间，用于识别新增或变更',
  reviewed_by BIGINT NOT NULL COMMENT '复核人',
  reviewed_time DATETIME NOT NULL COMMENT '复核时间',
  remark VARCHAR(500) NULL COMMENT '复核备注',
  version INT NOT NULL DEFAULT 0 COMMENT '复核版本',
  create_by BIGINT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT NULL COMMENT '更新人',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_outbound_pick_review_no (review_no),
  UNIQUE KEY uk_outbound_pick_review_scope (tenant_id, work_date, warehouse_id),
  KEY idx_outbound_pick_review_date (tenant_id, work_date, review_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日出库拣货任务复核记录';
