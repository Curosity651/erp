package com.erp.admin.product.validation;

import java.math.BigDecimal;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.erp.admin.product.enums.SkuResultCode;

/**
 * 通用税率验证器 当含税时，税率必须填写且大于0 支持所有实现了 TaxRateValidatable 接口的对象
 *
 * @author ballcat
 */
public class GenericTaxRateValidator implements ConstraintValidator<ValidTaxRate, TaxRateValidatable> {

	@Override
	public void initialize(ValidTaxRate constraintAnnotation) {
		// 初始化方法
	}

	@Override
	public boolean isValid(TaxRateValidatable validatable, ConstraintValidatorContext context) {
		if (validatable == null) {
			return true;
		}

		Integer includeTax = validatable.getIncludeTax();
		BigDecimal taxRate = validatable.getTaxRate();

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
