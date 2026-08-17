-- Use UTF-8 byte literals so applying this migration is independent of the
-- terminal and mysql client character set on Windows.
UPDATE sys_menu
SET title = CONVERT(0xE5BA93E4BD8DE5BA93E5AD98 USING utf8mb4),
    update_time = NOW()
WHERE id = 170512 AND deleted = 0;

UPDATE sys_menu
SET title = CONVERT(0xE4BABAE5B7A5E587BAE5BA93 USING utf8mb4),
    update_time = NOW()
WHERE id = 162002 AND deleted = 0;

UPDATE sys_menu
SET title = CONVERT(0xE587BAE5BA93E4BD9CE4B89A USING utf8mb4),
    update_time = NOW()
WHERE id = 170503 AND deleted = 0;
