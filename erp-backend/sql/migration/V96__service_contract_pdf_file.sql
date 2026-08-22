ALTER TABLE wms_service_contract
    ADD COLUMN contract_file_id BIGINT NULL COMMENT '合同PDF文件ID' AFTER contract_file_url,
    ADD INDEX idx_service_contract_file (contract_file_id);
