-- 销售出库的平台订单级分货、面单、打包与 Ozon 交接单配置。

ALTER TABLE wms_sales_outbound_order
    ADD COLUMN document_mode VARCHAR(24) NOT NULL DEFAULT 'WAREHOUSE_PRINT'
        COMMENT '平台资料处理 WAREHOUSE_PRINT/OWNER_PROVIDED' AFTER logistics_product_id;

ALTER TABLE wms_sales_outbound_order_item
    ADD COLUMN sorted_quantity INT NOT NULL DEFAULT 0 COMMENT '二次分货已确认数量' AFTER quantity;

CREATE TABLE IF NOT EXISTS wms_sales_outbound_package (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    outbound_order_id BIGINT       NOT NULL COMMENT '销售出库单ID',
    erp_order_id      BIGINT       NOT NULL COMMENT 'ERP平台订单ID',
    erp_tenant_id     BIGINT       NOT NULL COMMENT '货主ID',
    platform          VARCHAR(32)  NOT NULL COMMENT 'ozon/wildberries/yandex',
    shop_id           BIGINT       NOT NULL COMMENT '店铺ID',
    platform_order_id VARCHAR(128) NOT NULL COMMENT '平台订单号',
    sort_code         VARCHAR(32)  DEFAULT NULL COMMENT '订单级分货格口号',
    sort_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SORTED/NOT_REQUIRED',
    label_status      VARCHAR(24)  NOT NULL DEFAULT 'NOT_READY' COMMENT 'NOT_READY/READY/EXTERNAL_CONFIRMED',
    pack_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PACKED',
    packer_name       VARCHAR(64)  DEFAULT NULL,
    pack_time         DATETIME     DEFAULT NULL,
    version           INT          NOT NULL DEFAULT 0,
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbound_erp_order (outbound_order_id, erp_order_id),
    KEY idx_package_outbound_status (outbound_order_id, sort_status, pack_status),
    KEY idx_package_erp_order (erp_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售出库平台订单包裹';

-- 兼容迁移前已经存在的销售出库单，一笔 ERP 平台订单对应一个待打包包裹。
INSERT INTO wms_sales_outbound_package
    (outbound_order_id, erp_order_id, erp_tenant_id, platform, shop_id,
     platform_order_id, sort_code, sort_status, label_status, pack_status)
SELECT source.outbound_order_id,
       source.erp_order_id,
       source.erp_tenant_id,
       source.platform,
       source.shop_id,
       source.platform_order_id,
       CASE WHEN source.order_count > 1
            THEN CONCAT('B', source.outbound_order_id, '-', LPAD(source.package_seq, 2, '0'))
            ELSE NULL END,
       CASE WHEN source.order_count <= 1 THEN 'NOT_REQUIRED'
            WHEN source.order_status IN ('PICKED', 'PACKED', 'SHIPPED', 'COMPLETED') THEN 'SORTED'
            ELSE 'PENDING' END,
       CASE WHEN source.label_base64 IS NULL OR source.label_base64 = ''
            THEN 'NOT_READY' ELSE 'READY' END,
       CASE WHEN source.order_status IN ('PACKED', 'SHIPPED', 'COMPLETED')
            THEN 'PACKED' ELSE 'PENDING' END
FROM (
    SELECT DISTINCT soi.outbound_order_id,
           soi.erp_order_id,
           so.erp_tenant_id,
           so.platform,
           eo.shop_id,
           eo.platform_order_id,
           so.order_count,
           so.order_status,
           eo.label_base64,
           ROW_NUMBER() OVER (PARTITION BY soi.outbound_order_id ORDER BY soi.erp_order_id) AS package_seq
    FROM wms_sales_outbound_order_item soi
    JOIN wms_sales_outbound_order so ON so.id = soi.outbound_order_id
    JOIN erp_order eo ON eo.id = soi.erp_order_id
    WHERE so.source_type = 'SALES' AND soi.erp_order_id IS NOT NULL
) source
ON DUPLICATE KEY UPDATE
    platform_order_id = VALUES(platform_order_id),
    label_status = VALUES(label_status);

ALTER TABLE erp_order
    ADD COLUMN delivery_method_id BIGINT DEFAULT NULL COMMENT 'Ozon配送方式ID' AFTER warehouse_name,
    ADD COLUMN delivery_method_name VARCHAR(255) DEFAULT NULL COMMENT 'Ozon配送方式名称' AFTER delivery_method_id,
    ADD COLUMN confirm_state VARCHAR(16) NOT NULL DEFAULT 'NONE'
        COMMENT '平台确认幂等状态 NONE/PROCESSING/SUCCESS/FAILED' AFTER locked,
    ADD COLUMN confirm_started_at DATETIME DEFAULT NULL AFTER confirm_state;

-- 历史 Ozon FBS 订单从已保存的原始响应中回填配送方式，后续同步直接写结构化字段。
UPDATE erp_order
SET delivery_method_id = CAST(JSON_UNQUOTE(JSON_EXTRACT(raw_json, '$.delivery_method.id')) AS UNSIGNED),
    delivery_method_name = JSON_UNQUOTE(JSON_EXTRACT(raw_json, '$.delivery_method.name'))
WHERE platform = 'ozon'
  AND raw_json IS NOT NULL
  AND JSON_VALID(raw_json)
  AND JSON_EXTRACT(raw_json, '$.delivery_method.id') IS NOT NULL;

CREATE TABLE IF NOT EXISTS ozon_delivery_method_rule (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    tenant_id          BIGINT      NOT NULL COMMENT '货主租户ID',
    shop_id            BIGINT      NOT NULL COMMENT 'Ozon店铺ID',
    delivery_method_id BIGINT      NOT NULL COMMENT 'Ozon配送方式ID',
    act_required       TINYINT     NOT NULL DEFAULT 0 COMMENT '是否必须生成Ozon Act',
    containers_count   INT         NOT NULL DEFAULT 1 COMMENT '默认容器数',
    enabled            TINYINT     NOT NULL DEFAULT 1,
    create_time        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ozon_rule_scope (tenant_id, shop_id, delivery_method_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Ozon配送方式交接单规则';

-- 兼容现有业务：只在迁移时把原先“仓库名含大”转换成显式规则，运行时代码不再猜名称。
INSERT INTO ozon_delivery_method_rule
    (tenant_id, shop_id, delivery_method_id, act_required, containers_count, enabled)
SELECT DISTINCT tenant_id, shop_id, delivery_method_id, 1, 1, 1
FROM erp_order
WHERE platform = 'ozon'
  AND delivery_method_id IS NOT NULL
  AND warehouse_name LIKE '%大%'
ON DUPLICATE KEY UPDATE act_required = VALUES(act_required), enabled = 1;

ALTER TABLE ozon_shipment_act
    ADD COLUMN request_key VARCHAR(160) DEFAULT NULL COMMENT 'Act业务幂等键' AFTER tenant_id;

UPDATE ozon_shipment_act
SET request_key = CONCAT(tenant_id, ':', shop_id, ':', delivery_method_id, ':', departure_date)
WHERE request_key IS NULL;

ALTER TABLE ozon_shipment_act
    MODIFY COLUMN request_key VARCHAR(160) NOT NULL COMMENT 'Act业务幂等键',
    ADD UNIQUE KEY uk_ozon_act_request (request_key);
