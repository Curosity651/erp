# WMS 服务商模块发布核验记录

## 发布信息

- 核验日期：2026-08-24
- 核验范围：货主管理、物流产品、财务收入、财务支出、仓储概览、运营数据分析
- 数据库：Docker `erp-mysql` / `erp`
- 后端：`http://localhost:8081`
- 前端：`http://localhost:5360`

## 数据库备份

- 文件：`C:\Users\86178\Desktop\erp-provider-backup-20260824-081903\provider-backup.sql`
- 大小：92919 bytes
- SHA256：`B5E4AC612F8E333A87D21838746D624F09C0B21FDB5944DDAEA7E0396997CE89`
- 包含表：月度账单、服务商收入、物流产品、店铺、履约单、物理库存、货架分配、菜单和角色菜单。

## 数据库迁移

- 已执行：`V127__provider_finance_currency_and_indexes.sql`
- `wms_monthly_bill.currency`：`varchar(8) NOT NULL DEFAULT 'CNY'`
- 已建立索引：`idx_monthly_bill_tenant_month_status_currency`
- 已建立索引：`idx_client_billing_tenant_month_currency`
- 活动菜单 `180500 服务商看板`：0 条，仅保留运营数据分析。
- 当前月度账单为空，无历史金额需要回填；当前服务商收入记录为 RUB 6 条，金额合计 0.00，原币种未改写。

## 自动化验证

- 后端专项测试：32 项通过，0 失败，0 错误。
- 覆盖：货主隔离、物流产品编码/引用保护/权限、财务币种、账单状态、莫斯科账期、仓储库存归属、看板菜单唯一性。
- 前端 Vitest：3 项通过。
- 前端 `vue-tsc --noEmit`：通过。
- 前端生产构建：通过。
- 后端 Maven 打包：通过。
- 前端 HTTP：200。
- 后端 Spring Boot：已在 8081 端口启动，启动日志无 Mapper、SQL 或数据库连接错误。

## 已知非阻断项

- 前端构建仍报告项目原有的 `DicItemAttributesEditor.vue` 常量双向绑定警告。
- 前端构建仍报告部分大于 500 kB 的代码块警告。
- OSS 配置启动时提示两个逻辑配置指向同一个桶；本轮未调整文件存储配置。

## 回滚说明

- 应用回滚可恢复本轮代码前的 Git 版本。
- 数据库回滚时建议保留新增 `currency` 字段和索引，它们兼容旧应用且不会改变已有金额。
- 如需恢复数据，使用上述备份文件，并先在独立数据库验证。
