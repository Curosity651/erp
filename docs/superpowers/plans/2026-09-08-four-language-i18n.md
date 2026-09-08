# ERP Four-Language Internationalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver complete Chinese, English, Ukrainian, and Russian localization for all user-visible web UI, menus, messages, print documents, labels, and Excel exports.

**Architecture:** Keep translations in versioned code resources. The Vue application uses domain-scoped `vue-i18n` files and one persisted locale source; Spring resolves localized business messages from resource bundles using the existing `Accept-Language` request header. Dynamic business data remains unchanged, while stable menu paths, status codes, print headings, and export columns map to translation keys.

**Tech Stack:** Vue 3.5, TypeScript 5.7, Vue I18n 9, Pinia, Ant Design Vue 4, Day.js, `Intl`, Vitest 2, Spring Boot 2.7, Java 11, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-09-08-four-language-i18n-design.md`

## Global Constraints

- Supported locale codes are exactly `zh-CN`, `en-US`, `uk-UA`, and `ru-RU`.
- `zh-CN` is the first-visit default and the fallback locale.
- The selected locale persists in browser local storage and is not inferred from the browser.
- Printing and exporting always follow the current UI locale.
- User-entered and third-party business data is never automatically translated.
- Missing translations fall back to Chinese and are reported by development/test validation.
- External platform-generated labels remain unchanged; only system-owned surrounding content is localized.
- Do not add translation database tables or an online translation editor.

---

### Task 1: Locale Contract and Parity Test

**Files:**
- Create: `erp-frontend/src/locales/types.ts`
- Create: `erp-frontend/src/locales/locale-contract.ts`
- Create: `erp-frontend/src/locales/locale-contract.vitest.ts`
- Modify: `erp-frontend/src/config/index.ts`

**Interfaces:**
- Produces: `SUPPORTED_LOCALES`, `SupportedLocale`, `DEFAULT_LOCALE`, `isSupportedLocale()`.
- Produces: a recursive key flattener used by parity tests and residual validation.

- [ ] **Step 1: Write the failing locale contract test**

```ts
import { describe, expect, it } from 'vitest'
import { DEFAULT_LOCALE, SUPPORTED_LOCALES, isSupportedLocale } from './locale-contract'

describe('locale contract', () => {
  it('supports exactly the approved locales', () => {
    expect(SUPPORTED_LOCALES).toEqual(['zh-CN', 'en-US', 'uk-UA', 'ru-RU'])
    expect(DEFAULT_LOCALE).toBe('zh-CN')
    expect(isSupportedLocale('ru-RU')).toBe(true)
    expect(isSupportedLocale('de-DE')).toBe(false)
  })
})
```

- [ ] **Step 2: Run the test and verify the missing module failure**

Run: `pnpm exec vitest run src/locales/locale-contract.vitest.ts`

Expected: FAIL because `locale-contract.ts` does not exist.

- [ ] **Step 3: Implement the locale contract and enable i18n**

```ts
export const SUPPORTED_LOCALES = ['zh-CN', 'en-US', 'uk-UA', 'ru-RU'] as const
export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number]
export const DEFAULT_LOCALE: SupportedLocale = 'zh-CN'
export const isSupportedLocale = (value: string): value is SupportedLocale =>
  SUPPORTED_LOCALES.includes(value as SupportedLocale)
```

Set `enableI18n = true`, use the contract for `defaultLanguage`, and add Ukrainian and Russian display metadata to `supportLanguage`.

- [ ] **Step 4: Run the focused test and type check**

Run: `pnpm exec vitest run src/locales/locale-contract.vitest.ts && pnpm type-check`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add erp-frontend/src/config/index.ts erp-frontend/src/locales
git commit -m "feat(i18n): define four-language locale contract"
```

### Task 2: Four Locale Loaders and Persistent Switching

**Files:**
- Create: `erp-frontend/src/locales/lang/uk-UA.ts`
- Create: `erp-frontend/src/locales/lang/ru-RU.ts`
- Create: `erp-frontend/src/locales/lang/uk-UA/common.json`
- Create: `erp-frontend/src/locales/lang/ru-RU/common.json`
- Modify: `erp-frontend/src/locales/lang/zh-CN.ts`
- Modify: `erp-frontend/src/locales/lang/en-US.ts`
- Modify: `erp-frontend/src/locales/index.ts`
- Modify: `erp-frontend/src/locales/dayjs.ts`
- Modify: `erp-frontend/src/stores/i18n-store.ts`
- Modify: `erp-frontend/src/components/SelectLanguage/index.vue`
- Test: `erp-frontend/src/locales/language-switch.vitest.ts`

