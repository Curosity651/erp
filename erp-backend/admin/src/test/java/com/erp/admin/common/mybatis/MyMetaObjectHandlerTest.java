package com.erp.admin.common.mybatis;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.tenant.exception.TenantBusinessException;
import com.erp.admin.wms.model.entity.StockPosting;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MyMetaObjectHandlerTest {

	private final MyMetaObjectHandler handler = new FillFreeMetaObjectHandler();

	@AfterEach
	void clearTenantContext() {
		TenantContext.clear();
	}

	@Test
	void aggregate_stock_posting_may_keep_null_header_owner_in_platform_context() {
		TenantContext.setCurrentTenant(TenantContext.BLOCK_TENANT_ID);
		StockPosting posting = new StockPosting();
		posting.setAggregateTenantPosting(true);

		assertThatCode(() -> handler.insertFill(SystemMetaObject.forObject(posting)))
				.doesNotThrowAnyException();
		assertThat(posting.getErpTenantId()).isNull();
	}

	@Test
	void normal_stock_posting_still_requires_a_positive_tenant_context() {
		TenantContext.setCurrentTenant(TenantContext.BLOCK_TENANT_ID);
		StockPosting posting = new StockPosting();

		assertThatThrownBy(() -> handler.insertFill(SystemMetaObject.forObject(posting)))
				.isInstanceOf(TenantBusinessException.class);
	}

	private static class FillFreeMetaObjectHandler extends MyMetaObjectHandler {

		@Override
		public <T, E extends T> MetaObjectHandler strictInsertFill(MetaObject metaObject, String fieldName,
				Class<T> fieldType, E fieldVal) {
			return this;
		}
	}
}
