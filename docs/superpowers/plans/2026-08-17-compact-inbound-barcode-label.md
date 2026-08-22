# 收货商品紧凑条形码标签实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将收货完成后的商品标签从 80×50mm 二维码信息卡改为 50×25mm Code 128 条形码标签。

**Architecture:** 保留现有收货列表打印入口和实收商品聚合逻辑。前端使用 `jsbarcode` 将内部仓库 SKU 生成 Code 128 图片，纯函数负责输出紧凑打印文档，便于通过 Node 测试验证尺寸、内容和打印份数。

**Tech Stack:** Vue 3、TypeScript、jsbarcode、node:test、pnpm。

## Global Constraints

- 每个实收箱子打印一张标签。
- 标签固定为 50mm × 25mm。
- 标签上方只显示 Code 128 条形码，下方只显示完整内部仓库 SKU。
- 不显示二维码、货主、原始 SKU、商品名称、入库单号或件数序号。
- 不修改货物对照表、收货状态或后端接口。

---

### Task 1: 条形码标签生成与打印

**Files:**
- Modify: `erp-frontend/src/views/platform/inbound-ops/received-goods-print.test.ts`
- Modify: `erp-frontend/src/views/platform/inbound-ops/received-goods-print.ts`
- Modify: `erp-frontend/package.json`
- Modify: `erp-frontend/pnpm-lock.yaml`

**Interfaces:**
- Consumes: `ReceivedGoodsPrintItem[]`、内部仓库 SKU 和实收数量。
- Produces: `buildReceivedSkuLabelDocument(groups)` 返回可写入打印窗口的完整 HTML；`printReceivedSkuLabels()` 使用 Code 128 图片调用该函数。

- [x] **Step 1: 写失败测试**

在 `received-goods-print.test.ts` 中断言输出包含 `@page { size: 50mm 25mm; margin: 0; }`、每件一张标签、条形码图片和内部 SKU，同时不包含货主、原始 SKU、商品名称或件数序号。

- [x] **Step 2: 验证测试因缺少新函数而失败**

Run: `pnpm exec tsx --test src/views/platform/inbound-ops/received-goods-print.test.ts`

Expected: FAIL，提示 `buildReceivedSkuLabelDocument` 尚未导出。

- [x] **Step 3: 安装条码依赖并完成最小实现**

Run: `pnpm add jsbarcode && pnpm add -D @types/jsbarcode`

在 `received-goods-print.ts` 中使用浏览器 canvas 生成 `CODE128`，关闭库自带文字，保留必要静区；新增纯 HTML 生成函数并将打印模板改为 50×25mm。

- [x] **Step 4: 运行专项测试和类型检查**

Run: `pnpm exec tsx --test src/views/platform/inbound-ops/received-goods-print.test.ts`

Expected: PASS。

Run: `pnpm typecheck`

Expected: 与本次修改相关的 TypeScript 检查通过；若仓库已有无关错误，记录具体文件和错误。

- [x] **Step 5: 检查变更范围**

Run: `git diff --check -- erp-frontend/package.json erp-frontend/pnpm-lock.yaml erp-frontend/src/views/platform/inbound-ops/received-goods-print.ts erp-frontend/src/views/platform/inbound-ops/received-goods-print.test.ts`

Expected: 无空白错误，货物对照表代码未发生行为变化。
