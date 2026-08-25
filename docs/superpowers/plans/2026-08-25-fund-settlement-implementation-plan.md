# 资金结算与充值记录功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立 ERP 货主向 WMS 服务商、WMS 服务商向海外仓平台的两级充值记录、凭证审核、分币种余额与统一结算页面。

**Architecture:** 新增一张不可直接改余额的充值单表，充值通过状态机确认；余额由已确认充值减去现有物流产品费用或平台仓储费用实时汇总。后端提供按身份隔离的三组接口，前端将 WMS 原“财务收入/财务支出”合并为“资金结算”，ERP 和平台分别增加账户与审核入口。

**Tech Stack:** Java 8、Spring Boot 2.7、MyBatis-Plus/MyBatis XML、MySQL 8、Vue 3、TypeScript、Ant Design Vue、Vitest、JUnit 5、Mockito。

**Spec:** `docs/superpowers/specs/2026-08-25-fund-settlement-design.md`

## Global Constraints

- 系统只记录资金和凭证，不接支付接口，不操作真实银行资金。
- 余额按币种独立核算，不进行汇率折算。
- 余额不足只显示预警，不阻止任何订单或仓库作业。
- 月度账单是平台费用快照，不允许在余额中重复扣减。
- 已通过充值不可编辑、不可物理删除，只能创建冲正记录。
- 所有写操作必须校验当前身份、租户归属、凭证和状态 CAS。
- 不回退或覆盖工作区中其他未提交修改。

---

### Task 1: 数据库结构、实体与状态策略

**Files:**
- Create: `erp-backend/sql/migration/V132__fund_settlement_recharge.sql`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/model/entity/WmsRechargeOrder.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/model/enums/RechargeAccountScope.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/model/enums/RechargeStatus.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/mapper/WmsRechargeOrderMapper.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/finance/settlement/FundSettlementMigrationTest.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/finance/settlement/RechargeStatusPolicyTest.java`

**Interfaces:**
- Produces: `RechargeAccountScope.OWNER_ACCOUNT`, `RechargeAccountScope.PLATFORM_ACCOUNT`。
- Produces: `RechargeStatus.PENDING/APPROVED/REJECTED/CANCELLED/REVERSED`。
- Produces: `WmsRechargeOrderMapper.transition(id, fromStatus, toStatus, reviewerId, reviewerName, reviewTime, reason): int`。

- [ ] **Step 1: 编写迁移结构失败测试**

```java
@Test
void migrationMustCreateRechargeTableAndCasIndexes() throws Exception {
    String sql = read("../../sql/migration/V132__fund_settlement_recharge.sql");
    assertTrue(sql.contains("CREATE TABLE wms_recharge_order"));
    assertTrue(sql.contains("UNIQUE KEY uk_recharge_no"));
    assertTrue(sql.contains("KEY idx_recharge_account"));
    assertTrue(sql.contains("CHECK (amount > 0)"));
}
```

- [ ] **Step 2: 运行测试并确认因文件不存在失败**

Run: `mvn -pl admin -Dtest=FundSettlementMigrationTest test`

Expected: FAIL，提示读取不到 `V132__fund_settlement_recharge.sql`。

- [ ] **Step 3: 编写状态策略失败测试**

```java
@Test
void onlyPendingCanBeReviewedOrCancelled() {
    assertTrue(RechargeStatus.PENDING.canApprove());
    assertTrue(RechargeStatus.PENDING.canCancel());
    assertFalse(RechargeStatus.APPROVED.canApprove());
    assertTrue(RechargeStatus.APPROVED.canReverse());
}
```

- [ ] **Step 4: 实现迁移、实体、枚举和 CAS Mapper**

迁移核心结构：

```sql
CREATE TABLE wms_recharge_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  recharge_no VARCHAR(40) NOT NULL,
  account_scope VARCHAR(24) NOT NULL,
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
  CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='两级充值记录';
```

CAS Mapper 使用：

```java
@Update("UPDATE wms_recharge_order SET status=#{toStatus}, reviewer_id=#{reviewerId}, "
    + "reviewer_name=#{reviewerName}, review_time=#{reviewTime}, reject_reason=#{reason} "
    + "WHERE id=#{id} AND status=#{fromStatus}")
