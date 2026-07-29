# Inbound Goods Label Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add owner-scoped warehouse SKU recognition and post-receipt SKU label/reference-sheet printing.

**Architecture:** Derive the warehouse SKU from the owner name prefix and original SKU without changing inventory keys. Reuse the existing inbound detail API for owner, received quantities, names, and images; keep printing entirely read-only.

**Tech Stack:** Java 8, Spring Boot, MyBatis-Plus, JUnit 5, Vue 3, TypeScript, Ant Design Vue, qrcode.

## Global Constraints

- ERP owner screens continue to show the original SKU.
- Warehouse SKU format is exactly `OWNER_NAME-ORIGINAL_SKU`.
- Labels print one copy per actually received unit.
- No batch number is added.
- Printing never changes inventory or document status.

---

### Task 1: Warehouse SKU recognition

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/product/service/WarehouseSkuCodeService.java`
- Create: `erp-backend/admin/src/test/java/com/erp/admin/product/WarehouseSkuCodeServiceTest.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/product/service/SkuBarcodeService.java`

**Interfaces:**
- Produces: `WarehouseSkuCodeService.build(Long, String)` and `matches(Long, String, String)`.
- Consumes: `SysTenantMapper.selectById`.

- [ ] Write tests for normalized generation, case-insensitive matching, and missing owner code rejection.
- [ ] Run the focused test and verify it fails because the service is absent.
- [ ] Implement the service and connect it to `SkuBarcodeService.matches`.
- [ ] Run the focused test and existing outbound scan tests.

### Task 2: Print models and rendering

**Files:**
- Create: `erp-frontend/src/views/platform/inbound-ops/received-goods-print.ts`
- Create: `erp-frontend/src/views/platform/inbound-ops/received-goods-print.test.ts`

**Interfaces:**
- Produces: `buildWarehouseSkuCode`, `aggregateReceivedGoods`, `printReceivedSkuLabels`, and `printReceivedGoodsReference`.
- Consumes: `PurchaseInboundDetailVO`.

- [ ] Write a Node test proving duplicate SKU rows aggregate by actual quantity and zero-quantity rows are excluded.
- [ ] Run the Node test and verify the missing module failure.
- [ ] Implement aggregation and warehouse SKU generation.
- [ ] Implement 80mm × 50mm labels and the A4 image reference table.
- [ ] Run the Node test and frontend type check.

### Task 3: Receive-list actions

**Files:**
- Modify: `erp-frontend/src/views/platform/inbound-ops/InboundReceivePage.vue`

**Interfaces:**
- Consumes: existing `getInboundOpsDetail` plus Task 2 print functions.

- [ ] Add `商品标签` and `货物对照表` actions for `RECEIVED` and `COMPLETED`.
- [ ] Open the print window synchronously, load detail, then render.
- [ ] Keep `SUBMITTED` behavior unchanged and widen only the operation column as required.
- [ ] Run Vue type checking and production build.
