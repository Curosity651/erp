ALTER TABLE wms_monthly_bill
    ADD COLUMN payment_voucher_file_id BIGINT NULL COMMENT '付款凭证文件ID' AFTER paid_time,
    ADD INDEX idx_monthly_bill_payment_voucher (payment_voucher_file_id);
