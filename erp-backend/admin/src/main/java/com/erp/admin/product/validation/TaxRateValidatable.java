package com.erp.admin.product.validation;

import java.math.BigDecimal;

/**
 * 税率校验接口 用于统一获取含税和税率信息的接口
 *
 * @author ballcat
 */
public interface TaxRateValidatable {

	/**
	 * 获取是否含税
	 * @return 是否含税 0:否 1:是
	 */
	Integer getIncludeTax();

	/**
	 * 获取税率
	 * @return 税率
	 */
	BigDecimal getTaxRate();

}
