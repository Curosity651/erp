-- Unify provider funds and monthly reconciliation under one platform finance entry.

ALTER TABLE wms_monthly_bill
    ADD COLUMN reviewer_id BIGINT NULL COMMENT '月度对账复核人' AFTER confirmed_time,
    ADD COLUMN reviewer_name VARCHAR(100) NULL COMMENT '复核人名称快照' AFTER reviewer_id;

ALTER TABLE wms_billing_record
    ADD COLUMN operator_id BIGINT NULL COMMENT '人工调整操作人' AFTER remark,
    ADD COLUMN operator_name VARCHAR(100) NULL COMMENT '人工调整操作人名称快照' AFTER operator_id;

UPDATE sys_menu
SET title = '服务商结算',
    icon = 'account-book',
    path = 'provider-funds',
    uri = 'platform-finance/provider-funds/index',
    permission = 'platform-finance:settle',
    sort = 1,
    hidden = 0
WHERE id = 170603;

UPDATE sys_menu SET hidden=1 WHERE id=170601;

INSERT IGNORE INTO sys_role_menu (role_code, menu_id)
SELECT DISTINCT role_code, 170603
FROM sys_role_menu source
WHERE source.menu_id = 170601;
