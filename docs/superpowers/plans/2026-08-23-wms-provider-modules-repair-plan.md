# WMS 服务商模块修复实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 WMS 服务商货主管理、物流产品、财务收支、仓储概览和运营数据分析中的普通业务错误，使库存归属、金额币种、账单状态和新履约链路的展示保持一致。

**Architecture:** 保留现有三层租户和现有菜单结构，以数据库记账记录为财务事实来源，以库存记录的 `wms_tenant_id` 为库存归属来源。财务接口改为分币种返回，仓储容量与库存归属拆开计算，物流产品采用引用保护和明确的新增/编辑接口；不引入汇率服务，不重建第二套看板。

**Tech Stack:** Java 8、Spring Boot、Spring Security、MyBatis/MyBatis-Plus、MySQL 8、Vue 3、TypeScript、Ant Design Vue、ECharts、Maven、pnpm/Vite/Vitest。

**Spec:** `docs/superpowers/tasks/2026-08-23-wms-provider-modules-audit.md`

## 实施状态（2026-08-24）

- Task 1-10 的代码、迁移和自动化回归已完成。
- Task 11 已完成数据库备份、迁移、专项测试、前端构建和本地服务重启。
- 登录后的六模块人工点选验收仍需由实际账号在页面完成；自动化和启动日志未发现本轮阻断错误。
- 当前工作区包含此前连续开发留下的其他未提交改动，本轮未擅自将无关改动一并提交。

## Global Constraints

- 金额按原币种分别展示，禁止跨币种直接求和或计算净收益。
- 俄罗斯仓业务账期使用 `Europe/Moscow`。
- 公共暂存库位库存计入服务商库存，但不计入其物理库位容量。
- 已被活动业务引用的物流产品只能停用，不能删除。
- 不恢复 `180500 服务商看板`，统一使用 `900400 运营数据分析`。
- 不修改货主、店铺、订单和历史计费记录的业务归属。
- 数据库迁移必须可重复检查，正式执行前先备份。
- 每个任务使用测试先行，完成后单独提交；不得顺带重构无关模块。

---

## 文件结构与职责

### 数据库

- `erp-backend/sql/migration/V127__provider_finance_currency_and_indexes.sql`：月度账单币种、查询索引和历史数据回填。执行时若 V127 已占用，顺延到下一个可用版本号。

### 后端

- `OperatorFinanceMapper.xml`：按币种汇总收入，并关联新旧出库业务号。
- `OperatorFinanceService.java`：生成分币种汇总，停止用当前产品价格覆盖历史计费。
- `OperatorIncomeVO.java`：定义分币种总计、历史快照和业务引用字段。
- `WmsMonthlyBill.java`、`MonthlyBillVO.java`、`WmsMonthlyBillMapper.xml`：月度账单币种传递。
- `WmsLogisticsProductController.java`：拆分新建与编辑接口及权限。
- `WmsLogisticsProductService.java`：产品编码校验和删除引用保护。
- `WmsStorageOverviewMapper.xml`：分离容量范围与库存归属范围。
- `OperatorDashboardMapper.xml`：收入按币种、支出按状态和币种聚合。
- `OperatorDashboardService.java`、`OperatorDashboardVO.java`：输出不混币种的经营分析结构。
- `FulfillmentLogisticsFeeService.java`：使用签出时间和莫斯科时区生成账期。

### 前端

- `src/api/wms/operator-finance/types.ts`：收入/支出币种类型。
- `src/views/wms/finance-income/index.vue`：分币种卡片、汇总和新履约明细。
- `src/views/wms/finance-expense/index.vue`：使用账单币种，不再固定显示卢布。
- `src/api/wms/operator-dashboard/types.ts`：分币种趋势和账单状态结构。
- `src/views/dashboard/operator/index.vue`：按币种展示收入，按状态展示 CNY 支出，不再显示错误的跨币种净收益。
- `src/api/wms/logistics-product/index.ts`：拆分 create/update API。
- `src/views/wms/logistics-product/ProductFormModal.vue`：产品编码必填并匹配后端规则。
- `src/views/wms/storage-overview/index.vue`：容量与库存口径文案修正。

