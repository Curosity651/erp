package com.erp.admin.common.mybatis;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.tenant.enums.TenantResultCode;
import com.erp.admin.tenant.exception.TenantBusinessException;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * 全局 MyBatis-Plus 审计字段填充处理
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

  @Override
  public void insertFill(MetaObject metaObject) {
    LocalDateTime now = LocalDateTime.now();
    this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
    this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
	this.strictInsertFill(metaObject, "deleted", Long.class, 0L);
	// 货主单据 erp_tenant_id 自动盖章（仅对带该字段且未显式赋值的实体生效）：
	// S1 收敛：仅当为正值货主上下文时盖当前货主；无上下文/海外仓平台(-1)一律 fail-closed 抛错，
	// 不再兜底盖成货主 1（原兜底会把漏设上下文的插入静默误挂到货主 1，造成跨租户串数据）。
	// 已显式设值的（入库单/库存/批次/流水）不为 null，strictInsertFill 会跳过，不受影响；平台物理作业均显式赋值。
	if (metaObject.hasGetter("erpTenantId") && metaObject.getValue("erpTenantId") == null) {
	  boolean aggregateTenantPosting = metaObject.hasGetter("aggregateTenantPosting")
		  && Boolean.TRUE.equals(metaObject.getValue("aggregateTenantPosting"));
	  if (aggregateTenantPosting) {
		return;
	  }
	  Long erp = TenantContext.getCurrentTenant();
	  if (erp == null || erp <= 0) {
	    throw new TenantBusinessException(TenantResultCode.NO_TENANT_CONTEXT);
	  }
	  this.strictInsertFill(metaObject, "erpTenantId", Long.class, erp);
	}
  }

  @Override
  public void updateFill(MetaObject metaObject) {
    this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
  }
}
