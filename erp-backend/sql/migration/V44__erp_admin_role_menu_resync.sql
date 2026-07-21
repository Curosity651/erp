-- =====================================================================
-- 货主(ERP_USER)租户管理员角色 菜单权限重同步 backfill
--
-- 背景：菜单树在版本迭代中不断新增（如 V35 自定义出库按钮 162020-162023、
--   共享只读码 仓库查看160101/区域查看161601 等）。这些新菜单会被
--   TenantRoleMapper.selectAllowedMenuIds('ERP_USER') 纳入货主允许集
--   （从 130000/140000/150000/160000/165000/110000 等根递归取全子树，再并入若干共享码），
--   所以「新开通」的货主租户由 TenantProvisionService 自动获得。
--   但「存量」货主管理员角色 ROLE_ADMIN_T<tenantId> 是在新菜单加入前铸造的，
--   不会自动补齐 → 货主管理员登录后缺按钮/接口 403（如仓库下拉 /wms/warehouse/options
--   需要 wms:warehouse:read=160101）。
--
-- 本脚本：把所有 ERP_USER 货主管理员角色重新对齐到当前 ERP_USER 允许菜单集，
--   仅补其缺失的、且在允许集内的菜单（NOT EXISTS 去重，幂等，可重复执行）。
--   允许集定义与 mapper/tenant/TenantRoleMapper.xml#selectAllowedMenuIds(ERP_USER) 保持一致，
--   二者若调整需同步。WMS_OPERATOR / 平台租户不受影响。
-- =====================================================================

INSERT INTO sys_role_menu (role_code, menu_id)
WITH RECURSIVE inc AS (
    SELECT id FROM sys_menu WHERE id IN (130000,140000,150000,160000,165000,110000,10028,
        100100,100200,100700,101100,100900,101000, 900100,900200)
    UNION ALL SELECT m.id FROM sys_menu m JOIN inc ON m.parent_id = inc.id
),
allowed AS (
    SELECT id FROM inc
    UNION SELECT 100000 UNION SELECT 900000
    -- 共享只读码：仓库查看160101 / 区域查看161601（页面下拉依赖）
    UNION SELECT 160101 UNION SELECT 161601
    -- 角色授权必需：菜单权限查询100801 + 容器100800
    UNION SELECT 100800 UNION SELECT 100801
)
SELECT r.code, a.id
FROM sys_role r
JOIN sys_tenant t ON t.id = r.tenant_id AND t.tenant_type = 'ERP_USER'
CROSS JOIN allowed a
WHERE r.code LIKE 'ROLE_ADMIN_T%'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm WHERE rm.role_code = r.code AND rm.menu_id = a.id
  );
