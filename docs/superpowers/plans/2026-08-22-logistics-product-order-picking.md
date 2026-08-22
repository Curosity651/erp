# 物流产品与订单级取货签出实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有订单级履约内核上，实现 WMS 服务商物流产品、店铺默认产品、逐单取货贴单打包、批量签出和双边账务。

**Architecture:** 沿用 `wms_fulfillment_order` 一单一包裹和 `WAITING_PACK` 状态，不重建履约内核。物流产品属于 WMS 服务商，ERP 店铺保存默认产品，提交履约时把产品名称、说明、币种和费用写入不可变快照。海外仓按任务顺序逐单扫库位和 SKU，每单取齐后立即打印并复核面单、打包，最后对已打包订单批量签出。

**Tech Stack:** Java 8/11, Spring Boot, MyBatis-Plus, MySQL 8, JUnit 5/Mockito, Vue 3, TypeScript, Ant Design Vue, Vitest.

**Spec:** `docs/superpowers/specs/2026-08-22-logistics-product-order-picking-design.md`

## Global Constraints

- 一张平台订单或人工订单只对应一个履约订单和一个物理包裹。
- 不新增 `WAITING_LABEL`；沿用 `WAITING_PACK` 表示待面单复核和打包。
- 拣货任务只能包含同仓库、同 WMS 服务商、同 ERP 货主的订单。
- 任务内严格逐单操作，当前订单未打包不能进入下一单。
- 每张签出订单必须记录承运商、运输方式、跟踪号和实际重量。
- 签出后不跟踪物流轨迹。
- ERP 向 WMS 服务商支付物流产品费；WMS 服务商向海外仓平台支付仓内出库作业费。
- 批量下架和批量签出均按单返回成功或失败，允许部分成功。
- 所有库存扣减、平台动作和计费必须幂等。

---

### Task 1: 数据库与核心模型扩展

**Files:**
- Create: `erp-backend/sql/migration/V116__logistics_product_fulfillment_snapshot.sql`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/WmsLogisticsProduct.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/LogisticsProductDTO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/LogisticsProductVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/shop/model/entity/Shop.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/shop/model/dto/CreateOrUpdateShopRequest.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/shop/model/vo/ShopDetailVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/WmsFulfillmentOrder.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/FulfillmentCreateCommand.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/LogisticsProductSnapshotModelTest.java`

**Interfaces:**
- Produces: `defaultLogisticsProductId` on `Shop`.
- Produces: `logisticsProductId/code/name/description/defaultFee/actualFee/currency/feeAdjustmentReason` on fulfillment order and create command.

- [ ] **Step 1: Write the failing model contract test**

```java
@Test
void fulfillmentCreateCommandCarriesImmutableProductSnapshot() {
    FulfillmentCreateCommand command = new FulfillmentCreateCommand();
    command.setLogisticsProductId(7L);
    command.setLogisticsProductName("经济派送");
    command.setLogisticsProductDefaultFee(new BigDecimal("35.00"));
    assertEquals(7L, command.getLogisticsProductId());
    assertEquals("经济派送", command.getLogisticsProductName());
    assertEquals(new BigDecimal("35.00"), command.getLogisticsProductDefaultFee());
}
```

- [ ] **Step 2: Run the test and verify compilation fails on missing fields**

Run: `mvn -pl admin -Dtest=LogisticsProductSnapshotModelTest test`

- [ ] **Step 3: Add schema columns and indexes**

```sql
ALTER TABLE wms_logistics_product
  ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'RUB' AFTER unit_price,
  ADD COLUMN product_description VARCHAR(2000) NULL AFTER currency,
  ADD UNIQUE KEY uk_logistics_product_code (wms_tenant_id, product_code, deleted);

ALTER TABLE shop ADD COLUMN default_logistics_product_id BIGINT NULL AFTER default_wms_warehouse_id;

