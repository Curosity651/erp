-- 重排「系统管理」下菜单顺序为:角色管理 → 系统用户 → 组织架构 → 项目组管理 → 岗位管理 → 店铺管理
-- 未在需求内的(菜单权限/字典管理/配置信息)顺延到后面,保持原相对次序。
UPDATE sys_menu SET sort = 1 WHERE id = 100200; -- 角色管理
UPDATE sys_menu SET sort = 2 WHERE id = 100100; -- 系统用户
UPDATE sys_menu SET sort = 3 WHERE id = 100700; -- 组织架构
UPDATE sys_menu SET sort = 4 WHERE id = 100900; -- 项目组管理
UPDATE sys_menu SET sort = 5 WHERE id = 101000; -- 岗位管理
UPDATE sys_menu SET sort = 6 WHERE id = 101100; -- 店铺管理
UPDATE sys_menu SET sort = 7 WHERE id = 100800; -- 菜单权限
UPDATE sys_menu SET sort = 8 WHERE id = 100500; -- 字典管理
UPDATE sys_menu SET sort = 9 WHERE id = 100400; -- 配置信息
