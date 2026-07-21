-- Pallet storage and three independently accessible levels for every physical base location.
-- The existing wms_location remains a two-dimensional warehouse coordinate.

ALTER TABLE wms_warehouse
    ADD COLUMN pallet_levels int NOT NULL DEFAULT 3 COMMENT 'pallet levels per base location',
    ADD COLUMN max_sku_kinds_per_pallet int NOT NULL DEFAULT 4 COMMENT 'maximum distinct owner+SKU kinds',
    ADD COLUMN allow_cross_owner_mix tinyint NOT NULL DEFAULT 1 COMMENT 'allow mixed-owner pallet',
    ADD COLUMN default_pallet_length_mm int NOT NULL DEFAULT 1200 COMMENT 'default pallet length',
    ADD COLUMN default_pallet_width_mm int NOT NULL DEFAULT 1000 COMMENT 'default pallet width',
    ADD COLUMN default_pallet_height_mm int NOT NULL DEFAULT 1600 COMMENT 'usable pallet height',
    ADD COLUMN default_pallet_max_weight_kg decimal(10,2) NOT NULL DEFAULT 1000.00 COMMENT 'safe payload',
    ADD COLUMN default_pallet_utilization decimal(5,4) NOT NULL DEFAULT 0.8500 COMMENT 'volume utilization';

