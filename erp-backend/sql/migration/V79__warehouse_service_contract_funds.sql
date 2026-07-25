-- Warehouse service contract and refundable fund ledger.

CREATE TABLE IF NOT EXISTS wms_service_contract (
    id bigint NOT NULL AUTO_INCREMENT,
    contract_no varchar(64) NOT NULL,
    wms_tenant_id bigint NOT NULL,
    warehouse_id bigint NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    rack_unit_count int NOT NULL DEFAULT 3,
    monthly_rent_per_unit decimal(14,2) NOT NULL DEFAULT 20000.00,
    warehouse_deposit decimal(14,2) NOT NULL DEFAULT 60000.00,
    subscription_total decimal(14,2) NOT NULL DEFAULT 180000.00,
    refundable_rate decimal(6,4) NOT NULL DEFAULT 0.7000,
    refundable_amount decimal(14,2) NOT NULL DEFAULT 126000.00,
    service_amount decimal(14,2) NOT NULL DEFAULT 54000.00,
    monthly_service_recognition decimal(14,2) NOT NULL DEFAULT 4500.00,
    contract_file_url varchar(500) NULL,
    contract_status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'DRAFT/ACTIVE/SETTLED/TERMINATED',
    remark varchar(500) NULL,
    create_by bigint NULL,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_service_contract_no (contract_no),
    KEY idx_contract_operator_status (wms_tenant_id, contract_status),
    KEY idx_contract_warehouse (warehouse_id, start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='warehouse service contract';

CREATE TABLE IF NOT EXISTS wms_contract_rack (
    id bigint NOT NULL AUTO_INCREMENT,
    contract_id bigint NOT NULL,
    rack_assignment_id bigint NULL,
    rack_no varchar(20) NOT NULL,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_contract_rack (contract_id, rack_no),
    KEY idx_contract_rack_assignment (rack_assignment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='contract rack scope';

CREATE TABLE IF NOT EXISTS wms_contract_fund_ledger (
    id bigint NOT NULL AUTO_INCREMENT,
    biz_id varchar(100) NOT NULL,
    contract_id bigint NOT NULL,
    wms_tenant_id bigint NOT NULL,
    fund_component varchar(40) NOT NULL
        COMMENT 'WAREHOUSE_DEPOSIT/SUBSCRIPTION_REFUNDABLE/SUBSCRIPTION_SERVICE',
    transaction_type varchar(20) NOT NULL COMMENT 'RECEIPT/RECOGNITION/REFUND/FORFEIT/ADJUSTMENT',
    direction varchar(8) NOT NULL COMMENT 'IN/OUT',
    amount decimal(14,2) NOT NULL,
    currency varchar(8) NOT NULL DEFAULT 'CNY',
    accounting_month varchar(7) NULL,
    status varchar(20) NOT NULL DEFAULT 'POSTED',
    remark varchar(500) NULL,
    operator_id bigint NULL,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_contract_fund_biz (biz_id),
    KEY idx_contract_fund (contract_id, fund_component, create_time),
    KEY idx_contract_fund_operator (wms_tenant_id, accounting_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='contract deposit and subscription fund ledger';

INSERT IGNORE INTO sys_menu
    (id, parent_id, title, icon, path, uri, sort, type, permission, hidden, target_type, create_time, update_time, deleted)
VALUES
    (170602, 170600, '合同资金', 'audit', 'warehouse-contracts',
     'platform-finance/warehouse-contract/index', 2, 1,
     'platform-finance:contract:oper', 0, 1, NOW(), NOW(), 0);

INSERT IGNORE INTO sys_role_menu (role_code, menu_id)
SELECT DISTINCT role_code, 170602 FROM sys_role_menu WHERE menu_id = 170601;

