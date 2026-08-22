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
		Assert.hasText(owner.getWarehouseSkuPrefix(), "货主仓库SKU前缀未配置，无法生成内部SKU");
		return owner.getWarehouseSkuPrefix().trim().toUpperCase(Locale.ROOT) + "-" + skuCode.trim();
	}

	public static String normalizePrefix(String ownerName) {
		Assert.hasText(ownerName, "货主名称不能为空");
		String prefix = ownerName.trim().replaceAll("\\s+", "_").toUpperCase(Locale.ROOT);
		Assert.isTrue(prefix.length() <= 32, "货主名称生成的仓库SKU前缀不能超过32个字符");
		return prefix;
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

	public String extractSkuCode(Long erpTenantId, String warehouseSkuCode) {
		if (erpTenantId == null || !StringUtils.hasText(warehouseSkuCode)) {
			return null;
		}
		SysTenant owner = tenantMapper.selectById(erpTenantId);
		if (owner == null || !StringUtils.hasText(owner.getWarehouseSkuPrefix())) {
			return null;
		}
		String prefix = owner.getWarehouseSkuPrefix().trim().toUpperCase(Locale.ROOT) + "-";
		String code = warehouseSkuCode.trim();
		if (!code.toUpperCase(Locale.ROOT).startsWith(prefix) || code.length() <= prefix.length()) {
			return null;
		}
		return code.substring(prefix.length());
	}

}
