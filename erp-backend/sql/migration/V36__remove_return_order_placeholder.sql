-- =====================================================================
-- 删除货主端「退货订单」空占位页（菜单 162003）
--   决策(用户)：ERP+OMS 货主端「退货订单」自始至终是 common/Placeholder 空页，
--   从未落地成真实页面；退货入库能力已由 162004「自定义退货单」(V34) 承载。
--   故直接下线该菜单，避免货主看到一个永远为空的入口。
--   162003 无子权限、无专属前端组件、无独立业务表，删除干净。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V36__remove_return_order_placeholder.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

DELETE FROM sys_role_menu WHERE menu_id = 162003;
DELETE FROM sys_menu WHERE id = 162003;
