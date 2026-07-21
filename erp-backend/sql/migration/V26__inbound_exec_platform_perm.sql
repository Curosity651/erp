-- V26 平台入库作业权限修正。
-- 问题：平台作业接口 WmsInboundExecutionController 原用 hasAuthority('wms:purchase-inbound:read/edit')，
-- 这些权限码挂在货主「仓储管理160000」子树，平台角色不持有 → 平台收货/上架接口 403、看不到推送的入库单。
-- 修法：给平台作业菜单「入库收货上架」(170501) 赋平台自有权限码，控制器改用之（不再借货主的码）。
-- 平台角色本就授予了 170501，故权限随菜单自动生效（用户需重新登录刷新授权）。

UPDATE sys_menu SET permission = 'wms:inbound-exec:oper' WHERE id = 170501;