int transition(...);
```

- [ ] **Step 5: 运行结构与策略测试**

Run: `mvn -pl admin -Dtest=FundSettlementMigrationTest,RechargeStatusPolicyTest test`

Expected: PASS。

- [ ] **Step 6: 提交本任务**

```bash
git add erp-backend/sql/migration/V132__fund_settlement_recharge.sql \
  erp-backend/admin/src/main/java/com/erp/admin/finance/settlement \
  erp-backend/admin/src/test/java/com/erp/admin/finance/settlement
git commit -m "feat: add fund settlement recharge ledger"
```

---

### Task 2: 充值写入、审核、取消与冲正服务

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/model/dto/CreateRechargeDTO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/model/dto/ReviewRechargeDTO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/model/dto/ReverseRechargeDTO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/service/RechargeOrderService.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/finance/settlement/RechargeOrderServiceTest.java`

**Interfaces:**
- Consumes: `WmsRechargeOrderMapper.transition(...)` from Task 1。
- Produces: `createOwnerRecharge(CreateRechargeDTO): WmsRechargeOrder`。
- Produces: `createPlatformRecharge(CreateRechargeDTO): WmsRechargeOrder`。
- Produces: `reviewOwnerRecharge(long id, ReviewRechargeDTO): void`。
- Produces: `reviewPlatformRecharge(long id, ReviewRechargeDTO): void`。
- Produces: `cancel(long id): void`、`reverse(long id, ReverseRechargeDTO): void`。

- [ ] **Step 1: 编写服务失败测试**

```java
@Test
void ownerRechargeUsesCurrentOwnerAndItsParentProvider() {
    when(identity.currentIdentity(null)).thenReturn(ownerIdentity(6L));
    when(tenantMapper.selectById(6L)).thenReturn(ownerTenant(6L, 5L));
    when(fileService.getFileInfo(88L)).thenReturn(pngFile(88L));
    WmsRechargeOrder order = service.createOwnerRecharge(dto("1000.00", "RUB", 88L));
    assertEquals(6L, order.getErpTenantId());
    assertEquals(5L, order.getWmsTenantId());
    assertEquals("PENDING", order.getStatus());
}

@Test
void duplicateApprovalIsRejectedByCas() {
    when(mapper.transition(anyLong(), eq("PENDING"), eq("APPROVED"), any(), any(), any(), isNull()))
        .thenReturn(0);
    assertThrows(BusinessException.class,
        () -> service.reviewOwnerRecharge(1L, approveDto()));
}
```

同时覆盖：金额非正、币种空白、凭证不存在、凭证格式错误、跨服务商审核、货主取消他人充值、冲正非已通过充值。

- [ ] **Step 2: 运行服务测试并确认失败**

Run: `mvn -pl admin -Dtest=RechargeOrderServiceTest test`

Expected: FAIL，类和方法尚未实现。

- [ ] **Step 3: 实现 DTO 校验和身份解析**

```java
@NotNull
@DecimalMin(value = "0.01")
private BigDecimal amount;

@NotBlank
@Pattern(regexp = "[A-Za-z]{3,8}")
private String currency;

@NotNull
private Long voucherFileId;
```

`createOwnerRecharge` 必须从当前 ERP 身份读取 `tenantId`，再从 `SysTenant.parentWmsTenantId` 推导服务商；不得接受前端传入租户 ID。`createPlatformRecharge` 必须从 `WmsTenantContext` 读取当前服务商。

- [ ] **Step 4: 实现凭证、状态和租户校验**

复用 `SysFileService.getFileInfo`，只允许 PDF/JPG/PNG。审核前重新查询充值单并校验收款方身份，随后使用 CAS 更新：

```java
if (rechargeMapper.transition(id, "PENDING", target, userId, userName, now, reason) != 1) {
    throw new BusinessException(409, "充值单状态已变化，请刷新后重试");
}
```

冲正创建一条相同金额、相同币种并关联原单的 `REVERSED` 记录，同时将原单从 `APPROVED` CAS 更新为 `REVERSED`；统一余额查询排除原单并计入冲正效果，不能直接改金额。

