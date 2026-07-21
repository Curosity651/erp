package com.erp.admin.tenant.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.erp.admin.tenant.enums.TenantType;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import lombok.RequiredArgsConstructor;
import org.ballcat.mybatisplus.toolkit.WrappersX;
import org.springframework.stereotype.Service;

/**
 * 租户派生树查询（数据级可见性配套）。
 *
 * <p>用于"祖先按子树范围读取"的作用域解析：WMS 服务商 → 其名下货主 id 集合。 平台超管对货主商品/货物的可见性为"全部"，不经此服务限定。
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class TenantHierarchyService {

	private final SysTenantMapper sysTenantMapper;

	/**
	 * 取某 WMS 服务商名下全部货主的 tenant_id。
	 * @param wmsTenantId 服务商租户ID
	 * @return 货主 id 列表（无则空）
	 */
	public List<Long> descendantErpTenantIds(Long wmsTenantId) {
		if (wmsTenantId == null) {
			return Collections.emptyList();
		}
		return sysTenantMapper
			.selectList(WrappersX.lambdaQueryX(SysTenant.class)
				.select(SysTenant::getId)
				.eq(SysTenant::getTenantType, TenantType.ERP_USER.name())
				.eq(SysTenant::getParentWmsTenantId, wmsTenantId))
			.stream()
			.map(SysTenant::getId)
			.collect(Collectors.toList());
	}

}
