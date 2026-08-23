# 独立拣货任务与逐单作业 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将拣货任务拆成独立菜单，支持员工领取任务、任选订单逐单取货、面单核验、打包及异常处置，并让出库作业只承担签出。

**Architecture:** 保留现有仓库级任务、任务订单和任务明细三层模型，在任务表头增加领取信息，在任务订单增加独立进度和异常快照。后端以状态 CAS 和明细版本条件保证并发安全；前端新增任务列表页和独立作业页，复用现有平台面单与打包接口。

**Tech Stack:** Java 8、Spring Boot 2.7、MyBatis-Plus、JUnit 5、Mockito、Vue 3、TypeScript、Ant Design Vue 4、Vitest、MySQL 8。

**Spec:** `docs/superpowers/specs/2026-08-23-warehouse-grouped-picking-design.md`

## Global Constraints

- 任务只按仓库分组，不按货主、服务商、平台、店铺或物流产品拆分。
- 一张任务同一时刻只有一名领取人，其他员工只能查看。
- 订单可以任选，但一次只操作一单。
- 商品取齐、面单核验和打包全部完成后才进入出库作业。
- 异常订单保留库存预占，不阻塞任务内其他订单。
- 正式库存扣减及计费仍发生在签出事务中。
- 不实现多人共同拣一张任务、PDA 离线模式或自动路径优化。

---

### Task 1: 数据库状态与实体扩展

**Files:**
- Create: `erp-backend/sql/migration/V120__standalone_fulfillment_picking.sql`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/WmsFulfillmentPickTask.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/WmsFulfillmentPickTaskOrder.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentPickingServiceTest.java`

**Interfaces:**
- Produces task fields: `claimedTime`, `operatorId`, `taskStatus`.
- Produces order fields: `orderStatus`, `previousOrderStatus`, `previousFulfillmentStatus`, `exceptionType`, `exceptionReason`, `exceptionImageUrls`, `startedTime`, `completedTime`.

- [ ] **Step 1: Add a failing entity mapping test**

Add assertions that the new Java properties exist and that newly created tasks use `PENDING`.

- [ ] **Step 2: Run the focused test**

Run: `cd erp-backend && mvn -pl admin -Dtest=FulfillmentPickingServiceTest test`

Expected: FAIL because the fields and pending status do not exist.

- [ ] **Step 3: Add migration V120**

```sql
ALTER TABLE wms_fulfillment_pick_task
  ADD COLUMN claimed_time DATETIME NULL AFTER operator_id;

ALTER TABLE wms_fulfillment_pick_task_order
  ADD COLUMN previous_order_status VARCHAR(32) NULL,
  ADD COLUMN previous_fulfillment_status VARCHAR(32) NULL,
  ADD COLUMN exception_type VARCHAR(32) NULL,
  ADD COLUMN exception_reason VARCHAR(500) NULL,
  ADD COLUMN exception_image_urls TEXT NULL,
  ADD COLUMN started_time DATETIME NULL,
  ADD COLUMN completed_time DATETIME NULL;

CREATE INDEX idx_pick_task_operator_status
  ON wms_fulfillment_pick_task(operator_id, task_status);
```

Migrate historical `PICKING` rows as claimed, preserve historical `COMPLETED`, and do not reset progress.

- [ ] **Step 4: Extend both entities and create tasks as PENDING**

Change `createTask(List<Long>, Long)` so new tasks have `operatorId = null`, `claimedTime = null`, and `taskStatus = "PENDING"`. Do not transition fulfillment orders to `PICKING` until claim succeeds.

- [ ] **Step 5: Run tests and commit**

Run the focused backend test and commit the migration, entities, and test.

---

### Task 2: Task claim, release, transfer, filtering

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/WmsFulfillmentPickTaskMapper.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/FulfillmentPickTaskQueryDTO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentPickingService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentStatusSyncService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/FulfillmentPickingController.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentPickingServiceTest.java`