---

### Task 1: 建立财务币种数据库基础

**Files:**

- Create: `erp-backend/sql/migration/V127__provider_finance_currency_and_indexes.sql`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/platform/finance/model/entity/WmsMonthlyBill.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/platform/finance/model/vo/MonthlyBillVO.java`
- Modify: `erp-backend/admin/src/main/resources/mapper/financial/WmsMonthlyBillMapper.xml`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/platform/MonthlyBillCurrencySchemaTest.java`

**Interfaces:**

- Produces: `WmsMonthlyBill.currency: String`、`MonthlyBillVO.currency: String`。
- Produces: `wms_monthly_bill.currency VARCHAR(8) NOT NULL DEFAULT 'CNY'`。
- Consumes: 现有平台仓储作业费统一以 CNY 记账的业务口径。

- [ ] **Step 1: 写迁移结构测试**

测试读取迁移文件并断言包含以下语义：

```java
assertThat(sql).contains("ADD COLUMN currency");
assertThat(sql).contains("DEFAULT 'CNY'");
assertThat(sql).contains("idx_monthly_bill_tenant_month_status_currency");
```

- [ ] **Step 2: 运行测试并确认失败**

Run:

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=MonthlyBillCurrencySchemaTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL，原因是迁移文件或字段尚不存在。

- [ ] **Step 3: 编写数据库迁移**

迁移必须完成：

```sql
ALTER TABLE wms_monthly_bill
  ADD COLUMN currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '账单币种' AFTER total_amount;

UPDATE wms_monthly_bill SET currency = 'CNY'
WHERE currency IS NULL OR TRIM(currency) = '';

CREATE INDEX idx_monthly_bill_tenant_month_status_currency
  ON wms_monthly_bill (wms_tenant_id, bill_month, status, currency, deleted);

CREATE INDEX idx_client_billing_tenant_month_currency
  ON wms_client_billing_record (wms_tenant_id, bill_month, currency, fee_type);
```

执行前查询 `information_schema.statistics`，如果同名等价索引已存在则不要重复创建。

- [ ] **Step 4: 贯通实体、VO 和 Mapper**

在实体和 VO 增加：

```java
@Schema(title = "币种")
private String currency;
```

所有月度账单 INSERT 明确写入 `CNY`，所有详情和分页 SELECT 返回 `currency`。

- [ ] **Step 5: 运行后端测试**

Run:

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=MonthlyBillCurrencySchemaTest,MonthlyBillServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS。

- [ ] **Step 6: 提交**

```powershell
git add erp-backend/sql/migration erp-backend/admin/src/main/java/com/erp/admin/platform/finance erp-backend/admin/src/main/resources/mapper erp-backend/admin/src/test/java/com/erp/admin/platform/finance
git commit -m "fix: add currency to provider expense bills"
```

---

### Task 2: 修复服务商收入汇总和收入明细

**Files:**

- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/OperatorIncomeVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/OperatorFinanceMapper.java`
- Modify: `erp-backend/admin/src/main/resources/mapper/wms/OperatorFinanceMapper.xml`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/OperatorFinanceService.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/OperatorFinanceMapperSqlTest.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/OperatorFinanceServiceTest.java`

**Interfaces:**

- Produces: `OperatorIncomeVO.CurrencyTotal { String currency; BigDecimal amount; }`，使用 `@Data @NoArgsConstructor @AllArgsConstructor`。
- Produces: `Summary.currencyTotals: List<CurrencyTotal>`，移除含义错误的单一 `totalAmount`。
- Produces: `SummaryRow.currency`、`Record.currency`、`Record.businessNo`、`Record.platformOrderId`、`Record.productNameSnapshot`。

- [ ] **Step 1: 写 SQL 结构失败测试**

断言收入汇总 SQL：

```java
assertThat(xml).contains("r.currency AS currency");
assertThat(xml).contains("GROUP BY r.logistics_product_id, r.bill_month, r.currency");
assertThat(xml).contains("LEFT JOIN wms_fulfillment_order");
assertThat(xml).contains("COALESCE(f.fulfillment_no, o.outbound_no)");
```

