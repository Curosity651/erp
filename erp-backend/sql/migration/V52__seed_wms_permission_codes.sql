-- C1 / S2 剩余 5 控制器补权限码。此前这 5 个控制器(物流产品/资产账务/服务商财务/服务商看板/发货生产测算)
-- 无方法级 @PreAuthorize，任意登录身份可越权/越平台调用；而本系统 @per.hasPermission 无超管绕过，
-- 权限码必须经 sys_role_menu 授给角色才生效。故先在 sys_menu 播种按钮型权限码(type=2)并授权对应平台角色，
-- 再给控制器加注解(注解改动见 Java 代码)。
--
-- 归属(由现有页面菜单授权口径坐实)：
--   货主页(asset-finance 150200 / ship-prod-calc 165100) → ROLE_ADMIN_T6, ROLE_ADMIN_T7, ROLE_T6_ROLE_MANAGER
--   服务商页(logistics-product 180100 / income 180200 / expense 180300 / 新建 operator-dashboard) → ROLE_ADMIN_T5, ROLE_ADMIN_T8
--
-- 幂等：sys_menu 用固定 ID + ON DUPLICATE KEY UPDATE；sys_role_menu 靠唯一键(role_code,menu_id) + INSERT IGNORE。

-- ============ 1. 按钮型权限码(挂在已存在的页面菜单下) ============
INSERT INTO sys_menu (id, parent_id, title, permission, path, target_type, uri, sort, keep_alive, hidden, type, deleted)
VALUES
  -- 物流产品(服务商)：读/增/改/删
  (180101, 180100, '查看', 'wms:logistics-product:read', NULL, 1, '', 1, 0, 0, 2, 0),
  (180102, 180100, '新增', 'wms:logistics-product:add',  NULL, 1, '', 2, 0, 0, 2, 0),
  (180103, 180100, '编辑', 'wms:logistics-product:edit', NULL, 1, '', 3, 0, 0, 2, 0),
  (180104, 180100, '删除', 'wms:logistics-product:del',  NULL, 1, '', 4, 0, 0, 2, 0),
  -- 服务商财务(income/expense 两页共用同一读权限码)
  (180201, 180200, '查看', 'wms:operator-finance:read',  NULL, 1, '', 1, 0, 0, 2, 0),
  (180301, 180300, '查看', 'wms:operator-finance:read',  NULL, 1, '', 1, 0, 0, 2, 0),
  -- 资产账务(货主)：读
  (150201, 150200, '查看', 'wms:asset-finance:read',     NULL, 1, '', 1, 0, 0, 2, 0),
  -- 发货生产测算(货主)：读
  (165101, 165100, '查看', 'wms:ship-prod-calc:read',    NULL, 1, '', 1, 0, 0, 2, 0)
ON DUPLICATE KEY UPDATE
  parent_id=VALUES(parent_id), title=VALUES(title), permission=VALUES(permission),
  path=VALUES(path), target_type=VALUES(target_type), uri=VALUES(uri), type=VALUES(type), deleted=0;

-- ============ 2. 服务商看板：新建页面菜单 + 按钮(做法甲：连入口一起补) ============
-- 页面组件 views/dashboard/operator/index.vue → uri=dashboard/operator
INSERT INTO sys_menu (id, parent_id, title, icon, permission, path, target_type, uri, sort, keep_alive, hidden, type, deleted)
VALUES
  (180500, 180000, '服务商看板', 'dashboard', NULL, 'operator-dashboard', 1, 'dashboard/operator', 5, 0, 0, 1, 0),
  (180501, 180500, '查看', NULL, 'wms:operator-dashboard:read', NULL, 1, '', 1, 0, 0, 2, 0)
ON DUPLICATE KEY UPDATE
  parent_id=VALUES(parent_id), title=VALUES(title), icon=VALUES(icon), permission=VALUES(permission),
  path=VALUES(path), target_type=VALUES(target_type), uri=VALUES(uri), type=VALUES(type), deleted=0;

-- ============ 3. 授权(sys_role_menu，按平台归属) ============
-- 服务商 T5/T8：物流产品4按钮 + 服务商财务2按钮 + 服务商看板(页面+按钮)
INSERT IGNORE INTO sys_role_menu (role_code, menu_id) VALUES
  ('ROLE_ADMIN_T5', 180101), ('ROLE_ADMIN_T5', 180102), ('ROLE_ADMIN_T5', 180103), ('ROLE_ADMIN_T5', 180104),
  ('ROLE_ADMIN_T5', 180201), ('ROLE_ADMIN_T5', 180301), ('ROLE_ADMIN_T5', 180500), ('ROLE_ADMIN_T5', 180501),
  ('ROLE_ADMIN_T8', 180101), ('ROLE_ADMIN_T8', 180102), ('ROLE_ADMIN_T8', 180103), ('ROLE_ADMIN_T8', 180104),
  ('ROLE_ADMIN_T8', 180201), ('ROLE_ADMIN_T8', 180301), ('ROLE_ADMIN_T8', 180500), ('ROLE_ADMIN_T8', 180501);

-- 货主 T6/T7/T6经理：资产账务读 + 发货测算读
INSERT IGNORE INTO sys_role_menu (role_code, menu_id) VALUES
  ('ROLE_ADMIN_T6', 150201), ('ROLE_ADMIN_T6', 165101),
  ('ROLE_ADMIN_T7', 150201), ('ROLE_ADMIN_T7', 165101),
  ('ROLE_T6_ROLE_MANAGER', 150201), ('ROLE_T6_ROLE_MANAGER', 165101);
