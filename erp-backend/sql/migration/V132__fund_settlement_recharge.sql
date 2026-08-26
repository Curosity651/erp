CREATE TABLE IF NOT EXISTS wms_recharge_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  recharge_no VARCHAR(40) NOT NULL,
  account_scope VARCHAR(24) NOT NULL COMMENT 'OWNER_ACCOUNT/PLATFORM_ACCOUNT',
  wms_tenant_id BIGINT NOT NULL,
  erp_tenant_id BIGINT NULL,
  amount DECIMAL(14,2) NOT NULL,
  currency VARCHAR(8) NOT NULL,
  payment_time DATETIME NOT NULL,
  voucher_file_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  applicant_id BIGINT NOT NULL,
  applicant_name VARCHAR(100) NOT NULL,
  reviewer_id BIGINT NULL,
  reviewer_name VARCHAR(100) NULL,
  review_time DATETIME NULL,
  reject_reason VARCHAR(500) NULL,
  reverse_order_id BIGINT NULL,
  reverse_reason VARCHAR(500) NULL,
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_recharge_no (recharge_no),
  KEY idx_recharge_account (account_scope, wms_tenant_id, erp_tenant_id, currency, status, create_time),
  KEY idx_recharge_voucher (voucher_file_id),
  CONSTRAINT chk_recharge_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='两级充值记录';

UPDATE sys_menu
SET title='资金结算', icon='account-book', path='fund-settlement',
    uri='wms/fund-settlement/index', permission=NULL, hidden=0, sort=3
WHERE id=180200;

UPDATE sys_menu SET hidden=1 WHERE id=180300;

INSERT IGNORE INTO sys_role_menu (role_code, menu_id)
SELECT DISTINCT role_code, 180200 FROM sys_role_menu source
WHERE source.menu_id=180300;

INSERT IGNORE INTO sys_menu
    (id, parent_id, title, icon, path, uri, sort, type, permission, hidden,
     target_type, create_time, update_time, deleted)
VALUES
    (170603, 170600, '服务商资金', 'wallet', 'provider-funds',
     'platform-finance/provider-funds/index', 3, 1,
     'platform-finance:provider-funds:oper', 0, 1, NOW(), NOW(), 0);

INSERT IGNORE INTO sys_role_menu (role_code, menu_id)
SELECT DISTINCT role_code, 170603 FROM sys_role_menu source
WHERE source.menu_id=170601;
