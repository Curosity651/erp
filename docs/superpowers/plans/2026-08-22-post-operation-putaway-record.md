# Post-Operation Putaway Recording Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace recommendation-driven inbound putaway with a post-operation recording flow in which workers enter the locations and quantities they already placed.

**Architecture:** Keep the logical location inventory, immutable receipt, billing, locking, and order status transaction. Add explicit record-context and record-submit contracts, make capacity checks advisory except for requiring a reason on volume/weight overflow, and disable both legacy write endpoints.

**Tech Stack:** Java 8, Spring Boot 2.7, MyBatis-Plus, JUnit 5, Vue 3, TypeScript, Ant Design Vue, Vitest.

**Spec:** `docs/superpowers/specs/2026-08-22-post-operation-putaway-record-design.md`

## Global Constraints

- Do not add pallet, slot, stack, or batch concepts back into the new flow.
- Do not recommend a location or quantity.
- A SKU may be split across multiple locations, but its total must equal received quantity.
- Volume, weight, and SKU-kind overflow are advisory; volume or weight overflow requires an operator reason.
- Missing dimensions or weight must not block recording.
- Inventory, receipt, billing, operator, and status changes remain in one transaction.
- Old `/putaway` and `/logical-putaway` write endpoints must not write inventory.

---

### Task 1: Define the backend record contracts

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/PutawayRecordDTO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/PutawayRecordContextVO.java`
- Modify: `erp-backend/admin/src/test/java/com/erp/admin/wms/LogicalInboundPutawayServiceTest.java`

**Interfaces:**
- Produces: `PutawayRecordDTO` with `inboundOrderId`, billing fields, and `lines` containing `skuCode`, `quality`, `locationId`, `quantity`, and `capacityOverrideReason`.
- Produces: `PutawayRecordContextVO` with owner/order metadata, SKU summaries, allowed location summaries, and current capacity data.

- [ ] **Step 1: Write failing contract tests**

Add tests that construct two duplicate record lines and verify they merge by `locationId|skuCode|quality`, preserve the first nonblank `capacityOverrideReason`, and validate exact received totals.

- [ ] **Step 2: Run the focused test and verify compilation fails**

Run:

```powershell
mvn -pl admin '-Dtest=LogicalInboundPutawayServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: compilation failure because `PutawayRecordDTO` and record-oriented service methods do not exist.

- [ ] **Step 3: Add the DTO and context VO**

Use bean validation on required IDs, positive integer quantities, nonblank SKU codes, and nonempty lines. Keep capacity values nullable and include `capacityCalculable`, `volumeAllowed`, `weightAllowed`, and `skuKindsAllowed` on each location summary.

- [ ] **Step 4: Update the service static merge/total signatures to use `PutawayRecordDTO.RecordLine`**

The merge key must be:

```java
locationId + "|" + skuCode + "|" + normalizedQuality
```

- [ ] **Step 5: Run the focused test**

Expected: contract and static validation tests pass.

### Task 2: Implement context loading and advisory capacity checks

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/LogicalInboundPutawayService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/LocationCapacityService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/WmsInboundExecutionController.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/LogicalInboundPutawayServiceTest.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/LocationCapacityServiceTest.java`

**Interfaces:**
- Produces: `PutawayRecordContextVO context(Long inboundOrderId)`.
- Produces: `List<PutawayReceiptLineVO> record(PutawayRecordDTO dto)`.
- Produces: `LocationCapacityVO tryEvaluate(Long locationId, List<PlacementLine> additions)` or equivalent nonthrowing capacity result for missing measurements.

- [ ] **Step 1: Add failing tests for soft capacity behavior**

Cover:

```text
volume overflow + reason => accepted
weight overflow + reason => accepted
volume/weight overflow without reason => rejected
SKU-kind overflow => accepted with warning state
missing SKU dimensions => accepted as capacityCalculable=false
```

- [ ] **Step 2: Run focused tests and verify failures**

Run:

```powershell
mvn -pl admin '-Dtest=LogicalInboundPutawayServiceTest,LocationCapacityServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

- [ ] **Step 3: Replace recommendation plan construction with record context construction**

Load received quantities grouped by SKU, owner-scoped SKU data, internal warehouse SKU code, first actual/platform image, allowed assigned-rack locations, public TEMP locations, zone names/types, and current capacity summaries. Sort locations naturally by rack/column/location code and return no recommendation fields.

- [ ] **Step 4: Change submission to advisory checks**

Keep order/location locks, warehouse validation, service-provider permission, public TEMP access, quality-zone rules, exact quantity validation, inventory increase, immutable receipt, billing, status CAS, and operator recording. Remove hard assertions for `weightAllowed` and `skuKindsAllowed`; require `capacityOverrideReason` only when calculable volume or weight exceeds its configured limit.

- [ ] **Step 5: Add the new controller endpoints and disable old writes**

Expose:

```text
GET  /wms/inbound-execution/putaway-record-context
POST /wms/inbound-execution/putaway-record
```

