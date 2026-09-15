CREATE TABLE IF NOT EXISTS `wms_fulfillment_pick_package` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(40) NOT NULL,
  `task_id` bigint NOT NULL,
  `snapshot_hash` char(64) NOT NULL,
  `warehouse_file_name` varchar(255) DEFAULT NULL,
  `warehouse_object_key` varchar(500) DEFAULT NULL,
  `warehouse_sha256` char(64) DEFAULT NULL,
  `archive_file_name` varchar(255) DEFAULT NULL,
  `archive_object_key` varchar(500) DEFAULT NULL,
  `archive_sha256` char(64) DEFAULT NULL,
  `order_count` int NOT NULL DEFAULT 0,
  `total_quantity` int NOT NULL DEFAULT 0,
  `package_status` varchar(20) NOT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pick_package_batch_no` (`batch_no`),
  KEY `idx_pick_package_snapshot` (`task_id`, `snapshot_hash`, `package_status`),
  CONSTRAINT `fk_pick_package_task` FOREIGN KEY (`task_id`)
    REFERENCES `wms_fulfillment_pick_task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拣货任务双文件包生成记录';