**Interfaces:**
- Produces: `claimTask(Long taskId, Long userId)`
- Produces: `releaseTask(Long taskId, Long userId)`
- Produces: `transferTask(Long taskId, Long targetUserId)`
- Produces: `listTasks(FulfillmentPickTaskQueryDTO query)`

- [ ] **Step 1: Write failing lifecycle tests**

Cover: only one claimant succeeds, claiming transitions all task orders from fulfillment `WAITING_PICK` to `PICKING`, non-owner cannot release, release retains scan progress, transfer changes operator without clearing progress.

- [ ] **Step 2: Verify tests fail**

Run the focused service test; expect missing lifecycle methods.

- [ ] **Step 3: Add mapper CAS methods**

```java
int claim(@Param("id") Long id, @Param("userId") Long userId,
          @Param("claimedTime") LocalDateTime claimedTime);
int release(@Param("id") Long id, @Param("userId") Long userId);
int transfer(@Param("id") Long id, @Param("targetUserId") Long targetUserId,
             @Param("claimedTime") LocalDateTime claimedTime);
```

Each SQL update must include the expected current status/operator in its `WHERE` clause.

- [ ] **Step 4: Implement service lifecycle**

Claim locks the task, applies CAS, transitions associated fulfillment orders to `PICKING`, and records progress. Release changes the task to `PENDING` and clears only claimant metadata. Transfer requires administrative permission at the controller boundary and leaves status `PICKING`.

- [ ] **Step 5: Add filtered list endpoint**

Accept `taskNo`, `warehouseId`, `taskStatus`, `operatorId`, `startTime`, and `endTime`; order by create time descending.

- [ ] **Step 6: Run tests and commit**

Run backend tests and commit this independently testable lifecycle.

---

### Task 3: Selectable orders and exception lifecycle

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/FulfillmentPickExceptionDTO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/FulfillmentPickTaskOrderVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/FulfillmentPickTaskDetailVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentPickingService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/FulfillmentPickingController.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentPickingServiceTest.java`

**Interfaces:**
- Changes: `detail(Long taskId, Long selectedOrderId, Long currentUserId)`
- Changes: `scan(FulfillmentPickScanDTO dto, Long currentUserId)`
- Produces: `markException(Long taskId, Long orderId, FulfillmentPickExceptionDTO dto, Long userId)`
- Produces: `restoreException(Long taskId, Long orderId, Long adminUserId)`
- Produces: `cancelException(Long taskId, Long orderId, Long adminUserId)`
- Consumes: `FulfillmentStatusSyncService.cancelFromException(Long fulfillmentOrderId, String reason)`

- [ ] **Step 1: Write failing selectable-order tests**

Create two pending orders and prove the second can be scanned first. Assert first scan changes only that task order to `PICKING`; all lines complete change it to `WAITING_LABEL` while the fulfillment order becomes `WAITING_PACK`.

- [ ] **Step 2: Write failing permission and exception tests**

Assert non-claimant scan is rejected; exception stores both previous states; restore returns to those states with picked quantities intact. Cancellation with zero picked quantity releases reservations immediately; cancellation with picked quantity enters `CANCEL_RETURNING` until all picked goods are scanned back.

- [ ] **Step 3: Remove first-incomplete-order enforcement**

Resolve the task order by `taskId + fulfillmentOrderId`; validate it belongs to the task and is in `PENDING`, `PICKING`, or `WAITING_LABEL`. Keep existing location, internal SKU, quantity, and version checks.

- [ ] **Step 4: Return an order queue**

`FulfillmentPickTaskOrderVO` includes fulfillment number, source order number/type, owner ID, first location, SKU count, quantity, order status, and progress. Sort by natural location code, then sequence number.

- [ ] **Step 5: Implement exception transitions**

Serialize image URLs as JSON text, save exception metadata and previous states, move the fulfillment order to `EXCEPTION`, and recalculate the task as `PARTIAL_EXCEPTION`. Restore replays saved states. Implement `cancelFromException`: lock the fulfillment order, inspect summed picked quantity, directly release and cancel when zero; otherwise transition to `CANCEL_RETURNING` and reuse `scanReturn` for physical return. Both branches call the existing ERP progress sync.

- [ ] **Step 6: Run tests and commit**

Run the service and cancellation-related tests, then commit.

---

### Task 4: Secure label, verify, and pack operations

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentPickingService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentShippingService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/FulfillmentShippingController.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentShippingServiceTest.java`

