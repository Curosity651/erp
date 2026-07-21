-- 退货质检照片留证：复用阿里云 OSS 直传，qc_photos 存 sys_file 文件ID列表（逗号分隔）
-- 背景：原先 FAIL+电子类仅提交 photoCount 做校验门，照片从不落库。改为存 OSS fileId 以留痕可回溯。
ALTER TABLE wms_return_qc_item
    ADD COLUMN qc_photos VARCHAR(1000) NULL COMMENT '质检照片OSS文件ID(sys_file.id，逗号分隔)' AFTER qc_remark;