**Interfaces:**
- Consumes: `SupportedLocale`, `DEFAULT_LOCALE`, `isSupportedLocale()`.
- Produces: `loadLanguageAsync(locale: SupportedLocale): Promise<SupportedLocale>` and persisted `i18nStore.language`.

- [ ] **Step 1: Write tests for stored-language restoration and invalid-value fallback**
- [ ] **Step 2: Run the tests and confirm the existing initializer resets the saved language**
- [ ] **Step 3: Add `uk_UA`, `ru_RU`, Day.js `uk`/`ru`, and locale loaders**
- [ ] **Step 4: Change `install()` to load a valid stored value instead of unconditionally storing Chinese**
- [ ] **Step 5: Await language loading before rebuilding dynamic routes and add a localized failure notification**
- [ ] **Step 6: Run locale tests, type check, and production build**
- [ ] **Step 7: Commit with `feat(i18n): add persistent Ukrainian and Russian switching`**

### Task 3: Translation Resource Structure and Validation Tooling

**Files:**
- Create: `erp-frontend/src/locales/lang/{zh-CN,en-US,uk-UA,ru-RU}/{common,auth,menu,system,product,order,inbound,outbound,inventory,warehouse,finance,dashboard,print}.json`
- Create: `erp-frontend/scripts/check-i18n-keys.mjs`
- Create: `erp-frontend/scripts/check-user-visible-literals.mjs`
- Modify: `erp-frontend/package.json`
- Test: `erp-frontend/src/locales/resource-parity.vitest.ts`

**Interfaces:**
- Produces: `pnpm i18n:check` and `pnpm i18n:scan`.
- Produces: identical flattened key sets for all locale resources.

- [ ] **Step 1: Add a parity test that intentionally fails while domain files are absent**
- [ ] **Step 2: Create all domain files with the shared key hierarchy and approved translations**
- [ ] **Step 3: Implement `check-i18n-keys.mjs` to compare recursive key sets and reject empty translations**
- [ ] **Step 4: Implement the literal scanner with explicit exclusions for comments, tests, fixtures, third-party labels, and business sample data**
- [ ] **Step 5: Add `i18n:check` and `i18n:scan` package scripts**
- [ ] **Step 6: Run parity tests and `pnpm i18n:check`**
- [ ] **Step 7: Commit with `test(i18n): enforce locale resource parity`**

### Task 4: Shell, Dynamic Menu, Tabs, and Page Titles

**Files:**
- Create: `erp-frontend/src/locales/menu.ts`
- Modify: `erp-frontend/src/router/dynamic-routes.ts`
- Modify: `erp-frontend/src/router/guards.ts`
- Modify: `erp-frontend/src/layouts/BasicLayout.vue`
- Modify: `erp-frontend/src/layouts/components/MultiTab/**`
- Modify: `erp-frontend/src/layouts/components/RightContent/index.vue`
- Modify: `erp-frontend/src/layouts/components/RightContent/AvatarDropdown.vue`
- Modify: `erp-frontend/src/views/login/**`
- Test: `erp-frontend/src/locales/menu-localization.vitest.ts`

**Interfaces:**
- Produces: `resolveMenuTitle(path: string, fallback: string): string`.
- Produces: `refreshLocalizedRouteTitles()` invoked after a locale switch without clearing current tabs or form state.

- [ ] **Step 1: Write tests for known menu-path translation and unknown-path fallback**
- [ ] **Step 2: Implement menu path normalization and `menu.<normalizedPath>` lookup**
- [ ] **Step 3: Store a stable `localeKey` in route metadata instead of freezing the current translated text**
- [ ] **Step 4: Make sidebar, breadcrumbs, tabs, and `document.title` resolve translations reactively**
- [ ] **Step 5: Localize login, logout, profile, session expiry, 403, 404, and 500 pages**
- [ ] **Step 6: Verify switching languages does not reset routes, open tabs, forms, or filters**
- [ ] **Step 7: Commit with `feat(i18n): localize application shell and dynamic menus`**