ALTER TABLE wms_fulfillment_order
  ADD COLUMN logistics_product_id BIGINT NULL,
  ADD COLUMN logistics_product_code VARCHAR(50) NULL,
  ADD COLUMN logistics_product_name VARCHAR(100) NULL,
  ADD COLUMN logistics_product_description VARCHAR(2000) NULL,
  ADD COLUMN logistics_product_default_fee DECIMAL(12,2) NULL,
  ADD COLUMN logistics_product_actual_fee DECIMAL(12,2) NULL,
  ADD COLUMN logistics_product_currency VARCHAR(3) NULL,
  ADD COLUMN logistics_fee_adjustment_reason VARCHAR(500) NULL,
  ADD COLUMN shipping_method VARCHAR(128) NULL;
```

- [ ] **Step 4: Add Java fields with matching camel-case names**

Use `BigDecimal` for both fee fields and `String` for ISO currency; do not derive a live product price after fulfillment creation.

- [ ] **Step 5: Run the model test**

Run: `mvn -pl admin -Dtest=LogisticsProductSnapshotModelTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add erp-backend/sql/migration/V116__logistics_product_fulfillment_snapshot.sql erp-backend/admin/src/main/java/com/erp/admin/{shop,wms} erp-backend/admin/src/test/java/com/erp/admin/wms/LogisticsProductSnapshotModelTest.java
git commit -m "feat: add logistics product fulfillment snapshot"
```

### Task 2: 物流产品权限、币种与店铺默认值

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/WmsLogisticsProductService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/shop/service/ShopService.java`
- Modify: `erp-frontend/src/api/wms/logistics-product/types.ts`
- Modify: `erp-frontend/src/views/wms/logistics-product/ProductFormModal.vue`
- Modify: `erp-frontend/src/views/wms/logistics-product/index.vue`
- Modify: `erp-frontend/src/api/shop/types.ts`
- Modify: `erp-frontend/src/views/shop/ShopFormModal.vue`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/WmsLogisticsProductServiceTest.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/shop/ShopDefaultLogisticsProductTest.java`

**Interfaces:**
- Produces: `WmsLogisticsProductService.requireEnabledForOwner(Long productId, Long erpTenantId)`.
- Produces: shop detail/save support for `defaultLogisticsProductId`.

- [ ] **Step 1: Write failing ownership and shop-default tests**

```java
@Test
void ownerCanOnlyUseEnabledProductFromParentWms() {
    assertThrows(IllegalArgumentException.class,
        () -> service.requireEnabledForOwner(otherWmsProductId, erpTenantId));
}

