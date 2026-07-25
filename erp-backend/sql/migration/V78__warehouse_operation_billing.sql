-- Warehouse operation rate cards and auditable charge ledger.

CREATE TABLE IF NOT EXISTS wms_fee_rate_card (
    id bigint NOT NULL AUTO_INCREMENT,
    wms_tenant_id bigint NOT NULL DEFAULT 0 COMMENT '0=platform default; otherwise operator override',
    fee_code varchar(40) NOT NULL,
    fee_name varchar(100) NOT NULL,
    fee_type varchar(20) NOT NULL COMMENT 'INBOUND/OUTBOUND/DELIVERY/RETURN/INSPECTION/DRIVER',
    billing_unit varchar(20) NOT NULL COMMENT 'CBM/PALLET/BOX/ITEM/RATE',
    unit_price decimal(14,4) NOT NULL,
    currency varchar(8) NOT NULL DEFAULT 'CNY',
    effective_from date NOT NULL,
    effective_to date NULL,
    status tinyint NOT NULL DEFAULT 1,
    remark varchar(500) NULL,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fee_rate_period (wms_tenant_id, fee_code, effective_from),
    KEY idx_fee_rate_lookup (fee_code, wms_tenant_id, status, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='warehouse operation rate card';

INSERT IGNORE INTO wms_fee_rate_card
    (wms_tenant_id, fee_code, fee_name, fee_type, billing_unit, unit_price, currency, effective_from, remark)
VALUES
    (0, 'INBOUND_CBM', '入库操作费', 'INBOUND', 'CBM', 50.0000, 'CNY', '2026-01-01', '收货、理货、上架'),
    (0, 'AFTER_HOURS_SURCHARGE', '客户原因加班附加费', 'INBOUND', 'RATE', 0.5000, 'CNY', '2026-01-01', '基础操作费的50%'),
    (0, 'OUTBOUND_FULL_PALLET', '整托出库费', 'OUTBOUND', 'PALLET', 80.0000, 'CNY', '2026-01-01', '完整LPN离库'),
    (0, 'OUTBOUND_LARGE_BOX', '大箱出库费', 'OUTBOUND', 'BOX', 10.0000, 'CNY', '2026-01-01', '一箱一计费件'),
    (0, 'OUTBOUND_SMALL_ITEM', '小件散货出库费', 'OUTBOUND', 'ITEM', 1.0000, 'CNY', '2026-01-01', '按件计费');

ALTER TABLE wms_billing_record
    ADD COLUMN warehouse_id bigint NULL AFTER erp_tenant_id,
    ADD COLUMN source_type varchar(30) NULL AFTER fee_type,
    ADD COLUMN source_id bigint NULL AFTER source_type,
    ADD COLUMN fee_code varchar(40) NULL AFTER source_id,
    ADD COLUMN billing_unit varchar(20) NULL AFTER fee_code,
    ADD COLUMN billing_quantity decimal(14,4) NULL AFTER quantity,
    ADD COLUMN unit_price decimal(14,4) NULL AFTER billing_quantity,
    ADD COLUMN base_amount decimal(14,2) NULL AFTER unit_price,
    ADD COLUMN currency varchar(8) NOT NULL DEFAULT 'CNY' AFTER amount,
    ADD COLUMN charge_status varchar(20) NOT NULL DEFAULT 'POSTED' AFTER currency,
    ADD COLUMN reversed_record_id bigint NULL AFTER charge_status,
    ADD COLUMN rate_snapshot varchar(1000) NULL AFTER reversed_record_id,
    ADD COLUMN remark varchar(500) NULL AFTER source_ref,
    ADD KEY idx_billing_source (source_type, source_id),
    ADD KEY idx_billing_owner_month (erp_tenant_id, bill_month, fee_type),
    ADD KEY idx_billing_reversal (reversed_record_id);