- [ ] **Step 2: 写 Service 行为失败测试**

构造一条 `100 RUB` 和一条 `20 CNY`，断言返回两个币种总计而不是 `120`：

```java
assertThat(summary.getCurrencyTotals()).containsExactlyInAnyOrder(
    new CurrencyTotal("RUB", new BigDecimal("100.00")),
    new CurrencyTotal("CNY", new BigDecimal("20.00"))
);
```

- [ ] **Step 3: 运行测试并确认失败**

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=OperatorFinanceMapperSqlTest,OperatorFinanceServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 4: 修改 SQL 查询**

收入汇总必须按 `product_id + bill_month + currency` 分组。收入明细采用：

```sql
LEFT JOIN wms_fulfillment_order f ON f.id = r.fulfillment_order_id
LEFT JOIN wms_sales_outbound_order o ON o.id = r.outbound_order_id
```

业务号取值顺序：`f.fulfillment_no -> o.outbound_no -> f.source_order_no -> r.biz_id`。平台订单号使用 `f.source_order_no`。产品名和说明优先使用计费记录快照，快照为空才回退当前产品表。

- [ ] **Step 5: 修改 VO 和 Service**

Service 只聚合相同 `currency` 的金额。`enrichProductInfo` 不得把当前产品单价写成历史实际单价；当前产品名称只能作为旧记录无快照时的回退值。

- [ ] **Step 6: 运行测试并确认通过**

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=OperatorFinanceMapperSqlTest,OperatorFinanceServiceTest,FulfillmentLogisticsFeeServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 7: 提交**

```powershell
git add erp-backend/admin/src/main/java/com/erp/admin/wms erp-backend/admin/src/main/resources/mapper/wms/OperatorFinanceMapper.xml erp-backend/admin/src/test/java/com/erp/admin/wms
git commit -m "fix: separate provider income by currency"
```

---

### Task 3: 修复服务商财务收入和支出前端

**Files:**

- Modify: `erp-frontend/src/api/wms/operator-finance/types.ts`
- Modify: `erp-frontend/src/api/wms/operator-finance/index.ts`
- Modify: `erp-frontend/src/views/wms/finance-income/index.vue`
- Modify: `erp-frontend/src/views/wms/finance-expense/index.vue`
- Create: `erp-frontend/src/views/wms/finance-income/currency-summary.vitest.ts`

**Interfaces:**

- Consumes: Task 1 的 `ExpenseBillVO.currency`。
- Consumes: Task 2 的 `IncomeSummary.currencyTotals` 和记录快照字段。
- Produces: `formatMoney(amount, currency)`，统一使用 `Intl.NumberFormat`。

- [ ] **Step 1: 写金额格式测试**

覆盖 RUB、CNY、USD，并断言不存在固定 `₽`：

```ts
expect(formatMoney(120, 'RUB')).toContain('₽')
expect(formatMoney(120, 'CNY')).toContain('CN¥')
expect(formatMoney(120, 'USD')).toContain('$')
```

- [ ] **Step 2: 运行测试并确认失败**

```powershell
cd erp-frontend
pnpm exec vitest run src/views/wms/finance-income/currency-summary.vitest.ts
```

- [ ] **Step 3: 修改收入页面**

- 顶部每个币种显示一张紧凑统计卡；
- 汇总表增加“币种”；
- 金额列用记录自身币种格式化；
- 明细增加“履约单号/旧出库单号”“平台订单号”；
- 产品名称显示记账快照，已删除产品仍可读；
- 不显示跨币种总计。

- [ ] **Step 4: 修改支出页面**

- 账单列表和详情使用 `bill.currency`；
- 默认 CNY 显示人民币符号；
- 删除所有固定卢布符号；
- 状态文案保持 DRAFT=草稿估算、CONFIRMED=已确认应付、DISPUTED=争议中、PAID=已付款。

- [ ] **Step 5: 运行前端验证**

