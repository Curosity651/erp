-- V27 入库收货与入库上架拆分为两个页面。
-- 170501「入库收货上架」→「入库收货」（只做收货，扫码抽屉）；
-- 170502「上架」（原占位）→「入库上架」（三段式 + 上架抽屉），补平台作业权限码。
-- 组件实际由前端 COMPONENT_OVERRIDES(按 path: inbound-ops / putaway) 覆盖挂载；uri 同步更新保持一致。

UPDATE sys_menu
SET title = '入库收货', uri = 'platform/inbound-ops/InboundReceivePage'
WHERE id = 170501;

-- 170502 原为隐藏占位(hidden=1)，拆分后需在菜单显示 → hidden=0
UPDATE sys_menu
SET title = '入库上架', uri = 'platform/inbound-ops/InboundPutawayPage',
    permission = 'wms:inbound-exec:oper', hidden = 0
WHERE id = 170502;
