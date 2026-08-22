ALTER TABLE erp_order
    ADD COLUMN label_verify_codes TEXT NULL COMMENT '面单复核码JSON数组' AFTER label_base64;
