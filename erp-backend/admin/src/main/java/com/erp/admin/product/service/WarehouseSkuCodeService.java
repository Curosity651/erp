package com.erp.admin.product.service;

import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Generates the owner-scoped SKU code used on overseas warehouse labels.
 */
@Service
@RequiredArgsConstructor
public class WarehouseSkuCodeService {

	private final SysTenantMapper tenantMapper;

	public String build(Long erpTenantId, String skuCode) {
		Assert.notNull(erpTenantId, "货主不能为空");
		Assert.hasText(skuCode, "SKU不能为空");
		SysTenant owner = tenantMapper.selectById(erpTenantId);
		Assert.notNull(owner, "货主不存在");
		Assert.hasText(owner.getTenantName(), "货主名称未配置，无法生成仓库内部SKU");
		String ownerName = owner.getTenantName().trim().replaceAll("\\s+", "_");
		return ownerName.toUpperCase(Locale.ROOT) + "-" + skuCode.trim();
	}

	public boolean matches(Long erpTenantId, String skuCode, String scanCode) {
		if (!StringUtils.hasText(skuCode) || !StringUtils.hasText(scanCode)) {
			return false;
		}
		try {
			return build(erpTenantId, skuCode).equalsIgnoreCase(scanCode.trim());
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

}
