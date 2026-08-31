# 服务商结算统一实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将海外仓平台的服务商资金账户、充值审核、资金流水和月度服务对账单合并为“服务商结算”，并支持逐账单补收/冲减和可审计复核。

**Architecture:** 复用现有 `wms_recharge_order`、`wms_billing_record` 和 `wms_monthly_bill`。账单调整作为带正负金额的正式 `wms_billing_record` 写入并绑定账单；月度账单只做汇总与复核，不再次扣减余额。前端以现有服务商资金页面为统一入口，复用月度账单详情和费率接口。

**Tech Stack:** Java 8、Spring Boot、MyBatis-Plus、MySQL 8、Vue 3、TypeScript、Ant Design Vue、Vitest、JUnit 5、Mockito。

**Spec:** `docs/superpowers/specs/2026-08-25-provider-settlement-unification-design.md`

## Global Constraints

- 系统仅记录充值、收费和复核，不发起真实支付。
- 余额始终等于审核通过充值减去 `POSTED` 服务费，不得按月度账单再次扣减。
- 原始自动计费不可覆盖；纠错只能增加补收或冲减记录。
- 已复核账单不可调整、重算或再次复核。
- 不同币种不折算、不合并。

---

### Task 1: 拣货任务缓存页自动刷新

**Files:**
- Create: `erp-frontend/src/views/platform/fulfillment-picking/reactivation-refresh.ts`
- Create: `erp-frontend/src/views/platform/fulfillment-picking/reactivation-refresh.vitest.ts`
- Modify: `erp-frontend/src/views/platform/fulfillment-picking/FulfillmentPickingPage.vue`

**Interfaces:**
- Produces: `createReactivationRefresh(refresh)`，首次激活跳过，后续缓存激活执行 `refresh`。

- [x] **Step 1: 编写首次激活不重复、后续激活刷新的失败测试。**
- [x] **Step 2: 运行 Vitest，确认缺少刷新控制器时失败。**
- [x] **Step 3: 实现刷新控制器并接入 `onActivated`。**
- [x] **Step 4: 运行 Vitest 与 `vue-tsc --noEmit`，确认通过。**

### Task 2: 账单复核和调整数据结构

**Files:**
- Create: `erp-backend/sql/migration/V133__unify_provider_settlement.sql`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/platform/finance/model/entity/WmsMonthlyBill.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/platform/finance/model/vo/MonthlyBillVO.java`
- Modify: `erp-backend/admin/src/main/resources/mapper/financial/WmsMonthlyBillMapper.xml`

**Interfaces:**
- Produces: `reviewerId`、`reviewerName`、`confirmedTime` 复核快照字段。
- Produces: 统一菜单“服务商结算”，隐藏旧应收账单菜单。

- [ ] **Step 1: 增加 schema 测试，断言迁移包含复核字段与菜单调整。**
- [ ] **Step 2: 运行测试并确认当前迁移缺失而失败。**
- [ ] **Step 3: 新增 V133，增加复核字段、索引并迁移菜单。**
- [ ] **Step 4: 映射实体、VO 和查询 SQL。**
- [ ] **Step 5: 运行 schema 测试确认通过。**

### Task 3: 逐账单补收与冲减

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/platform/finance/model/dto/BillAdjustmentDTO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/platform/finance/service/WarehouseBillingService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/platform/finance/service/MonthlyBillService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/platform/finance/controller/MonthlyBillController.java`
- Modify: `erp-backend/admin/src/test/java/com/erp/admin/platform/MonthlyBillServiceTest.java`

**Interfaces:**
- Consumes: `BillAdjustmentDTO { billId, adjustmentType, feeCode, amount, sourceRef, remark }`。
- Produces: `POST /platform-finance/monthly-bill/{id}/adjustment`。

- [ ] **Step 1: 编写测试，覆盖待复核可调整、已复核拒绝、冲减写入负金额和调整后重算。**
- [ ] **Step 2: 运行测试确认失败。**
- [ ] **Step 3: 新增 DTO，并由账单 ID 推导服务商、账期和账单归属。**
- [ ] **Step 4: 写入 `BILL_ADJUSTMENT` 正式流水；补收为正、冲减为负。**
- [ ] **Step 5: 重算当前账单并返回最新详情。**
- [ ] **Step 6: 运行服务测试确认通过。**

### Task 4: 可审计账单复核

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/platform/finance/mapper/WmsMonthlyBillMapper.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/platform/finance/service/MonthlyBillService.java`
- Modify: `erp-backend/admin/src/test/java/com/erp/admin/platform/MonthlyBillServiceTest.java`

**Interfaces:**
- Produces: `confirmIfPending(id, reviewerId, reviewerName, time)` 条件更新。

- [ ] **Step 1: 编写复核人快照和并发 CAS 测试。**
- [ ] **Step 2: 运行测试确认旧接口无法满足。**
- [ ] **Step 3: 注入 `PrincipalAttributeAccessor` 并保存复核人快照。**
- [ ] **Step 4: 运行月度账单测试确认通过。**

### Task 5: 统一服务商结算前端

**Files:**
- Modify: `erp-frontend/src/views/platform-finance/provider-funds/index.vue`
- Modify: `erp-frontend/src/views/platform-finance/receivable-bill/components/ReceivableBillDrawer.vue`
- Create: `erp-frontend/src/views/platform-finance/receivable-bill/components/BillAdjustmentModal.vue`
- Modify: `erp-frontend/src/api/platform-finance/receivable/index.ts`
- Modify: `erp-frontend/src/api/platform-finance/receivable/types.ts`
- Modify: `erp-frontend/src/views/platform-finance/receivable-bill/constants.ts`

**Interfaces:**
- Consumes: 资金账户、充值、流水、月度账单分页、详情、调整和复核接口。
- Produces: 原型对应的统一页面与逐条 `查看 / 调整费用 / 复核` 操作。

- [ ] **Step 1: 增加状态与操作策略测试，断言只有待复核/有异议允许调整和复核。**
- [ ] **Step 2: 运行 Vitest 确认失败。**
- [ ] **Step 3: 扩展 API 类型和调整接口。**
- [ ] **Step 4: 实现逐账单调整弹窗，强制填写类型、金额、业务凭证和原因。**
- [ ] **Step 5: 修改详情抽屉，移除付款入口，展示复核人、调整记录和最终应收。**
- [ ] **Step 6: 在资金页下方集成月度对账单筛选和列表。**
- [ ] **Step 7: 运行 Vitest 和 TypeScript 检查。**

### Task 6: 数据迁移与端到端验证

**Files:**
- Modify: 当前开发数据库 schema 与 `sys_menu` 数据。

- [ ] **Step 1: 备份受影响表结构和菜单数据。**
- [ ] **Step 2: 执行 V133 并验证复核字段、菜单可见性和唯一索引。**
- [ ] **Step 3: 运行后端定向测试和编译。**
- [ ] **Step 4: 运行前端测试、类型检查和生产构建。**
- [ ] **Step 5: 重启前后端，验证列表切回刷新、调整费用、复核锁定和账户余额口径。**
