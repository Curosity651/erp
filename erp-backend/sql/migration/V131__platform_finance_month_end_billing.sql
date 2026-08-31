-- Platform finance: collect contract rack rent once and snapshot monthly service bills.

ALTER TABLE wms_service_contract
    ADD COLUMN rack_rent_total DECIMAL(14, 2) NOT NULL DEFAULT 0.00
        COMMENT '合同期货架租赁费，一次性收取' AFTER monthly_rent_per_unit;

UPDATE wms_service_contract
SET rack_rent_total = rack_unit_count * monthly_rent_per_unit
        * (TIMESTAMPDIFF(
            MONTH,
            DATE_FORMAT(start_date, '%Y-%m-01'),
            DATE_FORMAT(end_date, '%Y-%m-01')
        ) + 1),
    monthly_service_recognition = 0.00;

INSERT IGNORE INTO wms_contract_fund_ledger
    (biz_id, contract_id, wms_tenant_id, fund_component, transaction_type,
     direction, amount, currency, accounting_month, status, remark)
SELECT CONCAT('CONTRACT:', c.id, ':RECEIPT:RACK_RENT'),
       c.id, c.wms_tenant_id, 'RACK_RENT', 'RECEIPT',
       'IN', c.rack_rent_total, 'CNY',
       DATE_FORMAT(COALESCE(c.received_time, c.create_time), '%Y-%m'),
       'POSTED', '历史已收款合同按新口径补记一次性货架租赁费'
FROM wms_service_contract c
WHERE c.payment_status = 'PAID'
  AND c.rack_rent_total > 0;

ALTER TABLE wms_billing_record
    ADD COLUMN monthly_bill_id BIGINT NULL COMMENT '所属月度账单快照' AFTER id,
    ADD KEY idx_billing_monthly_bill (monthly_bill_id, charge_status, id);

UPDATE wms_billing_record r
JOIN wms_monthly_bill b
  ON b.wms_tenant_id = r.wms_tenant_id
 AND b.bill_month = r.bill_month
 AND b.deleted = 0
SET r.monthly_bill_id = b.id
WHERE r.monthly_bill_id IS NULL
  AND r.charge_status = 'POSTED'
  AND r.currency = 'CNY';
