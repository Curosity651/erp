-- =====================================================================
-- A1 · 多租户地基（纯新增，不改业务表）
-- 手工执行：mysql -h127.0.0.1 -P3307 -uroot -p123456 erp < erp-backend/sql/migration/V1__tenant_foundation.sql
-- 配套：com.erp.admin.tenant.* + com.erp.admin.common.tenant.*
-- =====================================================================

-- 1. 租户表（双层：tenant_type = WMS_OPERATOR 第一层 / ERP_USER 第二层）
CREATE TABLE IF NOT EXISTS sys_tenant (
  id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '租户ID',
  tenant_code          VARCHAR(32)  NOT NULL COMMENT '租户编码（英文短码）',
  tenant_name          VARCHAR(100) NOT NULL COMMENT '租户名称（公司名）',
  tenant_type          VARCHAR(20)  NOT NULL DEFAULT 'ERP_USER' COMMENT 'WMS_OPERATOR=3PL服务商 / ERP_USER=货主',
  parent_wms_tenant_id BIGINT       DEFAULT NULL COMMENT '所属WMS服务商tenant_id（ERP_USER必填）',
  contact_name         VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
  contact_phone        VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  contact_email        VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
  status               TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
  remark               VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted              BIGINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除标识（@TableLogic）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_code (tenant_code),
  KEY idx_parent_wms (parent_wms_tenant_id),
  KEY idx_type (tenant_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表（双层：WMS服务商>ERP货主）';

-- 2. 用户-租户绑定（一个用户只属于一个租户）
CREATE TABLE IF NOT EXISTS sys_user_tenant (
  id          BIGINT   NOT NULL AUTO_INCREMENT,
  user_id     BIGINT   NOT NULL COMMENT 'sys_user.id，唯一约束保证一用户一租户',
  tenant_id   BIGINT   NOT NULL COMMENT 'sys_tenant.id',
  is_admin    TINYINT  NOT NULL DEFAULT 0 COMMENT '是否本租户管理员',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id (user_id),
  KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-租户关联表（一对一）';

-- 3. 默认租户（历史数据归属）+ 存量用户全部绑定，确保旧数据不丢、可见
INSERT INTO sys_tenant (id, tenant_code, tenant_name, tenant_type, status)
SELECT 1, 'default_wms', '默认WMS服务商（历史数据）', 'WMS_OPERATOR', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant WHERE id = 1);

-- 注：BallCat sys_user 主键为 user_id，且含逻辑删除列 deleted，仅绑定未删除用户
INSERT INTO sys_user_tenant (user_id, tenant_id, is_admin)
SELECT u.user_id, 1, 0
FROM sys_user u
WHERE u.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_user_tenant ut WHERE ut.user_id = u.user_id);
