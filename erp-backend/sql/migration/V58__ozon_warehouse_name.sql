-- Ozon 发货仓库名落列（支撑「大仓/小仓」判定与筛选）
--
-- 背景：Ozon FBS 订单的发货仓库名只存在于 raw_json.delivery_method.warehouse 中，
--       既无法在列表页展示，也无法做 SQL 筛选（扫 JSON 全表扫描）。
-- 业务规则：仓库名包含「大」字即为大仓（大件），只有大仓订单需要生成运单(act)；
--          小仓订单打完面单与拣货单即结束。
-- 仅 Ozon FBS 订单有该字段；FBO 由 Ozon 仓配，raw_json 中无 delivery_method 节点，保持 NULL。
--
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V58__ozon_warehouse_name.sql
-- 一次性迁移，勿重复执行。

ALTER TABLE erp_order
    ADD COLUMN warehouse_name VARCHAR(200) DEFAULT NULL
        COMMENT 'Ozon FBS 发货仓库名(来自 delivery_method.warehouse)；含「大」字=大仓' AFTER warehouse_id;

-- 回填历史数据：raw_json 均为合法 JSON，直接用原生 JSON 函数提取
UPDATE erp_order
SET warehouse_name = JSON_UNQUOTE(JSON_EXTRACT(raw_json, '$.delivery_method.warehouse'))
WHERE platform = 'ozon'
  AND fulfillment_type = 'FBS'
  AND raw_json IS NOT NULL
  AND JSON_VALID(raw_json)
  AND JSON_EXTRACT(raw_json, '$.delivery_method.warehouse') IS NOT NULL;

-- 仓型筛选走 warehouse_name LIKE '%大%'，前缀通配符用不上索引；
-- 该索引服务于 (租户 + 平台) 维度收敛后再做仓库名比较，且便于按仓库名精确查询。
CREATE INDEX idx_erp_order_tenant_platform_wh
    ON erp_order (tenant_id, platform, warehouse_name);
