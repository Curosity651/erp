package com.erp.admin.product.service;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.product.mapper.SkuBarcodeMapper;
import com.erp.admin.product.model.entity.SkuBarcode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkuBarcodeService {

	private final SkuBarcodeMapper barcodeMapper;

	private final WarehouseSkuCodeService warehouseSkuCodeService;

	@Transactional(rollbackFor = Exception.class)
	public void replace(Long skuId, String skuCode, List<String> barcodes) {
		Assert.notNull(skuId, "SKU ID不能为空");
		barcodeMapper.deleteBySkuId(skuId);
		if (barcodes == null) {
			return;
		}
		Set<String> normalized = barcodes.stream()
				.filter(StringUtils::hasText)
				.map(String::trim)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		Assert.isTrue(normalized.size() <= 20, "一个SKU最多维护20个商品条码");
		int index = 0;
		for (String barcode : normalized) {
			Assert.isTrue(barcode.length() <= 128, "商品条码长度不能超过128位: " + barcode);
			SkuBarcode entity = new SkuBarcode();
			entity.setTenantId(TenantContext.getCurrentTenant());
			entity.setSkuId(skuId);
			entity.setSkuCode(skuCode);
			entity.setBarcode(barcode);
			entity.setBarcodeType("EAN_UPC");
			entity.setPrimaryFlag(index++ == 0 ? 1 : 0);
			entity.setEnabled(1);
			barcodeMapper.insert(entity);
		}
	}

	public List<String> listBySkuId(Long skuId) {
		if (skuId == null) {
			return new ArrayList<>();
		}
		return barcodeMapper.selectBySkuId(skuId).stream()
				.map(SkuBarcode::getBarcode).collect(Collectors.toList());
	}

	public void deleteBySkuId(Long skuId) {
		if (skuId != null) {
			barcodeMapper.deleteBySkuId(skuId);
		}
	}

	public String resolveSkuCode(Long erpTenantId, String scanCode) {
		if (!StringUtils.hasText(scanCode)) {
			return null;
		}
		String code = scanCode.trim();
		String warehouseSkuCode = warehouseSkuCodeService.extractSkuCode(erpTenantId, code);
		if (warehouseSkuCode != null) {
			return warehouseSkuCode;
		}
		return TenantContext.runAs(erpTenantId, () -> {
			SkuBarcode barcode = barcodeMapper.selectByBarcode(code);
			return barcode == null ? null : barcode.getSkuCode();
		});
	}

	public boolean matches(Long erpTenantId, String skuCode, String scanCode) {
		if (!StringUtils.hasText(scanCode)) {
			return false;
		}
		if (skuCode != null && skuCode.equalsIgnoreCase(scanCode.trim())) {
			return true;
		}
		if (warehouseSkuCodeService.matches(erpTenantId, skuCode, scanCode)) {
			return true;
		}
		String resolved = resolveSkuCode(erpTenantId, scanCode);
		return skuCode != null && skuCode.equalsIgnoreCase(resolved);
	}

}
