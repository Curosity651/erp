-- =====================================================================
-- C1 · 仓库结构扩展 + 品质分区 + 库位
-- wms_warehouse 加结构参数；新建 wms_zone(品质分区)、wms_location(库位)。
-- 三者均为平台级基础设施，无租户列（隔离在 wms_physical_inventory/wms_rack_assignment 层）。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V7__warehouse_structure_and_locations.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- 1) 扩展 wms_warehouse：结构参数 + 编码规则 + 幂等标记（不动现有 FBO/platform 字段）
ALTER TABLE wms_warehouse
  ADD COLUMN rack_rows             INT         NOT NULL DEFAULT 0   COMMENT '排数(结构参数)'        AFTER contact_phone,
  ADD COLUMN rack_layers           INT         NOT NULL DEFAULT 0   COMMENT '每排层数'              AFTER rack_rows,
  ADD COLUMN rack_columns          INT         NOT NULL DEFAULT 0   COMMENT '每排列数'              AFTER rack_layers,
  ADD COLUMN rack_no_prefix        VARCHAR(10) NOT NULL DEFAULT ''  COMMENT '排号前缀(A→A1;空=纯数字)' AFTER rack_columns,
  ADD COLUMN code_pad_width        TINYINT     NOT NULL DEFAULT 2   COMMENT '层/列补零位宽(2→03)'   AFTER rack_no_prefix,
  ADD COLUMN default_location_type VARCHAR(20)          DEFAULT 'BIG' COMMENT '默认库位类型 BIG/SMALL' AFTER code_pad_width,
  ADD COLUMN location_generated    TINYINT     NOT NULL DEFAULT 0   COMMENT '库位已生成幂等标记'     AFTER default_location_type;

-- 2) 品质分区
CREATE TABLE IF NOT EXISTS wms_zone (
  id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分区ID',
  warehouse_id BIGINT      NOT NULL COMMENT '所属仓库ID',
  zone_type    VARCHAR(20) NOT NULL COMMENT '分区类型 STANDARD/DEFECTIVE/RETURN/TEMP',
  zone_name    VARCHAR(50) NOT NULL COMMENT '分区名称',
  allocatable  TINYINT     NOT NULL DEFAULT 1 COMMENT '可分配 1是/0否',
  create_by    BIGINT      DEFAULT NULL COMMENT '创建人',
  create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by    BIGINT      DEFAULT NULL COMMENT '更新人',
  update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      BIGINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  PRIMARY KEY (id),
  KEY idx_wh (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓内品质分区';

-- 3) 库位
CREATE TABLE IF NOT EXISTS wms_location (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '库位ID',
  warehouse_id   BIGINT       NOT NULL COMMENT '所属仓库ID',
  zone_id        BIGINT       DEFAULT NULL COMMENT '所属分区ID',
  rack_no        VARCHAR(20)  NOT NULL COMMENT '排号(如A1,分配/计费单位)',
  layer_no       INT          DEFAULT NULL COMMENT '层号(1开始)',
  column_no      INT          DEFAULT NULL COMMENT '列号(1开始)',
  location_code  VARCHAR(50)  NOT NULL COMMENT '库位编码 A1-03-03',
  location_type  VARCHAR(20)  DEFAULT NULL COMMENT '库位类型 BIG/SMALL',
  pick_type      VARCHAR(20)  DEFAULT 'PICK' COMMENT '拣货类型 PICK/STORE',
  max_volume_cbm DECIMAL(10,4) DEFAULT NULL COMMENT '容量上限(立方米)',
  max_weight_kg  DECIMAL(10,2) DEFAULT NULL COMMENT '承重上限(千克)',
  create_by      BIGINT       DEFAULT NULL COMMENT '创建人',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by      BIGINT       DEFAULT NULL COMMENT '更新人',
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted        BIGINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wh_loc (warehouse_id, location_code),
  KEY idx_wh_rack (warehouse_id, rack_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库位';
