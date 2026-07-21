-- V33 WMS服务商·运营数据分析看板(900400)。
-- 后端 GET /wms/operator-dashboard/data 聚合(收入/支出/产品/货主/服务规模)；
-- 菜单由占位指向真实组件(服务商身份在 service 校验，不加权限码，免重登)。

UPDATE sys_menu SET uri = 'dashboard/operator/index' WHERE id = 900400;