Make both old POST endpoints throw `BusinessException(410, "旧版上架模式已停用，请使用上架记录功能")` before calling any service.

- [ ] **Step 6: Run focused backend tests**

Expected: all putaway and capacity tests pass.

### Task 3: Replace frontend recommendation contracts and API calls

**Files:**
- Modify: `erp-frontend/src/api/wms/inbound-execution/index.ts`
- Create: `erp-frontend/src/views/platform/inbound-ops/putaway-record.vitest.ts`
- Create: `erp-frontend/src/views/platform/inbound-ops/putaway-record.ts`

**Interfaces:**
- Produces: `PutawayRecordContextVO`, `PutawayRecordSkuVO`, `PutawayRecordLocationVO`, `PutawayRecordDTO`, and `PutawayRecordLine` TypeScript interfaces.
- Produces: pure helpers `mergeRecordLines`, `allocatedQuantity`, `capacityState`, and `requiresCapacityReason`.

- [ ] **Step 1: Write failing Vitest tests**

Cover empty initial rows, exact totals, duplicate-line merge, 80% warning threshold, overflow reason requirement, and uncalculable capacity.

- [ ] **Step 2: Run the test and verify failure**

Run:

```powershell
pnpm vitest run src/views/platform/inbound-ops/putaway-record.vitest.ts
```

- [ ] **Step 3: Replace API contracts**

Remove recommendation-specific public types from the inbound execution API and add calls to `/putaway-record-context` and `/putaway-record`. Keep receipt query and printing contracts unchanged.

- [ ] **Step 4: Implement the pure allocation and capacity helpers**

Helpers must not select a location or quantity. They only merge entered rows, calculate totals, classify normal/warning/overflow/unknown states, and determine whether a reason is required.

- [ ] **Step 5: Run the Vitest file**

Expected: all helper tests pass.

### Task 4: Rebuild the putaway drawer as an actual-result form

**Files:**
- Modify: `erp-frontend/src/views/platform/inbound-ops/PutawayDrawer.vue`
- Modify: `erp-frontend/src/views/platform/inbound-ops/InboundPutawayPage.vue`
- Modify: `erp-frontend/src/views/platform/inbound-ops/putaway-receipt-print.ts`

**Interfaces:**
- Consumes: record context and helper interfaces from Task 3.
- Produces: manual location/quantity rows and a `PutawayRecordDTO` submission.

- [ ] **Step 1: Remove recommendation-driven initialization**

Each SKU starts with exactly one row containing `quality=GOOD`, no `locationId`, and no quantity. Delete `candidateFor`, `recommendationText`, `recommendedQuantity`, and automatic distribution loops.

- [ ] **Step 2: Add manual record columns**

Render quality, searchable actual location, current capacity, post-record capacity, actual quantity, overflow reason, and split/delete actions. Location labels include location code, zone name/type, public TEMP marker, and current utilization.

- [ ] **Step 3: Implement split and validation behavior**

Split adds an empty row inheriting only quality. Show received/recorded/remaining counts. Disable submit unless every row has a location and positive quantity, totals match, after-hours reason is present when required, and every calculable volume/weight overflow has a reason.

- [ ] **Step 4: Update wording and submission**

Use “登记上架”, “上架记录”, “确认上架记录”, and “上架结果已记录”. Submit to the new endpoint and retain the post-success print prompt.

- [ ] **Step 5: Update list wording and print fields**

Show `RECEIVED` as “已收货 · 待登记上架”. Print internal warehouse SKU when present and include capacity override reason.

- [ ] **Step 6: Run frontend tests and type checking**

Run:

```powershell
pnpm vitest run src/views/platform/inbound-ops/putaway-record.vitest.ts
pnpm exec vue-tsc --noEmit
```

Expected: both commands exit 0.

### Task 5: Regression, build, runtime verification, and commit

**Files:**
- Modify only files required by failures directly caused by Tasks 1-4.

**Interfaces:**
- Verifies all interfaces produced by prior tasks.

- [ ] **Step 1: Run the relevant backend suite**

```powershell
mvn -pl admin '-Dtest=LogicalInboundPutawayServiceTest,LocationCapacityServiceTest,LocationInventoryServiceTest,WmsInboundExecutionServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

- [ ] **Step 2: Build the backend and frontend**

```powershell
mvn -pl admin -am -DskipTests package
pnpm build
```

- [ ] **Step 3: Restart Docker dependencies and application services**

Verify MySQL on `3307`, Redis on `6379`, backend on `8081`, and frontend on `5360`.

- [ ] **Step 4: Verify HTTP and logs**

Frontend `/` must return HTTP 200. Backend unauthenticated `/` may return 401. Backend logs must contain `Started AdminApplication` and no startup exception.

- [ ] **Step 5: Commit and push**

```powershell
git add erp-backend erp-frontend docs/superpowers/plans/2026-08-22-post-operation-putaway-record.md
git commit -m "feat: record actual inbound putaway results"
git push origin codex/logical-location-fulfillment
```