### Task 5: Shared Formatters, Enumerations, and Reusable Components

**Files:**
- Create: `erp-frontend/src/utils/locale-format.ts`
- Create: `erp-frontend/src/utils/locale-format.vitest.ts`
- Create: `erp-frontend/src/locales/business-enums.ts`
- Modify: `erp-frontend/src/components/**`
- Modify: `erp-frontend/src/hooks/**`
- Modify: `erp-frontend/src/utils/**`
- Modify: `erp-frontend/src/constants/**`

**Interfaces:**
- Produces: `formatLocaleDate`, `formatLocaleDateTime`, `formatLocaleNumber`, `formatLocaleCurrency`.
- Produces: `translateStatus(domain: string, code: string, fallback?: string)`.

- [ ] **Step 1: Write tests that compare Chinese, English, Ukrainian, and Russian number/date output**
- [ ] **Step 2: Implement locale-aware formatters using `Intl` and the active store locale**
- [ ] **Step 3: Replace hard-coded `toLocaleString('zh-CN')` and `toLocaleString('en-US')` where the value is user-facing**
- [ ] **Step 4: Centralize stable order, inventory, location, quality, finance, and operation status translations**
- [ ] **Step 5: Localize shared buttons, table tools, upload controls, confirmation dialogs, LOV, dictionary display, empty states, and validation messages**
- [ ] **Step 6: Run formatter tests, existing component tests, type check, and literal scan**
- [ ] **Step 7: Commit with `feat(i18n): localize shared components and formatting`**

### Task 6: ERP Owner Business Pages

**Files:**
- Modify: `erp-frontend/src/views/order/**`
- Modify: `erp-frontend/src/views/product/**`
- Modify: `erp-frontend/src/views/shop/**`
- Modify: `erp-frontend/src/views/wms/purchase-order/**`
- Modify: `erp-frontend/src/views/wms/purchase-inbound/**`
- Modify: `erp-frontend/src/views/wms/return-inbound/**`
- Modify: `erp-frontend/src/views/wms/manual-inbound/**`
- Modify: `erp-frontend/src/views/wms/custom-outbound/**`
- Modify: corresponding `order.json`, `product.json`, and `inbound.json` files for all four locales.

**Interfaces:**
- Consumes: common translation keys, locale formatters, and business enum translators.
- Produces: fully localized ERP owner ordering, SKU, store, purchasing, inbound, return, and manual outbound pages.

- [ ] **Step 1: Create a page inventory listing every user-visible literal in these directories**
- [ ] **Step 2: Add Chinese semantic keys, then matching English, Ukrainian, and Russian translations**
- [ ] **Step 3: Replace template text, table titles, placeholders, notifications, modal copy, and validation messages with `t()`**
- [ ] **Step 4: Preserve platform names, SKU values, addresses, product names, and user remarks as raw business data**
- [ ] **Step 5: Run owner-flow tests, type check, and scoped literal scan**
- [ ] **Step 6: Commit with `feat(i18n): localize ERP owner workflows`**

### Task 7: WMS Service Provider Business Pages

**Files:**
- Modify: `erp-frontend/src/views/wms/logistics-product/**`
- Modify: `erp-frontend/src/views/wms/fund-settlement/**`
- Modify: `erp-frontend/src/views/wms/asset-finance/**`
- Modify: `erp-frontend/src/views/wms/finance-income/**`
- Modify: `erp-frontend/src/views/wms/storage-overview/**`
- Modify: `erp-frontend/src/views/wms/inventory/**`
- Modify: corresponding `warehouse.json`, `inventory.json`, and `finance.json` files for all four locales.

**Interfaces:**
- Produces: localized service-provider product, settlement, account, storage, and inventory workflows.

- [ ] **Step 1: Add all provider-domain translation keys in Chinese and mirror them in the other languages**
- [ ] **Step 2: Replace page, modal, table, filter, status, chart legend, and account-ledger text**
- [ ] **Step 3: Verify currency codes remain business data while number layout follows locale**
- [ ] **Step 4: Run provider finance, logistics product, inventory, and storage tests**
- [ ] **Step 5: Run type check and scoped literal scan**
- [ ] **Step 6: Commit with `feat(i18n): localize WMS provider workflows`**

### Task 8: Overseas Warehouse Platform Pages

