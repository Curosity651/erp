package com.erp.admin.wms;

import java.util.Arrays;
import java.util.Collections;

import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.wms.service.InboundSkuReadinessService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InboundSkuReadinessServiceTest {

	private final SkuMapper skuMapper = mock(SkuMapper.class);

	private final InboundSkuReadinessService service = new InboundSkuReadinessService(skuMapper);

	@Test
	void accepts_skus_with_complete_outer_box_data() {
		Sku sku = readySku("SKU-A");
		when(skuMapper.selectBySkuCodes(Collections.singleton("SKU-A"))).thenReturn(Collections.singletonList(sku));

		assertThatCode(() -> service.assertReadyForInbound(Collections.singleton("SKU-A")))
			.doesNotThrowAnyException();
	}

	@Test
	void rejects_missing_or_incomplete_skus_with_actionable_message() {
		Sku incomplete = readySku("SKU-A");
		incomplete.setOuterGrossWeightG(null);
		when(skuMapper.selectBySkuCodes(Arrays.asList("SKU-A", "SKU-B")))
			.thenReturn(Collections.singletonList(incomplete));

		assertThatThrownBy(() -> service.assertReadyForInbound(Arrays.asList("SKU-A", "SKU-B")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("SKU-A")
			.hasMessageContaining("SKU-B")
			.hasMessageContaining("外箱尺寸和单箱毛重");
	}

	private Sku readySku(String skuCode) {
		Sku sku = new Sku();
		sku.setSkuCode(skuCode);
		sku.setOuterLengthMm(100);
		sku.setOuterWidthMm(100);
		sku.setOuterHeightMm(100);
		sku.setOuterGrossWeightG(1000);
		return sku;
	}
}