@Test
void disabledProductCannotBecomeShopDefault() {
    request.setDefaultLogisticsProductId(disabledProductId);
    assertThrows(IllegalArgumentException.class, () -> shopService.validateDefaults(request));
}
```

- [ ] **Step 2: Implement product validation and unique-code normalization**

```java
public WmsLogisticsProduct requireEnabledForOwner(Long productId, Long erpTenantId) {
    SysTenant owner = sysTenantMapper.selectById(erpTenantId);
    Assert.notNull(owner, "货主不存在");
    WmsLogisticsProduct product = this.getById(productId);
    Assert.isTrue(product != null && Integer.valueOf(1).equals(product.getStatus()), "物流产品不可用");
    Assert.isTrue(product.getWmsTenantId().equals(owner.getParentWmsTenantId()), "物流产品不属于当前服务商");
    return product;
}
```

- [ ] **Step 3: Validate shop default warehouse and product together on create/update**

The saved product must be enabled and belong to the current ERP owner's parent WMS provider. Clearing the product is allowed only when the shop itself is disabled; enabled shops require both defaults.

- [ ] **Step 4: Update WMS product and shop forms**

Product form fields: name, code, default fee, currency, description, status. Shop form loads `/wms/logistics-product/options`, auto-selects the saved default, and clears an unavailable product.

- [ ] **Step 5: Run focused tests and frontend type-check**

Run: `mvn -pl admin -Dtest=WmsLogisticsProductServiceTest,ShopDefaultLogisticsProductTest test`

Run: `pnpm type-check`

- [ ] **Step 6: Commit**

```bash
git add erp-backend/admin/src/main/java/com/erp/admin/wms/service/WmsLogisticsProductService.java erp-backend/admin/src/main/java/com/erp/admin/shop erp-backend/admin/src/test/java/com/erp/admin/{shop,wms} erp-frontend/src/api/{shop,wms/logistics-product} erp-frontend/src/views/{shop,wms/logistics-product}
git commit -m "feat: configure default logistics products by shop"
```

### Task 3: ERP 确认发货与履约快照

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/order/model/dto/SubmitFulfillmentDTO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/order/service/ErpOrderFulfillmentSubmissionService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentOrderService.java`
- Modify: `erp-frontend/src/api/order/fulfillment.ts`
- Modify: `erp-frontend/src/views/order/components/OrderConfirmModal.vue`
- Modify: `erp-frontend/src/views/order/hooks/useOrderConfirm.ts`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/order/ErpOrderFulfillmentSubmissionTest.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentOrderServiceTest.java`
- Test: `erp-frontend/src/views/order/hooks/fulfillment-confirm.test.ts`

**Interfaces:**
- Consumes: `requireEnabledForOwner(productId, erpTenantId)` from Task 2.
- Produces: `submitOrderFulfillment(erpOrderId, wmsWarehouseId, logisticsProductId)`.

- [ ] **Step 1: Add failing snapshot and override tests**

```java
@Test
void submissionUsesExplicitProductAndCopiesSnapshot() {
    dto.setLogisticsProductId(explicitProductId);
    Long id = service.submit(dto);
    WmsFulfillmentOrder saved = fulfillmentOrderMapper.selectById(id);
    assertEquals(explicitProductId, saved.getLogisticsProductId());
    assertEquals(saved.getLogisticsProductDefaultFee(), saved.getLogisticsProductActualFee());
}
```

- [ ] **Step 2: Resolve selected product**

Resolution order: request override, then shop default. Reject empty, disabled, or cross-provider products before inventory reservation.

- [ ] **Step 3: Copy immutable product snapshot into create command and order**

```java
command.setLogisticsProductId(product.getId());
command.setLogisticsProductCode(product.getProductCode());
command.setLogisticsProductName(product.getProductName());
command.setLogisticsProductDescription(product.getProductDescription());
command.setLogisticsProductDefaultFee(product.getUnitPrice());
command.setLogisticsProductActualFee(product.getUnitPrice());
command.setLogisticsProductCurrency(product.getCurrency());
```

- [ ] **Step 4: Add one shared product selector to the confirm modal**

Display the shop default, allow override, show fee/currency and description. For multi-shop batch confirmation, group by shop and submit each order with its resolved product ID.

- [ ] **Step 5: Run backend and frontend tests**

Run: `mvn -pl admin -Dtest=ErpOrderFulfillmentSubmissionTest,FulfillmentOrderServiceTest test`

Run: `pnpm exec vitest run src/views/order/hooks/fulfillment-confirm.test.ts`

- [ ] **Step 6: Commit**

```bash
git add erp-backend/admin/src/main/java/com/erp/admin/order erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentOrderService.java erp-backend/admin/src/test/java/com/erp/admin/{order,wms} erp-frontend/src/api/order/fulfillment.ts erp-frontend/src/views/order
git commit -m "feat: snapshot logistics product on fulfillment submission"
```

### Task 4: 逐单拣货路线和当前订单锁定

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/FulfillmentPickTaskDetailVO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/FulfillmentPickCurrentOrderVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentPickingService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/FulfillmentPickingController.java`
- Modify: `erp-frontend/src/api/wms/fulfillment/types.ts`
- Modify: `erp-frontend/src/views/platform/fulfillment-workbench/PickingTaskDrawer.vue`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentPickingServiceTest.java`

**Interfaces:**
- Produces: `FulfillmentPickTaskDetailVO.currentOrder` with order number, route lines, current location and next location.
- Produces: `FulfillmentPickingService.currentOrder(Long taskId)`.

- [ ] **Step 1: Write failing sequential-order tests**

```java
@Test
void scanRejectsSecondOrderUntilFirstOrderIsPacked() {
    FulfillmentPickScanDTO scan = scanFor(secondOrder);
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.scan(scan));
    assertTrue(ex.getMessage().contains("当前顺序订单"));
}
```

- [ ] **Step 2: Expose a current-order projection ordered by location and SKU**

The projection must include location type/zone, location code, internal SKU, planned/picked quantity, item image and sequence number. Do not expose all task orders as simultaneously actionable.

- [ ] **Step 3: Change task-order lifecycle**

Use `PENDING -> PICKING -> WAITING_PACK -> COMPLETED`. Scanning the final line moves the order to `WAITING_PACK` but does not unlock the next task order. Packing completion marks the task-order `COMPLETED` and unlocks the next order.

- [ ] **Step 4: Rebuild drawer around the current order**

Show a compact route list and a stable scan toolbar: current order, current location, scan location, scan internal SKU, quantity. The next order remains visible only as a non-actionable preview.

- [ ] **Step 5: Run tests and type-check**

Run: `mvn -pl admin -Dtest=FulfillmentPickingServiceTest test`

Run: `pnpm type-check`

- [ ] **Step 6: Commit**

```bash
git add erp-backend/admin/src/main/java/com/erp/admin/wms/{controller,model/vo,service}/Fulfillment* erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentPickingServiceTest.java erp-frontend/src/api/wms/fulfillment erp-frontend/src/views/platform/fulfillment-workbench/PickingTaskDrawer.vue
git commit -m "feat: enforce order-by-order picking workflow"
```

### Task 5: 面单复核、打包与实际物流信息

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/FulfillmentPackDTO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/FulfillmentLogisticsFeeDTO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentShippingService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/FulfillmentShippingController.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/WmsFulfillmentPickTaskOrderMapper.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentShippingServiceTest.java`

