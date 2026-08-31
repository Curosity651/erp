ALTER TABLE wms_fulfillment_pick_task
    ADD COLUMN operation_mode VARCHAR(16) NULL COMMENT '作业模式: SCAN/SIMPLE' AFTER operator_id,
    ADD COLUMN evidence_file_ids TEXT NULL COMMENT '整单作业凭证文件ID,逗号分隔' AFTER operation_mode,
    ADD COLUMN simplified_completed_time DATETIME NULL COMMENT '整单作业完成时间' AFTER evidence_file_ids;
