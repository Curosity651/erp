package com.erp.admin.wms;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.OperatorDashboardMapper;
import com.erp.admin.wms.mapper.WmsLogisticsProductMapper;
import com.erp.admin.wms.model.vo.OperatorDashboardVO;
import com.erp.admin.wms.service.OperatorDashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperatorDashboardCurrencyTest {

	@AfterEach
	void clearContext() {
		WmsTenantContext.clear();
	}

	@Test
	void dashboard_separates_currencies_and_expense_statuses() {
		OperatorDashboardMapper mapper = mock(OperatorDashboardMapper.class);
		WmsLogisticsProductMapper productMapper = mock(WmsLogisticsProductMapper.class);
		TenantIdentityService identityService = mock(TenantIdentityService.class);
		TenantIdentityVO identity = new TenantIdentityVO();
		identity.setIdentityType(TenantIdentityService.IDENTITY_WMS_OPERATOR);
		when(identityService.currentIdentity(any())).thenReturn(identity);

		when(mapper.sumIncomeByMonth(any(), any(), any(), any())).thenReturn(Arrays.asList(
			row("2026-08", "RUB", null, "100", 2L),
			row("2026-08", "CNY", null, "20", 1L)));
		when(mapper.sumExpenseByMonth(any(), any(), any())).thenReturn(Arrays.asList(
			row("2026-08", "CNY", "DRAFT", "10", null),
			row("2026-08", "CNY", "CONFIRMED", "40", null),
			row("2026-08", "CNY", "DISPUTED", "20", null),
			row("2026-08", "CNY", "PAID", "30", null)));
		when(mapper.countOwners(any())).thenReturn(Collections.emptyMap());
		when(mapper.countProducts(any())).thenReturn(Collections.emptyMap());
		when(mapper.sumByProduct(any(), any(), any(), any())).thenReturn(Collections.emptyList());
		when(mapper.sumIncomeByOwner(any(), any(), any(), any())).thenReturn(Collections.emptyList());
		when(mapper.countOutboundByOwner(any(), any(), any(), any())).thenReturn(Collections.emptyList());
		when(mapper.sumOwnerOnHand(any(), any())).thenReturn(0L);
		when(mapper.sumRacks(any(), any())).thenReturn(Collections.emptyMap());
		when(productMapper.listByIds(any())).thenReturn(Collections.emptyList());

		WmsTenantContext.setCurrentWmsTenant(5L);
		OperatorDashboardVO data = new OperatorDashboardService(mapper, productMapper, identityService)
			.getData("2026-08", "2026-08", null);

		assertThat(data.getOverview().getIncomeByCurrency())
			.extracting(OperatorDashboardVO.CurrencyAmount::getCurrency, OperatorDashboardVO.CurrencyAmount::getAmount)
			.containsExactlyInAnyOrder(
					org.assertj.core.groups.Tuple.tuple("RUB", new BigDecimal("100")),
					org.assertj.core.groups.Tuple.tuple("CNY", new BigDecimal("20")));
		OperatorDashboardVO.ExpenseSummary cny = data.getOverview().getExpenseByCurrency().get(0);
		assertThat(cny.getDraftEstimate()).isEqualByComparingTo("10");
		assertThat(cny.getConfirmedPayable()).isEqualByComparingTo("70");
		assertThat(cny.getDisputedAmount()).isEqualByComparingTo("20");
		assertThat(cny.getPaidAmount()).isEqualByComparingTo("30");
		assertThat(data.getOverview().getBalanceByCurrency())
			.extracting(OperatorDashboardVO.CurrencyAmount::getCurrency, OperatorDashboardVO.CurrencyAmount::getAmount)
			.contains(org.assertj.core.groups.Tuple.tuple("CNY", new BigDecimal("-50")));
	}

	private Map<String, Object> row(String month, String currency, String status, String amount, Long count) {
		Map<String, Object> row = new HashMap<>();
		row.put("billMonth", month);
		row.put("currency", currency);
		row.put("status", status);
		row.put("amount", new BigDecimal(amount));
		row.put("cnt", count);
		return row;
	}
}
