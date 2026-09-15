# Global SKU Return Excel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace owner-plus-SKU return registration with authoritative global warehouse SKU matching and add a validated Excel template/import workflow.

**Architecture:** The backend resolves each complete warehouse SKU to its unique tenant-scoped product and remains authoritative at final receipt submission. The frontend uses one global-SKU field, calls a batch resolver for preview, and parses Excel into a local draft before submission. Existing return disposition, warehouse processing, inventory posting, and billing flows remain unchanged.

**Tech Stack:** Java 8, Spring Boot, MyBatis/MyBatis-Plus, JUnit 5, Mockito, Vue 3, TypeScript, Ant Design Vue, ExcelJS, Vitest.

**Spec:** `docs/superpowers/specs/2026-09-16-global-sku-return-excel-design.md`

## Global Constraints

- A complete warehouse SKU is the only client-supplied product identity for new return receipts.
- The server must derive `erpTenantId` and tenant-scoped `skuCode`; it must not trust client ownership fields.
- Batch resolution and Excel import accept at most 500 non-empty SKU rows.
- SKU comparisons trim whitespace and ignore case while responses use the canonical server code.
- Excel import changes only the browser draft and never creates a return receipt automatically.
- Any invalid receipt line prevents the entire transaction from creating return orders.
- Do not add a database migration or a new runtime dependency.
- Preserve the existing return state machine, inventory posting, photo validation, and billing behavior.

---

## File Structure

- `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/WarehouseSkuResolveDTO.java`: validated batch resolve request.
- `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/WarehouseSkuResolveVO.java`: per-code canonical match or error.
- `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/WmsSkuLookupMapper.java`: exact full warehouse SKU query.
- `erp-backend/admin/src/main/java/com/erp/admin/wms/service/ReturnQcService.java`: batch resolver and authoritative receipt conversion.
- `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/ReturnQcController.java`: return-operation resolver endpoint.
- `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/ReturnReceiptDTO.java`: receipt lines accept `warehouseSkuCode`.
- `erp-backend/admin/src/test/java/com/erp/admin/wms/ReturnQcServiceTest.java`: resolver and registration behavior.
- `erp-frontend/src/api/wms/return-qc/types.ts`: resolver and receipt TypeScript contracts.
- `erp-frontend/src/api/wms/return-qc/index.ts`: `resolveReturnWarehouseSkus()` API.
- `erp-frontend/src/views/platform/return-ops/return-receipt-excel.ts`: workbook creation, parsing, validation, and download.
- `erp-frontend/src/views/platform/return-ops/return-receipt-excel.vitest.ts`: real ExcelJS round-trip and validation tests.
- `erp-frontend/src/views/platform/return-ops/return-receipt-flow.ts`: draft normalization and resolved-product application.
- `erp-frontend/src/views/platform/return-ops/return-receipt-flow.vitest.ts`: draft safety tests.
- `erp-frontend/src/views/platform/return-ops/ReturnReceiptModal.vue`: global SKU manual entry and Excel controls.
- `erp-frontend/src/views/platform/return-ops/ReturnProcessModal.vue`: global SKU-only display.
- `erp-frontend/src/locales/lang/{zh-CN,en-US,ru-RU,uk-UA}/platform.json`: labels and validation messages.

---

### Task 1: Backend Full Warehouse SKU Resolver

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/WarehouseSkuResolveDTO.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/vo/WarehouseSkuResolveVO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/mapper/WmsSkuLookupMapper.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/ReturnQcService.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/controller/ReturnQcController.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/ReturnQcServiceTest.java`

**Interfaces:**
- Consumes: existing `sku.warehouse prefix + '-' + sku.sku_code` convention from `WarehouseSkuCodeService`.
- Produces: `List<WarehouseSkuResolveVO> resolveWarehouseSkus(List<String> warehouseSkuCodes)` and `POST /wms/return-qc/sku-resolve`.

- [ ] **Step 1: Write failing resolver tests**

Add tests proving one canonical match is returned, an unknown code carries an error, duplicate normalized inputs are rejected, and a request over 500 entries is rejected. Use a real `SkuLookupVO` fixture:

```java
SkuLookupVO sku = new SkuLookupVO();
sku.setWarehouseSkuCode("JHIN-SKU-1");
sku.setErpTenantId(6L);
sku.setSkuCode("SKU-1");
sku.setChineseName("测试商品");
when(skuLookupMapper.findByWarehouseSkuCodes(any())).thenReturn(Collections.singletonList(sku));

