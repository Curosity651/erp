package com.erp.admin.product.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * 税率验证注解 当含税时，税率必须填写且大于0
 *
 * @author ballcat
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { TaxRateValidator.class, GenericTaxRateValidator.class })
@Documented
public @interface ValidTaxRate {

	String message() default "税率验证失败";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