- [ ] **Step 5: 运行服务测试**

Run: `mvn -pl admin -Dtest=RechargeOrderServiceTest test`

Expected: PASS。

- [ ] **Step 6: 提交本任务**

```bash
git add erp-backend/admin/src/main/java/com/erp/admin/finance/settlement \
  erp-backend/admin/src/test/java/com/erp/admin/finance/settlement/RechargeOrderServiceTest.java
git commit -m "feat: implement recharge approval workflow"
```

---

### Task 3: 分币种余额、统一流水与三方接口

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/model/qo/FundSettlementQO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/model/vo/FundAccountVO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/model/vo/FundLedgerVO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/model/vo/RechargeOrderVO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/mapper/FundSettlementQueryMapper.java`
- Create: `erp-backend/admin/src/main/resources/mapper/financial/FundSettlementQueryMapper.xml`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/service/FundAccountQueryService.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/controller/OwnerFundSettlementController.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/controller/OperatorFundSettlementController.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/finance/settlement/controller/PlatformFundSettlementController.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/finance/settlement/FundSettlementQueryMapperSqlTest.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/finance/settlement/FundSettlementControllerContractTest.java`

**Interfaces:**
- Produces: `ownerAccount(): List<FundAccountVO>`。
- Produces: `operatorOwnerAccounts(FundSettlementQO): List<FundAccountVO>`。
- Produces: `operatorPlatformAccount(): List<FundAccountVO>`。
- Produces: `platformProviderAccounts(FundSettlementQO): List<FundAccountVO>`。
- Produces: `ledger(accountScope, wmsTenantId, erpTenantId, currency, dateRange): PageResult<FundLedgerVO>`。

- [ ] **Step 1: 编写余额 SQL 失败测试**

```java
@Test
void ownerBalanceUsesApprovedRechargeMinusShippingCharges() {
    String xml = readMapper();
    assertTrue(xml.contains("status = 'APPROVED'"));
    assertTrue(xml.contains("wms_client_billing_record"));
    assertTrue(xml.contains("fee_type = 'SHIPPING'"));
    assertTrue(xml.contains("GROUP BY"));
    assertTrue(xml.contains("currency"));
}

@Test
void platformBalanceUsesBillingRecordsNotMonthlyBill() {
    String xml = readMapper();
    assertTrue(xml.contains("wms_billing_record"));
    assertFalse(balanceSelect(xml).contains("wms_monthly_bill"));
}
```

- [ ] **Step 2: 编写接口权限契约失败测试**

验证路由和权限分别为：

```text
/fund-settlement/owner/**       ERP_USER
/fund-settlement/operator/**    WMS_OPERATOR
/fund-settlement/platform/**    OVERSEAS_PLATFORM
```

Run: `mvn -pl admin -Dtest=FundSettlementQueryMapperSqlTest,FundSettlementControllerContractTest test`

Expected: FAIL。

- [ ] **Step 3: 实现余额查询 SQL**

货主余额按 `(wms_tenant_id, erp_tenant_id, currency)` 汇总；平台余额按 `(wms_tenant_id, currency)` 汇总。用充值和费用币种全集作为驱动，保证只有费用、没有充值时仍返回负余额。

```sql
balance = COALESCE(approved_recharge, 0) - COALESCE(charged_amount, 0)
warning = balance < 0
```

统一流水使用 `UNION ALL` 返回：`RECHARGE` 为正、`CHARGE` 为负、`REVERSAL` 为负，并按发生时间和 ID 倒序。

- [ ] **Step 4: 实现查询服务和三方控制器**

控制器只接受筛选参数，不接受可扩大租户范围的 ID。服务层使用 `TenantIdentityService`、`TenantContext`、`WmsTenantContext` 和 `TenantHierarchyService` 收窄范围。

- [ ] **Step 5: 运行接口与 SQL 测试**

Run: `mvn -pl admin -Dtest=FundSettlementQueryMapperSqlTest,FundSettlementControllerContractTest,RechargeOrderServiceTest test`

