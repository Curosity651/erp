package com.erp.admin.tenant.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.tenant.enums.TenantType;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.tenant.model.qo.TenantQO;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

/**
 * 租户 Mapper
 *
 * @author erp
 */
public interface SysTenantMapper extends ExtendMapper<SysTenant> {

	/**
	 * 分页查询 WMS 服务商（按编码/名称/状态过滤）。
	 * @param pageParam 分页参数
	 * @param qo 查询条件
	 * @return 服务商分页
	 */
	default PageResult<SysTenant> pageWmsOperators(PageParam pageParam, TenantQO qo) {
		IPage<SysTenant> page = this.prodPage(pageParam);
		LambdaQueryWrapperX<SysTenant> wrapper = WrappersX.lambdaQueryX(SysTenant.class)
			.eq(SysTenant::getTenantType, TenantType.WMS_OPERATOR.name())
			.likeIfPresent(SysTenant::getTenantCode, qo.getTenantCode())
			.likeIfPresent(SysTenant::getTenantName, qo.getTenantName())
			.eqIfPresent(SysTenant::getStatus, qo.getStatus())
			.orderByDesc(SysTenant::getId);
		this.selectPage(page, wrapper);
		return new PageResult<>(page.getRecords(), page.getTotal());
	}

	/**
	 * 分页查询某服务商名下的货主（按编码/名称/状态过滤）。
	 * @param pageParam 分页参数
	 * @param qo 查询条件
	 * @param parentWmsTenantId 父服务商租户ID（限定名下）
	 * @return 货主分页
	 */
	default PageResult<SysTenant> pageErpTenants(PageParam pageParam, TenantQO qo, Long parentWmsTenantId) {
		IPage<SysTenant> page = this.prodPage(pageParam);
		LambdaQueryWrapperX<SysTenant> wrapper = WrappersX.lambdaQueryX(SysTenant.class)
			.eq(SysTenant::getTenantType, TenantType.ERP_USER.name())
			.eq(SysTenant::getParentWmsTenantId, parentWmsTenantId)
			.likeIfPresent(SysTenant::getTenantCode, qo.getTenantCode())
			.likeIfPresent(SysTenant::getTenantName, qo.getTenantName())
			.eqIfPresent(SysTenant::getStatus, qo.getStatus())
			.orderByDesc(SysTenant::getId);
		this.selectPage(page, wrapper);
		return new PageResult<>(page.getRecords(), page.getTotal());
	}

	/**
	 * 全部启用中的货主租户 ID（后台任务逐货主 runAs 用，如库存预警）。
	 * @return 启用货主 id 列表
	 */
	default java.util.List<Long> listEnabledErpTenantIds() {
		return this.selectList(WrappersX.lambdaQueryX(SysTenant.class)
			.eq(SysTenant::getTenantType, TenantType.ERP_USER.name())
			.eq(SysTenant::getStatus, 1)
			.orderByAsc(SysTenant::getId))
			.stream()
			.map(SysTenant::getId)
			.collect(java.util.stream.Collectors.toList());
	}

	default java.util.List<Long> listEnabledErpTenantIdsByParent(Long parentWmsTenantId) {
		return this.selectList(WrappersX.lambdaQueryX(SysTenant.class)
			.eq(SysTenant::getTenantType, TenantType.ERP_USER.name())
			.eq(SysTenant::getParentWmsTenantId, parentWmsTenantId)
			.eq(SysTenant::getStatus, 1)
			.orderByAsc(SysTenant::getId))
			.stream()
			.map(SysTenant::getId)
			.collect(java.util.stream.Collectors.toList());
	}

}
