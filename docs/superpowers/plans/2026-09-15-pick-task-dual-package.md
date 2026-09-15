# Pick Task Dual Package Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace browser-only pick-list printing with generation of one warehouse ZIP and one Chinese archive ZIP per fulfillment pick task.

**Architecture:** A focused backend package service snapshots a task, obtains labels through existing platform actions, renders platform-separated documents, verifies both archives, and uploads them to private OSS. The frontend calls one endpoint and exposes the two returned downloads. A database batch record provides idempotency and auditability.

**Tech Stack:** Java 8, Spring Boot 2.7, MyBatis-Plus, PDFBox 2.0.27, FastExcel, `java.util.zip`, Vue 3, TypeScript, Vitest.

**Spec:** `docs/superpowers/specs/2026-09-15-pick-task-dual-package-design.md`

## Global Constraints

- Exactly two task-level ZIPs are published: warehouse and Chinese archive.
- Supported source types are `OZON`, `WB`, `YANDEX`, and `MANUAL`.
- Package generation must not advance fulfillment, picking, packing, shipment, or billing state.
- Cancelled task orders are excluded and recorded; every other task order must be complete in the warehouse package.
- Existing unrelated working-tree changes must be preserved.

---

### Task 1: Package contract and deterministic ZIP verification

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/pickpackage/PickPackageManifest.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/pickpackage/PickPackageArchive.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/pickpackage/PickPackageArchiveTest.java`

**Interfaces:**
- Produces: `PickPackageArchive.build(Map<String, byte[]>) -> byte[]`
- Produces: `PickPackageArchive.verify(byte[], Set<String>) -> VerificationResult`

- [ ] Write tests proving deterministic members, safe relative paths, SHA-256 recording, and rejection of missing/duplicate/unsafe members.
- [ ] Run `mvn -pl admin -Dtest=PickPackageArchiveTest test` and verify the tests fail because the classes do not exist.
- [ ] Implement the minimal manifest and ZIP builder/verifier.
- [ ] Re-run the focused tests and verify they pass.

### Task 2: Task snapshot and platform grouping

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/pickpackage/PickTaskPackageSnapshot.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/pickpackage/PickTaskPackageSnapshotFactory.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/pickpackage/PickTaskPackageSnapshotFactoryTest.java`

**Interfaces:**
- Consumes: `FulfillmentPickTaskDetailVO`
- Produces: immutable task/order/line snapshot, grouped by platform and shop, with `snapshotHash`.

- [ ] Write tests for mixed platforms, cancelled-order exclusion, location/SKU ordering, quantity totals, missing-line rejection, and stable snapshot hashes.
- [ ] Run the focused test and verify expected failures.
- [ ] Implement snapshot models and factory.
- [ ] Re-run the focused tests and verify they pass.

### Task 3: Warehouse and Chinese document renderers

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/pickpackage/PickPackageDocumentRenderer.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/pickpackage/PickPackageWorkbookRenderer.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/pickpackage/PickPackageDocumentRendererTest.java`

**Interfaces:**
- Consumes: `PickTaskPackageSnapshot` and label byte arrays.
- Produces: warehouse members (`00_Checklist`, per-platform pick lists and labels) and Chinese members (summary/detail/error workbooks and JSON manifests).

- [ ] Write tests that inspect generated PDFs/XLSX files and assert platform directories, ordering, totals, and Chinese-package exclusion of labels.
- [ ] Run the focused tests and verify expected failures.
- [ ] Implement minimal PDF/Excel rendering using PDFBox and FastExcel.
- [ ] Re-run the focused tests and verify they pass.

### Task 4: Orchestration, idempotency, and private OSS publishing

**Files:**
- Create: `erp-backend/sql/migration/V150__pick_task_dual_package.sql`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/entity/WmsFulfillmentPickPackage.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/WmsFulfillmentPickPackageMapper.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/FulfillmentPickPackageService.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/FulfillmentPickPackageVO.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/FulfillmentPickPackageServiceTest.java`

**Interfaces:**
- Produces: `FulfillmentPickPackageService.generate(taskId, userId) -> FulfillmentPickPackageVO`
- Uses existing `FulfillmentPickingService.detail`, platform label actions, and `OssService`.

- [ ] Write service tests for four-platform generation, label failure blocking, same-snapshot reuse, changed-snapshot regeneration, and no status mutation.
- [ ] Run the focused test and verify expected failures.
- [ ] Add migration, persistence model, mapper, and orchestration service.
- [ ] Re-run the focused tests and verify they pass.

### Task 5: HTTP endpoint and frontend download flow

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/FulfillmentPickingController.java`
- Modify: `erp-frontend/src/api/wms/fulfillment/index.ts`
- Modify: `erp-frontend/src/api/wms/fulfillment/types.ts`
- Modify: `erp-frontend/src/views/platform/fulfillment-picking/SimplifiedTaskModal.vue`
- Test: `erp-frontend/src/views/platform/fulfillment-picking/pick-package-download.vitest.ts`

**Interfaces:**
- Adds `POST /wms/fulfillment-picking/tasks/{id}/print-packages`.
- Adds frontend `generateFulfillmentPickPackages(id)` returning two downloadable files.

- [ ] Write frontend and controller contract tests that expect two named downloads and an actionable error state.
- [ ] Run focused backend/frontend tests and verify expected failures.
- [ ] Implement endpoint, API types, loading state, automatic sequential downloads, and fallback download buttons.
- [ ] Re-run focused tests and verify they pass.

### Task 6: Full verification

**Files:**
- Modify only files required by failures introduced by this feature.

- [ ] Run all new backend package tests.
- [ ] Run existing fulfillment picking/shipping tests.
- [ ] Run `mvn -pl admin -am -DskipTests package`.
- [ ] Run frontend Vitest tests, `pnpm type-check`, and `pnpm build-only`.
- [ ] Apply V150 to the local development database and verify table/index creation.
- [ ] Restart backend and frontend, generate a real task package where data permits, and independently read both ZIPs to verify members and hashes.
