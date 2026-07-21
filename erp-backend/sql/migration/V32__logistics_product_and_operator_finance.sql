-- V32 WMS服务商：物流产品管理 + 财务收入/支出页面。
-- 物流产品：服务商定义(词条tags+统一单价)，提供给名下货主；货主建单时选用，平台签出时按单价
-- 生成 wms_client_billing_record(链路二收入)。收入页=按产品×月聚合该流水；支出页=wms_monthly_bill 服务商侧只读。
-- 菜单 180100/180200/180300 由占位指向真实组件（服务商身份在 service 校验，不加权限码，免重登）。

CREATE TABLE wms_logistics_product (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    wms_tenant_id BIGINT         NOT NULL COMMENT '归属WMS服务商',
    product_name  VARCHAR(100)   NOT NULL COMMENT '产品名称',
    product_code  VARCHAR(50)    NULL COMMENT '产品编码',
    tags          VARCHAR(500)   NULL COMMENT '特性词条JSON数组(大件/小件/自提/自定义...)',
    unit_price    DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '统一单价(每次使用)',
    status        TINYINT        NOT NULL DEFAULT 1 COMMENT '1启用/0停用',
    remark        VARCHAR(500)   NULL,
    create_by     BIGINT         NULL,
    create_time   DATETIME       NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME       NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_logistics_product_tenant (wms_tenant_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'WMS服务商物流产品';

UPDATE sys_menu SET uri = 'wms/logistics-product' WHERE id = 180100;
UPDATE sys_menu SET uri = 'wms/finance-income' WHERE id = 180200;
UPDATE sys_menu SET uri = 'wms/finance-expense' WHERE id = 180300;
