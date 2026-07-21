package com.erp.admin.wms.service;

import java.util.Collections;
import java.util.List;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.tenant.enums.TenantType;
import com.erp.admin.tenant.service.TenantHierarchyService;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.WmsSkuLookupMapper;
import com.erp.admin.wms.model.vo.SkuLookupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 作业用 SKU 速查服务（数据级可见性 ②）。
 *
 * <p>作用域按身份：
 * <ul>
 * <li>平台超管：跨全部货主（它负责所有货物的物理收发）。</li>
 * <li>WMS 服务商：仅名下货主子树。</li>
 * <li>货主：仅自己。</li>
 * </ul>
 * 仅供收货/上架/出库等作业流程按 SKU 认货，不提供商品浏览页。
 *
 * @author erp
 */
@Service
@RequiredArgsConstructor
public class WmsSkuLookupService {

	private final WmsSkuLookupMapper skuLookupMapper;

	private final TenantIdentityService tenantIdentityService;

	private final TenantHierarchyService tenantHierarchyService;

	/**
	 * 按 SKU 编码速查商品（作用域随身份）。
	 * @param skuCode SKU 编码
	 * @return 命中的 SKU（可能多个货主同编码）
	 */
	public List<SkuLookupVO> lookup(String skuCode) {
		if (skuCode == null || skuCode.isEmpty()) {
			return Collections.emptyList();
		}
		String identity = tenantIdentityService.currentIdentity(null).getIdentityType();
		if (TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identity)) {
			return skuLookupMapper.findBySkuCodeAllTenants(skuCode);
		}
		if (TenantType.WMS_OPERATOR.name().equals(identity)) {
			List<Long> erpTenantIds = tenantHierarchyService
				.descendantErpTenantIds(WmsTenantContext.getCurrentWmsTenant());
			if (erpTenantIds.isEmpty()) {
				return Collections.emptyList();
			}
			return skuLookupMapper.findBySkuCodeInTenants(skuCode, erpTenantIds);
		}
		// 货主：仅自己
		Long own = TenantContext.getCurrentTenant();
		if (own == null) {
			return Collections.emptyList();
		}
		return skuLookupMapper.findBySkuCodeInTenants(skuCode, Collections.singletonList(own));
	}

}