```powershell
cd erp-frontend
pnpm exec vitest run src/views/wms/finance-income/currency-summary.vitest.ts
pnpm type-check
```

Expected: 测试和类型检查通过。

- [ ] **Step 6: 提交**

```powershell
git add erp-frontend/src/api/wms/operator-finance erp-frontend/src/views/wms/finance-income erp-frontend/src/views/wms/finance-expense
git commit -m "fix: display provider finance in original currencies"
```

---

### Task 4: 保护物流产品删除并统一产品编码

**Files:**

- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/LogisticsProductDTO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/WmsLogisticsProductService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/WmsLogisticsProductMapper.java`
- Modify: `erp-backend/admin/src/main/resources/mapper/wms/WmsLogisticsProductMapper.xml`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/WmsLogisticsProductServiceTest.java`

**Interfaces:**

- Produces: `createProduct(LogisticsProductDTO dto)`。
- Produces: `updateProduct(Long id, LogisticsProductDTO dto)`。
- Produces: `countActiveReferences(Long wmsTenantId, Long productId)`，返回店铺默认、待完成履约、旧出库活动引用数量。

- [ ] **Step 1: 写删除保护失败测试**

覆盖三种普通场景：

```java
// 店铺默认产品：删除失败
// 待下架/拣货/待签出履约单：删除失败
// 无活动引用、只有历史计费快照：允许删除
```

错误消息必须明确，例如：`物流产品正在被 4 个店铺和 6 个未完成订单使用，请先更换默认产品或停用该产品`。

- [ ] **Step 2: 写编码校验失败测试**

覆盖空编码、空格、小写标准化和同一服务商重复编码。不同服务商允许相同编码。

- [ ] **Step 3: 运行测试并确认失败**

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=WmsLogisticsProductServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 4: 实现编码规则**

DTO 增加：

```java
@NotBlank(message = "产品编码不能为空")
private String productCode;
```

Service 保存前统一 `trim().toUpperCase(Locale.ROOT)`，并将数据库唯一冲突转换为业务提示。

- [ ] **Step 5: 实现删除引用保护**

删除前分别查询：

- `shop.default_logistics_product_id = productId`；
- `wms_fulfillment_order.logistics_product_id = productId` 且状态不是 `SHIPPED/CANCELLED`；
- 旧销售出库表中仍未完成的引用。

存在任意活动引用时拒绝删除；停用操作仍允许，并且历史单据继续使用快照展示。

- [ ] **Step 6: 运行测试并确认通过**

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=WmsLogisticsProductServiceTest,ShopDefaultLogisticsProductTest,LogisticsProductSnapshotModelTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 7: 提交**

```powershell
git add erp-backend/admin/src/main/java/com/erp/admin/wms erp-backend/admin/src/main/resources/mapper/wms/WmsLogisticsProductMapper.xml erp-backend/admin/src/test/java/com/erp/admin/wms
git commit -m "fix: protect logistics products referenced by active business"
```

---

### Task 5: 拆分物流产品新建和编辑权限

**Files:**

- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/WmsLogisticsProductController.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/WmsLogisticsProductService.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/WmsLogisticsProductControllerTest.java`
- Modify: `erp-frontend/src/api/wms/logistics-product/index.ts`
- Modify: `erp-frontend/src/views/wms/logistics-product/ProductFormModal.vue`

**Interfaces:**

- Produces: `POST /wms/logistics-product`，仅新建，要求 add 权限。
- Produces: `PUT /wms/logistics-product/{id}`，仅编辑，要求 edit 权限。
- Consumes: Task 4 的 `createProduct` 和 `updateProduct`。

- [ ] **Step 1: 写权限失败测试**

断言：

```text
add-only 用户：POST 成功，PUT 拒绝
edit-only 用户：POST 拒绝，PUT 成功
PUT 请求体中的 id 被忽略，以路径 id 为准
```

- [ ] **Step 2: 运行测试并确认失败**

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=WmsLogisticsProductControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 3: 拆分后端接口**

```java
@PostMapping
@PreAuthorize("@per.hasPermission('wms:logistics-product:add')")
public ApiResult<Void> create(@Validated @RequestBody LogisticsProductDTO dto)

