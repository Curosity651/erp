-- V30 海外仓平台出库作业·退货质检（业务需求 1.4）。
-- 退货 = 带质检的重新入库。既有 wms_return_inbound_order 为单SKU扁平记录、无状态；此处附加平台质检工作流：
--   加 return_status 状态列（RETURN_PENDING→QC_PENDING→COMPLETED）；新建质检明细表 wms_return_qc_item；
--   退货质检菜单(170505)补平台权限码。不改既有退货单产生逻辑。

ALTER TABLE wms_return_inbound_order
    ADD COLUMN return_status VARCHAR(20) NULL DEFAULT 'RETURN_PENDING' COMMENT '退货质检状态 RETURN_PENDING/QC_PENDING/COMPLETED';

UPDATE wms_return_inbound_order SET return_status = 'RETURN_PENDING' WHERE return_status IS NULL;

CREATE TABLE wms_return_qc_item (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    return_order_id BIGINT      NOT NULL COMMENT '退货单ID',
    sku_code        VARCHAR(64) NOT NULL COMMENT 'SKU编码',
    electronic      TINYINT     NOT NULL DEFAULT 0 COMMENT '是否电子类(影响FAIL强制拍照)',
    expected_qty    INT         NOT NULL DEFAULT 0 COMMENT '应退数',
    received_qty    INT         NULL COMMENT '实收数(收货后)',
    qc_result       VARCHAR(10) NULL COMMENT '质检结果 PASS/FAIL',
    zone            VARCHAR(20) NULL COMMENT '回库分区 RETURN/STANDARD/DEFECTIVE',
    quality         VARCHAR(10) NULL COMMENT '品质 GOOD/DAMAGED',
    location_code   VARCHAR(50) NULL COMMENT '回库库位',
    qc_remark       VARCHAR(500) NULL COMMENT '质检备注',
    create_time     DATETIME    NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_return_qc_order (return_order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '退货质检明细(对账/理赔依据)';

UPDATE sys_menu SET permission = 'wms:return-qc:oper' WHERE id = 170505;
