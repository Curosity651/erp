CREATE TABLE IF NOT EXISTS wms_fulfillment_pick_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL,
  warehouse_id BIGINT NOT NULL,
  wms_tenant_id BIGINT NOT NULL,
  erp_tenant_id BIGINT NOT NULL,
  task_status VARCHAR(24) NOT NULL,
  order_count INT NOT NULL DEFAULT 0,
  total_quantity INT NOT NULL DEFAULT 0,
  operator_id BIGINT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fulfillment_pick_task_no (task_no),
  KEY idx_fulfillment_pick_scope (warehouse_id, erp_tenant_id, task_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wms_fulfillment_pick_task_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  fulfillment_order_id BIGINT NOT NULL,
  sequence_no INT NOT NULL,
  order_status VARCHAR(24) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fulfillment_pick_order (fulfillment_order_id),
  UNIQUE KEY uk_fulfillment_pick_sequence (task_id, sequence_no),
  KEY idx_fulfillment_pick_task_order (task_id, order_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wms_fulfillment_pick_task_line (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  fulfillment_order_id BIGINT NOT NULL,
  reservation_id BIGINT NOT NULL,
  inventory_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  location_code VARCHAR(64) NOT NULL,
  fulfillment_item_id BIGINT NOT NULL,
  sku_code VARCHAR(128) NOT NULL,
  warehouse_sku_code VARCHAR(160) NOT NULL,
  sequence_no INT NOT NULL,
  planned_quantity INT NOT NULL,
  picked_quantity INT NOT NULL DEFAULT 0,
  line_status VARCHAR(24) NOT NULL,
  version INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fulfillment_pick_reservation (reservation_id),
  KEY idx_fulfillment_pick_line_task (task_id, fulfillment_order_id, sequence_no),
  KEY idx_fulfillment_pick_scan (task_id, location_code, warehouse_sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