**Files:**
- Modify: `erp-frontend/src/views/platform/**`
- Modify: `erp-frontend/src/views/platform-finance/**`
- Modify: `erp-frontend/src/views/wms/location-mgmt/**`
- Modify: `erp-frontend/src/views/wms/stocktake/**`
- Modify: `erp-frontend/src/views/wms/return-qc/**`
- Modify: corresponding `inbound.json`, `outbound.json`, `warehouse.json`, `inventory.json`, and `finance.json` files.

**Interfaces:**
- Produces: localized receiving, putaway, order shelving, picking, outbound, location inventory, location transfer, stocktake, return QC, scrapping, contracts, receivables, and provider settlement pages.

- [ ] **Step 1: Inventory all platform literals and group keys by operation rather than component name**
- [ ] **Step 2: Localize filters, tables, drawers, task steps, scan instructions, warnings, confirmations, result summaries, and status labels**
- [ ] **Step 3: Ensure scanner workflows keep focus and entered values when locale changes**
- [ ] **Step 4: Run all existing platform flow and policy tests**
- [ ] **Step 5: Run type check and scoped literal scan**
- [ ] **Step 6: Commit with `feat(i18n): localize overseas warehouse operations`**

### Task 9: System, Notifications, Financial, and Dashboard Pages

**Files:**
- Modify: `erp-frontend/src/views/system/**`
- Modify: `erp-frontend/src/views/log/**`
- Modify: `erp-frontend/src/views/notify/**`
- Modify: `erp-frontend/src/views/financial/**`
- Modify: `erp-frontend/src/views/dashboard/**`
- Modify: `erp-frontend/src/views/basic/**`
- Modify: corresponding `system.json`, `finance.json`, and `dashboard.json` files.

**Interfaces:**
- Produces: localized administration, logs, announcements, reconciliation, analytics, and chart presentation.

- [ ] **Step 1: Add and translate domain keys for all included pages**
- [ ] **Step 2: Localize chart titles, legends, axis units, tooltips, KPI descriptions, table/export controls, and empty states**
- [ ] **Step 3: Update menu-management previews to use localized titles without altering saved menu business data**
- [ ] **Step 4: Run dashboard and finance tests, type check, and scoped literal scan**
- [ ] **Step 5: Commit with `feat(i18n): localize administration finance and analytics`**

### Task 10: Print, Label, and Excel Localization

**Files:**
- Modify: `erp-frontend/src/views/platform/fulfillment-picking/picking-task-print.ts`
- Modify: `erp-frontend/src/views/platform/inbound-ops/received-goods-print.ts`
- Modify: `erp-frontend/src/views/platform/inbound-ops/putaway-receipt-print.ts`
- Modify: `erp-frontend/src/views/platform/inbound-ops/putaway-record-excel.ts`
- Modify: `erp-frontend/src/views/wms/purchase-inbound/inbound-print.ts`
- Modify: `erp-frontend/src/views/order/yd-order/YdOrderPrintDialog.vue`
- Modify: `erp-frontend/src/views/order/wb-order/WbOrderPrintDialog.vue`
- Modify: `erp-frontend/src/views/order/ozon-order/OzonOrderPrintDialog.vue`
- Modify: `erp-frontend/src/views/order/ozon-order/OzonOrderPickListDialog.vue`
- Modify: `erp-frontend/src/views/platform/outbound-ops/PickListDrawer.vue`
- Modify: `erp-frontend/src/views/platform/pallet/pallet-label-print.ts`
- Modify: `erp-frontend/src/views/wms/shipping-order/ShippingOrderPage.vue`
- Modify: `erp-frontend/src/views/financial/wb-report/WbFinancialReportPage.vue`
- Modify: four `print.json` resource files.
- Test: existing print/export tests plus `erp-frontend/src/locales/print-locale.vitest.ts`.

**Interfaces:**
- Produces: `PrintLocaleContext { locale, t, formatDateTime, formatNumber }`.
- Print/export builders consume the context explicitly so standalone windows cannot silently revert to Chinese.

- [ ] **Step 1: Extend print tests to render one document in each locale and assert localized headings**
- [ ] **Step 2: Add the explicit print locale context and remove hard-coded `<html lang="zh-CN">` values**
- [ ] **Step 3: Localize labels, headings, totals, instructions, file names, worksheet names, and column titles**
- [ ] **Step 4: Keep SKU, order numbers, tracking numbers, recipient data, and external platform labels unchanged**
- [ ] **Step 5: Run all print/export tests and inspect generated HTML and workbooks**
- [ ] **Step 6: Commit with `feat(i18n): localize printing labels and exports`**