**Interfaces:**
- Produces: `assertTaskOperator(Long fulfillmentOrderId, Long userId)`
- Changes: `printLabel(Long id, Long userId)`, `verifyLabel(Long id, String barcode, Long userId)`, `pack(Long id, FulfillmentPackDTO dto, Long userId)`

- [ ] **Step 1: Add failing authorization tests**

Assert only the current task claimant can print, verify, or pack. Viewing and final signout remain controlled by their existing permissions.

- [ ] **Step 2: Implement claimant validation**

Resolve the unique task order and task, require `PICKING` or `PARTIAL_EXCEPTION`, and compare `operatorId` with the current user.

- [ ] **Step 3: Align task-order completion**

After successful pack, set task order `COMPLETED` and `completedTime`. Complete the task only when no non-cancelled order remains incomplete and no unresolved exception exists.

- [ ] **Step 4: Verify inventory boundary**

Keep physical inventory deduction and billing exclusively in `completeLocalShipment`; pack must not call `inventoryService.ship`.

- [ ] **Step 5: Run tests and commit**

Run picking and shipping service tests, then commit.

---

### Task 5: Independent menu and task-list page

**Files:**
- Create: `erp-backend/sql/migration/V121__standalone_fulfillment_pick_menu.sql`
- Create: `erp-frontend/src/views/platform/fulfillment-picking/FulfillmentPickingPage.vue`
- Create: `erp-frontend/src/views/platform/fulfillment-picking/picking-task-flow.ts`
- Create: `erp-frontend/src/views/platform/fulfillment-picking/picking-task-flow.vitest.ts`
- Modify: `erp-frontend/src/api/wms/fulfillment/index.ts`
- Modify: `erp-frontend/src/api/wms/fulfillment/types.ts`

**Interfaces:**
- Consumes lifecycle and list APIs from Tasks 2-4.
- Produces route component URI: `platform/fulfillment-picking/FulfillmentPickingPage`.

- [ ] **Step 1: Write failing frontend state tests**

Test helpers for showing “领取任务”, “继续作业”, “查看”, “释放任务”, and administrator “转交” by task state and current user.

- [ ] **Step 2: Add menu migration**

Insert “拣货任务” immediately after “订单下架”, shift “出库作业” sort order, and copy every role assignment from the existing outbound-workbench menu. Use UTF-8 byte literals, matching V117.

- [ ] **Step 3: Extend API types and clients**

Add query, claim, release, transfer, selected-order detail, exception, restore, and cancel clients. Add exact task/order status unions instead of unbounded strings.

- [ ] **Step 4: Build the list page**

Use the inbound-receive layout: natural wrapping filter form, `ProTable`, first/operation columns fixed, middle columns internally scrollable. Load warehouse and user options from existing hooks/components.

- [ ] **Step 5: Run frontend tests**

Run: `pnpm exec vitest run src/views/platform/fulfillment-picking/picking-task-flow.vitest.ts`

Run: `pnpm type-check`

- [ ] **Step 6: Commit**

Commit menu, API contract, list page, and tests.

---

### Task 6: Full-page task workbench

