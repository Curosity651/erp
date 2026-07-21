package com.erp.admin.product.validation;

import java.math.BigDecimal;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.erp.admin.product.enums.SkuResultCode;
import com.erp.admin.product.model.entity.Sku;

/**
 * 税率验证器 当含税时，税率必须填写且大于0
 *
 * @author ballcat
 */
public class TaxRateValidator implements ConstraintValidator<ValidTaxRate, Sku> {

	@Override
	public void initialize(ValidTaxRate constraintAnnotation) {
		// 初始化方法
	}

	@Override
	public boolean isValid(Sku sku, ConstraintValidatorContext context) {
		if (sku == null) {
			return true;
		}

		Integer includeTax = sku.getIncludeTax();
		BigDecimal taxRate = sku.getTaxRate();

		// 如果含税，税率必须填写且大于0
		if (includeTax != null && includeTax == 1) {
			if (taxRate == null || taxRate.compareTo(BigDecimal.ZERO) <= 0) {
				context.disableDefaultConstraintViolation();
				context.buildConstraintViolationWithTemplate(SkuResultCode.INVALID_TAX_RATE.getMessage())
					.addPropertyNode("taxRate")
					.addConstraintViolation();
				return false;
			}
		}

		return true;
	}

}
