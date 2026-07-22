CREATE TABLE IF NOT EXISTS wms_fbo_inventory_snapshot (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL COMMENT 'ERP owner tenant id',
  platform VARCHAR(32) NOT NULL,
  shop_id BIGINT NOT NULL,
  platform_warehouse_id VARCHAR(128) NOT NULL,
  platform_warehouse_name VARCHAR(255) DEFAULT NULL,
  platform_item_id VARCHAR(128) NOT NULL,
  sku_code VARCHAR(100) NOT NULL,
  quantity INT NOT NULL DEFAULT 0,
  sync_batch_no VARCHAR(64) NOT NULL,
  synced_at DATETIME NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fbo_snapshot_source
    (tenant_id, platform, shop_id, platform_warehouse_id, sku_code),
  KEY idx_fbo_snapshot_tenant_sku (tenant_id, sku_code),
  KEY idx_fbo_snapshot_tenant_shop (tenant_id, platform, shop_id),
  KEY idx_fbo_snapshot_synced_at (tenant_id, synced_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Owner-side read-only marketplace FBO inventory snapshot';

ALTER TABLE wms_fbo_sync_log
  ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0 AFTER id,
  ADD COLUMN sync_batch_no VARCHAR(64) DEFAULT NULL AFTER sync_type,
  ADD COLUMN source_count INT NOT NULL DEFAULT 0 AFTER total_count,
  ADD COLUMN snapshot_quantity INT NOT NULL DEFAULT 0 AFTER unmapped_count,
  ADD KEY idx_fbo_log_tenant_time (tenant_id, sync_time),
  ADD KEY idx_fbo_log_tenant_shop (tenant_id, shop_id);

UPDATE wms_fbo_sync_log l
JOIN shop s ON s.id = l.shop_id
SET l.tenant_id = s.tenant_id
WHERE l.tenant_id = 0;

-- Replace the legacy platform-owned permission with owner-only permissions.
UPDATE sys_menu
SET permission = 'wms:fbo-inventory:sync', title = 'FBO inventory sync'
WHERE id = 161004;

INSERT INTO sys_menu
  (id, parent_id, title, permission, path, target_type, uri, sort, keep_alive, hidden, type, deleted)
VALUES
  (161006, 161000, 'FBO inventory read', 'wms:fbo-inventory:read', NULL, 1, '', 6, 0, 0, 2, 0),
  (161007, 161000, 'FBO sync log', 'wms:fbo-inventory:log', NULL, 1, '', 7, 0, 0, 2, 0)
ON DUPLICATE KEY UPDATE
  permission = VALUES(permission), title = VALUES(title), deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_code, menu_id)
SELECT DISTINCT role_code, 161006 FROM sys_role_menu WHERE menu_id = 161004;
INSERT IGNORE INTO sys_role_menu (role_code, menu_id)
SELECT DISTINCT role_code, 161007 FROM sys_role_menu WHERE menu_id = 161004;

-- Overseas-platform roles must not own the FBO sync permission.
DELETE rm FROM sys_role_menu rm
JOIN sys_role r ON r.code = rm.role_code
WHERE rm.menu_id IN (160104, 161004, 161006, 161007)
  AND r.tenant_id = -1;