**Interfaces:**
- Produces: `adjustLogisticsFee(id, amount, reason)` restricted to WMS provider identity.
- Produces: `pack(id, dto)` requiring carrier, shipping method, tracking number and positive actual weight.

- [ ] **Step 1: Write failing validation and task-unlock tests**

```java
@Test
void packRequiresAllActualShippingFields() {
    dto.setTrackingNo(null);
    assertThrows(IllegalArgumentException.class, () -> service.pack(orderId, dto));
}

@Test
void packCompletesTaskOrderAndUnlocksNextOrder() {
    service.pack(firstOrderId, completeDto());
    assertEquals("COMPLETED", taskOrderMapper.selectByFulfillmentOrderId(firstOrderId).getOrderStatus());
}
```

- [ ] **Step 2: Add Bean Validation constraints**

```java
@NotBlank(message = "承运商不能为空") private String carrierName;
@NotBlank(message = "运输方式不能为空") private String shippingMethod;
@NotBlank(message = "跟踪号不能为空") private String trackingNo;
@NotNull @DecimalMin(value = "0", inclusive = false) private BigDecimal packageWeightKg;
```

- [ ] **Step 3: Keep platform-specific label adapters and verify barcode before packing**

Ozon/WB/Yandex continue through existing `FulfillmentPlatformActionService.fetchLabel`; manual fulfillment uses the system label adapter. Do not add carrier tracking calls.

- [ ] **Step 4: Implement WMS-only actual fee adjustment**

Lock the fulfillment row, require status before `SHIPPED`, require amount >= 0 and nonblank reason when amount differs from default. Overseas warehouse identities may read but cannot change the fee.

- [ ] **Step 5: Mark current task order complete only after pack succeeds**

This creates the strict loop: pick one order -> print -> verify -> pack -> next order.

- [ ] **Step 6: Run focused tests and commit**

Run: `mvn -pl admin -Dtest=FulfillmentShippingServiceTest test`