Expected: PASS。

- [ ] **Step 6: 提交本任务**

```bash
git add erp-backend/admin/src/main/java/com/erp/admin/finance/settlement \
  erp-backend/admin/src/main/resources/mapper/financial/FundSettlementQueryMapper.xml \
  erp-backend/admin/src/test/java/com/erp/admin/finance/settlement
git commit -m "feat: expose isolated fund settlement accounts"
```

---

### Task 4: ERP 货主端 WMS 账户与充值登记

**Files:**
- Create: `erp-frontend/src/api/fund-settlement/types.ts`
- Create: `erp-frontend/src/api/fund-settlement/index.ts`
- Create: `erp-frontend/src/views/wms/asset-finance/components/WmsFundAccountPanel.vue`
- Create: `erp-frontend/src/views/wms/asset-finance/components/RechargeCreateModal.vue`
- Modify: `erp-frontend/src/views/wms/asset-finance/AssetFinancePage.vue`
- Create: `erp-frontend/src/views/wms/asset-finance/fund-account-policy.ts`
- Test: `erp-frontend/src/views/wms/asset-finance/fund-account-policy.vitest.ts`

**Interfaces:**
- Consumes: `/fund-settlement/owner/accounts`、`/owner/recharges`、`/owner/ledger`、`POST /owner/recharges`、`POST /owner/recharges/{id}/cancel`。
- Produces: ERP 资产与账务页面中的 `WMS账户`页签。

- [ ] **Step 1: 编写前端状态策略失败测试**

```ts
it('only pending recharge created by current user can be cancelled', () => {
  expect(canCancelRecharge({ status: 'PENDING', applicantId: 9 }, 9)).toBe(true)
  expect(canCancelRecharge({ status: 'APPROVED', applicantId: 9 }, 9)).toBe(false)
})
```

Run: `pnpm vitest run src/views/wms/asset-finance/fund-account-policy.vitest.ts`

Expected: FAIL。

- [ ] **Step 2: 定义 API 类型和请求**

```ts
export interface FundAccountVO {
  accountScope: 'OWNER_ACCOUNT' | 'PLATFORM_ACCOUNT'
  wmsTenantId: number
  erpTenantId?: number
  currency: string
  rechargeAmount: number
  chargeAmount: number
  balance: number
  negative: boolean
  pendingCount: number
}
```

- [ ] **Step 3: 实现充值弹窗**

表单字段：金额、币种、付款时间、付款凭证、备注。凭证使用 `SysFileUpload`，未上传时禁用提交；成功后刷新账户、充值记录和流水。

- [ ] **Step 4: 将 WMS 账户并入资产与账务**

在现有明细页签后增加 `WMS账户`，展示分币种余额卡、充值记录和物流费用流水。负余额使用红色提示“仅作记录，不影响业务操作”。

- [ ] **Step 5: 运行前端单测和类型检查**

Run: `pnpm vitest run src/views/wms/asset-finance/fund-account-policy.vitest.ts && pnpm type-check`

Expected: PASS。

- [ ] **Step 6: 提交本任务**

```bash
git add erp-frontend/src/api/fund-settlement \
  erp-frontend/src/views/wms/asset-finance
git commit -m "feat: add owner WMS fund account"
```

---

### Task 5: WMS 服务商“资金结算”合并页面与菜单迁移

**Files:**
- Create: `erp-frontend/src/views/wms/fund-settlement/index.vue`
- Create: `erp-frontend/src/views/wms/fund-settlement/OwnerSettlementPanel.vue`
- Create: `erp-frontend/src/views/wms/fund-settlement/PlatformSettlementPanel.vue`
- Create: `erp-frontend/src/views/wms/fund-settlement/RechargeReviewDrawer.vue`
- Create: `erp-frontend/src/views/wms/fund-settlement/FundLedgerDrawer.vue`
- Modify: `erp-frontend/src/views/wms/finance-income/index.vue`
- Modify: `erp-frontend/src/views/wms/finance-expense/index.vue`
- Modify: `erp-frontend/src/router/dynamic-routes.ts`
- Modify: `erp-backend/sql/migration/V132__fund_settlement_recharge.sql`
- Create: `erp-frontend/src/views/wms/fund-settlement/settlement-tabs.vitest.ts`

