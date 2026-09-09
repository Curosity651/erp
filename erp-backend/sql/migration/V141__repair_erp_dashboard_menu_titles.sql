-- Repair legacy mojibake while keeping menu titles readable when i18n is unavailable.
UPDATE sys_menu SET title = '数据看板' WHERE id = 900100;
UPDATE sys_menu SET title = '销售目标' WHERE id = 900200;

UPDATE sys_menu SET title = '销售目标查询' WHERE id = 900201;
UPDATE sys_menu SET title = '销售目标新增' WHERE id = 900202;
UPDATE sys_menu SET title = '销售目标修改' WHERE id = 900203;
UPDATE sys_menu SET title = '销售目标删除' WHERE id = 900204;
