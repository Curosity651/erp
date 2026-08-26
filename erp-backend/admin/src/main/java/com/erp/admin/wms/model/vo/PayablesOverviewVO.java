package com.erp.admin.wms.model.vo;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/** Owner supplier and domestic-logistics accounts payable overview. */
@Data
@Builder
public class PayablesOverviewVO {

	private List<AssetFinanceOverviewVO.PayableCurrencyVO> supplierPayable;

	private AssetFinanceOverviewVO.PayableCurrencyVO providerPayable;

}