**Files:**
- Create: `erp-frontend/src/views/platform/fulfillment-picking/FulfillmentPickingWorkPage.vue`
- Create: `erp-frontend/src/views/platform/fulfillment-picking/components/PickingOrderQueue.vue`
- Create: `erp-frontend/src/views/platform/fulfillment-picking/components/PickingOrderPanel.vue`
- Create: `erp-frontend/src/views/platform/fulfillment-picking/components/PickingExceptionModal.vue`
- Modify: `erp-frontend/src/views/platform/fulfillment-picking/FulfillmentPickingPage.vue`
- Test: `erp-frontend/src/views/platform/fulfillment-picking/picking-task-flow.vitest.ts`

**Interfaces:**
- Route: `/ops/fulfillment-picking/work/:taskId`
- Queue emits: `select(orderId: number)`
- Panel consumes one selected order and emits `changed` after scan, label, verify, pack, or exception.

- [ ] **Step 1: Extend failing state tests**

Cover selectable `PENDING/PICKING/WAITING_LABEL` orders, read-only completed orders, disabled actions for non-claimants, and unresolved exceptions.

- [ ] **Step 2: Build the queue**

Show order number, platform, owner, first location, SKU/quantity, and status. Default-select the first actionable order but allow any actionable order to be selected.

- [ ] **Step 3: Build the active-order panel**

Render route lines and scan controls while picking; render label/verification controls only after every line is complete; render pack controls only after label verification. Persist only server-confirmed progress.

- [ ] **Step 4: Build exception handling**

Use existing upload component for optional photos; require type and reason; let administrators restore or cancel from the exception section.

- [ ] **Step 5: Add print task action**

Generate a compact printable picking sheet ordered by natural location code, containing task number, warehouse, order, internal SKU, location, and quantity.

- [ ] **Step 6: Verify responsive layout and commit**

Run Vitest, type-check, and build. Verify common desktop widths do not create body-level horizontal scrolling.

---

### Task 7: Reduce outbound workbench to signout and run regression

**Files:**
- Modify: `erp-frontend/src/views/platform/fulfillment-workbench/FulfillmentWorkbenchPage.vue`
- Delete after references are removed: `erp-frontend/src/views/platform/fulfillment-workbench/FulfillmentTaskList.vue`
- Delete after references are removed: `erp-frontend/src/views/platform/fulfillment-workbench/PickingTaskDrawer.vue`
- Modify: `erp-frontend/src/views/platform/fulfillment-workbench/workbench-flow.ts`
- Modify: `erp-frontend/src/views/platform/fulfillment-workbench/workbench-flow.vitest.ts`

**Interfaces:**
- Outbound workbench consumes only `GET /wms/fulfillment-shipping` and `POST /ship`.

- [ ] **Step 1: Remove the embedded task list**

Keep the existing packed-order table, logistics product, carrier information, selection, and batch signout behavior.

- [ ] **Step 2: Remove obsolete sequential helpers**

Delete old drawer-only `canOpenNextOrder` assumptions; keep only helpers used by batch signout.

- [ ] **Step 3: Run complete automated verification**

Backend:

```powershell
cd erp-backend
mvn -pl admin -Dtest=FulfillmentPickingServiceTest,FulfillmentDispatchServiceTest,FulfillmentShippingServiceTest test
mvn -pl admin -DskipTests package
```

Frontend:

```powershell
cd erp-frontend
pnpm exec vitest run
pnpm type-check
pnpm build-only
```

- [ ] **Step 4: Apply migrations and restart**

Back up the affected task tables, apply V120 and V121, restart the Java backend and Vue frontend, and verify ports and health endpoints.

- [ ] **Step 5: Normal-flow browser verification**

Verify: down-shelf creates pending tasks by warehouse; employee claims; any order can be selected; scans persist; label and pack gate correctly; exception does not block another order; completed packages appear in outbound workbench; batch signout deducts inventory once.

- [ ] **Step 6: Commit final cleanup**

Commit the outbound cleanup and any verification fixes without including unrelated dirty-worktree files.
