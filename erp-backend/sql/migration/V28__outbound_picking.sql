-- V28 海外仓平台出库作业·下架(拣货)。
-- 在既有销售出库单 wms_sales_outbound_order 上扩展平台作业字段(不改货主侧下单逻辑)，
-- 并新建 FIFO 分配(拣货单明细)表；给出库作业菜单(下架170503/打包签出170504)补平台权限码。
-- 状态口径：货主确认后为 CONFIRMED(平台视角=待下架 PENDING)；平台作业新增 PICKING/BACKORDER/PACKED/SHIPPED/COMPLETED。

ALTER TABLE wms_sales_outbound_order
    ADD COLUMN pick_mode   VARCHAR(20) NULL COMMENT '下架模式 CENTRALIZED/BY_ORDER/SECONDARY',
    ADD COLUMN picker_id   BIGINT      NULL COMMENT '拣货员用户ID',
    ADD COLUMN picker_name  VARCHAR(64) NULL COMMENT '拣货员姓名';

CREATE TABLE wms_outbound_pick_allocation (
    id                    BIGINT      NOT NULL AUTO_INCREMENT,
    outbound_order_id     BIGINT      NOT NULL COMMENT '出库单ID',
    physical_inventory_id BIGINT      NOT NULL COMMENT '批次ID(wms_physical_inventory.id)',
    sku_code              VARCHAR(64) NOT NULL COMMENT 'SKU编码',
    location_code         VARCHAR(50) NULL COMMENT '库位编码',
    inbound_date          DATE        NULL COMMENT '批次入库日(FIFO)',
    pick_order            INT         NULL COMMENT '批次同日次序',
    take_qty              INT         NOT NULL COMMENT '从该批次取货数(锁定量)',
    create_time           DATETIME    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_pick_alloc_order (outbound_order_id),
    KEY idx_pick_alloc_batch (physical_inventory_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '出库下架FIFO分配(拣货单明细)';

UPDATE sys_menu SET permission = 'wms:outbound-exec:oper' WHERE id IN (170503, 170504);