@PutMapping("/{id}")
@PreAuthorize("@per.hasPermission('wms:logistics-product:edit')")
public ApiResult<Void> update(@PathVariable Long id,
        @Validated @RequestBody LogisticsProductDTO dto)
```

- [ ] **Step 4: 修改前端 API 和表单**

新建调用 POST，编辑调用 PUT。产品编码加必填星号、必填规则和“例如 STANDARD”提示；失焦时转大写。

- [ ] **Step 5: 运行后端与前端验证**

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=WmsLogisticsProductControllerTest,WmsLogisticsProductServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
cd ..\erp-frontend
pnpm type-check
```

- [ ] **Step 6: 提交**

```powershell
git add erp-backend/admin/src/main/java/com/erp/admin/wms erp-backend/admin/src/test/java/com/erp/admin/wms erp-frontend/src/api/wms/logistics-product erp-frontend/src/views/wms/logistics-product
git commit -m "fix: separate logistics product create and update permissions"
```

---

### Task 6: 修复仓储概览的容量和库存归属

**Files:**

- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/WmsStorageOverviewMapper.java`
- Modify: `erp-backend/admin/src/main/resources/mapper/wms/WmsStorageOverviewMapper.xml`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/WmsStorageOverviewService.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/WmsStorageOverviewMapperSqlTest.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/WmsStorageOverviewServiceTest.java`
- Modify: `erp-frontend/src/views/wms/storage-overview/index.vue`

**Interfaces:**

- Produces: 仓库行中的 `allocatedLocations` 仅表示有效分配范围。
- Produces: `onHandQty`、`skuCount`、货主明细严格来自 `pi.wms_tenant_id = 当前服务商`。
- Produces: 公共暂存库存计入库存总量，但不暴露暂存库位编码。

- [ ] **Step 1: 写四组 SQL 失败测试**

测试数据口径：

1. 本服务商分配库位中的本服务商库存：计入容量和库存；
2. 公共暂存库位中的本服务商库存：只计入库存；
3. 同一库位中的其他服务商库存：不得计入；
4. 货架重新分配后残留的旧服务商库存：按库存自身归属统计，不随新分配串到新服务商。

- [ ] **Step 2: 运行测试并确认失败**

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=WmsStorageOverviewMapperSqlTest,WmsStorageOverviewServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 3: 重写仓库汇总 CTE**

查询拆成两组：

```sql
allocated_locations -- 只按有效 rack assignment
owned_inventory      -- WHERE pi.wms_tenant_id = #{wmsTenantId} AND pi.quantity > 0
```

仓库列表包含“有有效分配”或“有本服务商库存”的仓库。公共暂存库位只参与 `owned_inventory`，不参与容量分母。

- [ ] **Step 4: 修复货主明细**

必须同时满足：

```sql
pi.wms_tenant_id = #{wmsTenantId}
AND t.parent_wms_tenant_id = #{wmsTenantId}
AND pi.quantity > 0
```

货主占用库位数可以统计内部数量，但返回服务商前不得返回公共暂存库位名称。

- [ ] **Step 5: 修正文案**

前端将“分配货架数”改为“已分配排数”，将库存统计解释为“包含公共暂存库存”，但不展示公共暂存位置。