assertThat(service.resolveWarehouseSkus(Collections.singletonList(" jhin-sku-1 ")))
        .singleElement()
        .satisfies(result -> {
            assertThat(result.isMatched()).isTrue();
            assertThat(result.getWarehouseSkuCode()).isEqualTo("JHIN-SKU-1");
            assertThat(result.getErpTenantId()).isEqualTo(6L);
            assertThat(result.getSkuCode()).isEqualTo("SKU-1");
        });
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
mvn -f erp-backend/pom.xml -pl admin -am -Dtest=ReturnQcServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation or assertion failure because the resolver types and methods do not exist.

- [ ] **Step 3: Implement request, response, mapper, service, and controller**

`WarehouseSkuResolveDTO` contains `@NotEmpty @Size(max=500) List<@NotBlank String> warehouseSkuCodes`.

`WarehouseSkuResolveVO` contains:

```java
private String requestedCode;
private String warehouseSkuCode;
private Long erpTenantId;
private String ownerName;
private String skuCode;
private String skuName;
private boolean matched;
private String error;
```

Add `warehouseSkuCode` and `ownerName` to `SkuLookupVO`, and add mapper method:

```java
List<SkuLookupVO> findByWarehouseSkuCodes(@Param("warehouseSkuCodes") Collection<String> warehouseSkuCodes);
```

Implement it with one SQL query joining `sku s` to `sys_tenant t`, comparing `UPPER(CONCAT(t.warehouse_sku_prefix, '-', s.sku_code))` against normalized request codes. Return canonical `CONCAT(...) AS warehouse_sku_code` and `t.tenant_name AS owner_name`.

`ReturnQcService.resolveWarehouseSkus()` must assert platform identity, normalize once, reject normalized duplicates, query once, group by uppercase full code, and return results in request order. Zero matches use `matched=false,error=全局 SKU 不存在`; multiple matches use `matched=false,error=全局 SKU 匹配到多个商品`.

Expose:

```java
@PostMapping("/sku-resolve")
@PreAuthorize("hasAuthority('wms:return-qc:oper')")
public ApiResult<List<WarehouseSkuResolveVO>> resolveSkus(
        @Validated @RequestBody WarehouseSkuResolveDTO dto)
```

- [ ] **Step 4: Run focused backend tests and verify GREEN**

Run the Task 1 Maven command. Expected: all `ReturnQcServiceTest` tests pass.

- [ ] **Step 5: Commit Task 1**

Stage only the Task 1 files and commit:

```powershell
git commit -m "feat: resolve return goods by global SKU"
```

---

### Task 2: Authoritative Global SKU Receipt Registration

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/model/dto/ReturnReceiptDTO.java`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/service/ReturnQcService.java`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/wms/ReturnQcServiceTest.java`

**Interfaces:**
- Consumes: Task 1 full warehouse SKU query and normalized resolver result.
- Produces: `ReturnReceiptDTO.Line.warehouseSkuCode` as the only product identity accepted by `/receipts`.

- [ ] **Step 1: Write failing receipt tests**

Add tests that submit `JHIN-SKU-1` without owner or original SKU, capture inserted order/item, and assert the service uses resolved owner `6` and original SKU `SKU-1`. Add a two-owner test asserting two return orders are created, and an unknown-SKU test asserting no insert occurs.

```java
ReturnReceiptDTO.Line line = new ReturnReceiptDTO.Line();
line.setWarehouseSkuCode("JHIN-SKU-1");
line.setReceivedQty(3);
dto.setItems(Collections.singletonList(line));

service.registerReceipt(dto);

