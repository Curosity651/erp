-- Ozon 运单（交接单 act）落库
--
-- 业务：大仓(仓库名含「大」字)的 FBS 订单在打完面单后点【准备发运】，按
--       (店铺, 物流方式, 发货日期) 分组，每组向 Ozon 创建一份运单。
--       Ozon 侧异步生成（约 1~2 分钟），前端轮询状态，就绪后下载 PDF 并存 OSS。
--
-- 幂等：同一 (tenant_id, shop_id, delivery_method_id, departure_date) 若已存在
--       PENDING/READY 的运单，复用该行，不重复调用 Ozon act/create。
--
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V59__ozon_shipment_act.sql
-- 一次性迁移，勿重复执行。

CREATE TABLE ozon_shipment_act (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id            BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID(货主)',
    batch_no             VARCHAR(64)  NOT NULL COMMENT '一次【准备发运】的批次号，前端据此轮询',
    shop_id              BIGINT       NOT NULL COMMENT '店铺ID(运单调用所用的 Ozon 凭证归属)',
    delivery_method_id   BIGINT       NOT NULL COMMENT 'Ozon 物流方式ID',
    delivery_method_name VARCHAR(255) DEFAULT NULL COMMENT '物流方式名称(快照)',
    warehouse_name       VARCHAR(200) DEFAULT NULL COMMENT '发货仓库名(快照，含「大」字=大仓)',
    departure_date       DATE         NOT NULL COMMENT '发货日期',
    containers_count     INT          NOT NULL DEFAULT 1 COMMENT '箱数',
    ozon_act_id          BIGINT       DEFAULT NULL COMMENT 'Ozon 侧运单ID(act/create 返回)',
    status               VARCHAR(20)  NOT NULL DEFAULT 'CREATING'
                         COMMENT 'CREATING=创建中 / PENDING=Ozon生成中 / READY=可下载 / FAILED=失败',
    object_key           VARCHAR(500) DEFAULT NULL COMMENT 'OSS 对象键(PDF)',
    file_name            VARCHAR(500) DEFAULT NULL COMMENT '文件名',
    order_count          INT          NOT NULL DEFAULT 0 COMMENT '本组纳入的订单数(仅留痕，非运单实际货件数)',
    error_msg            VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
    created_by           BIGINT       DEFAULT NULL COMMENT '操作人 user_id',
    create_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_tenant_batch (tenant_id, batch_no),
    KEY idx_tenant_idem (tenant_id, shop_id, delivery_method_id, departure_date, status),
    UNIQUE KEY uk_tenant_ozon_act (tenant_id, ozon_act_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Ozon运单(交接单act)';

CREATE TABLE ozon_shipment_act_order (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    tenant_id      BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID(货主)',
    act_id         BIGINT      NOT NULL COMMENT 'ozon_shipment_act.id(本地主键，非Ozon运单ID)',
    order_id       BIGINT      NOT NULL COMMENT 'erp_order.id',
    posting_number VARCHAR(128) DEFAULT NULL COMMENT 'Ozon posting number(快照)',
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_act (act_id),
    UNIQUE KEY uk_act_order (act_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Ozon运单-订单关联(留痕)';