- [ ] **Step 6: 运行验证**

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=WmsStorageOverviewMapperSqlTest,WmsStorageOverviewServiceTest,LocationInventoryQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
cd ..\erp-frontend
pnpm type-check
```

- [ ] **Step 7: 提交**

```powershell
git add erp-backend/admin/src/main/java/com/erp/admin/wms erp-backend/admin/src/main/resources/mapper/wms/WmsStorageOverviewMapper.xml erp-backend/admin/src/test/java/com/erp/admin/wms erp-frontend/src/views/wms/storage-overview
git commit -m "fix: separate provider capacity from inventory ownership"
```

---

### Task 7: 修复运营数据分析的币种和账单状态

**Files:**

- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/OperatorDashboardVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/OperatorDashboardMapper.java`
- Modify: `erp-backend/admin/src/main/resources/mapper/wms/OperatorDashboardMapper.xml`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/OperatorDashboardService.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/OperatorDashboardCurrencyTest.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/OperatorDashboardMapperSqlTest.java`

**Interfaces:**

- Produces: `overview.incomeByCurrency`。
- Produces: `overview.expenseSummary { currency, draftEstimate, confirmedPayable, disputedAmount, paidAmount }`。
- Produces: `trend.incomeSeries[]`，每个序列包含 `currency` 和与月份对齐的金额数组。
- Produces: 仅同币种生成余额；不得返回单一跨币种 `netProfit`。

- [ ] **Step 1: 写混币种失败测试**

RUB 100、CNY 20 的看板必须返回两个收入序列，不能返回 120。CNY 账单状态分别写入 DRAFT、CONFIRMED、DISPUTED、PAID，断言四类金额分开。

- [ ] **Step 2: 运行测试并确认失败**

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=OperatorDashboardCurrencyTest,OperatorDashboardMapperSqlTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 3: 修改收入 SQL**

所有收入查询按 `currency` 分组：

```sql
GROUP BY r.bill_month, r.currency
GROUP BY r.logistics_product_id, r.currency
GROUP BY r.erp_tenant_id, t.tenant_name, r.currency
```

- [ ] **Step 4: 修改支出 SQL**

按 `bill_month + currency + status` 返回金额。状态口径固定为：

- DRAFT：草稿估算；
- CONFIRMED：已确认应付；
- DISPUTED：争议金额，单列，不计入已确认支出；
- PAID：已付款，同时属于已确认成本，但“已确认应付”卡片不得重复加总。

区间已确认成本口径为 `CONFIRMED + PAID`；现金支出口径只取 `PAID`。

- [ ] **Step 5: 重构 VO 和 Service 聚合**

删除 `income.subtract(expense)` 的跨币种路径。只有同币种存在收入和确认成本时才生成 `CurrencyBalance`：

```java
balance = incomeInCurrency.subtract(confirmedAndPaidExpenseInCurrency);
```

- [ ] **Step 6: 运行测试并确认通过**

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=OperatorDashboardCurrencyTest,OperatorDashboardMapperSqlTest,OperatorDashboardMenuMigrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 7: 提交**

```powershell
git add erp-backend/admin/src/main/java/com/erp/admin/wms erp-backend/admin/src/main/resources/mapper/wms/OperatorDashboardMapper.xml erp-backend/admin/src/test/java/com/erp/admin/wms
git commit -m "fix: make provider dashboard currency and status aware"
```

---

### Task 8: 改造运营数据分析前端并确认重复看板保持删除

**Files:**

- Modify: `erp-frontend/src/api/wms/operator-dashboard/types.ts`
- Modify: `erp-frontend/src/views/dashboard/operator/index.vue`
- Create: `erp-frontend/src/views/dashboard/operator/operator-dashboard.vitest.ts`
- Verify: `erp-backend/sql/migration/V126__remove_duplicate_provider_dashboard.sql`
- Verify: `erp-backend/admin/src/test/java/com/erp/admin/wms/OperatorDashboardMenuMigrationTest.java`

**Interfaces:**

- Consumes: Task 7 的分币种收入、分状态支出和同币种余额。
- Produces: 一套唯一的服务商运营分析页面。

- [ ] **Step 1: 写页面数据转换失败测试**

断言 RUB/CNY 分别生成图例，DRAFT 不计入确认成本，页面代码中不存在固定 `₽` 和跨币种 `income - expense`。

- [ ] **Step 2: 运行测试并确认失败**

```powershell
cd erp-frontend
pnpm exec vitest run src/views/dashboard/operator/operator-dashboard.vitest.ts
```

- [ ] **Step 3: 修改经营总览**

卡片按以下信息展示：

- 物流产品收入：按币种分行；
- 已确认平台费用：按币种分行；
- 已付款：按币种分行；
- 草稿估算、争议金额：独立提示；
- 同币种经营余额：只在可计算时显示。

- [ ] **Step 4: 修改趋势和排行**

- 趋势图每个收入币种一条线；
- 支出至少区分“确认成本”和“已付款”；
- 产品收入排行和货主贡献排行在金额旁显示币种；
- 不同币种不能共用一个按金额排序的 TOP10；按币种分组或由用户选择币种后排序。

- [ ] **Step 5: 验证菜单唯一性**

运行 `OperatorDashboardMenuMigrationTest`，确认前端路由和菜单没有重新引用 `180500`，只保留 `900400`。

- [ ] **Step 6: 运行前后端验证**

```powershell
cd erp-frontend
pnpm exec vitest run src/views/dashboard/operator/operator-dashboard.vitest.ts
pnpm type-check
cd ..\erp-backend
mvn -pl admin -am -Dtest=OperatorDashboardMenuMigrationTest,OperatorDashboardCurrencyTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 7: 提交**

