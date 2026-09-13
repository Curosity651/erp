-- Lightweight outbound handover records. Transport amounts are operational records only:
-- they are intentionally not linked to wms_client_billing_record or wms_monthly_bill.
ALTER TABLE wms_fulfillment_order
    ADD COLUMN handover_status VARCHAR(32) NULL COMMENT 'READY_HANDOVER/HANDED_OVER' AFTER shipped_by,
    ADD COLUMN vehicle_plate VARCHAR(64) NULL COMMENT '交接车辆车牌' AFTER handover_status,
    ADD COLUMN driver_name VARCHAR(128) NULL COMMENT '司机姓名' AFTER vehicle_plate,
    ADD COLUMN driver_phone VARCHAR(64) NULL COMMENT '司机电话' AFTER driver_name,
    ADD COLUMN departure_time DATETIME NULL COMMENT '发车时间' AFTER driver_phone,
    ADD COLUMN handover_destination VARCHAR(1000) NULL COMMENT '交接目的地' AFTER departure_time,
    ADD COLUMN recorded_freight_cost DECIMAL(14,2) NULL COMMENT '记录运费，不参与系统计费' AFTER handover_destination,
    ADD COLUMN recorded_freight_currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '记录运费币种' AFTER recorded_freight_cost,
    ADD COLUMN logistics_photo_file_ids VARCHAR(1000) NULL COMMENT '物流照片sys_file.id，逗号分隔' AFTER recorded_freight_currency,
    ADD COLUMN handover_remark VARCHAR(500) NULL COMMENT '交接备注' AFTER logistics_photo_file_ids,
    ADD COLUMN handover_time DATETIME NULL COMMENT '确认交接时间' AFTER handover_remark,
    ADD COLUMN handover_by BIGINT NULL COMMENT '确认交接操作人' AFTER handover_time,
    ADD INDEX idx_fulfillment_handover_page (handover_status, warehouse_id, erp_tenant_id, create_time),
    ADD INDEX idx_fulfillment_handover_by (handover_by);

UPDATE wms_fulfillment_order
SET handover_status = CASE fulfillment_status
    WHEN 'SHIPPED' THEN 'HANDED_OVER'
    WHEN 'PACKED' THEN 'READY_HANDOVER'
    ELSE NULL
END,
handover_destination = recipient_address,
handover_time = CASE WHEN fulfillment_status = 'SHIPPED' THEN shipped_time ELSE NULL END,
handover_by = CASE WHEN fulfillment_status = 'SHIPPED' THEN shipped_by ELSE NULL END
WHERE fulfillment_status IN ('PACKED', 'SHIPPED') AND deleted = 0;

CREATE TABLE wms_transport_expense_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '运输台账所属租户（海外仓平台或WMS服务商）',
    expense_type VARCHAR(32) NOT NULL COMMENT 'FUEL/DRIVER_MONTHLY',
    expense_date DATE NULL COMMENT '油费发生日期',
    settlement_month CHAR(7) NULL COMMENT '司机月结月份YYYY-MM',
    driver_name VARCHAR(128) NULL COMMENT '司机姓名',
    amount DECIMAL(14,2) NOT NULL COMMENT '记录金额，不参与系统计费',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    note VARCHAR(500) NULL COMMENT '备注',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_transport_expense_tenant_type_date (tenant_id, expense_type, expense_date, settlement_month, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运输费用独立记录台账（不参与计费）';
