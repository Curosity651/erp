package com.erp.admin.product.validation;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.erp.admin.product.enums.SkuResultCode;

/**
 * SKU编码格式验证器
 *
 * @author ballcat
 */
public class SkuCodeValidator implements ConstraintValidator<ValidSkuCode, String> {

	private static final Pattern SKU_CODE_PATTERN = Pattern.compile("^[A-Z0-9\\-_]{3,100}$");

	@Override
	public void initialize(ValidSkuCode constraintAnnotation) {
		// 初始化方法，可以获取注解参数
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.trim().isEmpty()) {
			return false;
		}

		// 验证SKU编码格式：只能包含大写字母、数字、横线和下划线，长度3-100
		boolean isValid = SKU_CODE_PATTERN.matcher(value.trim()).matches();

		if (!isValid) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate(SkuResultCode.INVALID_SKU_CODE_FORMAT.getMessage())
				.addConstraintViolation();
		}

		return isValid;
	}

}
