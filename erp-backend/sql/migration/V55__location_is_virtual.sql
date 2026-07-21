-- 虚拟库位（收纳积压货的通用化）。虚拟库位不对应任何物理货架格子、排号为永不租出的合成值，
-- 服务商仓储概览按"物理货架占用"统计 → 天然感知不到。平台可在库位管理里自定义增删多个虚拟库位。
-- 往虚拟库位放货：货主数量不变(allocatable 保持1)、服务商看不见、批次 container_stored=1 排除自动发货挑拣。
ALTER TABLE wms_location
    ADD COLUMN is_virtual TINYINT NOT NULL DEFAULT 0 COMMENT '是否虚拟库位(0物理1虚拟)：虚拟=不占物理货架、服务商不可见、收纳积压货用';
