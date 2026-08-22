# Direct Order Packing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make order-slot sorting optional and move mandatory order verification, label attachment, packing, and completion into the packing workflow.

**Architecture:** Keep the existing pick task and package tables. New tasks default to direct order packing, while an explicit `useSortSlots` option preserves slot sorting for large waves. Package `labelStatus` gains an `ATTACHED_CONFIRMED` state after a scanned platform order number matches the package.

**Tech Stack:** Spring Boot 2.7, MyBatis-Plus, JUnit 5, Vue 3, TypeScript, Ant Design Vue.

## Global Constraints

- Preserve existing pick, inventory reservation, ship, billing, and platform document APIs.
- Existing sorting tasks remain executable and may skip sorting only before any sorting quantity is recorded.
- A sales package can be completed only after item quantities are verified and its platform label is attached and scanned.

---

### Task 1: Optional Sorting State

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/BatchPickDTO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/BatchPickPreviewDTO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/OutboundPickingService.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/OutboundPickingServiceTest.java`

- [ ] Add failing tests for direct packing by default and explicit slot sorting.
- [ ] Add `useSortSlots` and use it when previewing and creating tasks.
- [ ] Add a guarded operation to skip an unstarted sorting stage.
- [ ] Run the focused picking tests.

### Task 2: Label Attachment Verification

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/PackageLabelScanDTO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/SalesOutboundPackageService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/OutboundShippingService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/OutboundShippingController.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/service/SalesOutboundPackageServiceTest.java`

- [ ] Add failing tests for a matching and mismatching platform-order label scan.
- [ ] Record an attached label as `ATTACHED_CONFIRMED` and write a LABEL scan event.
- [ ] Require item verification and attached label confirmation before package completion.
- [ ] Run package and shipping tests.

### Task 3: Warehouse UI

**Files:**
- Modify: `erp-frontend/src/views/platform/outbound-ops/BatchPickModal.vue`
- Modify: `erp-frontend/src/views/platform/outbound-ops/PickListDrawer.vue`
- Modify: `erp-frontend/src/views/platform/outbound-ops/PackModal.vue`
- Modify: `erp-frontend/src/api/wms/outbound-picking/index.ts`
- Modify: `erp-frontend/src/api/wms/outbound-picking/types.ts`
- Modify: `erp-frontend/src/api/wms/outbound-shipping/index.ts`
- Modify: `erp-frontend/src/api/wms/outbound-shipping/types.ts`

- [ ] Add an optional slot-sorting switch, default off.
- [ ] Show a skip-sorting command for untouched legacy sorting tasks.
- [ ] Present package work as item verification, label scan, then package completion.
- [ ] Run frontend type checking.

### Task 4: End-to-End Verification

- [ ] Run focused backend tests and the frontend type checker.
- [ ] Build and restart the backend.
- [ ] Create or reuse a direct-packing sales outbound task.
- [ ] Verify pick completion enters packing without mandatory slots.
- [ ] Verify an incorrect label is rejected and a matching label enables package completion.
- [ ] Verify all packages complete before sign-out is available.
