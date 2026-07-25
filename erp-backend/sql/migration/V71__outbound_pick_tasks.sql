CREATE TABLE IF NOT EXISTS `wms_outbound_pick_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_no` varchar(40) NOT NULL COMMENT '拣货任务号',
  `platform_tenant_id` bigint NOT NULL COMMENT '海外仓平台租户ID',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `erp_tenant_id` bigint NOT NULL COMMENT '货主ID',
  `source_type` varchar(20) NOT NULL COMMENT 'SALES/CUSTOM',
  `task_type` varchar(20) NOT NULL COMMENT 'SINGLE/WAVE',
  `task_status` varchar(20) NOT NULL COMMENT 'PICKING/COMPLETED/CANCELLED',
  `picker_id` bigint NOT NULL COMMENT '拣货员ID',
  `picker_name` varchar(100) DEFAULT NULL COMMENT '拣货员姓名',
  `order_count` int NOT NULL DEFAULT 0,
  `sku_count` int NOT NULL DEFAULT 0,
  `total_quantity` int NOT NULL DEFAULT 0,
  `whole_pallet_count` int NOT NULL DEFAULT 0,
  `secondary_order_count` int NOT NULL DEFAULT 0,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pick_task_no` (`task_no`),
  KEY `idx_pick_task_scope` (`platform_tenant_id`,`warehouse_id`,`erp_tenant_id`,`task_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='海外仓出库拣货任务/波次';

CREATE TABLE IF NOT EXISTS `wms_outbound_pick_task_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `outbound_order_id` bigint NOT NULL,
  `outbound_no` varchar(64) NOT NULL,
  `tote_no` varchar(32) DEFAULT NULL COMMENT '二次分拣周转筐号',
  `sort_required` tinyint NOT NULL DEFAULT 0,
  `sort_status` varchar(20) NOT NULL DEFAULT 'NOT_REQUIRED',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pick_task_order` (`task_id`,`outbound_order_id`),
  KEY `idx_pick_task_order_order` (`outbound_order_id`),
  CONSTRAINT `fk_pick_task_order_task` FOREIGN KEY (`task_id`) REFERENCES `wms_outbound_pick_task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拣货任务关联出库单';

CREATE TABLE IF NOT EXISTS `wms_outbound_pick_task_line` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `physical_inventory_id` bigint NOT NULL,
  `pallet_id` bigint DEFAULT NULL,
  `pallet_no` varchar(64) DEFAULT NULL,
  `slot_code` varchar(64) DEFAULT NULL,
  `location_code` varchar(64) DEFAULT NULL,
  `sku_code` varchar(100) NOT NULL,
  `inbound_date` date DEFAULT NULL,
  `pick_order` int DEFAULT NULL,
  `planned_qty` int NOT NULL,
  `picked_qty` int NOT NULL DEFAULT 0,
  `pick_strategy` varchar(20) NOT NULL COMMENT 'WHOLE_PALLET/PIECE',
  `line_status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pick_task_inventory` (`task_id`,`physical_inventory_id`),
  KEY `idx_pick_task_line_task` (`task_id`,`line_status`),
  CONSTRAINT `fk_pick_task_line_task` FOREIGN KEY (`task_id`) REFERENCES `wms_outbound_pick_task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拣货任务汇总取货行';

