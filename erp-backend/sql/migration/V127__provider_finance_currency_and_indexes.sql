-- Provider finance must preserve the original currency and support scoped month queries.

ALTER TABLE wms_monthly_bill
    ADD COLUMN currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '账单币种' AFTER total_amount;

UPDATE wms_monthly_bill
SET currency = 'CNY'
WHERE currency IS NULL OR TRIM(currency) = '';

CREATE INDEX idx_monthly_bill_tenant_month_status_currency
    ON wms_monthly_bill (wms_tenant_id, bill_month, status, currency, deleted);

CREATE INDEX idx_client_billing_tenant_month_currency
    ON wms_client_billing_record (wms_tenant_id, bill_month, currency, fee_type);
