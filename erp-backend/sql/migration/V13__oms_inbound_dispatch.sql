-- =====================================================================
-- D2 · 入库链路对接（选项B：OMS 下发层解耦 + 驱动现有 WMS 执行）
--   新建 OMS 货主下发层 oms_inbound_order(_item) + 同进程投递流水 oms_sync_outbox；
--   现有 wms_purchase_inbound_order 加 source_type / oms_inbound_order_id，放开 shipping_order_id 可空（支持自定义入库）。
--   两条链：①采购(采购单→物流单→入库单) ②自定义入库单 —— 均下发给平台执行【收货→上架】。
--   软删除遵现有约定 deleted(BIGINT)+@TableLogic；outbox 为投递流水【不加 deleted】。
--   审计 create_by/create_time/update_by/update_time。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V13__oms_inbound_dispatch.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- ---------- 1. OMS 入库下发单（货主侧）----------
CREATE TABLE IF NOT EXISTS oms_inbound_order (
  id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id             BIGINT       NOT NULL            COMMENT '货主租户ID(货物归属)',
  inbound_no            VARCHAR(64)  NOT NULL            COMMENT '入库单号',
  source_type           VARCHAR(16)  NOT NULL            COMMENT '来源: PURCHASE采购 / MANUAL自定义',
  purchase_order_id     BIGINT       DEFAULT NULL        COMMENT '采购单ID(PURCHASE)',
  shipping_order_id     BIGINT       DEFAULT NULL        COMMENT '物流单ID(PURCHASE)',
  warehouse_id          BIGINT       NOT NULL            COMMENT '目标仓库ID',
  custom_tracking_no    VARCHAR(128) DEFAULT NULL        COMMENT '自定义跟踪号(MANUAL)',
  expected_arrival_date DATE         DEFAULT NULL        COMMENT '预计到货日',
  status                VARCHAR(16)  NOT NULL DEFAULT 'CREATED' COMMENT '状态: CREATED/DISPATCHED/RECEIVED/COMPLETED/CANCELLED',
  wms_inbound_order_id  BIGINT       DEFAULT NULL        COMMENT '驱动生成的WMS执行单ID(回填)',
  remark                VARCHAR(500) DEFAULT NULL        COMMENT '备注',
  create_by             BIGINT       DEFAULT NULL        COMMENT '创建人ID',
  create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by             BIGINT       DEFAULT NULL        COMMENT '更新人ID',
  update_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted               BIGINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除标记: 0-未删除 / 时间戳-已删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_inbound_no (tenant_id, inbound_no, deleted),
  KEY idx_tenant (tenant_id),
  KEY idx_status (status),
  KEY idx_wms (wms_inbound_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OMS入库下发单(货主侧)';

-- ---------- 2. OMS 入库下发单明细 ----------
CREATE TABLE IF NOT EXISTS oms_inbound_order_item (
  id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  inbound_order_id  BIGINT       NOT NULL            COMMENT 'OMS入库单ID',
  tenant_id         BIGINT       NOT NULL            COMMENT '货主租户ID(冗余,便于隔离)',
  sku_code          VARCHAR(100) NOT NULL            COMMENT 'SKU编码',
  expected_quantity INT          NOT NULL DEFAULT 0  COMMENT '预期数量',
  received_quantity INT          NOT NULL DEFAULT 0  COMMENT '平台回传实收数量',
  putaway_quantity  INT          NOT NULL DEFAULT 0  COMMENT '平台回传上架数量',
  remark            VARCHAR(500) DEFAULT NULL        COMMENT '备注',
  create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_order (inbound_order_id),
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OMS入库下发单明细';

-- ---------- 3. 同进程投递流水（OMS→WMS 下发，禁 HTTP/MQ）----------
--   不加 deleted（投递流水）；幂等/重试由 status + retry_count + next_retry_time 控制。
CREATE TABLE IF NOT EXISTS oms_sync_outbox (
  id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  aggregate_type  VARCHAR(32)   NOT NULL            COMMENT '聚合类型: INBOUND_ORDER',
  aggregate_id    BIGINT        NOT NULL            COMMENT '聚合根ID(如 oms_inbound_order.id)',
  event_type      VARCHAR(32)   NOT NULL            COMMENT '事件类型: INBOUND_DISPATCH',
  payload         TEXT          DEFAULT NULL        COMMENT '事件载荷(JSON)',
  status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/DONE/FAILED',
  retry_count     INT           NOT NULL DEFAULT 0  COMMENT '已重试次数',
  next_retry_time DATETIME      DEFAULT NULL        COMMENT '下次重试时间',
  last_error      VARCHAR(1000) DEFAULT NULL        COMMENT '最后一次错误',
  create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_status_retry (status, next_retry_time),
  KEY idx_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OMS→WMS同进程下发投递流水';

-- ---------- 4. 现有 WMS 执行单对接改造 ----------
--   加来源标识 + 回链 OMS 下发单；放开 shipping_order_id 可空（自定义入库无物流单）。
--   现有 122 行均为 PURCHASE 且有 shipping_order_id，默认值兼容存量。
ALTER TABLE wms_purchase_inbound_order
  ADD COLUMN source_type          VARCHAR(16) NOT NULL DEFAULT 'PURCHASE' COMMENT '来源: PURCHASE采购 / MANUAL自定义' AFTER inbound_no,
  ADD COLUMN oms_inbound_order_id BIGINT      DEFAULT NULL COMMENT '来源OMS下发单ID' AFTER source_type,
  MODIFY COLUMN shipping_order_id BIGINT      DEFAULT NULL COMMENT '关联物流单ID(自定义入库为空)';

ALTER TABLE wms_purchase_inbound_order
  ADD KEY idx_oms_inbound (oms_inbound_order_id);
