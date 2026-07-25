-- Warehouse execution verification: SKU barcodes, actual picking, sorting and packing.

CREATE TABLE IF NOT EXISTS sku_barcode (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id    BIGINT       NOT NULL COMMENT 'ERP owner tenant',
    sku_id       BIGINT       NOT NULL COMMENT 'sku.id',
    sku_code     VARCHAR(100) NOT NULL,
    barcode      VARCHAR(128) NOT NULL,
    barcode_type VARCHAR(20)  NOT NULL DEFAULT 'EAN_UPC',
    primary_flag TINYINT      NOT NULL DEFAULT 0,
    enabled      TINYINT      NOT NULL DEFAULT 1,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_barcode_tenant_code (tenant_id, barcode),
    KEY idx_sku_barcode_sku (tenant_id, sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP SKU scannable barcode';

ALTER TABLE wms_outbound_pick_task
    ADD COLUMN exception_line_id BIGINT DEFAULT NULL COMMENT 'current exception line' AFTER secondary_order_count,
    ADD COLUMN exception_reason VARCHAR(500) DEFAULT NULL COMMENT 'current exception reason' AFTER exception_line_id,
    ADD COLUMN exception_time DATETIME DEFAULT NULL COMMENT 'exception reported time' AFTER exception_reason;

ALTER TABLE wms_outbound_pick_task_line
    ADD COLUMN shortage_qty INT NOT NULL DEFAULT 0 COMMENT 'reported missing quantity' AFTER picked_qty,
    ADD COLUMN exception_reason VARCHAR(500) DEFAULT NULL AFTER shortage_qty,
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER line_status;

ALTER TABLE wms_sales_outbound_order_item
    ADD COLUMN packed_quantity INT NOT NULL DEFAULT 0 COMMENT 'package scan verified quantity' AFTER sorted_quantity;

ALTER TABLE wms_sales_outbound_package
    ADD COLUMN handover_required TINYINT NOT NULL DEFAULT 0 COMMENT 'platform handover document required' AFTER label_status,
    ADD COLUMN handover_status VARCHAR(24) NOT NULL DEFAULT 'NOT_REQUIRED'
        COMMENT 'NOT_REQUIRED/PENDING/READY/EXTERNAL_CONFIRMED' AFTER handover_required,
    ADD COLUMN sort_by VARCHAR(64) DEFAULT NULL AFTER sort_status,
    ADD COLUMN sort_time DATETIME DEFAULT NULL AFTER sort_by;

UPDATE wms_sales_outbound_package p
JOIN erp_order eo ON eo.id = p.erp_order_id
JOIN ozon_delivery_method_rule r
  ON r.tenant_id = p.erp_tenant_id
 AND r.shop_id = eo.shop_id
 AND r.delivery_method_id = eo.delivery_method_id
 AND r.enabled = 1
 AND r.act_required = 1
SET p.handover_required = 1,
    p.handover_status = 'PENDING'
WHERE p.platform = 'ozon';

CREATE TABLE IF NOT EXISTS wms_outbound_scan_event (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    task_id     BIGINT       DEFAULT NULL,
    task_line_id BIGINT      DEFAULT NULL,
    package_id  BIGINT       DEFAULT NULL,
    stage       VARCHAR(16)  NOT NULL COMMENT 'PICK/SORT/PACK',
    event_type  VARCHAR(24)  NOT NULL COMMENT 'SCAN/MANUAL/EXCEPTION/RESOLVE',
    scan_code   VARCHAR(128) DEFAULT NULL,
    sku_code    VARCHAR(100) DEFAULT NULL,
    quantity    INT          NOT NULL DEFAULT 0,
    operator_id BIGINT       DEFAULT NULL,
    operator_name VARCHAR(100) DEFAULT NULL,
    remark      VARCHAR(500) DEFAULT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_outbound_scan_task (task_id, create_time),
    KEY idx_outbound_scan_package (package_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Outbound warehouse scan audit event';

-- Existing packages only need sorting when their task contains multiple packages.
-- Task creation will recalculate this for new work. Keep historical status unchanged.
