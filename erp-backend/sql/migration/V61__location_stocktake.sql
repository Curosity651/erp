-- Location-based stocktake foundation. Existing confirmed orders remain readable as legacy records.

ALTER TABLE wms_stocktake_order
    ADD COLUMN stocktake_mode varchar(20) NULL COMMENT 'FULL/CYCLE/SPECIAL' AFTER stocktake_scope,
    ADD COLUMN scope_config varchar(2000) NULL COMMENT 'JSON scope snapshot' AFTER stocktake_mode,
    ADD COLUMN blind_count tinyint NOT NULL DEFAULT 1 COMMENT '1 blind count' AFTER scope_config,
    ADD COLUMN freeze_mode varchar(20) NULL COMMENT 'WAREHOUSE/LOCATION/ITEM' AFTER blind_count,
    ADD COLUMN snapshot_time datetime NULL COMMENT 'physical inventory snapshot time' AFTER freeze_mode;

CREATE TABLE wms_stocktake_location_task (
    id bigint NOT NULL AUTO_INCREMENT,
    stocktake_order_id bigint NOT NULL,
    warehouse_id bigint NOT NULL,
    zone_id bigint NULL,
    location_id bigint NOT NULL,
    location_code varchar(50) NOT NULL,
    task_status varchar(20) NOT NULL DEFAULT 'PENDING',
    assignee_id bigint NULL,
    assignee_name varchar(64) NULL,
    started_time datetime NULL,
    completed_time datetime NULL,
    location_confirmed tinyint NOT NULL DEFAULT 0,
    version int NOT NULL DEFAULT 0,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stocktake_location (stocktake_order_id, location_id),
    KEY idx_stocktake_task_status (stocktake_order_id, task_status),
    KEY idx_stocktake_task_location (warehouse_id, location_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='stocktake location task';

ALTER TABLE wms_stocktake_order_item
    ADD COLUMN location_task_id bigint NULL AFTER stocktake_order_id,
    ADD COLUMN physical_inventory_id bigint NULL AFTER location_task_id,
    ADD COLUMN zone_id bigint NULL AFTER sku_code,
    ADD COLUMN location_code varchar(50) NULL AFTER zone_id,
    ADD COLUMN quality varchar(20) NOT NULL DEFAULT 'GOOD' AFTER location_code,
    ADD COLUMN allocatable tinyint NOT NULL DEFAULT 1 AFTER quality,
    ADD COLUMN inbound_date date NULL AFTER allocatable,
    ADD COLUMN pick_order int NULL AFTER inbound_date,
    ADD COLUMN reserved_quantity int NOT NULL DEFAULT 0 AFTER system_quantity,
    ADD COLUMN review_status varchar(20) NULL AFTER stocktake_status,
    ADD KEY idx_stocktake_item_task (location_task_id),
    ADD KEY idx_stocktake_item_physical (physical_inventory_id),
    ADD KEY idx_stocktake_item_location (stocktake_order_id, location_code);