**Interfaces:**
- Consumes: operator owner/platform account, recharge, review and ledger APIs from Task 3。
- Produces: `服务商运营 -> 资金结算`，主选项卡 `owner/platform`。

- [ ] **Step 1: 编写页签与旧路由兼容失败测试**

```ts
it('maps legacy finance routes to the matching settlement tab', () => {
  expect(resolveSettlementTab('/wms/finance-income')).toBe('owner')
  expect(resolveSettlementTab('/wms/finance-expense')).toBe('platform')
})
```

- [ ] **Step 2: 实现货主结算页签**

顶部四项：账户总余额、本月已确认充值、本月物流产品费用、待审核笔数。下方页内页签为账户总览、充值审核、物流费用流水；复用原收入查询，不复制费用数据。

- [ ] **Step 3: 实现平台结算页签**

顶部四项：平台余额、累计充值、累计平台费用、本月待出账费用。下方页内页签为平台账户、充值记录、服务账单；服务账单复用原支出 API。

- [ ] **Step 4: 实现菜单迁移和旧页面兼容**

```sql
UPDATE sys_menu
SET title='资金结算', path='fund-settlement', uri='wms/fund-settlement/index',
    permission='wms:fund-settlement:read'
WHERE id=180200;

UPDATE sys_menu SET hidden=1 WHERE id=180300;
```

把 `180300` 的角色授权复制给 `180200`。旧收入/支出组件改为渲染统一页面并传入默认页签，已打开的旧标签仍可工作。

- [ ] **Step 5: 运行前端验证**

Run: `pnpm vitest run src/views/wms/fund-settlement/settlement-tabs.vitest.ts && pnpm type-check`

Expected: PASS。

- [ ] **Step 6: 提交本任务**

```bash
git add erp-frontend/src/views/wms/fund-settlement \
  erp-frontend/src/views/wms/finance-income/index.vue \
  erp-frontend/src/views/wms/finance-expense/index.vue \
  erp-frontend/src/router/dynamic-routes.ts \
  erp-backend/sql/migration/V132__fund_settlement_recharge.sql
git commit -m "feat: merge provider finance into fund settlement"
```

---

### Task 6: 海外仓平台服务商资金审核页面

**Files:**
- Create: `erp-frontend/src/views/platform-finance/provider-funds/index.vue`
- Create: `erp-frontend/src/views/platform-finance/provider-funds/ProviderFundDrawer.vue`
- Modify: `erp-frontend/src/router/dynamic-routes.ts`
- Modify: `erp-backend/sql/migration/V132__fund_settlement_recharge.sql`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/finance/settlement/ProviderFundsMenuMigrationTest.java`

**Interfaces:**
- Consumes: `/fund-settlement/platform/accounts`、`/platform/recharges`、`/platform/ledger`、审核和冲正接口。
- Produces: `平台财务 -> 服务商资金` 菜单和页面。

- [ ] **Step 1: 编写菜单迁移失败测试**

```java
@Test
void migrationAddsProviderFundsMenuAndCopiesPlatformRoleAccess() {
    String sql = readMigration();
    assertTrue(sql.contains("'服务商资金'"));
    assertTrue(sql.contains("platform-finance/provider-funds/index"));
    assertTrue(sql.contains("INSERT IGNORE INTO sys_role_menu"));
}
```

- [ ] **Step 2: 实现平台账户列表**

筛选条件：WMS 服务商、币种、余额状态、日期。表格展示累计充值、平台服务费、记录余额、待审核和最近变动；第一列和操作列固定，中间横向滚动。

- [ ] **Step 3: 实现审核与冲正抽屉**

审核抽屉展示金额、币种、付款时间、提交人、凭证预览和备注。驳回及冲正必须填写原因，按钮提交后立即禁用并等待 CAS 返回。

- [ ] **Step 4: 增加菜单和权限**

使用新菜单 ID `170603`，父菜单 `170600`，权限 `platform-finance:provider-funds:oper`；从拥有 `170601` 应收账单权限的角色复制授权。

- [ ] **Step 5: 运行菜单测试、类型检查和前端构建**

Run: `mvn -pl admin -Dtest=ProviderFundsMenuMigrationTest test`

Run: `pnpm type-check && pnpm build`

Expected: 全部 PASS，构建退出码为 0。

- [ ] **Step 6: 提交本任务**

```bash
git add erp-frontend/src/views/platform-finance/provider-funds \
  erp-frontend/src/router/dynamic-routes.ts \
  erp-backend/sql/migration/V132__fund_settlement_recharge.sql \
  erp-backend/admin/src/test/java/com/erp/admin/finance/settlement/ProviderFundsMenuMigrationTest.java