```powershell
git add erp-frontend/src/api/wms/operator-dashboard erp-frontend/src/views/dashboard/operator
git commit -m "fix: present provider operations without currency mixing"
```

---

### Task 9: 统一出库计费账期时区

**Files:**

- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentLogisticsFeeService.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentLogisticsFeeServiceTest.java`

**Interfaces:**

- Produces: `resolveBillMonth(LocalDateTime shippedTime): String`。
- 规则: 有签出时间时按 `Europe/Moscow` 转换；无签出时间时使用莫斯科当前时间。

- [ ] **Step 1: 写跨月失败测试**

使用北京时间 `2026-09-01 01:30:00`，对应莫斯科时间仍为 `2026-08-31 20:30:00`，账期必须为 `2026-08`。

- [ ] **Step 2: 运行测试并确认失败**

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=FulfillmentLogisticsFeeServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 3: 实现显式时区**

```java
private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Moscow");
```

数据库 `LocalDateTime` 按系统存储口径转换为 `ZonedDateTime` 后取 `YearMonth`。不得继续直接调用无时区的 `YearMonth.now()`。

- [ ] **Step 4: 运行测试并确认通过**

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=FulfillmentLogisticsFeeServiceTest,FulfillmentShippingServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 5: 提交**

```powershell
git add erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentLogisticsFeeService.java erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentLogisticsFeeServiceTest.java
git commit -m "fix: calculate fulfillment bill month in Moscow time"
```

---

### Task 10: 补齐货主管理回归保护

**Files:**

- Modify: `erp-backend/admin/src/test/java/com/erp/admin/tenant/TenantProvisionServiceTest.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/ProviderOwnerIsolationTest.java`

**Interfaces:**

- Verifies: 服务商只能查看自己名下货主。
- Verifies: 非管理员不能新建、编辑或停用货主。
- Verifies: 货主停用后现有会话在下一次请求被拒绝。

- [ ] **Step 1: 增加隔离和停用测试**

必须覆盖两个服务商、各一个货主；服务商 A 查询和写操作不能命中服务商 B 的货主。

- [ ] **Step 2: 运行测试**

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=TenantProvisionServiceTest,ProviderOwnerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 如果现有行为完全正确应直接 PASS；若失败，只修复测试揭示的作用域问题，不改变货主管理流程。

- [ ] **Step 3: 提交**

```powershell
git add erp-backend/admin/src/test/java/com/erp/admin
git commit -m "test: protect provider owner tenant isolation"
```

---

### Task 11: 全链路回归、数据核验和发布

**Files:**

- Create: `docs/operations/wms-provider-finance-release-checklist.md`
- Update: `docs/superpowers/plans/2026-08-23-wms-provider-modules-repair-plan.md`（勾选实际完成项）

**Interfaces:**

- Consumes: Task 1-10 的全部接口和迁移。
- Produces: 可审计的发布记录、数据库核验结果和回滚点。

- [ ] **Step 1: 备份数据库并记录基线**

至少导出以下表：

```text
wms_monthly_bill
wms_client_billing_record
wms_logistics_product
shop
wms_fulfillment_order
wms_physical_inventory
wms_rack_assignment
sys_menu
sys_role_menu
```

- [ ] **Step 2: 在测试库执行迁移**

核验：

```sql
SELECT currency, COUNT(*), SUM(total_amount)
FROM wms_monthly_bill
GROUP BY currency;

