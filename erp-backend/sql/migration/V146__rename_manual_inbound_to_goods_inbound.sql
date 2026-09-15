-- Display-only rename for the ERP owner portal. Business routes and source type stay unchanged.
UPDATE sys_menu SET title = '商品入库' WHERE id = 162001;
UPDATE sys_menu SET title = '商品入库查看' WHERE id = 162005;
UPDATE sys_menu SET title = '商品入库新增' WHERE id = 162006;
UPDATE sys_menu SET title = '商品入库编辑' WHERE id = 162007;
UPDATE sys_menu SET title = '商品入库删除' WHERE id = 162008;