```bash
git add erp-backend/admin/src/main/java/com/erp/admin/wms erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentShippingServiceTest.java
git commit -m "feat: validate package logistics and unlock sequential picking"
```

### Task 6: 物流产品费与仓库作业费分账

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/platform/finance/service/WarehouseBillingService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/WmsClientBillingRecordMapper.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/WmsClientBillingRecord.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/platform/finance/FulfillmentBillingServiceTest.java`

**Interfaces:**
- Produces: `recordFulfillmentLogisticsProduct(WmsFulfillmentOrder order)`.
- Retains: `recordFulfillmentOutbound(WmsFulfillmentOrder order, int quantity)` for warehouse operation fee.

- [ ] **Step 1: Write failing two-ledger idempotency test**

```java
@Test
void shippedOrderCreatesExactlyTwoIndependentCharges() {
    billing.recordFulfillmentLogisticsProduct(order);
    billing.recordFulfillmentOutbound(order, 3);
    billing.recordFulfillmentLogisticsProduct(order);
    billing.recordFulfillmentOutbound(order, 3);
    assertEquals(1, clientBillingCount("LOGISTICS_PRODUCT:" + order.getId()));
    assertEquals(1, warehouseBillingCount("FULFILLMENT:" + order.getId()));
}
```

- [ ] **Step 2: Record product charge from order snapshot**

The amount, currency, product name and description come from the fulfillment snapshot. Do not read current `wms_logistics_product.unit_price` during signout.

- [ ] **Step 3: Keep warehouse operation charge on rate card**

Warehouse operation fee remains based on the configured outbound fee rate and belongs to the overseas warehouse platform. Its payer is the order's WMS provider, not the ERP tenant.

- [ ] **Step 4: Invoke both records inside local shipment transaction**

```java
billingService.recordFulfillmentLogisticsProduct(order);
billingService.recordFulfillmentOutbound(order, quantity);
```

Both inserts use deterministic unique `biz_id` values; retries return existing records.

- [ ] **Step 5: Run tests and commit**

Run: `mvn -pl admin -Dtest=FulfillmentBillingServiceTest,FulfillmentShippingServiceTest test`

```bash
git add erp-backend/admin/src/main/java/com/erp/admin/{platform/finance,wms} erp-backend/admin/src/test/java/com/erp/admin/platform/finance/FulfillmentBillingServiceTest.java
git commit -m "feat: split fulfillment product and warehouse charges"
```

### Task 7: 合并海外仓取货、面单、打包与批量签出界面

**Files:**
- Modify: `erp-frontend/src/api/wms/fulfillment/types.ts`
- Modify: `erp-frontend/src/api/wms/fulfillment/index.ts`
- Modify: `erp-frontend/src/views/platform/fulfillment-workbench/FulfillmentWorkbenchPage.vue`
- Modify: `erp-frontend/src/views/platform/fulfillment-workbench/PickingTaskDrawer.vue`
- Create: `erp-frontend/src/views/platform/fulfillment-workbench/OrderPackPanel.vue`
- Test: `erp-frontend/src/views/platform/fulfillment-workbench/workbench-flow.test.ts`

**Interfaces:**
- Consumes: current task order and route from Task 4.
- Consumes: label, pack, fee-adjustment and batch-ship APIs from Tasks 5-6.

- [ ] **Step 1: Write failing component-flow test**

```ts
it('enables label only after current order is fully picked and enables next order only after pack', async () => {
  expect(canPrintLabel(pickingOrder)).toBe(false)
  expect(canPrintLabel(waitingPackOrder)).toBe(true)
  expect(canOpenNextOrder(waitingPackOrder)).toBe(false)
  expect(canOpenNextOrder(packedOrder)).toBe(true)
})
```

- [ ] **Step 2: Use one workbench with two operational bands**

Top band: orders awaiting down-shelf and task creation. Main band: active picking task/current order with route, scan controls, platform label, actual shipping form and pack button. Bottom/right band: packed queue with checkboxes and batch-signout button.

- [ ] **Step 3: Build `OrderPackPanel.vue`**

Stable field order: platform order, logistics product and fee, print label, verify barcode, carrier, shipping method, tracking number, actual weight, confirm packed. Buttons remain disabled until prerequisites are satisfied.

- [ ] **Step 4: Present partial failures without discarding success**

After batch signout, remove successful rows, keep failed rows selected, and show `sourceOrderNo: reason` for each failure.

- [ ] **Step 5: Keep screen compact and consistent with existing admin UI**

Use Ant Design table, descriptions, alert and drawer/panel patterns already present. No landing page, decorative cards, nested cards or oversized headings.

- [ ] **Step 6: Run tests, type-check and build**

Run: `pnpm exec vitest run src/views/platform/fulfillment-workbench/workbench-flow.test.ts`

Run: `pnpm type-check`

Run: `pnpm build-only`

- [ ] **Step 7: Commit**

```bash
git add erp-frontend/src/api/wms/fulfillment erp-frontend/src/views/platform/fulfillment-workbench
git commit -m "feat: unify order picking packing and batch signout workbench"
```

### Task 8: 旧链路收口、迁移和端到端验收

**Files:**
- Modify: `erp-backend/sql/migration/V116__logistics_product_fulfillment_snapshot.sql`
- Modify: `erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentStateMachineTest.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentEndToEndTest.java`
- Modify: `docs/superpowers/specs/2026-08-22-logistics-product-order-picking-design.md` only if implementation reveals a factual mismatch.

**Interfaces:**
- Verifies the complete flow from ERP submission to `SHIPPED`.

- [ ] **Step 1: Add migration backfill and safe defaults**

Backfill existing product currency to `RUB`. Existing unshipped fulfillment test data without product snapshots must be cancelled/recreated or explicitly populated from a chosen product; never guess a cross-provider product.

- [ ] **Step 2: Write the end-to-end test**

```java
@Test
void ownerSubmissionThroughBatchSignoutKeepsInventoryAndBillingConsistent() {
    Long fulfillmentId = submitWithShopDefaultProduct();
    accept(fulfillmentId);
    Long taskId = createPickTask(fulfillmentId);
    scanAllLines(taskId, fulfillmentId);
    fetchAndVerifyLabel(fulfillmentId);
    packWithActualShipping(fulfillmentId);
    shipBatch(fulfillmentId);
    assertStatus(fulfillmentId, FulfillmentStatus.SHIPPED);
    assertReservationShippedOnce(fulfillmentId);
    assertProductChargeOnce(fulfillmentId);
    assertWarehouseChargeOnce(fulfillmentId);
}
```

- [ ] **Step 3: Verify cancellation boundary**

`WAITING_SHELF` can cancel and releases reservations. `WAITING_PICK` and later cannot be cancelled by ERP. `SHIPPED` cannot be modified or re-billed.

- [ ] **Step 4: Run backend suite**

Run: `mvn -pl admin -Dtest='ErpOrderFulfillmentSubmissionTest,Fulfillment*Test,WmsLogisticsProductServiceTest,ShopDefaultLogisticsProductTest' test`

- [ ] **Step 5: Run frontend suite and production build**

Run: `pnpm type-check`

Run: `pnpm build-only`

- [ ] **Step 6: Apply migration to local Docker MySQL and smoke-test APIs**

Verify: product CRUD, shop default, order submission, batch accept, task creation, sequential scans, label retrieval, barcode verification, packing, partial batch signout, inventory deduction, two billing records.

- [ ] **Step 7: Start backend and frontend and verify browser workflow**

Backend expected health: `http://localhost:8081/actuator/health` or the repository's configured health endpoint.

Frontend expected URL: `http://localhost:5360/` unless the port is occupied.

- [ ] **Step 8: Commit final verification adjustments**

```bash
git add erp-backend erp-frontend docs/superpowers/specs/2026-08-22-logistics-product-order-picking-design.md
git commit -m "test: verify logistics product fulfillment workflow"
```

