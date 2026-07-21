-- V29 海外仓平台出库作业·打包签出（业务需求 1.3.2）。
-- 在既有销售出库单上扩展打包/签出字段；新建客户计费流水表(链路二物流费, 幂等 biz_id)，供本模块生成、月度账单(模块5)汇总。
-- 库存扣减铁律：签出(SHIPPED)时才 quantity-=实发 并释放 reserved_qty（下架仅锁定）。

ALTER TABLE wms_sales_outbound_order
    ADD COLUMN pack_mode           VARCHAR(20)    NULL COMMENT '打包模式 BY_SKU/BY_ORDER/SECONDARY/CARTON',
    ADD COLUMN packer_name         VARCHAR(64)    NULL COMMENT '打包员',
    ADD COLUMN channel_name        VARCHAR(64)    NULL COMMENT '签出物流渠道名',
    ADD COLUMN tracking_no         VARCHAR(100)   NULL COMMENT '面单跟踪号',
    ADD COLUMN weight              DECIMAL(10, 3) NULL COMMENT '称重(kg)',
    ADD COLUMN shipping_fee        DECIMAL(12, 2) NULL COMMENT '链路二物流费',
    ADD COLUMN logistics_product_id BIGINT        NULL COMMENT '关联物流产品(计费依据)';

CREATE TABLE wms_client_billing_record (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    biz_id               VARCHAR(64)  NOT NULL COMMENT '业务幂等键(如 SHIP:{outboundOrderId})',
    wms_tenant_id        BIGINT       NULL DEFAULT 0 COMMENT '货架归属(WMS服务商)，无则0',
    erp_tenant_id        BIGINT       NOT NULL COMMENT '货主',
    outbound_order_id    BIGINT       NULL COMMENT '关联出库单',
    fee_type             VARCHAR(20)  NOT NULL COMMENT '费用类型 SHIPPING 物流费/OPERATION 操作费',
    amount               DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '金额',
    currency             VARCHAR(8)   NOT NULL DEFAULT 'RUB' COMMENT '币种',
    tracking_no          VARCHAR(100) NULL COMMENT '面单跟踪号',
    logistics_product_id BIGINT       NULL COMMENT '物流产品',
    bill_month           VARCHAR(7)   NULL COMMENT '账期 YYYY-MM',
    create_time          DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_client_billing_biz (biz_id),
    KEY idx_client_billing_tenant (wms_tenant_id, bill_month),
    KEY idx_client_billing_erp (erp_tenant_id, bill_month)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '客户计费流水(链路二物流费等，按biz_id幂等)';