verify(returnInboundMapper).insert(orderCaptor.capture());
verify(itemMapper).insert(itemCaptor.capture());
assertThat(orderCaptor.getValue().getErpTenantId()).isEqualTo(6L);
assertThat(itemCaptor.getValue().getSkuCode()).isEqualTo("SKU-1");
```

- [ ] **Step 2: Run focused test and verify RED**

Run the Task 1 Maven command. Expected: failure because receipt lines still require `erpTenantId + skuCode`.

- [ ] **Step 3: Implement authoritative conversion**

Replace `ReturnReceiptDTO.Line.erpTenantId` and `.skuCode` with `@NotBlank warehouseSkuCode`. In `registerReceipt()`, normalize and reject duplicate global codes, query every SKU before inserting, reject missing/multiple matches, create an internal pair of `(line, resolvedSku)`, then group by `resolvedSku.erpTenantId`. Keep warehouse authorization, photo validation, reason normalization, per-owner split, audit fields, and the transaction boundary unchanged.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run the Task 1 Maven command. Expected: all resolver, registration, and existing return tests pass.

- [ ] **Step 5: Commit Task 2**

```powershell
git commit -m "refactor: register returns with global SKU"
```

---

### Task 3: Frontend API Contract and Draft Resolver

**Files:**
- Modify: `erp-frontend/src/api/wms/return-qc/types.ts`
- Modify: `erp-frontend/src/api/wms/return-qc/index.ts`
- Create: `erp-frontend/src/views/platform/return-ops/return-receipt-flow.ts`
- Create: `erp-frontend/src/views/platform/return-ops/return-receipt-flow.vitest.ts`

**Interfaces:**
- Consumes: Task 1 `/sku-resolve` response and Task 2 receipt request.
- Produces: `resolveReturnWarehouseSkus(codes)`, `applyResolvedReturnSkus(lines, results)`, and safe receipt draft types.

- [ ] **Step 1: Write failing frontend flow tests**

Test that canonical results populate display-only owner/product fields, unmatched results add a row error, and the original draft is returned unchanged when any imported SKU fails:

```ts
const result = applyResolvedReturnSkus(
  [{ key: 1, warehouseSkuCode: 'jhin-sku-1', receivedQty: 2, returnReason: 'OTHER', photoFileIds: [] }],
  [{ requestedCode: 'jhin-sku-1', warehouseSkuCode: 'JHIN-SKU-1', matched: true,
     erpTenantId: 6, ownerName: 'JHIN', skuCode: 'SKU-1', skuName: '测试商品' }]
)
expect(result.errors).toEqual([])
expect(result.lines[0]).toMatchObject({ warehouseSkuCode: 'JHIN-SKU-1', ownerName: 'JHIN' })
```

- [ ] **Step 2: Run frontend flow test and verify RED**

```powershell
pnpm --dir erp-frontend exec vitest run src/views/platform/return-ops/return-receipt-flow.vitest.ts --maxWorkers=1 --minWorkers=1
```

Expected: module or export missing.

- [ ] **Step 3: Implement API and pure draft functions**

Add `WarehouseSkuResolveResult`, change `ReturnReceiptDTO.items` to carry `warehouseSkuCode`, and implement:

```ts
export function resolveReturnWarehouseSkus(warehouseSkuCodes: string[]) {
  return httpClient.post<ApiResult<WarehouseSkuResolveResult[]>>(`${BASE}/sku-resolve`, {
    warehouseSkuCodes
  })
}
```

Keep row merging in `return-receipt-flow.ts` pure; it must not mutate input arrays and must produce row-numbered errors for unmatched codes.

- [ ] **Step 4: Run focused frontend tests and verify GREEN**

Run the Task 3 Vitest command. Expected: pass.

- [ ] **Step 5: Commit Task 3**

```powershell
git commit -m "feat: add return global SKU client contract"
```

---

### Task 4: Excel Template and Import Validation

**Files:**
- Create: `erp-frontend/src/views/platform/return-ops/return-receipt-excel.ts`
- Create: `erp-frontend/src/views/platform/return-ops/return-receipt-excel.vitest.ts`

**Interfaces:**
- Consumes: draft line type from Task 3.
- Produces: `buildReturnReceiptTemplateBuffer()`, `parseReturnReceiptTemplateBuffer()`, `validateReturnReceiptRows()`, `downloadReturnReceiptTemplate()`, and `parseReturnReceiptTemplateFile()`.

- [ ] **Step 1: Write failing Excel tests**

Cover a real ExcelJS round trip with the exact columns `全局 SKU`, `实收数量`, `平台订单号`, `退货原因`; confirm the `填写说明` sheet exists; and assert errors for duplicate normalized SKU, non-positive/non-integer quantity, unsupported reason, wrong header, and row 501.

```ts
const buffer = await buildReturnReceiptTemplateBuffer()
const rows = await parseReturnReceiptTemplateBuffer(buffer)
expect(rows).toEqual([])

