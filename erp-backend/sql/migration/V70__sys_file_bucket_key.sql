-- 同一物理桶可能同时承载公有和私有对象，必须保存桶别名才能正确选择直链或签名 URL。
ALTER TABLE sys_file
    ADD COLUMN bucket_key VARCHAR(50) NULL AFTER bucket_name;

UPDATE sys_file
SET bucket_key = CASE
    WHEN object_key LIKE 'uploads/private/%' THEN 'private-files'
    ELSE 'public-files'
END
WHERE bucket_key IS NULL OR bucket_key = '';

ALTER TABLE sys_file
    MODIFY COLUMN bucket_key VARCHAR(50) NOT NULL COMMENT 'OSS桶别名';
