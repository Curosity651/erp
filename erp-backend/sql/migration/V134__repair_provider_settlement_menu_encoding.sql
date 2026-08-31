-- Repair 服务商结算 with UTF-8 bytes so the title is stable across SQL client encodings.
UPDATE sys_menu
SET title = CONVERT(0xE69C8DE58AA1E59586E7BB93E7AE97 USING utf8mb4),
    permission = 'platform-finance:settle',
    hidden = 0
WHERE id = 170603;
