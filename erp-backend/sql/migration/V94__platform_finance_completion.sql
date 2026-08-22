-- Complete platform finance contract collection and supplemental warehouse billing.

ALTER TABLE wms_service_contract
    ADD COLUMN payment_status varchar(20) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/PAID/REFUNDED' AFTER contract_status,
    ADD COLUMN received_time datetime NULL AFTER payment_status,
    ADD COLUMN settled_time datetime NULL AFTER received_time;

UPDATE wms_service_contract c
SET c.payment_status = CASE
        WHEN c.contract_status = 'SETTLED' THEN 'REFUNDED'
        WHEN EXISTS (
            SELECT 1
            FROM wms_contract_fund_ledger f
            WHERE f.contract_id = c.id
              AND f.transaction_type = 'RECEIPT'
              AND f.status = 'POSTED'
        ) THEN 'PAID'
        ELSE 'PENDING'
    END;

INSERT IGNORE INTO wms_fee_rate_card
    (wms_tenant_id, fee_code, fee_name, fee_type, billing_unit, unit_price,
     currency, effective_from, remark)
VALUES
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