CREATE TABLE IF NOT EXISTS wms_location_slot (
    id bigint NOT NULL AUTO_INCREMENT,
    warehouse_id bigint NOT NULL,
    location_id bigint NOT NULL COMMENT 'two-dimensional base location',
    level_no tinyint NOT NULL COMMENT 'L1 bottom, L3 top',
    slot_code varchar(64) NOT NULL COMMENT 'A10-03-L1',
    max_height_mm int NULL,
    max_weight_kg decimal(10,2) NULL,
    slot_status varchar(20) NOT NULL DEFAULT 'EMPTY' COMMENT 'EMPTY/OCCUPIED/LOCKED',
    version int NOT NULL DEFAULT 0,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_location_level (location_id, level_no),
    UNIQUE KEY uk_warehouse_slot (warehouse_id, slot_code),
    KEY idx_slot_status (warehouse_id, slot_status, level_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='three-level pallet slot';

CREATE TABLE IF NOT EXISTS wms_pallet (
    id bigint NOT NULL AUTO_INCREMENT,
    pallet_no varchar(80) NOT NULL,
    warehouse_id bigint NOT NULL,
    slot_id bigint NULL COMMENT 'last/history slot',
    current_slot_id bigint NULL COMMENT 'active slot; NULL after close/ship',
    slot_code varchar(64) NULL,
    pallet_type varchar(30) NOT NULL DEFAULT 'SINGLE_PARTIAL' COMMENT 'SINGLE_FULL/SINGLE_PARTIAL/MIXED',
    pallet_status varchar(20) NOT NULL DEFAULT 'PARTIAL' COMMENT 'PARTIAL/FULL/ALLOCATED/PICKING/SHIPPED/CLOSED/LOCKED',
    capacity_percent decimal(6,2) NULL,
    capacity_source varchar(30) NOT NULL DEFAULT 'MANUAL_REQUIRED' COMMENT 'SKU_QTY/VOLUME_WEIGHT/MANUAL/MANUAL_REQUIRED/UNCONFIRMED',
    estimated_volume_cbm decimal(12,6) NULL,
    estimated_weight_kg decimal(12,3) NULL,
    actual_weight_kg decimal(12,3) NULL,
    sku_kind_count int NOT NULL DEFAULT 0,
    whole_pallet_eligible tinyint NOT NULL DEFAULT 0,
    locked_reason varchar(255) NULL,
    version int NOT NULL DEFAULT 0,
    create_by bigint NULL,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by bigint NULL,
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pallet_no (pallet_no),
    UNIQUE KEY uk_active_slot (current_slot_id),
    KEY idx_pallet_warehouse_status (warehouse_id, pallet_status),
    KEY idx_pallet_slot_history (slot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='pallet lifecycle';

CREATE TABLE IF NOT EXISTS wms_pallet_operation_log (
    id bigint NOT NULL AUTO_INCREMENT,
    pallet_id bigint NOT NULL,
    operation_type varchar(30) NOT NULL,
    from_slot_code varchar(64) NULL,
    to_slot_code varchar(64) NULL,
    capacity_percent decimal(6,2) NULL,
    remark varchar(500) NULL,
    operator_id bigint NULL,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_pallet_operation (pallet_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='pallet audit trail';

ALTER TABLE wms_physical_inventory
    ADD COLUMN pallet_id bigint NULL AFTER location_code,
    ADD COLUMN slot_id bigint NULL AFTER pallet_id,
    ADD KEY idx_physical_pallet (pallet_id),
    ADD KEY idx_physical_slot (slot_id);

ALTER TABLE wms_stocktake_order_item
    ADD COLUMN pallet_id bigint NULL AFTER physical_inventory_id,
    ADD COLUMN slot_id bigint NULL AFTER pallet_id,
    ADD COLUMN slot_code varchar(64) NULL AFTER location_code;

-- Materialize three levels for every existing physical location.
INSERT IGNORE INTO wms_location_slot
    (warehouse_id, location_id, level_no, slot_code, max_height_mm, max_weight_kg, slot_status)
SELECT l.warehouse_id, l.id, levels.level_no,
       CONCAT(l.location_code, '-L', levels.level_no),
       w.default_pallet_height_mm, w.default_pallet_max_weight_kg, 'EMPTY'
FROM wms_location l
JOIN wms_warehouse w ON w.id = l.warehouse_id
JOIN (SELECT 1 level_no UNION ALL SELECT 2 UNION ALL SELECT 3) levels
WHERE l.deleted = 0 AND COALESCE(l.is_virtual, 0) = 0;

-- Put every occupied legacy base location on one historical L1 pallet. The operator can calibrate it later.
INSERT IGNORE INTO wms_pallet
    (pallet_no, warehouse_id, slot_id, current_slot_id, slot_code, pallet_type, pallet_status,
     capacity_percent, capacity_source, sku_kind_count, whole_pallet_eligible)
SELECT CONCAT('LEG-', pi.warehouse_id, '-', REPLACE(pi.location_code, '-', '')),
       pi.warehouse_id, s.id, s.id, s.slot_code,
       CASE WHEN COUNT(DISTINCT CONCAT(pi.erp_tenant_id, '|', pi.sku_code)) > 1 THEN 'MIXED' ELSE 'SINGLE_PARTIAL' END,
       'PARTIAL', NULL, 'UNCONFIRMED',
       COUNT(DISTINCT CONCAT(pi.erp_tenant_id, '|', pi.sku_code)), 0
FROM wms_physical_inventory pi
JOIN wms_location l ON l.warehouse_id = pi.warehouse_id AND l.location_code = pi.location_code AND l.deleted = 0
JOIN wms_location_slot s ON s.location_id = l.id AND s.level_no = 1
WHERE pi.quantity > 0 AND pi.pallet_id IS NULL
GROUP BY pi.warehouse_id, pi.location_code, s.id, s.slot_code;

UPDATE wms_physical_inventory pi
JOIN wms_pallet p ON p.pallet_no = CONCAT('LEG-', pi.warehouse_id, '-', REPLACE(pi.location_code, '-', ''))
SET pi.pallet_id = p.id, pi.slot_id = p.current_slot_id
WHERE pi.quantity > 0 AND pi.pallet_id IS NULL;

UPDATE wms_location_slot s
JOIN wms_pallet p ON p.current_slot_id = s.id
SET s.slot_status = 'OCCUPIED';

-- Pallet management under overseas warehouse operations.
INSERT IGNORE INTO sys_menu
    (id, parent_id, title, icon, path, uri, sort, type, permission, hidden, target_type, create_time, update_time, deleted)
VALUES
    (170512, 170500, '托盘管理', 'inbox', 'pallets', 'platform/pallet/PalletPage', 3, 1,
     'wms:inbound-exec:oper', 0, 1, NOW(), NOW(), 0);

INSERT IGNORE INTO sys_role_menu (role_code, menu_id)
SELECT DISTINCT role_code, 170512
FROM sys_role_menu
WHERE menu_id = 170500;