git commit -m "feat: add platform provider fund review"
```

---

### Task 7: 集成验证、数据库迁移与服务重启

**Files:**
- Modify if required by verified defects: files introduced in Tasks 1-6 only。
- Verify: `docs/superpowers/specs/2026-08-25-fund-settlement-design.md`

**Interfaces:**
- Consumes: all preceding tasks。
- Produces: 可运行的两级充值和资金结算功能。

- [ ] **Step 1: 运行全部专项后端测试**

```bash
mvn -pl admin -Dtest=FundSettlementMigrationTest,RechargeStatusPolicyTest,RechargeOrderServiceTest,FundSettlementQueryMapperSqlTest,FundSettlementControllerContractTest,ProviderFundsMenuMigrationTest test
```

Expected: 0 failures、0 errors。

- [ ] **Step 2: 运行关联财务回归测试**

```bash
mvn -pl admin -Dtest=MonthlyBillServiceTest,ServiceContractServiceTest,OperatorFinanceServiceTest,OperatorFinanceMapperSqlTest,PlatformFinanceSnapshotSchemaTest test
```

Expected: 0 failures、0 errors。

- [ ] **Step 3: 运行前端测试与生产构建**

```bash
pnpm vitest run src/views/wms/asset-finance/fund-account-policy.vitest.ts src/views/wms/fund-settlement/settlement-tabs.vitest.ts
pnpm type-check
pnpm build
```

Expected: 测试、类型检查和构建全部退出码 0。

- [ ] **Step 4: 备份并执行数据库迁移**

```powershell
docker exec erp-mysql mysqldump -uroot -p123456 erp wms_recharge_order sys_menu sys_role_menu > fund-settlement-before.sql
docker cp erp-backend/sql/migration/V132__fund_settlement_recharge.sql erp-mysql:/tmp/V132.sql
docker exec erp-mysql sh -c "mysql -uroot -p123456 --default-character-set=utf8mb4 erp < /tmp/V132.sql"
```

首次执行时 `wms_recharge_order` 尚不存在，备份命令改为只备份 `sys_menu sys_role_menu`；迁移后单独导出新表结构。

- [ ] **Step 5: 执行数据库核对**

```sql
SHOW CREATE TABLE wms_recharge_order;
SELECT id,title,path,uri,hidden FROM sys_menu WHERE id IN (170603,180200,180300);
SELECT role_code,menu_id FROM sys_role_menu WHERE menu_id IN (170603,180200);
```

Expected: 新表、索引、资金结算菜单、服务商资金菜单及角色授权均存在。

- [ ] **Step 6: 重启前后端并验证 HTTP**

后端在 `8081`、前端在 `5360` 监听；前端根路径返回 200，后端未登录根路径返回 401；启动日志不得出现 `APPLICATION FAILED` 或 `ERROR`。

- [ ] **Step 7: 手工验收两条完整链路**

```text
ERP充值 -> WMS审核 -> 货主余额增加 -> 物流产品费用扣减展示
WMS充值 -> 平台审核 -> 平台余额增加 -> 仓储服务费用扣减展示
```

同时验证：负余额不阻断业务、重复审核返回 409、跨租户查询返回 403 或空数据、冲正恢复余额、账单不重复扣款。

- [ ] **Step 8: 提交验证修正**

```bash
git add <仅限本功能验证中实际修正的文件>
git commit -m "test: verify fund settlement workflow"
```
