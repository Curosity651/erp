-- =====================================================================
-- 平台菜单：把「仓储管理」(160000) 下的物理作业三页迁到「海外仓作业」(170500)。
--   盘点管理(160800) / 调拨管理(160700) / 库存调整(160900) → 海外仓作业。
--   迁移后平台超管不再显示「仓储管理」(由 menu-filter 去掉 ops 子树/容器特例实现)；
--   仓储管理(160000) 仍是货主的 WMS 主菜单，保留不动。
--   前端深链(库存记录 SourceTypeRouteMap、调拨/盘点表单返回列表)已同步改为 /ops/* 路径。
-- 执行：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V18__move_stock_ops_to_overseas.sql
-- 一次性迁移，勿重复执行。
-- =====================================================================

-- 三页迁入 海外仓作业(170500)，排在「入库收货上架」(sort=1) 之后
UPDATE sys_menu SET parent_id = 170500, sort = 2 WHERE id = 160800; -- 盘点管理
UPDATE sys_menu SET parent_id = 170500, sort = 3 WHERE id = 160700; -- 调拨管理
UPDATE sys_menu SET parent_id = 170500, sort = 4 WHERE id = 160900; -- 库存调整

-- 占位作业项后移，保证真实作业项在前
UPDATE sys_menu SET sort = 11 WHERE id = 170502; -- 上架(隐藏占位)
UPDATE sys_menu SET sort = 12 WHERE id = 170503; -- 下架(拣货)占位
UPDATE sys_menu SET sort = 13 WHERE id = 170504; -- 打包签出占位
UPDATE sys_menu SET sort = 14 WHERE id = 170505; -- 退货质检占位
