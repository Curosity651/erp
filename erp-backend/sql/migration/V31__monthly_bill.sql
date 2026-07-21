-- V31 海外仓平台·平台财务·应收账单（链路一 平台→WMS服务商，业务需求 1.7）。
-- 新建月度账单头 wms_monthly_bill、操作费流水 wms_billing_record、服务商费率折扣 wms_tenant_rate_discount。
-- 货架租金源 = 既有 wms_rack_assignment.monthly_fee；操作费 = 汇总 wms_billing_record(按 wms_tenant_id)。
-- 应收账单菜单(170601)补平台权限码。

CREATE TABLE wms_monthly_bill (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    bill_month     VARCHAR(7)   NOT NULL COMMENT '账期 YYYY-MM',
    wms_tenant_id  BIGINT       NOT NULL COMMENT 'WMS服务商(计费对象)',
    rack_fee       DECIMAL(14, 2) NOT NULL DEFAULT 0 COMMENT '货架租金',
    inbound_fee    DECIMAL(14, 2) NOT NULL DEFAULT 0 COMMENT '入库费',
    outbound_fee   DECIMAL(14, 2) NOT NULL DEFAULT 0 COMMENT '出库费',
    delivery_fee   DECIMAL(14, 2) NOT NULL DEFAULT 0 COMMENT '配送费',
    return_fee     DECIMAL(14, 2) NOT NULL DEFAULT 0 COMMENT '退货费',
    inspection_fee DECIMAL(14, 2) NOT NULL DEFAULT 0 COMMENT '验货费',
    driver_fee     DECIMAL(14, 2) NOT NULL DEFAULT 0 COMMENT '司机费',
    total_amount   DECIMAL(14, 2) NOT NULL DEFAULT 0 COMMENT '合计应收',
    status         VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/PAID/DISPUTED',
    confirmed_time DATETIME     NULL COMMENT '确认时间',
    paid_time      DATETIME     NULL COMMENT '付款时间',
    remark         VARCHAR(500) NULL COMMENT '备注/争议原因',
    create_by      BIGINT       NULL,
    create_time    DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bill_month_tenant (bill_month, wms_tenant_id, deleted),
    KEY idx_bill_tenant (wms_tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '月度应收账单头(链路一)';

CREATE TABLE wms_billing_record (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    biz_id        VARCHAR(64)  NOT NULL COMMENT '业务幂等键',
    wms_tenant_id BIGINT       NOT NULL COMMENT 'WMS服务商',
    erp_tenant_id BIGINT       NULL COMMENT '货主(操作量归集来源)',
    bill_month    VARCHAR(7)   NOT NULL COMMENT '账期 YYYY-MM',
    fee_type      VARCHAR(20)  NOT NULL COMMENT 'INBOUND/OUTBOUND/DELIVERY/RETURN/INSPECTION/DRIVER',
    quantity      INT          NULL COMMENT '计费数量',
    amount        DECIMAL(14, 2) NOT NULL DEFAULT 0 COMMENT '金额(基础费率)',
    source_ref    VARCHAR(64)  NULL COMMENT '来源单据',
    create_time   DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_billing_biz (biz_id),
    KEY idx_billing_tenant_month (wms_tenant_id, bill_month, fee_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '操作费流水(按wms_tenant_id汇总)';

CREATE TABLE wms_tenant_rate_discount (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    wms_tenant_id BIGINT       NOT NULL COMMENT 'WMS服务商',
    discount_pct  DECIMAL(6, 4) NOT NULL DEFAULT 0 COMMENT '折扣百分比(如 -0.1000=九折; 实收=基础×(1+discount))',
    effective_from DATE        NULL,
    effective_to   DATE        NULL,
    remark        VARCHAR(500) NULL,
    create_time   DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_discount_tenant (wms_tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'WMS服务商费率折扣';

UPDATE sys_menu SET permission = 'platform-finance:bill:oper' WHERE id = 170601;
