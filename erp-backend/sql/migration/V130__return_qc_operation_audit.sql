ALTER TABLE wms_return_inbound_order
    ADD COLUMN received_by BIGINT NULL COMMENT '退货收货操作人' AFTER return_status,
    ADD COLUMN received_time DATETIME NULL COMMENT '退货收货时间' AFTER received_by,
    ADD COLUMN qc_by BIGINT NULL COMMENT '退货质检操作人' AFTER received_time,
    ADD COLUMN qc_time DATETIME NULL COMMENT '退货质检完成时间' AFTER qc_by,
    ADD COLUMN closed_by BIGINT NULL COMMENT '退货关闭操作人' AFTER qc_time,
    ADD COLUMN closed_time DATETIME NULL COMMENT '退货关闭时间' AFTER closed_by;
