-- =====================================================================
-- 库位调整单（库内移库单）：单据化改造。
--   平台新建调整单(PENDING 待调整，不动库存) → 「调整完成」逐条移库(COMPLETED，留痕) → 执行前可 CANCELLED。
--   新增两张表 + wms:location:read 查看权限（授平台角色）。
-- 幂等：建表 IF NOT EXISTS；权限 NOT EXISTS 去重。
-- =====================================================================

-- 库位调整单主表
CREATE TABLE IF NOT EXISTS wms_location_transfer_order (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    transfer_no    VARCHAR(32)  NOT NULL COMMENT '调整单号 LT+yyyyMMdd+4位序号',
    warehouse_id   BIGINT       NULL COMMENT '仓库ID',
    erp_tenant_id  BIGINT       NULL COMMENT '货主（货物归属）',
    order_status   VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/COMPLETED/CANCELLED',
    remark         VARCHAR(500) NULL COMMENT '备注',
    complete_time  DATETIME     NULL COMMENT '调整完成时间',
    complete_by    BIGINT       NULL COMMENT '调整完成操作人',
    create_by      BIGINT       NULL COMMENT '创建人',
    create_time    DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        BIGINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_lt_transfer_no (transfer_no),
    KEY idx_lt_erp_tenant (erp_tenant_id),
    KEY idx_lt_warehouse (warehouse_id),
    KEY idx_lt_status (order_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库位调整单主表';

-- 库位调整单明细表
CREATE TABLE IF NOT EXISTS wms_location_transfer_item (
    id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    transfer_order_id     BIGINT       NOT NULL COMMENT '库位调整单ID',
    erp_tenant_id         BIGINT       NULL COMMENT '货主（冗余到行）',
    sku_code              VARCHAR(64)  NULL COMMENT 'SKU编码',
    physical_inventory_id BIGINT       NULL COMMENT '源批次ID wms_physical_inventory.id',
    source_location_code  VARCHAR(64)  NULL COMMENT '源库位编码',
    source_quality        VARCHAR(16)  NULL COMMENT '源品质 GOOD/DAMAGED',
    target_location_code  VARCHAR(64)  NULL COMMENT '目标库位编码',
    target_zone_id        BIGINT       NULL COMMENT '目标库位分区ID',
    quantity              INT          NULL COMMENT '移动数量',
    to_good               TINYINT      NOT NULL DEFAULT 0 COMMENT '是否落库置良品 1=是',
    remark                VARCHAR(500) NULL COMMENT '备注',
    create_time           DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_lti_order (transfer_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库位调整单明细表';

-- 查看权限（列表/详情/候选）
INSERT INTO sys_menu (id, parent_id, title, permission, path, uri, type, sort, target_type, keep_alive, hidden, deleted)
SELECT 160912, 160910, '查看', 'wms:location:read', NULL, NULL, 2, 2, 1, 0, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 160912);

-- 授权：平台角色 → 查看权限
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT 'ROLE_OVERSEAS_PLATFORM_ADMIN', m.id
FROM (SELECT 160912 id) m
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu x WHERE x.role_code = 'ROLE_OVERSEAS_PLATFORM_ADMIN' AND x.menu_id = m.id
);
