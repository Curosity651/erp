-- 集装箱存储（调拨管理·平台隐藏收纳）。海外仓平台把货主积压货物挪进"集装箱"隐藏分区库位存储：
--   · 对货主：数量一件不变（allocatable 保持 1，仍计入可用），只是物理位置隐藏（口径甲）；
--   · 对服务商：集装箱库位建在"未租给任何服务商的专用排"上，仓储概览天然看不到；
--   · 自动发货 FIFO 挑拣排除 container_stored=1 的批次（口径A：积压货安静存着，不被优先发掉）；
--   · 平台可追踪，且可"取回"调拨回原库位。
-- 批次表 wms_physical_inventory 加两列承载该状态（台账表无 deleted，直接 ADD COLUMN）。
ALTER TABLE wms_physical_inventory
    ADD COLUMN container_stored TINYINT NOT NULL DEFAULT 0 COMMENT '是否已入集装箱隐藏存储(0否1是)：1=计入货主可用但排除FIFO挑拣、对服务商隐藏',
    ADD COLUMN origin_location_code VARCHAR(64) NULL COMMENT '入箱前原库位编码（供取回时默认调拨回原位）';

-- 追踪/挑拣过滤走 container_stored，建索引加速（仓库维度列已足够选择性）
CREATE INDEX idx_phys_container ON wms_physical_inventory (container_stored, warehouse_id);