expect(validateReturnReceiptRows([
  { excelRow: 2, warehouseSkuCode: 'JHIN-SKU-1', receivedQty: 2, returnReason: 'OTHER' }
])).toEqual({ lines: [expect.objectContaining({ warehouseSkuCode: 'JHIN-SKU-1' })], errors: [] })
```

- [ ] **Step 2: Run Excel tests and verify RED**

```powershell
pnpm --dir erp-frontend exec vitest run src/views/platform/return-ops/return-receipt-excel.vitest.ts --maxWorkers=1 --minWorkers=1
```

Expected: module missing.

- [ ] **Step 3: Implement workbook and parser**

Use dynamic `import('exceljs')`. Freeze row 1, set SKU and order columns to text, add a reason dropdown for rows 2–501, style required input cells, and put field rules plus one clearly marked example in `填写说明` rather than the import sheet. The parser ignores blank rows and preserves Excel row numbers. `validateReturnReceiptRows()` returns no lines whenever errors are present and limits displayed errors to 20 plus a remaining-count message.

- [ ] **Step 4: Run Excel tests and verify GREEN**

Run the Task 4 Vitest command. Expected: all Excel tests pass.

- [ ] **Step 5: Commit Task 4**

```powershell
git commit -m "feat: add return receipt Excel template"
```

---

### Task 5: Return Receipt Modal Integration

**Files:**
- Modify: `erp-frontend/src/views/platform/return-ops/ReturnReceiptModal.vue`
- Modify: `erp-frontend/src/views/platform/return-ops/return-receipt-flow.vitest.ts`

**Interfaces:**
- Consumes: Tasks 3 and 4 resolver, draft, workbook, and parser APIs.
- Produces: one-field manual global SKU entry, batch import preview, and global-SKU-only receipt payload.

- [ ] **Step 1: Extend failing flow tests for submission payload**

Add a pure `buildReturnReceiptPayload(form, lines)` test proving the payload contains `warehouseSkuCode`, quantity, order number, reason, and photo IDs but contains neither `erpTenantId` nor `skuCode`.

- [ ] **Step 2: Run flow test and verify RED**

Run the Task 3 Vitest command. Expected: missing payload builder.

- [ ] **Step 3: Implement modal integration**

Remove `PlatformOwnerSelect`. Replace the two old columns with a global SKU input and a resolved information block. Debounce manual resolution by 350 ms. Add “下载模板” and upload-based “导入 Excel” buttons. Validate and resolve imported rows before replacing `lines`; keep the existing draft if parsing or resolution fails. Disable submit while any line is resolving or unmatched. Preserve warehouse/date/remark, quantity, order number, reason, photo upload, and row removal.

Call `buildReturnReceiptPayload()` from `submit()` so the network payload has no owner or original SKU fields.

- [ ] **Step 4: Run focused tests, type check, and lint**

```powershell
pnpm --dir erp-frontend exec vitest run src/views/platform/return-ops/return-receipt-flow.vitest.ts src/views/platform/return-ops/return-receipt-excel.vitest.ts --maxWorkers=1 --minWorkers=1
pnpm --dir erp-frontend type-check
pnpm --dir erp-frontend exec eslint src/views/platform/return-ops/ReturnReceiptModal.vue src/views/platform/return-ops/return-receipt-flow.ts src/views/platform/return-ops/return-receipt-excel.ts
```

Expected: all commands exit 0 with no ESLint findings.

- [ ] **Step 5: Commit Task 5**

```powershell
git commit -m "feat: import return receipts by global SKU"
```

---

### Task 6: Global SKU Display and Internationalized Copy

**Files:**
- Modify: `erp-frontend/src/views/platform/return-ops/ReturnProcessModal.vue`
- Modify: `erp-frontend/src/locales/lang/zh-CN/platform.json`
- Modify: `erp-frontend/src/locales/lang/en-US/platform.json`
- Modify: `erp-frontend/src/locales/lang/ru-RU/platform.json`
- Modify: `erp-frontend/src/locales/lang/uk-UA/platform.json`
- Modify: `erp-frontend/src/locales/platform-i18n.vitest.ts`

**Interfaces:**
- Consumes: backend `ReturnOrderItemVO.warehouseSkuCode` and existing i18n contract.
- Produces: consistent global SKU labels and localized import/error copy.

- [ ] **Step 1: Add failing i18n contract assertions**

Add required keys for global SKU, matched owner/product, download template, import Excel, resolving, unmatched, import success, import failure, and maximum row count. Existing locale alignment test must require all four language files to expose identical keys and distinct non-empty translations.

- [ ] **Step 2: Run i18n test and verify RED**

```powershell
pnpm --dir erp-frontend exec vitest run src/locales/platform-i18n.vitest.ts --maxWorkers=1 --minWorkers=1
```

Expected: missing keys in all or some locales.

- [ ] **Step 3: Add translations and enforce global SKU display**

Replace old receipt description and validation text that asks for owner/internal SKU. Add all four language translations. In `ReturnProcessModal.vue`, render `record.warehouseSkuCode` only; when it is absent, render an error badge reading the localized “全局 SKU 缺失” and disable processing submission instead of showing `record.skuCode` as a fallback.

- [ ] **Step 4: Run i18n, type, and lint verification**

Run the Task 6 Vitest command, `pnpm --dir erp-frontend type-check`, and ESLint on both return modals plus locale test. Expected: exit 0.

- [ ] **Step 5: Commit Task 6**

Stage only return-related locale hunks along with the modal and test, then commit:

```powershell
git commit -m "fix: identify return goods by global SKU"
```

---

### Task 7: End-to-End Regression Verification

**Files:**
- Verify all files changed in Tasks 1–6.

**Interfaces:**
- Consumes: complete backend and frontend implementation.
- Produces: evidence that the feature builds and existing workflows remain green.

- [ ] **Step 1: Run all backend return tests**

```powershell
mvn -f erp-backend/pom.xml -pl admin -am -Dtest=ReturnQcServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: exit 0, zero failed tests.

- [ ] **Step 2: Run all frontend tests**

```powershell
pnpm --dir erp-frontend exec vitest run --maxWorkers=1 --minWorkers=1
```

Expected: exit 0, zero failed tests.

- [ ] **Step 3: Run frontend type, lint, and production build checks**

```powershell
pnpm --dir erp-frontend type-check
pnpm --dir erp-frontend exec eslint src/views/platform/return-ops src/api/wms/return-qc src/locales/platform-i18n.vitest.ts
pnpm --dir erp-frontend build-only
```

Expected: type check and lint exit 0; Vite reports a successful production build. Existing bundle-size warnings are informational and do not fail the build.

- [ ] **Step 4: Check repository diff hygiene**

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors; status contains only intended implementation changes plus pre-existing unrelated worktree changes.

- [ ] **Step 5: Perform local smoke check**

With the existing frontend and backend running, open the return-processing page, download the template, import one valid and one invalid workbook, verify the valid rows show canonical SKU/product/owner, verify invalid import preserves the old draft, and verify the network receipt body contains `warehouseSkuCode` without `erpTenantId` or `skuCode`.