### Task 11: Backend Message Resolution Foundation

**Files:**
- Create: `erp-backend/admin/src/main/java/com/erp/admin/common/i18n/BusinessMessageService.java`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/common/i18n/LocalizedBusinessException.java`
- Create: `erp-backend/admin/src/main/resources/i18n/messages.properties`
- Create: `erp-backend/admin/src/main/resources/i18n/messages_en_US.properties`
- Create: `erp-backend/admin/src/main/resources/i18n/messages_uk_UA.properties`
- Create: `erp-backend/admin/src/main/resources/i18n/messages_ru_RU.properties`
- Create: `erp-backend/admin/src/main/java/com/erp/admin/common/i18n/LocalizedBusinessExceptionHandler.java`
- Modify: `erp-backend/admin/src/main/resources/application.yml`
- Test: `erp-backend/admin/src/test/java/com/erp/admin/common/i18n/BusinessMessageServiceTest.java`

**Interfaces:**
- Produces: `message(String code, Object... args)`.
- Produces: `LocalizedBusinessException(String code, Object... args)` carrying an immutable message code and arguments.

- [ ] **Step 1: Write tests for all four locales, parameter interpolation, unsupported locale fallback, and missing-key fallback**
- [ ] **Step 2: Configure basename `i18n/messages` alongside existing framework bundles**
- [ ] **Step 3: Implement message resolution from `LocaleContextHolder`**
- [ ] **Step 4: Add a dedicated highest-precedence handler for `LocalizedBusinessException` that resolves the response text while logging the message key and arguments**
- [ ] **Step 5: Run the focused JUnit test and existing exception/security tests**
- [ ] **Step 6: Commit with `feat(i18n): add localized backend business messages`**

### Task 12: Migrate Backend User-Visible Business Errors

**Files:**
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/order/**`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/product/**`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/shop/**`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/wms/**`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/platform/**`
- Modify: `erp-backend/admin/src/main/java/com/erp/admin/finance/**`
- Modify: all four backend message bundles.
- Create: `erp-backend/admin/src/test/java/com/erp/admin/common/i18n/UserVisibleMessageCoverageTest.java`

**Interfaces:**
- Consumes: `LocalizedBusinessException` and the four message bundles.
- Produces: localized API validation and business-rule responses across normal business workflows.

- [ ] **Step 1: Build a scanner that identifies Chinese literals passed to business exceptions, assertions, and validation annotations**
- [ ] **Step 2: Migrate order, product, and shop errors; run their tests and commit**
- [ ] **Step 3: Migrate inbound, inventory, location, stocktake, return, and scrap errors; run WMS tests and commit**
- [ ] **Step 4: Migrate fulfillment, picking, packing, contracts, settlement, and finance errors; run platform and finance tests and commit**
- [ ] **Step 5: Add four-language HTTP tests using `Accept-Language` and verify stable error codes/arguments**
- [ ] **Step 6: Run the coverage scanner and reject remaining user-visible Chinese exception literals**

### Task 13: Full Regression and Visual Verification

**Files:**
- Create: `erp-frontend/src/locales/i18n-regression.vitest.ts`
- Modify: locale resources only for defects found during verification.
- Modify: responsive styles only where translated text overflows.

**Interfaces:**
- Produces: a release-ready four-language build with documented verification evidence.

- [ ] **Step 1: Run `pnpm i18n:check` and `pnpm i18n:scan`**
- [ ] **Step 2: Run `pnpm exec vitest run`**
- [ ] **Step 3: Run `pnpm type-check && pnpm build-only`**
- [ ] **Step 4: Run `mvn -pl admin test` and `mvn -pl admin package -DskipTests` from `erp-backend`**
- [ ] **Step 5: Start the local frontend and backend and verify login plus representative ERP, WMS, platform, print, export, and error flows in all four languages**
- [ ] **Step 6: Capture desktop and narrow-screen screenshots for each language and correct any overlap, clipping, or unexpected layout shift**
- [ ] **Step 7: Confirm `git diff --check`, inspect the final diff, and commit with `feat: complete four-language internationalization`**
