-- Overseas warehouse receives returns first; ERP owners only decide their disposition.
ALTER TABLE wms_return_inbound_order
    ADD COLUMN return_batch_no VARCHAR(50) NULL COMMENT '海外仓退货批次号' AFTER return_no,
    ADD COLUMN disposition_by BIGINT NULL COMMENT '货主处置操作人' AFTER received_time,
    ADD COLUMN disposition_time DATETIME NULL COMMENT '货主处置时间' AFTER disposition_by,
    ADD COLUMN processed_by BIGINT NULL COMMENT '海外仓处理操作人' AFTER disposition_time,
    ADD COLUMN processed_time DATETIME NULL COMMENT '海外仓处理完成时间' AFTER processed_by,
    ADD INDEX idx_return_batch_no (return_batch_no),
    ADD INDEX idx_return_owner_status (erp_tenant_id, return_status, deleted);

ALTER TABLE wms_return_qc_item
    ADD COLUMN platform_order_id VARCHAR(100) NULL COMMENT '来源平台订单号' AFTER sku_code,
    ADD COLUMN return_reason VARCHAR(20) NULL COMMENT '退货原因' AFTER platform_order_id,
    ADD COLUMN restock_qty INT NOT NULL DEFAULT 0 COMMENT '货主决定直接上架数量' AFTER damaged_qty,
    ADD COLUMN rework_qty INT NOT NULL DEFAULT 0 COMMENT '货主决定返工检测数量' AFTER restock_qty,
    ADD COLUMN scrap_qty INT NOT NULL DEFAULT 0 COMMENT '货主决定直接销毁数量' AFTER rework_qty,
    ADD COLUMN rework_pass_qty INT NOT NULL DEFAULT 0 COMMENT '返工后可上架数量' AFTER scrap_qty,
    ADD COLUMN rework_scrap_qty INT NOT NULL DEFAULT 0 COMMENT '返工后销毁数量' AFTER rework_pass_qty,
    ADD COLUMN disposition_remark VARCHAR(500) NULL COMMENT '货主处置说明' AFTER rework_scrap_qty,
    ADD COLUMN processed_location_code VARCHAR(50) NULL COMMENT '最终上架库位' AFTER disposition_remark,
    ADD COLUMN processed_location_id BIGINT NULL COMMENT '最终上架库位ID' AFTER processed_location_code;

UPDATE wms_return_inbound_order
SET return_batch_no = return_no
WHERE return_batch_no IS NULL;

UPDATE wms_return_inbound_order
SET return_status = CASE return_status
    WHEN 'RETURN_PENDING' THEN 'PENDING_OWNER'
    WHEN 'QC_PENDING' THEN 'PENDING_OPERATION'
    ELSE return_status
END;

UPDATE sys_menu
SET title = '退货处理', uri = 'wms/return-inbound/ReturnInboundPage', hidden = 0
WHERE id = 160500;

UPDATE sys_menu
SET title = '退货处理', uri = 'platform/return-ops/ReturnQcPage'
WHERE id = 170505;

UPDATE sys_menu SET hidden = 1 WHERE id = 162004;
