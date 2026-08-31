package com.erp.admin.wms.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.model.entity.Sku;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/** Ensures a SKU can participate in the logical-location capacity ledger. */
@Service
@RequiredArgsConstructor
public class InboundSkuReadinessService {

	private final SkuMapper skuMapper;

	public void assertReadyForInbound(Collection<String> skuCodes) {
		Set<String> requested = skuCodes == null ? new LinkedHashSet<>() : skuCodes.stream()
			.filter(code -> code != null && !code.trim().isEmpty())
			.map(code -> code.trim().toUpperCase())
			.collect(Collectors.toCollection(LinkedHashSet::new));
		Assert.notEmpty(requested, "入库明细SKU不能为空");

		List<Sku> rows = skuMapper.selectBySkuCodes(requested);
		Map<String, Sku> byCode = rows.stream().collect(Collectors.toMap(
				sku -> sku.getSkuCode().trim().toUpperCase(), sku -> sku, (left, right) -> left,
				LinkedHashMap::new));
		List<String> invalid = requested.stream()
			.filter(code -> !isReady(byCode.get(code)))
			.collect(Collectors.toList());

		Assert.isTrue(invalid.isEmpty(),
				"以下SKU不存在或未维护完整外箱尺寸和单箱毛重，不能创建入库单：" + String.join("、", invalid));
	}

	private boolean isReady(Sku sku) {
		return sku != null
				&& positive(sku.getOuterLengthMm())
				&& positive(sku.getOuterWidthMm())
				&& positive(sku.getOuterHeightMm())
				&& positive(sku.getOuterGrossWeightG());
	}

	private boolean positive(Integer value) {
		return value != null && value > 0;
	}
}
