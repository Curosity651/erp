ALTER TABLE wms_purchase_inbound_order
    ADD COLUMN receive_evidence_file_ids VARCHAR(2000) NULL
        COMMENT '收货现场照片文件ID，逗号分隔' AFTER receive_time;
