package com.erp.admin.wms;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.platform.finance.mapper.WmsMonthlyBillMapper;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.OperatorFinanceMapper;
import com.erp.admin.wms.mapper.WmsLogisticsProductMapper;
import com.erp.admin.wms.model.vo.OperatorIncomeVO;
import com.erp.admin.wms.service.OperatorFinanceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperatorFinanceServiceTest {

	@AfterEach
	void clearContext() {
		WmsTenantContext.clear();
	}

	@Test
	void income_summary_never_adds_different_currencies_together() {
		OperatorFinanceMapper financeMapper = mock(OperatorFinanceMapper.class);
		WmsLogisticsProductMapper productMapper = mock(WmsLogisticsProductMapper.class);
		WmsMonthlyBillMapper monthlyBillMapper = mock(WmsMonthlyBillMapper.class);
		TenantIdentityService identityService = mock(TenantIdentityService.class);

		TenantIdentityVO identity = new TenantIdentityVO();
		identity.setIdentityType(TenantIdentityService.IDENTITY_WMS_OPERATOR);
		when(identityService.currentIdentity(any())).thenReturn(identity);
		when(productMapper.listByIds(any())).thenReturn(Collections.emptyList());

		OperatorIncomeVO.SummaryRow rub = row("RUB", "100.00", 2L);
		OperatorIncomeVO.SummaryRow cny = row("CNY", "20.00", 1L);
		when(financeMapper.sumIncomeByProductMonth(eq(5L), any(), any(), any()))
			.thenReturn(Arrays.asList(rub, cny));

		OperatorFinanceService service = new OperatorFinanceService(financeMapper, productMapper,
				monthlyBillMapper, identityService);
		WmsTenantContext.setCurrentWmsTenant(5L);

		OperatorIncomeVO.Summary summary = service.incomeSummary(null, null, null);

		assertThat(summary.getCurrencyTotals())
			.extracting(OperatorIncomeVO.CurrencyTotal::getCurrency, OperatorIncomeVO.CurrencyTotal::getAmount)
			.containsExactlyInAnyOrder(
					org.assertj.core.groups.Tuple.tuple("RUB", new BigDecimal("100.00")),
					org.assertj.core.groups.Tuple.tuple("CNY", new BigDecimal("20.00")));
		assertThat(summary.getTotalCount()).isEqualTo(3L);
	}

	private OperatorIncomeVO.SummaryRow row(String currency, String subtotal, Long usageCount) {
		OperatorIncomeVO.SummaryRow row = new OperatorIncomeVO.SummaryRow();
		row.setCurrency(currency);
		row.setSubtotal(new BigDecimal(subtotal));
		row.setUsageCount(usageCount);
		return row;
	}
}
