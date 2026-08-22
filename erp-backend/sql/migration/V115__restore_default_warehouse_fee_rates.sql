-- Restore platform default warehouse operation rates when reference data was cleared.
-- Operator-specific rates still take precedence over these defaults.

INSERT IGNORE INTO wms_fee_rate_card
    (wms_tenant_id, fee_code, fee_name, fee_type, billing_unit, unit_price,
     currency, effective_from, remark)
VALUES
    (0, 'INBOUND_CBM', '入库操作费', 'INBOUND', 'CBM', 50.0000,
     'CNY', '2026-01-01', '收货、理货、上架'),
    (0, 'AFTER_HOURS_SURCHARGE', '客户原因加班附加费', 'INBOUND', 'RATE', 0.5000,
     'CNY', '2026-01-01', '基础操作费的50%'),
    (0, 'OUTBOUND_FULL_PALLET', '整托出库费', 'OUTBOUND', 'PALLET', 80.0000,
     'CNY', '2026-01-01', '完整LPN离库'),
    (0, 'OUTBOUND_LARGE_BOX', '大箱出库费', 'OUTBOUND', 'BOX', 10.0000,
     'CNY', '2026-01-01', '一箱一计费件'),
    (0, 'OUTBOUND_SMALL_ITEM', '小件散货出库费', 'OUTBOUND', 'ITEM', 1.0000,
     'CNY', '2026-01-01', '按件计费'),
    (0, 'DELIVERY_TRUCK_ACTUAL', '整车配送费', 'DELIVERY', 'ACTUAL', 0.0000,
     'CNY', '2026-01-01', '按实际发生金额登记'),
    (0, 'DELIVERY_LTL_SHARE', '零担配送分摊费', 'DELIVERY', 'ACTUAL', 0.0000,
     'CNY', '2026-01-01', '按实际托盘占比分摊后登记'),
    (0, 'DELIVERY_SHORT_SMALL_BOX', '短途小箱配送费', 'DELIVERY', 'BOX', 10.0000,
     'CNY', '2026-01-01', '5公里内且单箱小于20kg'),
    (0, 'DELIVERY_SHORT_LARGE_BOX', '短途大箱配送费', 'DELIVERY', 'BOX', 20.0000,
     'CNY', '2026-01-01', '5公里内且单箱小于50kg'),
    (0, 'RETURN_PICKUP_BOX', '退货取件费', 'RETURN', 'BOX', 10.0000,
     'CNY', '2026-01-01', '5公里内，含取货、拍照和分拣'),
    (0, 'RETURN_INSPECTION_GENERAL', '普货验货费', 'INSPECTION', 'ITEM', 5.0000,
     'CNY', '2026-01-01', '开箱检查并重新装回'),
    (0, 'RETURN_INSPECTION_ELECTRONIC', '电子产品验货费', 'INSPECTION', 'ITEM', 10.0000,
     'CNY', '2026-01-01', '开箱检查和开机测试');
