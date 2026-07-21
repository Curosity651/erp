-- 退货入库单记录被退的订单明细ID，供平台质检完成时回写「已退数量」到 erp_order_item / erp_order。
-- 货主退货改为「只申报」（不再建单即过账/扣减），质检与已退数量扣减统一由海外仓平台质检完成时处理。
ALTER TABLE wms_return_inbound_order
    ADD COLUMN order_item_id BIGINT NULL COMMENT '被退的 erp_order_item.id（平台质检完成时回写已退数量用）' AFTER erp_order_id;
