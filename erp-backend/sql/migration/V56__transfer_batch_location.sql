-- 调拨管理重做为「跨仓库·批次/库位级·两段式在途」：明细加"源批次"与"目标库位"。
--   · 发出：扣源仓(A)的源批次(物理) → A 可用降；过账 目标仓(B) 在途+；
--   · 到货：在 B 的目标库位建批次(物理) → B 可用升；过账 B 在途-；
--   · 目标库位必须落在【该货主的服务商在 B 仓当前有效租用的排】上，否则不能调（建单即拦）。
-- 可用量一律由物理批次派生(聚合)，在途由过账维护，二者不重叠、不双写。
ALTER TABLE wms_transfer_order_item
    ADD COLUMN source_physical_inventory_id BIGINT NULL COMMENT '源批次ID(A仓，发出时扣此批次)',
    ADD COLUMN target_location_code VARCHAR(64) NULL COMMENT '目标库位编码(B仓，须落在该货主服务商租用排)';