SELECT currency, COUNT(*), SUM(amount)
FROM wms_client_billing_record
GROUP BY currency;

SELECT COUNT(*) FROM sys_menu WHERE id = 180500 AND deleted = 0;
```

Expected: 月度账单历史值为 CNY；计费记录按原币种保留；服务商看板活动菜单数为 0。

- [ ] **Step 3: 运行后端完整专项测试**

```powershell
cd erp-backend
mvn -pl admin -am -Dtest=TenantProvisionServiceTest,ProviderOwnerIsolationTest,WmsLogisticsProductServiceTest,WmsLogisticsProductControllerTest,ShopDefaultLogisticsProductTest,LogisticsProductSnapshotModelTest,FulfillmentLogisticsFeeServiceTest,OperatorFinanceMapperSqlTest,OperatorFinanceServiceTest,MonthlyBillCurrencySchemaTest,MonthlyBillServiceTest,WmsStorageOverviewMapperSqlTest,WmsStorageOverviewServiceTest,OperatorDashboardMapperSqlTest,OperatorDashboardCurrencyTest,OperatorDashboardMenuMigrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 0 failures，0 errors。

- [ ] **Step 4: 运行前端验证**

```powershell
cd erp-frontend
pnpm exec vitest run src/views/wms/finance-income/currency-summary.vitest.ts src/views/dashboard/operator/operator-dashboard.vitest.ts
pnpm type-check
pnpm build-only
```

- [ ] **Step 5: 人工验收六个模块**

1. 货主管理：服务商只能看到自己货主，停用后货主无法继续访问。
2. 物流产品：编码必填；新增和编辑权限独立；被店铺或未完成订单引用时删除失败，停用成功。
3. 财务收入：RUB/CNY/USD 分开显示；新履约单号、平台订单号和历史产品快照完整。
4. 财务支出：显示 CNY；草稿、争议、已确认、已付款含义清楚。
5. 仓储概览：公共暂存库存计入本服务商库存，其他服务商库存不串入，容量只按已分配排统计。
6. 运营数据分析：只有一个菜单；收入按币种；支出按状态；没有跨币种净收益。

- [ ] **Step 6: 重启并检查日志**

按项目现有启动方式重启后端和前端。检查后端启动日志无 Flyway/SQL/Mapper 错误，浏览器控制台无接口类型和图表错误。

- [ ] **Step 7: 准备回滚**

应用回滚使用本轮任务开始前 Git 提交。数据库回滚不删除已经写入的 `currency` 数据；若必须回退应用，保留新增字段和索引兼容旧版本。只有在确认没有新账单依赖字段时才考虑删除列。

- [ ] **Step 8: 最终提交**

```powershell
git add docs/operations docs/superpowers/plans/2026-08-23-wms-provider-modules-repair-plan.md
git commit -m "docs: record provider module release verification"
```

---

## 实施顺序与优先级

1. **第一批，阻断错误记账：** Task 1、2、3、9。
2. **第二批，阻断业务失效和串数据：** Task 4、5、6。
3. **第三批，统一经营分析：** Task 7、8。
4. **第四批，回归和发布：** Task 10、11。

Task 1 完成前不要发布 Task 3 和 Task 7；Task 2 完成前不要发布新版收入页面；Task 7 与 Task 8 必须同批发布，避免前后端数据结构不一致。

## 验收结论标准

只有同时满足以下条件才可标记本计划完成：

- 所有专项测试通过；
- 前端类型检查和构建通过；
- 数据库历史账单币种回填完成且金额不变；
- 不同币种不再出现在同一金额合计中；
- 活动业务引用的物流产品无法删除；
- 公共暂存库存不漏计且不同服务商不串计；
- 新履约收入能展示业务单号和平台订单号；
- 运营数据分析是唯一服务商看板入口；
- 六个模块的普通人工操作全部通过。
