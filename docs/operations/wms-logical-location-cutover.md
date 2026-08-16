# 逻辑库位库存切换记录

## 基线信息

- 记录时间：2026-08-16
- 功能分支：`codex/logical-location-fulfillment`
- 后端：Java 8、Spring Boot 2.7.18、Maven
- 前端：Vue 3、TypeScript、pnpm
- 初始库存模式：`LEGACY`

## 改造前验证

### 后端打包

执行命令：

```powershell
mvn -f erp-backend/pom.xml -pl admin -DskipTests package
```

结果：`BUILD SUCCESS`，耗时 5 分 25 秒。首次执行下载了 BallCat 快照依赖。

现有警告：MapStruct 存在未映射目标字段，以及少量废弃 API、未检查泛型警告。它们在本次改造前已经存在，不阻止编译，本任务不处理。

### 前端类型检查

执行命令：

```powershell
pnpm --dir erp-frontend type-check
```

结果：通过，退出码为 0。

## 切换规则

1. 任务 1 至任务 19 实施期间保持 `erp.wms.core-mode=LEGACY`。
2. 完成数据库备份、业务数据清理、新库位初始化和全链路验证后，才允许改为 `LOGICAL_LOCATION`。
3. 切换前停止本项目的 Java 与 Vite 进程，MySQL 和 Redis 容器保持运行。
4. 旧托盘、托位、物理库存和销售出库写接口在新模式下必须被禁止，历史查询保留只读。
